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

import static com.google.cloud.verticals.foundations.dataharmonization.lsp.TextDocumentServiceImplTestUtil.openTextDocument;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.HoverParams;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class HoverTest {

  static LSPServer languageServer = new LSPServer(ImmutableSet.of(new ResourceLoader()));
  static TextDocumentServiceImpl textDocumentService = new TextDocumentServiceImpl(languageServer);
  private static final String BASE_TEST_RES_FOLDER = "hover_tests/";

  @Parameter public String testCaseName;

  @Parameter(1)
  public String inputWstlFile;

  @Parameter(2)
  public int[] runHoverFromLocation;

  @Parameter(3)
  public String hoverResponseResource;

  @Parameters(name = "{0}")
  public static List<Object[]> data() {
    // The parametrized tests
    return Arrays.asList(
        new Object[][] {
          {"built-in strFmt docs", "hover.wstl", new int[] {1, 1}, "strFmt.md"},
          {"built-in where docs", "hover.wstl", new int[] {2, 1}, "where.md"},
          {"dataflow bigQueryIO docs", "hover.wstl", new int[] {3, 4}, "dataflow_bigqueryIO.md"},
          {"test assertEquals docs", "hover.wstl", new int[] {4, 4}, "assertEquals.md"},
          {"validation mockGet docs", "hover.wstl", new int[] {5, 4}, "mockGet.md"},
          {"hover docs dont show from imported file", "hover_importer.wstl", new int[] {1, 1}, ""},
          {"hover importee shows docs", "hover_importee.wstl", new int[] {1, 1}, "strFmt.md"},
        });
  }

  @Before
  public void setUp() {
    languageServer.connect(new MockLanguageClient());
  }

  @Test
  public void built_in_hover_test() throws IOException, ExecutionException, InterruptedException {
    openTextDocument(textDocumentService, BASE_TEST_RES_FOLDER + inputWstlFile);
    int line = runHoverFromLocation[0];
    int col = runHoverFromLocation[1];
    Hover hoverRes = hover(BASE_TEST_RES_FOLDER + inputWstlFile, line, col);
    MarkupContent hoverContents = hoverRes.getContents().getRight();
    if (hoverResponseResource.isEmpty()) {
      assertThat(hoverContents).isEqualTo(null);
    } else {
    String expectedMd =
        new String(
            this.getClass()
                .getResourceAsStream("/" + BASE_TEST_RES_FOLDER + hoverResponseResource)
                .readAllBytes(),
            StandardCharsets.UTF_8);
      assertThat(trimStringLines(hoverContents.getValue())).isEqualTo(trimStringLines(expectedMd));
    }
  }

  public static Hover hover(String filePath, int line, int position)
      throws ExecutionException, InterruptedException {

    // Send a definition request
    String wstlURI = "res:///" + filePath;

    HoverParams definitionParams = new HoverParams();
    TextDocumentIdentifier textDocumentIdentifier = new TextDocumentIdentifier();
    textDocumentIdentifier.setUri(wstlURI);
    definitionParams.setTextDocument(textDocumentIdentifier);
    definitionParams.setPosition(new Position(line, position));

    return textDocumentService.hover(definitionParams).get();
  }

  private String trimStringLines(String inputString) {
    return String.join("\n", inputString.lines().map(l -> l.trim()).collect(toImmutableList()));
  }
}
