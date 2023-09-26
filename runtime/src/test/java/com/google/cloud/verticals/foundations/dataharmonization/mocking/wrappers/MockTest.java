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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableSet;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Test for {@link Mock}. */
@RunWith(JUnit4.class)
public class MockTest {

  @Test
  public void runtimeContextHasInvocation_funcNoArgs_canRunFalse() {
    RuntimeContext rtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    FunctionReference mockFuncRef = new FunctionReference("mockPkg", "mockFunc");
    InvocationRecord mockFuncInvocation = InvocationRecord.of(mockFuncRef, new Data[0]);
    ImmutableSet<InvocationRecord> pastInvocation = ImmutableSet.of(mockFuncInvocation);
    when(rtx.getMetaData().getMeta(Mock.INVOCATION_META_KEY)).thenReturn(pastInvocation);

    Mock mockToTest = new Mock(mockFuncRef, null);
    Assert.assertFalse(mockToTest.canRun(rtx));
  }

  @Test
  public void runtimeContextHasInvocation_functionHadSameArgs_canRunReturnsFalse() {
    RuntimeContext rtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    FunctionReference mockFuncRef = new FunctionReference("mockPkg", "mockFunc");
    Data arg = mock(Data.class);
    InvocationRecord mockFuncInvocation = InvocationRecord.of(mockFuncRef, new Data[] {arg});
    ImmutableSet<InvocationRecord> pastInvocation = ImmutableSet.of(mockFuncInvocation);
    when(rtx.getMetaData().getMeta(Mock.INVOCATION_META_KEY)).thenReturn(pastInvocation);

    Mock mockToTest = new Mock(mockFuncRef, null);
    Assert.assertFalse(mockToTest.canRun(rtx, arg));
  }

  @Test
  public void runtimeContextHasInvocation_functionHadDifferentDataArgs_canRunReturnsFalse() {
    RuntimeContext rtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    FunctionReference mockFuncRef = new FunctionReference("mockPkg", "mockFunc");
    Data arg = mock(Data.class);
    Data anotherData = mock(Data.class);
    InvocationRecord mockFuncInvocation = InvocationRecord.of(mockFuncRef, new Data[] {arg});
    ImmutableSet<InvocationRecord> pastInvocation = ImmutableSet.of(mockFuncInvocation);
    when(rtx.getMetaData().getMeta(Mock.INVOCATION_META_KEY)).thenReturn(pastInvocation);

    Mock mockToTest = new Mock(mockFuncRef, null);
    Assert.assertFalse(mockToTest.canRun(rtx, anotherData));
  }

  @Test
  public void runtimeContextHasInvocation_functionHasDifferentClosureArgs_canRunReturnsFalse() {
    RuntimeContext rtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    FunctionReference mockFuncRef = new FunctionReference("mockPkg", "mockFunc");
    Data closure1 = mock(Closure.class);
    when(closure1.isClass(any())).thenAnswer(Answers.CALLS_REAL_METHODS);
    Data data = mock(Data.class);
    Data closure2 = mock(Closure.class);
    when(closure2.isClass(any())).thenAnswer(Answers.CALLS_REAL_METHODS);
    InvocationRecord mockFuncInvocation =
        InvocationRecord.of(mockFuncRef, new Data[] {closure1, data});
    ImmutableSet<InvocationRecord> pastInvocation = ImmutableSet.of(mockFuncInvocation);
    when(rtx.getMetaData().getMeta(Mock.INVOCATION_META_KEY)).thenReturn(pastInvocation);

    Mock mockToTest = new Mock(mockFuncRef, null);
    Assert.assertTrue(mockToTest.canRun(rtx, closure2, data));
  }

  @Test
  public void runtimeContextHasInvocation_functionHasDifferentArgNumber_canRunReturnsFalse() {
    RuntimeContext rtx = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    FunctionReference mockFuncRef = new FunctionReference("mockPkg", "mockFunc");
    Data data1 = mock(Data.class);
    Data data2 = mock(Data.class);
    InvocationRecord mockFuncInvocation =
        InvocationRecord.of(mockFuncRef, new Data[] {data1, data2});
    ImmutableSet<InvocationRecord> pastInvocation = ImmutableSet.of(mockFuncInvocation);
    when(rtx.getMetaData().getMeta(Mock.INVOCATION_META_KEY)).thenReturn(pastInvocation);

    Mock mockToTest = new Mock(mockFuncRef, null);
    Assert.assertTrue(mockToTest.canRun(rtx, data1));
  }
}
