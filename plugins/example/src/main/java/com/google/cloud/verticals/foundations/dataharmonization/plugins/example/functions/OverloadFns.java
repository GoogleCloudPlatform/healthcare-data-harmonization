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
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;

/**
 * This class contains examples of overloaded functions. These are functions with the same name but
 * different argument types. The runtime will automatically select the right one based on the
 * argument types passed in.
 */
public final class OverloadFns {
  private OverloadFns() {}

  /**
   * An example function with multiple overloads. This overload returns the given field on the given
   * container. This overload is selected if the user calls <code>exampleOverloadFn</code> with a
   * container argument.
   *
   * @param field The field on the container to get. NOTE: String (also Boolean and Double) is
   *     extracted automatically from {@link Primitive}. This means that for overload selection
   *     purposes <code>field</code> is a Primitive (so two overloads where the only difference is
   *     one arg is Double and one arg is String are considered ambiguous).
   */
  @PluginFunction
  public static Data exampleOverloadFn(Container container, String field) {
    return container.getField(field);
  }

  /**
   * An example function with multiple overloads. This overload returns the given index on the given
   * array. This overload is selected if the user calls <code>exampleOverloadFn</code> with an array
   * argument.
   *
   * @param index The index on the array to get. NOTE: Double (also Boolean and String) is extracted
   *     automatically from {@link Primitive}. This means that for overload selection purposes
   *     <code>index</code> is a Primitive (so two overloads where the only difference is one arg is
   *     Double and one arg is String are considered ambiguous).
   */
  @PluginFunction
  public static Data exampleOverloadFn(Array array, Double index) {
    return array.getElement((int) Math.round(index));
  }
  /**
   * An example function with multiple overloads. This overload returns the concatenation of the two
   * given strings. This overload is selected if the user calls <code>exampleOverloadFn</code> with
   * two Primitive arguments (NOTE: String (also Boolean and Double) is extracted automatically from
   * {@link Primitive}, so this overload will be selected from two Doubles as well, but both
   * parameters will be null).
   *
   * @param context an optional parameter providing various useful engine APIs. In this case it is
   *     used to create a new Primitive. RuntimeContext is automatically injected by the runtime
   *     (i.e. user does not add it when calling your function), but must be the first parameter if
   *     it is present.
   * @return the concatenated strings. NOTE: A function can return either Data (like the overloads
   *     above) or any subclass or subinterface of Data.
   */
  @PluginFunction
  public static Primitive exampleOverloadFn(RuntimeContext context, String prefix, String suffix) {
    return context.getDataTypeImplementation().primitiveOf(prefix + suffix);
  }
}
