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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for join. */
@RunWith(JUnit4.class)
public class JoinTest {
  private static final String SUBDIR = "join/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Test
  public void join_basic() throws Exception {
    Engine engine = TESTER.initializeTestFile("basic.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("basic.json");
    assertDCAPEquals(expected, actual);
  }
}
