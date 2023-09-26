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

import static com.google.common.truth.Truth.assertThat;
import static java.time.Duration.ofMillis;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.diagnostics.FunctionCallInstrument.FunctionData;
import com.google.cloud.verticals.foundations.dataharmonization.diagnostics.NanoFunctionCallInstrument.TimeProvider;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.concurrent.Semaphore;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for NanoFunctionCallInstrument. Since the start/end calls are thin, these tests focus
 * primarily on exporting various sequences of calls.
 */
@RunWith(JUnit4.class)
public class NanoFunctionCallInstrumentTest {

  private TickerClock clock;

  @Before
  public void setup() {
    clock = new TickerClock();
  }

  @Test
  public void export_empty() {
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument();

    Table<String, String, FunctionData> got = instrument.export();
    assertThat(got).isEmpty();
  }

  @Test
  public void clear_returnZeroRows(){
    CallableFunction fn = func("a");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    instrument.startCall(fn);
    instrument.endCall(fn);
    instrument.clear();
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(0);
  }

  @Test
  public void singleCall_returnsOneRow() {
    CallableFunction fn = func("a");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    instrument.startCall(fn);
    instrument.endCall(fn);

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(1);
    assertThat(got).contains("", "a::a");

    // Check records
    FunctionData gotFD = got.get("", "a::a");
    assertThat(gotFD.numCalls).isEqualTo(1);
    assertThat(gotFD.total).isEqualTo(ofMillis(1));
    assertThat(gotFD.totalSelf).isEqualTo(ofMillis(1));
  }

  @Test
  public void sequentialCalls_returnsMultipleRows() {
    CallableFunction fna = func("a");
    CallableFunction fnb = func("b");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    instrument.startCall(fna);
    instrument.endCall(fna);
    instrument.startCall(fnb);
    instrument.endCall(fnb);

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(2);
    assertThat(got).contains("", "a::a");
    assertThat(got).contains("", "b::b");

    // Check records
    FunctionData gotFDA = got.get("", "a::a");
    assertThat(gotFDA.numCalls).isEqualTo(1);
    assertThat(gotFDA.total).isEqualTo(ofMillis(1));
    assertThat(gotFDA.totalSelf).isEqualTo(ofMillis(1));

    FunctionData gotFDB = got.get("", "b::b");
    assertThat(gotFDB.numCalls).isEqualTo(1);
    assertThat(gotFDB.total).isEqualTo(ofMillis(1));
    assertThat(gotFDB.totalSelf).isEqualTo(ofMillis(1));
  }

  @Test
  public void sequentialRepeatedCalls_returnsMultipleRows() {
    CallableFunction fna = func("a");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    instrument.startCall(fna); // t = 0
    instrument.endCall(fna); // t = 1, total = 1
    instrument.startCall(fna); // t = 2
    instrument.endCall(fna); // t = 3, total = 2

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(1);
    assertThat(got).contains("", "a::a");

    // Check records
    FunctionData gotFD = got.get("", "a::a");
    assertThat(gotFD.numCalls).isEqualTo(2);
    assertThat(gotFD.total).isEqualTo(ofMillis(2));
    assertThat(gotFD.totalSelf).isEqualTo(ofMillis(2));
  }

  @Test
  public void nestedCalls_calculatesTimeCorrectly() {
    CallableFunction fna = func("a");
    CallableFunction fnb = func("b");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    instrument.startCall(fna); // t = 0
    instrument.startCall(fnb); // t = 1
    instrument.endCall(fnb); // t = 2, totalb = 1
    instrument.endCall(fna); // t = 3, totala = 3, selfa = totala - totalb = 2

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(2);
    assertThat(got).contains("", "a::a");
    assertThat(got).contains("a::a", "b::b");

    // Check records
    FunctionData gotFDA = got.get("", "a::a");
    assertThat(gotFDA.numCalls).isEqualTo(1);
    assertThat(gotFDA.total).isEqualTo(ofMillis(3));
    assertThat(gotFDA.totalSelf).isEqualTo(ofMillis(2));

    FunctionData gotFDB = got.get("a::a", "b::b");
    assertThat(gotFDB.numCalls).isEqualTo(1);
    assertThat(gotFDB.total).isEqualTo(ofMillis(1));
    assertThat(gotFDB.totalSelf).isEqualTo(ofMillis(1));
  }

