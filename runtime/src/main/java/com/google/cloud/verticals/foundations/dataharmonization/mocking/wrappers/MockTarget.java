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
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * A wrapper for {@link Target} being mocked. Target is mocked using a {@link
 * com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction} whose
 * last parameter is converted into a free parameter that will be bound to the value written to the
 * {@link MockTarget}.
 */
public class MockTarget implements Target {

  private final DefaultClosure partial;

  private MockTarget(DefaultClosure partial) {
    this.partial = partial;
  }

  @Override
  public void write(RuntimeContext ctx, Data value) {
    Set<InvocationRecord> mockInvocation = ctx.getMetaData().getMeta(Mock.INVOCATION_META_KEY);
    if (mockInvocation == null) {
      mockInvocation = new HashSet<>();
    }
    InvocationRecord invocationRecord =
        InvocationRecord.of(
            partial.getFunctionRef(),
            Arrays.copyOf(partial.getArgs(), partial.getArgs().length - 1));
    mockInvocation.add(invocationRecord);
    ctx.getMetaData().setMeta(Mock.INVOCATION_META_KEY, mockInvocation);
    partial.bindNextFreeParameter(value).execute(ctx);
    mockInvocation.remove(invocationRecord);
  }

  /**
   * {@link com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor} for
   * {@link MockTarget}.
   */
  public static class Constructor implements Target.Constructor {
    private final Target.Constructor backing;
    private final List<Mock> mocks;

    public Constructor(Target.Constructor backing, List<Mock> mocks) {
      this.backing = backing;
      this.mocks = ImmutableList.copyOf(mocks);
    }

    @Override
    public Target construct(RuntimeContext ctx, Data... args) {
      List<Mock> runnableMock =
          mocks.stream().filter(m -> m.canRun(ctx, args)).collect(Collectors.toList());
      if (runnableMock.size() > 1) {
        throw new IllegalArgumentException(
            String.format(
                "Multiple mocks for target %s can be run with the given arguments. Consider"
                    + " adding selectors to the overloads so the current arguments can satisfy one"
                    + " selector.\n"
                    + " Matching mocks are: %s \n"
                    + " Current arguments are: %s.",
                getName(),
                runnableMock,
                runnableMock.stream().map(Mock::toString).collect(joining(", "))));
      }
      if (runnableMock.isEmpty()) {
        return backing.construct(ctx, args);
      }

      SortedSet<Integer> freeArgIndex = new TreeSet<>();
      freeArgIndex.add(args.length);
      Data[] fullArgSet = Arrays.copyOf(args, args.length + 1);
      fullArgSet[args.length] = new DefaultPrimitive("value to write");
      return new MockTarget(
          DefaultClosure.create(runnableMock.get(0).getMockRef(), freeArgIndex, fullArgSet));
    }

    public Target.Constructor getBacking() {
      return backing;
    }

    public List<Mock> getMocks() {
      return ImmutableList.copyOf(mocks);
    }

    @Override
    public String getName() {
      return backing.getName();
    }

    @Override
    public int hashCode() {
      return Objects.hash(backing, mocks);
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MockTarget.Constructor
          && backing == ((Constructor) obj).getBacking()
          && mocks.containsAll(((Constructor) obj).getMocks())
          && ((Constructor) obj).getMocks().containsAll(mocks);
    }
  }
}
