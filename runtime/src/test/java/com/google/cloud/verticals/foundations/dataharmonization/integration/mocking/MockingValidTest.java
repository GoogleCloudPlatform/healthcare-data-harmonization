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

package com.google.cloud.verticals.foundations.dataharmonization.integration.mocking;

import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Integration test for mocking functionality in ELP core. */
@RunWith(Parameterized.class)
public class MockingValidTest {
  private static final MockingTest TESTER = new MockingTest("mock_func_valid/");

  @Parameter public String name;

  @Parameter(1)
  public String wstlName;

  @Parameter(2)
  public String expectedOutputFilename;

  @Parameters(name = "{0}")
  public static ImmutableCollection<Object[]> tests() {
    return ImmutableList.of(
        new Object[] {"mock basic test", "basic", "basic.json"},
        new Object[] {"mock calls original function", "call_itself", "call_itself.json"},
        new Object[] {"mock with selector", "with_selector", "with_selector.json"},
        new Object[] {"mock function overload", "mock_overload", "mock_overload.json"},
        new Object[] {
          "mock out native Java function", "mock_JavaFunc", "mock_JavaFunc.json"
        },
        new Object[] {
          "mock with Java function from plugins",
          "mock_with_java_func",
          "mock_with_java_func.json"
        },
        new Object[] {
          "no compatible mock - run original",
          "no_compatible_mock",
          "no_compatible_mock.json"
        },
        new Object[]{
            "mock function calls original - reroute to mock when argument is different",
            "call_itself_with_args",
            "call_itself_with_args.json"
        },
        new Object[]{
            "mock java function that takes closure as arguments",
            "mock_JavaFuncTakesClosureArgs",
            "mock_JavaFuncTakesClosureArgs.json"
        });
  }

  @Test
  public void test() throws Exception {
    Engine engine = TESTER.initTestFileWithMock(wstlName);
    Data actual = engine.transform(NullData.instance);

    Data expected = TESTER.loadJson(expectedOutputFilename);
    try {
      AssertUtil.assertDCAPEquals(expected, actual);
    } catch (AssertionError e) {
      assertEquals(
          String.format("%s. Context: ", e.getLocalizedMessage()),
          JsonSerializerDeserializer.dataToPrettyJson(expected),
          JsonSerializerDeserializer.dataToPrettyJson(actual));
    }
  }

  @Test
  public void multiple_mockConfigs() throws IOException {
    ImportPath additionalMockConfig =
        ImportPath.of(
            ResourceLoader.TEST_LOADER,
            FileSystems.getDefault()
                .getPath(TESTER.getTestDir() + "multiple_mockconfig.mockconfig1.wstl"),
            FileSystems.getDefault().getPath(MockingTest.TEST_DIR));
    Engine engine =
        TESTER.initBuilderWithMock("multiple_mockconfig")
            .addMock(ExternalConfigExtractor.of(additionalMockConfig))
            .initialize()
            .build();
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("multiple_mockconfig.json");

    AssertUtil.assertDCAPEquals(expected, actual);
  }
}
