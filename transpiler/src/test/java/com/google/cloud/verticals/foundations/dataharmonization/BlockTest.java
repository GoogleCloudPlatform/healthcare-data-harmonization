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

import com.google.cloud.verticals.foundations.dataharmonization.data.Functions;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.BlockContext;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Transpiler#visitBlock(BlockContext)}. */
@RunWith(Parameterized.class)
public class BlockTest {
  private final String whistle;
  private final ValueSource expectedValueSource;
  private final List<FunctionDefinition> expectedRegFuncs;
  private static final List<String> UUIDs =
      ImmutableList.of(
          "ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4",
          "ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4",
          "00000000-31e9-f345-ffff-ffffb73ee369",
          "ffffffff-aaca-f867-0000-0000070c5774",
          "ffffffff-c747-e698-0000-00004d95fa51",
          "ffffffff-9e5d-1176-ffff-ffffb3b88942",
          "ffffffff-9f83-48fd-0000-00003faffa1a",
          "00000000-3c9b-c140-0000-0000446b8453",
          "00000000-7cba-eb40-0000-00001bac6207");

  public BlockTest(
      String name,
      String whistle,
      ValueSource expectedValueSource,
      List<FunctionDefinition> expectedRegFuncs) {
    this.whistle = whistle;
    this.expectedValueSource = expectedValueSource;
    this.expectedRegFuncs = expectedRegFuncs;
  }

  @Parameters(name = "block - {0}")
  public static Collection<Object[]> data() {
    return ImmutableList.copyOf(
        new Object[][] {
          {
            /* name = */ "simple block",
            /* whistle = */ "{\n\tfield: 1;\n\t// Comment\n\tfield2: \"two\";\n}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(1)).build())
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(1))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setField(FieldTarget.newBuilder().setPath("field"))
                            .setValue(ValueSource.newBuilder().setConstInt(1))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setField(FieldTarget.newBuilder().setPath("field2"))
                            .setValue(ValueSource.newBuilder().setConstString("two"))
                            .build())
                    .build())
          },
          {
            /* name = */ "nested block",
            /* whistle = */ "{\n\tvar x: 1;\n\t{\n\t\tvar x: 2\n\t}\n}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(2)).build())
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("x").build())
                    .setName("block_" + UUIDs.get(3))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            .setValue(ValueSource.newBuilder().setConstInt(2))
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(2))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            .setValue(ValueSource.newBuilder().setConstInt(1))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionReference.newBuilder()
                                                    .setName("block_" + UUIDs.get(3)))
                                            .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                            .build()))
                            .build())
                    .build()),
          },
          {
            /* name = */ "nested block using multiple vars",
            /* whistle = */ "{\n\tvar x: 1;\n\tvar y: 4;\n\t{\n\t\tvar x: y\n\t}\n}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(4)).build())
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .addArgs(Argument.newBuilder().setName("y").build())
                    .addArgs(Argument.newBuilder().setName("x").build())
                    .setName("block_" + UUIDs.get(5))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            .setValue(ValueSource.newBuilder().setFromLocal("y"))
                            .build())
                    .build(),
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(4))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setVar(VariableTarget.newBuilder().setName("x").build())
                            .setValue(ValueSource.newBuilder().setConstInt(1))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setVar(VariableTarget.newBuilder().setName("y").build())
                            .setValue(ValueSource.newBuilder().setConstInt(4))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setValue(
                                ValueSource.newBuilder()
                                    .setFunctionCall(
                                        FunctionCall.newBuilder()
                                            .setReference(
                                                FunctionReference.newBuilder()
                                                    .setName("block_" + UUIDs.get(5)))
                                            .addArgs(ValueSource.newBuilder().setFromLocal("y"))
                                            .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                            .build()))
                            .build())
                    .build()),
          },
          {
            /* name = */ "block with $this",
            /* whistle = */ "{\n\tfield: 1;\n\tfield2: \"$this\";\n}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(6)).build())
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(6))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setField(FieldTarget.newBuilder().setPath("field"))
                            .setValue(ValueSource.newBuilder().setConstInt(1))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setField(FieldTarget.newBuilder().setPath("field2"))
                            .setValue(ValueSource.newBuilder().setConstString("$this"))
                            .build())
                    .build())
          },
          {
            /* name = */ "block with iterated target",
            /* whistle = */ "{\n\tsome_target(): something[];\n}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(7)).build())
                    .addArgs(ValueSource.newBuilder().setFromLocal("something"))
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(7))
                    .addArgs(Argument.newBuilder().setName("something"))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setCustomSink(
                                FunctionCall.newBuilder()
                                    .setReference(
                                        FunctionReference.newBuilder().setName("some_target")))
                            .setValue(ValueSource.newBuilder().setFromLocal("something"))
                            .setIterateSource(true))
                    .build())
          },
          {
            /* name = */ "simple block with keyword names",
            /* whistle = */ "{\n"
                + "\treplace: 1;\n"
                + "\tmerge merge: \"two\";\n"
                + "\treplace var replace.extend[0].append: \"three\";}",
            /* expectedValueSource = */ createValueSource(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setName("block_" + UUIDs.get(8)).build())
                    .build()),
            /* expectedRegFuncs = */ ImmutableList.of(
                FunctionDefinition.newBuilder()
                    .setInheritParentVars(true)
                    .setName("block_" + UUIDs.get(8))
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setField(FieldTarget.newBuilder().setPath("replace"))
                            .setValue(ValueSource.newBuilder().setConstInt(1))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setCustomSink(
                                FunctionCall.newBuilder()
                                    .setReference(Functions.SET_REF)
                                    .addArgs(ValueSource.newBuilder().setConstString("$this"))
                                    .addArgs(ValueSource.newBuilder().setConstString("merge"))
                                    .addArgs(ValueSource.newBuilder().setConstString("merge")))
                            .setValue(ValueSource.newBuilder().setConstString("two"))
                            .build())
                    .addMapping(
                        FieldMapping.newBuilder()
                            .setCustomSink(
                                FunctionCall.newBuilder()
                                    .setReference(Functions.SET_REF)
                                    .addArgs(ValueSource.newBuilder().setConstString("replace"))
                                    .addArgs(
                                        ValueSource.newBuilder()
                                            .setConstString(".extend[0].append"))
                                    .addArgs(ValueSource.newBuilder().setConstString("replace")))
                            .setValue(ValueSource.newBuilder().setConstString("three"))
                            .build())
                    .build())
          },
        });
  }

  private static ValueSource createValueSource(FunctionCall call) {
    return ValueSource.newBuilder().setFunctionCall(call).build();
  }

  @BeforeClass
  public static void setUp() {
    LambdaHelper.setSeed(100);
  }

  /**
   * Transpiles {@code whistle} and makes sure that the resulting proto agrees with expected and
   * that the resulting {@link FunctionDefinition} proto is populated as expected.
   */
  @Test
  public void test() {
    Transpiler transpiler = new Transpiler();
    transpiler.environment.declareOrInheritVariable("something");

    Message actual = transpiler.transpile(whistle, WhistleParser::block);

    // Strip metas for this test.
    actual = clearMeta(actual);
    List<FunctionDefinition> gotFuncs =
        transpiler.getAllFunctions().stream()
            .map(TestHelper::clearMeta)
            .collect(Collectors.toList());

    assertEquals(expectedValueSource, actual);
    assertEquals(expectedRegFuncs, gotFuncs);
  }
}
