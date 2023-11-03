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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.imports;

import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.IntegrationTestBase;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.ExamplePlugin;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** This class tests the example loader and parser, but also demonstrates an integration test. */
@RunWith(JUnit4.class)
public class ImportsIntegrationTest extends IntegrationTestBase {

  @Test
  public void exampleLoaderParser_setsMetadata() throws IOException {
    // This test file path is relative to the test resources.
    Engine engine =
        initializeBuilder("/tests/imports.wstl")
            // Load our example plugin
            .withDefaultPlugins(new ExamplePlugin())
            // Initialize the engine (run the imports and such).
            .initialize()
            // Build the engine for execution.
            .build();

    // Execute the test file. In this test we don't actually care about the output.
    engine.transform(NullData.instance);

    // Verify our parser set the metadata correctly, and the loader loaded the content correctly.
    assertThat((Object) engine.getRuntimeContext().getMetaData().getMeta("example"))
        .isEqualTo("/this-is-a-test");
  }
}
