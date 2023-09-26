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

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.IMPORT_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.sourceMeta;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FunctionNames;
import com.google.cloud.verticals.foundations.dataharmonization.data.Packages;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.ImportInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.ExpressionContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetPathContext;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.TargetPathSegmentContext;
import com.google.protobuf.Any;
import java.util.Arrays;
import java.util.List;

/** * Helper functions for {@link Transpiler}. */
public final class TranspilerHelper {
  static ValueSource constructFunctionCallVS(
      FunctionReference functionReference,
      Meta.Builder meta,
      boolean buildClosure,
      List<ValueSource> args) {
    FunctionCall.Builder vs =
        FunctionCall.newBuilder()
            .setReference(functionReference)
            .addAllArgs(args)
            .setBuildClosure(buildClosure);
    if (meta != null) {
      vs.setMeta(meta);
    }
    return ValueSource.newBuilder().setFunctionCall(vs.build()).build();
  }

  static Import constructImport(ValueSource valueSource, Source source, String pathCode) {
    ImportInfo importInfo = ImportInfo.newBuilder().setPathCode(pathCode).build();
    Meta.Builder meta = Meta.newBuilder().putEntries(IMPORT_META_KEY, Any.pack(importInfo));
    if (source != null) {
      meta.putEntries(SOURCE_META_KEY, Any.pack(source));
    }
    if (Packages.ALIAS.containsKey(valueSource.getConstString())) {
      valueSource =
          ValueSource.newBuilder()
              .setConstString(Packages.ALIAS.get(valueSource.getConstString()))
              .build();
    }
    return Import.newBuilder().setValue(valueSource).setMeta(meta).build();
  }

  /**
   * Convert expression to Lambda functionCall ValueSource
   *
   * @param transpiler transpiler to keep track of visitor information
   * @param prefix: prefix used to generate name of the lambda function
   * @param expr: ExpressionContext to convert.
   * @param freeArgNames: list of free Arg names.
   * @return a ValueSource of the lambda Closure.
   */
  static ValueSource expressionToLambdaFunctionCallVS(
      Transpiler transpiler,
      String prefix,
      ExpressionContext expr,
      List<String> freeArgNames,
      FunctionType type) {
    // TODO(rpolyano): Remove this function.
    FunctionCall closure =
        LambdaHelper.lambda(
            transpiler,
            Signature.of(
                freeArgNames.stream()
                    .map(Signature.Argument::free)
                    .toArray(Signature.Argument[]::new)),
            expr,
            prefix,
            type);
    return ValueSource.newBuilder().setFunctionCall(closure).build();
  }

  static String generateTemplate(String str) {
    return str.replaceAll("(?<!\\\\)\\{\\}", "%s"); // {} not preceded by \ is replaced by %s.
  }

  /**
   * Processes escaped strings (or identifiers) into Java string literals.
   *
   * <p>In quoted strings everything is unescaped; Escapes (\ prefix) are only meaningful in the
   * Whistle syntax - we escape things like \' and \" and \{ etc, but these do not need to be
   * escaped in the proto. Additionally, \n \r \t and \\ are converted to their char equivalents.
   *
   * <p>In identifiers/path segments these rules are followed:
   *
   * <ol>
   *   <li>'x' -> \x if x is [, ], or . (and x is not already escaped as \x)
   *   <li>\x -> x if x is not \, [, ], or . (and the \ is not escaped itself)
   * </ol>
   */
  public static String preprocessString(String str) {
    if (str.startsWith("\"") && str.endsWith("\"")) {
      return preprocessConstString(str);
    }

    if (str.startsWith("'") && str.endsWith("'")) {
      str = preprocessQuotedIdentifier(str);
    }

    // On all identifiers (quoted and not), \x -> x if x is not \, [, ], or .
    return unescape(str, "[^\\\\.\\[\\]]");
  }

  private static String preprocessConstString(String str) {
    // Strip quotes.
    str = str.substring(1, str.length() - 1);

    // Unescape everything except \r, \n, and \t.
    str = unescape(str, "[^nrt\\\\]");
    // Split by \\ - this removes all escaped \ from the string.
    return stream(str.split("\\\\\\\\", -1))
        // Replace \n (two chars) with single char \n, same with \r, and \t.
        .map(s -> s.replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t"))
        // Re-join with single \ (i.e. replacing) \\ from the split with single \.
        .collect(joining("\\"));
  }

  private static String preprocessQuotedIdentifier(String str) {
    // Strip quotes.
    str = str.substring(1, str.length() - 1);

    // 'x' -> \x if x is [, ], or .
    return escape(str, "[\\[\\].]");
  }

  /**
   * Returns given content, with all matches of patternToEscape prefixed with \ if and only if they
   * weren't already prefixed. Takes into account \pattern (will be untouched), vs \\pattern (will
   * become \\\pattern), vs \\\pattern (will be untouched), etc.
   */
  private static String escape(String content, String patternToEscape) {
    // This regex is matching ((\\)+)(<patternToEscape>)|(<neg look behind>\)(<pattern to escape>)
    //                        ^ $1   ^ $2                                    ^$3
    // That is, the pattern to escape preceeded by an even number of backslashes or no backslashes.
    // The even backslashes are left in place ($1), since they all escape each other. The pattern
    // match itself ($2 and $3, only one of which will match because of |) is prefixed with \ (i.e.
    // escaped).
    String pattern =
        String.format("((?:\\\\\\\\)+)(%s)|(?<!\\\\)(%s)", patternToEscape, patternToEscape);
    return content.replaceAll(pattern, "$1\\\\$2$3");
  }

  /**
   * Returns given content, with all matches of \patternToEscape stripped of the \ prefix, if and
   * only if the prefix wasn't itself escaped. Takes into account \pattern (will become pattern), vs
   * \\pattern (will be untouched), vs \\\pattern (will become \\pattern), etc.
   */
  private static String unescape(String content, String patternToUnescape) {
    // This regex is matching ((\\)*)((\)?(<patternToEscape>))
    //                        ^ $1        ^ $2
    // That is, the pattern to unescape preceeded by an odd number of backslashes or no backslashes.
    // The preceeding even number of backslashes are left in place ($1), since they all escape
    // each other. The final odd backslash is removed (i.e. the match is unescaped).
    String pattern = String.format("((?:\\\\\\\\)*)(?:\\\\)?(%s)", patternToUnescape);
    return content.replaceAll(pattern, "$1$2");
  }

  static ValueSource getPathOnValue(ValueSource src, Source codeSource, StringBuilder path) {
    ValueSource result;
    if (path.length() == 0) {
      result = src;
    } else {
      result =
          constructFunctionCallVS(
              FunctionNames.GET_FIELD.getFunctionReferenceProto(),
              sourceMeta(codeSource),
              false,
              Arrays.asList(src, ValueSource.newBuilder().setConstString(path.toString()).build()));
    }
    return result;
  }

  static String extractPath(TargetPathContext ctx) {
    if (ctx == null || ctx.targetPathSegment() == null) {
      return "";
    }
    return ctx.targetPathSegment().stream()
        .reduce("", (all, next) -> all + preprocessPathSegment(next), String::concat);
  }

  private static String preprocessPathSegment(TargetPathSegmentContext ctx) {
    if (ctx.fieldName() != null && ctx.fieldName().identifier() != null) {
      return ctx.fieldName().PATH_DELIM().getText()
          + preprocessString(ctx.fieldName().identifier().getText());
    }

    return preprocessString(ctx.getText());
  }

  private TranspilerHelper() {}
}
