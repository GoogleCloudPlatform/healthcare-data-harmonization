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
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.OPEN_BRACKET;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprInfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprInfixOpLambdaContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPostfixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprPrefixOpContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExprSourceContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.SourceExpressionContext;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

/**
 * BracketsRedundant class removes the brackets from an ExprSource node if the parent and child are
 * not both operators (e.g. infixOp). <br>
 * Example 1:
 *
 * <pre> if (!thisCondition) then {...}</pre>
 *
 * will become
 *
 * <pre>if !thisCondition  then {...}</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre> var x: (a + b)</pre>
 *
 * will become
 *
 * <pre>var x: a + b</pre>
 *
 * <br>
 * Example 3:
 *
 * <pre>var x: !(a?) and (!b?)</pre>
 *
 * will not change. <br>
 * Example 4:
 *
 * <pre>((x))</pre>
 *
 * will become
 *
 * <pre>x</pre>
 */
public class BracketsRedundant implements LinterRule<ExprSourceContext> {

  @Override
  public Class<ExprSourceContext> anchor() {
    return ExprSourceContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    ImmutableSet<Class<?>> opTypes =
        ImmutableSet.of(
            ExprInfixOpContext.class,
            ExprPrefixOpContext.class,
            ExprPostfixOpContext.class,
            ExprInfixOpLambdaContext.class);
    if (!canHaveBrackets(node)) {
      return false;
    }

    BaseTreeNode parent = getFirstValidParent(node);
    BaseTreeNode child = getOperableChild(node);

    boolean childIsOp = opTypes.contains(child.getOriginalNodeType());
    boolean parentIsOp = opTypes.contains(parent.getOriginalNodeType());

    return !parentIsOp || !childIsOp;
  }

  @Override
  public void apply(BaseTreeNode treeNode) {
    treeNode = treeNode.asInternal().getChildren().get(0);
    BaseTreeNode firstNode = treeNode.asInternal().getChildren().get(0);
    BaseTreeNode lastNode = Iterables.getLast(treeNode.asInternal().getChildren());
    if (firstNode.asTerminal().getValue().equals(OPEN_BRACKET)
        && !firstNode.isLinterCreated()
        && lastNode.asTerminal().getValue().equals(CLOSE_BRACKET)
        && !lastNode.isLinterCreated()) {
      treeNode.asInternal().getChildren().remove(firstNode);
      treeNode.asInternal().getChildren().remove(lastNode);
    }
  }

  /**
   * Given an ExprSource (presumably with brackets) finds the first child/grandchild that can be
   * checked to determine whether those brackets should be removed.
   *
   * <p>That is, the returned node is the first descendant of the given exprSource that is not
   * itself another exprSource that already had its brackets removed.
   */
  private static BaseTreeNode getOperableChild(BaseTreeNode exprSource) {
    BaseTreeNode child = operableGrandchild(exprSource);
    while (canHaveBrackets(child) && !hasBrackets(child)) {
      exprSource = child;
      child = operableGrandchild(exprSource);
    }
    return child;
  }

  private static BaseTreeNode operableGrandchild(BaseTreeNode exprSource) {
    BaseTreeNode sourceExpr = exprSource.asInternal().getChildren().get(0);
    if (!sourceExpr.getOriginalNodeType().equals(SourceExpressionContext.class)) {
      return sourceExpr;
    }
    return sourceExpr
        .asInternal()
        .getChildren()
        .get(sourceExpr.asInternal().getChildren().size() == 3 ? 1 : 0);
  }

  private static boolean hasBrackets(BaseTreeNode exprSource) {
    return exprSource
            .asInternal()
            .getChildren()
            .get(0)
            .getOriginalNodeType()
            .equals(SourceExpressionContext.class)
        && exprSource.asInternal().getChildren().get(0).asInternal().getChildren().size() == 3;
  }

  private static boolean canHaveBrackets(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(ExprSourceContext.class)
        && node.asInternal()
            .getChildren()
            .get(0)
            .getOriginalNodeType()
            .equals(SourceExpressionContext.class);
  }

  private static BaseTreeNode getFirstValidParent(BaseTreeNode exprSource) {
    BaseTreeNode parent = exprSource.getParent();
    while (parent != null
        && parent.getOriginalNodeType().equals(SourceExpressionContext.class)
        && parent.asInternal().getChildren().size() < 3) {
      parent = parent.getParent().getParent();
    }
    return parent;
  }
}
