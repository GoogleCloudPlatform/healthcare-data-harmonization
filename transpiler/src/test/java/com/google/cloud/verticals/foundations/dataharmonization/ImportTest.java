/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.clearMeta;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.protobuf.AbstractMessage;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the Import rule. */
@RunWith(JUnit4.class)
public class ImportTest {
  @Test
  public void createsImport() {
    ValueSource importValue =
        ValueSource.newBuilder().setConstString("./test_import/file.wstl").build();
    Import expected = Import.newBuilder().setValue(importValue).build();
    AbstractMessage got =
        new Transpiler(new Environment("test"))
            .transpile("import \"./test_import/file.wstl\";", WhistleParser::importStatement);
    assertEquals(expected, clearMeta(got));
  }

  @Test
  public void importConstString() {
    ValueSource importValue =
        ValueSource.newBuilder().setConstString("gs://gcs-bucket/file.wstl").build();
    Import expected = Import.newBuilder().setValue(importValue).build();
    AbstractMessage got =
        new Transpiler(new Environment("test"))
            .transpile("import \"gs://gcs-bucket/file.wstl\";", WhistleParser::importStatement);
    assertEquals(expected, clearMeta(got));
  }

  @Test
  public void importBuiltInFunctionCall() {
    FunctionCall functionCall =
        FunctionCall.newBuilder()
            .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
            .addArgs(ValueSource.newBuilder().setConstString("hello %s").build())
            .addArgs(ValueSource.newBuilder().setConstString("world"))
            .build();
    ValueSource importValue = ValueSource.newBuilder().setFunctionCall(functionCall).build();
    Import expected = Import.newBuilder().setValue(importValue).build();

    AbstractMessage got =
        new Transpiler(new Environment("test"))
            .transpile(
                "import builtins::strFmt(\"hello %s\", \"world\");",
                WhistleParser::importStatement);
    assertEquals(expected, clearMeta(got));
  }

  @Test
  public void importFunctionCall() {
    FunctionCall functionCall =
        FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("func").build())
            .addArgs(ValueSource.newBuilder().setConstBool(true))
            .build();
    ValueSource importValue = ValueSource.newBuilder().setFunctionCall(functionCall).build();
    Import expected = Import.newBuilder().setValue(importValue).build();

    AbstractMessage got =
        new Transpiler(new Environment("test"))
            .transpile("import func(true);", WhistleParser::importStatement);
    assertEquals(expected, clearMeta(got));
  }

  @Test
  public void importInterpStr() {
    FunctionCall pluginPrefix =
        FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("PluginPrefix").build())
            .build();
    FunctionCall implicitStrFmt =
        FunctionCall.newBuilder()
            .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
            .addArgs(ValueSource.newBuilder().setConstString("%s.DataflowPlugin").build())
            .addArgs(ValueSource.newBuilder().setFunctionCall(pluginPrefix))
            .build();
    ValueSource importValue = ValueSource.newBuilder().setFunctionCall(implicitStrFmt).build();
    Import expected = Import.newBuilder().setValue(importValue).build();

    AbstractMessage got =
        new Transpiler(new Environment("test"))
            .transpile(
                "import \"{PluginPrefix()}.DataflowPlugin\";", WhistleParser::importStatement);
    assertEquals(expected, clearMeta(got));
  }
}
