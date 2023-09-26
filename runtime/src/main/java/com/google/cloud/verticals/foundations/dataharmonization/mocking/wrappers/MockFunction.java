/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers;

import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.debug.DebugInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/** A Wrapper for function being mocked. */
public class MockFunction extends CallableFunction {
  private final CallableFunction originalOverload;
  private final List<Mock> mocks;

  /**
   * @param originalOverload the original function being mocked.
   * @param mocks the {@link Mock}s associated with the {@code originalOverload}. Each Mock in the
   *     list is supposed to have compatible signature with the {@code originalOverload}.
   */
  public MockFunction(CallableFunction originalOverload, List<Mock> mocks) {
    this.originalOverload = originalOverload;
    this.mocks = ImmutableList.copyOf(mocks);
  }

  @Override
  protected Data callInternal(RuntimeContext context, Data... args) {
    List<Mock> runnableMock =
        this.mocks.stream().filter(m -> m.canRun(context, args)).collect(Collectors.toList());
    if (runnableMock.size() > 1) {
      throw new IllegalArgumentException(
          String.format(
              "Multiple mocks for function %s can be run with the given arguments. Consider adding"
                  + " selectors to the overloads so the current arguments can only satisfy one"
                  + " selector.\n"
                  + " Matching mocks are: %s \n"
                  + " Current arguments are: %s.",
              this.getSignature(),
              runnableMock,
              runnableMock.stream().map(Mock::toString).collect(joining(", "))));
    }
    if (runnableMock.isEmpty()) {
      return originalOverload.call(context, args);
    }
    Mock mockToRun = runnableMock.get(0);
    Set<InvocationRecord> mockInvocation = context.getMetaData().getMeta(Mock.INVOCATION_META_KEY);
    if (mockInvocation == null) {
      mockInvocation = new HashSet<>();
    }
    InvocationRecord invocationRecord = InvocationRecord.of(mockToRun.getMockRef(), args);
    mockInvocation.add(invocationRecord);
    context.getMetaData().setMeta(Mock.INVOCATION_META_KEY, mockInvocation);
    Data result = DefaultClosure.create(mockToRun.getMockRef(), args).execute(context);
    mockInvocation.remove(invocationRecord);
    return result;
  }

  @Override
  public Signature getSignature() {
    return originalOverload.getSignature();
  }

  @Override
  public DebugInfo getDebugInfo() {
    // TODO(): add specific FunctionType for mock function.
    return DebugInfo.simpleFunction(getClass().getName(), FunctionType.NATIVE);
  }

  public CallableFunction getOriginalOverload() {
    return originalOverload;
  }

  public List<Mock> getMocks() {
    return new ArrayList<>(mocks);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MockFunction
        && Objects.equals(originalOverload, ((MockFunction) obj).getOriginalOverload())
        && mocks.containsAll(((MockFunction) obj).getMocks())
        && ((MockFunction) obj).getMocks().containsAll(mocks);
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.originalOverload, mocks);
  }
}
