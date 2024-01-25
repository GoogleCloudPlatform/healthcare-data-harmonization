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

/**
 * Abstraction for tags/markup elements that may differ from format to format (e.x. HTML vs
 * Markdown).
 */
public interface MarkupFormat {

  String startList();

  String endList();

  String startNumList();

  String endNumList();

  String startListItem();

  String endListItem();

  String startInlineCode();

  String endInlineCode();

  String startCodeBlock();

  String endCodeBlock();

  String currentIndent();

  String paragraphBreak();

  String startBold();

  String endBold();

  String startHeading(int level);

  String endHeading(int level);

  String autoTOC();

  String fileExt();

  /**
   * Maps the given javadoc (open) tag to the equivalent in the given formatter.
   *
   * @param name the tag name, e.g. "ol" for an ordered list or "p" for a paragraph.
   */
  static String mapStartTag(String name, MarkupFormat formatter) {
    switch (name) {
      case "ol":
        return formatter.startNumList();
      case "ul":
        return formatter.startList();
      case "pre":
        return formatter.startCodeBlock();
      case "code":
        return formatter.startInlineCode();
      case "p":
        return formatter.paragraphBreak();
      case "li":
        return formatter.startListItem();
      case "b":
        return formatter.startBold();
    }

    return String.format("<%s>", name);
  }

  /**
   * Maps the given javadoc (closing) tag to the equivalent in the given formatter.
   *
   * @param name the tag name, e.g. "ol" for an ordered list or "p" for a paragraph.
   */
  static String mapEndTag(String name, MarkupFormat formatter) {
    switch (name) {
      case "ol":
        return formatter.endNumList();
      case "ul":
        return formatter.endList();
      case "pre":
        return formatter.endCodeBlock();
      case "code":
        return formatter.endInlineCode();
      case "li":
        return formatter.endListItem();
      case "b":
        return formatter.endBold();
    }

    return String.format("</%s>", name);
  }

  String text(String text);

  String markup(String text);
}
