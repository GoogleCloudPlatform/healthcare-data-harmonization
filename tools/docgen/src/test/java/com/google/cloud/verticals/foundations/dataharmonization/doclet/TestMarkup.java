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
package com.google.cloud.verticals.foundations.dataharmonization.doclet;

import com.google.cloud.verticals.foundations.dataharmonization.doclet.formatting.MarkupFormat;

/** Test markup that just reports what was called. */
public class TestMarkup implements MarkupFormat {

  @Override
  public String startList() {
    return "<list>";
  }

  @Override
  public String endList() {
    return "</list>";
  }

  @Override
  public String startNumList() {
    return "<numlist>";
  }

  @Override
  public String endNumList() {
    return "</numlist>";
  }

  @Override
  public String startListItem() {
    return "<item>";
  }

  @Override
  public String endListItem() {
    return "</item>";
  }

  @Override
  public String startInlineCode() {
    return "<inline>";
  }

  @Override
  public String endInlineCode() {
    return "</inline>";
  }

  @Override
  public String startCodeBlock() {
    return "<block>";
  }

  @Override
  public String endCodeBlock() {
    return "</block>";
  }

  @Override
  public String currentIndent() {
    return "<indent>";
  }

  @Override
  public String paragraphBreak() {
    return "<break/>";
  }

  @Override
  public String startBold() {
    return "<b>";
  }

  @Override
  public String endBold() {
    return "</b>";
  }

  @Override
  public String startHeading(int level) {
    return String.format("<h%d>", level);
  }

  @Override
  public String endHeading(int level) {
    return String.format("</h%d>", level);
  }

  @Override
  public String autoTOC() {
    return "<toc/>";
  }

  @Override
  public String fileExt() {
    return "test";
  }

  @Override
  public String text(String text) {
    return "<text>" + text + "</text>";
  }

  @Override
  public String markup(String text) {
    return "<markup>" + text + "</markup>";
  }
}
