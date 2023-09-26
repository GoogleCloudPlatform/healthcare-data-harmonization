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
package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.CancellationToken;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Simple implementation of a thread-safe cancellation token. */
public class DefaultCancellationToken implements CancellationToken {
  private boolean cancelled;
  private String reason;

  private final Set<Consumer<CancellationToken>> callbacks = new HashSet<>();

  @Override
  public boolean isCancelled() {
    synchronized (callbacks) {
      return cancelled;
    }
  }

  @Override
  public boolean cancel(String reason) {
    ImmutableSet<Consumer<CancellationToken>> cbsToCall;
    synchronized (callbacks) {
      if (cancelled) {
        return false;
      }

      cancelled = true;
      this.reason = reason;
      cbsToCall = ImmutableSet.copyOf(callbacks);
    }
    cbsToCall.forEach(c -> c.accept(this));
    return true;
  }

  @Override
  public void registerCancelCallback(Consumer<CancellationToken> callback) {
    boolean callNow;
    synchronized (callbacks) {
      callbacks.add(callback);
      callNow = cancelled;
    }
    if (callNow) {
      callback.accept(this);
    }
  }

  @Override
  public String getReason() {
    synchronized (callbacks) {
      return reason;
    }
  }
}
