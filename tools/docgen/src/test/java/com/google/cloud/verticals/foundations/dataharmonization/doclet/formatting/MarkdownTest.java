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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for markdown wrapper. */
@RunWith(JUnit4.class)
public class MarkdownTest {

  @Test
  public void startInlineCode() {
    assertThat(new Markdown().startInlineCode()).isEqualTo("`");
  }

  @Test
  public void endInlineCode() {
    Markdown md = new Markdown();
    assertThat(md.endInlineCode()).isEmpty();

    md.startInlineCode();
    assertThat(md.endInlineCode()).isEqualTo("`");
  }

  @Test
  public void startCodeBlock() {
    assertThat(new Markdown().startCodeBlock()).isEqualTo("\n```\n");
  }

  @Test
  public void endCodeBlock() {
    Markdown md = new Markdown();
    assertThat(md.endCodeBlock()).isEmpty();

    md.startCodeBlock();
    assertThat(md.endCodeBlock()).isEqualTo("```\n");
  }

  @Test
  public void paragraphBreak() {
    assertThat(new Markdown().paragraphBreak()).isEqualTo("\n");
  }

  @Test
  public void startBold() {
    assertThat(new Markdown().startBold()).isEqualTo("**");
  }

  @Test
  public void endBold() {
    assertThat(new Markdown().endBold()).isEqualTo("**");
  }

  @Test
  public void startHeadings() {
    Markdown md = new Markdown();

    assertThat(md.startHeading(1)).isEqualTo("# ");
    assertThat(md.startHeading(2)).isEqualTo("## ");
    assertThat(md.startHeading(5)).isEqualTo("##### ");
  }

  @Test
  public void endHeadings() {
    Markdown md = new Markdown();

    assertThat(md.endHeading(1)).isEqualTo("\n");
    assertThat(md.endHeading(2)).isEqualTo("\n");
    assertThat(md.endHeading(5)).isEqualTo("\n");
  }

  @Test
  public void autoToc() {
    assertThat(new Markdown().autoTOC()).isEqualTo("[TOC]\n\n");
  }

  @Test
  public void fileExit() {
    assertThat(new Markdown().fileExt()).isEqualTo("md");
  }

  @Test
  public void numberedList() {
    Markdown md = new Markdown();
    String got =
        md.startNumList()
            + md.startListItem()
            + md.text("One:")
            + md.startCodeBlock()
            + md.text("Code 1\nCode 1.1")
            + md.endCodeBlock()
            + md.endListItem()
            + md.startListItem()
            + md.text("Two:")
            + md.startCodeBlock()
            + md.text("Code 2")
            + md.endCodeBlock()
            + md.endListItem()
            + md.endNumList();

    assertThat(got.trim())
        .isEqualTo(
            "1. One:\n\n"
                + "    ```\n"
                + "    Code 1\n"
                + "    Code 1.1\n"
                + "    ```\n"
                + "1. Two:\n\n"
                + "    ```\n"
                + "    Code 2\n"
                + "    ```");
  }

  @Test
  public void bulletList() {
    Markdown md = new Markdown();
    String got =
        md.startList()
            + md.startListItem()
            + md.text("One:")
            + md.startCodeBlock()
            + md.text("Code 1")
            + md.endCodeBlock()
            + md.endListItem()
            + md.startListItem()
            + md.text("Two:")
            + md.startCodeBlock()
            + md.text("Code 2")
            + md.endCodeBlock()
            + md.endListItem()
            + md.endList();

    assertThat(got.trim())
        .isEqualTo(
            "* One:\n\n"
                + "    ```\n"
                + "    Code 1\n"
                + "    ```\n"
                + "* Two:\n\n"
                + "    ```\n"
                + "    Code 2\n"
                + "    ```");
  }

  @Test
  public void nestedList() {
    Markdown md = new Markdown();
    String got =
        md.startList()
            + md.startListItem()
            + md.text("One:")
            + (md.startNumList()
                + md.startListItem()
                + md.text("Nested 1")
                + md.endListItem()
                + md.startListItem()
                + md.text("Nested 2")
                + md.endListItem()
                + md.endNumList())
            + md.endListItem()
            + md.startListItem()
            + md.text("Two:")
            + (md.startNumList()
                + md.startListItem()
                + md.text("Nested 3")
                + md.startCodeBlock()
                + md.text("Code 1\nCode 1.1")
                + md.endCodeBlock()
                + md.endListItem()
                + md.startListItem()
                + md.text("Nested 4")
                + md.endListItem()
                + md.endNumList())
            + md.endListItem()
            + md.endList();

    assertThat(got.trim())
        .isEqualTo(
            "* One:\n\n"
                + "    1. Nested 1\n"
                + "    1. Nested 2\n"
                + "\n* Two:\n\n"
                + "    1. Nested 3\n\n"
                + "        ```\n"
                + "        Code 1\n"
                + "        Code 1.1\n"
                + "        ```\n"
                + "    1. Nested 4");
  }

  @Test
  public void nestedList_unclosedlistItems() {
    Markdown md = new Markdown();
    String got =
        md.startList()
            + md.startListItem()
            + md.text("One:")
            + (md.startNumList()
                + md.startListItem()
                + md.text("Nested 1")
                + md.startListItem()
                + md.text("Nested 2")
                + md.endNumList())
            + md.startListItem()
            + md.text("Two:")
            + (md.startNumList()
                + md.startListItem()
                + md.text("Nested 3")
                + md.startCodeBlock()
                + md.text("Code 1\nCode 1.1")
                + md.endCodeBlock()
                + md.startListItem()
                + md.text("Nested 4")
                + md.endNumList())
            + md.endList();

    assertThat(got.trim())
        .isEqualTo(
            "* One:\n\n"
                + "    1. Nested 1\n"
                + "    1. Nested 2\n\n"
                + "* Two:\n\n"
                + "    1. Nested 3\n\n"
                + "        ```\n"
                + "        Code 1\n"
                + "        Code 1.1\n"
                + "        ```\n"
                + "    1. Nested 4");
  }

  @Test
  public void text_preservesTrailingWhitespace() {
    Markdown md = new Markdown();

    String naughtyText = "hello\nworld \nthere's        quite   some wonky whitespace here   ";

    assertThat(md.text(naughtyText))
        .isEqualTo("hello world there's quite some wonky whitespace here ");
  }
}