  @Test
  public void multipleThreads_keepsThemSeparate() {
    CallableFunction fna = func("a");
    CallableFunction fnb = func("b");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    Semaphore synca = new Semaphore(0);
    Semaphore syncb = new Semaphore(0);
    Semaphore done = new Semaphore(0);

    Thread ta =
        new Thread(
            () -> {
              instrument.startCall(fna);
              syncb.release();
              synca.acquireUninterruptibly();
              instrument.endCall(fna);
              syncb.release();
              done.release();
            });
    Thread tb =
        new Thread(
            () -> {
              syncb.acquireUninterruptibly();
              instrument.startCall(fnb);
              synca.release();
              syncb.acquireUninterruptibly();
              instrument.endCall(fnb);
              done.release();
            });

    // The order we want is
    // start fna, start fnb, end fna, end fnb
    ta.start();
    tb.start();

    done.acquireUninterruptibly(2);

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(2);
    assertThat(got).contains("", "a::a");
    assertThat(got).contains("", "b::b");

    // Check records
    FunctionData gotFDA = got.get("", "a::a");
    assertThat(gotFDA.numCalls).isEqualTo(1);
    assertThat(gotFDA.total).isEqualTo(ofMillis(2));
    assertThat(gotFDA.total).isEqualTo(ofMillis(2));

    FunctionData gotFDB = got.get("", "b::b");
    assertThat(gotFDB.numCalls).isEqualTo(1);
    assertThat(gotFDB.total).isEqualTo(ofMillis(2));
    assertThat(gotFDB.totalSelf).isEqualTo(ofMillis(2));
  }

  @Test
  public void recursiveCalls_calculatesTimeCorrectly() {
    CallableFunction fna = func("a");
    NanoFunctionCallInstrument instrument = new NanoFunctionCallInstrument(clock);

    // Call the funcs
    instrument.startCall(fna); // t = 0
    instrument.startCall(fna); // t = 1
    instrument.startCall(fna); // t = 2
    instrument.endCall(fna); // t = 3, total1 = 1, self1 = 1
    instrument.endCall(fna); // t = 4, total2 = 4 - 1 = 3, self2 = total2 - total1 = 2
    instrument.endCall(fna); // t = 5, total3 = 5 - 0 = 5, self3 = total3 - total2 = 2

    // Check data size
    ImmutableTable<String, String, FunctionData> got = stripHashes(instrument.export());
    assertThat(got).hasSize(2);
    assertThat(got).contains("", "a::a");
    assertThat(got).contains("a::a", "a::a");

    // Check records
    FunctionData gotFD1 = got.get("", "a::a");
    assertThat(gotFD1.numCalls).isEqualTo(1);
    assertThat(gotFD1.total).isEqualTo(ofMillis(/*total3*/ 5));
    assertThat(gotFD1.totalSelf).isEqualTo(ofMillis(/*self3*/ 2));

    FunctionData gotFD2 = got.get("a::a", "a::a");
    assertThat(gotFD2.numCalls).isEqualTo(2);
    assertThat(gotFD2.total).isEqualTo(ofMillis(/*total1+total2*/ 1 + 3));
    assertThat(gotFD2.totalSelf).isEqualTo(ofMillis(/*self1+self2*/ 1 + 2));
  }

  public CallableFunction func(String name) {
    CallableFunction function = mock(CallableFunction.class);
    Signature signature = new Signature(name, name, ImmutableList.of(), false);

    when(function.getName()).thenReturn(name);
    when(function.getSignature()).thenReturn(signature);

    return function;
  }

  private static ImmutableTable<String, String, FunctionData> stripHashes(
      Table<String, String, FunctionData> table) {
    return table.cellSet().stream()
        .map(
            c ->
                new Cell<String, String, FunctionData>() {
                  @Override
                  public String getRowKey() {
                    return Iterables.get(Splitter.on(" @").split(c.getRowKey()), 0);
                  }

                  @Override
                  public String getColumnKey() {
                    return Iterables.get(Splitter.on(" @").split(c.getColumnKey()), 0);
                  }

                  @Override
                  public FunctionData getValue() {
                    return c.getValue();
                  }
                })
        .collect(
            ImmutableTable.toImmutableTable(
                c -> c.getRowKey(), c -> c.getColumnKey(), c -> c.getValue()));
  }

  private static class TickerClock implements TimeProvider {
    private long time;
    private static final int STEP = 1000_000;

    @Override
    public long nanoTime() {
      long ret = time;
      time += STEP;
      return ret;
    }
  }
}
