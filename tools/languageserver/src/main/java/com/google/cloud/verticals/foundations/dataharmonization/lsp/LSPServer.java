/*
 * Copyright 2020 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.lsp.exception.CloseConnectionException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.flogger.GoogleLogger;
import com.google.gson.JsonObject;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SaveOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.TextDocumentSyncOptions;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * Implementation for {@link LanguageServer} and {@link LanguageClientAware} which are used to
 * create a connection to clients, using the Language Server Protocol.
 */
public class LSPServer implements LanguageServer, LanguageClientAware {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();
  private final TextDocumentServiceImpl textDocumentService;
  private LanguageClient languageClient;
  private Set<Loader> loaders = new HashSet<>();
  private String importRoot;

  public LSPServer() {
    textDocumentService = new TextDocumentServiceImpl(this);
  }
  /** Constructor which will allow loading non-default loaders. */
  public LSPServer(Set<Loader> loaders) {
    this();
    this.loaders = loaders;
  }

  // Initialize the language server and set server capabilities.
  @Override
  public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
    logger.atInfo().log("Initialize server capabilities. %s", params);
    InitializeResult initializeResult = new InitializeResult(new ServerCapabilities());

    // Set completion capabilities
    initializeResult.getCapabilities().setCompletionProvider(new CompletionOptions());

    // TODO() Implement hover
    initializeResult.getCapabilities().setHoverProvider(true);

    // Set TextDocumentSyncOptions
    initializeResult.getCapabilities().setTextDocumentSync(getTextDocumentSyncOptions());

    // Enable go to definition
    initializeResult.getCapabilities().setDefinitionProvider(true);

    // Enable document formatting.
    initializeResult.getCapabilities().setDocumentFormattingProvider(true);

    Object initializationOptions = params.getInitializationOptions();
    if (initializationOptions instanceof JsonObject) {
      JsonObject jsonObject = (JsonObject) initializationOptions;

      // Check if the JSON object has the "importRoot" key
      if (jsonObject.has("importRoot")) {
        this.importRoot = jsonObject.get("importRoot").getAsString();
      }
    } else {
      logger.atWarning().log(
          "Initialization options are expected to be of type JsonObject, but instead received a"
              + " value of type %s",
          initializationOptions.getClass().getName());
    }

    return CompletableFuture.completedFuture(initializeResult);
  }

  /**
   * Helper method do set configurations for {@link TextDocumentSyncOptions}.
   *
   * @return TextDocumentSyncOptions
   */
  private TextDocumentSyncOptions getTextDocumentSyncOptions() {
    // Set Document sync options for open, close, change and save.
    TextDocumentSyncOptions textDocumentSyncOptions = new TextDocumentSyncOptions();

    // Send file open and close notifications.
    textDocumentSyncOptions.setOpenClose(true);

    // Set document sync kind to full, to receive the full text on document changes.
    textDocumentSyncOptions.setChange(TextDocumentSyncKind.Full);

    // Set save options to return text on save notifications.
    textDocumentSyncOptions.setSave(new SaveOptions(true));
    return textDocumentSyncOptions;
  }

  // This responds to an event from the client, which is meant to shut the server down,
  // but not close the connection. A client can only send an exit request after sending a shutdown
  // request. In this implementation this method has no affect as of now.
  @Override
  public CompletableFuture<Object> shutdown() {
    return null;
  }

  // Close the connection to the client with a status code of 0
  @Override
  public void exit() {
    throw new CloseConnectionException(0);
  }

  @Override
  public TextDocumentService getTextDocumentService() {
    return textDocumentService;
  }

  // Not implemented by the Jupyter client, which returns null for workspaceFolders
  @Override
  public WorkspaceService getWorkspaceService() {
    return new WorkSpaceServiceImpl();
  }

  // Sets client which initiated a connection
  @Override
  public void connect(LanguageClient client) {
    logger.atInfo().log("Connecting to client");
    this.languageClient = client;
  }

  public LanguageClient getLanguageClient() {
    return this.languageClient;
  }

  public Set<Loader> getLoaders() {
    return loaders;
  }

  public String getImportRoot() {
    return this.importRoot;
  }

  @VisibleForTesting
  void setImportRoot(String importRoot) {
    this.importRoot = importRoot;
  }
}
