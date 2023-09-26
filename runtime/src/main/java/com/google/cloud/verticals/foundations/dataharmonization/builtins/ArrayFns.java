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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.stream.Collectors.toCollection;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.SingleNullArrayExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Preconditions;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Builtin functions for dealing with arrays. */
public final class ArrayFns {

  private ArrayFns() {}

  /**
   * Creates a new {@code Array} from the values provided to the {@code items} argument. Provides a
   * convenient way to create an {@code Array} without needing to be concerned with the {@code
   * Array} implementation. Creating an array using {@code [foo, bar, baz]} is syntactic sugar
   * for the equivalent {@code arrayOf(foo, bar, baz)}.
   *
   * <pre><code>
   * var container1: {
   *   entry: [
   *     {
   *       request: {
   *         method: "POST"
   *         url: "Type/abc123"
   *     }
   *       resource: {
   *         active: "true"
   *         id: "1234567890"
   *       }
   *     }
   *   ]
   * }
   * var array1: ["item1", 1, 2] // Syntactic sugar for arrayOf("item1", 1, 2)
   * var string1: "mystring"
   *
   * // Pass the data to arrayOf().
   * arrayOf(container1, array1, string1)
   *
   * // Returns the following output:
   * // [
   * //   {
   * //     "entry": [
   * //       {
   * //         "request": {
   * //           "method": "POST",
   * //           "url": "Type/abc123"
   * //         },
   * //         "resource": {
   * //           "active": "true",
   * //           "id": "1234567890"
   * //         }
   * //       }
   * //     ]
   * //   },
   * //   {
   * //     "code": "200",
   * //     "response": "out"
   * //   },
   * //   "mystring"
   * // ]
   *
   * // Passing an array to arrayOf() returns the same array.
   * arrayOf(array1)
   * // Returns the following output:
   * ["item1", 1, 2]
   * </code></pre>
   *
   * @param items one or more {@code Data} data elements
   */
  @PluginFunction
  public static Array arrayOf(RuntimeContext ctx, Data... items) {
    if (!SingleNullArrayExperiment.isEnabled(ctx)
        && items.length == 1
        && items[0] == NullData.instance) {
      return ctx.getDataTypeImplementation().emptyArray();
    }
    return ctx.getDataTypeImplementation().arrayOf(ImmutableList.copyOf(items));
  }

  /** Returns the length of the given {@link Array}. */
  @PluginFunction
  public static Primitive listLen(RuntimeContext ctx, Array array) {
    return ctx.getDataTypeImplementation().primitiveOf((double) array.size());
  }

  /**
   * Returns an array of sequentially ordered integers from start (inclusive) to end (exclusive) by
   * an increment step of 1. Example:
   *
   * <pre>
   * var x : range(5, 10) // x: [5, 6, 7, 8, 9]
   * </pre>
   */
  @PluginFunction
  public static Array range(RuntimeContext ctx, Primitive start, Primitive end) {
    int castedStart = (int) Preconditions.requireNum(start, "start");
    int castedEnd = (int) Preconditions.requireNum(end, "end");
    return ctx.getDataTypeImplementation()
        .arrayOf(
            IntStream.range(castedStart, castedEnd)
                .mapToObj(i -> ctx.getDataTypeImplementation().primitiveOf((double) i))
                .collect(toImmutableList()));
  }

  /**
   * Returns an array of sequentially ordered integers with provided size starting from 0 and
   * incremented in steps of 1. Example:
   *
   * <pre>
   * var x : range(5) // x: [0, 1, 2, 3, 4]
   * </pre>
   */
  @PluginFunction
  public static Array range(RuntimeContext ctx, Primitive size) {
    return range(ctx, ctx.getDataTypeImplementation().primitiveOf(0.0), size);
  }

  /** Catch-all for where applied to null, to disambiguate null from Array and Container. */
  @PluginFunction
  public static NullData where(NullData nullData, Closure predicate) {
    return nullData;
  }

