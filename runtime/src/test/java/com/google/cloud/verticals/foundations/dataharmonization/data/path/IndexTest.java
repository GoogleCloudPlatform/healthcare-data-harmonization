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
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyContainer;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mutableArrayOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.Mockito;

/** Tests for Index. */
@RunWith(JUnit4.class)
public class IndexTest {
  @Test
  public void get_onContainer_callsContainerGet() {
    Array array = emptyArray();
    new Index(123).get(array);
    verify(array).getElement(123);
  }

  @Test
  public void get_onContainer_throws() {
    Container container = emptyContainer();

    assertThrows(UnsupportedOperationException.class, () -> new Index(123).get(container));
  }

  @Test
  public void get_nullIndex_returnsNull() {
    Array array = emptyArray();
    Data result = new Index(null).get(array);
    assertTrue(result.isNullOrEmpty());
  }

  @Test
  public void set_onArray_callsArraySet() {
    Array array = emptyArray();
    Data value = mock(Data.class);
    array = new Index(123).set(array, value).asArray();
    verify(array).setElement(123, value);
  }

  @Test
  public void set_nullIndex_appends() {
    Array array = emptyArray();
    Data value = mock(Data.class);
    array = new Index(null).set(array, value).asArray();
    verify(array).setElement(0, value);

    when(array.size()).thenReturn(100);
    array = new Index(null).set(array, value).asArray();
    verify(array).setElement(100, value);
  }

  @Test
  public void set_nullIndex_concat() {
    Data value = mock(Data.class);
    Array array = mutableArrayOf(value);
    Array array2 = mutableArrayOf(value, value);

    array = new Index(null).set(array, array2).asArray();
    InOrder inOrder = Mockito.inOrder(array);
    inOrder.verify(array).setElement(1, value);
    inOrder.verify(array).setElement(2, value);
    assertEquals(3, array.size());
  }

  @Test
  public void set_nullIndexSelf_deepCopyConcat() {
    Data value = mock(Data.class);
    Array array = mutableArrayOf(value);
    Array copy = mutableArrayOf(value);
    when(array.deepCopy()).thenReturn(copy);

    Array array2 = array;

    array = new Index(null).set(array, array2).asArray();
    InOrder inOrder = Mockito.inOrder(array);
    inOrder.verify(array).setElement(1, value);
    assertEquals(2, array.size());
  }

  @Test
  public void set_onContainer_throws() {
    Container container = emptyContainer();
    Data value = mock(Data.class);
    assertThrows(UnsupportedOperationException.class, () -> new Index(123).set(container, value));
  }

  @Test
  public void create_returnsEmptyArray() {
    Data created = new Index(0).create(testDTI());
    assertTrue(created.isArray());
    assertTrue(created.isNullOrEmpty());
  }

  @Test
  public void isField_false() {
    assertFalse(new Index(0).isField());
  }

  @Test
  public void isIndex_true() {
    assertTrue(new Index(0).isIndex());
  }

  @Test
  public void toString_emptyIndex() {
    Index emptyIndex = new Index(null);

    assertEquals("[]", emptyIndex.toString());
  }

  @Test
  public void toString_nonEmpty() {
    Index index1 = new Index(0);
    Index index2 = new Index(123456789);
    Index index3 = new Index(123);

    assertEquals("[0]", index1.toString());
    assertEquals("[123456789]", index2.toString());
    assertEquals("[123]", index3.toString());
  }
}
