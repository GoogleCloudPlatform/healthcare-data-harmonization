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

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;

/** Simple storage class for issues occurring in a transpiled file. */
public class TranspilationIssue {
  private final FileInfo file;
  // Line number of issue (1 based), inclusive.
  private final int line;

  // Column number of issue (1 based), inclusive.
  private final int col;

  // End line of issue (1 based), inclusive (unlike endCol, which is exclusive).
  private final int endLine;

  // End column number of issue (1 based), exclusive.
  private final int endCol;

  private final String message;

  public TranspilationIssue(FileInfo file, int line, int col, String message) {
    this(file, line, col, line, col + 1, message);
  }

  public TranspilationIssue(
      FileInfo file, int line, int col, int endLine, int endCol, String message) {
    this.message = message;
    this.file = file;
    this.line = line;
    this.col = col;
    this.endLine = endLine;
    this.endCol = endCol;
  }

  public String getMessage() {
    return message;
  }

  public FileInfo getFile() {
    return file;
  }

  public int getLine() {
    return line;
  }

  public int getCol() {
    return col;
  }

  public int getEndLine() {
    return endLine;
  }

  public int getEndCol() {
    return endCol;
  }
}
