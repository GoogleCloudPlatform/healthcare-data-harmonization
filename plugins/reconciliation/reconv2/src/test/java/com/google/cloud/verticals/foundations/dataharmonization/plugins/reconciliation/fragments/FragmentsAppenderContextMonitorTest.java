// Copyright 2024 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.fragments;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.ContainerJsonSerializer;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.InlineConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.JsonFileUtils;
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
public final class FragmentsAppenderContextMonitorTest {

  private static final String RESOURCE_DIR = "/mapping/hdev2/fragmentMonitorTests/";

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_noFragments_success()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    Container resources = JsonFileUtils.readJsonFile(RESOURCE_DIR, "noFragments.resources.json");
    monitor.enable();
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "noFragments.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_withFragments_success()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    Container resources = JsonFileUtils.readJsonFile(RESOURCE_DIR, "happyPath.resources.json");
    Container inputFragments = JsonFileUtils.readJsonFile(RESOURCE_DIR, "happyPath.fragments.json");
    Array inputFragmentArray = inputFragments.getField("fragments").asArray();
    ctx.getMetaData().setSerializableMeta("fragments", inputFragmentArray);
    monitor.enable();
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_nonContainer_doesNotOutputFragments()
      throws IOException, URISyntaxException {
    String nonContainerReturnData = "[\"data1\", \"data2\"]";
    Data data = JsonSerializerDeserializer.jsonToData(nonContainerReturnData.getBytes(UTF_8));

    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    monitor.enable();
    Data got = monitor.onRuntimeContextFinish(ctx, data.asArray());

    assertEquals(got, data);
  }

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_monitorDisabled()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    Container resources = JsonFileUtils.readJsonFile(RESOURCE_DIR, "happyPath.resources.json");
    monitor.disable();
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    assertEquals(got, resources);
  }

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_fragmentsFieldNotOverwritten()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    Container resources = JsonFileUtils.readJsonFile(RESOURCE_DIR, "hasFragments.resources.json");
    Container inputFragments =
        JsonFileUtils.readJsonFile(RESOURCE_DIR, "hasFragments.fragments.json");
    Array inputFragmentArray = inputFragments.getField("fragments").asArray();
    ctx.getMetaData().setSerializableMeta("fragments", inputFragmentArray);
    monitor.enable();
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "hasFragments.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }

  @Test
  public void testOnFragmentsAppenderContextMonitorFinish_fragmentsFieldEmpty_overwritten()
      throws IOException, URISyntaxException {
    InputStream wstlStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "happyPath.mapping.wstl");
    String wstlConfig = new String(wstlStream.readAllBytes(), UTF_8);
    Engine engine =
        new Engine.Builder(InlineConfigExtractor.of(wstlConfig, URI.create("")))
            .withDefaultPlugins()
            .initialize()
            .build();
    RuntimeContext ctx = engine.getRuntimeContext();
    FragmentsAppenderContextMonitor monitor = new FragmentsAppenderContextMonitor();
    Container resources =
        JsonFileUtils.readJsonFile(RESOURCE_DIR, "hasEmptyFragments.resources.json");
    Container inputFragments =
        JsonFileUtils.readJsonFile(RESOURCE_DIR, "hasEmptyFragments.fragments.json");
    Array inputFragmentArray = inputFragments.getField("fragments").asArray();
    ctx.getMetaData().setSerializableMeta("fragments", inputFragmentArray);
    monitor.enable();
    Data got = monitor.onRuntimeContextFinish(ctx, resources);

    InputStream wantInputStream =
        JsonFileUtils.class.getResourceAsStream(RESOURCE_DIR + "hasEmptyFragments.result.json");
    String wantString = new String(wantInputStream.readAllBytes(), UTF_8);
    JsonElement wantJson = JsonParser.parseString(wantString);
    JsonElement gotJson = ContainerJsonSerializer.processContainer(got.asContainer());

    assertEquals(wantJson, gotJson);
  }
}
