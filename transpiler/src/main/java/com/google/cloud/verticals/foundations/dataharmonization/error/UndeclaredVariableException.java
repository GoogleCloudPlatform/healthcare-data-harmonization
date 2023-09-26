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
package com.google.cloud.verticals.foundations.dataharmonization.error;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.transpiler.WhistleParser.InputSourceContext;

/**
 * {@link RuntimeException} thrown when transpiler encounters a variable that has not been declared.
 */
public class UndeclaredVariableException extends RuntimeException {
  // Used to retrieve details about the line which caused the exception.
  private final InputSourceContext inputCtx;
  private final FileInfo fileInfo;

  public UndeclaredVariableException(String msg, InputSourceContext inputCtx, FileInfo fileInfo) {
    super(msg);
    this.inputCtx = inputCtx;
    this.fileInfo = fileInfo;
  }

 public TranspilationIssue getTranspilationIssue() {

      return new TranspilationIssue(
          fileInfo,
          inputCtx.getStart().getLine(),
          inputCtx.getStart().getCharPositionInLine() + 1,
          inputCtx.getStop().getLine(),
          inputCtx.getStart().getCharPositionInLine() + inputCtx.getText().length() + 1,
          this.getMessage());
    }
}
