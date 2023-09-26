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

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockData;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DataFns. */
@RunWith(JUnit4.class)
public class DataFnsTest {

  @Test
  public void test_fields_sorts() {
    RuntimeContext context = RuntimeContextUtil.testContext();
    Container container =
        MockData.mutableContainerOf(
            i -> {
              i.set("z", testDTI().primitiveOf(1.));
              i.set("a", testDTI().primitiveOf(2.));
              i.set("g", testDTI().primitiveOf(3.));
            });

    Array actual = DataFns.fields(context, container);
    assertDCAPEquals(
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf("a"),
                    testDTI().primitiveOf("g"),
                    testDTI().primitiveOf("z"))),
        actual);
  }

  @Test
  public void test_values_sortsByField() {
    RuntimeContext context = RuntimeContextUtil.testContext();
    Container container =
        MockData.mutableContainerOf(
            i -> {
              i.set("z", testDTI().primitiveOf(1.));
              i.set("a", testDTI().primitiveOf(2.));
              i.set("g", testDTI().primitiveOf(3.));
            });

    Array actual = DataFns.values(context, container);
    assertDCAPEquals(
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(1.))),
        actual);
  }
}
