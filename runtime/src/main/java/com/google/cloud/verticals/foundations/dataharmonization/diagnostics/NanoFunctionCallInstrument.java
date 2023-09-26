// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.diagnostics;

import static com.google.common.collect.ImmutableTable.toImmutableTable;
import static java.util.stream.Collectors.toCollection;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.base.VerifyException;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Table;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * A {@link FunctionCallInstrument} implementation that logs and times every function call.
 *
 * <p>It attempts to do so as efficiently as possible, by storing a long integer quadruplet per call
 * + 2 longs per original function.
 *
 * <p>The overhead of sampling the current nano time may be included in the timing of each function
 * call depending on the implementation of the given {@link TimeProvider}.
 */
public class NanoFunctionCallInstrument implements FunctionCallInstrument {

  // Given we are going to spam the data buffer with data it makes sense to at least preallocate
  // some significant size to avoid wasting all our time and memory on ArrayList.grow (source: trust
  // me). Buffer size chosen arbitrarily.
  private static final int BUFFER_SIZE = 10000;

  private long nextFnId = 1;

  // Indices of each component of the long[] sample quadruplet
  private static final int THREAD_INDEX = 0;
  private static final int TIME_INDEX = 1;
  private static final int FUNC_INDEX = 2;
  private static final int START_CALL_INDEX = 3;

  private final BiMap<Long, Signature> idToFn = HashBiMap.create();
  private final List<long[]> data = new ArrayList<>(BUFFER_SIZE);
  private final TimeProvider clock;

  public NanoFunctionCallInstrument(TimeProvider clock) {
    this.clock = clock;
  }

  public NanoFunctionCallInstrument() {
    clock = System::nanoTime;
  }

  public void clear() {
    data.clear();
    idToFn.clear();
    nextFnId = 1;
  }

  private static long threadID(long[] sample) {
    return sample[THREAD_INDEX];
  }

  private static Duration time(long[] sample) {
    return Duration.ofNanos(sample[TIME_INDEX]);
  }

  private static long funcId(long[] sample) {
    return sample[FUNC_INDEX];
  }

  private static boolean isStart(long[] sample) {
    return sample[START_CALL_INDEX] == 1;
  }

  private static long[] sample(long ts, long fnId, boolean startCall) {
    return new long[] {Thread.currentThread().getId(), ts, fnId, startCall ? 1 : 0};
  }

  @Override
  public synchronized void startCall(CallableFunction function) {
    stamp(function, /* startCall= */ true, clock.nanoTime());
  }

  @Override
  public synchronized void endCall(CallableFunction function) {
    stamp(function, /* startCall= */ false, clock.nanoTime());
  }

  private synchronized long idOf(CallableFunction function) {
    return idToFn
        .inverse()
        .computeIfAbsent(function.getSignature(), /* key */ unused -> nextFnId++);
  }

  private synchronized void stamp(CallableFunction function, boolean startCall, long ts) {
    long fnId = idOf(function);
    data.add(sample(ts, fnId, startCall));
  }

  @Override
  public synchronized Table<String, String, FunctionData> export() {
    Map<Long, Collection<long[]>> rowsByThreadId =
        data.stream()
            .collect(
                Collectors.groupingBy(
                    NanoFunctionCallInstrument::threadID, toCollection(ArrayList::new)));

    Map<Long, FunctionData> samples = new HashMap<>();

    // Treat each thread separately.
    for (Collection<long[]> thread : rowsByThreadId.values()) {
      Deque<long[]> callStack = new ArrayDeque<>();
      Deque<Duration> dependantTimes = new ArrayDeque<>();

      // Each sample is either the start or end of the call.
      for (long[] row : thread) {
        if (isStart(row)) {
          callStack.push(row);
          dependantTimes.push(Duration.ZERO);
          continue;
        }

        // Only process anything when the call ends.
        // The top of the callStack has the info of when the call started.
        // The top of dependantTimes stack has the sum of all the times of all the calls that were
        // above us in the stack.
        long[] popRow = callStack.pop();
        Duration depTime = dependantTimes.pop();

        if (funcId(popRow) != funcId(row)) {
          throw new VerifyException(
              String.format("Stack out of sync - expected %d got %d", funcId(row), funcId(popRow)));
        }

        Duration duration = time(row).minus(time(popRow));
        Duration selfTime = duration.minus(depTime);

        if (!dependantTimes.isEmpty()) {
          dependantTimes.push(dependantTimes.pop().plus(duration));
        }

        // We want caller -> callee pairs to help narrow down performance issues.
        long parent = callStack.isEmpty() ? -1 : funcId(callStack.peek());
        long stackPairId = encode(parent, funcId(row));

        if (!samples.containsKey(stackPairId)) {
          samples.put(stackPairId, new FunctionData(selfTime, duration));
        } else {
          samples.get(stackPairId).add(selfTime, duration);
        }
      }
    }

    // Convert the ids into function names.
    return samples.entrySet().stream()
        .map(e -> new AbstractMap.SimpleEntry<>(decode(e.getKey()), e.getValue()))
        .collect(toImmutableTable(e -> e.getKey()[0], e -> e.getKey()[1], Entry::getValue));
  }

  private String[] decode(long id) {
    long callerId = id >> (Long.SIZE / 2);
    long calleeId = id & 0x00000000ffffffffL;

    return new String[] {callerId > 0 ? ref(idToFn.get(callerId)) : "", ref(idToFn.get(calleeId))};
  }

  private static long encode(long callerId, long calleeId) {
    if (callerId >= 0) {
      return callerId << (Long.SIZE / 2) | calleeId;
    }

    return calleeId;
  }

  private static String ref(Signature fn) {
    return String.format("%s::%s @%X", fn.getPackageName(), fn.getName(), fn.getArgs().hashCode());
  }

  /**
   * Interface abstraction for a clock that provides (as precise as possible) time in nanoseconds.
   */
  @FunctionalInterface
  public interface TimeProvider {
    @SuppressWarnings("GoodTime") // TODO(rpolyano): Performance critical code.
    long nanoTime();
  }
}
