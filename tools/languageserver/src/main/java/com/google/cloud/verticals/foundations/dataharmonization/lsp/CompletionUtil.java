/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.eclipse.lsp4j.MarkupKind.MARKDOWN;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.InsertReplaceEdit;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

/**
 * Class which holds static helper methods that create Language Server {@link CompletionItem}s from
 * Whistle Domain Objects.
 */
public final class CompletionUtil {

  private static final String BUILT_IN_PACKAGE_NAME = "builtins";
  private static final Pattern WHISTLE_NON_IDENTIFIER_PATTERN = Pattern.compile("[^$a-zA-Z_0-9:.]");
  private static final String ARG_TYPE_FOR_USER_DEFINED_FUNC = "Data";

  /**
   * Helper method which creates a {@link CompletionItem} given a {@link CallableFunction} and a
   * plugin package name.
   *
   * @param fxn The CallableFunction for which a CompletionItem will be created.
   * @param packageName The plugin package name to which this function belongs.
   * @return A {@link CompletionItem} used by the TextDocumentService.
   */
  static CompletionItem createCompletionItemFromCallableFunction(
      CallableFunction fxn,
      String packageName,
      String rootConfigPackageName,
      boolean isUserDefinedFuncs) {
    String fxnName = fxn.getName();
    String fxnSignature = fxn.getSignature().toString();
    // Built ins and local context functions do not need a package reference.
    String functionPackagePrefix =
        packageName.equals(BUILT_IN_PACKAGE_NAME)
                || fxn.getDebugInfo().getPackage().equals(rootConfigPackageName)
            ? ""
            : packageName + "::";

    String completionSignature;
    if (!isUserDefinedFuncs) {
      completionSignature = SignatureLookup.getSignatureFromCallableFunction(fxn);
    } else {
      completionSignature = getCompletionSignature(fxn);
    }
    return createCompletionItem(
        functionPackagePrefix + fxnName,
        functionPackagePrefix + completionSignature,
        functionPackagePrefix + fxnSignature,
        DocLookup.getMarkdownFromCallableFunction(fxn));
  }

  private static String getCompletionSignature(CallableFunction fxn) {
    ImmutableList<String> args =
        ((WhistleFunction) fxn)
            .getProto().getArgsList().stream()
                .map(arg -> ARG_TYPE_FOR_USER_DEFINED_FUNC + " " + arg.getName())
                .collect(toImmutableList());
    StringBuilder signature =
        new StringBuilder()
            .append(fxn.getName())
            .append("(")
            .append(String.join(", ", args))
            .append(")");
    return signature.toString();
  }

  static CompletionItem createCompletionItemFromTargetConstructor(
      Constructor targetConstructor, String packageName) {
    String targetName = targetConstructor.getName();
    String targetPrefix = packageName.equals(BUILT_IN_PACKAGE_NAME) ? "" : packageName + "::";

    return createCompletionItem(
        targetPrefix + targetName, targetPrefix + targetName, targetPrefix + targetName, null);
  }

  private static CompletionItem createCompletionItem(
      String label, String insertText, String detail, String documentation) {
    CompletionItem completionItem = new CompletionItem();
    completionItem.setLabel(label);
    completionItem.setInsertText(insertText);
    completionItem.setKind(CompletionItemKind.Function);
    completionItem.setDetail(detail);
    if (documentation != null) {
      completionItem.setDocumentation(new MarkupContent(MARKDOWN, documentation));
    }
    return completionItem;
  }

  private static CompletionItem createVariableCompletionItem(String variableName) {
    CompletionItem completionItem = new CompletionItem();
    completionItem.setLabel(variableName);
    completionItem.setKind(CompletionItemKind.Variable);
    completionItem.setInsertText(variableName);
    return completionItem;
  }

