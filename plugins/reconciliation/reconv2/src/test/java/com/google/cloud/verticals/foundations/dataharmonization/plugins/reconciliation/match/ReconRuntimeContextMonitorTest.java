// Copyright 2023 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.match;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.ContainerJsonSerializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.InlineConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.ReconRuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ReconRuntimeContextMonitorTest {
  @Test
  public void testOnReconRuntimeContextMonitorFinish_noFragments_success()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/nofragments.matching.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    ReconRuntimeContextMonitor monitor = new ReconRuntimeContextMonitor();
    Container resources =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/nofragments.resources.json");
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/nofragments.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnReconRuntimeContextMonitorFinish_success()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/happyPath.matching.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    ReconRuntimeContextMonitor monitor = new ReconRuntimeContextMonitor();
    Container resources =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/happyPath.resources.json");
    Container inputFragments =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/happyPath.fragments.json");
    Array inputFragmentArray = inputFragments.getField("fragments").asArray();
    ctx.getMetaData().setSerializableMeta("fragments", inputFragmentArray);
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/happyPath.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnReconRuntimeContextMonitorFinish_invalidInput()
      throws IOException {
    InputStream wstlStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/happyPath.matching.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    ReconRuntimeContextMonitor monitor = new ReconRuntimeContextMonitor();
    Container resources =
        MatchingTestUtils.readJsonFile(
            "hdev2/reconMonitorTests/missingResourceType.resources.json");
    assertThrows(PropertyValueFetcherException.class, () -> {
      monitor.onRuntimeContextFinish(ctx, resources);
    });
  }

  @Test
  public void testOnReconRuntimeContextMonitorFinish_missingMatchingRules()
      throws IOException {
    InputStream wstlStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/missingRules.matching.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    ReconRuntimeContextMonitor monitor = new ReconRuntimeContextMonitor();
    Container resources =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/happyPath.resources.json");
    Container inputFragments =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/happyPath.fragments.json");
    Array inputFragmentArray = inputFragments.getField("fragments").asArray();
    ctx.getMetaData().setSerializableMeta("fragments", inputFragmentArray);
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/missingRules.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnReconRuntimeContextMonitorFinish_disabledMonitor() throws IOException {
    InputStream wstlStream =
        MatchingTestUtils.class.getResourceAsStream(
            "/matching/hdev2/reconMonitorTests/happyPath.matching.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    ReconRuntimeContextMonitor monitor = new ReconRuntimeContextMonitor();
    Container resources =
        MatchingTestUtils.readJsonFile("hdev2/reconMonitorTests/happyPath.resources.json");
    monitor.disable();

    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    assertEquals(got, resources);
  }
}
