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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for blocks. This aims to test (end-to-end) mechanisms and semantics of block
 * expressions, and how they interact with other structures and language mechanics.
 */
@RunWith(JUnit4.class)
public class BlocksTest {
  private static final String SUBDIR = "block/";
  private static final IntegrationTest TEST = new IntegrationTest(SUBDIR);

  @Test
  public void blocks_inheritVars() throws IOException {
    // Test that vars are inherited by blocks and that writing to an outer var in an inner block
    // modifies it.
    // i.e. {
    //   var x: 1;
    //   innerBlock: {
    //      Here we ensure x is 1
    //      var x: x + 1;
    //   };
    //   Here we ensure x is now 2.
    // };
    // See blocks.wstl for the full structure of nested blocks being tested.
    Engine engine = TEST.initializeTestFile("blocks.wstl");
    Data actual = engine.transform(NullData.instance);

    Data expected =
        mutableContainerOf(
            c -> {
              c.set("start_value_of_v", testDTI().primitiveOf(1.));
              c.set(
                  "block1",
                  mutableContainerOf(
                      b1 -> {
                        b1.set("start_value_of_v", testDTI().primitiveOf(1.));
                        b1.set(
                            "block2",
                            mutableContainerOf(
                                b2 -> {
                                  b2.set("start_value_of_v", testDTI().primitiveOf(2.));
                                  b2.set("end_value_of_v", testDTI().primitiveOf(3.));
                                }));
                        b1.set("end_value_of_v", testDTI().primitiveOf(3.));
                      }));
              c.set("end_value_of_v", testDTI().primitiveOf(3.));
            });
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void blocks_withThis() throws IOException {
    Engine engine = TEST.initializeTestFile("blocks_with_this.wstl");
    Data actual = engine.transform(NullData.instance);

    Data expected = mutableContainerOf(c -> c.set("output", testDTI().primitiveOf(30.)));
    assertDCAPEquals(expected, actual);
  }
}
