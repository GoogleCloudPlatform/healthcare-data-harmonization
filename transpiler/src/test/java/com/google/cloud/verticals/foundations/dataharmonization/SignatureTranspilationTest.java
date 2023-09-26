/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.verticals.foundations.dataharmonization.Signature.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallContext;
import java.util.Arrays;
import java.util.Collection;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for {@link Signature#transpileFunctionCall}. */
@RunWith(Parameterized.class)
public class SignatureTranspilationTest {

  @Parameter(0)
  public String name;

  @Parameter(1)
  public String whistle;

  @Parameter(2)
  public Signature signature;

  @Parameter(3)
  public FunctionCall expected;

  @Parameter(4)
  public Exception expectedException;

  @Parameters(name = "{0}")
  public static Collection<Object[]> cases() {
    return Arrays.asList(
        new Object[] {
          /* name= */ "no args",
          /* whistle= */ "test()",
          /* signature= */ Signature.of(),
          /* expected proto = */ functionCall("test"),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "const arg",
          /* whistle= */ "test(1)",
          /* signature= */ Signature.of(Argument.value()),
          /* expected proto = */ functionCall("test", num(1)),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "many const args",
          /* whistle= */ "test(1, 2)",
          /* signature= */ Signature.of(Argument.value(), Argument.value()),
          /* expected proto = */ functionCall("test", num(1), num(2)),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "free arg",
          /* whistle= */ "test(freeArg)",
          /* signature= */ Signature.of(Argument.free("freeArg")),
          /* expected proto = */ functionCall("test", free("freeArg")),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "closure arg",
          /* whistle= */ "test(callback())",
          /* signature= */ Signature.of(Argument.closure()),
          /* expected proto = */ functionCall(
              "test", functionCallValueSource("lambda_ffffffff-b8d5-9fd6-ffff-ffffbc12dbb4")),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "closure arg with free arg",
          /* whistle= */ "test(callback(freeArg))",
          /* signature= */ Signature.of(Argument.closure(Argument.free("freeArg"))),
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource(
                  "lambda_00000000-31e9-f345-ffff-ffffb73ee369", free("freeArg"))),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "closure arg with mixed args",
          /* whistle= */ "test(callback(1, freeArg))",
          /* signature= */ Signature.of(
              Argument.closure(Argument.value(), Argument.free("freeArg"))),
          // Constant args should not show up in the lambda call; They are transferred down to the
          // call to callback inside the lambda definition.
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource(
                  "lambda_ffffffff-aaca-f867-0000-0000070c5774", free("freeArg"))),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "closure arg with outer value",
          /* whistle= */ "test(callback(placeholderVar, freeArg))",
          /* signature= */ Signature.of(
              Argument.closure(Argument.value(), Argument.free("freeArg"))),
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource(
                  "lambda_ffffffff-c747-e698-0000-00004d95fa51",
                  free("freeArg"),
                  local("placeholderVar"))),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "variadic",
          /* whistle= */ "test(1, 2, 3)",
          /* signature= */ Signature.ofVariadic(Argument.value()),
          /* expected proto = */ functionCall("test", num(1), num(2), num(3)),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "variadic is optional",
          /* whistle= */ "test()",
          /* signature= */ Signature.ofVariadic(Argument.value()),
          /* expected proto = */ functionCall("test"),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "closure arg with any args",
          /* whistle= */ "test(callback(1, placeholderVar))",
          /* signature= */ Signature.of(Argument.closure(Signature.any())),
          // Constant args should not show up in the lambda call; They are transferred down to the
          // call to callback inside the lambda definition.
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource(
                  "lambda_" + "ffffffff-9e5d-1176-ffff-ffffb3b88942", local("placeholderVar"))),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "synchronized closure",
          /* whistle= */ "test(callback($1, $2, $3), 10, 20, 30)",
          /* signature= */ Signature.ofSynchronized(),
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource(
                  "lambda_ffffffff-9f83-48fd-0000-00003faffa1a",
                  free("$1"),
                  free("$2"),
                  free("$3")),
              num(10),
              num(20),
              num(30)),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "synchronized closure 1 arg",
          /* whistle= */ "test(callback($), 10)",
          /* signature= */ Signature.ofSynchronized(),
          /* expected proto = */ functionCall(
              "test",
              functionCallValueSource("lambda_00000000-3c9b-c140-0000-0000446b8453", free("$")),
              num(10)),
          /* expected exception = */ null
        },
        new Object[] {
          /* name= */ "incorrect number of args",
          /* whistle= */ "test(\"a\", 2, \"c\")",
          /* signature= */ Signature.of(Argument.value(), Argument.value()),
          /* expected proto = */ null,
          /* expected exception = */ new IllegalArgumentException(
              "Too many parameters for ::test: want 2, got 3: \"a\", 2, \"c\"")
        });
  }

  private static ValueSource functionCallValueSource(String name, ValueSource... args) {
    return ValueSource.newBuilder().setFunctionCall(functionCall(name, true, args)).build();
  }

  private static FunctionCall functionCall(String name, ValueSource... args) {
    return functionCall(name, false, args);
  }

  private static FunctionCall functionCall(String name, boolean closure, ValueSource... args) {
    return FunctionCall.newBuilder()
        .setBuildClosure(closure)
        .setReference(FunctionReference.newBuilder().setName(name))
        .addAllArgs(Arrays.asList(args))
        .build();
  }

  private static ValueSource num(int num) {
    return ValueSource.newBuilder().setConstInt(num).build();
  }

  private static ValueSource free(String name) {
    return ValueSource.newBuilder().setFreeParameter(name).build();
  }

  private static ValueSource local(String name) {
    return ValueSource.newBuilder().setFromLocal(name).build();
  }

  @BeforeClass
  public static void setUp() {
    LambdaHelper.setSeed(100);
  }

  @Test
  public void test() {
    CharStream stream = CharStreams.fromString(whistle);
    WhistleLexer wstlLexer = new WhistleLexer(stream);
    CommonTokenStream tokens = new CommonTokenStream(wstlLexer);

    WhistleParser wstlParser = new WhistleParser(tokens);

    FunctionCallContext callExpr = wstlParser.functionCall();

    Environment env = new Environment("root");
    env.declareOrInheritVariable("placeholderVar");
    Transpiler transpiler = new Transpiler(env);

    if (expectedException == null) {
      FunctionCall actual =
          signature.transpileFunctionCall(
              transpiler,
              (FunctionReference) callExpr.functionName().accept(transpiler),
              callExpr.expression(),
              callExpr);
      // Strip meta for this test.
      actual = TestHelper.clearMeta(actual);

      assertEquals(expected, actual);
      assertTrue(signature.supportsNumArgs(expected.getArgsCount()));
    } else {
      Exception got =
          assertThrows(
              expectedException.getClass(),
              () ->
                  signature.transpileFunctionCall(
                      transpiler,
                      (FunctionReference) callExpr.functionName().accept(transpiler),
                      callExpr.expression(),
                      callExpr));
      assertTrue(
          String.format(
              "Expected %s to contain %s", got.getMessage(), expectedException.getMessage()),
          got.getMessage().contains(expectedException.getMessage()));
    }
  }
}
