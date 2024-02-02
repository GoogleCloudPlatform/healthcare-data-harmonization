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

import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.COMMENT;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.NEWLINE;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.SPACE;
import static com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.TerminalNode.Type.TEXT;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.error.ErrorStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.IndexedDepthFirstIterator.TreeChildReference;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleBaseVisitor;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.IntervalSet;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;

/** AntlrToTree parses Whistle text, and creates a mutable tree. */
public class AntlrToTree extends WhistleBaseVisitor<Void> {

  private Token[] tokens;
  private Map<ParseTree, BaseTreeNode> ruleToTree;
  private final Function<WhistleParser, RuleContext> parserRule;
  private final boolean throwLinterException;

  public AntlrToTree() {
    this.parserRule = WhistleParser::program;
    this.throwLinterException = true;
  }

  public AntlrToTree(
      Function<WhistleParser, RuleContext> parserRule, boolean throwLinterException) {
    this.parserRule =
        Objects.requireNonNull(parserRule, "The parser rule provided must not be null.");
    this.throwLinterException = throwLinterException;
  }

  public BaseTreeNode tree(String whistle, FileInfo info) {
    CharStream stream = CharStreams.fromString(whistle);
    WhistleLexer wstlLexer = new WhistleLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(wstlLexer);

    WhistleParser wstlParser = new WhistleParser(tokens);
    ErrorStrategy strategy = new ErrorStrategy(info);
    wstlParser.setErrorHandler(strategy);

    RuleContext r = parserRule.apply(wstlParser);

    this.tokens = tokens.getTokens().toArray(Token[]::new);

    this.ruleToTree = new HashMap<>();

    BaseTreeNode root = new InternalNode(r.getClass(), null, false);
    ruleToTree.put(r, root);

    RuntimeException supressedEx = null;

    try {
      r.accept(this);
    } catch (RuntimeException ex) {
      // ANTLR will continue parsing even if there is a syntax error, and this might make the syntax
      // structure incorrect, causing NPE or some other exceptions down the line. This catch is
      // meant to catch those NPEs/others, then check for syntax errors that caused them.
      supressedEx = ex;
    }

    if (strategy.hasIssues() && throwLinterException) {
      TranspilationException tex = new TranspilationException(strategy.getIssues());
      if (supressedEx != null) {
        tex.addSuppressed(supressedEx);
      }
      throw tex;
    } else if (supressedEx != null) {
      throw supressedEx;
    }

    return root;
  }

  /**
   * Recursively walks the Antlr tree, visiting all the children of the node, and builds a mutable
   * Ast tree using TreeNodes.
   *
   * @param node {@link RuleNode} the current node of the Antlr tree being visited.
   */
  @Override
  public Void visitChildren(RuleNode node) {
    int start;
    int end;
    // if visiting the root node, ensure the start and end include all tokens
    if (node.getParent() == null) {
      start = 0;
      end = tokens.length - 1;
    } else {
      start = node.getSourceInterval().a;
      end = node.getSourceInterval().b;
    }

    BaseTreeNode currentNode = ruleToTree.get(node);
    // the intervals where nodes are found, not including spaces or comments
    IntervalSet childrenIntervals =
        new IntervalSet(
            IntStream.range(0, node.getChildCount())
                .mapToObj(node::getChild)
                .map(ParseTree::getSourceInterval)
                .filter(Objects::nonNull)
                .collect(toImmutableList()));

    int lastChildIndex = -1;
    for (int i = start; i <= end; i++) {
      // the indices in the gaps between nodes contain either whitespaces or comments
      if (!childrenIntervals.contains(i)) {
        TerminalNode spaceNode;
        if (tokens[i].getText().strip().startsWith("//")) {
          spaceNode = new TerminalNode(tokens[i].getText(), currentNode, false, COMMENT);
        } else {
          spaceNode = new TerminalNode(tokens[i].getText(), currentNode, false, SPACE);
        }
        currentNode.asInternal().children.add(spaceNode);
        continue;
      }
      lastChildIndex++;
      ParseTree child = node.getChild(lastChildIndex);
      BaseTreeNode childNode;
      // Check if the child node is newline, terminal, or internal.
      if (child.getChildCount() == 0 && child.getText().equals("\n")) {
        childNode = new TerminalNode(child.getText(), currentNode, false, NEWLINE);
      } else if (child.getChildCount() == 0) {
        childNode = new TerminalNode(child.getText(), currentNode, false, TEXT);
      } else {
        childNode =
            ruleToTree.computeIfAbsent(
                child, pt -> new InternalNode(pt.getClass(), currentNode, false));
      }
      currentNode.asInternal().getChildren().add(childNode);
      child.accept(this);
      i = child.getSourceInterval().b;
    }

    return null;
  }

  /** Print the tree for debugging. */
  public static String toDebugString(BaseTreeNode root) {
    return renderDebugString(root, "", 0);
  }

  /** Print the tree as code. */
  public static String toCodeString(BaseTreeNode root) {
    return renderCode(root);
  }

  static String renderCode(BaseTreeNode node) {
    IndexedDepthFirstIterator codeWalker = node.dfsWalker();
    StringBuilder code = new StringBuilder();
    TreeChildReference entry = new TreeChildReference(node.asInternal(), -1);
    while (codeWalker.hasNext()) {
      if (entry.node().isTerminal() && !entry.node().asTerminal().value.equals("<EOF>")) {
        code.append(entry.node().asTerminal().value);
      }
      entry = codeWalker.next();
    }
    return code.toString();
  }

  private static String renderDebugString(BaseTreeNode node, String indent, int level) {
    String indicators = "abcdefghijklmnopqrstuvwxyz";
    if (isNonEmptyTerminalNode(node)) {
      return indent + "'" + node.asTerminal().value.replace("\n", "newline") + "'";
    }

    List<BaseTreeNode> children = node.asInternal().children;
    List<String> lines = new ArrayList<>(children.size() + 1);
    lines.add(indent + node.getOriginalNodeType().getSimpleName());
    indent += indicators.charAt(level % indicators.length()) + " ";
    for (BaseTreeNode child : children) {
      lines.add(renderDebugString(child, indent, level + 1));
    }
    return String.join("\n", lines);
  }

  private static boolean isNonEmptyTerminalNode(BaseTreeNode node) {
    return node.isTerminal() && !Strings.isNullOrEmpty(node.asTerminal().getValue());
  }
}
