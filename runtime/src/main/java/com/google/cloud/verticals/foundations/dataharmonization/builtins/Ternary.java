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

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;

/** Ternary function builtin. */
public final class Ternary {
  private Ternary() {}

  @PluginFunction(inheritParentVars = true)
  public static Data ternary(RuntimeContext context, Data condition, Closure truePart) {
    return isTruthy(condition) ? truePart.execute(context) : NullData.instance;
  }

  @PluginFunction(inheritParentVars = true)
  public static Data ternary(
      RuntimeContext context, Data condition, Closure truePart, Closure falsePart) {
    return isTruthy(condition) ? truePart.execute(context) : falsePart.execute(context);
  }

  /**
   * Returns true iff the given Data is truthy. This means it is one of:
   *
   * <ul>
   *   <li>a true boolean
   *   <li>not a boolean and not null or empty
   * </ul>
   */
  public static boolean isTruthy(Data item) {
    boolean isNull = item == null || item.isNullOrEmpty();
    boolean isBoolean = !isNull && item.isPrimitive() && item.asPrimitive().bool() != null;
    boolean isTrueBoolean = isBoolean && item.asPrimitive().bool();
    return isTrueBoolean || (!isBoolean && !isNull);
  }
}
