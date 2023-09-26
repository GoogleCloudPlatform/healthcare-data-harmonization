/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.function;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.wrappers.WrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.MultipleMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * DefaultOverloadSelector is a class used for selecting a function implementation, by comparing its
 * signature to some given arguments. Uses a distance heuristic to disambiguate overloads.
 */
public class DefaultOverloadSelector implements OverloadSelector {
  // the small penalty to add when distance is calculated by unwrapping the data. And maximum number
  // of argument allowed for the distance calculation to stay accurate is 1/WRAPPER_EPSILON.
  public static final Double WRAPPER_EPSILON = 1.0E-5;

  /**
   * Returns the distance between two types. A distance of 0 indicates that the two types are the
   * same exact type. A distance of 1 indicates that {@code want} is an interface implemented by
   * {@code got}. A distance of 2 indicates that {@code want} is a superclass/superinterface of
   * {@code got} (i.e. {@code want} can be assigned an instance of {@code got}, or in other words
   * {@code got} can be upcast to {@code want}). A distance of {@link Double#POSITIVE_INFINITY}
   * indicates that the types do not match at all.
   *
   * @param want the base type getting matched to.
   * @param got the type being check for assignability and distance to the base type.
   */
  static double distance(@Nonnull Class<? extends Data> want, @Nonnull Data got) {
    Set<Class<?>> gotClasses = new HashSet<>();
    gotClasses.add(got.getClass());

    if (gotClasses.contains(want) || (want.equals(NullData.class) && got.isNullOrEmpty())) {
      return 0;
    }

    if (got.isArray()) {
      gotClasses.add(Array.class);
    }
    if (got.isContainer()) {
      gotClasses.add(Container.class);
    }
    if (got.isDataset()) {
      gotClasses.add(Dataset.class);
    }
    if (got.isPrimitive()) {
      gotClasses.add(Primitive.class);
    }
    if (Arrays.asList(got.getClass().getInterfaces()).contains(Data.class)) {
      gotClasses.add(Data.class);
    }

    if (want.isInterface() && gotClasses.contains(want)) {
      return 1;
    }
    if (want.isAssignableFrom(got.getClass())) {
      return 2;
    }

    if (got.isClass(WrapperData.class) && got.isClass(want)) {
      return WRAPPER_EPSILON + distance(want, got.asClass(want));
    }

    return Double.POSITIVE_INFINITY;
  }

  /**
   * Computes the distance between the given arguments' types and the given signature. This applies
   * {@link #distance(Class, Data)} to each argument type in the signature against each argument in
   * gotArgs.
   *
   * @return The sum of all distances. A value of {@link Double#POSITIVE_INFINITY} indicates that at
   *     least one argument did not match.
   */
  public static double distance(@Nonnull Signature want, @Nonnull Data[] gotArgs) {
    double sum = 0;
    List<Class<? extends Data>> args = want.getArgs();
    int i;
    for (i = 0; i < args.size(); i++) {
      if (i == args.size() - 1 && want.isVariadic()) {
        boolean doNotUnpackArray = Array.class.isAssignableFrom(want.getLastArgType());
        Data[] remainingGotArgs = normalizeVariadic(gotArgs, i, doNotUnpackArray);
        if (!doNotUnpackArray
            && gotArgs.length == i + 1
            && gotArgs[i] != null
            && gotArgs[i].isArray()) {
          sum += 0.1;
        }
        int finalI = i;
        sum +=
            stream(remainingGotArgs)
                .mapToDouble(gotArg -> gotArg != null ? distance(args.get(finalI), gotArg) : 0)
                .max()
                .orElse(0);
        i = gotArgs.length;
        break;
      }

      // Not enough args given.
      if (i >= gotArgs.length) {
        return Double.POSITIVE_INFINITY;
      }
      if (gotArgs[i] == null) {
        continue;
      }

      sum += distance(args.get(i), gotArgs[i]);
    }

    // Too many args given.
    if (i < gotArgs.length) {
      return Double.POSITIVE_INFINITY;
    }

    return sum;
  }

  /**
   * Normalizes parameters passed by a user to a variadic function into a single array.
   *
   * <p>Given a variadic function like {@code fn(a, b, v...)}, values for v can be:
   *
   * <ol>
   *   <li>omitted entirely - {@code fn(a, b)}
   *   <li>Passed as an {@link Array} - {@code fn(a, b, [v1, v2, vn])}
   *   <li>Passed as a series of independent args - {@code fn(a, b, v1, v2, vn)}
   * </ol>
   *
   * Given the index of the variadic argument (in the example above, index 2) and all the arguments
   * passed to the function (including {@code a, b} in the example above) this function returns an
   * array of all the arguments being assigned to the variadic parameter ({@code v1, v2, vn} in the
   * second and third examples above).
   */
  private static Data[] normalizeVariadic(Data[] args, int startIndex, boolean doNotUnpackArray) {
    // There are a few cases for a variadic argument:

    // 1) Omitted entirely.
    if (args.length <= startIndex) {
      return new Data[0];
    }

    // 2) Present as a single array argument (only if we are allowed to unpack it though)
    if (!doNotUnpackArray
        && args.length == startIndex + 1
        && args[startIndex] != null
        && args[startIndex].isArray()) {
      return args[startIndex].asArray().stream().toArray(Data[]::new);
    }

    // 3) Present as multiple independent args
    return Arrays.copyOfRange(args, startIndex, args.length);
  }

  /**
   * Selects the {@link CallableFunction} from the given list whose signature best matches the given
   * args. A best match is defined as the one with the smallest {@link #distance(Signature,
   * Data[])}.
   *
   * @throws NoMatchingOverloadsException If
   *     <ul>
   *       <li>No functions are given.
   *       <li>None of the given functions match at all.
   *     </ul>
   *
   * @throws MultipleMatchingOverloadsException If more than one of the given functions share the
   *     smallest distance.
   */
  @Override
  public CallableFunction select(List<CallableFunction> overloadGroup, Data[] args) {
    if (overloadGroup == null || overloadGroup.isEmpty()) {
      throw new NoMatchingOverloadsException();
    }

    // If there is only one option, then it's not really an overload.
    if (overloadGroup.size() == 1) {
      double dist = distance(overloadGroup.get(0).getSignature(), args);
      if (Double.isInfinite(dist)) {
        throw new NoMatchingOverloadsException(overloadGroup.get(0), args);
      }

      return overloadGroup.get(0);
    }

    // Find the closest matching distance.
    CallableFunction closest = null;
    double minDistance = Double.POSITIVE_INFINITY;
    boolean multipleClosest = true;

    for (CallableFunction candidate : overloadGroup) {
      double distance = distance(candidate.getSignature(), args);
      if (Double.isInfinite(distance)) {
        continue;
      }

      if (Double.isInfinite(minDistance) || distance < minDistance) {
        minDistance = distance;
        closest = candidate;
        multipleClosest = false;
      } else if (minDistance == distance) {
        multipleClosest = true;
      }
    }

    // Fail if closest is infinite, i.e. mismatch.
    if (Double.isInfinite(minDistance)) {
      throw new NoMatchingOverloadsException(overloadGroup, args);
    }

    // If multiple closest matches, then we only find them now to save on performance during initial
    // search.
    if (multipleClosest) {
      double finalMinDistance = minDistance;
      ImmutableList<CallableFunction> allClosest =
          overloadGroup.stream()
              .filter(c -> distance(c.getSignature(), args) == finalMinDistance)
              .collect(toImmutableList());

      throw new MultipleMatchingOverloadsException(allClosest, args);
    }

    return closest;
  }
}
