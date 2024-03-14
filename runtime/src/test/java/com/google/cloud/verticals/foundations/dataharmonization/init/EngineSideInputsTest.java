/*
 * Copyright 2023 Google LLC.
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

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.MapLoader;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.InlineConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Engine.initialize which ensure everything from .withSideInputs() is loaded. */
@RunWith(JUnit4.class)
public final class EngineSideInputsTest {

  private static final String IMPORT_PATH_URI_SCHEME = ResourceLoader.TEST_LOADER;
  private static final String SIDE_INPUT_FILENAME = "/tests/init/import/standalone_functions.wstl";

  private static URI mainConfigUri() throws URISyntaxException {
    return new URI(IMPORT_PATH_URI_SCHEME, null, "/tests/init/standalone.wstl", null);
  }

  private static URI formatImportUri(String path) throws URISyntaxException {
    return new URI(String.format("%s://%s", IMPORT_PATH_URI_SCHEME, path));
  }

  private static ImportPath sideInputImportPath() {
    return
        ImportPath.of(
            MapLoader.NAME,
            FileSystems.getDefault().getPath(SIDE_INPUT_FILENAME),
            FileSystems.getDefault().getPath("/tests/init/import"));
  }

  @Test
  public void testInitialize_withSideInput_inlineWhistleConfig()
      throws IOException, URISyntaxException {
    String inlineConfig = "package pkg\n\ndef goodbye(name) {\n\tname: name\n}";

    InlineConfigExtractor sideInput =
        InlineConfigExtractor.of(inlineConfig, sideInputImportPath(), true);
    Engine.InitializedBuilder initialized =
        new Engine.Builder(ExternalConfigExtractor.of(mainConfigUri(), formatImportUri("/")))
            .withDefaultPlugins(new TestLoaderPlugin())
            .withSideInputs(ImmutableList.of(sideInput))
            .initialize();

    // Assert the sideInputs were loaded properly.
    Set<CallableFunction> functionMatches =
        initialized
            .getRegistries()
            .getFunctionRegistry("pkg")
            .getOverloads(ImmutableSet.of("pkg"), "goodbye");
    assertThat(functionMatches).hasSize(1);
  }

  @Test
  public void testInitialize_withSideInput_whistleConfigPath()
      throws IOException, URISyntaxException {
    byte[] sideInputContents =
        EngineSideInputsTest.class.getResourceAsStream(SIDE_INPUT_FILENAME).readAllBytes();

    Engine.InitializedBuilder initialized =
        new Engine.Builder(ExternalConfigExtractor.of(mainConfigUri(), formatImportUri("/")))
            .withDefaultPlugins(new TestLoaderPlugin())
            .withDefaultLoaders(
                ImmutableSet.of(
                    new MapLoader(
                        ImmutableMap.of(
                            String.format("vfs://%s", SIDE_INPUT_FILENAME), sideInputContents))))
            .withSideInputs(ImmutableList.of(ExternalConfigExtractor.of(sideInputImportPath())))
            .initialize();

    // Assert the sideInputs were loaded properly.
    Set<CallableFunction> functionMatches =
        initialized
            .getRegistries()
            .getFunctionRegistry("test")
            .getOverloads(ImmutableSet.of("test"), "goodbye");
    assertThat(functionMatches).hasSize(1);
  }
}
