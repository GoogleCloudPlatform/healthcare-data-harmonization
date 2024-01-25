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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static org.eclipse.lsp4j.MarkupKind.MARKDOWN;

import com.google.cloud.verticals.foundations.dataharmonization.Transpiler;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine.InitializedBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.lsp.SymbolLookup.SymbolWithPosition;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.DefaultFileSystemShim;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.FileSystemShim;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.Linter;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.AntlrToTree;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.ast.BaseTreeNode;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.rules.LinterRule;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.GoogleLogger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

/**
 * Implementation for {@link TextDocumentService} which defines actions from the Language Server
 * Protocol which are related to Text based documents.
 */
public class TextDocumentServiceImpl implements TextDocumentService {
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private final TranspileUtil transpileUtil;
  private final LSPServer languageServer;
  private final Map<String, String> documentTextMap;
  private final EngineUtil engineUtil;
  private final DiagnosticMessageCollector diagnosticMessageCollector;
  public FileSystemShim fs = new DefaultFileSystemShim();

  public TextDocumentServiceImpl(LSPServer server) {
    languageServer = server;
    documentTextMap = new HashMap<>();
    diagnosticMessageCollector = new DiagnosticMessageCollector();
    transpileUtil = new TranspileUtil(diagnosticMessageCollector);
    engineUtil = new EngineUtil(languageServer.getLoaders(), diagnosticMessageCollector);
  }

  /**
   * Implementation of a <a
   * href="https://microsoft.github.io/language-server-protocol/specifications/specification-3-17/#textDocument_completion"
   * >completion request</a>. This implementation will return all built-in functions.
   *
   * @param completionParams includes cursor position and text information sent from the client
   * @return a {@link CompletableFuture} of a list of {@link CompletionItem}
   */
  @Override
  public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(
      CompletionParams completionParams) {
    logger.atInfo().log("Completion request triggered. %s", completionParams);

    // Fetch the documentText from the documentMap
    String documentURI = completionParams.getTextDocument().getUri();
    String documentText = documentTextMap.get(documentURI);

    // Generate the substring prefix from which the autocomplete options will be created
    int lineNumber = completionParams.getPosition().getLine();
    int cursorPosition = completionParams.getPosition().getCharacter();

    ImmutableSet<String> variables =
        engineUtil.getDefinitionSymbolsFromPosition(documentURI, lineNumber, cursorPosition);

    // Get completion items based on the lineNumber, cursorPosition, text and the completionItems
    // which were last generated from a Whistle Engine instance.
    ImmutableList<CompletionItem> filteredItems =
        CompletionUtil.generateCompletionItems(
            documentText,
            lineNumber,
            cursorPosition,
            engineUtil.getPluginCompletionItems().column(documentURI),
            engineUtil.getUserDefinedCompletionItems().get(documentURI),
            variables);

    return CompletableFuture.supplyAsync(() -> Either.forLeft(filteredItems));
  }

  // TODO() Implement hover functionality
  /**
   * Implementation of a <a
   * href="https://microsoft.github.io/language-server-protocol/specifications/specification-3-17/#textDocument_hover"
   * >hover request</a>.
   *
   * @param params includes cursor position and text information sent from the client
   * @return a {@link CompletableFuture} of a {@link Hover} object
   */
  @Override
  public CompletableFuture<Hover> hover(HoverParams params) {
    logger.atInfo().log("Hover request triggered. %s", params);
    SymbolWithPosition symbol =
        engineUtil.getReferenceSymbolFromPosition(
            params.getTextDocument().getUri(),
            params.getPosition().getLine(),
            params.getPosition().getCharacter());
    Hover hoverResponse = new Hover();
    if (symbol != null) {
      String markdown = DocLookup.getMarkdownFromSymbol(symbol);
      if (markdown != null) {
        hoverResponse.setContents(new MarkupContent(MARKDOWN, markdown));
        return CompletableFuture.supplyAsync(() -> hoverResponse);
      }
    }
    hoverResponse.setContents(ImmutableList.of());
    return CompletableFuture.supplyAsync(() -> hoverResponse);
  }

  @Override
  public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>>
      definition(DefinitionParams params) {

    String documentURI = params.getTextDocument().getUri();
    int lineNumber = params.getPosition().getLine();
    int cursorPosition = params.getPosition().getCharacter();
    ImmutableList<Location> locations =
        engineUtil.fetchLocationFromPosition(documentURI, lineNumber, cursorPosition);

    logger.atInfo().log("Found the following location for the token, %s", locations);

    return CompletableFuture.supplyAsync(() -> Either.forLeft(locations));
  }

