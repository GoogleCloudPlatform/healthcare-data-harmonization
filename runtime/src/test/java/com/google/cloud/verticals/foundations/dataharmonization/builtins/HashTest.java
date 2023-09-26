/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static junit.framework.TestCase.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for hash builtin. */
@RunWith(Parameterized.class)
public class HashTest {
  @Parameter(0)
  public String testCaseName;

  @Parameter(1)
  public Data firstObj;

  @Parameter(2)
  public Data secondObj;

  @Parameter(3)
  public boolean wantEquals;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"string and num", testDTI().primitiveOf("1"), testDTI().primitiveOf(1.), false},
          {"string and char code", testDTI().primitiveOf("1"), testDTI().primitiveOf(49.), false},
          {"num and bool", testDTI().primitiveOf(1.), testDTI().primitiveOf(true), false},
          {"equal num", testDTI().primitiveOf(102.), testDTI().primitiveOf(102.), true},
          {
            "equal num with different decimal precision",
            testDTI().primitiveOf(102.01000),
            testDTI().primitiveOf(102.01),
            true
          },
          {"equal string", testDTI().primitiveOf("asd"), testDTI().primitiveOf("asd"), true},
          {"equal bool", testDTI().primitiveOf(false), testDTI().primitiveOf(false), true},
          {"string and bool", testDTI().primitiveOf(""), testDTI().primitiveOf(false), false},
          {"num and bool falsey", testDTI().primitiveOf(0.), testDTI().primitiveOf(false), false},
          {
            "container key order ignored",
            testDTI()
                .containerOf(
                    ImmutableSortedMap.of(
                        "a",
                        testDTI().primitiveOf(1.),
                        "b",
                        testDTI().primitiveOf(2.),
                        "c",
                        testDTI().primitiveOf(3.))),
            testDTI()
                .containerOf(
                    ImmutableSortedMap.of(
                        "b",
                        testDTI().primitiveOf(2.),
                        "a",
                        testDTI().primitiveOf(1.),
                        "c",
                        testDTI().primitiveOf(3.))),
            true
          },
          {
            "nested containers",
            testDTI()
                .containerOf(
                    ImmutableMap.of(
                        "a",
                        testDTI()
                            .containerOf(
                                ImmutableMap.of(
                                    "x",
                                    testDTI().primitiveOf("foo"),
                                    "y",
                                    testDTI()
                                        .containerOf(
                                            ImmutableMap.of(
                                                "z",
                                                testDTI().primitiveOf(77.),
                                                "j",
                                                testDTI().primitiveOf(99.))))),
                        "b",
                        testDTI().primitiveOf(2.),
                        "c",
                        testDTI().primitiveOf(3.))),
            testDTI()
                .containerOf(
                    ImmutableMap.of(
                        "b",
                        testDTI().primitiveOf(2.),
                        "a",
                        testDTI()
                            .containerOf(
                                ImmutableMap.of(
                                    "x",
                                    testDTI().primitiveOf("foo"),
                                    "y",
                                    testDTI()
                                        .containerOf(
                                            ImmutableMap.of(
                                                "z",
                                                testDTI().primitiveOf(77.),
                                                "j",
                                                testDTI().primitiveOf(99.))))),
                        "c",
                        testDTI().primitiveOf(3.))),
            true
          },
          {
            "equal arrays",
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(4.))),
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(4.))),
            true
          },
          {
            "non-equal arrays",
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(9.))),
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(4.))),
            false
          },
          {
            "varying array item order",
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(4.),
                        testDTI().primitiveOf(3.))),
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI().primitiveOf(1.),
                        testDTI().primitiveOf(2.),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(4.))),
            false
          },
          {
            "array of containers",
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI()
                            .containerOf(
                                ImmutableMap.of(
                                    "x",
                                    testDTI().primitiveOf(1.),
                                    "y",
                                    testDTI().primitiveOf(9.99))),
                        testDTI().containerOf(ImmutableMap.of("y", testDTI().primitiveOf(2.))),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(9.))),
            testDTI()
                .arrayOf(
                    ImmutableList.of(
                        testDTI()
                            .containerOf(
                                ImmutableMap.of(
                                    "y",
                                    testDTI().primitiveOf(9.99),
                                    "x",
                                    testDTI().primitiveOf(1.))),
                        testDTI().containerOf(ImmutableMap.of("y", testDTI().primitiveOf(2.))),
                        testDTI().primitiveOf(3.),
                        testDTI().primitiveOf(9.))),
            true
          },
          {"null and null", NullData.instance, NullData.instance, true},
          {"string and null", testDTI().primitiveOf("1"), NullData.instance, false},
          {"num and null", testDTI().primitiveOf(1.), NullData.instance, false},
          {"bool and null", testDTI().primitiveOf(true), NullData.instance, false}
        });
  }

  @Test
  public void test() {
    Primitive hashFirstObj = Core.intHash(testDTI(), firstObj);
    Primitive hashSecondObj = Core.intHash(testDTI(), secondObj);
    assertEquals(
        "testing hash for: " + testCaseName + " - failed with:",
        wantEquals,
        hashFirstObj.equals(hashSecondObj));
  }
}
