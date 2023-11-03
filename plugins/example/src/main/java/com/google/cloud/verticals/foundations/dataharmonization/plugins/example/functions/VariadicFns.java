/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.functions;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;

/** Contains examples of variadic functions. */
public final class VariadicFns {
  private VariadicFns() {}

  /**
   * An example of a variadic function. Returns the first given container if any are given,
   * otherwise null. This function can either be called with a number of containers as explicit
   * params, e.g. <code>exampleVariadicFn(a, b, c)</code> or with an array of them, e.g. <code>
   * exampleVariadicFn([a, b, c])</code>.
   *
   * <p>NOTE: Any supported type can be a variadic parameter, but beware of {@link Array} and its
   * subclasses. A variadic {@link Array} parameter will result in unpredictable behaviour and
   * should be avoided.
   */
  @PluginFunction
  public static Container exampleVariadicFn(Container... containers) {
    return containers.length > 0 ? containers[0] : NullData.instance;
  }
}