  @Override
  @Nullable
  public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
    try {
      logger.atInfo().log("Formatting request triggered. %s", params);
      URI filepath = new URI(params.getTextDocument().getUri());
      Linter linter = new Linter();

      // Providing empty arrays for include and exclude rules returns all rules by default.
      ImmutableList<LinterRule<?>> linterRules =
          linter.specifyRules(new ArrayList<>(), new ArrayList<>());
      int tabSize = params.getOptions().getTabSize();

      BaseTreeNode result =
          linter.lint(
              fs.readString(filepath.getPath()),
              WhistleParser::program,
              FileInfo.getDefaultInstance(),
              linterRules,
              tabSize);
      String formattedText = AntlrToTree.toCodeString(result);

      TextEdit textEdit =
          new TextEdit(
              new Range(new Position(0, 0), new Position(Integer.MAX_VALUE, Integer.MAX_VALUE)),
              formattedText);
      return CompletableFuture.supplyAsync(() -> Collections.singletonList(textEdit));
    } catch (Exception e) {
      logger.atSevere().withCause(e).log("Error while formatting whistle file.");
      return CompletableFuture.completedFuture(ImmutableList.<TextEdit>of());
    }
  }

  /** Called when a client opens a {@link TextDocumentItem} */
  @Override
  public void didOpen(DidOpenTextDocumentParams params) {
    logger.atInfo().log("File Opened. %s", params);
    // Save the opened document text.
    String documentURI = params.getTextDocument().getUri();
    String documentText = params.getTextDocument().getText();
    saveDocumentText(documentURI, documentText);
    // Init loaded function, target registries and completionItem maps for the document
    engineUtil.didOpen(documentURI);
    publishDiagnostics(documentURI);
  }

  /** Called when a client makes changes to a {@link TextDocumentItem} */
  @Override
  public void didChange(DidChangeTextDocumentParams params) {
    logger.atInfo().log("File Changed. %s", params);
    // Save the updated document text.
    String documentURI = params.getTextDocument().getUri();
    String documentText = params.getContentChanges().get(0).getText();
    saveDocumentText(documentURI, documentText);

    // Reset diagnostics for this document
    diagnosticMessageCollector.resetDocument(documentURI);
    transpileUtil.generateDiagnosticsFromTranspiler(documentText, documentURI, new Transpiler());
    // Resets the userDefined completion items, definition locations and imports for this document
    engineUtil.didChange(documentURI);
    // Load an engine instance, to create completion items for any imported plugins, and update
    // diagnostics to include any plugin import errors.
    engineUtil.loadEngine(documentURI, documentText, languageServer.getImportRoot());

    // TODO(): find a way to make this more responsive when large schema files are used.
    // addValidationDiagnostics(documentURI, documentText, diagnostics, builder);

    // Publish only this set of diagnostics
    diagnosticMessageCollector.publishDiagnosticsForDocument(
        documentURI, languageServer.getLanguageClient());
  }

  /** Called when a client makes closes a {@link TextDocumentItem} */
  @Override
  public void didClose(DidCloseTextDocumentParams params) {
    logger.atInfo().log("Closing file. %s", params);
    // Evict the closed documentURI from the document map.
    String documentURI = params.getTextDocument().getUri();
    documentTextMap.remove(documentURI);
    engineUtil.didClose(documentURI);
    diagnosticMessageCollector.clearDiagnostics(documentURI);
  }

  /** Called when a client saves a {@link TextDocumentItem} */
  @Override
  public void didSave(DidSaveTextDocumentParams params) {
    logger.atInfo().log("File saved, %s", params);
    String documentURI = params.getTextDocument().getUri();
    saveDocumentText(documentURI, params.getText());
    publishDiagnostics(documentURI);
  }

  /**
   * Resets, generates and then publish diagnostics for a documentURI and any documents importing
   * this documentURI.
   *
   * @param documentURI the document being for which diagnostics are being published.
   */
  private void publishDiagnostics(String documentURI) {
    // Get the list of docs which are importing this doc. This list will have been generated on
    // document open and updated on document change
    ImmutableSet<String> importers =
        engineUtil.getDocumentImportURIs().entrySet().stream()
            .filter(e -> e.getValue().contains(documentURI))
            .map(Entry::getKey)
            .collect(toImmutableSet());

    // Reset diagnostics
    diagnosticMessageCollector.resetDocument(documentURI);
    // TODO(): This will need some form of e2e/integration testing to test more dynamic
    // use of the language server.
    importers.forEach(diagnosticMessageCollector::resetDocument);

    // Generate diagnostics for this doc.
    generateAllDiagnosticsForDocument(documentURI, documentTextMap.get(documentURI));
    // Generate diagnostics for importer docs
    documentTextMap.entrySet().stream()
        .filter(e -> importers.contains(e.getKey()))
        .collect(toImmutableMap(Entry::getKey, Entry::getValue))
        .forEach(this::generateAllDiagnosticsForDocument);

    // Publish updated diagnostics
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
  }

  /**
   * Generates diagnostics from the Transpiler, Engine and Validation services for a given
   * documentURI and documentText. The provided diagnostics map will save generated Diagnostics to
   * the appropriate key.
   *
   * @param documentURI documentURI for the body of text to be processed.
   * @param documentText body of text to be processed.
   */
  private void generateAllDiagnosticsForDocument(String documentURI, String documentText) {
    transpileUtil.generateDiagnosticsFromTranspiler(documentText, documentURI, new Transpiler());
    InitializedBuilder builder =
        engineUtil.loadEngine(documentURI, documentText, languageServer.getImportRoot());
  }

  /**
   * Helper method which updates the documentMap to store the documentText for each documentURI key.
   */
  private void saveDocumentText(String documentURI, String documentText) {
    documentTextMap.put(documentURI, documentText);
  }
}
