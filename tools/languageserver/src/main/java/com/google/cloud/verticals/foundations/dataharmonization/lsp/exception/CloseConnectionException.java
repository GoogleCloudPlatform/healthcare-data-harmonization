/*
 * Copyright 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.lsp.exception;

/**
 * Unchecked Exception class which is used to signal a connection closing. Wraps the associated exit
 * status code at the time of the closed connection.
 *
 * <p>See go/bugpattern/SystemExitOutsideMain for more information about this pattern.
 */
public class CloseConnectionException extends RuntimeException {
  private final int statusCode;

  public CloseConnectionException(int stausCode) {
    statusCode = stausCode;
  }

  public int getStatusCode() {
    return statusCode;
  }
}
