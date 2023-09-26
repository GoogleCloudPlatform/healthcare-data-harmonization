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
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.clearMeta;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.protobuf.AbstractMessage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Transpiler#program} )}. */
@RunWith(Parameterized.class)
public class ProgramTest {
  private static final FileInfo TEST_INFO =
      FileInfo.newBuilder().setUrl("gs://hello/world.wstl").build();

  private final String whistle;
  private final PipelineConfig expectedProgram;
  private final FileInfo file;

  public ProgramTest(String whistle, PipelineConfig expectedProgram, FileInfo file) {
    this.whistle = whistle;
    this.expectedProgram = expectedProgram;
    this.file = file;
  }

  @Parameters(name = "program - {0}")
  public static List<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "package x\none: \"one\";",
            PipelineConfig.newBuilder()
                .setPackageName("x")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("x_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one"))))
                .build(),
            TEST_INFO
          },
          {
            "package ':) \uD83D\uDE42'\none: \"one\";",
            PipelineConfig.newBuilder()
                .setPackageName(":) \uD83D\uDE42")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName(":) \uD83D\uDE42_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one"))))
                .build(),
            TEST_INFO
          },
          {
            "package \":) \uD83D\uDE42\"\none: \"one\";",
            PipelineConfig.newBuilder()
                .setPackageName(":) \uD83D\uDE42")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName(":) \uD83D\uDE42_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one"))))
                .build(),
            TEST_INFO
          },
          {
            "option \"foo\"\n",
            PipelineConfig.newBuilder()
                .setPackageName("world")
                .addOptions(Option.newBuilder().setName("foo"))
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("world_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR)))
                .build(),
            TEST_INFO
          },
          {
            "option \"foo/bar/baz\";",
            PipelineConfig.newBuilder()
                .setPackageName("world")
                .addOptions(Option.newBuilder().setName("foo/bar/baz"))
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("world_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR)))
                .build(),
            TEST_INFO
          },
          {
            "one: \"one\";",
            PipelineConfig.newBuilder()
                .setPackageName("world")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("world_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one"))))
                .build(),
            TEST_INFO
          },
          {
            "one: \"one\";",
            PipelineConfig.newBuilder()
                .setPackageName("$default")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("$default_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one"))))
                .build(),
            null
          },
          {
            "one: \"one\"; var oneVar: $this",
            PipelineConfig.newBuilder()
                .setPackageName("$default")
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("$default_root_function")
                        .addArgs(Argument.newBuilder().setName(TranspilerData.ROOT_VAR))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setField(FieldTarget.newBuilder().setPath("one"))
                                .setValue(ValueSource.newBuilder().setConstString("one")))
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setVar(VariableTarget.newBuilder().setName("oneVar").build())
                                .setValue(ValueSource.newBuilder().setFromLocal("$this").build())))
                .build(),
            null
          },
        });
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler();

    AbstractMessage got = t.transpile(whistle, file);

    // Strip metas for this test.
    got = clearMeta(got);

    assertEquals(expectedProgram, got);
  }
}
