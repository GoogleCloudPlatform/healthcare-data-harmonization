/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.init;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.InlineConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for Engine.initialize. */
@RunWith(Parameterized.class)
public class EngineInitializeTest {
  private static final String IMPORT_PATH_URI_SCHEME = ResourceLoader.TEST_LOADER;

  @Parameter public String testCaseName;

  @Parameter(1)
  public String config;

  @Parameter(2)
  public URI importsRootURI;

  @Parameter(3)
  public Class<? extends Exception> expectedExceptionClass;

  @Parameter(4)
  public String expectedExceptionMsg;

  @Parameter(5)
  public Class<? extends Exception> inlineInitExpectedExceptionClass;

  @Parameter(6)
  public String inlineInitExpectedExceptionMsg;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() throws URISyntaxException {
    return Arrays.asList(
        new Object[][] {
          {"standalone config", "/tests/init/standalone.wstl", new URI(""), null, "", null, ""},
          {
            "root relative import config",
            "/tests/init/root_relative_import.wstl",
            formatImportPath("/"),
            null,
            "",
            null,
            "",
          },
          {
            "root relative import config with empty imports root",
            "/tests/init/root_relative_import.wstl",
            new URI(""),
            ImportException.class,
            "/tests/init/tests/init/standalone.wstl",
            IllegalStateException.class,
            "root relative import is disabled without imports root",
          },
          {
            "relative import config with imports root different from main config folder",
            "/tests/init/relative_import.wstl",
            formatImportPath("/tests"),
            null,
            "",
            // TODO() better error message should be used.
            ImportException.class,
            "Error processing import res:///standalone.wstl",
          },
          {
            "import a function call",
            "/tests/init/func_import.wstl",
            formatImportPath("/tests"),
            null,
            "",
            ImportException.class,
            "Error processing import res:///constants.wstl",
          },
          {
            "import a string with a function call",
            "/tests/init/str_interp_import.wstl",
            formatImportPath("/tests"),
            null,
            "",
            ImportException.class,
            "Error processing import res:///constants.wstl",
          },
          {
            "import an undefined function",
            "/tests/init/undefined_import.wstl",
            formatImportPath("/tests"),
            IllegalArgumentException.class,
            "undef() cannot be evaluated into a valid string import path: Unknown function undef()",
            IllegalArgumentException.class,
            "undef() cannot be evaluated into a valid string import path: Unknown function undef()",
          },
          {
            "Process a file which does not transpile function",
            "/tests/init/transpile_exception.wstl",
            new URI(""),
            TranspilationException.class,
            "Errors occurred during transpilation",
            TranspilationException.class,
            "Errors occurred during transpilation",
          },
        });
  }

  private static URI formatImportPath(String path) throws URISyntaxException {
    return new URI(String.format("%s://%s", IMPORT_PATH_URI_SCHEME, path));
  }

  @Test
  public void testInitialize_whistleConfigPath() throws IOException, URISyntaxException {
    URI whistleConfig = new URI(IMPORT_PATH_URI_SCHEME, null, config, null);
    if (expectedExceptionClass == null) {
      new Engine.Builder(ExternalConfigExtractor.of(whistleConfig, importsRootURI))
          .withDefaultPlugins(new TestLoaderPlugin())
          .initialize()
          .build();
    } else {
      Exception e =
          assertThrows(
              expectedExceptionClass,
              () ->
                  new Engine.Builder(ExternalConfigExtractor.of(whistleConfig, importsRootURI))
                      .withDefaultPlugins(new TestLoaderPlugin())
                      .initialize()
                      .build());
      assertTrue(
          String.format("Didn't find \"%s\" in\n\"%s\"", expectedExceptionMsg, e.getMessage()),
          e.getMessage().contains(expectedExceptionMsg));
    }
  }

  @Test
  public void testInitialize_inlineWhistleConfig() throws IOException, URISyntaxException {
    String inlineWhistleConfig =
        new String(
            ByteStreams.toByteArray(EngineInitializeTest.class.getResourceAsStream(config)), UTF_8);
    if (inlineInitExpectedExceptionMsg.isEmpty()) {
      new Engine.Builder(InlineConfigExtractor.of(inlineWhistleConfig, importsRootURI))
          .withDefaultPlugins(new TestLoaderPlugin())
          .initialize()
          .build();
    } else {
      Exception e =
          assertThrows(
              inlineInitExpectedExceptionClass,
              () ->
                  new Engine.Builder(InlineConfigExtractor.of(inlineWhistleConfig, importsRootURI))
                      .withDefaultPlugins(new TestLoaderPlugin())
                      .initialize()
                      .build());
      assertTrue(
          String.format("%s\n%s", inlineInitExpectedExceptionMsg, e.getMessage()),
          e.getMessage().contains(inlineInitExpectedExceptionMsg));
    }
  }
}
