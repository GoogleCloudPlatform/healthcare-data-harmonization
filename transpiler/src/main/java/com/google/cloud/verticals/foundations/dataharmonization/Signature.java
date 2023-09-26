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

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.buildSource;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.symbols.SymbolHelper;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExpressionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.FunctionCallContext;
import com.google.protobuf.Any;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.antlr.v4.runtime.ParserRuleContext;

/** Contains information about a function's signature. */
public final class Signature {
  private final Argument[] args;
  private final boolean isVariadic;

  private Signature(Argument[] args, boolean isVariadic) {
    this.args = args;
    this.isVariadic = isVariadic;
  }

  /** Creates a signature with the given args. */
  public static Signature of(Argument... args) {
    return new Signature(args, false);
  }

  /** Creates a signature with the given args that is variadic (the last arg is variadic). */
  public static Signature ofVariadic(Argument... args) {
    return new Signature(args, true);
  }

  /**
   * Creates a signature where the closure free parameters are synchronized to the arguments given.
   * i.e. like ofVariadic(closure(free("$1"), ... free("$n")), value() 1, ..., value() n))
   */
  public static Signature ofSynchronized() {
    return new Signature(
        new Argument[] {
          Argument.closure(Signature.ofVariadic(Argument.free("$%d"))), Argument.value()
        },
        true);
  }

  /**
   * Creates a signature that just matches any arguments given (including none). Looks like
   * Fn(Data... args).
   */
  public static Signature any() {
    return new Signature(new Argument[] {Argument.value()}, true);
  }

  /** Returns all the names of the free parameters in this Signature. */
  public List<String> getFreeParamNames() {
    return Arrays.stream(args)
        .filter(Argument::isFreeParam)
        .map(Argument::getFreeParamName)
        .collect(Collectors.toList());
  }

  /**
   * Transpiles the provided function reference and argument expressions to a proto FunctionCall
   * object adhering to this Signature, generating lambdas as needed for this Signature's arguments.
   *
   * @param transpiler the transpiler instance.
   * @param ref a reference of the function being called (i.e. that this signature represents).
   * @param givenArgs the expressions of the arguments to this function. Expressions may be enclosed
   *     in lambdas if the signature's corresponding argument is a closure.
   * @param sourceContext the parser rule that started this call, used for adding source meta.
   */
  public FunctionCall transpileFunctionCall(
      Transpiler transpiler,
      FunctionReference ref,
      List<ExpressionContext> givenArgs,
      ParserRuleContext sourceContext) {
    FunctionCall.Builder call = FunctionCall.newBuilder();
    call.setReference(ref);
    for (int i = 0; i < givenArgs.size(); i++) {

      // skip null arguments; selectors without accompanying expressions or
      // variable defaults to having null as argument.
      if (givenArgs.get(i) == null) {
        continue;
      }
      if (i >= args.length && !isVariadic) {
        // TODO(rpolyano): Use sourceContext.addErrorNode() ?
        throw new IllegalArgumentException(
            String.format(
                "Too many parameters for %s::%s: want %d, got %d: %s",
                ref.getPackage(),
                ref.getName(),
                args.length,
                givenArgs.size(),
                givenArgs.stream()
                    .map(ExpressionContext::getText)
                    .collect(Collectors.joining(", "))));
      }

      // The type of argument we want:
      Argument argType = args.length > 0 ? args[Math.min(i, args.length - 1)] : Argument.value();

      ValueSource argVs;
      // Depending on the type of arg we want, we transpile the expression in different ways:
      if (argType.isClosure()) {
        // If we want a closure, we transpile to a lambda closure.

        // If the closure is variadic then we want one free param for each remaining
        // expression/argument.
        Signature closureSig = argType.getClosureSignature();
        if (closureSig.isVariadic) {
          int numArgs = givenArgs.size() - i - 1;
          closureSig =
              Signature.of(
                  IntStream.range(1, numArgs + 1)
                      .mapToObj(j -> Argument.free(numArgs == 1 ? "$" : String.format("$%d", j)))
                      .toArray(Argument[]::new));
        }

        argVs =
            ValueSource.newBuilder()
                .setFunctionCall(
                    LambdaHelper.lambda(
                        transpiler,
                        closureSig,
                        givenArgs.get(i),
                        argType.lambdaPrefix,
                        argType.lambdaType))
                .build();
      } else if (argType.isFreeParam()) {
        // If we want a free param, we ignore the expression value (the caller will need to deal
        // with separately).
        argVs = ValueSource.newBuilder().setFreeParameter(argType.getFreeParamName()).build();
      } else {
        // Otherwise we just want a regular value so we transpile it directly.
        argVs = (ValueSource) givenArgs.get(i).accept(transpiler);
      }
      call.addArgs(argVs);
    }

    Meta.Builder meta = Meta.newBuilder();
    meta.putEntries(SOURCE_META_KEY, Any.pack(buildSource(sourceContext)));
    if (sourceContext instanceof FunctionCallContext) {
      meta = SymbolHelper.withSymbol(meta, (FunctionCallContext) sourceContext);
    }

    call.setMeta(meta);
    return call.build();
  }

  public boolean supportsNumArgs(int numArgs) {
    return args.length == numArgs || (isVariadic && numArgs >= args.length - 1);
  }

  /** Represents information about an argument that may be a closure with some free parameters. */
  public static final class Argument {
    private final Signature closureSignature;
    private final String freeParamName;
    private final String lambdaPrefix;
    private final FunctionType lambdaType;

    private Argument(Signature closureSignature, String lambdaPrefix, FunctionType lambdaType) {
      this.closureSignature = closureSignature;
      this.lambdaPrefix = lambdaPrefix;
      this.lambdaType = lambdaType;
      this.freeParamName = null;
    }

    private Argument(String freeParamName) {
      this.freeParamName = freeParamName;
      this.closureSignature = null;
      this.lambdaPrefix = null;
      this.lambdaType = null;
    }

    private Argument() {
      this.freeParamName = null;
      this.closureSignature = null;
      this.lambdaPrefix = null;
      this.lambdaType = null;
    }

    /** Creates an argument that should be a closure. */
    public static Argument closure(Argument... closureArgs) {
      return new Argument(Signature.of(closureArgs), "lambda_", FunctionType.LAMBDA);
    }

    /** Creates an argument that should be a closure with the given signature. */
    public static Argument closure(Signature closureSignature) {
      return new Argument(closureSignature, "lambda_", FunctionType.LAMBDA);
    }

    /**
     * Creates an argument that should be a closure, with the given lambda attributes to determine
     * naming and meta for the generated function.
     */
    public static Argument closure(
        String lambdaPrefix, FunctionType lambdaType, Argument... closureArgs) {
      return new Argument(Signature.of(closureArgs), lambdaPrefix, lambdaType);
    }

    /** Creates an argument that is a free parameter in a closure. */
    public static Argument free(String freeParamName) {
      return new Argument(freeParamName);
    }

    /** Creates an argument that is a regular value. */
    public static Argument value() {
      return new Argument();
    }

    public Signature getClosureSignature() {
      return closureSignature;
    }

    public String getFreeParamName() {
      return freeParamName;
    }

    public boolean isClosure() {
      return closureSignature != null;
    }

    public boolean isFreeParam() {
      return freeParamName != null;
    }
  }
}
