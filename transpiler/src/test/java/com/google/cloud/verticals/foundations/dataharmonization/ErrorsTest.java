/*
 * Copyright 2021 Google LLC.
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationIssue;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Message;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import org.antlr.v4.runtime.RuleContext;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Tests for error strategy and transpilation error messages. */
@RunWith(Parameterized.class)
public class ErrorsTest {
  private final String whistle;
  private final List<String> wantMessageSnippets;
  private final Function<WhistleParser, RuleContext> rule;
  private final int[] wantLineColNums;

  public ErrorsTest(
      String name,
      String whistle,
      List<String> wantMessageSnippets,
      int[] wantLineColNums,
      Function<WhistleParser, RuleContext> rule) {
    this.whistle = whistle;
    this.wantMessageSnippets = wantMessageSnippets;
    this.rule = rule;
    this.wantLineColNums = wantLineColNums;
  }

  @Parameters(name = "errors - {0}")
  public static Collection<Object[]> data() {
    return ImmutableList.of(
        new Object[] {
          "unwanted token",
          "import \"foo\"\noption foo",
          ImmutableList.of("2:1-2:7 - unexpected 'option', expecting"),
          new int[] {2, 1, 2, 7},
          (Function<WhistleParser, RuleContext>) WhistleParser::program
        },
        new Object[] {
          "input mismatch",
          "option 1",
          ImmutableList.of("1:8-1:9 - unexpected '1', expecting"),
          new int[] {1, 8, 1, 9},
          (Function<WhistleParser, RuleContext>) WhistleParser::program
        },
        new Object[] {
          "extraneous token",
          "package test\n1 + 1;;",
          ImmutableList.of("2:7-2:8 - unexpected ';'"),
          new int[] {2, 7, 2, 8},
          (Function<WhistleParser, RuleContext>) WhistleParser::program
        },
        new Object[] {
          "missing token",
          "1.",
          ImmutableList.of("1:3-1:4 - expecting INTEGER at '<EOF>'"),
          new int[] {1, 3, 1, 4},
          (Function<WhistleParser, RuleContext>) WhistleParser::expression
        },
        new Object[] {
          "no viable alternative",
          "package test\n1 + 1/;",
          ImmutableList.of("2:6-2:8 - unexpected input at /;"),
          new int[] {2, 6, 2, 8},
          (Function<WhistleParser, RuleContext>) WhistleParser::program
        },
        new Object[] {
          "input mismatch exception",
          "import \"foo\"\nvar d:\n\n",
          ImmutableList.of(
              "2:6-2:7 - unexpected '\\n" + "', expecting one of {'if',", // ...
              " IDENTIFIER (Examples of Identifiers include, Function, Variable and Package"
                  + " names), STRING}"),
          new int[] {2, 6, 2, 7},
          (Function<WhistleParser, RuleContext>) WhistleParser::program
        },
        new Object[] {
          "merge mode with append",
          "extend var foo.bar[].baz: 123",
          ImmutableList.of("Merge modes ('extend') cannot be used with array appends []"),
          new int[] {1, 1, 1, 21},
          (Function<WhistleParser, RuleContext>) WhistleParser::target
        });
  }

  @Test
  public void test() {
    Transpiler t = new Transpiler();

    Message got;
    try {
      got = t.transpile(whistle, rule, FileInfo.newBuilder().setUrl("test.wstl").build());
    } catch (TranspilationException ex) {
      String message = ex.getMessage();
      wantMessageSnippets.forEach(
          want ->
              assertTrue(
                  String.format("Error does not contain %s\nGot: %s", want, message),
                  message.contains(want)));
      TranspilationIssue issue = ex.getIssues().get(0);
      assertEquals(wantLineColNums[0], issue.getLine());
      assertEquals(wantLineColNums[1], issue.getCol());
      assertEquals(wantLineColNums[2], issue.getEndLine());
      assertEquals(wantLineColNums[3], issue.getEndCol());
      return;
    }

    fail(
        String.format(
            "Expected an exception but got a transpiled message:\n"
                + "Original Whistle:\n"
                + "%s\n"
                + "Transpiled Result:\n"
                + "%s",
            whistle, got));
  }
}
