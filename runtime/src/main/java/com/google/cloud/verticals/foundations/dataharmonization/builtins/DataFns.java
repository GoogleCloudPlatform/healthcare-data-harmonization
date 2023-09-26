/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;

/** Functions for loading data. */
public final class DataFns {
  private DataFns() {}

  /**
   * Returns an array of the fields in the given container. The fields are sorted alphabetically.
   */
  @PluginFunction
  public static Array fields(RuntimeContext context, Container container) {
    return context
        .getDataTypeImplementation()
        .arrayOf(
            container.fields().stream()
                .sorted()
                .map(context.getDataTypeImplementation()::primitiveOf)
                .collect(toImmutableList()));
  }

  /**
   * Returns an array of the values in the given container. The order of values corresponds to the
   * fields being sorted alphabetically.
   */
  @PluginFunction
  public static Array values(RuntimeContext context, Container container) {
    return context
        .getDataTypeImplementation()
        .arrayOf(
            container.fields().stream()
                .sorted()
                .map(container::getField)
                .collect(toImmutableList()));
  }
}
