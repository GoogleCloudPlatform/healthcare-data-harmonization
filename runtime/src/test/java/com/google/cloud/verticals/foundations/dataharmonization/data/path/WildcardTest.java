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

package com.google.cloud.verticals.foundations.dataharmonization.data.path;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockDataset;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Wildcard. */
@RunWith(JUnit4.class)
public class WildcardTest {

  @Test
  public void get_onNull_returnsEmpty() {
    Data got = new Wildcard(false).get(NullData.instance);
    assertTrue(got.isArray());
    assertEquals(0, got.asArray().size());
  }

  @Test
  public void get_onEmpty_returnsEmpty() {
    Data got = new Wildcard(false).get(emptyArray());
    assertTrue(got.isArray());
    assertEquals(0, got.asArray().size());
  }

  @Test
  public void get_onNonCollection_throws() {
    Exception got =
        assertThrows(
            UnsupportedOperationException.class,
            () -> new Wildcard(false).get(containerOf(mock(Data.class))));
    assertThat(got.getMessage())
        .contains("Wildcard can only be used on transparent collections (but was applied to");
  }

  /** Test [*] on [[...], [...], [...]] with flattening */
  @Test
  public void get_onArrayOfArraysFlattened_returnsField() {
    Data fake = mock(Data.class);

    // data = [[fake, fake, ... (10x)], [fake, fake, ... (10x)], ... (10x)]
    Array data = arrayOf(arrayOf(fake, 10), 10);
    Data got = new Wildcard(true).get(data);
    assertTrue(got.isArray());
    assertArrayEquals(
        // want = [fake, fake, ... (100x)]
        arrayOf(fake, 100).stream().toArray(Data[]::new),
        got.asArray().stream().toArray(Data[]::new));
  }

  /** Test [*].field on [{...}, {...}, {...}] */
  @Test
  public void get_onDatasetOfContainers_returnsField() {
    Data fake = mock(Data.class);
    Array backing = arrayOf(containerOf(fake), 10);

    // data =  dataset like [{field: fake}, {field: fake}, ...]
    Dataset data = new MockDataset(backing);
    Data got = new Wildcard(false).get(data, Path.parse("field"));
    assertTrue(got.isDataset());

    // Copy out of the dataset
    Data[] gotArr = ((MockDataset) got).getBackingArray().stream().toArray(Data[]::new);

    // want = [fake, ...]
    assertArrayEquals(arrayOf(fake, 10).stream().toArray(Data[]::new), gotArr);
  }

  @Test
  public void set_throws() {
    assertThrows(
        UnsupportedOperationException.class,
        () -> new Wildcard(false).set(emptyArray(), emptyArray()));
  }

  @Test
  public void create() {
    assertTrue(new Wildcard(false).create(testDTI()).isArray());
  }

  @Test
  public void isIndex() {
    assertTrue(new Wildcard(false).isIndex());
  }

  @Test
  public void isField() {
    assertFalse(new Wildcard(false).isField());
  }

  @Test
  public void toString_any() {
    assertEquals("[*]", new Wildcard(false).toString());
  }
}
