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

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.common.collect.ImmutableSet;
import java.util.function.Predicate;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Instrumentation Builder. */
@RunWith(JUnit4.class)
public class InstrumentationTest {
  @Test
  public void withFunctionCall_setsIt() {
    FunctionCallInstrument fnInstrument = mock(FunctionCallInstrument.class);

    Instrumentation.Builder builder = Instrumentation.builder();
    builder = builder.withFunctionCall(fnInstrument);
    assertThat(builder.build().functionCalls(mock(CallableFunction.class)))
        .isSameInstanceAs(fnInstrument);
  }

  @Test
  public void withFilter_callsIt() {
    FunctionCallInstrument fnInstrument = mock(FunctionCallInstrument.class);
    CallableFunction pass = mock(CallableFunction.class);
    CallableFunction fail = mock(CallableFunction.class);
    Predicate<CallableFunction> filter = mock(Filter.class);
    when(filter.test(any())).thenReturn(false);
    when(filter.test(refEq(pass))).thenReturn(true);

    Instrumentation.Builder builder = Instrumentation.builder();
    builder = builder.withFunctionCall(fnInstrument);
    builder = builder.withFunctionCallFilter(filter);
    Instrumentation instrumentation = builder.build();

    assertThat(instrumentation.functionCalls(pass)).isSameInstanceAs(fnInstrument);
    assertThat(instrumentation.functionCalls(fail)).isEqualTo(FunctionCallInstrument.noop());
  }

  @Test
  public void isNativeOrUserDefined_noDebugInfo_returnsFalse() {
    CallableFunction function = mock(CallableFunction.class);

    assertThat(Instrumentation.isNativeOrUserDefined(function)).isFalse();
  }

  @Test
  public void isNativeOrUserDefined_specificDebugInfoTypes() {
    ImmutableSet<FunctionType> wantTruthySpec =
        ImmutableSet.of(FunctionType.NATIVE, FunctionType.DECLARED);

    // Enforce test integrity.
    assertThat(FunctionType.values()).isNotEmpty();

    // Test all possible values.
    for (FunctionType value : FunctionType.values()) {
      if (value == FunctionType.UNRECOGNIZED) {
        continue;
      }
      DebugInfo info = DebugInfo.simpleFunction("test", value);
      CallableFunction function = mock(CallableFunction.class);
      when(function.getDebugInfo()).thenReturn(info);

      boolean want = wantTruthySpec.contains(value);
      assertThat(Instrumentation.isNativeOrUserDefined(function)).isEqualTo(want);
    }
  }

  private interface Filter extends Predicate<CallableFunction> {}
}
