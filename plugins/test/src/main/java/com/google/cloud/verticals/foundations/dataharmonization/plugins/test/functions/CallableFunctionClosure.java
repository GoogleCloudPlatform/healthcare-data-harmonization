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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions;

import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableSortedSet;
import java.util.SortedSet;

/** A closure for calling a given CallableFunction. Does not support free parameters. */
public class CallableFunctionClosure implements Closure {

  private final CallableFunction function;
  private final Data[] boundArgs;

  public CallableFunctionClosure(CallableFunction function, Data[] boundArgs) {
    this.function = function;
    this.boundArgs = boundArgs;
  }

  @Override
  public boolean isNullOrEmpty() {
    return false;
  }

  @Override
  public Data deepCopy() {
    return new CallableFunctionClosure(
        function, stream(boundArgs).map(Data::deepCopy).toArray(Data[]::new));
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public Data[] getArgs() {
    return boundArgs;
  }

  @Override
  public Closure bindNextFreeParameter(Data value) {
    throw new UnsupportedOperationException(
        String.format(
            "CallableFunctionClosure for %s does not support free parameters", getName()));
  }

  @Override
  public Data execute(RuntimeContext context) {
    return function.call(context, boundArgs);
  }

  @Override
  public int getNumFreeParams() {
    return 0;
  }

  @Override
  public SortedSet<Integer> getFreeArgIndices() {
    return ImmutableSortedSet.of();
  }

  @Override
  public String getName() {
    return function.getName();
  }
}
