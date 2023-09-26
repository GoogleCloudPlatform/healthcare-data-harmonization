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
package com.google.cloud.verticals.foundations.dataharmonization.function.whistle;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import java.io.Serializable;

/** Allows using any function as a target. */
public class FunctionTarget implements Target, Serializable {
  private final Closure fn;

  private FunctionTarget(Closure fn) {
    this.fn = fn;
  }

  @Override
  public void write(RuntimeContext ctx, Data value) {
    // Discard the return value.
    fn.bindNextFreeParameter(value).execute(ctx);
  }

  public static FunctionTarget construct(RuntimeContext ctx, FunctionCall call) {
    FunctionCall withFree =
        FunctionCall.newBuilder(call)
            .addArgs(ValueSource.newBuilder().setFreeParameter("$write"))
            .build();
    Closure closure = DefaultClosure.create(ctx, withFree);
    return new FunctionTarget(closure);
  }
}
