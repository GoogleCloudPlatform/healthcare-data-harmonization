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
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetCustomSinkContext;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.AbstractMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Transpiler#visitTargetCustomSink(TargetCustomSinkContext)} )}. */
@RunWith(Parameterized.class)
public class FunctionDefTest {
  private final String whistle;
  private final FunctionDefinition expected;
  private final List<FunctionDefinition> expectedRegFuncs;

  public FunctionDefTest(
      String name,
      String whistle,
      FunctionDefinition expected,
      List<FunctionDefinition> expectedOtherFunctions) {
    this.whistle = whistle;
    this.expected = expected;
    this.expectedRegFuncs = expectedOtherFunctions;
  }

  @Parameters(name = "function def - {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "constant valued function",
            "def hello() 123",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(ValueSource.newBuilder().setConstInt(123))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "var valued function",
            "def hello(x) x",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(ValueSource.newBuilder().setFromLocal("x"))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "function with required args",
            "def hello(required x, required y, z) x",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").setModifier("required").build())
                .addArgs(Argument.newBuilder().setName("y").setModifier("required").build())
                .addArgs(Argument.newBuilder().setName("z").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(ValueSource.newBuilder().setFromLocal("x"))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "function call valued function",
            "def hello(x, y) x + y",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").build())
                .addArgs(Argument.newBuilder().setName("y").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference(
                                                WhistleHelper.getTokenLiteral(WhistleParser.ADD)))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("y"))
                                        .build()))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "array valued function",
            "def hello(x, y) [x, y]",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").build())
                .addArgs(Argument.newBuilder().setName("y").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                                        .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("y"))
                                        .build()))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "function using block with $this",
            "def hello() {\n\t$this: 123;\n}",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addMapping(
                    FieldMapping.newBuilder()
                        .setField(FieldTarget.newBuilder().setPath("$this"))
                        .setValue(ValueSource.newBuilder().setConstInt(123))
                        .build())
                .build(),
            ImmutableList.of(),
          },
          {
            "block valued function",
            "def hello(x, y) {\n\tz: [x, y];\n}",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").build())
                .addArgs(Argument.newBuilder().setName("y").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setField(FieldTarget.newBuilder().setPath("z"))
                        .setValue(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                                        .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("y"))
                                        .build()))
                        .build())
                .build(),
            ImmutableList.of(),
          },
          {
            "merge mode replace named function",
            "def replace() 123",
            FunctionDefinition.newBuilder()
                .setName("replace")
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(ValueSource.newBuilder().setConstInt(123))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "merge mode merge named function",
            "def merge() 123",
            FunctionDefinition.newBuilder()
                .setName("merge")
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(ValueSource.newBuilder().setConstInt(123))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
          {
            "function with multiple newlines",
            "def hello(\n\nx,\n\ny\n\n)\n\n {\nx + y\n}",
            FunctionDefinition.newBuilder()
                .setName("hello")
                .addArgs(Argument.newBuilder().setName("x").build())
                .addArgs(Argument.newBuilder().setName("y").build())
                .addMapping(
                    FieldMapping.newBuilder()
                        .setValue(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference(
                                                WhistleHelper.getTokenLiteral(WhistleParser.ADD)))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("x"))
                                        .addArgs(ValueSource.newBuilder().setFromLocal("y"))
                                        .build()))
                        .build())
                .build(),
            new ArrayList<FunctionDefinition>(),
          },
        });
  }

  @BeforeClass
  public static void setUp() {
    LambdaHelper.setSeed(100);
  }

  @Test
  public void test() {
    Environment env = new Environment("testRoot");
    Transpiler t = new Transpiler(env);

    AbstractMessage got = t.transpile(whistle, WhistleParser::functionDef);

    List<FunctionDefinition> allExpected = new ArrayList<>(expectedRegFuncs);
    allExpected.add(expected);

    // Strip metas for this test.
    got = clearMeta(got);
    List<FunctionDefinition> gotFuncs =
        t.getAllFunctions().stream().map(TestHelper::clearMeta).collect(Collectors.toList());

    assertEquals(expected, got);
    assertEquals(allExpected, gotFuncs);
  }
}
