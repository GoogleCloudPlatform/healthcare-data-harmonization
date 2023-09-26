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

package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;

/** A simple class for capturing invocations and verifying them after the fact. */
final class InvocationCapture {
  private final Data ret;

  RuntimeContext ctx;
  Object[] args;
  boolean invoked;

  InvocationCapture(Data ret) {
    this.ret = ret;
  }

  Data capture(RuntimeContext context, Object... args) {
    this.ctx = context;
    this.args = args;
    invoked = true;

    return ret;
  }
}
