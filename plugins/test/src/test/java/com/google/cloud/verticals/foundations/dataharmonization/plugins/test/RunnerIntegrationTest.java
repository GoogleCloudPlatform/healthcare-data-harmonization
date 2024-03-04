/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.IntegrationTest;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions.RunnerFns;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration test that runs on the test framework. */
@RunWith(JUnit4.class)
public class RunnerIntegrationTest {

  @Test
  public void test_mapping_report() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("codelab_test.wstl");
    Data actual =
        engine.transform(
            testDTI()
                .containerOf(ImmutableMap.of("returnReportAsData", testDTI().primitiveOf(true))));
    Data want = test.loadJson("codelab_test_report.json");

    AssertUtil.assertDCAPEquals(want, actual);
  }

  @Test
  public void test_mapping_test() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("codelab_test.wstl");
    String want = test.loadText("codelab_test_report.txt");

    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () ->
                engine.transform(
                    testDTI()
                        .containerOf(
                            ImmutableMap.of("returnReportAsData", testDTI().primitiveOf(false)))));

    assertThat(ex).hasCauseThat().isInstanceOf(VerifyException.class);
    assertThat(ex).hasMessageThat().contains(want);
  }

  @Test
  public void test_missingAssertion() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("bad_setup.wstl");

    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> engine.transform(testDTI().primitiveOf("missing_assert")));

    assertThat(ex).hasCauseThat().isInstanceOf(TestSetupException.class);
    assertThat(ex)
        .hasMessageThat()
        .contains(
            "Test function test_codelab_missingAssertion did not make any assertions. Make sure you"
                + " call one of the assertion functions at least once. Assertion functions are:"
                + " test::assertEquals, test::assertEquals, test::assertNull, test::assertTrue");
  }

  @Test
  public void test_extraParam() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("bad_setup.wstl");

    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> engine.transform(testDTI().primitiveOf("extra_param")));

    assertThat(ex).hasCauseThat().isInstanceOf(TestSetupException.class);
    assertThat(ex)
        .hasMessageThat()
        .contains("Test function test_codelab_extraParams(Data) cannot have parameters.");
  }

  @Test
  public void test_noFuncs() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("bad_setup.wstl");

    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> engine.transform(testDTI().primitiveOf("no_funcs")));

    assertThat(ex).hasCauseThat().isInstanceOf(TestSetupException.class);
    assertThat(ex)
        .hasMessageThat()
        .contains(
            "Could not find test functions in package no_funcs. Test function names must start with"
                + " test_ and take no parameters.");

    ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> engine.transform(testDTI().primitiveOf("not a real package")));

    assertThat(ex).hasCauseThat().isInstanceOf(TestSetupException.class);
    assertThat(ex)
        .hasMessageThat()
        .contains(
            "Could not find any functions at all in package not a real package. Are you missing an"
                + " import?");
  }

  @Test
  public void test_execPaths_nonExistantPath() throws IOException {
    Engine engine =
        new Engine.Builder(
                TestConfigExtractor.of(
                    ImportPath.of(ResourceLoader.TEST_LOADER, Path.of("/test.wstl"), Path.of("/")),
                    PipelineConfig.getDefaultInstance()))
            .withDefaultPlugins(new TestLoaderPlugin())
            .initialize()
            .build();

    IOException ex =
        assertThrows(
            IOException.class,
            () ->
                RunnerFns.execPaths(
                    engine.getRuntimeContext(),
                    "./non-existant.wstl",
                    testDTI().arrayOf(),
                    NullData.instance));
    assertThat(ex).hasMessageThat().contains("non-existant.wstl was not found");
  }

  @Test
  public void test_execPaths_yieldsError() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("exec.wstl");
    Container inputWrapper =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "input",
                    testDTI().containerOf(ImmutableMap.of("fail", testDTI().primitiveOf(true)))));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> engine.transform(inputWrapper));
    assertThat(ex).hasMessageThat().contains("You asked for this");
  }

  @Test
  public void test_execPaths_executesIt() throws IOException {
    IntegrationTest test = new IntegrationTest();
    Engine engine = test.initializeTestFile("exec.wstl");
    Data input = test.loadJson("codelab_input.json");
    Container inputWrapper = testDTI().containerOf(ImmutableMap.of("input", input));

    Data expected = test.loadJson("exec_want.json");
    Data result = engine.transform(inputWrapper);

    assertThat(result).isEqualTo(expected);
  }
}
