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

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.time.Duration;

/**
 * An instrument to time function calls.
 *
 * <p>Implementers of this interface should take extreme care to maximize performance and minimize
 * overhead as the methods within may be called for every function executed.
 */
public interface FunctionCallInstrument {

  /** Logs the start of a function call for the given function. */
  void startCall(CallableFunction function);

  /** Logs the end of a function call for the given function. */
  void endCall(CallableFunction function);

  /** Exports the data stored in this instrument. The table maps caller -> callee -> timings. */
  Table<String, String, FunctionData> export();

  /** Returns an instrument that does nothing, and costs nothing to call. */
  static FunctionCallInstrument noop() {
    return new Noop();
  }

  /** FunctionCallInstrument that does nothing, and costs nothing to call. */
  final class Noop implements FunctionCallInstrument {

    @Override
    public void startCall(CallableFunction function) {}

    @Override
    public void endCall(CallableFunction function) {}

    @Override
    public ImmutableTable<String, String, FunctionData> export() {
      return ImmutableTable.of();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Noop;
    }

    @Override
    public int hashCode() {
      return Noop.class.hashCode();
    }
  }

  /** Data class for storing function timing information. */
  class FunctionData {
    public Duration totalSelf;
    public Duration total;
    public long numCalls;

    public FunctionData(Duration totalSelf, Duration total, long numCalls) {
      this.totalSelf = totalSelf;
      this.total = total;
      this.numCalls = numCalls;
    }

    public FunctionData(Duration totalSelf, Duration total) {
      this.totalSelf = totalSelf;
      this.total = total;
      this.numCalls = 1;
    }

    /** Add a sample to this data, incrementing number of calls and adding to the total timings. */
    public void add(Duration selfTime, Duration duration) {
      this.numCalls++;
      this.totalSelf = this.totalSelf.plus(selfTime);
      this.total = this.total.plus(duration);
    }
  }
}
