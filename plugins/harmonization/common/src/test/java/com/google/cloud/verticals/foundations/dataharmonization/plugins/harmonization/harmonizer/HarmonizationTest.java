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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.LocalHarmonizationParser;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Harmonization} */
@RunWith(JUnit4.class)
public class HarmonizationTest {

  private static final String CODE = "code";
  private static final String DISPLAY = "display";
  private static final String SYSTEM = "system";
  private static final String VERSION = "version";
  private static final String PROTO_FILE_NAME = "/sample.textproto";

static final String CONCEPT_MAP1 = "/test1.harmonization.json";
  static final String CONCEPT_MAP2 = "/test2.harmonization.json";
  static final String FILE = "file";
  static final String FORWARD_SLASH = "/";
  static final int ZERO = 0;
  static final int ONE = 1;

  private RuntimeContext ctx;
  private Engine engine;

  @Before
  public void setUp() throws IOException {
    InputStream textprotoStream = LocalHarmonizerTest.class.getResourceAsStream(PROTO_FILE_NAME);
    PipelineConfig pConfig =
        TextFormat.parse(new String(textprotoStream.readAllBytes()), PipelineConfig.class);
    engine = new Engine.Builder(TestConfigExtractor.of(pConfig)).initialize().build();
    ctx = engine.getRuntimeContext();
  }

  @After
  public void teardown() {
    engine.close();
  }

  private void parseConceptMap(String conceptMapPath) throws IOException {
    URL url = LocalHarmonizationParserTest.class.getResource(conceptMapPath);
    InputStream conceptMapStream =
        LocalHarmonizationParserTest.class.getResourceAsStream(conceptMapPath);
    ImportPath importPath =
        ImportPath.of(
            FILE,
            Path.of(url.getPath()),
            Path.of(url.getPath().substring(ZERO, url.getPath().lastIndexOf(FORWARD_SLASH))));
    byte[] conceptMapBytes = conceptMapStream.readAllBytes();
    new LocalHarmonizationParser()
        .parse(
            conceptMapBytes,
            ctx.getRegistries(),
            ctx.getMetaData(),
            mock(ImportProcessor.class),
            importPath);
  }

  private Container createContainer(String code, String display, String system, String version) {
    return testDTI()
        .containerOf(
            ImmutableMap.<String, Data>builder()
                .put(CODE, testDTI().primitiveOf(code))
                .put(DISPLAY, testDTI().primitiveOf(display))
                .put(SYSTEM, testDTI().primitiveOf(system))
                .put(VERSION, testDTI().primitiveOf(version))
                .build());
  }

  @Test
  public void testLocalHarmonizerInRuntimeContext_harmonize_testSingleTarget() {
    try {
      parseConceptMap(CONCEPT_MAP1);

      Array expected = testDTI().emptyArray();
      expected =
          expected.setElement(
              0,
              createContainer("target_code11_ab", "target_display11_ab", "target_system_b", "v1"));
      expected =
          expected.setElement(
              1,
              createContainer("target_code11_ac", "target_display11_ac", "target_system_c", "v1"));

      Assert.assertEquals(
          expected,
          Harmonization.harmonize(
              ctx,
              Harmonization.HARMONIZATION_SOURCE_LOCAL,
              "source_code1",
              "source_system_a",
              "conceptmap_id1"));

    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
      System.out.println("Exception Message: " + e.getMessage());
    }
  }

  @Test
  public void testLocalHarmonizerInRuntimeContext_harmonizeWithTarget_testSingleTarget() {
    try {
      parseConceptMap(CONCEPT_MAP1);
      parseConceptMap(CONCEPT_MAP2);

      Array expected = testDTI().emptyArray();
      expected =
          expected.setElement(
              ZERO,
              createContainer("target_code21_ab", "target_display21_ab", "target_system_b", "v1"));
      expected =
          expected.setElement(
              ONE,
              createContainer("target_code22_ab", "target_display22_ab", "target_system_b", "v1"));

      Data actual =
          Harmonization.harmonizeWithTarget(
              ctx,
              Harmonization.HARMONIZATION_SOURCE_LOCAL,
              "source_code2",
              "source_system_a",
              "target_system_b",
              "conceptmap_id1");
      assertEquals(expected, actual);
    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
      System.out.println("Exception Message: " + e.getMessage());
    }
  }

  @Test
  public void testNoLocalHarmonizerInRuntimeContext_harmonize() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Harmonization.harmonize(
                    ctx, Harmonization.HARMONIZATION_SOURCE_LOCAL, "", "", "missing_conceptmap"));
    assertEquals(
        "No ConceptMap has been imported. You probably need to import some `*.harmonization.json` "
            + "in your config file.",
        thrown.getMessage());
  }

  @Test
  public void testNoLocalHarmonizerInRuntimeContext_harmonizeWithTarget() {
    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Harmonization.harmonizeWithTarget(
                    ctx,
                    Harmonization.HARMONIZATION_SOURCE_LOCAL,
                    "",
                    "",
                    "",
                    "missing_conceptmap"));
    assertEquals(
        "No ConceptMap has been imported. You probably need to import some `*.harmonization.json` "
            + "in your config file.",
        thrown.getMessage());
  }
}
