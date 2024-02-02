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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.getWhistleText;
import static com.google.common.truth.Truth.assertThat;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Whistle Linter tool Unit tests. */
@RunWith(JUnit4.class)
public class LinterTest {
  String directory = "linterTest/";
  String input = "_input.wstl";
  String output = "_output.wstl";
  Function<WhistleParser, RuleContext> programParserRule = WhistleParser::program;

  public LinterTest() {}

  @Test
  public void allRules_mainTest_success() throws IOException {
    String testName = "mainTest";
    TestFileSystemShim fs = new TestFileSystemShim();
    // Not specifying the flags --include or --exclude defaults to all rules being implemented.
    String[] args = {"/" + directory + testName + input};
    Linter.fs = fs;
    Linter.main(args);
    Optional<String> resultFileName = fs.getTestOutput().toString().lines().findFirst();
    String resultCode = fs.getTestOutput().toString().lines().skip(1).collect(joining("\n"));
    assertThat(resultFileName.isPresent()).isTrue();
    assertThat(resultFileName.get()).isEqualTo("/" + directory + testName + input);
    assertThat(resultCode).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void nonexistentRule_throwsError() {
    String testName = "error";
    String[] args = {"/" + directory + testName + input, "-i=DoesNotExistRule"};
    assertThrows(
        "The linting rule DoesNotExistRule does not exist.",
        IOException.class,
        () -> Linter.main(args));
  }

  @Test
  public void noRules_success() throws IOException {
    String testName = "noRules";
    Linter linter = new Linter();
    BaseTreeNode result =
        linter.lint(
            getWhistleText(directory + testName + input),
            programParserRule,
            FileInfo.getDefaultInstance(),
            Arrays.asList());
    assertThat(AntlrToTree.toCodeString(result))
        .isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void includeSpacingBlock_success() throws IOException {
    String testName = "includeSpacingBlock";
    TestFileSystemShim fs = new TestFileSystemShim();
    // Not specifying the flags --include or --exclude defaults to all rules being implemented.
    String[] args = {"/" + directory + testName + input, "--include=SpacingBlock"};
    Linter.fs = fs;
    Linter.main(args);
    Optional<String> resultFileName = fs.getTestOutput().toString().lines().findFirst();
    String resultCode = fs.getTestOutput().toString().lines().skip(1).collect(joining("\n"));
    assertThat(resultFileName.isPresent()).isTrue();
    assertThat(resultFileName.get()).isEqualTo("/" + directory + testName + input);
    assertThat(resultCode).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void excludeBracketsRedundant_success() throws IOException {
    String testName = "excludeBracketsRedundant";
    TestFileSystemShim fs = new TestFileSystemShim();
    // Not specifying the flags --include or --exclude defaults to all rules being implemented.
    String[] args = {"/" + directory + testName + input, "--exclude=BracketsRedundant"};
    Linter.fs = fs;
    Linter.main(args);
    Optional<String> resultFileName = fs.getTestOutput().toString().lines().findFirst();
    String resultCode = fs.getTestOutput().toString().lines().skip(1).collect(joining("\n"));
    assertThat(resultFileName.isPresent()).isTrue();
    assertThat(resultFileName.get()).isEqualTo("/" + directory + testName + input);
    assertThat(resultCode).isEqualTo(getWhistleText(directory + testName + output));
  }

  @Test
  public void incorrectRuleOrder_throwsError() {
    String testName = "error";
    String[] args = {"/" + directory + testName + input, "-i=SpacingFunctionCall,SpacingBlock"};
    assertThrows(
        "The SpacingBlock rule must come before the SpacingFunctionCall rule.",
        IOException.class,
        () -> Linter.main(args));
  }

  @Test
  public void spacingFunctionCallWithoutSpacingBlock_throwsError() {
    String testName = "error";
    String[] args = {"/" + directory + testName + input, "-i=SpacingFunctionCall"};
    assertThrows(
        "The SpacingBlock rule must come before the SpacingFunctionCall rule.",
        IOException.class,
        () -> Linter.main(args));
  }

  @Test
  public void correctRuleOrder_success() throws IOException {
    String testName = "correctRuleOrder";
    TestFileSystemShim fs = new TestFileSystemShim();
    // Not specifying the flags --include or --exclude defaults to all rules being implemented.
    String[] args = {
      "/" + directory + testName + input, "--include=SpacingBlock,SpacingFunctionCall"
    };
    Linter.fs = fs;
    Linter.main(args);
    Optional<String> resultFileName = fs.getTestOutput().toString().lines().findFirst();
    String resultCode = fs.getTestOutput().toString().lines().skip(1).collect(joining("\n"));
    assertThat(resultFileName.isPresent()).isTrue();
    assertThat(resultFileName.get()).isEqualTo("/" + directory + testName + input);
    assertThat(resultCode).isEqualTo(getWhistleText(directory + testName + output));
  }
}
