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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.makeTreeFour;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.makeTreeOne;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.makeTreeThree;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.makeTreeTwo;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.TestResourceUtils.makeTreeZero;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.TEXT;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class IndexedDepthFirstIteratorTest {

  private List<BaseTreeNode> nodes;
  private final int startingIndex;

  public IndexedDepthFirstIteratorTest(
      String testName, List<BaseTreeNode> nodes, int startingIndex) {
    this.nodes = nodes;
    this.startingIndex = startingIndex;
  }

  @Test
  public void test_next() {
    // Get the node of the tree at the root
    TestTree testTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator childWalker = initializeTestWalker(testTree);
    boolean hasNext = childWalker.hasNext();
    assertThat(hasNext).isEqualTo(startingIndex < nodes.size() - 1);
    if (!hasNext) {
      return;
    }
    TreeChildReference entry = childWalker.next();
    int currentIndex = startingIndex + 1;
    verifyPostActionIteratorState(childWalker, entry, testTree, currentIndex);
  }

  @Test
  public void test_peek_prev() {
    // Get the node of the tree at the root
    TestTree testTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator childWalker = initializeTestWalker(testTree);

    boolean hasPrev = childWalker.getLast().node().getParent() != null;
    assertThat(hasPrev).isEqualTo(startingIndex > 0);
    if (!hasPrev) {
      return;
    }
    int currentIndex = startingIndex - 1;
    TreeChildReference entry = childWalker.peekPrev();
    assertThat(entry.node()).isEqualTo(nodes.get(currentIndex));

    // verify that the state of the iterator did not change
    BaseTreeNode child =
        nodes.get(startingIndex).isInternal()
                && !nodes.get(startingIndex).asInternal().getChildren().isEmpty()
            ? nodes.get(startingIndex).asInternal().getChildren().get(0)
            : null;
    BaseTreeNode sibling = testTree.nextSiblingOf(nodes.get(startingIndex));

    verifyIteratorState(
        childWalker,
        testTree.getTreeChildReference(child),
        testTree.getTreeChildReference(sibling),
        child != null,
        sibling != null,
        startingIndex > -1 ? testTree.getTreeChildReference(nodes.get(startingIndex)) : null);
  }

  @Test
  public void test_remove() {
    // Get the node of the tree at the root
    TestTree testTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator childWalker = initializeTestWalker(testTree);
    boolean hasNext = childWalker.hasNext();
    assertThat(hasNext).isEqualTo(startingIndex < nodes.size() - 1);
    if (!hasNext) {
      return;
    }
    childWalker.next();
    childWalker.remove();
    List<BaseTreeNode> updatedNodes = new ArrayList<>(nodes);
    updatedNodes.remove(startingIndex + 1);
    nodes = updatedNodes;

    // remake the tree with the node removed
    TestTree postRemovalTestTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator postRemovalChildWalker = initializeTestWalker(postRemovalTestTree);

    verifyPostActionIteratorState(
        postRemovalChildWalker,
        postRemovalChildWalker.getLast(),
        postRemovalTestTree,
        startingIndex);
  }

  @Test
  public void test_insert() {
    // Get the node of the tree at the root
    TestTree testTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator childWalker = initializeTestWalker(testTree);
    boolean hasNext = childWalker.hasNext();
    assertThat(hasNext).isEqualTo(startingIndex < nodes.size() - 1);
    if (!hasNext) {
      return;
    }
    BaseTreeNode newInsertionNode =
        new TerminalNode("new Insertion Node", childWalker.getLast().node(), true, TEXT);
    childWalker.insert(newInsertionNode);

    List<BaseTreeNode> updatedNodes = new ArrayList<>(nodes);
    updatedNodes.add(startingIndex + 1, newInsertionNode);
    nodes = updatedNodes;

    // remake the tree with the node inserted
    TestTree postRemovalTestTree = new TestTree(nodes.get(0).asInternal());
    IndexedDepthFirstIterator postRemovalChildWalker = initializeTestWalker(postRemovalTestTree);

    verifyPostActionIteratorState(
        postRemovalChildWalker,
        postRemovalChildWalker.getLast(),
        postRemovalTestTree,
        startingIndex);
  }

  @Parameters(name = "{0}")
  public static ImmutableList<Object[]> data() {
    return ImmutableList.copyOf(
        new Object[][] {
          {"At root with one child", makeTreeOne(), 0},
          {"At root with no children", makeTreeZero(), 0},
          {"At root with multiple children", makeTreeFour(), 0},
          {"At an internal node with multiple children and no sibling", makeTreeTwo(), 1},
          {"At an internal node with multiple children and a sibling", makeTreeThree(), 1},
          {"At a leaf node with no sibling", makeTreeOne(), 1},
          {"At a leaf node with a sibling", makeTreeFour(), 1}
        });
  }

  private IndexedDepthFirstIterator initializeTestWalker(TestTree testTree) {
    BaseTreeNode tree = nodes.get(0);
    IndexedDepthFirstIterator childWalker = tree.dfsWalker();
    for (int i = -1; i < startingIndex; i++) {
      childWalker.next();
    }
    BaseTreeNode child =
        nodes.get(startingIndex).isInternal()
                && !nodes.get(startingIndex).asInternal().getChildren().isEmpty()
            ? nodes.get(startingIndex).asInternal().getChildren().get(0)
            : null;
    BaseTreeNode sibling = testTree.nextSiblingOf(nodes.get(startingIndex));

    verifyIteratorState(
        childWalker,
        testTree.getTreeChildReference(child),
        testTree.getTreeChildReference(sibling),
        child != null,
        sibling != null,
        startingIndex > -1 ? testTree.getTreeChildReference(nodes.get(startingIndex)) : null);
    return childWalker;
  }

  private void verifyPostActionIteratorState(
      IndexedDepthFirstIterator childWalker,
      TreeChildReference entry,
      TestTree testTree,
      int currentIndex) {
    assertThat(entry.node()).isEqualTo(nodes.get(currentIndex));

    BaseTreeNode child =
        nodes.get(currentIndex).isInternal()
                && !nodes.get(currentIndex).asInternal().getChildren().isEmpty()
            ? nodes.get(currentIndex).asInternal().getChildren().get(0)
            : null;
    BaseTreeNode sibling = testTree.nextSiblingOf(nodes.get(currentIndex));

    verifyIteratorState(
        childWalker,
        testTree.getTreeChildReference(child),
        testTree.getTreeChildReference(sibling),
        child != null,
        sibling != null,
        currentIndex > -1 ? testTree.getTreeChildReference(nodes.get(currentIndex)) : null);
  }

  public static void verifyIteratorState(
      IndexedDepthFirstIterator current,
      TreeChildReference child,
      TreeChildReference sibling,
      boolean childPushed,
      boolean siblingPushed,
      TreeChildReference last) {
    List<TreeChildReference> next = new ArrayList<>();
    if (child != null) {
      next.add(child);
    }
    if (sibling != null) {
      next.add(sibling);
    }

    assertThat(current.getNext()).containsAtLeastElementsIn(next).inOrder();
    assertThat(current.getChildPushed()).isEqualTo(childPushed);
    assertThat(current.getSiblingPushed()).isEqualTo(siblingPushed);
    assertThat(current.getLast()).isEqualTo(last);
  }
}
