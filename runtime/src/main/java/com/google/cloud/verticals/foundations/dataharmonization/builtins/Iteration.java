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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.IntStream;

/** Builtin function for iteration. */
public final class Iteration {

  /**
   * Iteration stub for iterating over NullData, to disambiguate it from a dataset or an array. This
   * method always returns NullData and never calls the given closure.
   */
  @PluginFunction
  public static NullData iterate(Closure closure, NullData... iterables) {
    return NullData.instance;
  }

  /**
   * Iterate the given Array(s) together passing them one element at a time to the given closure,
   * and composing the results into a new array. The closure must therefore have the same number of
   * free arguments as there are arrays to iterate together.
   *
   * <p>If multiple arrays are iterated together, then zipped iteration is performed. This means
   * that for every index, an element is selected at that index from each iterated array (except
   * empty arrays, where null is selected for every index). These elements are passed in as
   * arguments to the function (in place of the arrays), and the results of the function are
   * composed to the returned new Array. All non-empty arrays must have the same sizes (empty Arrays
   * just yield as many nulls as necessary).
   *
   * <p>For example, if (a, b, c, x, y) are args, a and b are arrays of size 3, c is an array of
   * size 0 (empty) and x and y are non-iterated args (that is, they are not free args), then the
   * given function will be called like:
   *
   * <ul>
   *   <li>fn(a[0], b[0], null, x, y)
   *   <li>fn(a[1], b[1], null, x, y)
   *   <li>fn(a[2], b[2], null, x, y)
   * </ul>
   *
   * The results of these calls will be collected into an array of length 3.
   *
   * <p>This method may also be called directly, with an anonymous expression. For example: <code>
   * iterate($1 + $2, ["a", "b", "c"], ["A", "B", "C"])</code> will return <code>["aA", "bB", "cC"]
   * </code>. Alternatively, <code>iterate($ + 100, [1, 2, 3])</code> will return <code>
   * [101, 102, 103]</code>. As many arguments as desired may be passed in, and will be bound to $1,
   * $2, $3...$n in order. If there is only one array argument, it is bound to just $ (with no
   * number).
   *
   * @param context RuntimeContext provided by the runtime.
   * @param closure The closure to use for iteration.
   * @param iterables The arrays to iterate.
   */
  @PluginFunction
  public static Array iterate(RuntimeContext context, Closure closure, Array... iterables) {
    if (closure.getNumFreeParams() != iterables.length) {
      throw new IllegalArgumentException(
          String.format(
              "Iteration closure must have equal number of free parameters as there are arrays to"
                  + " iterate. Had %d free params but tried to iterate over %d arrays.",
              closure.getNumFreeParams(), iterables.length));
    }

    if (iterables.length == 0) {
      return NullData.instance;
    }

    int iterationSize = stream(iterables).map(Array::size).reduce(0, Iteration::getIterationSize);
    if (iterationSize == -1) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot iterate arrays of different non-zero sizes: %s",
              stream(iterables).map(Array::size).map(Object::toString).collect(joining(", "))));
    }

    if (iterationSize == 0) {
      return NullData.instance;
    }

    return context
        .getDataTypeImplementation()
        .arrayOf(
            IntStream.range(0, iterationSize)
                .mapToObj(i -> iterate(context, closure, iterables, i))
                .filter(d -> !d.isNullOrEmpty())
                .collect(toImmutableList()));
  }

  /**
   * Containers are iterated by passing in the values of each key. The resulting value is then
   * assigned to the corresponding key in the output container.
   *
   * <p>The main notable difference between arrays and containers is that there is no size
   * requirement for containers - since the iteration is done upon key-values, containers that are
   * missing the key just get a Null value.
   *
   * <p>For example, if (a, b, c, x, y) are args, a, b, and c are containers such that a = {k1: ...,
   * k3: ...} and b = {k1: ..., k2: ...} and c = {}, then the given function will be called like:
   *
   * <ul>
   *   <li>fn(a.k1, b.k1, c.k1, x, y)
   *   <li>fn(a.k2, b.k2, c.k2, x, y)
   *   <li>fn(a.k3, b.k3, c.k3, x, y)
   * </ul>
   *
   * where a.k2 == b.k3 == c.k1 == c.k2 == c.k3 == null
   *
   * <p>The results of these calls will be collected into a container as in:
   *
   * <pre><code>
   * {
   *    k1: fn(a.k1, b.k1, c.k1, x, y)
   *    k2: fn(a.k2, b.k2, c.k2, x, y)
   *    k3: fn(a.k3, b.k3, c.k3, x, y)
   * }
   * </code></pre>
   *
   * Example:
   *
   * <pre><code>
   * var c1: {
   *   k1: "c1k1"
   *   k2: "c1k2"
   * }
   *
   * def modify(value) value + "-modified"
   *
   * modify(c1[]) == {
   *   "k1": "c1k1-modified",
   *   "k2": "c1k2-modified"
   * }
   *
   * var c2: {
   *   k1: "c2k1"
   *   k3: "c2k3"
   * }
   *
   * var c3: {
   *   k1: "c3k1"
   *   k3: "c3k3"
   * }
   *
   * sum(c1[], c2[], c3[]) == {
   *   "k1": "c1k1c2k1c3k1",
   *   "k2": "c1k2",
   *   "k3": "c2k3c3k3"
   * }
   * </code></pre>
   *
   * @param context RuntimeContext provided by the runtime.
   * @param closure The closure to use for iteration.
   * @param iterables The containers to iterate.
   */
  @PluginFunction
  public static Container iterate(RuntimeContext context, Closure closure, Container... iterables) {
    Set<String> keySet = new HashSet<>();

    for (Container container : iterables) {
      keySet.addAll(container.fields());
    }

    return context
        .getDataTypeImplementation()
        .containerOf(
            keySet.stream()
                .map(key -> iterate(context, closure, key, iterables))
                .filter(k -> !k.getValue().isNullOrEmpty())
                .collect(toImmutableMap(Entry::getKey, Entry::getValue)));
  }

  /**
   * Iterates a dataset through the given closure. Each dataset element will be passed through the
   * given closure using the dataset's {@link Dataset#map(RuntimeContext, Closure, boolean)}
   * implementation.
   *
   * @param context RuntimeContext provided by the runtime.
   * @param closure The closure to use for iteration.
   * @param dataset The dataset to iterate/map.
   */
  @PluginFunction
  public static Dataset iterate(RuntimeContext context, Closure closure, Dataset dataset) {
    if (closure.getNumFreeParams() != 1) {
      throw new IllegalArgumentException(
          String.format(
              "Closure iterating over a dataset must have exactly 1 free parameter, but had %d.",
              closure.getNumFreeParams()));
    }
    return dataset.map(context, closure, false);
  }

  private static Data iterate(
      RuntimeContext context, Closure closure, Array[] iterables, int index) {
    for (Array iterable : iterables) {
      closure = closure.bindNextFreeParameter(iterable.getElement(index));
    }

    return closure.execute(context);
  }

  private static Entry<String, Data> iterate(
      RuntimeContext context, Closure closure, String key, Container... iterables) {
    for (Container container : iterables) {
      Data data = container.getField(key);
      closure = closure.bindNextFreeParameter(data);
    }
    return Map.entry(key, closure.execute(context));
  }

  private static int getIterationSize(int currentSize, int nextArraySize) {
    if (currentSize == 0) {
      return nextArraySize;
    }

    if (nextArraySize == 0) {
      return currentSize;
    }

    return currentSize == nextArraySize ? currentSize : -1;
  }

  private Iteration() {}
}
