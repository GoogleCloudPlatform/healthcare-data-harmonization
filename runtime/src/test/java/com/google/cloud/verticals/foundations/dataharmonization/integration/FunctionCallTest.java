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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for function calls. */
@RunWith(JUnit4.class)
public class FunctionCallTest {

  private static final String SUBDIR = "functioncall/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Test
  public void wildcardPackage_matchFunction() throws Exception {
    Engine engine = TESTER.initializeTestFile("wildcard.wstl");
    Data actual = engine.transform(NullData.instance);
    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "outputA", testDTI().primitiveOf(3.0),
                    "outputB", testDTI().primitiveOf(11.0)));
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void functionAsTarget_callsFunction() throws Exception {
    Engine engine = TESTER.initializeTestFile("func_as_target.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("func_as_target.json");
    assertDCAPEquals(expected, actual);
  }
}