  /**
   * Filters the given {@link Array}, returning a new one containing only items that match the given
   * predicate. Each array element is represented in the predicate by {@code $}. Example:
   *
   * <pre>{@code
   * var array: [-1, 2, -3, -4, 5, -6]
   * var positiveArray: array[where $ > 0] // positiveArray: [2, 5]
   * }</pre>
   */
  @PluginFunction
  public static Array where(RuntimeContext context, Array array, Closure predicate) {
    return context
        .getDataTypeImplementation()
        .arrayOf(
            array.stream()
                .filter(i -> Ternary.isTruthy(predicate.bindNextFreeParameter(i).execute(context)))
                .collect(toImmutableList()));
  }

  /**
   * Filters the given {@link Container}, returning a new one containing only items that match the
   * given predicate. For each field-value pair in the Container, they are available in the
   * predicate as {@code $.field} and {@code $.value}. If the predicate returns false, the resulting
   * container will not have this field-value pair present. Example:
   *
   * <pre>{@code
   * var container: {
   *   f1: 1; f2: 2; f3: 3; f4: 4;
   * }
   * var newContainer: container[where $.value < 2 || $.field == "f4"]
   * // newContainer : {f1: 1; f4: 4;}
   * }</pre>
   */
  @PluginFunction
  public static Container where(RuntimeContext context, Container container, Closure predicate) {
    DataTypeImplementation dti = context.getDataTypeImplementation();
    return dti.containerOf(
        container.fields().stream()
            .filter(
                f ->
                    Ternary.isTruthy(
                        predicate
                            .bindNextFreeParameter(
                                dti.containerOf(
                                    ImmutableMap.of(
                                        "field",
                                        dti.primitiveOf(f),
                                        "value",
                                        container.getField(f))))
                            .execute(context)))
            .collect(toImmutableMap(Function.identity(), container::getField)));
  }

  /**
   * Groups the elements of an array by their keys extracted by the {@code keyExtractor} closure.
   * The output is an array of objects/containers, each with two fields "key" and "value", analogous
   * to key:value pairs. The "key" field of each container in the return value is a unique join key
   * in the original collection computed by the {@code keyExtractor} closure. The "value" field will
   * be a collection of elements from the original collection that maps to the join key in the "key"
   * field. Example:
   *
   * <pre>{@code
   * var array: [{num: 1; word: "one";}, {num: 2; word: "two";},
   *             {num: 3; word: "three";}, {num: 4; word: "four";}]
   * var groupByResult: array[groupBy if $.num > 2 then "biggerThan2" else $.num + 2]
   * // groupByResult == [{key: "biggerThan2"; elements: [{num: 3; word: "three";},
   * //                                                   {num: 4; word: "four";}];},
   * //                   {key: 3; elements: [{num: 1; word: "one";}];},
   * //                   {key: 4; elements: [{num: 2; word: "two";}];}
   * //                   ]
   *
   * }</pre>
   */
  @PluginFunction
  public static Array groupBy(RuntimeContext context, Array array, Closure keyExtractor) {
    Map<Data, List<Data>> groupByResult =
        array.stream()
            .collect(
                Collectors.groupingBy(
                    element -> keyExtractor.bindNextFreeParameter(element).execute(context)));
    return context
        .getDataTypeImplementation()
        .arrayOf(
            groupByResult.entrySet().stream()
                .map(
                    e ->
                        context
                            .getDataTypeImplementation()
                            .containerOf(
                                ImmutableMap.of(
                                    "key",
                                    e.getKey(),
                                    "elements",
                                    context.getDataTypeImplementation().arrayOf(e.getValue()))))
                .collect(ImmutableList.toImmutableList()));
  }

  /**
   * Returns the lastIndex data {@link Data} in a given {@link Array} or Null data for an empty
   * array
   */
  @PluginFunction
  public static Data last(Array array) {
    int lastIndex = array.size() - 1;
    return lastIndex >= 0 ? array.getElement(lastIndex) : NullData.instance;
  }

