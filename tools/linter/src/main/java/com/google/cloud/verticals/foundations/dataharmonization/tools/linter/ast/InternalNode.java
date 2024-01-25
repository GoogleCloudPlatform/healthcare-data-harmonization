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

import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * In additional to the fields in BaseTreeNode, Internal node contains the originalNodeType (e.g.
 * StatementContext.class), and a list of children.
 */
public class InternalNode extends BaseTreeNode {
  final Class<? extends ParseTree> originalNodeType;
  List<BaseTreeNode> children = new ArrayList<>();

  public InternalNode(
      Class<? extends ParseTree> originalNodeType, BaseTreeNode parent, boolean linterCreated) {
    super(parent, linterCreated);
    this.originalNodeType = originalNodeType;
  }

  public List<BaseTreeNode> getChildren() {
    return children;
  }

  public void addChild(BaseTreeNode child) {
    children.add(child);
  }

  @Override
  public Class<?> getOriginalNodeType() {
    return this.originalNodeType;
  }

  @Override
  public InternalNode asInternal() {
    return this;
  }

  @Override
  public boolean isTerminal() {
    return false;
  }

  @Override
  public boolean isInternal() {
    return true;
  }
}
