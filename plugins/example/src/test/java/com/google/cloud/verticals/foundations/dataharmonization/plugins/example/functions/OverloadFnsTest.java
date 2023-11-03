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

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockData;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for InstanceFns (i.e. the functions within). */
@RunWith(JUnit4.class)
public class OverloadFnsTest {
  @Test
  public void exampleOverloadFn_container_returnsField() {
    Data expected = mock(Data.class);

    // Use the MockData utility class to mock a container where any field returns the given item.
    Container test = MockData.containerOf(expected);

    Data actual = OverloadFns.exampleOverloadFn(test, "some field");

    // Note the use of assertThat instead of assertDCAPEquals; Since expected does not have any
    // actual content, assertDCAPEquals is not useful. It would be if expected had an actual example
    // value.
    assertThat(expected).isSameInstanceAs(actual);
  }

  @Test
  public void exampleOverloadFn_array_returnsIndex() {
    Data expected = mock(Data.class);

    // Use the MockData utility class to mock an array where any index returns the given item.
    Array test = MockData.arrayOf(expected, 2000);

    Data actual = OverloadFns.exampleOverloadFn(test, 1337.);

    // Note the use of assertThat instead of assertDCAPEquals; Since expected does not have any
    // actual content, assertDCAPEquals is not useful. It would be if expected had an actual example
    // value.
    assertThat(expected).isSameInstanceAs(actual);
  }

  @Test
  public void exampleOverloadFn_strings_returnsConcatenation() {
    // Now we are interested in the primitive value returned, so we use testDTI.
    Data expected = testDTI().primitiveOf("helloworld");
    // Note the use of utility methods to mock runtime context
    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithRegistry();

    Data actual = OverloadFns.exampleOverloadFn(context, "hello", "world");

    // Now that we are checking the contents, we use assertDCAPEquals.
    assertDCAPEquals(expected, actual);
  }
}
