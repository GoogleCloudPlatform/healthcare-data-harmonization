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
package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.google.cloud.verticals.foundations.dataharmonization.error.UndeclaredVariableException;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Environment Class. */
@RunWith(JUnit4.class)
public class EnvironmentTest {

  @Test
  public void testInitEnv() {
    Environment env = new Environment("testRoot");
    assertTrue(env.isRoot());
    Environment child = env.createChild("child1");
    assertFalse(child.isRoot());
  }

  @Test
  public void testDeclareVariable() {
    Environment env = new Environment("testEnv");
    env.declareOrInheritVariable("foo");
    assertThat(env.getLocalVars()).contains("foo");
    assertThat(env.getLocalVars()).hasSize(1);
  }

  @Test
  public void testDeclareVariable_multipleTimes() {
    Environment env = new Environment("testEnv");
    env.declareOrInheritVariable("foo");
    env.declareOrInheritVariable("foo");
    assertThat(env.getLocalVars()).contains("foo");
    assertThat(env.getLocalVars()).hasSize(1);
  }

  @Test
  public void testDeclareVariable_nullName() {
    Environment env = new Environment("testEnv");

    assertThrows(IllegalArgumentException.class, () -> env.declareOrInheritVariable(null));
  }

  @Test
  public void testDeclareVariable_emptyStr() {
    Environment env = new Environment("testEnv");

    assertThrows(IllegalArgumentException.class, () -> env.declareOrInheritVariable(""));
  }

  @Test
  public void testDeclareVariables_emptyList() {
    Environment env = new Environment("testEnv");
    env.declareLocalVariables(Arrays.asList());
    assertThat(env.getLocalVars()).isEmpty();
  }

  @Test
  public void testDeclareVariables_oneElementList() {
    Environment env = new Environment("testEnv");
    env.declareLocalVariables(Arrays.asList("foo"));
    assertThat(env.getLocalVars()).hasSize(1);
    assertTrue(env.hasVarInScope("foo"));
  }

  @Test
  public void testDeclareVariables_listWithDuplicates() {
    Environment env = new Environment("testEnv");
    env.declareLocalVariables(Arrays.asList("foo", "foo", "bar", "baz"));
    assertThat(env.getLocalVars()).hasSize(3);
    assertTrue(env.hasVarInScope("foo"));
    assertTrue(env.hasVarInScope("bar"));
    assertTrue(env.hasVarInScope("baz"));
  }

  @Test
  public void testDeclareVariables_listWithNull() {
    Environment env = new Environment("testEnv");
    ArrayList<String> lst = new ArrayList<>();
    lst.add("foo");
    lst.add("bar");
    lst.add(null);
    assertThrows(IllegalArgumentException.class, () -> env.declareLocalVariables(lst));
  }

  @Test
  public void testDeclareVariables_listWithEmptyStr() {
    Environment env = new Environment("testEnv");

    assertThrows(
        IllegalArgumentException.class,
        () -> env.declareLocalVariables(Arrays.asList("foo", "foo", "", "baz")));
  }

  @Test
  public void testDeclareVariables_envWithVarNewVarUnique() {
    Environment env =
        new Environment(
            "testEnv",
            false,
            null,
            Arrays.asList("foo", "bar"),
            ImmutableList.of(),
            ImmutableList.of());
    env.declareLocalVariables(Arrays.asList("baz"));
    assertThat(env.getLocalVars()).hasSize(3);
    assertTrue(env.hasVarInScope("foo"));
    assertTrue(env.hasVarInScope("bar"));
    assertTrue(env.hasVarInScope("baz"));
  }

  @Test
  public void testDeclareVariables_envWithVarNewVarDuplicate() {
    Environment env =
        new Environment(
            "testEnv",
            false,
            null,
            Arrays.asList("foo", "bar"),
            ImmutableList.of(),
            ImmutableList.of());
    env.declareLocalVariables(Arrays.asList("foo"));
    assertThat(env.getLocalVars()).hasSize(2);
    assertTrue(env.hasVarInScope("foo"));
    assertTrue(env.hasVarInScope("bar"));
  }

