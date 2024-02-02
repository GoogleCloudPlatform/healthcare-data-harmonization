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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.forAllNodes;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.getWhistleText;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree.toCodeString;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.NEWLINE;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.SPACE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import java.io.IOException;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** AntlrToTree Unit tests. */
@RunWith(JUnit4.class)
public class AntlrToTreeTest {

  static Function<WhistleParser, RuleContext> programParserRule = WhistleParser::program;
  String directory = "antlrToTreeTest/";
  String input = "_input.wstl";

  @Test
  public void treeDebug_success() throws IOException {
    String testName = "debug";
    BaseTreeNode tree =
        new AntlrToTree()
            .tree(getWhistleText(directory + testName + input), FileInfo.getDefaultInstance());
    Assert.assertEquals(
        getWhistleText(directory + testName + "_output.txt"), AntlrToTree.toDebugString(tree));
  }

  @Test
  public void treeCreation_success() throws IOException {
    String testName = "treeCreation";
    String text = getWhistleText(directory + testName + input);
    BaseTreeNode tree = new AntlrToTree().tree(text, FileInfo.getDefaultInstance());
    assertThat(text).isEqualTo(toCodeString(tree));
  }

  @Test
  public void treeCreation_nullParserRule_throwsEx() throws IOException {
    String testName = "treeCreationError";
    assertThrows(
        "The parser rule provided must not be null.",
        NullPointerException.class,
        () ->
            new AntlrToTree(null, true)
                .tree(getWhistleText(directory + testName + input), FileInfo.getDefaultInstance()));
  }

  @Test
  public void treeCreation_invalidWhistleCode_throwEx() throws IOException {
    String invalidWhistleText = getWhistleText(directory + "invalidWhistleText" + input);
    Exception ex =
        assertThrows(
            Exception.class,
            () -> new AntlrToTree().tree(invalidWhistleText, FileInfo.getDefaultInstance()));
    assertThat(ex)
        .hasMessageThat()
        .contains(
            "unexpected '{', expecting one of {'if', BOOL, 'var', 'side', 'root', NEWLINE, 'merge',"
                + " 'append', 'replace', 'extend', '-', '*', '!', '(', '[', '{', '}', INTEGER,"
                + " IDENTIFIER (Examples of Identifiers include, Function, Variable and Package"
                + " names), STRING}");
  }

  @Test
  public void treeCreation_invalidWhistleCode_throwExFalse_returnsPartialResult()
      throws IOException {
    String invalidWhistleText = getWhistleText(directory + "invalidWhistleText" + input);
    BaseTreeNode tree =
        new AntlrToTree(programParserRule, false)
            .tree(invalidWhistleText, FileInfo.getDefaultInstance());
    assertThat(invalidWhistleText).isEqualTo(toCodeString(tree));
  }

  @Test
  public void treeCreation_allNodesAreLinterCreated_success() throws IOException {
    String testName = "linterCreated";
    BaseTreeNode tree =
        new AntlrToTree()
            .tree(getWhistleText(directory + testName + input), FileInfo.getDefaultInstance());
    forAllNodes(tree, n -> assertThat(n.isLinterCreated()).isFalse());
  }

  @Test
  public void treeCreation_spaceNodeIsTypeSpace_success() throws IOException {
    String testName = "spaceType";
    BaseTreeNode tree =
        new AntlrToTree()
            .tree(getWhistleText(directory + testName + input), FileInfo.getDefaultInstance());
    List<BaseTreeNode> children = tree.asInternal().getChildren();
    for (BaseTreeNode child : children.get(0).asInternal().getChildren()) {
      if (child.isTerminal() && child.asTerminal().getValue().contains(" ")) {
        assertThat(child.asTerminal().getType()).isEqualTo(SPACE);
      }
    }
  }

  @Test
  public void treeCreation_newlineNodeIsTypeNewline_success() throws IOException {
    String testName = "newLineType";
    BaseTreeNode tree =
        new AntlrToTree()
            .tree(getWhistleText(directory + testName + input), FileInfo.getDefaultInstance());
    List<BaseTreeNode> children = tree.asInternal().getChildren();
    for (BaseTreeNode child : children.get(0).asInternal().getChildren()) {
      if (child.isTerminal() && child.asTerminal().getValue().contains("\n")) {
        assertThat(child.asTerminal().getType()).isEqualTo(NEWLINE);
      }
    }
  }
}
