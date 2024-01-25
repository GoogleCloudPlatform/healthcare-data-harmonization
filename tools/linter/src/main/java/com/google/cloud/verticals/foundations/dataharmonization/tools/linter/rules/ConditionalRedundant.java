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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.ELSE;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.BlockContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprBlockContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprConditionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.StatementContext;
import java.util.List;

/**
 * If the {@link ExprBlockContext} node following an else statement in an {@link
 * ExprConditionContext} node does not contain any statements, the 'else' and its ExprBlock should
 * be removed. <br>
 * Example 1:
 *
 * <pre>
 * if a + b then {
 *     doThis()
 * } else {
 *
 * }
 * </pre>
 *
 * becomes
 *
 * <pre>
 * if a + b then {
 *     doThis()
 * }
 * </pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>
 * if a + b then {
 *     doThis()
 * } else {
 *     doSomethingElse()
 * }
 * will not change.
 * </pre>
 */
public final class ConditionalRedundant implements LinterRule<ExprConditionContext> {

  @Override
  public Class<ExprConditionContext> anchor() {
    return ExprConditionContext.class;
  }

  /**
   * ExprConditionContext can contain :
   *
   * <ul>
   *   <li>if
   *   <li>condition
   *   <li>then
   *   <li>ExprBlock
   *   <li>else
   *   <li>ExprBlock
   * </ul>
   *
   * <p>If the length of an ExprConditionContext's children is >4, it contains an 'else'.
   */
  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(anchor())
        && node.isInternal()
        && node.asInternal().getChildren().size() > 4;
  }

  @Override
  public void apply(BaseTreeNode node) {
    List<BaseTreeNode> children = node.asInternal().getChildren();
    for (int i = 0; i < children.size(); i++) {
      BaseTreeNode child = children.get(i);
      if (child.isTerminal()
          && child.asTerminal().getValue().equals(ELSE)
          && children.size() - 1 >= i + 1) {
        // Find the next ExprBlock node following the "else". There may or may not be spaces.
        BaseTreeNode exprBlockNode;
        if (children.size() - 1 >= i + 2
            && children.get(i + 1).isTerminal()
            && children.get(i + 1).asTerminal().getType().equals(TerminalNode.Type.SPACE)) {
          exprBlockNode = children.get(i + 2);
        } else {
          exprBlockNode = children.get(i + 1);
        }
        // If the else block doesn't contain any statements, remove the block and the else
        // statement.
        if (exprBlockNode.getOriginalNodeType().equals(ExprBlockContext.class)
            && !containsStatements(exprBlockNode)) {
          children.remove(exprBlockNode);
          children.remove(i);
        }
      }
    }
    removeTrailingWhitespace(children);
  }

  private void removeTrailingWhitespace(List<BaseTreeNode> children) {
    for (int i = children.size() - 1; i > 0; i--) {
      if (!children.get(i).isWhitespace()) {
        break;
      } else {
        children.remove(i);
      }
    }
  }

  private static boolean containsStatements(BaseTreeNode exprBlockNode) {
    BaseTreeNode blockNode = exprBlockNode.asInternal().getChildren().get(0);
    if (!blockNode.getOriginalNodeType().equals(BlockContext.class)) {
      return false;
    }
    for (BaseTreeNode child : blockNode.asInternal().getChildren()) {
      if (child.getOriginalNodeType().equals(StatementContext.class)) {
        return true;
      }
    }
    return false;
  }
}
