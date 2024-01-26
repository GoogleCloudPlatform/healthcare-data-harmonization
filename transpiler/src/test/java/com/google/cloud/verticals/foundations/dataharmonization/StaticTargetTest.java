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

import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget.FieldType;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetStaticContext;
import com.google.protobuf.AbstractMessage;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Transpiler#visitTargetStatic(TargetStaticContext)}. */
@RunWith(Parameterized.class)
public class StaticTargetTest {
  private static final FunctionReference SET_REF =
      FunctionReference.newBuilder().setPackage("builtins").setName("set").build();
  private static final FunctionReference SIDE_REF =
      FunctionReference.newBuilder().setPackage("builtins").setName("side").build();

  private final String whistle;
  private final AbstractMessage expectedVS;

  public StaticTargetTest(String whistle, AbstractMessage expectedVS) {
    this.whistle = whistle;
    this.expectedVS = expectedVS;
  }

  @Parameters(name = "target - {0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "my_field",
            FieldTarget.newBuilder().setPath("my_field").setType(FieldType.LOCAL).build()
          },
          {
            "merge my_field",
            FunctionCall.newBuilder()
                .setReference(SET_REF)
                .addArgs(ValueSource.newBuilder().setConstString("$this").build())
                .addArgs(ValueSource.newBuilder().setConstString("my_field").build())
                .addArgs(ValueSource.newBuilder().setConstString("merge").build())
                .build()
          },
          {
            "my_field\\ with\\ special\\.chars\\[2\\]",
            FieldTarget.newBuilder()
                .setPath("my_field with special\\.chars\\[2\\]")
                .setType(FieldType.LOCAL)
                .build()
          },
          {
            "extrabackslash\\\\\\.chars\\[2\\]\\'",
            FieldTarget.newBuilder()
                .setPath("extrabackslash\\\\\\.chars\\[2\\]'")
                .setType(FieldType.LOCAL)
                .build()
          },
          {
            "'my_field with special.chars'.'also\\\\escap\\'d'",
            FieldTarget.newBuilder()
                .setPath("my_field with special\\.chars.also\\\\escap'd")
                .setType(FieldType.LOCAL)
                .build()
          },
          {
            "append 'my_field with special.chars'.'also\\\\escap\\'d'",
            FunctionCall.newBuilder()
                .setReference(SET_REF)
                .addArgs(ValueSource.newBuilder().setConstString("$this").build())
                .addArgs(
                    ValueSource.newBuilder()
                        .setConstString("my_field with special\\.chars.also\\\\escap'd")
                        .build())
                .addArgs(ValueSource.newBuilder().setConstString("append").build())
                .build()
          },
          {
            "my_field.path",
            FieldTarget.newBuilder().setPath("my_field.path").setType(FieldType.LOCAL).build()
          },
          {
            "extend my_field.path",
            FunctionCall.newBuilder()
                .setReference(SET_REF)
                .addArgs(ValueSource.newBuilder().setConstString("$this").build())
                .addArgs(ValueSource.newBuilder().setConstString("my_field.path").build())
                .addArgs(ValueSource.newBuilder().setConstString("extend").build())
                .build()
          },
          {
            "my_field[0]",
            FieldTarget.newBuilder().setPath("my_field[0]").setType(FieldType.LOCAL).build()
          },
          {
            "my_field[0].path",
            FieldTarget.newBuilder().setPath("my_field[0].path").setType(FieldType.LOCAL).build()
          },
          {"[0]", FieldTarget.newBuilder().setPath("[0]").setType(FieldType.LOCAL).build()},
          {
            "[0].path",
            FieldTarget.newBuilder().setPath("[0].path").setType(FieldType.LOCAL).build()
          },
          {
            "side my_field",
            FieldTarget.newBuilder().setPath("my_field").setType(FieldType.SIDE).build()
          },
          {
            "side my_field.path",
            FieldTarget.newBuilder().setPath("my_field.path").setType(FieldType.SIDE).build()
          },
          {
            "replace side my_field.path",
            FunctionCall.newBuilder()
                .setReference(SIDE_REF)
                .addArgs(ValueSource.newBuilder().setConstString("my_field.path").build())
                .addArgs(ValueSource.newBuilder().setConstString("replace").build())
                .build()
          },
          {
            "side my_field[0]",
            FieldTarget.newBuilder().setPath("my_field[0]").setType(FieldType.SIDE).build()
          },
          {
            "side my_field[0].path",
            FieldTarget.newBuilder().setPath("my_field[0].path").setType(FieldType.SIDE).build()
          },
          {"side [0]", FieldTarget.newBuilder().setPath("[0]").setType(FieldType.SIDE).build()},
          {
            "side [0].path",
            FieldTarget.newBuilder().setPath("[0].path").setType(FieldType.SIDE).build()
          },
          {"var my_field", VariableTarget.newBuilder().setName("my_field").setPath("").build()},
          {
            "var my_field.path",
            VariableTarget.newBuilder().setName("my_field").setPath(".path").build()
          },
          {
            "var my_field[0]",
            VariableTarget.newBuilder().setName("my_field").setPath("[0]").build()
          },
          {
            "append var my_field[0]",
            FunctionCall.newBuilder()
                .setReference(SET_REF)
                .addArgs(ValueSource.newBuilder().setConstString("my_field").build())
                .addArgs(ValueSource.newBuilder().setConstString("[0]").build())
                .addArgs(ValueSource.newBuilder().setConstString("append").build())
                .build()
          },
          {
            "var my_field[0].path",
            VariableTarget.newBuilder().setName("my_field").setPath("[0].path").build()
          },
          {"var [0]", VariableTarget.newBuilder().setName("[0]").setPath("").build()},
          {"var [0].path", VariableTarget.newBuilder().setName("[0]").setPath(".path").build()},
          {
            "my_field\n\n.my_field1",
            FieldTarget.newBuilder().setPath("my_field.my_field1").setType(FieldType.LOCAL).build()
          },
        });
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler(new Environment("testSide"));
    AbstractMessage got = t.transpile(whistle, WhistleParser::target);
    assertEquals(expectedVS, got);
  }
}
