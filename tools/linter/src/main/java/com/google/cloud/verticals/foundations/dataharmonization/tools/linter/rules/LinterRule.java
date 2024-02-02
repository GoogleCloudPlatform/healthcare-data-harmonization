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

package com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import org.antlr.v4.runtime.RuleContext;

/**
 * A formatting rule to be applied to Whistle code.
 *
 * <p>It can be assumed that matchesWithAnchor will always we be called before apply, so apply does
 * not need to check the TreeNode and can assume it already matchesWithAnchor.
 *
 * <p>The anchor method returns to the class of node for which the linter rule will apply (e.g.
 * BlockContext.class). The matchesWithAnchor method can also be used to check the class types of
 * parents and children of the current node, or other parameters required to apply the rule.
 */
public interface LinterRule<T extends RuleContext> {
  Class<T> anchor();

  /**
   * Ensures the node type and other conditions (e.g. parent node's type) match with the
   * requirements for applying the rule.
   */
  default boolean matchesWithAnchor(BaseTreeNode node) {
    return true;
  }

  /**
   * Applies the linting rule to a given node from the tree of Whistle code.
   *
   * @param node {@link BaseTreeNode} the node, located in tree, that is having a rule applied to it
   */
  void apply(BaseTreeNode node);

}
