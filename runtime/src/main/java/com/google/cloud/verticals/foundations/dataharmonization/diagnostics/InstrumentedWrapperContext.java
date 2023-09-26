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
package com.google.cloud.verticals.foundations.dataharmonization.diagnostics;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import java.util.function.BiFunction;

/** RuntimeContext wrapper with hooks for instrumentation. */
public class InstrumentedWrapperContext extends WrapperContext<InstrumentedWrapperContext> {

  private final Instrumentation instrumentation;

  public InstrumentedWrapperContext(RuntimeContext innerContext, Instrumentation instrumentation) {
    super(innerContext, InstrumentedWrapperContext.class);
    this.instrumentation = instrumentation;
  }

  @Override
  protected InstrumentedWrapperContext rewrap(RuntimeContext innerContext) {
    return new InstrumentedWrapperContext(innerContext, instrumentation);
  }

  @Override
  public Data wrap(
      CallableFunction function, Data[] args, BiFunction<RuntimeContext, Data[], Data> delegate) {
    // Wrap the delegate instead of the super call to bring the start/end calls as close to the
    // function's actual code as possible.
    return super.wrap(
        function,
        args,
        (xctx, xargs) -> {
          try {
            instrumentation.functionCalls(function).startCall(function);
            return delegate.apply(xctx, xargs);
          } finally {
            instrumentation.functionCalls(function).endCall(function);
          }
        });
  }
}
