// Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting;

import com.google.common.collect.Streams;
import com.google.common.flogger.FluentLogger;
import java.util.Stack;
import java.util.stream.Collectors;

/** Implementation of MarkupFormat for Markdown (i.e. .md files). */
public class Markdown implements MarkupFormat {
  private final Stack<Boolean> listNumbered = new Stack<>();
  private final Stack<Boolean> lastListItemClosed = new Stack<>();
  private int indent = 0;
  private boolean inCodeBlock;
  private boolean inCodeLine;
  private boolean lastCharIsNewLine = true;
  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  @Override
  public String startList() {
    log("startList");
    listNumbered.push(false);
    lastListItemClosed.push(true);
    return paragraphBreak();
  }

  @Override
  public String endList() {
    log("endList");
    listNumbered.pop();
    if (!lastListItemClosed.pop()) {
      indent--;
    }
    return paragraphBreak();
  }

  @Override
  public String startNumList() {
    log("startNumList");
    listNumbered.push(true);
    lastListItemClosed.push(true);
    return paragraphBreak();
  }

  @Override
  public String endNumList() {
    log("endNumList");
    listNumbered.pop();
    if (!lastListItemClosed.pop()) {
      indent--;
    }
    return paragraphBreak();
  }

  @Override
  public String startListItem() {
    log("startListItem");
    if (!lastListItemClosed.pop()) {
      indent--;
    }
    String ret = ensureOnNewLine(listNumbered.peek() ? "1. " : "* ");
    indent++;
    lastListItemClosed.push(false);
    return ret;
  }

  @Override
  public String endListItem() {
    log("endListItem");
    indent--;
    lastListItemClosed.pop();
    lastListItemClosed.push(true);
    return "";
  }

  @Override
  public String startInlineCode() {
    log("startInlineCode");
    if (inCodeBlock || inCodeLine) {
      return "";
    }
    lastCharIsNewLine = false;
    inCodeLine = true;
    return "`";
  }

  @Override
  public String endInlineCode() {
    log("endInlineCode");
    if (!inCodeLine) {
      return "";
    }
    inCodeLine = false;
    lastCharIsNewLine = false;
    return "`";
  }

  @Override
  public String startCodeBlock() {
    log("startCodeBlock");
    if (inCodeBlock || inCodeLine) {
      return "";
    }
    inCodeBlock = true;
    return ensureOnNewLine("\n" + currentIndent() + "```\n");
  }

  private String ensureOnNewLine(String part) {
    if (part.isEmpty()) {
      if (lastCharIsNewLine) {
        return "";
      }
      lastCharIsNewLine = true;
      return "\n";
    }
    String prefix = lastCharIsNewLine ? "" : "\n";
    String indent = part.startsWith("\n") ? "" : currentIndent();
    String ret = prefix + indent + part;
    lastCharIsNewLine = ret.endsWith("\n");
    return ret;
  }

  @Override
  public String endCodeBlock() {
    log("endCodeBlock");
    if (!inCodeBlock) {
      return "";
    }
    inCodeBlock = false;
    return ensureOnNewLine("```\n");
  }

  @Override
  public String currentIndent() {
    return " ".repeat(indent * 4);
  }

  @Override
  public String paragraphBreak() {
    log("paragraphBreak");
    return ensureOnNewLine("\n");
  }

  @Override
  public String startBold() {
    log("startBold");
    lastCharIsNewLine = false;
    return "**";
  }

  @Override
  public String endBold() {
    log("endBold");
    lastCharIsNewLine = false;
    return "**";
  }

  @Override
  public String startHeading(int level) {
    log("startHeading");
    lastCharIsNewLine = false;
    return "#".repeat(level) + " ";
  }

  @Override
  public String endHeading(int level) {
    log("endHeading");
    lastCharIsNewLine = true;
    return "\n";
  }

  @Override
  public String autoTOC() {
    log("autoTOC");
    lastCharIsNewLine = false;
    return "[TOC]" + paragraphBreak();
  }

  @Override
  public String fileExt() {
    return "md";
  }

  @Override
  public String text(String text) {
    if (inCodeBlock || inCodeLine) {
      text = text.stripLeading();
      return markup(text);
    }
    log("text: " + text + "<end text>");
    if (text.length() == 0) {
      return "";
    }

    // Preserve the last character - if it is a newline or an empty space we want to keep it (since
    // the next text may rely on this).
    char lastChar = text.charAt(text.length() - 1);
    text = text.replaceAll("\\s+", " ").stripTrailing();
    if (lastCharIsNewLine) {
      text = currentIndent() + text;
    }
    lastCharIsNewLine = lastChar == '\n';
    return text + (Character.isWhitespace(lastChar) ? Character.toString(lastChar) : "");
  }

  @Override
  public String markup(String markup) {
    log("markup: " + markup + "<end markup>");

    if (markup.lines().count() > 1) {
      markup =
          Streams.concat(
                  markup.lines().limit(1).map(l -> lastCharIsNewLine ? (currentIndent() + l) : l),
                  markup.lines().skip(1).map(l -> currentIndent() + l))
              .collect(Collectors.joining("\n"));
    } else {
      markup = lastCharIsNewLine ? (currentIndent() + markup) : markup;
    }

    lastCharIsNewLine = markup.endsWith("\n");
    return markup;
  }

  private void log(String text) {
    if (!logger.atFine().isEnabled()) {
      return;
    }

    String from;
    try {
      throw new RuntimeException();
    } catch (RuntimeException ex) {
      from = ex.getStackTrace()[3].getMethodName();
    }

    logger.atFine().log("%s\n  %s\n", from, text);
  }
}
