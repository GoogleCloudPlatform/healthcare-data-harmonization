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
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingArrays;
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
public class SpacingArraysTest {
  Function<WhistleParser, RuleContext> programParserRule = WhistleParser::program;
  String directory = "spacingArraysTest/";
  String input = "_input.wstl";
  String output = "_output.wstl";

  @Test
  public void spacingArrays_noNewlines_noChange() throws IOException {
    String testName = "noNewlines";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + input));
  }

  @Test
  public void spacingArrays_multipleNewlines_success() throws IOException {
    String testName = "multipleNewlines";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_singleMethod_success() throws IOException {
    String testName = "singleMethod";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_multipleMethods_success() throws IOException {
    String testName = "multipleMethods";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_alreadyIndented_noChange() throws IOException {
    String testName = "alreadyIndented";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + input));
  }

  @Test
  public void spacingArrays_tooManyIndents_success() throws IOException {
    String testName = "tooManyIndents";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_singleNewline_success() throws IOException {
    String testName = "singleNewline";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_bracketLastLine_success() throws IOException {
    String testName = "bracketLastLine";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void spacingArrays_extraSpaces_success() throws IOException {
    String testName = "extraSpaces";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingBlock(), new SpacingArrays()));
    assertThat(toCodeString(result)).isEqualTo(getWhistleText(directory + testName + output));
  }
}
