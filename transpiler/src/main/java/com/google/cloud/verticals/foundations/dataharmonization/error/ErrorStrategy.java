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
package com.google.cloud.verticals.foundations.dataharmonization.error;

import com.google.cloud.verticals.foundations.dataharmonization.WhistleVocabulary;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.common.collect.ImmutableSet;
import com.google.common.flogger.GoogleLogger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.NoViableAltException;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.misc.IntervalSet;

/** Error strategy that accumulates issues into a list. */
public class ErrorStrategy extends DefaultErrorStrategy {
  private final FileInfo file;
  private final List<TranspilationIssue> issues = new ArrayList<>();
  private static final GoogleLogger logger = GoogleLogger.forEnclosingClass();

  public ErrorStrategy(FileInfo file) {
    this.file = file;
  }

  @Override
  protected void reportInputMismatch(Parser recognizer, InputMismatchException e) {
    logger.atInfo().log("Reporting input Mismatch");

    // Fetch the last non-empty token
    Token token =
        fetchLastNonEmptyToken(e.getOffendingToken(), (CommonTokenStream) e.getInputStream());

    int startLine = token.getLine();
    int startCol = token.getCharPositionInLine() + 1;
    int endLine = token.getLine();
    int endCol = token.getCharPositionInLine() + token.getText().length() + 1;

    issues.add(
        new TranspilationIssue(
            file,
            startLine,
            startCol,
            endLine,
            endCol,
            String.format(
                "unexpected %s, expecting %s",
                getTokenErrorDisplay(e.getOffendingToken()),
                generateExpectedErrorMessage(e.getExpectedTokens(), recognizer))));
  }

  @Override
  protected void reportUnwantedToken(Parser recognizer) {
    logger.atInfo().log("Reporting unwanted Token");

    // From DefaultErrorStrategy
    if (inErrorRecoveryMode(recognizer)) {
      return;
    }
    beginErrorCondition(recognizer);

    // Fetch the last non-empty token
    Token token =
        fetchLastNonEmptyToken(
            recognizer.getCurrentToken(), (CommonTokenStream) recognizer.getInputStream());

    int startLine = token.getLine();
    int startCol = token.getCharPositionInLine() + 1;
    int endLine = token.getLine();
    int endCol = token.getCharPositionInLine() + token.getText().length() + 1;

    issues.add(
        new TranspilationIssue(
            file,
            startLine,
            startCol,
            endLine,
            endCol,
            String.format(
                "unexpected %s, expecting %s",
                getTokenErrorDisplay(token),
                generateExpectedErrorMessage(getExpectedTokens(recognizer), recognizer))));
  }

  /**
   * Helper function which is used to fetch the last non-empty token. If the parser is returning a
   * newline or EOF error token, we fetch the last non-empty token and add use this token to render
   * a highlighted error. This is due to Jupyter not highlighting errors on empty tokens.
   *
   * @param currToken The current error token.
   * @param tokenStream The stream of all parsed tokens.
   * @return The last non-empty token.
   */
  private Token fetchLastNonEmptyToken(Token currToken, CommonTokenStream tokenStream) {

    List<Token> allTokens = tokenStream.getTokens();
    int currentIndex = currToken.getTokenIndex();
    while (currToken.getText().equals("\n") || currToken.getText().equals("<EOF>")) {
      currToken = allTokens.get(currentIndex--);
    }
    return currToken;
  }

  @Override
  protected void reportMissingToken(Parser recognizer) {

    logger.atInfo().log("Reporting missing Token");

    // From DefaultErrorStrategy
    if (inErrorRecoveryMode(recognizer)) {
      return;
    }
    beginErrorCondition(recognizer);

    Token offendingToken = recognizer.getCurrentToken();
    issues.add(
        new TranspilationIssue(
            file,
            offendingToken.getLine(),
            offendingToken.getCharPositionInLine() + 1,
            String.format(
                "expecting %s at %s",
                generateExpectedErrorMessage(getExpectedTokens(recognizer), recognizer),
                getTokenErrorDisplay(offendingToken))));
  }

  /**
   * Helper method which makes use of {@link WhistleVocabulary} to generate tokens from a given
   * IntervalSet, and then generate the required error message string.
   *
   * @param intervalSet Set of intervals which map to tokens defined in the whistle grammar.
   * @param recognizer the Whistle parser which holds a default vocabulary.
   * @return an error message string.
   */
  private String generateExpectedErrorMessage(IntervalSet intervalSet, Parser recognizer) {
    WhistleVocabulary whistleVocabulary = new WhistleVocabulary(recognizer.getVocabulary());
    ImmutableSet<String> tokens = whistleVocabulary.getDisplayName(intervalSet.toList());

    if (tokens.size() == 1) {
      return tokens.iterator().next();
    } else {
      return "one of {" + String.join(", ", tokens) + "}";
    }
  }

  @Override
  protected void reportNoViableAlternative(Parser recognizer, NoViableAltException e) {

    logger.atInfo().log("Reporting no viable alternative");

    String location = getStringBetweenTokens(recognizer, e.getStartToken(), e.getOffendingToken());
    issues.add(
        new TranspilationIssue(
            file,
            e.getStartToken().getLine(),
            e.getStartToken().getCharPositionInLine() + 1,
            e.getOffendingToken().getLine(),
            e.getOffendingToken().getCharPositionInLine()
                + e.getOffendingToken().getText().length()
                + 1,
            String.format("unexpected input at %s", location)));
  }

  // Helper method to add transpilationIssue to the list of issues
  public void addIssue(TranspilationIssue transpilationIssue) {
    issues.add(transpilationIssue);
  }

  private String getStringBetweenTokens(Parser recognizer, Token startToken, Token endToken) {
    TokenStream stream = recognizer.getInputStream();
    if (stream == null) {
      // We can't get the stream (for some reason, not sure how this can happen), so provide the
      // information we do have.
      return String.format(
          "%s...%s", getTokenErrorDisplay(startToken), getTokenErrorDisplay(endToken));
    }
    if (startToken.getType() == Token.EOF) {
      // End token is not meaningful if start token is the end of the file.
      return getTokenErrorDisplay(startToken);
    }

    return stream.getText(startToken, endToken);
  }

  public boolean hasIssues() {
    return !issues.isEmpty();
  }

  public List<TranspilationIssue> getIssues() {
    return Collections.unmodifiableList(issues);
  }
}
