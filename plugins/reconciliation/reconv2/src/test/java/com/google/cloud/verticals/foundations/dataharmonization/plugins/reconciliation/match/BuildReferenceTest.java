/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.match;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.MatchingPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for buildReferenceFor plugin function. */
@RunWith(JUnit4.class)
public class BuildReferenceTest {
  private RuntimeContext ctx;
  private MatchingPlugin matchingPlugin;

  @Before
  public void setup() {
    matchingPlugin = new MatchingPlugin();
    ctx = RuntimeContextUtil.mockRuntimeContextWithRegistry();
  }

  @Test
  public void buildReferenceFor_nullFragments_storeFragments() {
    Container inputFragment =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "resourceType",
                    testDTI().primitiveOf("Patient"),
                    "id",
                    testDTI().primitiveOf("1")));
    Primitive reference = matchingPlugin.buildReferenceFor(ctx, inputFragment);

    ImmutableList<Container> expected = ImmutableList.of(inputFragment);

    ImmutableList<Container> actual = getFragments(ctx);
    assertEquals(expected, actual);
    assertEquals(testDTI().primitiveOf("Patient/1"), reference);
  }

  @Test
  public void buildReferenceFor_withExistingFragments_storeFragments() {
    // Preset fragment resource 1.
    Container inputFragment1 =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "resourceType",
                    testDTI().primitiveOf("Patient"),
                    "id",
                    testDTI().primitiveOf("1")));

    ctx.getMetaData()
        .setSerializableMeta("fragments", testDTI().arrayOf(ImmutableList.of(inputFragment1)));

    Container inputFragment2 =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "resourceType",
                    testDTI().primitiveOf("Patient"),
                    "id",
                    testDTI().primitiveOf("2")));

    Primitive reference = matchingPlugin.buildReferenceFor(ctx, inputFragment2);

    ImmutableList<Container> expected = ImmutableList.of(inputFragment1, inputFragment2);

    ImmutableList<Container> actual = getFragments(ctx);
    assertEquals(expected, actual);
    assertEquals(testDTI().primitiveOf("Patient/2"), reference);
  }

  private static ImmutableList<Container> getFragments(RuntimeContext ctx) {
    Array fragments = ctx.getMetaData().getSerializableMeta("fragments");
    return fragments.stream().map(Data::asContainer).collect(toImmutableList());
  }
}
