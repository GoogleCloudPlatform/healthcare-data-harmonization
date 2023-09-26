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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.function.BiFunction;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;

/** Tests for instrumentation integration with runtime context. */
@RunWith(JUnit4.class)
public class InstrumentedWrapperContextTest {

  @Test
  public void wrap_hooksFunctionCall() {
    // Mock up profiler.
    FunctionCallInstrument callInstrument = mock(FunctionCallInstrument.class);
    Instrumentation instrumentation =
        Instrumentation.builder().withFunctionCall(callInstrument).build();

    // Mock up inner context that just calls the delegate.
    RuntimeContext inner = mock(RuntimeContext.class);
    when(inner.wrap(any(), any(), any()))
        .thenAnswer(
            a ->
                a.<BiFunction<RuntimeContext, Data[], Data>>getArgument(2)
                    .apply(inner, new Data[0]));

    // Mock up inputs.
    Data wantResult = mock(Data.class);
    CallableFunction wantCallable = mock(CallableFunction.class);
    BiFunction<RuntimeContext, Data[], Data> delegate = (r, a) -> wantResult;

    // Invoke the test.
    InstrumentedWrapperContext instrumentedWrapperContext =
        new InstrumentedWrapperContext(inner, instrumentation);
    Data got = instrumentedWrapperContext.wrap(wantCallable, new Data[0], delegate);

    // Assert that profiler got called.
    assertThat(got).isSameInstanceAs(wantResult);
    InOrder callOrder = inOrder(callInstrument);
    callOrder.verify(callInstrument).startCall(wantCallable);
    callOrder.verify(callInstrument).endCall(wantCallable);
  }
}
