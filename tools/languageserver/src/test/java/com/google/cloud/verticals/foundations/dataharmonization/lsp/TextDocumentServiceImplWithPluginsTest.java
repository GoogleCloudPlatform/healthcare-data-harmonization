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

import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.getCompletionItems;
import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.getCompletionSignature;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.LoggingPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.GoogleLogger;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for TextDocumentServiceImpl that include 3P plugins. These tests are not run in blaze since
 * some dependencies rely on dataflow, which is not completely buildable in blaze as of the time
 * this test class was written.
 */
@RunWith(JUnit4.class)
public class TextDocumentServiceImplWithPluginsTest {

  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  private LSPServer languageServer;
  private TextDocumentServiceImpl textDocumentService;
  private static final Plugin builtins = new Builtins();
  private static final LoggingPlugin loggingPlugin = new LoggingPlugin();
  private static final TestPlugin testPlugin = new TestPlugin();

  @Before
  public void setUp() {
    languageServer = new LSPServer(ImmutableSet.of(new ResourceLoader()));
    textDocumentService = new TextDocumentServiceImpl(languageServer);
    languageServer.connect(new MockLanguageClient());
  }

  @Test
  public void completionTest_prefix_engine_init_all_completion_items() throws IOException {
    String inputPath = "engine_init.wstl";
    // Get all completion items generated from a file which has imports and transpiles successfully.
    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 5, 0);
    assertThat(allCompletionItems)
        .hasSize(
            countCompletionItems(builtins)
                + countCompletionItems(loggingPlugin)
                + countCompletionItems(testPlugin));
  }

  private int countCompletionItems(Plugin plugin) {
    return (int) plugin.getFunctions().stream().filter(this::isFunctionLoaded).count()
        + plugin.getTargets().size();
  }

  private boolean isFunctionLoaded(CallableFunction fxn) {
    return (fxn.getDebugInfo() != null
            && (fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.NATIVE)
                || fxn.getDebugInfo().getFunctionInfo().getType().equals(FunctionType.DECLARED)))
        || !fxn.getSignature().getInheritsParentVars();
  }

  @Test
  public void completionTest_prefix_engine_init_get_plugin_completion_items() throws IOException {
    String inputPath = "engine_init.wstl";
    // Get all the imported dataflow plugin completion functions
    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 6, 2);
    assertThat(allCompletionItems)
        .hasSize(testPlugin.getFunctions().size() + testPlugin.getTargets().size());

    List<String> expectedCompletionItems = getPluginPackageCompletionItemStrings(testPlugin);

    assertThat(allCompletionItems).containsExactlyElementsIn(expectedCompletionItems);
  }

  // Test to check if bad functions bodies do not prevent plugin imports.
  @Test
  public void completionTest_prefix_engine_init_get_plugin_bad_function_body() throws IOException {
    String inputPath = "engine_init_bad_function_body.wstl";
    // Get all the imported dataflow plugin completion functions
    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 6, 2);

    assertThat(allCompletionItems)
        .hasSize(testPlugin.getFunctions().size() + testPlugin.getTargets().size());

    List<String> expectedCompletionItems = getPluginPackageCompletionItemStrings(testPlugin);

    assertThat(allCompletionItems).containsExactlyElementsIn(expectedCompletionItems);
  }

  /**
   * Helper function which returns a list of strings which represent completion items that get
   * generated from a plugin package.
   *
   * @param plugin The plugin package from where functions and targets will be fetched.
   * @return A list of strings which represent the completion items that would be generated.
   */
  private List<String> getPluginPackageCompletionItemStrings(Plugin plugin) {
    List<CallableFunction> functions = plugin.getFunctions();
    List<Constructor> targets = plugin.getTargets();
    String packagePrefix = plugin.getPackageName() + "::";
    List<String> expectedFunctionsItems =
        functions.stream().map(fn -> packagePrefix + fn.getName()).collect(Collectors.toList());
    List<String> expectedTargetItems =
        targets.stream().map(t -> packagePrefix + t.getName()).collect(Collectors.toList());
    return Stream.concat(expectedFunctionsItems.stream(), expectedTargetItems.stream())
        .collect(Collectors.toList());
  }

  // Test to check if transpile run time exceptions do not prevent engine loading and generating
  // autocompletes for an imported plugin
  @Test
  public void autoComplete_partial_transpile_get_plugin_completion_items() throws IOException {
    String inputPath = "autocomplete_partial_transpile.wstl";
    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 8, 5);
    assertThat(allCompletionItems)
        .hasSize(testPlugin.getFunctions().size() + testPlugin.getTargets().size());
    List<String> expectedCompletionItems = getPluginPackageCompletionItemStrings(testPlugin);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedCompletionItems);
  }

  @Test
  public void autoComplete_functions_package_prefix_test() throws IOException {
    String inputPath = "autocomplete_importer.wstl";
    String[] expectedImportedFunctionNames = {"importee::funcFromImportee"};
    String[] expectedLocalFunctionNames = {"localFunction"};

    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 6, 3);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedLocalFunctionNames);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 8, 10);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedImportedFunctionNames);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 10, 6);
    assertThat(allCompletionItems)
        .hasSize(testPlugin.getFunctions().size() + testPlugin.getTargets().size());
    assertThat(allCompletionItems)
        .containsExactlyElementsIn(getPluginPackageCompletionItemStrings(testPlugin));
  }

  @Test
  public void autocomplete_variable_prefix_test() throws IOException {
    String inputPath = "autocomplete_variable.wstl";
    String[] expectedVariable2 = {"myVariable2"};
    String[] expectedVariable1 = {"myVariable1"};
    String[] expectedMultipleVariables1 = {"myVariable2", "myVariable3"};
    String[] expectedMultipleVariables2 = {"myVariable2", "myVariable4"};

    List<String> allCompletionItems = getCompletionItems(textDocumentService, inputPath, 2, 5);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedVariable1);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 13, 7);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedVariable2);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 8, 9);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedMultipleVariables1);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 11, 9);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedMultipleVariables2);

    allCompletionItems = getCompletionItems(textDocumentService, inputPath, 16, 5);
    assertThat(allCompletionItems).containsExactlyElementsIn(expectedVariable1);
  }

  @Test
  public void autocomplete_signature_should_have_args_name_test() throws IOException {
    String inputPath = "autocomplete_signature.wstl";

    String[] expectedSignatureForFormatDateFunctions = {
      "formatDateTime(String format, String iso8601DateTime)",
      "formatDateTimeZ(String format, String timezone, String iso8601DateTime)"
    };
    String[] expectedSignatureForAssertFuncs = {
      "test::assertEquals(Data want, Data got)",
      "test::assertNull(Data data)",
      "test::assertTrue(Boolean bool)",
    };
    String[] expectedSignatureForRunFuncs = {
      "test::reportAll()",
      "test::reportAll(String packageName)",
      "test::run(Closure body)",
      "test::runAll()",
      "test::runAll(String packageName)",
      "test::runSingle(String name, Closure body)",
    };
    String[] expectedSignatureForLocalFunction = {
      "localfunction(Data number1, Data number2)", "localfunction2()"
    };
    String[] expectedVariable = {"myVariable1"};

    List<String> allCompetionItems = getCompletionSignature(textDocumentService, inputPath, 2, 5);
    assertThat(allCompetionItems)
        .containsExactlyElementsIn(expectedSignatureForFormatDateFunctions);

    allCompetionItems = getCompletionSignature(textDocumentService, inputPath, 4, 8);
    assertThat(allCompetionItems).containsExactlyElementsIn(expectedSignatureForAssertFuncs);

    allCompetionItems = getCompletionSignature(textDocumentService, inputPath, 16, 7);
    assertThat(allCompetionItems).containsExactlyElementsIn(expectedSignatureForRunFuncs);

    // for user defined functions.
    allCompetionItems = getCompletionSignature(textDocumentService, inputPath, 14, 4);
    assertThat(allCompetionItems).containsExactlyElementsIn(expectedSignatureForLocalFunction);

    allCompetionItems = getCompletionSignature(textDocumentService, inputPath, 19, 5);
    assertThat(allCompetionItems).containsExactlyElementsIn(expectedVariable);
  }
}
