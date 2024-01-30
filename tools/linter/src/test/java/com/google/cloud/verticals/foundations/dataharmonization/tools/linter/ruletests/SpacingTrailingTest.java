// Copyright 2023 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ruletests;

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.getWhistleText;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree.toCodeString;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.Linter;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingTrailing;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class SpacingTrailingTest {
  private static final String DIRECTORY = "spacingTrailingTest/";
  private static final String INPUT = "_input.wstl";
  private static final String OUTPUT = "_output.wstl";
  private static final Linter LINTER = new Linter();

  @Test
  public void spacingTrailing_singleMethod_success() throws IOException {
    String testName = "singleMethod";

    BaseTreeNode result =
        LINTER.lint(
            addTrailingSpaces(getWhistleText(DIRECTORY + testName + INPUT)),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingTrailing()));

    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + OUTPUT));
  }

  @Test
  public void spacingTrailing_multipleMethods_success() throws IOException {
    String testName = "multipleMethods";

    BaseTreeNode result =
        LINTER.lint(
            addTrailingSpaces(getWhistleText(DIRECTORY + testName + INPUT)),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingTrailing()));

    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + OUTPUT));
  }

  @Test
  public void spacingTrailing_spaceBeforeEOF_success() throws IOException {
    String testName = "spaceBeforeEOF";

    BaseTreeNode result =
        LINTER.lint(
            addTrailingSpaces(getWhistleText(DIRECTORY + testName + INPUT)),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingTrailing()));

    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + OUTPUT));
  }

  @Test
  public void spacingTrailing_noTrailingSpaces_noChange() throws IOException {
    String testName = "noTrailingSpaces";

    BaseTreeNode result =
        LINTER.lint(
            getWhistleText(DIRECTORY + testName + INPUT),
            WhistleParser::program,
            FileInfo.getDefaultInstance(),
            Arrays.asList(new SpacingTrailing()));

    assertThat(toCodeString(result)).isEqualTo(getWhistleText(DIRECTORY + testName + INPUT));
  }

  private String addTrailingSpaces(String whistleText) {
    return String.join(" \n", whistleText.split("\n", -1)) + " ";
  }
}
