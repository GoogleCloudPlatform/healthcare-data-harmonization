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
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.nul;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for append merge strategy. */
@RunWith(JUnit4.class)
public class AppendMergeStrategyTest {

  @Test
  public void append_null_appends() {
    Array array = mutableArrayOf(arbitrary(), arbitrary());
    Data current = containerOf(array);

    Data inbound = nul();
    Path path = Path.parse("a");

    Data result = new AppendMergeStrategy().merge(testDTI(), current, inbound, path);
    verify(array).setElement(2, inbound);
    assertThat(result).isEqualTo(current);
    assertThat(result.asContainer().getField("a").asArray().getElement(2).isNullOrEmpty()).isTrue();
  }

  @Test
  public void append_data_appends() {
    Array array = mutableArrayOf(arbitrary(), arbitrary());
    Data current = containerOf(array);

    Data inbound = arbitrary();
    Path path = Path.parse("a");

    Data result = new AppendMergeStrategy().merge(testDTI(), current, inbound, path);
    verify(array).setElement(2, inbound);
    assertThat(result).isEqualTo(current);
  }

  @Test
  public void append_array_appendsAsNested() {
    Array array = mutableArrayOf(arbitrary(), arbitrary());
    Data current = containerOf(array);

    Data inbound = arrayOf(arbitrary(), 10);
    Path path = Path.parse("a");

    Data result = new AppendMergeStrategy().merge(testDTI(), current, inbound, path);
    verify(array).setElement(2, inbound);
    assertThat(result).isEqualTo(current);
    assertThat(array.size()).isEqualTo(3);
    assertThat(array.getElement(2)).isEqualTo(inbound);
    assertThat(result.asContainer().getField("a").asArray().getElement(2)).isEqualTo(inbound);
  }

  @Test
  public void append_dataEmptyPath_appends() {
    Array array = mutableArrayOf(arbitrary(), arbitrary());

    Data inbound = arbitrary();
    Path path = Path.empty();

    Data result = new AppendMergeStrategy().merge(testDTI(), array, inbound, path);
    verify(array).setElement(2, inbound);
    assertThat(result).isEqualTo(array);
  }

  @Test
  public void append_nonArrayAndArray_throws() {
    Data nonArray = arbitrary();

    Data inbound = arrayOf(arbitrary(), 10);
    Path path = Path.empty();

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> new AppendMergeStrategy().merge(testDTI(), nonArray, inbound, path));
    assertThat(ex).hasMessageThat().contains("Cannot append to Data$MockitoMock");
  }
}
