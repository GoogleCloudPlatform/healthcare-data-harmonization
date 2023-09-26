/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import java.io.Serializable;
import java.util.function.Consumer;

/** Manages a flag that allows early cancellation of a process. */
public interface CancellationToken extends Serializable {

  /** Returns whether this token has been cancelled (i.e. cancellation has been requested). */
  boolean isCancelled();

  /**
   * Requests/marks this token as cancelled, iff it was not already cancelled. Notifies all
   * consumers (immediately) iff it was not already cancelled. If the token was already cancelled or
   * cancellation is not supported for some other reason, this returns false and does nothing.
   */
  boolean cancel(String reason);

  /** Registers a function to call when cancellation is requested. See {@link #cancel(String)}. */
  void registerCancelCallback(Consumer<CancellationToken> callback);

  /**
   * Returns the reason given for cancellation. This shall always be null if {@link #isCancelled()}
   * is false.
   */
  String getReason();

  /** Exception thrown when cancellation is requested. */
  class CancelledException extends RuntimeException {
    private final CancellationToken token;

    public CancelledException(CancellationToken token) {
      super(String.format("Operation was cancelled: %s", token.getReason()));
      this.token = token;
    }

    /** Returns the original token that caused this exception. */
    public CancellationToken getToken() {
      return token;
    }
  }
}
