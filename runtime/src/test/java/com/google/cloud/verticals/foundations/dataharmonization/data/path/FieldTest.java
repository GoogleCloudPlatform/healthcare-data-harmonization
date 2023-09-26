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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Field. */
@RunWith(JUnit4.class)
public class FieldTest {

  @Test
  public void get_onContainer_callsContainerGet() {
    Container container = emptyContainer();
    new Field("foo").get(container);
    verify(container).getField("foo");
  }

  @Test
  public void get_onArray_throws() {
    Array array = emptyArray();
    assertThrows(UnsupportedOperationException.class, () -> new Field("foo").get(array));
  }

  @Test
  public void set_onContainer_callsContainerSet() {
    Container container = emptyContainer();
    Data value = mock(Data.class);
    container = new Field("foo").set(container, value).asContainer();
    verify(container).setField("foo", value);
  }

  @Test
  public void set_onArray_throws() {
    Array array = emptyArray();
    Data value = mock(Data.class);
    assertThrows(UnsupportedOperationException.class, () -> new Field("foo").set(array, value));
  }

  @Test
  public void create_returnsEmptyContainer() {
    Data created = new Field("").create(testDTI());
    assertTrue(created.isContainer());
    assertTrue(created.isNullOrEmpty());
  }

  @Test
  public void toString_emptyField() {
    Field emptyField = new Field("");

    assertThat(emptyField.toString()).isEmpty();
  }

  @Test
  public void toString_nonEmpty() {
    String field1 = "field1";
    String field2 = "2";
    String field3 = "?!@#";

    assertEquals(field1, new Field(field1).toString());
    assertEquals(field2, new Field(field2).toString());
    assertEquals(field3, new Field(field3).toString());
  }

  @Test
  public void isIndex_false() {
    assertFalse(new Field("").isIndex());
  }

  @Test
  public void isField_true() {
    assertTrue(new Field("").isField());
  }
}
