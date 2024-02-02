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

package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;


import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.BracketsRedundant;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.ConditionalRedundant;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.LinterRule;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SemicolonsRedundant;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingArrays;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingBlock;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingConditional;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingFunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingFunctionDef;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingOperator;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingStatement;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.SpacingTrailing;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleBaseVisitor;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/** Main linter class. */
public class Linter extends WhistleBaseVisitor<BaseTreeNode> {
  static FileSystemShim fs = new DefaultFileSystemShim();

  // Contains currently active rules.
  private List<LinterRule<?>> activeRules = new ArrayList<>();

  // Contains all default Linter rules.
  // LINT.IfChange(all_rules)
  // TODO(): add a topological sort so that rules are run in the correct order.
  private static final ImmutableList<LinterRule<?>> allRules =
      ImmutableList.of(
          new SpacingBlock(),
          new SpacingConditional(),
          new BracketsRedundant(),
          new SpacingOperator(),
          new SemicolonsRedundant(),
          new SpacingFunctionDef(),
          new SpacingStatement(),
          new ConditionalRedundant(),
          new SpacingTrailing(),
          new SpacingFunctionCall(),
          new SpacingArrays());

  private static final int DEFAULT_INDENT_SIZE = 2;
  // LINT.ThenChange(//depot/github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/tools/linter/README.md)
  @Parameters List<String> files = new ArrayList<>();

  // TODO (): Add an option to list all available rules.
  @Option(
      names = {"-i", "--include"},
      description = "Linter Rules to include.")
  private String include = "";

  @Option(
      names = {"-e", "--exclude"},
      description = "Linter Rules to exclude.")
  private String exclude = "";

  @Option(
      names = {"-in", "--indent"},
      description = "Indent size")
  private Integer tabsize = DEFAULT_INDENT_SIZE;

  public Linter() {
  }

  public static void main(String[] args) throws IOException {
    Linter linter = new Linter();
    new CommandLine(linter).parseArgs(args);
    List<String> includedRules = parseArguments(linter.include);
    List<String> excludedRules = parseArguments(linter.exclude);
    for (String filename : linter.files) {
      ImmutableList<LinterRule<?>> activeRules = linter.specifyRules(includedRules, excludedRules);
      BaseTreeNode result =
          linter.lint(
              fs.readString(filename),
              WhistleParser::program,
              FileInfo.getDefaultInstance(),
              activeRules,
              linter.tabsize);
      try (Writer outputWriter = fs.createWriter(filename)) {
        outputWriter.write(AntlrToTree.toCodeString(result));
      }
    }
  }

  /**
   * Applies only active linting rules to the given whistle code.
   *
   * @param whistle {@link String} the Whistle text to be formatted
   * @param parserRule {@link Function<WhistleParser, RuleContext>} Whistle grammar rule to be
   *     applied (program rule by default)
   * @param info {@link FileInfo} information on the Whistle file to be formatted
   * @param activeRules {@link List<LinterRule<?>>} list of linting rules to apply
   * @param indentSize indent size to apply
   * @return {@link BaseTreeNode} the root node of a mutable tree of formatted Whistle code
   */
  public BaseTreeNode lint(
      String whistle,
      Function<WhistleParser, RuleContext> parserRule,
      FileInfo info,
      List<LinterRule<?>> activeRules,
      int indentSize) {
    AntlrToTree antlrToTree = new AntlrToTree();
    BaseTreeNode root = antlrToTree.tree(whistle, info);
    this.activeRules = activeRules;
    LinterConstants.setIndent(indentSize);

    applyRules(root);

    return root;
  }

  /**
   * Overloaded lint method with default size.
   *
   * @param whistle {@link String} the Whistle text to be formatted
   * @param parserRule {@link Function<WhistleParser, RuleContext>} Whistle grammar rule to be
   *     applied (program rule by default)
   * @param info {@link FileInfo} information on the Whistle file to be formatted
   * @param activeRules {@link List<LinterRule<?>>} list of linting rules to apply
   * @return {@link BaseTreeNode} the root node of a mutable tree of formatted Whistle code
   */
  public BaseTreeNode lint(
      String whistle,
      Function<WhistleParser, RuleContext> parserRule,
      FileInfo info,
      List<LinterRule<?>> activeRules) {
    return lint(whistle, parserRule, info, activeRules, DEFAULT_INDENT_SIZE);
  }

  private void applyRules(BaseTreeNode root) {
    visitNodes(root);
  }

  private void visitNodes(BaseTreeNode treeNode) {
    if (treeNode.isInternal()) {
      for (BaseTreeNode child : treeNode.asInternal().getChildren()) {
        visitNodes(child);
      }
    }

    for (LinterRule<?> rule : activeRules) {
      if (treeNode.getOriginalNodeType() != null
          && rule.anchor().isAssignableFrom(treeNode.getOriginalNodeType())
          && rule.matchesWithAnchor(treeNode)) {
        rule.apply(treeNode);
      }
    }
  }

  private static List<String> parseArguments(String arg) {
    if (Strings.isNullOrEmpty(arg)) {
      return new ArrayList<>();
    }
    return Arrays.asList(arg.split(","));
  }

  public ImmutableList<LinterRule<?>> specifyRules(
      List<String> includedRules, List<String> excludedRules) throws IOException {
    if (includedRules.isEmpty() && excludedRules.isEmpty()) {
      return allRules;
    }
    boolean ruleFound = false;
    ArrayList<LinterRule<?>> activeRules = new ArrayList<>();
    for (String element : includedRules) {
      for (LinterRule<?> rule : allRules) {
        if (rule.toString().contains(element)) {
          activeRules.add(rule);
          ruleFound = true;
        }
      }
      if (!ruleFound) {
        throw new IOException(String.format("The linting rule %s does not exist.", element));
      }
    }
    // If includedRules is empty, but excludedRules is not, remove the excluded rules from the
    // default list of allRules.
    if (includedRules.isEmpty()) {
      activeRules = new ArrayList<>(allRules);
    }
    ruleFound = false;
    for (String element : excludedRules) {
      for (LinterRule<?> rule : allRules) {
        if (rule.getClass().getSimpleName().contains(element)) {
          activeRules.remove(rule);
          ruleFound = true;
        }
      }
      if (!ruleFound) {
        throw new IOException(String.format("The linting rule %s does not exist.", element));
      }
    }

    // Enforce having the SpacingBlock rule before the SpacingFunctionCall rule.
    boolean spacingBlockFound = false;
    for (LinterRule<?> rule : activeRules) {
      if (rule.getClass().equals(SpacingBlock.class)) {
        spacingBlockFound = true;
      }
      if (rule.getClass().equals(SpacingFunctionCall.class) && !spacingBlockFound) {
        throw new IOException(
            "The SpacingBlock rule must come before the SpacingFunctionCall rule.");
      }
    }
    return ImmutableList.copyOf(activeRules);
  }
}
