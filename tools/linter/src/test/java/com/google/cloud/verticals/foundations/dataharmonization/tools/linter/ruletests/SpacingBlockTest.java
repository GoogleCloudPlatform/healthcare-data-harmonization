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
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingBlock;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpacingBlockTest {
  Function<WhistleParser, RuleContext> programParserRule = WhistleParser::program;
  String directory = "spacingBlockTest/";
  String input = "_input.wstl";
  String output = "_output.wstl";

  @Test
  public void spacingBlock_singleMethod_success() throws IOException {
    String testName = "singleMethod";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingBlock_multipleMethods_success() throws IOException {
    String testName = "multipleMethods";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingBlock_invalidWhistle_throwsError() throws IOException {
    String testName = "invalidWhistle";
    Linter linter = new Linter();
    assertThrows(
        "Errors occurred during transpilation",
        TranspilationException.class,
        () ->
            linter.lint(
                getWhistleText(directory + testName + input),
                programParserRule,
                FileInfo.getDefaultInstance(),
                Arrays.asList(new SpacingBlock())));
  }

  @Test
  public void spacingBlock_withComments_success() throws IOException {
    String testName = "withComments";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingBlock_multipleNewlines_success() throws IOException {
    String testName = "multipleNewlines";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }
}