  /**
   * Helper function which returns the list of autocomplete options at a given line and cursor
   * position.
   *
   * @param documentText the documentText.
   * @param lineNumber line number at which this request is being made.
   * @param cursorPosition cursor position where this request is being made.
   * @param pluginCompletionItems map of plugins to set of completionItems for any plugins loaded in
   *     this document.
   * @param userCompletionItems user defined completion items defined in this package
   * @return List of {@link CompletionItem}s.
   */
  public static ImmutableList<CompletionItem> generateCompletionItems(
      String documentText,
      int lineNumber,
      int cursorPosition,
      Map<String, Set<CompletionItem>> pluginCompletionItems,
      Set<CompletionItem> userCompletionItems,
      Set<String> variableItems) {
    // Return the substring prefix from which autocomplete options will be generated
    String prefix = generatePrefix(documentText, lineNumber, cursorPosition);

    // Fetch the plugin functions loaded for the documentURI and filter by the prefix string
    ImmutableList<CompletionItem> pluginItems =
        pluginCompletionItems.values().stream()
            .flatMap(Set::stream)
            .collect(toImmutableList())
            .stream()
            .filter(completionItem -> completionItem.getLabel().startsWith(prefix))
            .collect(toImmutableList());

    pluginItems.forEach(ci -> setTextEdit(ci, lineNumber, cursorPosition, prefix));

    ImmutableList<CompletionItem> userItems =
        userCompletionItems.stream()
            .filter(completionItem -> completionItem.getLabel().startsWith(prefix))
            .collect(toImmutableList());

    userItems.forEach(ci -> setTextEdit(ci, lineNumber, cursorPosition, prefix));

    ImmutableList<CompletionItem> variableCompletionItems =
        variableItems.stream()
            .filter(variableName -> variableName.startsWith(prefix))
            .map(CompletionUtil::createVariableCompletionItem)
            .collect(toImmutableList());

    variableCompletionItems.forEach(ci -> setTextEdit(ci, lineNumber, cursorPosition, prefix));

    return ImmutableList.<CompletionItem>builder()
        .addAll(pluginItems)
        .addAll(userItems)
        .addAll(variableCompletionItems)
        .build();
  }

  /**
   * Helper function which gets used during an auto-completion action. This function takes as input
   * the document text, line number and the cursor position from where an auto-complete action was
   * triggered. It returns the prefix string, based on the line and cursor position, which will then
   * be used to generate auto-complete options.
   */
  private static String generatePrefix(String documentText, int lineNumber, int cursorPosition) {
    List<String> documentLines = Splitter.onPattern("\\r?\\n").splitToList(documentText);
    String currentLine = documentLines.get(lineNumber);

    // Find the first non-identifier char to the left of the current cursor position,to determine
    // the prefix start index.
    Matcher m = WHISTLE_NON_IDENTIFIER_PATTERN.matcher(currentLine);
    int startIndex = 0;
    m = m.region(0, cursorPosition);
    while (m.find()) {
      startIndex = m.end();
    }
    // Return the substring prefix from which autocomplete options will be generated
    return currentLine.substring(startIndex, cursorPosition);
  }

  /**
   * Sets the {@link TextEdit} parameter of the {@link CompletionItem} based on the position and the
   * prefix where the completion request is generated. Both textEdit and insertReplaceEdit fields
   * are set, and either can be used by the client depending on how it implements completion
   * operations. See the lsp spec for more details on how a client may use this value.
   *
   * @param completionItem The incoming {@link CompletionItem}.
   * @param lineNumber Line number where this request originated.
   * @param cursorPosition Cursor position where this request originated.
   * @param prefix The prefix present at the position this request is generated.
   */
  private static void setTextEdit(
      CompletionItem completionItem, int lineNumber, int cursorPosition, String prefix) {

    // A special textEdit parameter which provides values for both an insert and a replace
    // operation.
    // To enable replace on completion in vscode, set "editor.suggest.insertMode":"replace"
    // in the vscode client, the default the mode is "insert".
    InsertReplaceEdit insertReplaceEdit =
        new InsertReplaceEdit(
            completionItem.getInsertText(),
            new Range(
                new Position(lineNumber, cursorPosition - prefix.length()),
                new Position(lineNumber, cursorPosition)),
            new Range(
                new Position(lineNumber, cursorPosition - prefix.length()),
                new Position(
                    lineNumber,
                    cursorPosition - prefix.length() + completionItem.getInsertText().length())));

    completionItem.setTextEdit(Either.forRight(insertReplaceEdit));
  }
}
