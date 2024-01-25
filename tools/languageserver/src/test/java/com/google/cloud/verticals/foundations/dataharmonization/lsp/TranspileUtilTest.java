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

import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.createDiagnosticError;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TranspileUtil.TRANSPILER_SERVICE_DIAGNOSTICS;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.Transpiler;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;
import java.io.IOException;
import java.util.Set;
import org.eclipse.lsp4j.Diagnostic;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for TranspileUtil. */
@RunWith(JUnit4.class)
public class TranspileUtilTest {
  private static final String TEST_URI = "test/URI";
  TranspileUtil transpileUtil;
  DiagnosticMessageCollector diagnosticMessageCollector;
  LSPServer languageServer = new LSPServer(ImmutableSet.of(new ResourceLoader()));

  @Before
  public void setUp() {
    languageServer.connect(new MockLanguageClient());
    diagnosticMessageCollector = new DiagnosticMessageCollector();
    diagnosticMessageCollector.resetDocument(TEST_URI);
    transpileUtil = new TranspileUtil(diagnosticMessageCollector);
  }

  @Test
  public void diagnosticTest_single_transpile_error() throws IOException {
    String inputPath = "single_transpile_error.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);

    Diagnostic diagnostic =
        createDiagnosticError(
            new int[] {3, 15},
            new int[] {3, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected ';', expecting one of {'if', BOOL, 'merge', 'append', 'replace', 'extend',"
                + " '-', '*', '!', '(', '[', '{', INTEGER, IDENTIFIER (Examples of Identifiers"
                + " include, Function, Variable and Package names), STRING}");

    ImmutableSet<Diagnostic> expected = ImmutableSet.of(diagnostic);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }

  @Test
  public void diagnosticTest_no_transpile_error() throws IOException {
    String inputPath = "no_transpile_error.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertThat(response).isEmpty();
  }

  @Test
  public void diagnosticTest_multiline_transpile_error() throws IOException {
    String inputPath = "multiline_transpile_error.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);

    Diagnostic diagnostic1 =
        createDiagnosticError(
            new int[] {11, 15},
            new int[] {11, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "expecting INTEGER at ')'");

    Diagnostic diagnostic2 =
        createDiagnosticError(
            new int[] {11, 0},
            new int[] {11, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected input at badFunction(4--)");

    Diagnostic diagnostic3 =
        createDiagnosticError(
            new int[] {8, 15},
            new int[] {8, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "expecting one of {EOF, ';', NEWLINE} at '['");

    Diagnostic diagnostic4 =
        createDiagnosticError(
            new int[] {3, 15},
            new int[] {3, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected ';', expecting one of {'if', BOOL, 'merge', 'append', 'replace', 'extend',"
                + " '-', '*', '!', '(', '[', '{', INTEGER, IDENTIFIER (Examples of Identifiers"
                + " include, Function, Variable and Package names), STRING}");

    ImmutableSet<Diagnostic> expected =
        ImmutableSet.of(diagnostic1, diagnostic2, diagnostic3, diagnostic4);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }

  @Test
  public void diagnosticTest_undeclared_single_variable_error() throws IOException {
    String inputPath = "undeclared_single_variable_error.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);

    Diagnostic diagnosticSource =
        createDiagnosticError(
            new int[] {5, 13},
            new int[] {5, 27},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable undeclared_var is undeclared.");

    ImmutableSet<Diagnostic> expected = ImmutableSet.of(diagnosticSource);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }

  @Test
  public void diagnosticTest_undeclared_multi_variable_error() throws IOException {
    String inputPath = "undeclared_multi_variable_error.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);

    Diagnostic diagnostic1 =
        createDiagnosticError(
            new int[] {5, 13},
            new int[] {5, 29},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable undeclared_var_1 is undeclared.");

    Diagnostic diagnostic2 =
        createDiagnosticError(
            new int[] {14, 18},
            new int[] {14, 34},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable undeclared_var_2 is undeclared.");

    Diagnostic diagnostic3 =
        createDiagnosticError(
            new int[] {15, 0},
            new int[] {15, 16},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable undeclared_var_3 is undeclared.");

    ImmutableSet<Diagnostic> expected = ImmutableSet.of(diagnostic3, diagnostic2, diagnostic1);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }

  @Test
  public void diagnosticTest_newline_eof_errors() throws IOException {
    String inputPath = "newline_errors.wstl";
    String wstlInput = Resources.toString(Resources.getResource(inputPath), UTF_8);

    Diagnostic diagnostic1 =
        createDiagnosticError(
            new int[] {10, 5},
            new int[] {10, 6},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected '<EOF>', expecting one of {'if', BOOL, 'merge', 'append', 'replace',"
                + " 'extend', '-', '*', '!', '(', '[', '{', INTEGER, IDENTIFIER (Examples of"
                + " Identifiers include, Function, Variable and Package names), STRING}");

    Diagnostic diagnostic2 =
        createDiagnosticError(
            new int[] {7, 5},
            new int[] {7, 6},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected '\\n"
                + "', expecting one of {'if', BOOL, 'merge', 'append', 'replace', 'extend', '-',"
                + " '*', '!', '(', '[', '{', INTEGER, IDENTIFIER (Examples of Identifiers include,"
                + " Function, Variable and Package names), STRING}");

    Diagnostic diagnostic3 =
        createDiagnosticError(
            new int[] {4, 0},
            new int[] {4, 3},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected 'var', expecting '.'");

    Diagnostic diagnostic4 =
        createDiagnosticError(
            new int[] {2, 4},
            new int[] {2, 5},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected '\\n', expecting '.'");

    Diagnostic diagnostic5 =
        createDiagnosticError(
            new int[] {0, 23},
            new int[] {0, 24},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "unexpected '.', expecting NEWLINE");

    ImmutableSet<Diagnostic> expected =
        ImmutableSet.of(diagnostic1, diagnostic2, diagnostic3, diagnostic4, diagnostic5);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, new Transpiler());
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }

  @Test
  public void diagnosticTest_transpiler_mock_runtimeException() {
    String wstlInput = "text to mock a transpiler runtime exception";
    FileInfo fileInfo = FileInfo.newBuilder().setUrl(TEST_URI).build();
    Transpiler mockTranspiler = mock(Transpiler.class);
    when(mockTranspiler.transpile(wstlInput, fileInfo))
        .thenThrow(new RuntimeException("Mocking a transpiler runtime exception"));
    // TranspileUtil transpileUtil = new TranspileUtil(diagnosticMessageCollector);
    transpileUtil.generateDiagnosticsFromTranspiler(wstlInput, TEST_URI, mockTranspiler);
    diagnosticMessageCollector.publishCollectedDiagnosticsToClient(
        languageServer.getLanguageClient());

    ImmutableSet<Diagnostic> expected =
        ImmutableSet.of(
            createDiagnosticError(
                new int[] {0, 0},
                new int[] {0, 1},
                TRANSPILER_SERVICE_DIAGNOSTICS,
                "Mocking a transpiler runtime exception"));
    Set<Diagnostic> response =
        ((MockLanguageClient) languageServer.getLanguageClient()).getDiagnostics(TEST_URI);
    assertEquals(expected, response);
  }
}
