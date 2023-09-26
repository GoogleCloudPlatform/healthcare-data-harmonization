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

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableSortedSet;
import java.io.Serializable;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Function;

/** A closure for a native function that has only a single (free) parameter. */
public class NativeUnaryClosure implements Closure {
  private final SerializableBiFunction<RuntimeContext, Data, Data> delegate;
  private final Data freeArg;
  private final boolean isBound;

  public NativeUnaryClosure(Function<Data, Data> delegate) {
    this((ctx, x) -> delegate.apply(x), new FreeParameter("$"));
  }

  public NativeUnaryClosure(SerializableBiFunction<RuntimeContext, Data, Data> delegate) {
    this(delegate, new FreeParameter("$"));
  }

  public NativeUnaryClosure(Function<Data, Data> delegate, Data freeArg) {
    this((ctx, x) -> delegate.apply(x), freeArg);
  }

  public NativeUnaryClosure(
      SerializableBiFunction<RuntimeContext, Data, Data> delegate, Data freeArg) {
    this.delegate = delegate;
    this.freeArg = freeArg;
    this.isBound = !(freeArg instanceof FreeParameter);
  }

  @Override
  public boolean isNullOrEmpty() {
    return false;
  }

  @Override
  public Data deepCopy() {
    return new NativeUnaryClosure(delegate, freeArg.deepCopy());
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public Data[] getArgs() {
    return new Data[] {freeArg};
  }

  @Override
  public Closure bindNextFreeParameter(Data value) {
    if (isBound) {
      throw new IllegalStateException("Cannot re-bind already bound free parameter.");
    }
    return new NativeUnaryClosure(delegate, value);
  }

  @Override
  public Data execute(RuntimeContext context) {
    if (!isBound) {
      throw new IllegalStateException("Cannot execute with unbound free parameter.");
    }
    return delegate.apply(context, freeArg);
  }

  @Override
  public int getNumFreeParams() {
    return !isBound ? 1 : 0;
  }

  @Override
  public SortedSet<Integer> getFreeArgIndices() {
    return !isBound ? ImmutableSortedSet.of(0) : ImmutableSortedSet.of();
  }

  @Override
  public String getName() {
    return "NativeLambda";
  }

  /** Serializable version of {@link BiFunction} */
  public interface SerializableBiFunction<T, U, V> extends BiFunction<T, U, V>, Serializable {}
}
