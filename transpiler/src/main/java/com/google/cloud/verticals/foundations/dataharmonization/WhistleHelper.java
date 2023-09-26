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

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.FUNCTION_INFO_META_KEY;
import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.protobuf.Any;
import java.util.Arrays;
import java.util.List;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

/** Helper methods for working with the generated ANTLR code. */
public final class WhistleHelper {

  /**
   * Given a token id, like {@link WhistleLexer#VAR} returns it's literal. See {@link
   * Vocabulary#getLiteralName(int)}.
   *
   * @throws IllegalArgumentException if the token does not have a literal representation.
   */
  public static String getTokenLiteral(int token) {
    String tokenLiteral = WhistleLexer.VOCABULARY.getLiteralName(token);
    if (tokenLiteral == null) {
      throw new IllegalArgumentException(
          String.format(
              "Token %s does not have a string literal.",
              WhistleLexer.VOCABULARY.getDisplayName(token)));
    }

    // Strip the single quotes
    return tokenLiteral.substring(1, tokenLiteral.length() - 1);
  }

  /**
   * Returns a Source position proto that describes the range of text covered by the given rule
   * context.
   */
  public static Source buildSource(ParserRuleContext context) {
    return buildSource(context.getStart(), context.getStop());
  }

  /**
   * Returns a Source position proto that describes the range of text covered by the given token.
   */
  public static Source buildSource(Token context) {
    return buildSource(context, context);
  }
  /**
   * Returns a Source position proto that describes the range between (and including) the two given
   * tokens.
   */
  public static Source buildSource(Token start, Token end) {
    int endLineNum = end.getLine();
    // when the last token span multiple lines we want to account for that
    // the increment to the end line number is usually splitLastLine.length - 1
    // however when the token ends in "\n" we don't count the next line for readability.
    String[] splitLastLine = end.getText().split("\n", -1);
    endLineNum += (splitLastLine.length - 1);
    if (splitLastLine.length > 1 && splitLastLine[splitLastLine.length - 1].isEmpty()) {
      endLineNum -= 1;
    }
    int endColNum = 0;
    String[] endLines = end.getText().split("\n", -1);
    if (endLines.length > 1 && endLines[endLines.length - 1].isEmpty()) {
      endColNum -= 1;
      endLines = Arrays.copyOf(endLines, endLines.length - 1);
    }
    if (endLines.length == 1) {
      endColNum += (end.getCharPositionInLine() + end.getStopIndex() - end.getStartIndex());
    } else {
      endColNum += endLines[endLines.length - 1].length();
    }
    return Source.newBuilder()
        .setStart(
            SourcePosition.newBuilder()
                .setLine(start.getLine())
                .setColumn(start.getCharPositionInLine()))
        .setEnd(SourcePosition.newBuilder().setLine(endLineNum).setColumn(endColNum))
        .build();
  }

  public static Meta.Builder sourceMeta(Source source) {
    return Meta.newBuilder().putEntries(SOURCE_META_KEY, Any.pack(source));
  }

  public static Meta.Builder functionMeta(ParserRuleContext context, FunctionType funcType) {
    return Meta.newBuilder()
        .putEntries(SOURCE_META_KEY, Any.pack(buildSource(context)))
        .putEntries(
            FUNCTION_INFO_META_KEY, Any.pack(FunctionInfo.newBuilder().setType(funcType).build()));
  }

  public static Meta.Builder functionMeta(
      List<? extends ParserRuleContext> contexts, FunctionType funcType) {
    Meta.Builder meta = Meta.newBuilder();
    if (!contexts.isEmpty()) {
      meta.putEntries(
          SOURCE_META_KEY,
          Any.pack(
              buildSource(
                  contexts.get(0).getStart(), contexts.get(contexts.size() - 1).getStop())));
    }

    return meta.putEntries(
        FUNCTION_INFO_META_KEY, Any.pack(FunctionInfo.newBuilder().setType(funcType).build()));
  }

  public static Meta.Builder argAliasMeta(ParserRuleContext context, Argument argument) {
    return Meta.newBuilder().putEntries(SOURCE_META_KEY, Any.pack(buildSource(context)));
  }

  private WhistleHelper() {}
}
