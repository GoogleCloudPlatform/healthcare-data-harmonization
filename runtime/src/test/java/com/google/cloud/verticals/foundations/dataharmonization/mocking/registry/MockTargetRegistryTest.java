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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.registry;

import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.MockTarget;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link MockTargetRegistry}. */
@RunWith(JUnit4.class)
public class MockTargetRegistryTest {

  private MockTargetRegistry reg;
  private static final String MATCHED_TARGET_PKG = "MATCHED_PKG";
  private static final String MATCHED_TARGET_NAME = "MATCHED_NAME";
  private static final String UNMATCHED_TARGET_PKG = "UNMATCHED_PKG";
  private static final String UNMATCHED_TARGET_NAME = "UNMATCHED_NAME";
  private static final Target.Constructor MATCHED_TARGET =
      new TestTargetConstructor(MATCHED_TARGET_NAME);
  private static final Target.Constructor UNMATCHED_TARGET =
      new TestTargetConstructor(UNMATCHED_TARGET_NAME);
  private static final ImmutableList<Mock> mocks =
      ImmutableList.of(
          new Mock(
              new FunctionReference("mockPkg", "mockFuncName"),
              new FunctionReference("selectorPkg", "selectorName")));

  @Before
  public void setUp() {
    Map<FunctionReference, List<Mock>> originToMocks = new HashMap<>();
    originToMocks.put(new FunctionReference(MATCHED_TARGET_PKG, MATCHED_TARGET_NAME), mocks);
    reg = new MockTargetRegistry(originToMocks);
    reg.register(MATCHED_TARGET_PKG, MATCHED_TARGET);
    reg.register(UNMATCHED_TARGET_PKG, UNMATCHED_TARGET);
  }

  @Test
  public void matchFuncRef_getMockTarget() {
    Set<Constructor> actual =
        reg.getOverloads(
            ImmutableSet.of(MATCHED_TARGET_PKG, "IrrelevantPackage"), MATCHED_TARGET_NAME);
    Set<Constructor> expected = ImmutableSet.of(new MockTarget.Constructor(MATCHED_TARGET, mocks));
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void noMatchingFuncRef_getOriginal() {
    Set<Constructor> actual =
        reg.getOverloads(ImmutableSet.of(UNMATCHED_TARGET_PKG), UNMATCHED_TARGET_NAME);
    Set<Constructor> expected = ImmutableSet.of(UNMATCHED_TARGET);
    Assert.assertEquals(expected, actual);
  }
}
