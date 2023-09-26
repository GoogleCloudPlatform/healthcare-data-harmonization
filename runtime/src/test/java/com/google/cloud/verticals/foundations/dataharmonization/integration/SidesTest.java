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
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Integration tests for side outputs. */
@RunWith(Parameterized.class)
public class SidesTest {
  private static final String SUBDIR = "side/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Parameter public String name;

  @Parameter(1)
  public String wstlFile;

  @Parameter(2)
  public String jsonFile;

  @Parameters(name = "{0}")
  public static ImmutableCollection<Object[]> tests() {
    return ImmutableList.of(
        new Object[] {"with sides in single file", "sides_single.wstl", "sides_single.json"},
        new Object[] {"with sides in multiple files", "sides_multi_1.wstl", "sides_multi.json"},
        new Object[] {"no sides in multiple files", "sides_root.wstl", "sides_root.json"},
        new Object[] {
          "with sides and without in a single file", "sides_mixed.wstl", "sides_single.json"
        },
        new Object[] {"nested withSides calls", "sides_nested.wstl", "sides_nested.json"});
  }

  @Test
  public void test() throws Exception {
    Engine engine = TESTER.initializeTestFile(wstlFile);
    Data actual = engine.transform(NullData.instance);

    Data expected = TESTER.loadJson(jsonFile);
    assertDCAPEquals(expected, actual);
  }
}
