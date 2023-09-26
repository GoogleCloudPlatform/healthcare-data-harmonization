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

import com.google.cloud.verticals.foundations.dataharmonization.data.function.reference.DefaultFcnRefProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.data.function.reference.FcnRefProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.data.function.reference.HigherOrderFcnRefProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.data.registry.FunctionRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.ImportInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.targets.FieldTargetProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.targets.TargetProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.targets.VarTargetProtoGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.common.collect.ImmutableMap;

/**
 * TranspilerData records and supplies {@link Transpiler} with const data to allow easier
 * reconfiguration.
 */
public final class TranspilerData {
  public static final String SOURCE_META_KEY = Source.getDescriptor().getFullName();
  public static final String FILE_META_KEY = FileInfo.getDescriptor().getFullName();
  public static final String IMPORT_META_KEY = ImportInfo.getDescriptor().getFullName();
  public static final String SYMBOLS_META_KEY = Symbols.getDescriptor().getFullName();
  public static final String BUILTIN_PKG = "builtins";
  public static final String FUNCTION_INFO_META_KEY = FunctionInfo.getDescriptor().getFullName();
  public static final String THIS = "$this";
  static final String PKG_REF_DELIM = "::";
  static final String INIT_ENV_NAME = "<init>";
  static final String ROOT_VAR = "$root";
  static final String ROOT_FUNCTION_NAME_FORMAT = "%s_root_function";

  private static final ImmutableMap<String, TargetProtoGenerator> TARGETKW_TO_TARGETGEN =
      ImmutableMap.of(
          WhistleHelper.getTokenLiteral(WhistleLexer.VAR),
          new VarTargetProtoGenerator(),
          // TODO(): Remove root keyword.
          WhistleHelper.getTokenLiteral(WhistleLexer.ROOT),
          new FieldTargetProtoGenerator(/*side*/ true),
          WhistleHelper.getTokenLiteral(WhistleLexer.SIDE),
          new FieldTargetProtoGenerator(/*side*/ true),
          // Field is the default target, when there is no keyword.
          "",
          new FieldTargetProtoGenerator(/*root*/ false));

  static TargetProtoGenerator getTargetGenerator(String targetKeyword) {
    if (!TARGETKW_TO_TARGETGEN.containsKey(targetKeyword)) {
      throw new IllegalArgumentException(
          String.format("Unsupported target keyword %s.", targetKeyword));
    }

    return TARGETKW_TO_TARGETGEN.get(targetKeyword);
  }

  /** Lambda function name templates. */
  protected static final class LambdaFuncNames {
    static final String TERNARY_THEN = "ternary-then_";
    static final String TERNARY_ELSE = "ternary-else_";
    static final String INFIX_OPERATOR = "infix-operator-%s_";
    static final String SELECTOR = "selector-%s_";
    static final String BLOCK = "block_";

    private LambdaFuncNames() {}
  }

  /** Function names. */
  protected static final class FunctionNames {

    static final String ITERATE_FUNC = "iterate";
    static final FcnRefProtoGenerator TERNARY_FUNC = new HigherOrderFcnRefProtoGenerator("ternary");
    static final DefaultFcnRefProtoGenerator ARRAYOF_FUNC =
        new DefaultFcnRefProtoGenerator("arrayOf");
    static final DefaultFcnRefProtoGenerator STR_INTERP = new DefaultFcnRefProtoGenerator("strFmt");
    static final DefaultFcnRefProtoGenerator GET_FIELD = new DefaultFcnRefProtoGenerator("get");
    // Function Registration.
    // Map from operator to function name, hard coded for now, to be passed in from runtime later
    // TODO (): automatically register them from runtime.
    static final FunctionRegistry<FcnRefProtoGenerator> OPERATORS =
        new FunctionRegistry<>(
            ImmutableMap.<String, FcnRefProtoGenerator>builder()
                .put("+", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "sum"))
                .put("-", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "sub"))
                .put("*", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "mul"))
                .put("/", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "div"))
                .put("!=", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "neq"))
                .put("==", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "eq"))
                .put(">", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "gt"))
                .put("<", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "lt"))
                .put(">=", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "gtEq"))
                .put("<=", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "ltEq"))
                .put("!", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "not"))
                .put("and", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "and"))
                .put("or", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "or"))
                .put("?", new DefaultFcnRefProtoGenerator(BUILTIN_PKG, "isNotNil"))
                .buildOrThrow());

    private FunctionNames() {}
  }

  private TranspilerData() {}
}