  @Test
  public void testDeclareVariables() {
    Environment env = new Environment("testEnv");
    env.declareLocalVariables(Arrays.asList("foo", "bar", "baz"));
    assertThat(env.getLocalVars()).hasSize(3);
    assertTrue(env.hasVarInScope("foo"));
    assertTrue(env.hasVarInScope("bar"));
    assertTrue(env.hasVarInScope("baz"));
  }

  @Test
  public void testReadVar_existLocal() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild1");
    child.declareOrInheritVariable("foo");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
  }

  @Test
  public void testReadVar_existInParent() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild1");
    env.declareOrInheritVariable("foo");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertThat(child.getVarFromParents()).contains("foo");
    assertThat(child.getLocalVars()).doesNotContain("foo");
  }

  @Test
  public void testReadVar_existInParentBeforeCreation() {
    Environment env = new Environment("testRoot");
    env.declareOrInheritVariable("foo");
    Environment child = env.createChild("testChild1");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertThat(child.getVarFromParents()).contains("foo");
    assertThat(child.getLocalVars()).doesNotContain("foo");
  }

  @Test
  public void testReadVar_existInGrandParent() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild");
    Environment grandchild = child.createChild("testGrandchild");
    env.declareOrInheritVariable("foo");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), grandchild.readVar("foo"));
    assertThat(grandchild.getVarFromParents()).contains("foo");
    assertThat(grandchild.getLocalVars()).doesNotContain("foo");
  }

  @Test
  public void testReadVar_existInChild() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild1");
    child.declareOrInheritVariable("foo");
    assertThrows(UndeclaredVariableException.class, () -> env.readVar("foo"));
  }

  @Test
  public void testReadVar_existInSibling() {
    Environment env = new Environment("testRoot");
    Environment child1 = env.createChild("testChild1");
    Environment child2 = env.createChild("testChild2");
    child2.declareOrInheritVariable("foo");
    assertThrows(UndeclaredVariableException.class, () -> child1.readVar("foo"));
  }

  @Test
  public void testReadVar_sameVarDeclaredFirstParent() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild");
    env.declareOrInheritVariable("foo");
    child.declareOrInheritVariable("foo");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), env.readVar("foo"));
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertThat(child.getLocalVars()).containsExactly("$this");
    assertThat(child.getVarFromParents()).hasSize(1);
    assertThat(env.getLocalVars()).hasSize(1);
  }

  @Test
  public void testReadVar_sameVarDeclaredFirstChild() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild");
    child.declareOrInheritVariable("foo");
    env.declareOrInheritVariable("foo");
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), env.readVar("foo"));
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertThat(child.getLocalVars()).hasSize(2);
    assertThat(child.getVarFromParents()).isEmpty();
    assertThat(env.getLocalVars()).hasSize(1);
  }

  @Test
  public void testHasLocalVar() {
    Environment env = new Environment("testEnv");
    env.declareOrInheritVariable("foo");
    assertTrue(env.hasVarInScope("foo"));
  }

  @Test
  public void testHasLocalVarAfterCreation() {
    Environment env = new Environment("testRoot");
    Environment child = env.createChild("testChild");
    env.declareOrInheritVariable("foo");
    assertFalse(child.hasVarInScope("foo"));
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertTrue(child.hasVarInScope("foo"));
  }

  @Test
  public void testHasLocalVarBeforeCreation() {
    Environment env = new Environment("testRoot");
    env.declareOrInheritVariable("foo");
    Environment child = env.createChild("testChild");
    assertFalse(child.hasVarInScope("foo"));
    assertEquals(ValueSource.newBuilder().setFromLocal("foo").build(), child.readVar("foo"));
    assertTrue(child.hasVarInScope("foo"));
  }
}
