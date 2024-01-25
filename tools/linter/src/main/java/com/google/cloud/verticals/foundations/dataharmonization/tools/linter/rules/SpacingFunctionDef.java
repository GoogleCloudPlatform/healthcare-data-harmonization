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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.CLOSE_BRACKET;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.DEF;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.SPACE;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionDefContext;
import java.util.List;

/**
 * SpacingFunctionDef makes sure there is a single space after a "def" or ")" in a function
 * definition. <br>
 * Example 1:
 *
 * <pre>def   exampleFunction(a, b)  {...}</pre>
 *
 * will become
 *
 * <pre>def exampleFunction(a, b) {...}</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>def exampleFunction(a, b){...}</pre>
 *
 * will become
 *
 * <pre>def exampleFunction(a, b) {...}</pre>
 */
public final class SpacingFunctionDef implements LinterRule<FunctionDefContext> {

  @Override
  public Class<FunctionDefContext> anchor() {
    return FunctionDefContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(anchor());
  }

  @Override
  public void apply(BaseTreeNode node) {
    List<BaseTreeNode> children = node.asInternal().getChildren();
    // Go through the children of the FunctionDef node, and ensure there is a single space after
    // 'def' or ')'.
    for (int i = 0; i < children.size(); i++) {
      if (isCloseBracket(children.get(i)) || isDef(children.get(i))) {
        if (i + 1 <= children.size() && isNonLinterCreatedSpaceType(children.get(i + 1))) {
          // Remove any existing spaces.
          children.remove(i + 1);
        }
        insertNewSpaceNode(children, i + 1, node);
      }
    }
  }

  private void insertNewSpaceNode(List<BaseTreeNode> children, int i, BaseTreeNode parentNode) {
    BaseTreeNode spaceNode = new TerminalNode(SPACE, parentNode, true, TerminalNode.Type.SPACE);
    children.add(i, spaceNode);
  }

  private boolean isNonLinterCreatedSpaceType(BaseTreeNode node) {
    return node.isTerminal()
        && node.asTerminal().getType().equals(TerminalNode.Type.SPACE)
        && !node.isLinterCreated();
  }

  private static boolean isDef(BaseTreeNode node) {
    return node.isTerminal() && node.asTerminal().getValue().equals(DEF);
  }

  private static boolean isCloseBracket(BaseTreeNode node) {
    return node.isTerminal() && node.asTerminal().getValue().equals(CLOSE_BRACKET);
  }
}
