/*
 * Copyright 2023 Google LLC.
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

import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class VariadicTest {

  @Test
  public void variadic_null() throws Exception {
    IntegrationTest tester = new IntegrationTest("variadic/");
    Engine engine = tester.initializeTestFile("variadic_null.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = tester.loadJson("variadic_null.json");

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void variadic_emptyArray() throws Exception {
    IntegrationTest tester = new IntegrationTest("variadic/");
    Engine engine = tester.initializeTestFile("variadic_empty_array.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = tester.loadJson("variadic_empty_array.json");

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void variadic_emptyString() throws Exception {
    IntegrationTest tester = new IntegrationTest("variadic/");
    Engine engine = tester.initializeTestFile("variadic_empty_str.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = tester.loadJson("variadic_empty_str.json");

    assertDCAPEquals(expected, actual);
  }
}
