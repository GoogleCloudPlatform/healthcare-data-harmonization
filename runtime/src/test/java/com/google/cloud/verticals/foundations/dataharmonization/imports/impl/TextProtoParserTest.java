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

package com.google.cloud.verticals.foundations.dataharmonization.imports.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.errorprone.annotations.Var;
import com.google.protobuf.TextFormat;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for ClassLoader. */
@RunWith(JUnit4.class)
public class TextProtoParserTest {

  @Test
  public void parse_validProto_parsesIntoPipelineConfig() throws IOException {
    PipelineConfig config = createTestProto();
    String textProto = TextFormat.printer().printToString(config);
    byte[] bytes = textProto.getBytes(UTF_8);

    PackageRegistry<CallableFunction> fnReg = mock(TestFunctionPackageRegistry.class);
    RuntimeContext ctx = RuntimeContextUtil.mockRuntimeContextWithRegistry();
    ImportProcessor ip = mock(ImportProcessor.class, Answers.RETURNS_MOCKS);

    when(ctx.getRegistries().getFunctionRegistry(config.getPackageName())).thenReturn(fnReg);

    new TextprotoParser()
        .parse(
            bytes,
            ctx.getRegistries(),
            ctx.getMetaData(),
            ip,
            ImportPathUtil.projectFile("/root/any.textproto"));

    verify(ip).processImports(ImportPathUtil.absPath("/root/any.textproto"), any(), eq(config));
    verify(fnReg).register(eq("pkg"), argThat(f -> f.getName().equals("foo")));
  }

  private static PipelineConfig createTestProto() {
    return PipelineConfig.newBuilder()
        .setPackageName("pkg")
        .addFunctions(FunctionDefinition.newBuilder().setName("foo").build())
        .build();
  }

  @Test
  public void parse_invalidProto_throws() {
    PipelineConfig config = createTestProto();
    @Var String textProto = TextFormat.printer().printToString(config);

    // Slight manglin' to make it invalid
    textProto = textProto.trim().substring(1);

    byte[] badBytes = textProto.getBytes(UTF_8);
    RuntimeContext ctx = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    ImportProcessor ip = mock(ImportProcessor.class, Answers.RETURNS_MOCKS);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TextprotoParser()
                .parse(badBytes, ctx.getRegistries(), ctx.getMetaData(), ip, null));
  }

  @Test
  public void parse_wrongType_throws() {
    FunctionDefinition config = FunctionDefinition.newBuilder().setName("asd").build();
    String textProto = TextFormat.printer().printToString(config);
    byte[] badBytes = textProto.getBytes(UTF_8);
    RuntimeContext ctx = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    ImportProcessor ip = mock(ImportProcessor.class, Answers.RETURNS_MOCKS);

    assertThrows(
        IllegalArgumentException.class,
        () ->
            new TextprotoParser()
                .parse(badBytes, ctx.getRegistries(), ctx.getMetaData(), ip, null));
  }

  @Test
  public void getName() {
    assertEquals(TextprotoParser.NAME, new TextprotoParser().getName());
  }
}
