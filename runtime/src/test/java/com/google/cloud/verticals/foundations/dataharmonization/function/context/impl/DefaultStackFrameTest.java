/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame.DefaultBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultStackFrame. */
@RunWith(JUnit4.class)
public class DefaultStackFrameTest {

  @Test
  public void getVar_nonExistingVar_returnsNull() {
    StackFrame sf = new DefaultStackFrame.DefaultBuilder().build();

    assertTrue(sf.getVar("any").isNullOrEmpty());
    assertThat(sf.getVars()).isEmpty();
  }

  @Test
  public void getVar_inheritedNonExistingVar_returnsNull() {
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();

    assertTrue(child.getVar("any").isNullOrEmpty());
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void setVarGetVar_existingVar_returnsIt() {
    Data value = mock(Data.class);
    StackFrame sf = new DefaultStackFrame.DefaultBuilder().build();
    sf.setVar("any", value);

    Assert.assertEquals(value, sf.getVar("any"));
    assertEquals(ImmutableSet.of("any"), sf.getVars());
  }

  @Test
  public void getVar_inheritedExistingVar_returnsIt() {
    Data value = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    parent.setVar("any", value);

    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();

    Assert.assertEquals(value, child.getVar("any"));
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void getVar_inheritedSpecialCaseVar_returnsLocal() {
    Data value = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    parent.setVar(WhistleFunction.OUTPUT_VAR, value);

    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();

    Assert.assertEquals(NullData.instance, child.getVar("any"));
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void getVar_deeplyInheritedExistingVar_returnsIt() {
    Data value = mock(Data.class);
    StackFrame grandparent = new DefaultStackFrame.DefaultBuilder().build();
    grandparent.setVar("any", value);
    StackFrame parent =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(grandparent)
            .setInheritParentVars(true)
            .build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();

    Assert.assertEquals(value, child.getVar("any"));
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void getVar_deeplyInheritedShadowedExistingVar_returnsIt() {
    Data originalVal = mock(Data.class);
    Data shadowVal = mock(Data.class);
    StackFrame grandparent = new DefaultStackFrame.DefaultBuilder().build();
    grandparent.setVar("any", originalVal);
    StackFrame parent =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(grandparent)
            .setInheritParentVars(true)
            .build();
    parent.setLocalVar("any", shadowVal);
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();

    Assert.assertEquals(shadowVal, child.getVar("any"));
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void getVar_nonInheritedExistingVar_returnsNull() {
    Data value = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    parent.setVar("any", value);

    StackFrame child =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(parent)
            .setInheritParentVars(false)
            .build();

    assertTrue(child.getVar("any").isNullOrEmpty());
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void setVar_nonInheritedNonExistingVar_setsOnChild() {
    Data value = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(parent)
            .setInheritParentVars(false)
            .build();
    child.setVar("any", value);

    assertTrue(parent.getVar("any").isNullOrEmpty());
    assertThat(parent.getVars()).isEmpty();
    Assert.assertEquals(value, child.getVar("any"));
    assertEquals(ImmutableSet.of("any"), child.getVars());
  }

  @Test
  public void setVar_inheritedNonExistingVar_setsOnChild() {
    Data value = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    child.setVar("any", value);

    assertTrue(parent.getVar("any").isNullOrEmpty());
    assertThat(parent.getVars()).isEmpty();
    Assert.assertEquals(value, child.getVar("any"));
    assertEquals(ImmutableSet.of("any"), child.getVars());
  }

  @Test
  public void setVar_inheritedExistingVar_setsOnParent() {
    Data oldVal = mock(Data.class);
    Data newVal = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    parent.setVar("any", oldVal);
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    child.setVar("any", newVal);

    Assert.assertEquals(newVal, parent.getVar("any"));
    assertEquals(ImmutableSet.of("any"), parent.getVars());
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void setVar_inheritedSpecialCaseVar_setsLocally() {
    Data oldVal = mock(Data.class);
    Data newVal = mock(Data.class);
    StackFrame parent = new DefaultStackFrame.DefaultBuilder().build();
    parent.setVar(WhistleFunction.OUTPUT_VAR, oldVal);
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    child.setVar(WhistleFunction.OUTPUT_VAR, newVal);

    Assert.assertEquals(oldVal, parent.getVar(WhistleFunction.OUTPUT_VAR));
    assertEquals(ImmutableSet.of(WhistleFunction.OUTPUT_VAR), parent.getVars());
    Assert.assertEquals(newVal, child.getVar(WhistleFunction.OUTPUT_VAR));
    assertEquals(ImmutableSet.of(WhistleFunction.OUTPUT_VAR), child.getVars());
  }

  @Test
  public void setVar_deeplyInheritedExistingVar_setsOnParent() {
    Data oldVal = mock(Data.class);
    Data newVal = mock(Data.class);
    StackFrame grandparent = new DefaultStackFrame.DefaultBuilder().build();
    grandparent.setVar("any", oldVal);
    StackFrame parent =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(grandparent)
            .setInheritParentVars(true)
            .build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    child.setVar("any", newVal);

    Assert.assertEquals(newVal, grandparent.getVar("any"));
    assertEquals(ImmutableSet.of("any"), grandparent.getVars());
    assertThat(parent.getVars()).isEmpty();
    assertThat(child.getVars()).isEmpty();
  }

  @Test
  public void setVar_deeplyInheritedShadowedExistingVar_setsOnFurthestParent() {
    Data oldVal = mock(Data.class);
    Data newVal = mock(Data.class);
    StackFrame grandparent = new DefaultStackFrame.DefaultBuilder().build();
    grandparent.setVar("any", oldVal);
    StackFrame parent =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(grandparent)
            .setInheritParentVars(true)
            .build();
    parent.setLocalVar("any", oldVal);
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    child.setVar("any", newVal);

    Assert.assertEquals(newVal, grandparent.getVar("any"));
    Assert.assertEquals(oldVal, parent.getVar("any"));
    assertEquals(ImmutableSet.of("any"), grandparent.getVars());
    assertEquals(ImmutableSet.of("any"), parent.getVars());
    assertThat(child.getVars()).isEmpty();
  }

  private static StackFrame generateStackFrame(
      String name, StackFrame parent, boolean inheritParentVars) {
    return new DefaultStackFrame.DefaultBuilder()
        .setName(name)
        .setParent(parent)
        .setInheritParentVars(inheritParentVars)
        .build();
  }

  @Test
  public void equals_onDefaultStackFrame_identity() throws Exception {
    StackFrame parent = generateStackFrame("parentFrame1", null, true);
    StackFrame sf1 = generateStackFrame("testFrame1", parent, true);
    assertEquals(sf1, sf1);
  }

  @Test
  public void equals_onDefaultStackFrame_sameValues() throws Exception {
    StackFrame parent = generateStackFrame("parentFrame1", null, true);
    StackFrame sf1 = generateStackFrame("testFrame1", parent, true);
    StackFrame parent2 = generateStackFrame("parentFrame1", null, true);
    StackFrame sf2 = generateStackFrame("testFrame1", parent2, true);

    assertEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_differentParents() throws Exception {
    StackFrame parent = generateStackFrame("parentFrame1", null, true);
    StackFrame sf1 = generateStackFrame("testFrame1", parent, true);
    StackFrame parent2 = generateStackFrame("parentFrame1", null, false);
    StackFrame sf2 = generateStackFrame("testFrame1", parent2, true);

    Assert.assertNotEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_nullParents() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    StackFrame sf2 = generateStackFrame("testFrame1", null, true);

    assertEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_differentGrandparents() throws Exception {

    StackFrame grandparent = generateStackFrame("grandparentFrame1", null, true);
    StackFrame parent = generateStackFrame("parentFrame1", grandparent, true);
    StackFrame sf1 = generateStackFrame("testFrame1", parent, true);

    StackFrame grandparent2 = generateStackFrame("grandparentFrame1", null, false);
    StackFrame parent2 = generateStackFrame("parentFrame1", grandparent2, true);
    StackFrame sf2 = generateStackFrame("testFrame1", parent2, true);

    Assert.assertNotEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_differentInheritValue() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    StackFrame sf2 = generateStackFrame("testFrame1", null, false);

    Assert.assertNotEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_differentNames() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    StackFrame sf2 = generateStackFrame("testFrame2", null, true);

    Assert.assertNotEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_blankNames() throws Exception {
    StackFrame parent = generateStackFrame("parentFrame1", null, true);
    StackFrame sf1 = generateStackFrame("", parent, true);
    StackFrame parent2 = generateStackFrame("parentFrame1", null, true);
    StackFrame sf2 = generateStackFrame("", parent2, true);

    assertEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_sameVars() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    sf1.setVar("testVar1", testDTI().primitiveOf("TestValue"));
    sf1.setVar("testVar2", testDTI().primitiveOf("TestValue2"));
    StackFrame sf2 = generateStackFrame("testFrame1", null, true);
    sf2.setVar("testVar1", testDTI().primitiveOf("TestValue"));
    sf2.setVar("testVar2", testDTI().primitiveOf("TestValue2"));

    assertEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_differentVars() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    sf1.setVar("testVar1", testDTI().primitiveOf("TestValue"));
    sf1.setVar("testVar2", testDTI().primitiveOf("TestValue2"));
    StackFrame sf2 = generateStackFrame("testFrame1", null, true);
    sf2.setVar("testVar", testDTI().primitiveOf("TestValue"));
    sf2.setVar("testVar2", testDTI().primitiveOf("TestValue2"));

    Assert.assertNotEquals(sf1, sf2);
  }

  @Test
  public void equals_onDefaultStackFrame_nullFrame() throws Exception {
    StackFrame sf1 = generateStackFrame("testFrame1", null, true);
    sf1.setVar("testVar1", testDTI().primitiveOf("TestValue"));
    sf1.setVar("testVar2", testDTI().primitiveOf("TestValue2"));

    Assert.assertNotEquals(null, sf1);
  }

  private void makeStackOverflowSingleStackFrameName() {
    StackFrame lastStackFrame = null;
    for (int i = 0; i < DefaultBuilder.STACK_FRAMES_LIMIT + 1; i++) {
      lastStackFrame = generateStackFrame("testFrame", lastStackFrame, true);
    }
  }

  private void makeStackOverflowMultipleStackFrameName() {
    StackFrame lastStackFrame = null;
    ImmutableMap<String, Integer> counts =
        ImmutableMap.of(
            "frameA",
            DefaultBuilder.STACK_FRAMES_LIMIT / 10,
            "frameB",
            DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 2,
            "frameC",
            DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 3,
            "frameD",
            DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 4 + 1);
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      for (int i = 0; i < entry.getValue(); i++) {
        lastStackFrame = generateStackFrame(entry.getKey(), lastStackFrame, true);
      }
    }
  }

  @Test
  public void build_stackOverflowSingleStackFrameName_throws() throws Exception {
    WhistleStackOverflowError error =
        assertThrows(WhistleStackOverflowError.class, this::makeStackOverflowSingleStackFrameName);
    assertThat(error).hasMessageThat().contains("testFrame: " + DefaultBuilder.STACK_FRAMES_LIMIT);
    assertThat(error).hasMessageThat().contains("<The top of the stack> testFrame");
  }

  @Test
  public void build_stackOverflowMultipleStackFrameName_throwsAndSorts() throws Exception {
    WhistleStackOverflowError error =
        assertThrows(
            WhistleStackOverflowError.class, this::makeStackOverflowMultipleStackFrameName);
    assertThat(error)
        .hasMessageThat()
        .contains("frameD: " + DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 4);
    assertThat(error)
        .hasMessageThat()
        .contains("frameC: " + DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 3);
    assertThat(error)
        .hasMessageThat()
        .contains("frameB: " + DefaultBuilder.STACK_FRAMES_LIMIT / 10 * 2);
    assertThat(error)
        .hasMessageThat()
        .contains("frameA: " + DefaultBuilder.STACK_FRAMES_LIMIT / 10);
    assertThat(error)
        .hasMessageThat()
        .matches(Pattern.compile(".*frameD.*frameC.*frameB.*frameA.*", Pattern.DOTALL));
  }

  @Test
  public void build_stack_valid() throws Exception {
    StackFrame lastStackFrame = null;
    lastStackFrame = generateStackFrame("testFrame1", lastStackFrame, true);
    lastStackFrame = generateStackFrame("testFrame2", lastStackFrame, true);
    lastStackFrame = generateStackFrame("testFrame3", lastStackFrame, true);
    assertNotNull(lastStackFrame);
  }
}
