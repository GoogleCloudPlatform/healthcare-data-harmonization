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
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyContainer;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableContainerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.mockRuntimeContextWithDefaultMetaData;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.mockVarCapableRuntimeContext;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.MergeModeExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Option;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for MappingTarget. TODO(): Move the merge tests out to a separate test file. */
@RunWith(JUnit4.class)
public class VarTargetTest {

  @Test
  public void write_noMerge_overwrites() {
    Data existing = mock(Data.class);
    Map<String, Data> vars = newHashMap("existing", existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget("existing", Path.empty());

    Data inbound = mock(Data.class);
    target.write(context, inbound);

    assertThat(vars).containsEntry("existing", inbound);
  }

  @Test
  public void write_mergeNullWithNotNull_overwrites() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data inbound = mock(Data.class);
    target.write(context, inbound);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, inbound);
  }

  @Test
  public void write_mergeImmutableWithContainer_overwrites() {
    Map<String, Data> vars =
        newHashMap(WhistleFunction.OUTPUT_VAR, containerOf("immutable", mock(Data.class)));

    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data inbound = containerOf("field", mock(Data.class));
    assertThrows(UnsupportedOperationException.class, () -> target.write(context, inbound));
  }

  @Test
  public void write_mergeNullWithContainer_overwrites() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data inbound = containerOf("field", mock(Data.class));
    target.write(context, inbound);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, inbound);
  }

  @Test
  public void write_mergeNullWithArray_overwrites() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data inbound = arrayOf(mock(Data.class));
    target.write(context, inbound);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, inbound);
  }

  @Test
  public void write_mergePrimitiveWithNull_skips() {
    Data existing = mock(Primitive.class);
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, NullData.instance);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeUserVarPrimitiveWithNull_overwrites() {
    Data existing = mock(Primitive.class);
    Map<String, Data> vars = newHashMap("user", existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget("user", Path.empty());

    target.write(context, NullData.instance);

    assertThat(vars).containsEntry("user", NullData.instance);
  }

  @Test
  public void write_mergeOutputPrimitiveWithNull_noop() {
    Primitive prim = mock(Primitive.class);
    Data existing = mutableContainerOf(i -> i.set("existing", prim));
    ImmutableMap<String, Data> vars = ImmutableMap.of(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.parse("existing"));

    target.write(context, NullData.instance);

    assertDCAPEquals(
        mutableContainerOf(i -> i.set("existing", prim)), vars.get(WhistleFunction.OUTPUT_VAR));
  }

  @Test
  public void write_mergeContainerWithNull_noop() {
    // Make a non-empty container
    Data existing = mutableContainerOf(init -> init.set("foo", mock(Data.class)));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, NullData.instance);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeContainerWithEmptyContainer_noop() {
    // Make a non-empty container
    Data existing = mutableContainerOf(init -> init.set("foo", mock(Data.class)));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, emptyContainer());

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeContainerWithEmptyArray_noop() {
    // Make a non-empty container
    Data existing = mutableContainerOf(init -> init.set("foo", mock(Data.class)));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, arrayOf());

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeContainers_mergesKeys() {
    Data one = mock(Data.class);
    Data existing = mutableContainerOf(init -> init.set("one", one));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data two = mock(Data.class);
    Data inbound = containerOf("two", two);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("one", "two"), actual.fields());
    assertEquals(one, actual.getField("one"));
    assertEquals(two, actual.getField("two"));
  }

  @Test
  public void write_mergeRecursiveContainers_mergesKeysRecursively() {
    Data grandchild = mock(Data.class);
    Data child = mutableContainerOf(init -> init.set("one", grandchild));
    Data existing = mutableContainerOf(init -> init.set("child", child));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data grandchild2 = mock(Data.class);
    Data child2 = containerOf("two", grandchild2);
    Data inbound = containerOf("child", child2);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("child"), actual.fields());

    assertTrue(actual.getField("child").isContainer());
    Container actualChild = actual.getField("child").asContainer();
    assertEquals(ImmutableSet.of("one", "two"), actualChild.fields());
    assertEquals(grandchild, actualChild.getField("one"));
    assertEquals(grandchild2, actualChild.getField("two"));
  }

  @Test
  public void write_mergeContainersWithConflictingKeys_overwrites() {
    Data one = spy(Primitive.class);
    Data existing = mutableContainerOf(init -> init.set("one", one));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data two = mock(Data.class);
    Data inbound = containerOf("one", two);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("one"), actual.fields());
    assertEquals(two, actual.getField("one"));
  }

  @Test
  public void write_mergeArrayWithNull_noop() {
    // Make a non-empty array
    Data existing = mutableArrayOf(mock(Data.class));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, NullData.instance);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeArrayWithEmptyContainer_noop() {
    // Make a non-empty array
    Data existing = mutableArrayOf(mock(Data.class));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, emptyContainer());

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeArrayWithEmptyArray_noop() {
    // Make a non-empty array
    Data existing = mutableArrayOf(mock(Data.class));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    target.write(context, arrayOf());

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void write_mergeArrayWithPrim_overwrites() {
    // Make a non-empty array
    Data existing = mutableArrayOf(mock(Data.class));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data incoming = mock(Primitive.class);

    target.write(context, incoming);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, incoming);
  }

  @Test
  public void write_mergeArrayWithArray_concats() {
    // Make a non-empty array
    Data one = mock(Data.class);
    Data existing = mutableArrayOf(one);
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data two = mock(Data.class);
    Data incoming = arrayOf(two);

    target.write(context, incoming);

    assertEquals(2, vars.get(WhistleFunction.OUTPUT_VAR).asArray().size());
    assertEquals(one, vars.get(WhistleFunction.OUTPUT_VAR).asArray().getElement(0));
    assertEquals(two, vars.get(WhistleFunction.OUTPUT_VAR).asArray().getElement(1));
  }

  @Test
  public void var_emptyPath_overwrites() {
    Data existing = mock(Data.class);
    Map<String, Data> vars = newHashMap("hello", existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget("hello", Path.empty());

    Data incoming = mock(Data.class);

    target.write(context, incoming);

    assertThat(vars).containsEntry("hello", incoming);
  }

  @Test
  public void var_nonEmptyPath_merges() {
    Data world = mock(Data.class);

    Container existing = mutableContainerOf(s -> s.set("world", world));
    Map<String, Data> vars = newHashMap("hello", existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget("hello", Path.parse("world2"));

    Data world2 = mock(Data.class);

    target.write(context, world2);

    assertThat(vars).containsEntry("hello", existing);
    assertThat(existing.getField("world")).isEqualTo(world);
    assertThat(existing.getField("world2")).isEqualTo(world2);
  }

  @Test
  public void this_emptyPath_merges() {
    Data world = mock(Data.class);

    Container existing = mutableContainerOf(s -> s.set("world", world));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.empty());

    Data world2 = mock(Data.class);
    Data incoming = mutableContainerOf(s -> s.set("world2", world2));
    ;

    target.write(context, incoming);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
    assertThat(existing.getField("world")).isEqualTo(world);
    assertThat(existing.getField("world2")).isEqualTo(world2);
  }

  @Test
  public void this_nonEmptyPath_merges() {
    Data world = mock(Data.class);

    Container existing = mutableContainerOf(s -> s.set("world", world));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    MappingTarget target = new VarTarget(WhistleFunction.OUTPUT_VAR, Path.parse("world2"));

    Data world2 = mock(Data.class);

    target.write(context, world2);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
    assertThat(existing.getField("world")).isEqualTo(world);
    assertThat(existing.getField("world2")).isEqualTo(world2);
  }

  @Test
  public void construct_invalidNumArgs_tooMany() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new VarTarget.Constructor()
                    .construct(
                        context,
                        testDTI().primitiveOf("a"),
                        testDTI().primitiveOf("b"),
                        testDTI().primitiveOf("c"),
                        testDTI().primitiveOf("some.extra.path")));
    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Must provide one to three string arguments to set (provided 4) that specifies the"
                + " variable to write to");
  }

  @Test
  public void construct_invalidNumArgs_none() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> new VarTarget.Constructor().construct(context)); // no path string provided
    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Must provide one to three string arguments to set (provided 0) that specifies the"
                + " variable to write to");
  }

  @Test
  public void construct_invalidArgs_nonString() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    IllegalArgumentException thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new VarTarget.Constructor()
                    .construct(
                        context, testDTI().primitiveOf(1.2), testDTI().primitiveOf("valid")));
    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Must provide one to three string arguments to set (provided 2) that specifies the"
                + " variable to write to");

    thrown =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new VarTarget.Constructor()
                    .construct(
                        context, testDTI().containerOf(vars), testDTI().primitiveOf("valid")));
    assertThat(thrown)
        .hasMessageThat()
        .contains(
            "Must provide one to three string arguments to set (provided 2) that specifies the"
                + " variable to write to");
  }

  @Test
  public void construct_write_mergeNullWithContainer_overwrites() {
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, NullData.instance);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Target target = new VarTarget.Constructor().construct(context, testDTI().primitiveOf(""));

    Data inbound = containerOf("field", mock(Data.class));
    target.write(context, inbound);

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, inbound);
  }

  @Test
  public void construct_write_mergeContainerWithEmptyContainer_noop() {
    // Make a non-empty container
    Data existing = mutableContainerOf(init -> init.set("foo", mock(Data.class)));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Target target = new VarTarget.Constructor().construct(context, testDTI().primitiveOf(""));

    target.write(context, emptyContainer());

    assertThat(vars).containsEntry(WhistleFunction.OUTPUT_VAR, existing);
  }

  @Test
  public void construct_write_mergeContainers_mergesKeys() {
    Data one = mock(Data.class);
    Data existing = mutableContainerOf(init -> init.set("one", one));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Target target = new VarTarget.Constructor().construct(context, testDTI().primitiveOf(""));

    Data two = mock(Data.class);
    Data inbound = containerOf("two", two);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("one", "two"), actual.fields());
    assertEquals(one, actual.getField("one"));
    assertEquals(two, actual.getField("two"));
  }

  @Test
  public void construct_write_mergeContainersWithConflictingKeys_overwrites() {
    Data one = spy(Primitive.class);
    Data existing = mutableContainerOf(init -> init.set("one", one));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Target target = new VarTarget.Constructor().construct(context, testDTI().primitiveOf(""));

    Data two = spy(Container.class);
    Data inbound = containerOf("one", two);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("one"), actual.fields());
    assertEquals(two, actual.getField("one"));
  }

  @Test
  public void construct_write_mergeContainersExplicitReplace_replaces() {
    Data one = mock(Data.class);
    Data existing = mutableContainerOf(init -> init.set("one", one));
    Map<String, Data> vars = newHashMap(WhistleFunction.OUTPUT_VAR, existing);
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    context = new MergeModeExperiment().enable(context, Option.getDefaultInstance());
    Target target =
        new VarTarget.Constructor()
            .construct(
                context,
                testDTI().primitiveOf(WhistleFunction.OUTPUT_VAR),
                testDTI().primitiveOf(""),
                testDTI().primitiveOf("replace"));

    Data two = mock(Data.class);
    Data inbound = containerOf("two", two);
    target.write(context, inbound);

    assertTrue(vars.get(WhistleFunction.OUTPUT_VAR).isContainer());
    Container actual = vars.get(WhistleFunction.OUTPUT_VAR).asContainer();
    assertEquals(ImmutableSet.of("two"), actual.fields());
    assertEquals(two, actual.getField("two"));
  }

  @Test
  public void construct_explicitMergeModeWithoutOption_errors() {
    UnsupportedOperationException ex =
        assertThrows(
            UnsupportedOperationException.class,
            () ->
                new VarTarget.Constructor()
                    .construct(
                        mockRuntimeContextWithDefaultMetaData(),
                        testDTI().primitiveOf(WhistleFunction.OUTPUT_VAR),
                        testDTI().primitiveOf(""),
                        testDTI().primitiveOf("replace")));
    assertThat(ex).hasMessageThat().contains(new MergeModeExperiment().getName());
  }

  private static <K, V> HashMap<K, V> newHashMap(K key, V value) {
    HashMap<K, V> map = new HashMap<>();
    map.put(key, value);
    return map;
  }
}
