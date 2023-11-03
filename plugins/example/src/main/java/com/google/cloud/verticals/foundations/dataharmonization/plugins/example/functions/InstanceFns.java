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

import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.io.ExampleService;
import java.io.Serializable;

/**
 * This class contains an example of a function that relies on an instance variable. This is mostly
 * useful for dependency injection. NOTE: Maintaining/modifying state between function calls is
 * strongly discouraged. Instance variables should only be used in a read-only capacity.
 *
 * <p>Also note that instance classes (and thus their members) must be serializable.
 */
public final class InstanceFns implements Serializable {
  private final ExampleService exampleService;

  public InstanceFns(ExampleService exampleService) {
    this.exampleService = exampleService;
  }

  /** An example function that calls an instance of the {@link ExampleService}. */
  @PluginFunction
  public Primitive exampleInstanceFn(RuntimeContext context, String userId) {
    String greeting = exampleService.getUserGreeting(userId);
    return context.getDataTypeImplementation().primitiveOf(greeting);
  }

  /**
   * An example static function that converts integer form of userId into String. Demonstrating that
   * you can also have static method in an instance function class as long as you import the
   * functions accordingly in {@link
   * com.google.cloud.verticals.foundations.dataharmonization.plugins.example.ExamplePlugin}.
   */
  @PluginFunction
  public Primitive convertUserId(RuntimeContext context, Integer intUserId) {
    return context.getDataTypeImplementation().primitiveOf(intUserId.toString());
  }
}
