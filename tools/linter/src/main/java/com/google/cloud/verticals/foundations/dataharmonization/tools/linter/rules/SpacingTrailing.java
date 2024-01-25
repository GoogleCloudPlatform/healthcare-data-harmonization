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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.SPACE;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ProgramContext;

/**
 * SpacingTrailing removes any trailing spaces from the end of a line.
 *
 * <p>Example 1:
 *
 * <pre>var: a  </pre>
 *
 * becomes
 *
 * <pre>var: a</pre>
 */
public final class SpacingTrailing implements LinterRule<ProgramContext> {

  @Override
  public Class<ProgramContext> anchor() {
    return ProgramContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(ProgramContext.class);
  }

  @Override
  public void apply(BaseTreeNode node) {
    IndexedDepthFirstIterator walker = node.dfsWalker();
    while (walker.hasNext()) {
      TreeChildReference entry = walker.next();
      if (entry.node().isTerminal() && entry.node().asTerminal().getType().equals(SPACE)) {
        TreeChildReference maybeNewline = walker.peek();
        if (maybeNewline.node().isNewline() || maybeNewline.node().isEof()) {
          walker.remove();
        }
      }
    }
  }
}
