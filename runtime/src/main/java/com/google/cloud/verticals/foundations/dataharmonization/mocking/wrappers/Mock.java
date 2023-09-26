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

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Ternary;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Reference to a mock of a function. Contains the logic to determine if the mock function can be
 * executed.
 */
public class Mock implements Serializable {

  // The key to retrieve the set of past invocation record from function metaData.
  static final String INVOCATION_META_KEY = "MOCK_FUNC_INVOCATION";

  private final FunctionReference mockRef;
  private final FunctionReference selectorRef;

  /**
   * @param mockRef {@link FunctionReference} to the mock function.
   * @param selectorRef {@link FunctionReference} to the selector function.
   */
  public Mock(FunctionReference mockRef, FunctionReference selectorRef) {
    this.mockRef = mockRef;
    this.selectorRef = selectorRef;
  }

  public FunctionReference getMockRef() {
    return this.mockRef;
  }

  public FunctionReference getSelectorRef() {
    return this.selectorRef;
  }

  /**
   * Returns if this {@link Mock} can be run. i.e. this Mock has not been executed before AND its
   * selector, if there is any, returns true given the arguments.
   *
   * @param context the current {@link RuntimeContext}.
   * @param args the argument to the {@link MockFunction#call}
   */
  boolean canRun(RuntimeContext context, Data... args) {
    Set<InvocationRecord> pastInvocation = context.getMetaData().getMeta(INVOCATION_META_KEY);
    // TODO (): better error message when selector has incompatible signature.
    boolean alreadyExecuted =
        pastInvocation != null && pastInvocation.contains(InvocationRecord.of(mockRef, args));
    if (alreadyExecuted) {
      return false;
    }
    if (selectorRef == null) {
      return true;
    }
    if (pastInvocation == null) {
      pastInvocation = new HashSet<>();
    }

    InvocationRecord selectorRecord = InvocationRecord.of(selectorRef, args);

    if (pastInvocation.contains(selectorRecord)) {
      return false;
    }

    pastInvocation.add(selectorRecord);
    context.getMetaData().setMeta(INVOCATION_META_KEY, pastInvocation);
    boolean selectorOk =
        Ternary.isTruthy(DefaultClosure.create(selectorRef, args).execute(context));
    pastInvocation.remove(selectorRecord);

    return selectorOk;
  }

  @Override
  public String toString() {
    String result = String.format("Mock: %s", this.mockRef);
    return this.selectorRef == null
        ? result
        : result + String.format(" (selector: %s", this.selectorRef);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Mock
        && Objects.equals(this.mockRef, ((Mock) obj).getMockRef())
        && Objects.equals(this.selectorRef, ((Mock) obj).getSelectorRef());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.mockRef, this.selectorRef);
  }
}
