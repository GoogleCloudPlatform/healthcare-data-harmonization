/*
 * Copyright 2022 Google LLC.
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

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.services.LanguageClient;

/** Class which provides methods for with handling LSP {@link Diagnostic}s. */
public class DiagnosticMessageCollector {
  private final Map<String, Set<Diagnostic>> documentDiagnosticsMap = new HashMap<>();

  public void addDiagnosticsToDocumentURI(String documentURI, Diagnostic diagnosticToAdd) {
    documentDiagnosticsMap
        .computeIfAbsent(documentURI, k -> new LinkedHashSet<>())
        .add(diagnosticToAdd);
  }

  /**
   * Create a 0-based {@link Diagnostic} object from the provided inputs.
   *
   * @param startLine Start line
   * @param startCol Start column
   * @param endLine End line
   * @param endCol Endi column
   * @param message The message for this {@link Diagnostic} object.
   * @param source The source where this {@link Diagnostic} originated from
   * @param severity The {@link DiagnosticSeverity} of the {@link Diagnostic} object.
   * @return A {@link Diagnostic} object.
   */
  public static Diagnostic createDiagnostic(
      int startLine,
      int startCol,
      int endLine,
      int endCol,
      String message,
      String source,
      DiagnosticSeverity severity) {
    Position startPosition = new Position(startLine, startCol);
    Position endPosition = new Position(endLine, endCol);
    return new Diagnostic(new Range(startPosition, endPosition), message, severity, source);
  }

  /**
   * Resets the set of {@link Diagnostic}s that are being tracked for a given documentURI. This
   * function is used whenever we regenerate diagnostics (which are generated from running
   * transpiler, engine and validation services against a document) for a document and is done when
   * the language server receives a document opened, changed and saved request for a given document.
   *
   * @param documentURI documentURI for which diagnostics will be reset.
   */
  public void resetDocument(String documentURI) {
    documentDiagnosticsMap.put(documentURI, new LinkedHashSet<>());
  }

  /**
   * Used to stop tracking {@link Diagnostic}s for a given documentURI. Used whenever the Language
   * Server receives a documentClose command for a given documentURI.
   *
   * @param docURI documentURI which was closed.
   */
  public void clearDiagnostics(String docURI) {
    documentDiagnosticsMap.remove(docURI);
  }

  /**
   * Publishes diagnostics for each documentURI using the provided LSP client.
   *
   * @param client An LSP client.
   */
  public void publishCollectedDiagnosticsToClient(LanguageClient client) {
    documentDiagnosticsMap
        .keySet()
        .forEach(documentURI -> publishDiagnosticsForDocument(documentURI, client));
  }

  /**
   * Publishes a single set of {@link Diagnostic} items for a given documentURI using the provided
   * {@link LanguageClient}.
   *
   * @param documentURI documentURI whose Diagnostics need to be published.
   * @param client an LSP client.
   */
  void publishDiagnosticsForDocument(String documentURI, LanguageClient client) {
    PublishDiagnosticsParams diagnosticsParams = new PublishDiagnosticsParams();
    diagnosticsParams.setUri(documentURI);
    diagnosticsParams.setDiagnostics(ImmutableList.copyOf(documentDiagnosticsMap.get(documentURI)));
    client.publishDiagnostics(diagnosticsParams);
  }
}
