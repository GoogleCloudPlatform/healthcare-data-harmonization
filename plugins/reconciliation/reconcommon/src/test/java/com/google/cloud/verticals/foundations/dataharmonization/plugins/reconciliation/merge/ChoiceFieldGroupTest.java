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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for the ChoiceFieldGroup class. */
@RunWith(JUnit4.class)
public class ChoiceFieldGroupTest {

  @Test
  public void builder_build_emptyBuild() {
    ChoiceFieldGroup empty = ChoiceFieldGroup.builder().build();

    assertEquals(Collections.emptySet(), empty.getGroups());
    // _All_ possible fields will not be a choice field in the empty group.
    assertFalse(empty.isChoiceField("any"));
  }

  @Test
  public void builder_withGroup_build_singletonGroup() {
    String singleField = "onlyOneField";
    ChoiceFieldGroup singletonGroupSet = ChoiceFieldGroup.builder().withGroup(singleField).build();

    // Single choice field group generated.
    ImmutableSet<ImmutableSet<String>> expected = ImmutableSet.of(ImmutableSet.of(singleField));
    assertEquals(expected, singletonGroupSet.getGroups());

    // 'singleField' set as a choice field.
    assertTrue(singletonGroupSet.isChoiceField(singleField));
  }

  @Test
  public void builder_multipleWithGroupCalls_multipleGroupSetsGenerated() {
    String groupOneFieldOne = "oneOne";
    String groupOneFieldTwo = "oneTwo";
    String groupTwoFieldOne = "twoOne";
    String groupTwoFieldTwo = "twoTwo";
    ChoiceFieldGroup multipleGroupSets =
        ChoiceFieldGroup.builder()
            .withGroup(groupOneFieldOne, groupOneFieldTwo)
            .withGroup(groupTwoFieldOne, groupTwoFieldTwo)
            .build();

    // Multiple choice field sets generated.
    ImmutableSet<ImmutableSet<String>> expected =
        ImmutableSet.of(
            ImmutableSet.of(groupOneFieldOne, groupOneFieldTwo),
            ImmutableSet.of(groupTwoFieldOne, groupTwoFieldTwo));
    assertEquals(expected, multipleGroupSets.getGroups());

    // All four choice fields properly return as such.
    assertTrue(multipleGroupSets.isChoiceField(groupOneFieldOne));
    assertTrue(multipleGroupSets.isChoiceField(groupOneFieldTwo));
    assertTrue(multipleGroupSets.isChoiceField(groupTwoFieldOne));
    assertTrue(multipleGroupSets.isChoiceField(groupTwoFieldTwo));
  }
}
