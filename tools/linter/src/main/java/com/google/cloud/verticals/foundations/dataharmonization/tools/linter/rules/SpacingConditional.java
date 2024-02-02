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
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.IF;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.SPACE;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.THEN;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprConditionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprSourceContext;
import com.google.common.collect.Iterables;
import java.util.List;

/**
 * SpacingConditional makes sure there is a single space after an "if", before and after a "then"
 * and before and after an "else" in a conditional node. <br>
 * Example 1:
 *
 * <pre>if  resource   then  {...}</pre>
 *
 * will become
 *
 * <pre>if resource then {...}</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>if(resource)then{...}else{...}
 * </pre>
 *
 * will become
 *
 * <pre>if (resource) then {...} else {...}</pre>
 */
public class SpacingConditional implements LinterRule<ExprConditionContext> {

  @Override
  public Class<ExprConditionContext> anchor() {
    return ExprConditionContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.isInternal() && node.getOriginalNodeType().equals(anchor());
  }

  @Override
  public void apply(BaseTreeNode node) {
    List<BaseTreeNode> children = node.asInternal().getChildren();
    // Remove any existing spaces from after 'if' and before and after 'then' and 'else'.
    // Also remove any spaces from the start or end of the source expression
    for (int i = children.size() - 1; i >= 0; i--) {
      BaseTreeNode child = children.get(i);
      if (child.isTerminal()
          && child.asTerminal().getValue().equals(IF)
          && children.size() > i + 1) {
        removeSpacesAfter(children, i);
        removeSpacesExprSourceContext(children, i);
      }
      if (child.isTerminal()
          && (child.asTerminal().getValue().equals(THEN)
              || child.asTerminal().getValue().equals(ELSE))) {
        removeSpacesAfter(children, i);
        removeSpacesBefore(children, i);
      }
    }
    // Add single spaces back in.
    for (int i = children.size() - 1; i >= 0; i--) {
      BaseTreeNode child = children.get(i);
      if (child.isTerminal()
          && child.asTerminal().getValue().equals(IF)
          && children.size() > i + 1) {
        insertNewSpaceNode(children, i + 1, node);
      }
      if (child.isTerminal()
          && (child.asTerminal().getValue().equals(THEN)
              || child.asTerminal().getValue().equals(ELSE))) {
        insertNewSpaceNode(children, i + 1, node);
        insertNewSpaceNode(children, i, node);
      }
    }
  }

  private void removeSpacesExprSourceContext(List<BaseTreeNode> children, int i) {
    if (!children.get(i + 1).getOriginalNodeType().equals(ExprSourceContext.class)) {
      return;
    }
    List<BaseTreeNode> sourceExpressionChildren =
        children.get(i + 1).asInternal().getChildren().get(0).asInternal().getChildren();
    while (sourceExpressionChildren.get(0).isWhitespace()) {
      sourceExpressionChildren.remove(0);
    }
    while (Iterables.getLast(sourceExpressionChildren).isWhitespace()) {
      sourceExpressionChildren.remove(sourceExpressionChildren.size() - 1);
    }
  }

  private void removeSpacesBefore(List<BaseTreeNode> children, int i) {
    BaseTreeNode prevChild = children.get(i - 1);
    if (prevChild.isTerminal()
        && !prevChild.isLinterCreated()
        && prevChild.asTerminal().getType().equals(TerminalNode.Type.SPACE)) {
      children.remove(i - 1);
    }
  }

  private void removeSpacesAfter(List<BaseTreeNode> children, int i) {
    BaseTreeNode nextChild = children.get(i + 1);
    if (nextChild.isTerminal()
        && !nextChild.isLinterCreated()
        && nextChild.asTerminal().getType().equals(TerminalNode.Type.SPACE)) {
      children.remove(i + 1);
    }
  }

  private void insertNewSpaceNode(List<BaseTreeNode> children, int i, BaseTreeNode parentNode) {
    BaseTreeNode spaceNode = new TerminalNode(SPACE, parentNode, true, TerminalNode.Type.SPACE);
    children.add(i, spaceNode);
  }
}
