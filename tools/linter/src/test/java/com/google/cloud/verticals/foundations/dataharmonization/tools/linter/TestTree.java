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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.InternalNode;
import java.util.HashMap;
import java.util.Map;

class TestTree {
  private final Map<BaseTreeNode, BaseTreeNode> nextSibs = new HashMap<>();
  private final Map<BaseTreeNode, BaseTreeNode> prevSibs = new HashMap<>();
  private final Map<BaseTreeNode, TreeChildReference> treeChildReferences = new HashMap<>();

  public TestTree(InternalNode root) {
    addSibs(root);
    treeChildReferences.put(root, new TreeChildReference(root, -1));
  }

  private void addSibs(InternalNode root) {
    for (int i = 0; i < root.getChildren().size(); i++) {
      BaseTreeNode current = root.getChildren().get(i);
      treeChildReferences.put(current, new TreeChildReference(root, i));
      if (i < root.getChildren().size() - 1) {
        nextSibs.put(current, root.getChildren().get(i + 1));
      }
      if (i > 0) {
        prevSibs.put(current, root.getChildren().get(i - 1));
      }

      if (current.isInternal()) {
        addSibs(current.asInternal());
      }
    }
  }

  public BaseTreeNode nextSiblingOf(BaseTreeNode node) {
    return nextSibs.getOrDefault(node, null);
  }

  public BaseTreeNode prevSiblingOf(BaseTreeNode node) {
    return prevSibs.getOrDefault(node, null);
  }

  public TreeChildReference getTreeChildReference(BaseTreeNode baseTreeNode) {
    if (baseTreeNode == null) {
      return null;
    }
    TreeChildReference treeChildReference = treeChildReferences.get(baseTreeNode);
    if (treeChildReference == null) {
      throw new IllegalArgumentException(
          "Tree child reference cannot be null. You messed something up real bad.");
    }
    return treeChildReference;
  }
}
