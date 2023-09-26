/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.registry;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;

/**
 * Fake skeleton implementation of {@link CallableFunction} that only supplies function name and
 * signature.
 */
public class TestCallableFunction extends CallableFunction {

  private final Signature signature;

  TestCallableFunction(Signature signature) {
    this.signature = signature;
  }

  @Override
  protected Data callInternal(RuntimeContext context, Data... args) {
    throw new UnsupportedOperationException("Function not supported.");
  }

  @Override
  public Signature getSignature() {
    return signature;
  }

  @Override
  public DebugInfo getDebugInfo() {
    throw new UnsupportedOperationException("Function not supported.");
  }
}
