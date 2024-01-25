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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.indent;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.BlockContext;
import com.google.common.collect.Iterables;

/**
 * SpacingBlock class adds an indent in front of every line inside an ExprBlock. <br>
 * Example 1:
 *
 * <pre>
 * def test() {
 * anotherFunction()
 * }
 * </pre>
 *
 * becomes
 *
 * <pre>
 * def test() {
 *     anotherFunction()
 * }
 * </pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>
 * def test() {
 *        anotherFunction()
 * }
 * </pre>
 *
 * becomes
 *
 * <pre>
 * def test() {
 *     anotherFunction()
 * }
 * </pre>
 */
public class SpacingBlock implements LinterRule<BlockContext> {

  @Override
  public Class<BlockContext> anchor() {
    return BlockContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(anchor());
  }

  @Override
  public void apply(BaseTreeNode treeNode) {
    addIndents(treeNode);
  }

  private void addIndents(BaseTreeNode treeNode) {
    IndexedDepthFirstIterator childWalker = treeNode.dfsWalker();

    while (childWalker.hasNext()) {
      TreeChildReference entry = childWalker.next();

      // If the node is internal, or a non-newline or non-comment terminal node, do not enter the
      // inner loop.
      if (entry.node().isInternal() || !(entry.node().isNewline() || entry.node().isComment())) {
        continue;
      }

      // If a newline or comment is found, start trimming the spaces after it.
      while (childWalker.hasNext()) {
        TreeChildReference maybeSpaceEntry = childWalker.peek();

        // If a non-whitespace or newline or linter created node is reached, stop trimming spaces.
        if (!maybeSpaceEntry.node().isWhitespace()
            || maybeSpaceEntry.node().isNewline()
            || maybeSpaceEntry.node().isLinterCreated()) {
          break;
        }

        // Remove existing spaces.
        childWalker.next();
        childWalker.remove();
      }

      // If the current and next nodes are newlines, do not insert an indent.
      if (entry.node().isNewline() && childWalker.peek().node().isNewline()) {
        continue;
      }

      // If the next token is the last token of treeNode (i.e. the last }) we're done.
      if (treeNode.isInternal()
          && childWalker.peek().node() == Iterables.getLast(treeNode.asInternal().getChildren())) {
        break;
      }

      // Insert a new tab.
      BaseTreeNode space = new TerminalNode(indent, treeNode, true, TerminalNode.Type.SPACE);
      childWalker.insert(space);
    }
  }
}
