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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.TEXT;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.InternalNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ProgramContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.StatementContext;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

/** Helper functions for LinterTest and AntlrToTreeTest. */
public class TestResourceUtils {

  private TestResourceUtils() {}

  public static String getWhistleText(String testFile) throws IOException {
    return Resources.toString(Resources.getResource(testFile), UTF_8);
  }

  static void resetTest(String filename, String whistleText) throws IOException {
    File newWhistleFile = new File(filename);
    try (FileWriter f2 = new FileWriter(newWhistleFile, UTF_8)) {
      f2.write(whistleText);
    }
  }

  public static void forAllNodes(BaseTreeNode root, Consumer<BaseTreeNode> body) {
    // DFS through all the nodes, executing body.accept(node) on each one.
    body.accept(root);
    for (BaseTreeNode child : root.asInternal().getChildren()) {
      if (child.isTerminal()) {
        body.accept(child);
        continue;
      }
      body.accept(child);
      forAllNodes(child, body);
    }
  }

  /**
   * Creates a tree with the following structure:
   *
   * <pre>
   * x
   * </pre>
   *
   * @return {@link ImmutableList<BaseTreeNode>} a list of the nodes in DFS order, with the root
   *     always first
   */
  public static ImmutableList<BaseTreeNode> makeTreeZero() {
    BaseTreeNode x = new InternalNode(ProgramContext.class, null, false);
    return ImmutableList.of(x);
  }

  /**
   * Creates a tree with the following structure:
   *
   * <pre>
   * x
   * |
   * y
   * </pre>
   *
   * @return {@link ImmutableList<BaseTreeNode>} a list of the nodes in DFS order, with the root
   *     always first
   */
  public static ImmutableList<BaseTreeNode> makeTreeOne() {
    BaseTreeNode x = new InternalNode(ProgramContext.class, null, false);
    BaseTreeNode y = new TerminalNode("y", x, false, TEXT);
    x.asInternal().addChild(y);
    return ImmutableList.of(x, y);
  }

  /**
   * Creates a tree with the following structure, and returns the root:
   *
   * <pre>
   *    x
   *    |
   *    y
   *   |  |
   *  z    w
   * </pre>
   *
   * @return {@link ImmutableList<BaseTreeNode>} a list of the nodes in DFS order, with the root
   *     always first
   */
  public static ImmutableList<BaseTreeNode> makeTreeTwo() {
    BaseTreeNode x = new InternalNode(ProgramContext.class, null, false);
    BaseTreeNode y = new InternalNode(StatementContext.class, x, false);
    BaseTreeNode z = new TerminalNode("z", y, false, TEXT);
    BaseTreeNode w = new TerminalNode("w", y, false, TEXT);
    x.asInternal().addChild(y);
    y.asInternal().addChild(z);
    y.asInternal().addChild(w);
    return ImmutableList.of(x, y, z, w);
  }

  /**
   * Creates a tree with the following structure, and returns the root:
   *
   * <pre>
   *      x
   *    |   |
   *    y   q
   *  |  |
   *  z  w
   *    </pre>
   *
   * @return {@link ImmutableList<BaseTreeNode>} a list of the nodes in DFS order, with the root
   *     always first
   */
  public static ImmutableList<BaseTreeNode> makeTreeThree() {
    BaseTreeNode x = new InternalNode(ProgramContext.class, null, false);
    BaseTreeNode y = new InternalNode(StatementContext.class, x, false);
    BaseTreeNode z = new TerminalNode("z", y, false, TEXT);
    BaseTreeNode w = new TerminalNode("w", y, false, TEXT);
    BaseTreeNode q = new TerminalNode("q", w, false, TEXT);
    x.asInternal().addChild(y);
    y.asInternal().addChild(z);
    y.asInternal().addChild(w);
    x.asInternal().addChild(q);
    return ImmutableList.of(x, y, z, w, q);
  }

  /**
   * Creates a tree with the following structure, and returns the root:
   *
   * <pre>
   *      x
   *    |   |
   *    y   z
   * </pre>
   *
   * @return {@link ImmutableList<BaseTreeNode>} a list of the nodes in DFS order, with the root
   *     always first
   */
  public static ImmutableList<BaseTreeNode> makeTreeFour() {
    BaseTreeNode x = new InternalNode(ProgramContext.class, null, false);
    BaseTreeNode y = new TerminalNode("y", x, false, TEXT);
    BaseTreeNode z = new TerminalNode("z", x, false, TEXT);
    x.asInternal().addChild(y);
    x.asInternal().addChild(z);
    return ImmutableList.of(x, y, z);
  }
}