  /**
   * Remove duplicate data from the input {@link Data}. Elements are compared with the {@link
   * #equals} method. There is no guarantee as to which specific duplicates will be removed. The
   * order of the elements in the returned value will be preserved.
   *
   * @param array an array of elements
   * @return a {@link Data} object with duplicates removed
   */
  @PluginFunction
  public static Array unique(RuntimeContext ctx, Array array) {
    return ctx.getDataTypeImplementation()
        .arrayOf(new LinkedHashSet<>(array.stream().collect(toImmutableList())));
  }

  /**
   * Remove duplicates from the input {@link Array}. Elements are compared using the provided
   * 'keySelector' {@link Closure} function. Existing values take precedence over later values, such
   * that repeated elements are resolved by removing the later-occurring element in the Array. The
   * order of the elements in the returned value will be preserved.
   *
   * @param ctx {@link RuntimeContext} within which to run the plugin function.
   * @param array {@link Array} to deduplicate.
   * @param keySelector {@link Closure} for extracting key from array entries to deduplicate over.
   * @return a {@link Array} with duplicate elements removed.
   */
  @PluginFunction
  public static Array uniqueBy(RuntimeContext ctx, Array array, Closure keySelector) {
    return ctx.getDataTypeImplementation()
        .arrayOf(
            new ArrayList<>(
                array.stream()
                    .collect(
                        Collectors.toMap(
                            e -> keySelector.bindNextFreeParameter(e).execute(ctx),
                            e -> e,
                            (e1, e2) -> e1,
                            LinkedHashMap<Data, Data>::new))
                    .values()));
  }

  /** Denotes a sorting direction to orient a comparison of objects. */
  private enum SortDirection {
    ASCENDING,
    DESCENDING
  }

  /** Comparator for {@link Primitive} objects. */
  private static class PrimitiveComparator implements Comparator<Primitive> {

    /**
     * Compares two {@link Primitive} objects using NullData-first ordering.
     *
     * @param e1 first (this) Primitive to compare.
     * @param e2 second (that) Primitive to compare.
     * @return a negative integer, zero, or a positive integer when e1 is less than, equal to, or
     *     greater than e2, respectively
     * @throws UnsupportedOperationException if comparison between different members of e1 and e2 is
     *     attempted (e.g. e1 has non-null string member, while e2 has non-null numeric member).
     */
    @Override
    public int compare(Primitive e1, Primitive e2) {
      if (bothNumeric(e1, e2)) {
        return e1.num().compareTo(e2.num());
      } else if (bothStrings(e1, e2)) {
        return e1.string().compareTo(e2.string());
      } else if (bothBooleans(e1, e2)) {
        return e1.bool().compareTo(e2.bool());
      } else if (e1.isNullOrEmpty()) {
        return e2.isNullOrEmpty() ? 0 : -1;
      } else if (e2.isNullOrEmpty()) {
        return 1;
      } else {
        throw new UnsupportedOperationException(
            String.format(
                "Comparison between different members of Primitive objects is not supported"
                    + " (e1: %s, e2: %s).",
                e1, e2));
      }
    }

    private static boolean bothNumeric(Primitive first, Primitive second) {
      return (first.num() != null) && (second.num() != null);
    }

    private static boolean bothStrings(Primitive first, Primitive second) {
      return (first.string() != null) && (second.string() != null);
    }

    private static boolean bothBooleans(Primitive first, Primitive second) {
      return (first.bool() != null) && (second.bool() != null);
    }
  }

  /**
   * Extracts {@link Primitive} key from given element.
   *
   * @param element {@link Data} object from which to extract key.
   * @param keySelector {@link Closure} to use for extracting sortBy key.
   * @param context {@link RuntimeContext} within which to run key selector.
   * @return extracted Primitive key.
   * @throws UnsupportedOperationException if the keySelector provided returns non-Primitive types.
   */
  private static Primitive selectKeyAsPrimitive(
      Data element, Closure keySelector, RuntimeContext context) {
    Data key = keySelector.bindNextFreeParameter(element).execute(context);
    if (!key.isPrimitive()) {
      throw new UnsupportedOperationException(
          String.format("Expected Primitive for sortBy key, but got %s instead.", key.getClass()));
    }
    return key.asPrimitive();
  }

