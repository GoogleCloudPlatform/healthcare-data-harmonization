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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;
import javax.annotation.Nullable;

/** IndexedDepthFirstIterator can walk the tree forwards and backwards, and implement changes. */
public class IndexedDepthFirstIterator implements Iterator<TreeChildReference> {
  private final Deque<TreeChildReference> next;
  private boolean childPushed;
  private boolean siblingPushed;
  private TreeChildReference last;

  IndexedDepthFirstIterator(BaseTreeNode start) {
    next = new ArrayDeque<>();
    if (start.isInternal()) {
      next.push(new TreeChildReference(start.asInternal(), -1));
    }
  }

  private IndexedDepthFirstIterator(
      Deque<TreeChildReference> next,
      boolean childPushed,
      boolean siblingPushed,
      TreeChildReference last) {
    this.next = new ArrayDeque<>(next);
    this.childPushed = childPushed;
    this.siblingPushed = siblingPushed;
    this.last = last;
  }

  public Deque<TreeChildReference> getNext() {
    return next;
  }

  public boolean getChildPushed() {
    return childPushed;
  }

  public boolean getSiblingPushed() {
    return siblingPushed;
  }

  public TreeChildReference getLast() {
    return last;
  }

  @Override
  public boolean hasNext() {
    return !next.isEmpty();
  }

  @Override
  public void remove() {
    if (last.getKey().asInternal().children.isEmpty()) {
      return;
    }

    // Remove the child.
    last.getKey().asInternal().children.remove((int) last.getValue());

    // Update the last to the node in the iteration before the one that was deleted.
    last = peekPrev();

    // If the next is a child that no longer exists, skip it.
    if (childPushed) {
      next.pop();
    }

    // If next is a sibling that got shifted over, shift the entry.
    if (siblingPushed) {
      TreeChildReference sibling = next.pop();
      next.push(new TreeChildReference(sibling.getKey(), sibling.getValue() - 1));
    }

    // Remove can only be called once per next, so these can just be flags.
    // If we want to allow repeated remove calls then these would need to be stacks.
    childPushed = false;
    siblingPushed = false;
  }

  public TreeChildReference peekPrev() {
    InternalNode parent = last.getKey().asInternal();
    int prevSibling = last.getValue() - 1;
    if (prevSibling < 0) {
      int index = -1;
      if (parent.parent != null) {
        index = parent.parent.asInternal().children.indexOf(parent);
        return new TreeChildReference(parent.parent.asInternal(), index);
      }
      return new TreeChildReference(last.getKey(), index);
    }

    int childIndex = prevSibling;
    while (!parent.getChildren().get(childIndex).isTerminal()) {
      parent = parent.getChildren().get(childIndex).asInternal();
      childIndex = parent.getChildren().size() - 1;
    }

    return new TreeChildReference(parent, childIndex);
  }

  @Nullable
  @CanIgnoreReturnValue
  @Override
  public TreeChildReference next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }

    childPushed = false;
    siblingPushed = false;

    TreeChildReference nextEntry = next.pop();
    BaseTreeNode currentParent = nextEntry.getKey();
    Integer currentChildIndex = nextEntry.getValue();

    BaseTreeNode currentChild = nextEntry.node();

    // Check our sibling if any.
    if (currentChildIndex >= 0
        && currentChildIndex < currentParent.asInternal().children.size() - 1) {
      next.push(new TreeChildReference(currentParent.asInternal(), currentChildIndex + 1));
      siblingPushed = true;
    }

    // Check our child, if any. Since stack, this will get checked first (DFS).
    if (currentChild.isInternal() && !currentChild.asInternal().children.isEmpty()) {
      next.push(new TreeChildReference(currentChild.asInternal(), 0));
      childPushed = true;
    }

    last = nextEntry;
    return nextEntry;
  }

  public TreeChildReference peek() {
    return next.peek();
  }

  public IndexedDepthFirstIterator fork() {
    return new IndexedDepthFirstIterator(next, childPushed, siblingPushed, last);
  }

  /**
   * Inserts the given child after the last node that was returns by next(). Next call to next()
   * will point to this node.
   *
   * <p>Only valid to be called after next().
   */
  public void insert(BaseTreeNode treeNode) {
    TreeChildReference nextEntry = next.peek();

    // The next child location, or where it would have been.
    InternalNode parent = nextEntry != null ? nextEntry.getKey() : last.getKey();
    int index = nextEntry != null ? nextEntry.getValue() : (last.getValue() + 1);

    // Insert there.
    parent.children.add(index, treeNode);

    // Add it to iteration if it wasn't there.
    if (next.isEmpty()) {
      next.push(new TreeChildReference(parent, index));
    }
  }

  /** Helper class for walking the tree. */
  public static class TreeChildReference extends SimpleImmutableEntry<InternalNode, Integer> {

    public TreeChildReference(InternalNode key, Integer value) {
      super(key, value);
    }

    public BaseTreeNode node() {
      if (getValue() < 0) {
        return getKey();
      }
      if (getKey().isInternal()) {
        return getKey().asInternal().children.get(getValue());
      }
      return getKey();
    }

    @Override
    public String toString() {
      return String.format("%d token of\n%s", getValue(), AntlrToTree.renderCode(getKey()));
    }
  }
}
