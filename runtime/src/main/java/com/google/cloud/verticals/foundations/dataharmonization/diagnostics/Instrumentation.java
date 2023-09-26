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

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.diagnostics.FunctionCallInstrument.FunctionData;
import com.google.cloud.verticals.foundations.dataharmonization.diagnostics.export.Writer;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.common.collect.Table;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Predicate;

/** A data class for storing instances of various instruments (an instrumentation suite). */
public final class Instrumentation {
  private static final FunctionCallInstrument NOOP_FUNCTION_CALL_INSTRUMENT =
      FunctionCallInstrument.noop();
  private final FunctionCallInstrument functionCallInstrument;
  private final Predicate<CallableFunction> functionFilter;

  private Instrumentation(
      FunctionCallInstrument functionCallInstrument, Predicate<CallableFunction> functionFilter) {
    this.functionCallInstrument = functionCallInstrument;
    this.functionFilter = functionFilter;
  }

  /** Create a new builder to create an instrumentation suite. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns an instrument for timing function calls. */
  public FunctionCallInstrument functionCalls(CallableFunction function) {
    if (functionFilter.test(function)) {
      return functionCallInstrument;
    }
    return NOOP_FUNCTION_CALL_INSTRUMENT;
  }

  /**
   * Writes all the data from {@link FunctionCallInstrument#export} with the given writer to the
   * given output stream.
   */
  public void writeFunctions(
      Writer<Table<String, String, FunctionData>> writer, OutputStream stream) throws IOException {
    writer.write(stream, functionCallInstrument.export());
  }

  /**
   * Returns true iff the given function is known to be either a native function or user-defined.
   * That is - ignores blocks, lambdas, etc.
   */
  public static boolean isNativeOrUserDefined(CallableFunction function) {
    return function.getDebugInfo() != null
        && (function.getDebugInfo().getFunctionInfo().getType() == FunctionType.NATIVE
            || function.getDebugInfo().getFunctionInfo().getType() == FunctionType.DECLARED);
  }

  /** Builder for an Instrumentation suite. */
  public static class Builder {
    private FunctionCallInstrument functionCallInstrument = FunctionCallInstrument.noop();
    private Predicate<CallableFunction> functionFilter = unused -> true;

    /** Set the function call instrument. */
    @CanIgnoreReturnValue
    public Builder withFunctionCall(FunctionCallInstrument fci) {
      this.functionCallInstrument = fci;
      return this;
    }

    /** Set the function call filter. */
    @CanIgnoreReturnValue
    public Builder withFunctionCallFilter(Predicate<CallableFunction> filter) {
      this.functionFilter = filter;
      return this;
    }

    /** Instantiate the instrumentation suite. */
    public Instrumentation build() {
      return new Instrumentation(functionCallInstrument, functionFilter);
    }
  }
}
