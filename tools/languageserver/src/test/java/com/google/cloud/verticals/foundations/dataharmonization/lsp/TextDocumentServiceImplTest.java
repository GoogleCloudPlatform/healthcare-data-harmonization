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

import static com.google.cloud.verticals.foundations.dataharmonization.lsp.EngineUtil.ENGINE_INIT_DIAGNOSTICS;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.changeTextDocument;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.createDiagnosticError;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.getCompletionItems;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.openTextDocument;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.saveTextDocument;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TranspileUtil.TRANSPILER_SERVICE_DIAGNOSTICS;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.mockito.Mockito.spy;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.FileSystemShim;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.FormattingOptions;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mockito;

/** Tests for textDocumentServiceImpl which only includes builtin plugins. */
@RunWith(JUnit4.class)
public class TextDocumentServiceImplTest {

  private LSPServer languageServer;
  private TextDocumentServiceImpl textDocumentService;
  private static final Plugin builtins = new Builtins();
  private ResourceLoader spyResourceLoader;

  @Before
  public void setUp() {
    ResourceLoader resourceLoader = new ResourceLoader();
    spyResourceLoader = spy(resourceLoader);
    languageServer = new LSPServer(ImmutableSet.of(spyResourceLoader));
    textDocumentService = new TextDocumentServiceImpl(languageServer);
    languageServer.connect(new MockLanguageClient());
  }

