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
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.VarTarget.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Integration tests for the set builtin {@link Constructor}. This aims to test (end-to-end)
 * mechanisms and semantics of using set expressions, and how they interact with other structures
 * and language mechanics for creating {@link Container} and {@link Array} entries.
 */
@RunWith(JUnit4.class)
public class SetTest {
  private static final String SUBDIR = "set/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Test
  public void set_simpleVariables() throws IOException {
    /*
    Test that sets of variable Container keys can be made with simple variable references, with
    multiple entries per Container, and at multiple levels of nesting.
    i.e. {
      var key: "one";
      singleEntryContainer: {
         set(key): someValue;
      }
      ...
    };
    See set.wstl for the full structure of sets being tested.
     */
    Engine engine = TESTER.initializeTestFile("set.wstl");
    Data actual = engine.transform(NullData.instance);

    Data expected =
        mutableContainerOf(
            c -> {
              c.set(
                  "owContainer", mutableContainerOf(c1 -> c1.set("b", testDTI().primitiveOf(2.))));
              c.set(
                  "singleEntryContainer",
                  mutableContainerOf(c1 -> c1.set("one", testDTI().primitiveOf("oneValue"))));
              c.set(
                  "multiEntryContainer",
                  mutableContainerOf(
                      c2 -> {
                        c2.set("one", testDTI().primitiveOf("oneValue"));
                        c2.set("two", testDTI().primitiveOf("twoValue"));
                      }));
              c.set(
                  "nestedEntryContainer",
                  mutableContainerOf(
                      c3 -> {
                        c3.set(
                            "one",
                            mutableContainerOf(
                                c3inner ->
                                    c3inner.set(
                                        "innerOne",
                                        mutableContainerOf(
                                            c3innerinner ->
                                                c3innerinner.set(
                                                    "innerInnerOne",
                                                    testDTI()
                                                        .primitiveOf("innerInnerOneValue"))))));
                        c3.set("two", testDTI().primitiveOf("twoValue"));
                      }));
            });
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void set_pathVariables() throws IOException {
    /*
    Test that sets of variable Container keys and Array indices can be made with Path variables.
    i.e. {
      var obj: {
        innerObj: {
          key: "someKey";
          anotherKey: 0;
        }
      }
      pathKeyedContainer: {
        set(obj.innerObj.key): someValue;
      }
      set("pathIndexedArray[{obj.innerObj.anotherKey}]"): someValue;
      }
      ...
    };
    See set_with_paths.wstl for the full structure of sets being tested.
     */
    Engine engine = TESTER.initializeTestFile("set_with_paths.wstl");
    Data actual = engine.transform(NullData.instance);

    Data expected =
        mutableContainerOf(
            c -> {
              c.set(
                  "pathKeyedContainer",
                  mutableContainerOf(
                      c1 -> {
                        c1.set("one", testDTI().primitiveOf("oneValue"));
                        c1.set("two", testDTI().primitiveOf("twoValue"));
                        c1.set("three", testDTI().primitiveOf("threeValue"));
                      }));
              c.set(
                  "pathKeyedArray",
                  mutableArrayOf(
                      NullData.instance,
                      testDTI().primitiveOf("second"),
                      testDTI().primitiveOf("third"),
                      testDTI().primitiveOf("fourth")));
            });
    assertDCAPEquals(expected, actual);
  }
}
