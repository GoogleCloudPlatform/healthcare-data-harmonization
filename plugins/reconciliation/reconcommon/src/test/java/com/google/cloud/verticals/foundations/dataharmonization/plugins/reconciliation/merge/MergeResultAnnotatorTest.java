/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.DefaultStableIdGeneratorTest;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for MergeResultAnnotator. */
@RunWith(JUnit4.class)
public class MergeResultAnnotatorTest {
  private RuntimeContext ctx;
  private Engine engine;
  private MergeResultAnnotator annotator;

  @Before
  public void setup() throws IOException {
    InputStream textprotoStream =
        DefaultStableIdGeneratorTest.class.getResourceAsStream("/target_sample.textproto");
    Pipeline.PipelineConfig pConfig =
        TextFormat.parse(new String(textprotoStream.readAllBytes()), Pipeline.PipelineConfig.class);
    engine = new Engine.Builder(TestConfigExtractor.of(pConfig)).initialize().build();
    ctx = engine.getRuntimeContext();
    annotator = new MergeResultAnnotator();
  }

  @Test
  public void envVarNotSet_returnsData() {
    Data data = testDTI().primitiveOf("test");
    Data result = annotator.annotate(ctx, "MyFunction", data);
    assertEquals(data, result);
  }

  @Test
  public void envVarSet_false_returnsData() {
    // setDebugModeEnabled(false);
    Data data = testDTI().primitiveOf("test");
    Data result = annotator.annotate(ctx, "MyFunction", data);
    assertEquals(data, result);
  }

  // TODO() Refactor unused reconciliation functions under Merging plugin.
  // @Test
  // public void annotatePrimitive() {
  //   setDebugModeEnabled(true);
  //   Data data = testDTI().primitiveOf("test");
  //   Data expected = testDTI().primitiveOf("test [MyFunction]");
  //
  //   Data result = annotator.annotate(ctx, "MyFunction", data);
  //
  //   assertEquals(expected, result);
  // }
  //
  // @Test
  // public void annotateContainer() {
  //   setDebugModeEnabled(true);
  //   Data data =
  //       testDTI()
  //           .containerOf(
  //               ImmutableMap.of(
  //                   "field1", testDTI().primitiveOf("value1"),
  //                   "field2", testDTI().primitiveOf("value2")));
  //   Data expected =
  //       testDTI()
  //           .containerOf(
  //               ImmutableMap.of(
  //                   "field1", testDTI().primitiveOf("value1 [MyFunction]"),
  //                   "field2", testDTI().primitiveOf("value2 [MyFunction]")));
  //
  //   Data result = annotator.annotate(ctx, "MyFunction", data);
  //
  //   assertEquals(expected, result);
  // }
  //
  // @Test
  // public void annotateArray() {
  //   setDebugModeEnabled(true);
  //
  //   Data data =
  //       testDTI()
  //           .arrayOf(ImmutableList.of(testDTI().primitiveOf(1.5), testDTI().primitiveOf(true)));
  //   Data expected =
  //       testDTI()
  //           .arrayOf(
  //               ImmutableList.of(
  //                   testDTI().primitiveOf("1.5 [MyFunction]"),
  //                   testDTI().primitiveOf("true [MyFunction]")));
  //
  //   Data result = annotator.annotate(ctx, "MyFunction", data);
  //
  //   assertEquals(expected, result);
  // }
  //
  // @Test
  // public void annotateRecursive() {
  //   setDebugModeEnabled(true);
  //
  //   Data data =
  //       testDTI()
  //           .containerOf(
  //               ImmutableMap.of(
  //                   "field1",
  //                       testDTI()
  //                           .arrayOf(
  //                               ImmutableList.of(
  //                                   testDTI().primitiveOf(1.5),
  //                                   testDTI()
  //                                       .containerOf(
  //                                           ImmutableMap.of(
  //                                               "field1", testDTI().primitiveOf("test"))))),
  //                   "field2", testDTI().primitiveOf("value2")));
  //   Data expected =
  //       testDTI()
  //           .containerOf(
  //               ImmutableMap.of(
  //                   "field1",
  //                       testDTI()
  //                           .arrayOf(
  //                               ImmutableList.of(
  //                                   testDTI().primitiveOf("1.5 [MyFunction]"),
  //                                   testDTI()
  //                                       .containerOf(
  //                                           ImmutableMap.of(
  //                                               "field1",
  //                                               testDTI().primitiveOf("test [MyFunction]"))))),
  //                   "field2", testDTI().primitiveOf("value2 [MyFunction]")));
  //
  //   Data result = annotator.annotate(ctx, "MyFunction", data);
  //
  //   assertEquals(expected, result);
  // }

  @After
  public void teardown() {
    engine.close();
  }

  // private void setDebugModeEnabled(boolean enabled) {
  //   Container serializableMetadataEntry =
  //       testDTI()
  //           .containerOf(
  //               ImmutableMap.of("ENABLE_DEBUG_MERGE_ANNOTATION",
  // testDTI().primitiveOf(enabled)));
  //
  //   ctx.getMetaData()
  //       .setSerializableMeta(EnvironmentPlugin.ENVIRONMENT_VARIABLES, serializableMetadataEntry);
  // }
}
