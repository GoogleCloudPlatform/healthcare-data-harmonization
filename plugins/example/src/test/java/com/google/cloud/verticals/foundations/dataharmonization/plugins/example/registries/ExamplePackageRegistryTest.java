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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries.functions.EchoFunction;
import com.google.cloud.verticals.foundations.dataharmonization.registry.util.LevenshteinDistance;
import com.google.common.collect.ImmutableSet;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for ExamplePackageRegistry */
@RunWith(JUnit4.class)
public class ExamplePackageRegistryTest {

  // Functions which we will register in some of our tests. See below for case where a JavaFunction
  // is registered.
  private final CallableFunction reg1 = new EchoFunction("reg1");
  private final CallableFunction reg2 = new EchoFunction("reg2");

  // Test return of an echo function which is the custom behaviour for this package registry
  @Test
  public void examplePackageRegistry_getOverloads_returnsEcho() {
    ExamplePackageRegistry examplePackageRegistry = new ExamplePackageRegistry();
    Set<CallableFunction> res =
        examplePackageRegistry.getOverloads(ImmutableSet.of("ExampleTest"), "testFxn_echo");
    assertThat(res).hasSize(1);
    for (CallableFunction fxn : res) {
      assertThat(fxn).isInstanceOf(EchoFunction.class);
      assertThat(fxn.getSignature().getName()).isEqualTo("testFxn_echo");
    }
  }

  // Test for case when no matches are found, and no echo function is created
  @Test
  public void examplePackageRegistry_getOverloads_returnsNone() {
    ExamplePackageRegistry examplePackageRegistry = new ExamplePackageRegistry();

    examplePackageRegistry.register("ExampleTest", reg1);
    examplePackageRegistry.register("ExampleTest", reg2);

    // Doesn't satisfy reqs for an echoFunction to be created, since the function doesn't end with
    // _echo
    Set<CallableFunction> res =
        examplePackageRegistry.getOverloads(ImmutableSet.of("ExampleTest"), "regNotPresent");
    assertThat(res).isEmpty();
  }

  // Test for case when the default backing package registry is used to return registered functions
  @Test
  public void examplePackageRegistry_getOverloads_returnRegisteredFunction() {

    ExamplePackageRegistry examplePackageRegistry = new ExamplePackageRegistry();

    examplePackageRegistry.register("ExampleTest", reg1);
    examplePackageRegistry.register("ExampleTest", reg2);

    Set<CallableFunction> res =
        examplePackageRegistry.getOverloads(ImmutableSet.of("ExampleTest"), "reg1");

    assertThat(res).containsExactly(reg1);
  }

  // A method to be registered, and ends with _echo
  public static Data methodToRegister_echo() {
    return NullData.instance;
  }

  // Test for returning a registered function that ends with _echo
  @Test
  public void examplePackageRegistry_getOverloads_returnRegisteredFunctionEndsWithUnderScore()
      throws NoSuchMethodException {
    ExamplePackageRegistry examplePackageRegistry = new ExamplePackageRegistry();

    // Create a JavaFunction type to register, named as an EchoFunction may be named.
    Method method = this.getClass().getDeclaredMethod("methodToRegister_echo", (Class<?>[]) null);
    JavaFunction javaFunction = new JavaFunction(method, null);
    examplePackageRegistry.register("ExampleTest", javaFunction);

    Set<CallableFunction> res =
        examplePackageRegistry.getOverloads(
            ImmutableSet.of("ExampleTest"), "methodToRegister_echo");

    // Assert the only function returned is an instance of JavaFunction, and named as expected.
    assertThat(res).hasSize(1);
    for (CallableFunction fxn : res) {
      assertThat(fxn).isInstanceOf(JavaFunction.class);
      assertThat(fxn.getSignature().getName()).isEqualTo("methodToRegister_echo");
    }
  }

  // Test for returning a bestMatchOverLoad
  @Test
  public void examplePackageRegistry_getBestMatchOverload() {
    ExamplePackageRegistry examplePackageRegistry = new ExamplePackageRegistry();

    // Register two functions to the registry
    examplePackageRegistry.register("example", reg1);
    examplePackageRegistry.register("example", reg2);

    // Showing how a user typo would trigger bestMatchOverloads to return suggested functions
    Map<String, Set<CallableFunction>> suggested =
        examplePackageRegistry.getBestMatchOverloads(
            ImmutableSet.of("example"), "reg", new LevenshteinDistance(2));

    // Set of expected suggested functions names to be returned.
    ImmutableSet<String> expectedFunctionNames = ImmutableSet.of("reg_echo", "reg1", "reg2");
    for (Entry<String, Set<CallableFunction>> r : suggested.entrySet()) {
      assertThat(r.getKey()).isEqualTo("example");
      Set<CallableFunction> functions = r.getValue();
      assertThat(functions).hasSize(3);

      // Check that all functions are echoFunctions and their names are in the expected suggested
      // function names
      for (CallableFunction fxn : functions) {
        assertThat(fxn).isInstanceOf(EchoFunction.class);
        assertThat(expectedFunctionNames).contains(fxn.getSignature().getName());
      }
    }
  }
}
