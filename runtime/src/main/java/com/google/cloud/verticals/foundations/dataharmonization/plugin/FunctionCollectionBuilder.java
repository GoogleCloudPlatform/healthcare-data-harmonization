// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Provides more streamlined API and more standardized implementation for plugins to provide list of
 * functions to register.
 */
public class FunctionCollectionBuilder {

  private final String packageName;
  private ImmutableList.Builder<CallableFunction> funcListBuilder;

  /**
   * Creates a new collection for functions. All functions added to this collection will be
   * registered under package {@code packageName}.
   *
   * @param packageName The package name to register functions in the current
   *     FunctionCollectionBuilder.
   */
  public FunctionCollectionBuilder(String packageName) {
    this.packageName = packageName;
    this.funcListBuilder = ImmutableList.builder();
  }

  /**
   * Converts all public instance methods that are annotated with {@code @PluginFunction} in the
   * {@code instance} into {@link CallableFunction} that can be invoked by Whistle mapping engine.
   * Usually used for functions that require externally injected state controller or IO connectors.
   * The given instance is used as the instance on which those functions will be called. NOTE: This
   * instance's state may be reset to the state passed to this method (assuming this method is
   * called during the plugin loading stage) throughout the execution of the engine.
   *
   * @param instance on which the registered functions will be called.
   * @return a {@link FunctionCollectionBuilder} containing the registered functions.
   */
  @CanIgnoreReturnValue
  public FunctionCollectionBuilder addAllJavaPluginFunctionsInInstance(
      @Nonnull Serializable instance) {
    funcListBuilder =
        funcListBuilder.addAll(JavaFunction.ofPluginFunctionsInInstance(instance, packageName));
    return this;
  }

  /**
   * Converts all public static methods that are annotated with {@code @PluginFunction} in the
   * {@code clazz} into {@link CallableFunction} that can be invoked by Whistle mapping engine. For
   * the function to be successfully processed by this method, it has to satisfy the following
   * conditions:
   *
   * <ul>
   *   <li>Is public and static
   *   <li>
   *   <li>Is annotated with {@code @PluginFunction}
   * </ul>
   *
   * @param clazz The class to register functions from.
   * @return a {@link FunctionCollectionBuilder} containing the registered functions.
   */
  @CanIgnoreReturnValue
  public FunctionCollectionBuilder addAllJavaPluginFunctionsInClass(Class<?> clazz) {
    funcListBuilder =
        funcListBuilder.addAll(JavaFunction.ofPluginFunctionsInClass(clazz, packageName));
    return this;
  }

  /** Returns the currently collected {@link CallableFunction}s as a ImmutableListBuilder. */
  public ImmutableList.Builder<CallableFunction> toImmutableListBuilder() {
    return funcListBuilder;
  }

  /** Returns the currently collected {@link CallableFunction}s as a list. */
  public List<CallableFunction> build() {
    return funcListBuilder.build();
  }
}
