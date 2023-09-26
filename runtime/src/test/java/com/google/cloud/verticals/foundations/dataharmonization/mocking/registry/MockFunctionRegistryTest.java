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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.registry;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.MockFunction;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link MockFunctionRegistry}. */
@RunWith(JUnit4.class)
public class MockFunctionRegistryTest {

  private static final String ORIG_PKG_NAME = "origPkg";
  private static final String ORIG_FUNC_NAME = "origFunc";
  private static final String MOCK_PKG_NAME = "mockPkg";
  private static final String MOCK_FUNC_NAME = "mockFunc";
  private static final Mock MOCK_REF =
      new Mock(new FunctionReference(MOCK_PKG_NAME, MOCK_FUNC_NAME), null);
  private static final String MOCK_PKG_NAME1 = "mockPkg1";
  private static final String MOCK_FUNC_NAME1 = "mockFunc1";
  private static final Mock MOCK_REF1 =
      new Mock(new FunctionReference(MOCK_PKG_NAME1, MOCK_FUNC_NAME1), null);
  private Map<FunctionReference, List<Mock>> originalToMock;

  @Before
  public void setUp() {
    originalToMock = new HashMap<>();
    originalToMock.put(
        new FunctionReference(ORIG_PKG_NAME, ORIG_FUNC_NAME),
        ImmutableList.of(MOCK_REF, MOCK_REF1));
  }

  @Test
  public void registerFunctionNotInMap_registerOriginal() {
    MockFunctionRegistry reg = new MockFunctionRegistry(this.originalToMock);
    String packageName = "randomPackage";
    CallableFunction funcToRegister =
        new TestCallableFunction(new Signature(packageName, "foo", ImmutableList.of(), false));
    reg.register(packageName, funcToRegister);
    assertThat(reg.getOverloads(ImmutableSet.of("randomPackage"), "foo")).contains(funcToRegister);
  }

  @Test
  public void registerFunctionInMapNoMock_registerOriginal() {
    MockFunctionRegistry reg = new MockFunctionRegistry(this.originalToMock);
    CallableFunction incompatibleMock =
        new TestCallableFunction(
            new Signature(MOCK_PKG_NAME, MOCK_FUNC_NAME, ImmutableList.of(Primitive.class), false));
    CallableFunction funcToRegister =
        new TestCallableFunction(
            new Signature(ORIG_PKG_NAME, ORIG_FUNC_NAME, ImmutableList.of(), false));

    reg.register(MOCK_PKG_NAME, incompatibleMock);
    reg.register(ORIG_PKG_NAME, funcToRegister);

    assertThat(reg.getOverloads(ImmutableSet.of(MOCK_PKG_NAME), MOCK_FUNC_NAME))
        .contains(incompatibleMock);
    assertThat(reg.getOverloads(ImmutableSet.of(ORIG_PKG_NAME), ORIG_FUNC_NAME))
        .contains(funcToRegister);
  }

  @Test
  public void registerFunctionInMap_registerWrapperWithMatchedMock() {
    MockFunctionRegistry reg = new MockFunctionRegistry(this.originalToMock);
    CallableFunction compatibleMock =
        new TestCallableFunction(
            new Signature(
                MOCK_PKG_NAME, MOCK_FUNC_NAME, ImmutableList.of(Data.class, Data.class), false));
    CallableFunction incompatibleMock =
        new TestCallableFunction(
            new Signature(
                MOCK_PKG_NAME, MOCK_FUNC_NAME1, ImmutableList.of(Primitive.class), false));
    CallableFunction original =
        new TestCallableFunction(
            new Signature(
                ORIG_PKG_NAME,
                ORIG_FUNC_NAME,
                ImmutableList.of(Primitive.class, Container.class),
                false));

    reg.register(MOCK_PKG_NAME, compatibleMock);
    reg.register(MOCK_PKG_NAME1, incompatibleMock);
    reg.register(ORIG_PKG_NAME, original);

    MockFunction expected = new MockFunction(original, ImmutableList.of(MOCK_REF));

    assertThat(compatibleMock.getSignature().getInheritsParentVars()).isFalse();
    assertThat(reg.getOverloads(ImmutableSet.of(MOCK_PKG_NAME), MOCK_FUNC_NAME))
        .contains(compatibleMock);
    assertThat(reg.getOverloads(ImmutableSet.of(MOCK_PKG_NAME1), MOCK_FUNC_NAME1))
        .contains(incompatibleMock);
    assertThat(reg.getOverloads(ImmutableSet.of(ORIG_PKG_NAME), ORIG_FUNC_NAME)).contains(expected);
  }
}
