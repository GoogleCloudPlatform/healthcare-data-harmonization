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

import org.antlr.v4.runtime.tree.TerminalNodeImpl;

/**
 * In additional to the fields in BaseTreeNode, TerminalNode contains the Type of node (i.e.
 * comment, space, newline, or regular terminal node), and the value.
 */
public class TerminalNode extends BaseTreeNode {
  final Type type;
  String value;

  public TerminalNode(String value, BaseTreeNode parent, boolean linterCreated, Type type) {
    super(parent, linterCreated);
    this.type = type;
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public Type getType() {
    return type;
  }

  @Override
  public Class<?> getOriginalNodeType() {
    return TerminalNodeImpl.class;
  }

  @Override
  public boolean isTerminal() {
    return true;
  }

  @Override
  public boolean isInternal() {
    return false;
  }

  @Override
  public TerminalNode asTerminal() {
    return this;
  }

  /** Types of Terminal Tree Nodes. */
  public enum Type {
    TEXT,
    SPACE,
    NEWLINE,
    COMMENT
  }
}
