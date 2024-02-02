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
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Test for expression transpilation that does not require constructing closure functions. */
@RunWith(Parameterized.class)
public class ExpressionTest {
  @Parameter(0)
  public String testName;

  @Parameter(1)
  public String whistle;

  @Parameter(2)
  public Environment env;

  @Parameter(3)
  public ValueSource expectedVS;

  @Parameter(4)
  public Class<? extends Exception> expectedException;

  @Parameters(name = "{0} - `{1}`")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {
            "constant int positive",
            "3",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstInt(3).build(),
            null
          },
          {
            "constant int positive with leading zero",
            "0003",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstInt(3).build(),
            null
          },
          {
            "constant int negative",
            "-3",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstInt(-3).build(),
            null
          },
          {
            "constant int negative with space",
            "-  3",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstInt(-3).build(),
            null
          },
          {
            "constant positive float",
            "3.1415926535",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstFloat(3.1415926535).build(),
            null
          },
          {
            "constant negative float",
            "-3.14",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstFloat(-3.14).build(),
            null
          },
          {
            "constant bool true",
            "true",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstBool(true).build(),
            null
          },
          {
            "constant bool false",
            "false",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstBool(false).build(),
            null
          },
          {
            "constant string",
            "\"hello world\"",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstString("hello world").build(),
            null
          },
          {
            "constant string non-ASCII",
            "\"新年快乐🥰с новым годом ❀\\{(•▽•)\\}❀\"",
            new Environment("testEnv empty"),
            ValueSource.newBuilder().setConstString("新年快乐🥰с новым годом ❀{(•▽•)}❀").build(),
            null
          },
          {
            "string interpolation expression with escaped braces",
            "\"hello \\{\\} and {1 + 1}.\"",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
                        .addArgs(
                            ValueSource.newBuilder().setConstString("hello {} and %s.").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference("+"))
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .build())
                                .build())
                        .build())
                .build(),
            null
          },
          {
            "string interpolation with single expression",
            "\"hello {1 + 1}\"",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setConstString("hello %s").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference("+"))
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .build())
                                .build())
                        .build())
                .build(),
            null
          },
          {
            "string interpolation with multiple expressions",
            "\"hello {1 + 1} and {true}\"",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.STR_INTERP.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setConstString("hello %s and %s").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference("+"))
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .build())
                                .build())
                        .addArgs(ValueSource.newBuilder().setConstBool(true).build())
                        .build())
                .build(),
            null
          },
          {
            "list init empty",
            "[]",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                        .build())
                .build(),
            null
          },
          {
            "list init single element",
            "[1]",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "list init multiple const",
            "[1, \"foo\", true]",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstString("foo").build())
                        .addArgs(ValueSource.newBuilder().setConstBool(true).build())
                        .build())
                .build(),
            null
          },
          {
            "list init multiple comments",
            "[ // Foo, \n  // Bar, \n  // FizzBuzz \n ]",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.ARRAYOF_FUNC.getFunctionReferenceProto())
                        .build())
                .build(),
            null
          },
          {
            "prefix operation",
            "!1",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("!"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "postfix operation",
            "1?",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("?"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "infix operation",
            "1 + 1",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("+"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "infix operation",
            "1 - 1",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("-"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "infix operation",
            "1 * 1",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("*"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "infix operation",
            "1 / 1",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("/"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "composed infix operation",
            "1 + 2 * (3 - 4)",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.OPERATORS.getSymbolReference("+"))
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference("*"))
                                        .addArgs(ValueSource.newBuilder().setConstInt(2))
                                        .addArgs(
                                            ValueSource.newBuilder()
                                                .setFunctionCall(
                                                    FunctionCall.newBuilder()
                                                        .setReference(
                                                            FunctionNames.OPERATORS
                                                                .getSymbolReference("-"))
                                                        .addArgs(
                                                            ValueSource.newBuilder()
                                                                .setConstInt(3)
                                                                .build())
                                                        .addArgs(
                                                            ValueSource.newBuilder()
                                                                .setConstInt(4)
                                                                .build())
                                                        .build())
                                                .build())
                                        .build()))
                        .build())
                .build(),
            null
          },
          {
            "simple input source path defined",
            "foo",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder().setFromLocal("foo").build(),
            null
          },
          {
            "simple input source path undefined",
            "foo",
            new Environment("testEnv empty"),
            ValueSource.getDefaultInstance(),
            TranspilationException.class
          },
          {
            "simple functionCall source",
            "func(1)",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionReference.newBuilder().setName("func").build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .build())
                .build(),
            null
          },
          {
            "simple functionCall source with no arguments",
            "func()",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionReference.newBuilder().setName("func").build())
                        .build())
                .build(),
            null
          },
          {
            "source path with multiple fields no iteration",
            "foo.bar[1].my_field\\ with\\ special\\.chars\\\\\\[2\\].1[*].baz",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setConstString(
                                    ".bar[1].my_field with special\\.chars\\\\\\[2\\].1[*].baz")
                                .build())
                        .build())
                .build(),
            null
          },
          {
            "source path with escaped segment",
            "foo.bar[1].'my_field with special.chars\\\\[2]'.1[*].baz",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setConstString(
                                    ".bar[1].my_field with special\\.chars\\\\\\[2\\].1[*].baz")
                                .build())
                        .build())
                .build(),
            null
          },
          {
            "functionCall source with multiple mixed arguments",
            "$func(foo, \n"
                + "bar[1].my_field\\ with\\ special\\.chars\\\\\\[2\\].1[*].baz, \n"
                + "1, true, \"a string\", \n"
                + "anotherFunc(foo) + 1 \n"
                + ")",
            new Environment(
                "testEnv declared in parent",
                false,
                new Environment(
                    "test parent",
                    false,
                    null,
                    Arrays.asList("bar"),
                    ImmutableList.of(),
                    ImmutableList.of()),
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionReference.newBuilder().setName("$func").build())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo"))
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.GET_FIELD.getFunctionReferenceProto())
                                        .addArgs(
                                            ValueSource.newBuilder().setFromLocal("bar").build())
                                        .addArgs(
                                            ValueSource.newBuilder()
                                                .setConstString(
                                                    "[1].my_field with"
                                                        + " special\\.chars\\\\\\[2\\].1[*].baz")
                                                .build())
                                        .build())
                                .build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstBool(true).build())
                        .addArgs(ValueSource.newBuilder().setConstString("a string").build())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionNames.OPERATORS.getSymbolReference("+"))
                                        .addArgs(
                                            ValueSource.newBuilder()
                                                .setFunctionCall(
                                                    FunctionCall.newBuilder()
                                                        .setReference(
                                                            FunctionReference.newBuilder()
                                                                .setName("anotherFunc")
                                                                .build())
                                                        .addArgs(
                                                            ValueSource.newBuilder()
                                                                .setFromLocal("foo")
                                                                .build())
                                                        .build())
                                                .build())
                                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                                        .build())
                                .build())
                        .build())
                .build(),
            null
          },
          {
            "function call with non existed arguments",
            "func(foo, bar)",
            new Environment(
                "testEnv incomplete",
                false,
                null,
                ImmutableList.of(),
                Arrays.asList("bar"),
                ImmutableList.of()),
            ValueSource.getDefaultInstance(),
            TranspilationException.class
          },
          {
            "function call with path selection",
            "func().bar[1][*][]",
            new Environment(
                "testEnv empty",
                false,
                null,
                ImmutableList.of(),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(
                            ValueSource.newBuilder()
                                .setFunctionCall(
                                    FunctionCall.newBuilder()
                                        .setReference(
                                            FunctionReference.newBuilder().setName("func").build())
                                        .build())
                                .build())
                        .addArgs(ValueSource.newBuilder().setConstString(".bar[1][*]").build())
                        .build())
                .setIterate(true)
                .build(),
            null
          },
          {
            "multiple empty selector in the path - empty selector in the middle",
            "foo.bar[][*][1].baz[]",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.getDefaultInstance(),
            IllegalArgumentException.class
          },
          {
            "multiple empty selector in the path - iterate true",
            "foo.bar[*][1].baz[]",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo").build())
                        .addArgs(ValueSource.newBuilder().setConstString(".bar[*][1].baz"))
                        .build())
                .setIterate(true)
                .build(),
            null
          },
          {
            "multiple empty selector in the path - iterate false",
            "foo.bar[*][1][*]",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo").build())
                        .addArgs(ValueSource.newBuilder().setConstString(".bar[*][1][*]"))
                        .build())
                .build(),
            null
          },
          {
            "simple functionCall with multiple newlines",
            "func(\n\n1,\n\n 2\n\n)",
            new Environment("testEnv empty"),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionReference.newBuilder().setName("func").build())
                        .addArgs(ValueSource.newBuilder().setConstInt(1).build())
                        .addArgs(ValueSource.newBuilder().setConstInt(2).build())
                        .build())
                .build(),
            null
          },
          {
            "multiple selector in path with multiple newlines",
            "foo\n\n.bar[*][1]",
            new Environment(
                "testEnv declared",
                false,
                null,
                Arrays.asList("foo"),
                ImmutableList.of(),
                ImmutableList.of()),
            ValueSource.newBuilder()
                .setFunctionCall(
                    FunctionCall.newBuilder()
                        .setReference(FunctionNames.GET_FIELD.getFunctionReferenceProto())
                        .addArgs(ValueSource.newBuilder().setFromLocal("foo").build())
                        .addArgs(ValueSource.newBuilder().setConstString(".bar[*][1]"))
                        .build())
                .build(),
            null
          },
        });
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler(env);
    FileInfo fileInfo = FileInfo.newBuilder().setUrl("unitTestURI").build();

    if (expectedException != null) {
      assertThrows(
          expectedException, () -> t.transpile(whistle, WhistleParser::expression, fileInfo));
    } else {
      ValueSource got = (ValueSource) t.transpile(whistle, WhistleParser::expression);

      // Strip metas for this test.
      got = clearMeta(got);

      assertEquals(expectedVS, got);
    }
  }
}
