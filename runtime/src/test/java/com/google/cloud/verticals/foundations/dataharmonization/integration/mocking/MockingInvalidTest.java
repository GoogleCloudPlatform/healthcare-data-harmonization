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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for invalid cases in mocking. */
@RunWith(JUnit4.class)
public class MockingInvalidTest {
  private static final MockingTest TESTER = new MockingTest("mock_func_invalid/");

  @Test
  public void multipleMocksSatisfy_throwError() throws IOException {
    Engine engine = TESTER.initTestFileWithMock("invalid_multiple_mocks_satisfy");
    Exception e =
        Assert.assertThrows(
            WhistleRuntimeException.class, () -> engine.transform(NullData.instance));
    assertThat(e)
        .hasMessageThat()
        .contains(
            "Multiple mocks for function foo(Data, Data, Data) can be run with the given arguments."
                + " Consider adding selectors to the overloads so the current arguments can only"
                + " satisfy one selector.\n");
  }

  @Test
  public void selectorIncompatibleWithMock_throwErrorAtRuntime() throws IOException {
    Engine engine = TESTER.initTestFileWithMock("invalid_selector_incompatible");
    Exception e =
        Assert.assertThrows(
            WhistleRuntimeException.class, () -> engine.transform(NullData.instance));
    assertThat(e)
        .hasMessageThat()
        .contains(
            "Function incompatibleSelector(Data, Data) does not match given argument types"
                + " Primitive/DefaultPrimitive");
  }

  @Test
  public void invalidFuncRefInMockConfig_throwError() {
    Exception e =
        Assert.assertThrows(
            WhistleRuntimeException.class,
            () -> TESTER.initTestFileWithMock("invalid_function_ref"));
    assertThat(e).hasMessageThat().contains("Failed to parse function reference");
  }
}
