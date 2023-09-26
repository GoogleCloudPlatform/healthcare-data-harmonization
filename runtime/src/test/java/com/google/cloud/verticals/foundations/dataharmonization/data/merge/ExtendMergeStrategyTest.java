/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.data.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arbitrary;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyContainer;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Extend Merge Strategy */
@RunWith(JUnit4.class)
public class ExtendMergeStrategyTest {

  @Test
  public void arrays_emptyWithNonEmpty_concat() {
    Array current = mutableArrayOf();
    Array inbound = arrayOf(arbitrary(), arbitrary());

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isArray()).isTrue();
    assertThat(result.asArray().size()).isEqualTo(2);
  }

  @Test
  public void arrays_nonEmptyWithEmpty_concat() {
    Array current = mutableArrayOf(arbitrary(), arbitrary());
    Array inbound = emptyArray();

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isArray()).isTrue();
    assertThat(result.asArray().size()).isEqualTo(2);
  }

  @Test
  public void arrays_nonEmptyWithNonEmpty_concat() {
    final Data[] items = new Data[] {arbitrary(), arbitrary(), arbitrary(), arbitrary()};

    Array current = mutableArrayOf(items[0], items[1]);
    Array inbound = arrayOf(items[2], items[3]);

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isArray()).isTrue();
    assertThat(result.asArray().size()).isEqualTo(4);
    for (int i = 0; i < 4; i++) {
      assertThat(result.asArray().getElement(i)).isSameInstanceAs(items[i]);
    }
  }

  @Test
  public void containers_emptyWithNonEmpty_concat() {
    Data item = arbitrary();

    Container current = testDTI().containerOf(ImmutableMap.of());
    Container inbound = containerOf("hello", item);

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isContainer()).isTrue();
    assertThat(result.asContainer().fields()).containsExactly("hello");
    assertThat(result.asContainer().getField("hello")).isSameInstanceAs(item);
  }

  @Test
  public void containers_nonEmptyWithEmpty_concat() {
    Data item = arbitrary();

    Container current = testDTI().containerOf(ImmutableMap.of("hello", item));
    Container inbound = emptyContainer();

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isContainer()).isTrue();
    assertThat(result.asContainer().fields()).containsExactly("hello");
    assertThat(result.asContainer().getField("hello")).isSameInstanceAs(item);
  }

  @Test
  public void containers_nonEmptyWithNonEmpty_concat() {
    final Data[] items = new Data[] {arbitrary(), arbitrary()};

    Container current = testDTI().containerOf(ImmutableMap.of("hello", items[0]));
    Container inbound = containerOf("world", items[1]);

    Data result = new ExtendMergeStrategy().merge(testDTI(), current, inbound, Path.empty());
    assertThat(result.isContainer()).isTrue();
    assertThat(result.asContainer().fields()).containsExactly("hello", "world");
    assertThat(result.asContainer().getField("hello")).isSameInstanceAs(items[0]);
    assertThat(result.asContainer().getField("world")).isSameInstanceAs(items[1]);
  }

  @Test
  public void containers_nonEmptyWithNonEmptyAtPath_concat() {
    final Data[] items = new Data[] {arbitrary(), arbitrary()};

    Container current = testDTI().containerOf(ImmutableMap.of("hello", items[0]));
    Container inbound = containerOf("world", items[1]);

    Array wrapper = arrayOf(current);

    Data result = new ExtendMergeStrategy().merge(testDTI(), wrapper, inbound, Path.parse("[0]"));
    assertThat(result.isArray()).isTrue();
    assertThat(result.asArray().getElement(0).isContainer()).isTrue();

    result = result.asArray().getElement(0);
    assertThat(result.asContainer().fields()).containsExactly("hello", "world");
    assertThat(result.asContainer().getField("hello")).isSameInstanceAs(items[0]);
    assertThat(result.asContainer().getField("world")).isSameInstanceAs(items[1]);
  }

  @Test
  public void mismatchedTypes_arrayContainer() {
    Container container = containerOf(arbitrary());
    Array array = arrayOf(arbitrary());

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExtendMergeStrategy().merge(testDTI(), container, array, Path.empty()));
    assertThat(ex).hasMessageThat().contains("cannot be extended");

    ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExtendMergeStrategy().merge(testDTI(), array, container, Path.empty()));
    assertThat(ex).hasMessageThat().contains("cannot be extended");
  }

  @Test
  public void mismatchedTypes_primitives() {
    Array array = arrayOf(arbitrary());
    Primitive prim = testDTI().primitiveOf(123.);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExtendMergeStrategy().merge(testDTI(), array, prim, Path.empty()));
    assertThat(ex).hasMessageThat().contains("cannot be extended");

    ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExtendMergeStrategy().merge(testDTI(), prim, array, Path.empty()));
    assertThat(ex).hasMessageThat().contains("not applicable");
  }
}