  /** Returns PrimitiveComparator with natural or reversed ordering depending on sortDirection. */
  private static Comparator<Primitive> getComparator(SortDirection sortDirection) {
    PrimitiveComparator comparator = new PrimitiveComparator();
    return sortDirection.equals(SortDirection.ASCENDING) ? comparator : comparator.reversed();
  }

  /**
   * Sorts {@link Array} in sortDirection using key specified by the provided keySelector.
   *
   * @param context {@link RuntimeContext} within which to run key selector.
   * @param array {@link Array} to sort.
   * @param keySelector {@link Closure} to use for extracting sortBy key, which must be a Primitive.
   * @param sortDirection {@link SortDirection} specifying the direction of the sort.
   * @return sorted {@link Array}.
   */
  private static Array sortBy(
      RuntimeContext context, Array array, Closure keySelector, SortDirection sortDirection) {
    Comparator<Primitive> comparator = getComparator(sortDirection);
    return context
        .getDataTypeImplementation()
        .arrayOf(
            array.stream()
                .sorted(
                    Comparator.comparing(
                        elem -> selectKeyAsPrimitive(elem, keySelector, context), comparator))
                .collect(toImmutableList()));
  }

  /**
   * Sorts an {@link Array} using the key specified by the provided {@link Closure}.
   *
   * @param context {@link RuntimeContext} within which to run key selector.
   * @param array {@link Array} to sort.
   * @param keySelector {@link Closure} to use for extracting sortBy key.
   * @return sorted {@link Array}.
   */
  @PluginFunction
  public static Array sortBy(RuntimeContext context, Array array, Closure keySelector) {
    return sortBy(context, array, keySelector, SortDirection.ASCENDING);
  }

  /**
   * Sorts an {@link Array} in descending order using the key specified by the provided {@link
   * Closure}.
   *
   * @param context {@link RuntimeContext} within which to run key selector.
   * @param array {@link Array} to sort.
   * @param keySelector {@link Closure} to use for extracting sort-by key.
   * @return {@link Array} sorted in descending order.
   */
  @PluginFunction
  public static Array sortByDescending(RuntimeContext context, Array array, Closure keySelector) {
    return sortBy(context, array, keySelector, SortDirection.DESCENDING);
  }

  /**
   * Performs a reduction on the elements of an {@link Array} using an associative accumulation
   * {@link Closure}. If the array is empty, the given seed is returned.
   *
   * <p>Example:
   *
   * <pre><code>
   * reduce([1, 2, 3], 10, $acc + $cur) == 16
   * reduce([1], 10, $acc + $cur) == 11
   * reduce([1], 10, "some const") == "some const"
   * </code></pre>
   *
   * @param context {@link RuntimeContext} within which to run the accumulator.
   * @param array {@link Array} to reduce.
   * @param seed Initial value of $acc.
   * @param accumulator {@link Closure} to use as the accumulation function. <code>$acc</code>
   *     represents the cumulative reduced value so far, and <code>$cur</code> represents the
   *     current value to accumulate. The result of this closure is the next <code>$acc</code>.
   * @return {@link Data} representing the result of the reduction.
   */
  @PluginFunction
  public static Data reduce(RuntimeContext context, Array array, Data seed, Closure accumulator) {
    return array.stream()
        .reduce(
            seed,
            (acc, cur) ->
                accumulator.bindNextFreeParameter(acc).bindNextFreeParameter(cur).execute(context));
  }

