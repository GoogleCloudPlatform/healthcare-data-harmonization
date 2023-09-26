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

import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import java.util.Collection;
import java.util.stream.Collectors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Test for filters. */
@RunWith(Parameterized.class)
public class SelectorsTest {
  private static final String SUBDIR = "selectors/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Parameter(0)
  public String prefix;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asParams("where_containers", "sortby", "reduce", "last", "groupby");
  }

  private static Collection<Object[]> asParams(String... prefixes) {
    return stream(prefixes).map(p -> new Object[] {p}).collect(Collectors.toList());
  }

  @Test
  public void selector() throws Exception {
    Engine engine = TESTER.initializeTestFile(prefix + ".wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson(prefix + ".json");

    assertDCAPEquals(expected, actual);
  }
}
