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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.mocking.MockingTestPlugin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for imports. */
@RunWith(JUnit4.class)
public class ImportTest {
  private static final String SUBDIR = "init/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);

  @Test
  public void import_functionCall() throws Exception {
    Engine engine = TESTER.initializeTestFile("func_import.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = new DefaultPrimitive("hello world");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void import_stringInterp() throws Exception {
    Engine engine = TESTER.initializeTestFile("str_interp_import.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = new DefaultPrimitive("hello world");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void import_undefinedFunctionCall_throw() throws Exception {
    IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () -> TESTER.initializeTestFile("undefined_import.wstl"));
    assertThat(error)
        .hasMessageThat()
        .contains("undef() cannot be evaluated into a valid string import path");
    assertThat(error).hasMessageThat().contains("Unknown function undef()");
  }

  @Test
  public void repeated_import_throws_repeated_overload_exception() throws Exception {
    ImportException error =
        assertThrows(
            ImportException.class,
            () ->
                new IntegrationTestBase()
                    .initializeBuilder("/tests/init/repeated_import.wstl")
                    .withDefaultPlugins(new MockingTestPlugin())
                    .initialize()
                    .build());

    assertThat(error).hasCauseThat().isInstanceOf(IllegalArgumentException.class);
    assertThat(error)
        .hasMessageThat()
        .contains("Item with name 'mockFunc' already exists in package 'test'");
  }
}
