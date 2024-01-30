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

import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.openTextDocument;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.eclipse.lsp4j.DefinitionParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to goto {@link TextDocumentServiceImpl#definition(DefinitionParams)} functionality
 */
@RunWith(Enclosed.class)
public class GoToDefinitionTest {

  static LSPServer languageServer = new LSPServer(ImmutableSet.of(new ResourceLoader()));
  static TextDocumentServiceImpl textDocumentService = new TextDocumentServiceImpl(languageServer);

  /** Parameterized tests. */
  @RunWith(Parameterized.class)
  public static class GoToDefinitionTestParameterized {

    @Parameter public String testCaseName;

    @Parameter(1)
    public String inputWstlFile;

    @Parameter(2)
    public int[] getDefinitionFrom;

    @Parameter(3)
    public List<Location> expectedLocation;

    @Parameters(name = "{0}")
    public static List<Object[]> data() {

      // The parametrized tests
      return Arrays.asList(
          new Object[][] {
            {
              "Only variables file",
              "goto_variables.wstl",
              new int[] {3, 14},
              ImmutableList.of(createLocation("goto_variables.wstl", 0, 0, 0, 12))
            },
            {
              "Go to a function definition in the same file",
              "goto_definition.wstl",
              new int[] {13, 4},
              ImmutableList.of(createLocation("goto_definition.wstl", 9, 0, 11, 0))
            },
            {
              "Go to a declared variable in the same file",
              "goto_definition.wstl",
              new int[] {15, 13},
              ImmutableList.of(createLocation("goto_definition.wstl", 5, 0, 5, 23))
            },
            {
              "Go to a variable which shares the name of a function",
              "goto_definition.wstl",
              new int[] {18, 41},
              ImmutableList.of(createLocation("goto_definition.wstl", 7, 0, 7, 45))
            },
            {
              "Go to the definition of a nested function reference",
              "goto_definition.wstl",
              new int[] {21, 21},
              ImmutableList.of(createLocation("goto_definition.wstl", 20, 0, 20, 27))
            },
            {
              "Go to the definition of a deeper nested function reference",
              "goto_definition.wstl",
              new int[] {22, 58},
              ImmutableList.of(createLocation("goto_definition.wstl", 20, 0, 20, 27))
            },
            {
              "Go to a reference which does not exist",
              "goto_definition.wstl",
              new int[] {20, 0},
              ImmutableList.of()
            },
            {
              "Go to a function which does not exist",
              "goto_definition.wstl",
              new int[] {24, 0},
              ImmutableList.of()
            },
            {
              "Go to a definition of an imported function",
              "goto_definition.wstl",
              new int[] {17, 46},
              ImmutableList.of(createLocation("goto_definition_import_file.wstl", 2, 0, 2, 55))
            },
            {
              "Go to a local function which shares the name with an imported function",
              "goto_package_b.wstl",
              new int[] {5, 0},
              ImmutableList.of(createLocation("goto_package_b.wstl", 3, 0, 3, 27))
            },
            {
              "Go to a function which shares the name with a local function, but is defined in"
                  + " another package",
              "goto_package_b.wstl",
              new int[] {6, 0},
              ImmutableList.of(createLocation("goto_package_a.wstl", 2, 0, 2, 37))
            },
            {
              "Tests to go to a nested variables which shares a name with a global variable"
                  + " another package",
              "goto_nested_variable_def_merge.wstl",
              new int[] {4, 11},
              ImmutableList.of(createLocation("goto_nested_variable_def_merge.wstl", 2, 3, 2, 10))
            },
            {
              "Go to a field variable reference which is performing a merge.",
              "goto_field_merge_var.wstl",
              new int[] {4, 5},
              ImmutableList.of(createLocation("goto_field_merge_var.wstl", 0, 0, 0, 11))
            },
            {
              "Go to a function reference which is made inside a function call.",
              "goto_function_call_in_function_def.wstl",
              new int[] {4, 6},
              ImmutableList.of(
                  createLocation("goto_function_call_in_function_def.wstl", 1, 0, 1, 24))
            },
            {
              "Go to a inner variable doing a merge on a outer variable.",
              "goto_nested_variables.wstl",
              new int[] {9, 7},
              ImmutableList.of(createLocation("goto_nested_variables.wstl", 0, 0, 2, 0))
            },
            {
              "Go to a nested variables which refers to an outer parent variable",
              "goto_nested_variables.wstl",
              new int[] {14, 6},
              ImmutableList.of(createLocation("goto_nested_variables.wstl", 4, 0, 6, 0))
            },
            {
              "Go to a nested variables which refers to redeclared variable",
              "goto_nested_variables.wstl",
              new int[] {13, 6},
              ImmutableList.of(createLocation("goto_nested_variables.wstl", 12, 3, 12, 11))
            },
            {
              "Go to a redeclared variable",
              "goto_nested_variables.wstl",
              new int[] {20, 4},
              ImmutableList.of(createLocation("goto_nested_variables.wstl", 18, 0, 18, 16))
            },
            {
              "Tests for blocks. Main block.",
              "goto_blocktest.wstl",
              new int[] {13, 16},
              ImmutableList.of(createLocation("goto_blocktest.wstl", 0, 0, 0, 8))
            },
            {
              "Tests for blocks. Block1.",
              "goto_blocktest.wstl",
              new int[] {11, 18},
              ImmutableList.of(createLocation("goto_blocktest.wstl", 5, 2, 5, 14))
            },
            {
              "Tests for blocks. Block2",
              "goto_blocktest.wstl",
              new int[] {9, 20},
              ImmutableList.of(createLocation("goto_blocktest.wstl", 8, 4, 8, 16))
            },
            {
              "Path writes inner var",
              "goto_path_writes.wstl",
              new int[] {5, 8},
              ImmutableList.of(createLocation("goto_path_writes.wstl", 4, 4, 4, 13))
            },
            {
              "Path writes inner write field",
              "goto_path_writes.wstl",
              new int[] {4, 9},
              ImmutableList.of(createLocation("goto_path_writes.wstl", 3, 4, 3, 11))
            },
            {
              "Path write outer var",
              "goto_path_writes.wstl",
              new int[] {8, 5},
              ImmutableList.of(createLocation("goto_path_writes.wstl", 0, 0, 0, 9))
            },
            {
              "Go to a uniquely named function argument.",
              "goto_function_arguments.wstl",
              // Go to the z param in func(x,z)
              new int[] {7, 11},
              ImmutableList.of(createLocation("goto_function_arguments.wstl", 6, 12, 6, 12))
            },
            {
              "Go to a function argument which shares its name with a function argument in another"
                  + "function overload.",
              "goto_function_arguments.wstl",
              new int[] {7, 7},
              ImmutableList.of(createLocation("goto_function_arguments.wstl", 6, 9, 6, 9))
            },
            {
              "Go to a function argument which shares the name with a variable at the root level.",
              "goto_function_arguments.wstl",
              new int[] {3, 7},
              ImmutableList.of(createLocation("goto_function_arguments.wstl", 2, 9, 2, 9))
            },
            {
              "Go to a inner variable which shares the name with a function arg.",
              "goto_function_arguments.wstl",
              new int[] {12, 11},
              ImmutableList.of(createLocation("goto_function_arguments.wstl", 11, 4, 11, 15))
            },
            {
              "Go to an function argument referenced in a target assignment.",
              "goto_function_arguments.wstl",
              new int[] {11, 11},
              ImmutableList.of(createLocation("goto_function_arguments.wstl", 10, 15, 10, 15))
            },
            {
              "Go to an function which is called as part of a target argument and is treated as a"
                  + " field mapping.",
              "goto_function_in_field_mapping.wstl",
              new int[] {6, 32},
              ImmutableList.of(createLocation("goto_function_in_field_mapping.wstl", 2, 0, 2, 17))
            }
          });
    }

    @Before
    public void setUp() {
      languageServer.connect(new MockLanguageClient());
    }

    @Test
    public void goToDefinitionTest() throws IOException {
      openTextDocument(textDocumentService, inputWstlFile);
      int line = getDefinitionFrom[0];
      int col = getDefinitionFrom[1];
      List<? extends Location> locations = getDefinition(inputWstlFile, line, col);
      assertThat(locations).containsExactlyElementsIn(expectedLocation).inOrder();
    }
  }

  /** Non parameterized tests for tests which require extra logic. */
  @RunWith(JUnit4.class)
  public static class GoToDefinitionTestNonParameterized {

    @Before
    public void setUp() {
      languageServer.connect(new MockLanguageClient());
    }

    @Test
    public void goto_multiple_usage_same_function_test() throws IOException {
      // Go to the same function definition from multiple references to that function
      String inputPath = "goto_package_b.wstl";
      openTextDocument(textDocumentService, inputPath);

      List<? extends Location> locations = getDefinition(inputPath, 5, 0);
      Location expectedLocation = createLocation(inputPath, 3, 0, 3, 27);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 8, 0);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 9, 0);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 10, 3);
      assertThat(locations).containsExactly(expectedLocation);
    }

    @Test
    public void goto_overloaded_functions() throws IOException {
      // Tests to go to the correct overloaded function definition for a function which has
      // overload definitions defined locally
      String inputPath = "goto_overloaded_functions.wstl";
      openTextDocument(textDocumentService, inputPath);
      List<? extends Location> locations;
      Location expectedLocation;

      locations = getDefinition(inputPath, 6, 0);
      expectedLocation = createLocation(inputPath, 2, 0, 2, 14);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 7, 0);
      expectedLocation = createLocation(inputPath, 3, 0, 3, 16);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 8, 0);
      expectedLocation = createLocation(inputPath, 4, 0, 4, 18);
      assertThat(locations).containsExactly(expectedLocation);
    }

    @Test
    public void goto_overloaded_imported_functions() throws IOException {
      // Tests to go to the correct overloaded function definition which come from an imported
      // package
      String inputPath = "goto_overloaded_in_other_package.wstl";
      String importedPath = "goto_overloaded_functions.wstl";
      openTextDocument(textDocumentService, inputPath);
      List<? extends Location> locations;
      Location expectedLocation;

      locations = getDefinition(inputPath, 6, 0);
      expectedLocation = createLocation(importedPath, 2, 0, 2, 14);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 7, 0);
      expectedLocation = createLocation(importedPath, 3, 0, 3, 16);
      assertThat(locations).containsExactly(expectedLocation);

      locations = getDefinition(inputPath, 8, 0);
      expectedLocation = createLocation(importedPath, 4, 0, 4, 18);
      assertThat(locations).containsExactly(expectedLocation);
    }
  }

  /**
   * Helper function which returns the function names that are generated by the text document
   * service definition method. Takes as input the path to the input test file and a line and
   * position number from which to generate a definition request.
   *
   * @return A list of {@link Location}s for any found definition objects.
   */
  public static List<? extends Location> getDefinition(String filePath, int line, int position) {

    // Send a definition request
    String wstlURI = "res:///" + filePath;

    DefinitionParams definitionParams = new DefinitionParams();
    TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri(wstlURI);
    definitionParams.setTextDocument(textDocumentIdentifier);
    definitionParams.setPosition(new Position(line, position));

    return textDocumentService.definition(definitionParams).join().getLeft();
  }

  static Location createLocation(
      String filePath, int startLine, int startCol, int endLine, int endCol) {
    Position startPosition = new Position(startLine, startCol);
    Position endPosition = new Position(endLine, endCol);
    Range range = new Range(startPosition, endPosition);
    String wstlURI = "res:///" + filePath;
    return new Location(wstlURI, range);
  }
}
