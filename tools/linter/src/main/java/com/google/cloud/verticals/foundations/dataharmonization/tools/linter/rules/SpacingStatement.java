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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.COLON;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.SPACE;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.StatementContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetKeywordContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetStaticContext;
import java.util.ArrayList;
import java.util.List;

/**
 * SpacingStatement makes sure there is a single space after a field or variable assignment
 * (TARGET_ASSIGN:) in a statement. <br>
 * Example 1:
 *
 * <pre>var a:1+1</pre>
 *
 * becomes
 *
 * <pre>var a: 1+1</pre>
 *
 * <br>
 * Example 2:
 *
 * <pre>var b:   2+2</pre>
 *
 * becomes
 *
 * <pre>var b: 2+2</pre>
 *
 * <br>
 * Example 3:
 *
 * <pre>var   c:  blah()</pre>
 *
 * becomes
 *
 * <pre>var c: blah()</pre>
 *
 * <br>
 * Example 4:
 *
 * <pre>var d :e</pre>
 *
 * becomes
 *
 * <pre>var d: e</pre>
 *
 * <br>
 * Example 5:
 *
 * <pre>field:e</pre>
 *
 * becomes
 *
 * <pre>field: e</pre>
 */
public final class SpacingStatement implements LinterRule<StatementContext> {
  @Override
  public Class<StatementContext> anchor() {
    return StatementContext.class;
  }

  @Override
  public boolean matchesWithAnchor(BaseTreeNode node) {
    return node.getOriginalNodeType().equals(anchor());
  }

  @Override
  public void apply(BaseTreeNode node) {
    List<BaseTreeNode> children;
    if (node.isInternal()) {
      children = node.asInternal().getChildren();
    } else {
      children = new ArrayList<>();
    }
    // Go through the children of the statement node, and ensure there is a single space before and
    // after ':', and after 'var'.
    for (int i = 0; i < children.size(); i++) {
      // Check for colon
      if (isColon(children.get(i))) {
        int colonIndex = i;
        // Remove any existing spaces.
        if (children.size() - 1 > i && isNonLinterCreatedSpaceType(children.get(i + 1))) {
          children.remove(i + 1);
        }
        if (i > 0 && isNonLinterCreatedSpaceType(children.get(i - 1))) {
          children.remove(i - 1);
          colonIndex--;
        }
        // Insert a new space after the colon.
        BaseTreeNode spaceNode = new TerminalNode(SPACE, node, true, TerminalNode.Type.SPACE);
        children.add(colonIndex + 1, spaceNode);
      }
      // check for var keyword
      if (containsVar(children.get(i))) {
        List<BaseTreeNode> grandchildren = children.get(i).asInternal().getChildren();
        if (grandchildren.size() > 1 && isNonLinterCreatedSpaceType(grandchildren.get(1))) {
          // Remove any existing spaces.
          grandchildren.remove(1);
        }
        // Insert a new space.
        BaseTreeNode spaceNode = new TerminalNode(SPACE, node, true, TerminalNode.Type.SPACE);
        grandchildren.add(1, spaceNode);
      }
    }
  }

  private static boolean isNonLinterCreatedSpaceType(BaseTreeNode node) {
    return node.isTerminal()
        && node.asTerminal().getType().equals(TerminalNode.Type.SPACE)
        && !node.isLinterCreated();
  }

  private static boolean containsVar(BaseTreeNode node) {
    if (!(node.isInternal() && node.getOriginalNodeType().equals(TargetStaticContext.class))) {
      return false;
    }
    List<BaseTreeNode> children = node.asInternal().getChildren();
    for (BaseTreeNode child : children) {
      if (child.getOriginalNodeType().equals(TargetKeywordContext.class)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isColon(BaseTreeNode node) {
    return node.isTerminal() && node.asTerminal().getValue().equals(COLON);
  }
}
