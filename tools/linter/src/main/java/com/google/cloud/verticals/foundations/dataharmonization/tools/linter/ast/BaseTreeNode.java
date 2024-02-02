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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.LinterConstants.EOF;

/**
 * BaseTreeNode can be implemented as either InternalNode (node has children) or TerminalNode (node
 * does not have children).
 *
 * <p>All BaseTreeNodes will contain the node's parent, and a flag indicating if the node has been
 * created by the linter itself (and therefore should not be changed).
 */
public abstract class BaseTreeNode {
  protected final BaseTreeNode parent;
  protected final boolean linterCreated;

  public BaseTreeNode(BaseTreeNode parent, boolean linterCreated) {
    this.parent = parent;
    this.linterCreated = linterCreated;
  }

  public abstract boolean isTerminal();

  public abstract boolean isInternal();

  public abstract Class<?> getOriginalNodeType();

  public TerminalNode asTerminal() {
    throw new ClassCastException();
  }

  public InternalNode asInternal() {
    throw new ClassCastException();
  }

  public BaseTreeNode getParent() {
    return parent;
  }

  public boolean isLinterCreated() {
    return linterCreated;
  }

  public boolean isWhitespace() {
    return isTerminal() && asTerminal().value != null && asTerminal().value.isBlank();
  }

  public boolean isNewline() {
    return isTerminal() && asTerminal().type == TerminalNode.Type.NEWLINE;
  }

  public boolean isComment() {
    return isTerminal() && asTerminal().type == TerminalNode.Type.COMMENT;
  }

  public boolean isEof() {
    return isTerminal() && asTerminal().value.equals(EOF);
  }

  public IndexedDepthFirstIterator dfsWalker() {
    return new IndexedDepthFirstIterator(this);
  }
}
