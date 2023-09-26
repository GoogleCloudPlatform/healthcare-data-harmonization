/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mockWithClass;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.TestWrapperData;
import java.io.Serializable;
import java.lang.reflect.Method;
import org.junit.Test;
import org.mockito.Answers;

/** Base class to test for Java Function scenarios that should throw errors. */
public abstract class JavaInvalidFunctionTestBase {

  abstract JavaFunction constructJavaFunction(Method javaFunction, Serializable instance);

  // TODO(): Add concrete exception types for all these.
  @Test
  public void new_invalidReturnType_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> constructJavaFunction(TestMethods.get("invalidReturnType"), new TestMethods(null)));
  }

  @Test
  public void new_invalidArgType_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> constructJavaFunction(TestMethods.get("invalidArgType"), new TestMethods(null)));
  }

  @Test
  public void new_instanceMethodWithNoInstance_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () -> constructJavaFunction(TestMethods.get("noArgs"), null));
  }

  @Test
  public void new_invalidRuntimeContextPosition_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidRuntimeContextPosition"), new TestMethods(null)));
  }

  @Test
  public void new_invalidPrivateProtection_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidPrivateProtection"), new TestMethods(null)));
  }

  @Test
  public void new_invalidProtectedProtection_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidProtectedProtection"), new TestMethods(null)));
  }

  @Test
  public void new_invalidPackagePrivateProtection_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidPackageProtectedProtection"), new TestMethods(null)));
  }

  @Test
  public void new_multipleRuntimeContexts_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidMultipleRuntimeContexts"), new TestMethods(null)));
  }

  @Test
  public void new_variadicRuntimeContext_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            constructJavaFunction(
                TestMethods.get("invalidVariadicRuntimeContexts"), new TestMethods(null)));
  }

  @Test
  public void call_missingParameter_throws() {
    JavaFunction fn = constructJavaFunction(TestMethods.get("oneArg"), new TestMethods(null));

    assertThrows(IllegalArgumentException.class, () -> fn.call(new TestContext()));
  }

  @Test
  public void call_invalidParameterType_throws() {
    JavaFunction fn = constructJavaFunction(TestMethods.get("oneArg"), new TestMethods(null));

    assertThrows(
        IllegalArgumentException.class,
        () -> fn.call(new TestContext(), mockWithClass(Primitive.class)));
  }

  @Test
  public void call_notSpecificEnoughParameterType_throws() {
    JavaFunction fn =
        constructJavaFunction(TestMethods.get("specificImplArg"), new TestMethods(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> fn.call(new TestContext(), mockWithClass(Container.class)));
  }

  @Test
  public void call_tooManyArgs_throws() {
    JavaFunction fn = constructJavaFunction(TestMethods.get("noArgs"), new TestMethods(null));

    assertThrows(
        IllegalArgumentException.class, () -> fn.call(new TestContext(), mock(Primitive.class)));
  }

  @Test
  public void call_nonPrimitiveTypeForPrimitiveArg_throws() {
    JavaFunction fn =
        constructJavaFunction(TestMethods.get("primitiveArgs"), new TestMethods(null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            fn.call(
                new TestContext(),
                mock(Container.class, Answers.CALLS_REAL_METHODS),
                mock(Container.class, Answers.CALLS_REAL_METHODS),
                mock(Container.class, Answers.CALLS_REAL_METHODS)));
  }

  @Test
  public void call_wrongPrimitiveType_throws() {
    JavaFunction fn =
        constructJavaFunction(TestMethods.get("primitiveArgs"), new TestMethods(null));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            fn.call(
                new TestContext(),
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf(1.0)));
  }

  @Test
  public void call_wrongDataTypeWithWrapper_throws() {
    JavaFunction fn = constructJavaFunction(TestMethods.get("oneArg"), new TestMethods(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> fn.call(new TestContext(), new TestWrapperData(testDTI().primitiveOf(1.0))));
  }
}
