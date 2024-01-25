// Copyright 2023 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;

import static com.google.cloud.verticals.foundations.dataharmonization.WhistleHelper.getTokenLiteral;

import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleLexer;
import com.google.common.collect.ImmutableList;

/** Constants used in the Whistler Linter tool. */
public final class LinterConstants {
  private LinterConstants() {}

  public static final String DEF = getTokenLiteral(WhistleLexer.DEF);
  public static final String EOF = "<EOF>";
  public static final String VAR = getTokenLiteral(WhistleLexer.VAR);
  public static final String IF = getTokenLiteral(WhistleLexer.IF);
  public static final String THEN = getTokenLiteral(WhistleLexer.THEN);
  public static final String ELSE = getTokenLiteral(WhistleLexer.ELSE);
  public static final String OPEN_BRACKET = getTokenLiteral(WhistleLexer.OPEN_BRACKET);
  public static final String CLOSE_BRACKET = getTokenLiteral(WhistleLexer.CLOSE_BRACKET);
  public static final String COLON = ":";
  public static final String SEMICOLON = ";";
  public static final String SPACE = " ";
  public static String indent = "  ";
  public static final String NEWLINE = "\n";

  public static final ImmutableList<String> INFIX_OPERATORS =
      ImmutableList.of("+", "-", "/", "*", "==", "!=", ">", ">=", "<", "<=", "and", "or");

  public static void setIndent(int indentSize) {
    LinterConstants.indent = " ".repeat(indentSize);
  }
}
