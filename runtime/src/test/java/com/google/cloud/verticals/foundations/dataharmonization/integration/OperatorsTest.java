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
public class OperatorsTest {
  private static final String SUBDIR = "operators/";
  private static final IntegrationTest TEST = new IntegrationTest(SUBDIR);

  @Test
  public void operators_heterogeneousEq() throws IOException {
    Engine engine = TEST.initializeTestFile("eq_heterogenous.wstl");
    Data actual = engine.transform(NullData.instance);

    Data expected = TEST.loadJson("eq_heterogenous.json");
    assertDCAPEquals(expected, actual);
  }
}
