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

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.Linter;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.BracketsRedundant;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingConditional;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpacingConditionalTest {
  Function<WhistleParser, RuleContext> programParserRule = WhistleParser::program;
  String directory = "spacingConditionalTest/";
  String input = "_input.wstl";
  String output = "_output.wstl";

  @Test
  public void spacingConditional_noSpaces_success() throws IOException {
    String testName = "noSpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingConditional()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingConditional_tooManySpaces_success() throws IOException {
    String testName = "tooManySpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingConditional()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingConditional_enoughSpaces_noChange() throws IOException {
    String testName = "enoughSpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingConditional()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + input));
  }

  @Test
  public void spacingConditional_elseTooManySpaces_success() throws IOException {
    String testName = "elseTooManySpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingConditional()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingConditional_elseIf_success() throws IOException {
    String testName = "elseIf";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingConditional()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingConditional_doubleSpacesRedundantBrackets_success() throws IOException {
    String testName = "doubleSpacesRedundantBrackets";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(
                new BracketsRedundant(), // remove redundant brackets before fixing the spacing
                new SpacingConditional()));
    System.out.println(toCodeString(result));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }
}
