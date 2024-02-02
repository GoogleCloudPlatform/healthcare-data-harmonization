/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ruletests;

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.getWhistleText;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree.toCodeString;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.Linter;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingFunctionDef;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SpacingFunctionDefTest {
  private static final String DIRECTORY = "spacingFunctionDefTest/";
  private static final String INPUT = "_input.wstl";
  private static final String OUTPUT = "_output.wstl";

  @Test
  public void spacingFunctionDef_tooManySpaces_success() throws IOException {
    String testName = "tooManySpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(DIRECTORY + testName + INPUT),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingFunctionDef()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + OUTPUT));
  }

  @Test
  public void spacingFunctionDef_notEnoughSpaces_success() throws IOException {
    String testName = "notEnoughSpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(DIRECTORY + testName + INPUT),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingFunctionDef()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + OUTPUT));
  }

  @Test
  public void spacingFunctionDef_noSpaces_throwsError() {
    String testName = "noSpaces";
    Linter linter = new Linter();
    assertThrows(
        "Errors occurred during transpilation",
        TranspilationException.class,
        () ->
            linter.lint(
                getWhistleText(DIRECTORY + testName + INPUT),
                WhistleParser::program,
                FileInfo.getDefaultInstance(),
                Arrays.asList(new SpacingFunctionDef())));
  }
}
