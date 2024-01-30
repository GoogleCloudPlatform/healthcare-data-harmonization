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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules;

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.EOF;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.SEMICOLON;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.StatementContext;
import java.util.List;

/**
 * SemicolonsRedundant removes the semicolon from Statements that are within an ExprBlock and
 * followed by a new line.
 *
 * <p>When multiple statements are on the same line, semicolons in between statements will not be
 * removed. <br>
 * Example 1:
 *
 * <pre>var: a;</pre>
 *
 * becomes
 *
 * <pre>var: a</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>var: a; var: b;</pre>
 *
 * will keep the middle semicolon and remove the trailing semicolon, to become
 *
 * <pre>var: a; var: b</pre>
 */
public final class SemicolonsRedundant implements LinterRule<StatementContext> {

  @Override
  public Class<StatementContext> anchor() {
    return StatementContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(anchor());
  }

  @Override
  public void apply(BaseTreeNode treeNode) {
    List<BaseTreeNode> children = treeNode.asInternal().getChildren();
    BaseTreeNode trailingSemicolon = null;
    int trailingSemicolonIndex = 0;
    for (int i = children.size() - 1; i >= 0; i--) {
      BaseTreeNode child = children.get(i);
      if (child.isTerminal() && child.asTerminal().getValue().equals(SEMICOLON)) {
        trailingSemicolon = child;
        trailingSemicolonIndex = i;
      }
      if (!child.isWhitespace() && !child.isComment()) {
        break;
      }
    }
    if (trailingSemicolon != null && isFollowedByNewline(treeNode, trailingSemicolon)) {
      children.remove(trailingSemicolonIndex);
    }
  }

  private static boolean isFollowedByNewline(BaseTreeNode treeNode, BaseTreeNode semicolon) {
    IndexedDepthFirstIterator parentWalker = treeNode.getParent().dfsWalker();
    while (parentWalker.hasNext()) {
      TreeChildReference entry = parentWalker.next();
      if (!entry.node().equals(semicolon)) {
        continue;
      }
      // Continue searching for a newline, until a non-space/non-comment node is found.
      while (parentWalker.hasNext()) {
        TreeChildReference maybeNewLine = parentWalker.peek();
        if (maybeNewLine.node().isTerminal()
            && (maybeNewLine.node().asTerminal().getType().equals(TerminalNode.Type.NEWLINE)
                || maybeNewLine.node().asTerminal().getValue().equals(EOF))) {
          return true;
        } else if (!maybeNewLine.node().isWhitespace() && !maybeNewLine.node().isComment()) {
          break;
        }
        parentWalker.next();
      }
    }
    return false;
  }
}
