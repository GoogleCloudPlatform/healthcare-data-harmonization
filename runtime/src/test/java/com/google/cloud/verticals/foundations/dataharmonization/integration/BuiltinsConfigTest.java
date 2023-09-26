/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.BuiltinsConfig;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.mocking.MockingTestPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Integration tests for Builtins interpretation of BuiltinsConfig. */
@RunWith(JUnit4.class)
public class BuiltinsConfigTest {
  @Test
  public void noFsFuncs_doesNotRegisterThem() throws IOException {
    BuiltinsConfig config = BuiltinsConfig.builder().setAllowFsFuncs(false).build();
    Builtins builtins = new Builtins(config);

    ImportPath file =
        ImportPath.of(
            ResourceLoader.TEST_LOADER,
            Path.of("/tests/builtinsconfig/files.wstl"),
            Path.of("/tests/builtinsconfig/"));

    try (Engine engine =
        new Engine.Builder(
                ExternalConfigExtractor.of(file),
                ImmutableList.of(new TestLoaderPlugin(), builtins))
            .initialize()
            .build()) {
      NoMatchingOverloadsException ex =
          assertThrows(
              NoMatchingOverloadsException.class, () -> engine.transform(NullData.instance));
      assertThat(ex).hasMessageThat().contains("Unknown function loadText");
    }
  }

  @Test
  public void fsFuncs_doesRegisterThem() throws IOException {
    BuiltinsConfig config = BuiltinsConfig.builder().setAllowFsFuncs(true).build();
    Builtins builtins = new Builtins(config);

    ImportPath file =
        ImportPath.of(
            ResourceLoader.TEST_LOADER,
            Path.of("/tests/builtinsconfig/files.wstl"),
            Path.of("/tests/builtinsconfig/"));

    try (Engine engine =
        new Engine.Builder(
                ExternalConfigExtractor.of(file),
                ImmutableList.of(new TestLoaderPlugin(), builtins))
            .initialize()
            .build()) {
      Data result = engine.transform(NullData.instance);
      assertThat(result.toString()).contains("loadText(\"./files.wstl\")");
    }
  }

  @Test
  public void pluginAllowlist_testPluginNotAllowed_throws() throws IOException {
    BuiltinsConfig config =
        BuiltinsConfig.builder()
            .setImportablePluginAllowlist(ImmutableSet.of("bingo", "bango", "bongo"))
            .build();
    Builtins builtins = new Builtins(config);

    ImportPath file =
        ImportPath.of(
            ResourceLoader.TEST_LOADER,
            Path.of("/tests/builtinsconfig/imports.wstl"),
            Path.of("/tests/builtinsconfig/"));

    ImportException ex =
        assertThrows(
            ImportException.class,
            () ->
                new Engine.Builder(
                        ExternalConfigExtractor.of(file),
                        ImmutableList.of(new TestLoaderPlugin(), builtins))
                    .initialize()
                    .build());
    assertThat(ex)
        .hasMessageThat()
        .contains(
            "Plugin class"
                + " com.google.cloud.verticals.foundations.dataharmonization.integration.mocking.MockingTestPlugin"
                + " is not known. Known plugins:\n"
                + "  bango\n"
                + "  bingo\n"
                + "  bongo");
  }

  @Test
  public void pluginAllowlist_testPluginAllowed_imports() throws IOException {
    BuiltinsConfig config =
        BuiltinsConfig.builder()
            .setImportablePluginAllowlist(ImmutableSet.of(MockingTestPlugin.class.getName()))
            .build();
    Builtins builtins = new Builtins(config);

    ImportPath file =
        ImportPath.of(
            ResourceLoader.TEST_LOADER,
            Path.of("/tests/builtinsconfig/imports.wstl"),
            Path.of("/tests/builtinsconfig/"));

    try (Engine engine =
        new Engine.Builder(
                ExternalConfigExtractor.of(file),
                ImmutableList.of(new TestLoaderPlugin(), builtins))
            .initialize()
            .build()) {
      Data result = engine.transform(NullData.instance);
      assertThat(result.toString()).contains("foo");
    }
  }
}
