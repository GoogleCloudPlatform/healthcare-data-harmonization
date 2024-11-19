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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.Signature.Argument.closure;
import static com.google.cloud.verticals.foundations.dataharmonization.Signature.Argument.free;
import static com.google.cloud.verticals.foundations.dataharmonization.Signature.Argument.value;

import com.google.cloud.verticals.foundations.dataharmonization.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class contains (for now, hardcoded) references to various builtin and plugin functions and
 * their signatures.
 */
public final class Functions {
  public static final String BUILTIN_PKG = "builtins";
  // Core:
  public static final FunctionReference TERNARY_REF = builtin("ternary");
  public static final FunctionReference STRFMT_REF = builtin("strFmt");
  public static final FunctionReference ITERATE_REF = builtin("iterate");
  public static final FunctionReference ARRAYOF_REF = builtin("arrayOf");
  public static final FunctionReference GET_REF = builtin("get");
  public static final FunctionReference SET_REF = builtin("set");
  public static final FunctionReference WITH_ERROR_REF = builtin("withError");
  public static final FunctionReference RETHROW_ERROR_REF = builtin("rethrowError");
  public static final FunctionReference TIMED_REF = builtin("timed");
  public static final FunctionReference WITH_TIMEOUT_REF = builtin("withTimeout");
  public static final FunctionReference SIDE_REF = builtin("side");
  public static final FunctionReference WITH_SIDES = builtin("withSides");
  public static final FunctionReference EXTRACT_REGEX = builtin("extractRegex");

  // Operators:
  public static final FunctionReference AND_REF = builtin("and");
  public static final FunctionReference OR_REF = builtin("or");

  // Selectors:
  // TODO(): Use a wildcard package for selectors.
  public static final FunctionReference WHERE_REF = ref("*", "where");
  public static final FunctionReference LAST_REF = ref("*", "last");
  public static final FunctionReference SORTBY_REF = ref("*", "sortBy");
  public static final FunctionReference SORTBY_DESC_REF = ref("*", "sortByDescending");
  public static final FunctionReference GROUPBY_REF = ref("*", "groupBy");
  public static final FunctionReference REDUCE_REF = ref("*", "reduce");
  public static final FunctionReference UNIQUEBY_REF = ref("*", "uniqueBy");
  public static final FunctionReference JOIN_REF = ref("*", "join");

  // Test (plugin):
  public static final FunctionReference RUN_REF = ref("test", "run");
  public static final FunctionReference RUN_SINGLE_REF = ref("test", "runSingle");

