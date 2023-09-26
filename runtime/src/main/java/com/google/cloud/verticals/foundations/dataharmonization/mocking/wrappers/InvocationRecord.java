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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;

/**
 * Java POJO for an invocation for a {@link CallableFunction} with a list of arguments in Whistle.
 * Used in the mocking framework to determine if a mock function or target is called by itself.
 */
class InvocationRecord {
  // TODO(): refactor this to use autovalue.
  private final FunctionReference functionReference;
  private final ImmutableList<Data> args;

  private InvocationRecord(FunctionReference reference, List<Data> args) {
    this.functionReference = reference;
    this.args = ImmutableList.copyOf(args);
  }

  public static InvocationRecord of(FunctionReference reference, Data[] args) {
    // only keep the closure arguments and make others NullData
    ImmutableList<Data> filteredArgs =
        stream(args)
            .map(d -> d.isClass(Closure.class) ? d : NullData.instance)
            .collect(toImmutableList());
    return new InvocationRecord(reference, filteredArgs);
  }

  FunctionReference getFunctionReference() {
    return functionReference;
  }

  List<Data> getArgs() {
    return args;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof InvocationRecord)) {
      return false;
    }
    InvocationRecord that = (InvocationRecord) obj;
    return Objects.equals(getFunctionReference(), that.getFunctionReference())
        && getArgs().equals(that.getArgs());
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionReference, args);
  }
}