  /**
   * Performs a reduction on the elements of an {@link Array} using an associative accumulation
   * {@link Closure}. If the array is empty, null data is returned. If the array has only one item,
   * that item is returned.
   *
   * <p>Example:
   *
   * <pre><code>
   * reduce([1, 2, 3], $acc + $cur) == 6
   * reduce([1], $acc + $cur) == 1
   * reduce([1], "some const") == 1
   * </code></pre>
   *
   * @param context {@link RuntimeContext} within which to run the accumulator.
   * @param array {@link Array} to reduce.
   * @param accumulator {@link Closure} to use as the accumulation function. <code>$acc</code>
   *     represents the cumulative reduced value so far, and <code>$cur</code> represents the
   *     current value to accumulate. The result of this closure is the next <code>$acc</code>.
   * @return {@link Data} representing the result of the reduction.
   */
  @PluginFunction
  public static Data reduce(RuntimeContext context, Array array, Closure accumulator) {
    return array.stream()
        .reduce(
            (acc, cur) ->
                accumulator.bindNextFreeParameter(acc).bindNextFreeParameter(cur).execute(context))
        .orElse(NullData.instance);
  }

  /**
   * Performs a full outer join on the two given arrays.
   *
   * <p>Example:
   *
   * <pre><code>
   * var array1: [{
   *   id: 1
   *   val: "1aaa"
   * }, {
   *   id: 2
   *   val: "1bbb"
   * }]
   * var array2: [{
   *   id: 1
   *   val: "2aaa"
   * }, {
   *   id: 3
   *   val: "2ccc"
   * }]
   *
   * join(array1, array2, $left.id == $right.id) == [
   *  [{ id: 1; val: "1aaa"; }, { id: 1; val: "2aaa"; }],
   *  [{ id: 2; val: "1bbb"; }, {}],
   *  [{}, { id: 3; val: "2ccc"; }]
   * ]
   * </code>
   * </pre>
   *
   * <b>Ordering:</b> Order is preserved such that:
   *
   * <ul>
   *   <li>Items from left (and corresponding matches from right, if any) appear first
   *   <li>Unmatched remaining items from right appear last
   * </ul>
   *
   * <b>Duplicate items:</b> Duplicate items are only matched once. That is, if left is {@code Al Al
   * Bl}, and right is {@code Ar Br Br Cr}, the result is
   *
   * <pre>
   * Al - Ar
   * Al - null
   * Bl - Br
   * null - Br
   * null - Cr
   * </pre>
   *
   * @param context {@link RuntimeContext} within which to run the join.
   * @param left The left array
   * @param right The right array
   * @param joinOp a predicate operating on $left and $right, which returns true if the two elements
   *     shall be joined together
   * @return Array of joined elements. Joined pairs are themselves arrays, with matching elements
   *     from left and right, or nulls in place of no matches.
   */
  @PluginFunction
  public static Array join(RuntimeContext context, Array left, Array right, Closure joinOp) {
    // TODO(rpolyano): This is currently O(n^2), so would only be effective with small arrays.
    //    Need to optimize but preserving semantics (duplicates handling and ordering) may be
    //    tricky.
    List<Data> unjoinedRight = right.stream().collect(toCollection(ArrayList::new));
    List<Data> joined = new ArrayList<>(left.size() + right.size());
    left.stream()
        .forEach(
            l -> {
              AtomicReference<Data> matched = new AtomicReference<>();
              unjoinedRight.forEach(
                  r -> {
                    if (matched.get() != null) {
                      return;
                    }
                    matched.set(
                        Ternary.isTruthy(
                                joinOp
                                    .bindNextFreeParameter(l)
                                    .bindNextFreeParameter(r)
                                    .execute(context))
                            ? r
                            : null);
                  });
              Data join =
                  joined(
                      context.getDataTypeImplementation(),
                      l,
                      matched.get() != null ? matched.get() : NullData.instance);
              joined.add(join);
              unjoinedRight.remove(matched.get());
            });
    unjoinedRight.forEach(
        r -> {
          Data join = joined(context.getDataTypeImplementation(), NullData.instance, r);
          joined.add(join);
        });
    return context.getDataTypeImplementation().arrayOf(joined);
  }

  private static Data joined(DataTypeImplementation dti, Data left, Data right) {
    return dti.arrayOf(ImmutableList.of(left, right));
  }
}
