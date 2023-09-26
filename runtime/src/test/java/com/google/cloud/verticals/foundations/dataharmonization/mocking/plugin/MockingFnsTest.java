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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.plugin;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link MockingPlugin}. */
@RunWith(JUnit4.class)
public class MockingFnsTest {

  private RuntimeContext context;
  private MetaData metaData;

  @Before
  public void setUp() {
    metaData = new DefaultMetaData();
    context = mock(RuntimeContext.class);
    when(context.getMetaData()).thenReturn(metaData);
  }

  @Test
  public void registerMock_modifyMetaData() {
    MockingFns.mock(
        this.context, "origPkg::origFunc", "mockPkg::mockFunc", "selectorPkg::selectorFunc");
    MockingFns.mock(this.context, "origPkg::origFunc", "mockPkg::mockFunc1");

    FunctionReference keyExpected = new FunctionReference("origPkg", "origFunc");
    List<Mock> valueExpected =
        new ArrayList<>(
            ImmutableList.of(
                new Mock(
                    new FunctionReference("mockPkg", "mockFunc"),
                    new FunctionReference("selectorPkg", "selectorFunc")),
                new Mock(new FunctionReference("mockPkg", "mockFunc1"), null)));
    Map<FunctionReference, List<Mock>> actualMap =
        metaData.getSerializableMeta(MockingPlugin.MOCK_META_KEY);

    assertThat(actualMap).containsEntry(keyExpected, valueExpected);
  }

  @Test
  public void registerMockTarget_modifyMetaData() {
    MockingFns.mockTarget(
        this.context, "origPkg::origTarget", "mockPkg::mockTarget", "selectorPkg::selectorFunc");
    MockingFns.mockTarget(this.context, "origPkg::origTarget", "mockPkg::mockTarget1");

    FunctionReference keyExpected = new FunctionReference("origPkg", "origTarget");
    List<Mock> valueExpected =
        new ArrayList<>(
            ImmutableList.of(
                new Mock(
                    new FunctionReference("mockPkg", "mockTarget"),
                    new FunctionReference("selectorPkg", "selectorFunc")),
                new Mock(new FunctionReference("mockPkg", "mockTarget1"), null)));
    Map<FunctionReference, List<Mock>> actualMap =
        metaData.getSerializableMeta(MockingPlugin.MOCK_TARGET_META_KEY);

    assertThat(actualMap).containsEntry(keyExpected, valueExpected);
  }

  @Test
  public void registerMockWrongFormat_throwError() {
    Exception e =
        Assert.assertThrows(
            IllegalArgumentException.class,
            () -> MockingFns.mock(this.context, "OrigPkg::OrigFunc", "mockFuncWithoutPkg"));
    assertThat(e)
        .hasMessageThat()
        .contains("Failed to parse function reference mockFuncWithoutPkg.");
  }
}
