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

package com.google.cloud.verticals.foundations.dataharmonization.exceptions;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;

/**
 * Exception thrown when there are import errors which could occur for reasons like ioexception,
 * invalid imports or external factors related to whistle or jupyter env.
 */
public class ImportException extends RuntimeException {
  private Source source;
  private String currentPath;

  public ImportException(ImportPath importPath, Exception e) {
    super(String.format("Error processing import %s\n%s", importPath, e.getMessage()), e);
  }

  public ImportException(ImportPath importPath, Exception e, Source source, String currentPath) {
    super(String.format("Error processing import %s\n%s", importPath, e.getMessage()), e);
    this.source = source;
    this.currentPath = currentPath;
  }

  public ImportException(String message, Source source, String currentPath) {
    super(message);
    this.source = source;
    this.currentPath = currentPath;
  }

  public Source getSource() {
    return source;
  }

  public String getCurrentPath() {
    return currentPath;
  }
}