  // TODO (): automatically register them from runtime.
  public static final Signature UNKNOWN_FUNCTION_SIGNATURE = Signature.ofVariadic(value());
  private static final Map<FunctionReference, List<Signature>> functionRefToSignatures =
      new HashMap<FunctionReference, List<Signature>>() {
        @CanIgnoreReturnValue
        public Signature addSig(FunctionReference key, Signature value) {
          if (BUILTIN_PKG.equals(key.getPackage())) {
            // Add builtin funcs without package as well, since they can be called without it.
            // TODO (): Use global aliasing resolution here.
            super.computeIfAbsent(ref("", key.getName()), x -> new ArrayList<>()).add(value);
          }
          super.computeIfAbsent(key, x -> new ArrayList<>()).add(value);
          return value;
        }

        {
          // Core functions:
          addSig(
              TERNARY_REF,
              Signature.of(
                  value() /* condition */,
                  closure(LambdaFuncNames.TERNARY_THEN, FunctionType.IMPLICIT) /* then */,
                  closure(LambdaFuncNames.TERNARY_ELSE, FunctionType.IMPLICIT) /* else */));
          addSig(
              TERNARY_REF,
              Signature.of(
                  value() /* condition */,
                  closure(LambdaFuncNames.TERNARY_THEN, FunctionType.IMPLICIT) /* then */));
          addSig(ITERATE_REF, Signature.ofSynchronized());
          addSig(ARRAYOF_REF, Signature.ofVariadic(value()));
          addSig(WITH_SIDES, Signature.of(closure(Signature.any())));
          addSig(EXTRACT_REGEX, Signature.of(value(), value(), closure(free("$"))));

          // Operators:
          addSig(
              AND_REF,
              Signature.ofVariadic(closure(LambdaFuncNames.INFIX_OPERATOR, FunctionType.IMPLICIT)));
          addSig(
              OR_REF,
              Signature.ofVariadic(closure(LambdaFuncNames.INFIX_OPERATOR, FunctionType.IMPLICIT)));
          addSig(
              WITH_ERROR_REF,
              Signature.of(
                  closure(LambdaFuncNames.BLOCK, FunctionType.IMPLICIT),
                  closure("error_", FunctionType.IMPLICIT, free("$error"))));
          addSig(
              RETHROW_ERROR_REF,
              Signature.of(
                  closure(LambdaFuncNames.BLOCK, FunctionType.IMPLICIT),
                  closure("error_", FunctionType.IMPLICIT, free("$error"))));
          addSig(
              TIMED_REF,
              Signature.of(
                  closure(LambdaFuncNames.BLOCK, FunctionType.IMPLICIT),
                  closure("timed_", FunctionType.IMPLICIT, free("$time"))));
          addSig(
              WITH_TIMEOUT_REF,
              Signature.of(
                  closure(LambdaFuncNames.BLOCK, FunctionType.IMPLICIT),
                  value() /* timeoutSeconds */,
                  closure("with_timeout_", FunctionType.IMPLICIT)));

          // Selectors:
          addSig(
              WHERE_REF,
              Signature.of(
                  value(),
                  closure(LambdaFuncNames.selector("where"), FunctionType.LAMBDA, free("$"))));

          addSig(LAST_REF, Signature.of(value()));

          addSig(
              GROUPBY_REF,
              Signature.of(
                  value(),
                  closure(LambdaFuncNames.selector("groupby"), FunctionType.LAMBDA, free("$"))));
          addSig(
              SORTBY_REF,
              Signature.of(
                  value(),
                  closure(LambdaFuncNames.selector("sortby"), FunctionType.LAMBDA, free("$"))));
          addSig(
              SORTBY_DESC_REF,
              Signature.of(
                  value(),
                  closure(LambdaFuncNames.selector("sortbydesc"), FunctionType.LAMBDA, free("$"))));
          addSig(
              REDUCE_REF,
              Signature.of(
                  value(),
                  closure(
                      LambdaFuncNames.selector("accumulator"),
                      FunctionType.LAMBDA,
                      free("$acc"),
                      free("$cur"))));
          addSig(
              REDUCE_REF,
              Signature.of(
                  value(), /* array */
                  value(), /* seed */
                  closure(
                      LambdaFuncNames.selector("accumulator"),
                      FunctionType.LAMBDA,
                      free("$acc"),
                      free("$cur"))));
          addSig(
              UNIQUEBY_REF,
              Signature.of(
                  value(),
                  closure(LambdaFuncNames.selector("uniqueBy"), FunctionType.LAMBDA, free("$"))));
          addSig(
              JOIN_REF,
              Signature.of(
                  value(),
                  value(),
                  closure(
                      LambdaFuncNames.selector("join"),
                      FunctionType.LAMBDA,
                      free("$left"),
                      free("$right"))));

          // Test (plugin):
          addSig(RUN_REF, Signature.of(closure()));
          addSig(RUN_SINGLE_REF, Signature.of(value(), closure()));
        }
      };

  public static final ImmutableMap<String, FunctionReference> OPERATOR_SYMBOL_TO_REF =
      ImmutableMap.<String, FunctionReference>builder()
          .put("+", builtin("sum"))
          .put("-", builtin("sub"))
          .put("*", builtin("mul"))
          .put("/", builtin("div"))
          .put("!=", builtin("neq"))
          .put("==", builtin("eq"))
          .put(">", builtin("gt"))
          .put("<", builtin("lt"))
          .put(">=", builtin("gtEq"))
          .put("<=", builtin("ltEq"))
          .put("!", builtin("not"))
          .put("and", AND_REF)
          .put("or", OR_REF)
          .put("?", builtin("isNotNil"))
          .buildOrThrow();

  public static FunctionReference builtin(String functionName) {
    return ref(BUILTIN_PKG, functionName);
  }

  public static FunctionReference ref(String packageName, String functionName) {
    return FunctionReference.newBuilder().setPackage(packageName).setName(functionName).build();
  }

  public static Signature getSignature(FunctionReference ref, int numArgs) {
    if (functionRefToSignatures.containsKey(ref)) {
      return functionRefToSignatures.get(ref).stream()
          .filter(s -> s.supportsNumArgs(numArgs))
          .findFirst()
          .orElse(UNKNOWN_FUNCTION_SIGNATURE);
    }
    if (ref.getPackage().isEmpty()) {
      ref = FunctionReference.newBuilder(ref).setPackage("*").build();
    }
    return functionRefToSignatures.getOrDefault(ref, new ArrayList<>()).stream()
        .filter(s -> s.supportsNumArgs(numArgs))
        .findFirst()
        .orElse(UNKNOWN_FUNCTION_SIGNATURE);
  }

  public static FunctionReference getOperatorRef(String op) {
    return OPERATOR_SYMBOL_TO_REF.get(op);
  }

  private Functions() {}
}
