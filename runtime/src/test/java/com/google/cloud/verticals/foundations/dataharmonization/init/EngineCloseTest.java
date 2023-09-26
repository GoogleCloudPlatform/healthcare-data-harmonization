/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.init;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for {@link Engine#close()}. */
@RunWith(JUnit4.class)
public class EngineCloseTest {

  @Test
  public void engine_isAutoClosable() {
    assertThat(Engine.class).isAssignableTo(AutoCloseable.class);
  }

  @Test
  public void close_noPlugins() throws IOException {
    Engine engine = new Engine.Builder(TestConfigExtractor.of()).initialize().build();
    engine.close();

    // No exceptions.
  }

  @Test
  public void doubleClose_noPlugins() throws IOException {
    Engine engine = new Engine.Builder(TestConfigExtractor.of()).initialize().build();
    engine.close();
    engine.close();

    // No exceptions.
  }

  @Test
  public void close_closesPlugins() throws IOException {
    Plugin plugin = mock(Plugin.class, Answers.CALLS_REAL_METHODS);

    Engine engine =
        new Engine.Builder(TestConfigExtractor.of())
            .withDefaultPlugins(plugin)
            .initialize()
            .build();

    engine.close();

    verify(plugin).close();
  }

  @Test
  public void close_closesMockPlugins() throws IOException {
    Plugin plugin = mock(Plugin.class, Answers.CALLS_REAL_METHODS);
    PipelineConfig mockConfig =
        PipelineConfig.newBuilder()
            .setRootBlock(
                FunctionDefinition.newBuilder()
                    .setName("mock_root")
                    .addArgs(Argument.newBuilder().setName("root").build())
                    .build())
            .build();

    Engine engine =
        new Engine.Builder(TestConfigExtractor.of())
            .addMock(TestConfigExtractor.of(mockConfig))
            .withDefaultMockPlugins(plugin)
            .initialize()
            .build();

    engine.close();

    // One close for each of 2 loads -
    // when mock is initialized and when main config is initialized (mock is initialized there too)
    verify(plugin, times(2)).close();
  }

  @Test
  public void close_doubleCloseMockPlugins() throws IOException {
    Plugin plugin = mock(Plugin.class, Answers.CALLS_REAL_METHODS);
    PipelineConfig mockConfig =
        PipelineConfig.newBuilder()
            .setRootBlock(
                FunctionDefinition.newBuilder()
                    .setName("mock_root")
                    .addArgs(Argument.newBuilder().setName("root").build())
                    .build())
            .build();

    Engine engine =
        new Engine.Builder(TestConfigExtractor.of())
            .addMock(TestConfigExtractor.of(mockConfig))
            .withDefaultMockPlugins(plugin)
            .initialize()
            .build();

    engine.close();
    engine.close();

    // One close for each of 2 loads -
    // when mock is initialized and when main config is initialized (mock is initialized there too)
    verify(plugin, times(2)).close();
  }
}
