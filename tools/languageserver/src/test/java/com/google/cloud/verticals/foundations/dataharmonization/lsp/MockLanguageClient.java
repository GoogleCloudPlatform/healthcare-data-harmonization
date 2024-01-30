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

import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.services.LanguageClient;

/**
 * Mock client which is used in testing to determine diagnostics that may be published from a server
 * to a connected client.
 */
public class MockLanguageClient implements LanguageClient {
  Map<String, Set<Diagnostic>> diagnosticsMap = new HashMap<>();

  @Override
  public void telemetryEvent(Object object) {}

  @Override
  public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
    diagnosticsMap.put(diagnostics.getUri(), ImmutableSet.copyOf(diagnostics.getDiagnostics()));
  }

  public Set<Diagnostic> getDiagnostics(String uri) {
    return diagnosticsMap.get(uri);
  }

  @Override
  public void showMessage(MessageParams messageParams) {}

  @Override
  public CompletableFuture<MessageActionItem> showMessageRequest(
      ShowMessageRequestParams requestParams) {
    return null;
  }

  @Override
  public void logMessage(MessageParams message) {}
}