  /**
   * Test which checks if autocomplete on an empty whitespace returns all built in functions, and
   * the single user-defined function.
   */
  @Test
  public void completionTest_prefix_autocomplete_whitespace() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 1, 0);
    assertThat(returnedFunctions)
        // +1 to account for user-defined function in the test file1
        .hasSize(
            (int) builtins.getFunctions().stream().filter(this::isFunctionLoaded).count()
                + builtins.getTargets().size()
                + 1);
  }

  private boolean isFunctionLoaded(CallableFunction fxn) {
    return (fxn.getDebugInfo() != null
            && (fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.NATIVE)
                || fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.DECLARED)))
        || !fxn.getSignature().getInheritsParentVars();
  }

  @Test
  public void completionTest_prefix_autocomplete_blank_line() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 3, 4);

    // prefix: "ca"
    String[] expectedFunctionNames = {
      "calculateElapsedDuration", "calculateNewDateTime", "callFn", "callPackageFn"
    };
    assertThat(returnedFunctions).containsExactlyElementsIn(expectedFunctionNames);
  }

  @Test
  public void completionTest_prefix_autocomplete_with_non_blank_line() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 6, 6);

    // prefix: "ra"
    String[] expectedFunctionNames = {"range", "range"};
    assertThat(returnedFunctions).containsExactlyElementsIn(expectedFunctionNames);
  }

  @Test
  public void completionTest_prefix_autocomplete_with_tabs() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 5, 8);

    // prefix: "call"
    String[] expectedFunctionNames = {"callFn", "callPackageFn"};
    assertThat(returnedFunctions).containsExactlyElementsIn(expectedFunctionNames);
  }

  @Test
  public void completionTest_prefix_autocomplete_package() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 7, 11);
    // prefix: "package::ca" . Should return nothing.
    assertThat(returnedFunctions).isEmpty();
  }

  @Test
  public void completionTest_prefix_autocomplete_startline() throws IOException {
    String inputPath = "prefix_autocomplete.wstl";
    List<String> returnedFunctions = getCompletionItems(textDocumentService, inputPath, 8, 1);

    // prefix: "a"
    String[] expectedFunctionNames = {"and", "arrayOf", "absPath"};
    assertThat(returnedFunctions).containsExactlyElementsIn(expectedFunctionNames);
  }

  @Test
  public void import_without_prefix_should_be_checked_in_root_dir() throws IOException {
    String inputPath = "import_without_prefix_importer.wstl";

    openTextDocument(textDocumentService, inputPath);

    Diagnostic expectedDiagnostic =
        createDiagnosticError(
            new int[] {1, 0},
            new int[] {1, 44},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import res:///import_without_prefix_importee.wstl\n"
                + "/import_without_prefix_importee.wstl was not found.");

    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///import_without_prefix_importer.wstl");

    assertThat(publishedDiagnostics).containsExactly(expectedDiagnostic);

    // Set the root directory. which removes exception.
    languageServer.setImportRoot("/import_without_prefix/");
    openTextDocument(textDocumentService, inputPath);

    Set<Diagnostic> publishedDiagnosticsAfterchanges =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///import_without_prefix_importer.wstl");

    assertThat(publishedDiagnosticsAfterchanges).isEmpty();
  }

  @Test
  public void import_without_prefix_should_be_rechecked_after_file_changes() throws IOException {
    String inputPath = "import_change_importer.wstl";

    openTextDocument(textDocumentService, inputPath);

    Diagnostic expectedDiagnostic =
        createDiagnosticError(
            new int[] {1, 0},
            new int[] {1, 46},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import res:///import_without_prefix_importee.wstl\n"
                + "/import_without_prefix_importee.wstl was not found.");

    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///import_change_importer.wstl");

    assertThat(publishedDiagnostics).containsExactly(expectedDiagnostic);

    // Change the import path to one without a prefix so that it will find this file in the root
    // directory, which removes exception.
    List<TextDocumentContentChangeEvent> changeEvents = new ArrayList<>();
    changeEvents.add(
        new TextDocumentContentChangeEvent(
            null, null, "\nimport \"import_without_prefix_importee.wstl\";\n\nname: \"John\";"));
    languageServer.setImportRoot("/import_without_prefix/");
    changeTextDocument(textDocumentService, inputPath, changeEvents);

    Set<Diagnostic> publishedDiagnosticsAfterChange =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///import_change_importer.wstl");

    assertThat(publishedDiagnosticsAfterChange).isEmpty();
  }

  // Test to check if bad imports are added to the diagnostics, along with any transpilation issues.
  @Test
  public void bad_import_diagnostics_test() throws IOException {
    String inputPath = "engine_bad_import.wstl";

    Diagnostic expectedDiagnostic1 =
        createDiagnosticError(
            new int[] {6, 0},
            new int[] {6, 29},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable this_should_also_be_diagnosed is undeclared.");

    Diagnostic expectedDiagnostic2 =
        createDiagnosticError(
            new int[] {5, 0},
            new int[] {5, 24},
            TRANSPILER_SERVICE_DIAGNOSTICS,
            "Variable this_should_be_diagnosed is undeclared.");

    Diagnostic expectedDiagnostic3 =
        createDiagnosticError(
            new int[] {2, 0},
            new int[] {2, 113},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import"
                + " class:///com.google.cloud.verticals.foundations.dataharmonization.plugins.dataflow.ThisIsAImportException\n"
                + "Class not found:"
                + " com.google.cloud.verticals.foundations.dataharmonization.plugins.dataflow.ThisIsAImportException."
                + " Make sure it is in the current classpath.");

    Diagnostic expectedDiagnostic4 =
        createDiagnosticError(
            new int[] {3, 0},
            new int[] {3, 23},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import res:///FileDNE.wstl\n" + "/FileDNE.wstl" + " was not found.");

    PublishDiagnosticsParams expected =
        new PublishDiagnosticsParams(
            "res:///engine_bad_import.wstl",
            Arrays.asList(
                expectedDiagnostic1,
                expectedDiagnostic2,
                expectedDiagnostic3,
                expectedDiagnostic4));

    openTextDocument(textDocumentService, inputPath);
    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///engine_bad_import.wstl");

    assertThat(publishedDiagnostics).containsExactlyElementsIn(expected.getDiagnostics());
  }

  // Test to check if transpile run time exceptions do not prevent engine loading and generating
  // autocompletes for a local function
  @Test
  public void autoComplete_partial_transpile_get_local_completion_items() throws IOException {
    String inputPath = "autocomplete_partial_transpile.wstl";
    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 24, 3);

    String[] expectedFunctionNames = {"localFunction"};
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedFunctionNames);
  }

  @Test
  public void diagnosticTest_importException_from_an_imported_file() throws IOException {
    String importerPath = "import_exception_in_imported_file_importer.wstl";
    String importeePath = "import_exception_in_imported_file_importee.wstl";
    openTextDocument(textDocumentService, importerPath);
    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///" + importerPath);

    Diagnostic expectedDiagnostic =
        createDiagnosticError(
            new int[] {0, 0},
            new int[] {0, 2},
            ENGINE_INIT_DIAGNOSTICS,
            "Errors in imported files: [res:///import_exception_in_imported_file_importee.wstl]");

    assertThat(publishedDiagnostics).containsExactly(expectedDiagnostic);

    Set<Diagnostic> importeePublishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///" + importeePath);

    Diagnostic expectedImporteeDiagnostic =
        createDiagnosticError(
            new int[] {1, 0},
            new int[] {1, 21},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import res:///ThisImportDNE\n/ThisImportDNE was not found.");

    assertThat(importeePublishedDiagnostics).containsExactly(expectedImporteeDiagnostic);
  }

  @Test
  public void diagnosticTest_importee_changes_updates_importer_diagnostics() throws IOException {
    String importerPath = "import_exception_in_imported_file_importer.wstl";
    String importeePath = "import_exception_in_imported_file_importee.wstl";
    openTextDocument(textDocumentService, importerPath);
    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///" + importerPath);
    Diagnostic expectedDiagnostic =
        createDiagnosticError(
            new int[] {0, 0},
            new int[] {0, 2},
            ENGINE_INIT_DIAGNOSTICS,
            "Errors in imported files: [res:///import_exception_in_imported_file_importee.wstl]");
    assertThat(publishedDiagnostics).containsExactly(expectedDiagnostic);

    Set<Diagnostic> importeePublishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///" + importeePath);

    Diagnostic expectedImporteeDiagnostic =
        createDiagnosticError(
            new int[] {1, 0},
            new int[] {1, 21},
            ENGINE_INIT_DIAGNOSTICS,
            "Error processing import res:///ThisImportDNE\n/ThisImportDNE was not found.");
    assertThat(importeePublishedDiagnostics).containsExactly(expectedImporteeDiagnostic);

    // Mock a change in the importee file which should clear the diagnostic error message
    ImportPath importeeMockPath = ImportPath.of("res", Path.of("/" + importeePath), Path.of("/"));
    Mockito.doReturn(new byte[0]).when(spyResourceLoader).load(importeeMockPath);

    openTextDocument(textDocumentService, importeePath);
    saveTextDocument(textDocumentService, importeePath, "");

    // Assert no errors present
    assertThat(
            ((MockLanguageClient) languageServer.getLanguageClient())
                .getDiagnostics("res:///" + importerPath))
        .isEmpty();
    assertThat(
            ((MockLanguageClient) languageServer.getLanguageClient())
                .getDiagnostics("res:///" + importeePath))
        .isEmpty();

    // Default back to normal behaviour
    Mockito.doCallRealMethod().when(spyResourceLoader).load(importeeMockPath);
    openTextDocument(textDocumentService, importeePath);
    assertThat(
            ((MockLanguageClient) languageServer.getLanguageClient())
                .getDiagnostics("res:///" + importerPath))
        .containsExactly(expectedDiagnostic);
    assertThat(
            ((MockLanguageClient) languageServer.getLanguageClient())
                .getDiagnostics("res:///" + importeePath))
        .containsExactly(expectedImporteeDiagnostic);
  }

  @Test
  public void diagnosticTest_unique_var_field_options_enabled() throws IOException {
    String inputPath = "engine_init_unique_vars_fields.wstl";
    openTextDocument(textDocumentService, inputPath);
    Set<Diagnostic> publishedDiagnostics =
        ((MockLanguageClient) languageServer.getLanguageClient())
            .getDiagnostics("res:///engine_init_unique_vars_fields.wstl");

    Diagnostic expectedDiagnostic =
        createDiagnosticError(
            new int[] {6, 8},
            new int[] {6, 34},
            ENGINE_INIT_DIAGNOSTICS,
            "Fields and variables cannot share the same name within the same context, cannot name"
                + " variable \"x\" at 7:8-7:34, previously found field at 3:0-3:4");

    assertThat(publishedDiagnostics).containsExactly(expectedDiagnostic);
  }

  @Test
  public void formattingTest_should_format_entire_document() throws IOException {
    String inputPath = "format_entire_doc_input.wstl";
    String outputPath = "format_entire_doc_output.wstl";
    int tabSize = 5;

    FileSystemShim fs = new TestFileSystemShim();
    textDocumentService.fs = fs;

    TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri("/" + inputPath);

    FormattingOptions formattingOptions = new FormattingOptions();
    formattingOptions.setTabSize(tabSize);

    DocumentFormattingParams params = new DocumentFormattingParams();
    params.setTextDocument(textDocumentIdentifier);
    params.setOptions(formattingOptions);

    List<? extends TextEdit> textEdit = textDocumentService.formatting(params).join();
    assertThat(textEdit).isNotEmpty();
    String formattedText = textEdit.get(0).getNewText();

    InputStream is = TextDocumentServiceImplTest.class.getResourceAsStream("/" + outputPath);
    assertThat(is).isNotNull();
    String expectedFormattedText = new String(is.readAllBytes(), UTF_8);

    assertThat(formattedText).contains(expectedFormattedText);
  }

  @Test
  public void formattingTest_should_not_format_if_code_does_not_compile() throws IOException {
    String inputPath = "format_code_that_does_not_compile_input.wstl";
    int tabSize = 5;

    FileSystemShim fs = new TestFileSystemShim();
    textDocumentService.fs = fs;

    TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri("/" + inputPath);

    FormattingOptions formattingOptions = new FormattingOptions();
    formattingOptions.setTabSize(tabSize);

    DocumentFormattingParams params = new DocumentFormattingParams();
    params.setTextDocument(textDocumentIdentifier);
    params.setOptions(formattingOptions);

    List<? extends TextEdit> textEdit = textDocumentService.formatting(params).join();
    assertThat(textEdit).isEmpty();
  }
}
