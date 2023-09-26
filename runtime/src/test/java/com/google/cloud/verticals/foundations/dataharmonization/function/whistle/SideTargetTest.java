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

package com.google.cloud.verticals.foundations.dataharmonization.function.whistle;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.MergeModeExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Option;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for side target. */
@RunWith(JUnit4.class)
public class SideTargetTest {

  @Test
  public void write_noStackEmptyPath_writesBottomFrame() {
    StackFrame sf = mock(StackFrame.class);
    when(sf.getVar(anyString())).thenReturn(NullData.instance);

    RuntimeContext context = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    when(context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(null);
    when(context.bottom()).thenReturn(sf);

    SideTarget target = new SideTarget(Path.empty());

    Data expected = mock(Data.class);
    target.write(context, expected);

    verify(sf).setVar(WhistleFunction.OUTPUT_VAR, expected);
  }

  @Test
  public void write_withStackEmptyPath_writesTopStackItem() {
    Deque<Data> stack = new ArrayDeque<>();
    stack.push(NullData.instance);

    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    when(context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(stack);

    SideTarget target = new SideTarget(Path.empty());

    Data expected = mock(Data.class);
    target.write(context, expected);

    assertThat(stack).hasSize(1);
    Data actual = stack.peek();
    assertEquals(expected, actual);
  }

  @Test
  public void write_withStackEmptyPathMerge_mergesTopStackItem() {
    Deque<Data> stack = new ArrayDeque<>();
    stack.push(mutableContainerOf((i) -> i.set("a", testDTI().primitiveOf("aa"))));

    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    when(context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(stack);

    SideTarget target = new SideTarget(Path.empty());

    Data inbound = mutableContainerOf((i) -> i.set("b", testDTI().primitiveOf("bb")));
    target.write(context, inbound);

    assertThat(stack).hasSize(1);
    Data actual = stack.peek();
    Data expected =
        mutableContainerOf(
            i -> {
              i.set("a", testDTI().primitiveOf("aa"));
              i.set("b", testDTI().primitiveOf("bb"));
            });

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void write_withStackWithFieldMerge_mergesTopStackItem() {
    Deque<Data> stack = new ArrayDeque<>();
    stack.push(mutableContainerOf((i) -> i.set("a", testDTI().primitiveOf("aa"))));

    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    when(context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(stack);

    SideTarget target = new SideTarget(Path.parse("b"));

    Data inbound = testDTI().primitiveOf("bb");
    target.write(context, inbound);

    assertThat(stack).hasSize(1);
    Data actual = stack.peek();
    Data expected =
        mutableContainerOf(
            i -> {
              i.set("a", testDTI().primitiveOf("aa"));
              i.set("b", testDTI().primitiveOf("bb"));
            });

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void write_withStackWithFieldMergeMode_replacesTopStackItem() {
    Deque<Data> stack = new ArrayDeque<>();
    stack.push(
        mutableContainerOf((i) -> i.set("a", testDTI().arrayOf(testDTI().primitiveOf("aa")))));

    RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithMockedMetaData();
    when(context.getMetaData().getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(stack);
    context = new MergeModeExperiment().enable(context, Option.getDefaultInstance());

    Target target =
        new SideTarget.Constructor()
            .construct(context, testDTI().primitiveOf("a"), testDTI().primitiveOf("replace"));

    Data inbound = testDTI().arrayOf(testDTI().primitiveOf("bb"));
    target.write(context, inbound);

    assertThat(stack).hasSize(1);
    Data actual = stack.peek();
    Data expected =
        mutableContainerOf(i -> i.set("a", testDTI().arrayOf(testDTI().primitiveOf("bb"))));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void write_withStackWithField_writesTopStackItem() {
    Deque<Data> stack = new ArrayDeque<>();
    stack.push(NullData.instance);

    RuntimeContext context = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    MetaData metaData = mock(MetaData.class);
    when(metaData.getMeta(SideTarget.SIDES_STACK_META_KEY)).thenReturn(stack);
    when(context.getMetaData()).thenReturn(metaData);
    when(context.getDataTypeImplementation()).thenReturn(testDTI());

    SideTarget target = new SideTarget(Path.parse("b"));

    Data inbound = testDTI().primitiveOf("bb");
    target.write(context, inbound);

    assertThat(stack).hasSize(1);
    Data actual = stack.peek();
    Data expected = mutableContainerOf(i -> i.set("b", testDTI().primitiveOf("bb")));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void construct_explicitMergeModeWithoutOption_errors() {
    UnsupportedOperationException ex =
        assertThrows(
            UnsupportedOperationException.class,
            () ->
                new SideTarget.Constructor()
                    .construct(
                        mockRuntimeContextWithDefaultMetaData(),
                        testDTI().primitiveOf("a"),
                        testDTI().primitiveOf("replace")));
    assertThat(ex).hasMessageThat().contains(new MergeModeExperiment().getName());
  }
}
