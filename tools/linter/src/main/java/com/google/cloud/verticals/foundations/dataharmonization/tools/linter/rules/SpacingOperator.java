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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.INFIX_OPERATORS;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.SPACE;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprInfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPostfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPrefixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExpressionContext;
import java.util.ArrayList;
import java.util.List;

/**
 * SpacingOperator adds a space on either side of the operator in an ExprInfixOp. <br>
 * Example 1:
 *
 * <pre>var a: 1+2</pre>
 *
 * becomes
 *
 * <pre>var a: 1 + 2</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>if a>=b then {...}</pre>
 *
 * becomes
 *
 * <pre>if a >= b then {...}</pre>
 *
 * <br>
 * Example 3:
 *
 * <pre>! x</pre>
 *
 * becomes
 *
 * <pre>!x</pre>
 *
 * <br>
 * Example 4:
 *
 * <pre>x ?</pre>
 *
 * becomes
 *
 * <pre>x?</pre>
 */
public class SpacingOperator implements LinterRule<ExpressionContext> {
  @Override
  public Class<ExpressionContext> anchor() {
    return ExpressionContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(ExprInfixOpContext.class)
        || node.getOriginalNodeType().equals(ExprPrefixOpContext.class)
        || node.getOriginalNodeType().equals(ExprPostfixOpContext.class);
  }

  @Override
  public void apply(BaseTreeNode treeNode) {
    removeSpaces(treeNode);
    if (treeNode.getOriginalNodeType().equals(ExprInfixOpContext.class)) {
      addSpaces(treeNode);
    }
  }

  private void removeSpaces(BaseTreeNode node) {
    List<BaseTreeNode> children = node.asInternal().getChildren();
    // Find the location of any existing spaces, and remove them.
    for (int i = children.size() - 1; i >= 0; i--) {
      if (children.get(i).isTerminal()
          && children.get(i).asTerminal().getType().equals(TerminalNode.Type.SPACE)
          && !children.get(i).isLinterCreated()) {
        children.remove(i);
      }
    }
  }

  private void addSpaces(BaseTreeNode node) {
    // Keep track of where the operator is, and add the spaces before and after it.
    int infixOperatorIndex = 0;
    List<BaseTreeNode> children = node.asInternal().getChildren();
    for (int i = 0; i < children.size(); i++) {
      List<BaseTreeNode> grandChildren = new ArrayList<>();
      if (children.get(i).isInternal()) {
        grandChildren = children.get(i).asInternal().getChildren();
      }
      if (!grandChildren.isEmpty()
          && grandChildren.get(0).isTerminal()
          && INFIX_OPERATORS.contains(grandChildren.get(0).asTerminal().getValue())) {
        infixOperatorIndex = i;
      }
    }
    // Add a single space before and after the infix operator.
    children.add(
        infixOperatorIndex + 1, new TerminalNode(SPACE, node, true, TerminalNode.Type.SPACE));
    children.add(infixOperatorIndex, new TerminalNode(SPACE, node, true, TerminalNode.Type.SPACE));
  }
}
