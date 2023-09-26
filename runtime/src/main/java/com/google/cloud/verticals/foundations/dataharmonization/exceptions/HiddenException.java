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

package com.google.cloud.verticals.foundations.dataharmonization.exceptions;

/**
 * Exception that only contains the stack and class of some original (i.e. no message). Intended to
 * avoid sensitive data leaking through exceptions.
 */
public class HiddenException extends Exception {
  public HiddenException(Throwable original) {
    super(
        String.format("Hidden %s", original.getClass().getSimpleName()),
        wrapCause(original.getCause()));
    setStackTrace(original.getStackTrace());
  }

  private static Throwable wrapCause(Throwable cause) {
    if (cause == null) {
      return null;
    }
    if (cause instanceof HiddenException) {
      return cause;
    }

    return new HiddenException(cause);
  }
}
