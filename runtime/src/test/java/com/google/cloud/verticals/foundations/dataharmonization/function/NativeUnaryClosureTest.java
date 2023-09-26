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
package com.google.cloud.verticals.foundations.dataharmonization.function;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure.FreeParameter;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for NativeUnaryClosure. */
@RunWith(JUnit4.class)
public class NativeUnaryClosureTest {

  @Test
  public void isNullOrEmpty_false() {
    Closure cl = new NativeUnaryClosure(Function.identity());
    assertThat(cl.isNullOrEmpty()).isFalse();
  }

  @Test
  public void isWritable_false() {
    Closure cl = new NativeUnaryClosure(Function.identity());
    assertThat(cl.isNullOrEmpty()).isFalse();
  }

  @Test
  public void getArgs_unbound_returnsFree() {
    Closure cl = new NativeUnaryClosure(Function.identity());
    Data[] got = cl.getArgs();
    assertThat(got).hasLength(1);
    assertThat(got[0]).isInstanceOf(FreeParameter.class);
  }

  @Test
  public void getArgs_bound_returnsArg() {
    Data binding = mock(Data.class);
    Closure cl = new NativeUnaryClosure(Function.identity(), binding);
    Data[] got = cl.getArgs();
    assertThat(got).hasLength(1);
    assertThat(got[0]).isSameInstanceAs(binding);
  }

  @Test
  public void bindNextFreeParameter_unbound_binds() {
    Data binding = mock(Data.class);
    Closure unbound = new NativeUnaryClosure(Function.identity());
    Closure bound = unbound.bindNextFreeParameter(binding);

    assertThat(unbound.getArgs()[0]).isInstanceOf(FreeParameter.class);
    assertThat(bound.getArgs()[0]).isSameInstanceAs(binding);
  }

  @Test
  public void bindNextFreeParameter_bound_throws() {
    Data binding = mock(Data.class);
    Closure bound = new NativeUnaryClosure(Function.identity(), binding);
    assertThrows(IllegalStateException.class, () -> bound.bindNextFreeParameter(binding));
  }

  @Test
  public void execute_unbound_throws() {
    Closure unbound = new NativeUnaryClosure(Function.identity());
    assertThrows(IllegalStateException.class, () -> unbound.execute(mock(RuntimeContext.class)));
  }

  @Test
  public void execute_bound_callsDelegate() {
    Data binding = mock(Data.class);
    Function<Data, Data> delegate = mock(DataFunction.class);
    Closure bound = new NativeUnaryClosure(delegate, binding);
    bound.execute(mock(RuntimeContext.class));

    verify(delegate).apply(binding);
  }

  @Test
  public void getNumFreeParams_unbound_1() {
    Closure unbound = new NativeUnaryClosure(Function.identity());
    assertThat(unbound.getNumFreeParams()).isEqualTo(1);
  }

  @Test
  public void getNumFreeParams_bound_0() {
    Closure bound = new NativeUnaryClosure(Function.identity(), mock(Data.class));
    assertThat(bound.getNumFreeParams()).isEqualTo(0);
  }

  @Test
  public void getFreeArgIndices_unbound_0() {
    Closure unbound = new NativeUnaryClosure(Function.identity());
    assertThat(unbound.getFreeArgIndices()).containsExactly(0);
  }

  @Test
  public void getFreeArgIndices_bound_empty() {
    Closure bound = new NativeUnaryClosure(Function.identity(), mock(Data.class));
    assertThat(bound.getFreeArgIndices()).isEmpty();
  }

  private interface DataFunction extends Function<Data, Data> {}
}
