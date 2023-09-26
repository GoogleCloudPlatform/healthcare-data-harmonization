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

package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.stream.IntStream;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultArray. */
@RunWith(JUnit4.class)
public class DefaultArrayTest {

  @Test
  public void isArray_returnsTrue() {
    assertTrue(new DefaultArray().isArray());
  }

  @Test
  public void isNullOrEmpty_emptyArray_returnsTrue() {
    assertTrue(new DefaultArray().isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_presetArray_returnsTrue() {
    assertFalse(new DefaultArray(ImmutableList.of(new DefaultPrimitive(5.0))).isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_setArray_returnsFalse() {
    Array notEmpty = new DefaultArray();
    notEmpty = notEmpty.setElement(0, new DefaultPrimitive(true));
    assertFalse(notEmpty.isNullOrEmpty());
  }

  @Test
  public void getElement_existingElement_returnsIt() {
    Array array = new DefaultArray(ImmutableList.of(new DefaultPrimitive(5.0)));
    assertEquals(5.0, array.getElement(0).asPrimitive().num(), 0);
  }

  @Test
  public void getElement_nonExistingElement_returnsNullValue() {
    Array array = new DefaultArray();
    // Make sure it is not java null, i.e. no NPEs
    assertNotNull(array.getElement(123));
    assertTrue(array.getElement(123).isNullOrEmpty());
  }

  @Test
  public void setElement_emptyIndex_setsElement() {
    Array array = new DefaultArray();
    array = array.setElement(0, new DefaultPrimitive(5.0));
    assertEquals(5.0, array.getElement(0).asPrimitive().num(), 0);
  }

  @Test
  public void setElement_previousIndex_overwrites() {
    Array array = new DefaultArray();
    array = array.setElement(0, new DefaultPrimitive(1.0)).setElement(0, new DefaultPrimitive(5.0));
    assertEquals(5.0, array.getElement(0).asPrimitive().num(), 0);
  }

  @Test
  public void setElement_outOfRangeIndex_fillsNulls() {
    Array array = new DefaultArray();
    array = array.setElement(123, new DefaultPrimitive(5.0));
    Array finalArray = array;
    IntStream.range(0, 123).forEach(i -> assertTrue(finalArray.getElement(i).isNullOrEmpty()));
    assertEquals(5.0, array.getElement(123).asPrimitive().num(), 0);
  }

  @Test
  public void size_presetArray_keepsSize() {
    Array array = new DefaultArray(ImmutableList.of(new DefaultPrimitive(5.0)));
    assertEquals(1, array.size());
  }

  @Test
  public void size_nullPadding_countsInSize() {
    Array array = new DefaultArray(ImmutableList.of(new DefaultPrimitive(5.0)));
    array = array.setElement(123, new DefaultPrimitive(5.0));
    assertEquals(124, array.size());
  }

  @Test
  public void size_allNulls_countInSize() {
    Array array = new DefaultArray(ImmutableList.of(new DefaultPrimitive(5.0)));
    array = array.setElement(123, NullData.instance);
    assertEquals(124, array.size());
  }

  @Test
  public void clone_clonesElementsDeeply() {
    Array inner = new DefaultArray();
    Array original = new DefaultArray(ImmutableList.of(inner));
    Array clone = original.deepCopy().asArray();

    assertNotNull(clone);
    assertTrue(clone.getElement(0).isArray());

    inner = inner.setElement(0, new DefaultPrimitive(5.0));
    original.setElement(0, inner);

    assertEquals(5.0, original.getElement(0).asArray().getElement(0).asPrimitive().num(), 0.0);
    assertTrue(clone.getElement(0).asArray().getElement(0).isNullOrEmpty());
  }

  @Test
  public void toString_empty_returnsString() {
    Array array = new DefaultArray();
    assertEquals("[]", array.toString());
  }

  @Test
  public void toString_nonEmpty_returnsString() {
    Array array = new DefaultArray();
    array =
        array.setElement(0, new DefaultPrimitive("bar")).setElement(1, new DefaultPrimitive(1.));
    assertEquals("[bar, 1]", array.toString());
  }

  @Test
  public void equals_returnsTrue_elements() {
    Array boolTrue = new DefaultArray(ImmutableList.of(new DefaultPrimitive(true)));
    Array boolTrueCopy = new DefaultArray(ImmutableList.of(new DefaultPrimitive(true)));
    Array stringOne = new DefaultArray(ImmutableList.of(new DefaultPrimitive("one")));
    Array stringOneCopy = new DefaultArray(ImmutableList.of(new DefaultPrimitive("one")));
    Array doubleOne = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));
    Array doubleOneCopy = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));

    assertEquals(boolTrue, boolTrueCopy);
    assertEquals(stringOne, stringOneCopy);
    assertEquals(doubleOne, doubleOneCopy);
    assertEquals(boolTrueCopy, boolTrue);
    assertEquals(stringOneCopy, stringOne);
    assertEquals(doubleOneCopy, doubleOne);

    assertEquals(boolTrue.hashCode(), boolTrueCopy.hashCode());
    assertEquals(stringOne.hashCode(), stringOneCopy.hashCode());
    assertEquals(doubleOne.hashCode(), doubleOneCopy.hashCode());
  }

  @Test
  public void equals_returnsTrue_self() {
    Array one = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));
    assertEquals(one, one);
  }

  @Test
  public void equals_returnsFalse_elements() {
    Array boolTrue = new DefaultArray(ImmutableList.of(new DefaultPrimitive(true)));
    Array stringOne = new DefaultArray(ImmutableList.of(new DefaultPrimitive("one")));
    Array doubleOne = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));
    Array stringDoubleStyleOne = new DefaultArray(ImmutableList.of(new DefaultPrimitive("1.")));

    assertNotEquals(boolTrue, stringOne);
    assertNotEquals(stringOne, boolTrue);
    assertNotEquals(doubleOne, stringDoubleStyleOne);
    assertNotEquals(stringDoubleStyleOne, doubleOne);

    assertNotEquals(boolTrue.hashCode(), stringOne.hashCode());
    assertNotEquals(doubleOne.hashCode(), stringDoubleStyleOne.hashCode());
  }

  @Test
  public void equals_returnsFalse_nonArrayType() {
    Container container = new DefaultContainer().setField("0", new DefaultPrimitive(1.));
    Array defaultArray = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));

    assertNotEquals(container, defaultArray);
    assertNotEquals(defaultArray, container);
  }

  @Test
  public void equals_returnsTrue_nonDefaultArray_elements() {
    Array nonDefaultArray = arrayOf(new DefaultPrimitive(0.), new DefaultPrimitive(1.));
    Array defaultArray =
        new DefaultArray(ImmutableList.of(new DefaultPrimitive(0.), new DefaultPrimitive(1.)));

    assertEquals(defaultArray, nonDefaultArray);
  }

  @Test
  public void equals_returnsFalse_nonDefaultArray_elements() {
    Array nonDefaultArray = arrayOf(new DefaultPrimitive(0.), new DefaultPrimitive(1.));
    Array defaultArray =
        new DefaultArray(ImmutableList.of(new DefaultPrimitive(0.), new DefaultPrimitive(2.)));

    assertNotEquals(defaultArray, nonDefaultArray);
  }

  @Test
  public void equals_returnsFalse_nonDefaultArray_size() {
    Array nonDefaultArray = arrayOf(new DefaultPrimitive(0.), new DefaultPrimitive(1.));
    Array defaultArray = new DefaultArray();
    defaultArray = defaultArray.setElement(0, new DefaultPrimitive(0.));

    assertNotEquals(defaultArray, nonDefaultArray);
  }

  @Test
  public void equals_returnsTrue_isNullOrEmpty() {
    DefaultArray emptyArray = new DefaultArray();
    Array nullData = NullData.instance;

    assertEquals(emptyArray, nullData);
    assertEquals(nullData, emptyArray);
  }

  @Test
  public void getThrough_returnsProjectedArray() {
    Path path = Path.parse("foo");
    Data fooValue = mock(Container.class);
    Data container = containerOf(fooValue);

    DefaultArray array = new DefaultArray(Collections.nCopies(10, container));
    Array got = array.getThrough(path);

    assertEquals(DefaultArray.class, got.getClass());
    assertDCAPEquals(arrayOf(fooValue, 10), got);
  }

  @Test
  public void getThrough_filtersNulls() {
    Path path = Path.parse("foo");
    DefaultArray array = new DefaultArray(Collections.nCopies(10, new DefaultContainer()));

    Array got = array.getThrough(path);

    assertEquals(DefaultArray.class, got.getClass());
    assertEquals(0, got.size());
  }

  @Test
  public void flatten_flattensNestedArray() {
    // Have 5 unique items so we can assert order.
    Data[] flatItems =
        new Data[] {
          mock(Data.class), mock(Data.class), mock(Data.class), mock(Data.class), mock(Data.class)
        };
    DefaultArray array =
        new DefaultArray(
            ImmutableList.of(
                arrayOf(flatItems[0], flatItems[1]),
                arrayOf(flatItems[2], flatItems[3]),
                arrayOf(flatItems[4])));
    Array got = array.flatten();
    assertDCAPEquals(arrayOf(flatItems), got);
  }

  @Test
  public void flatten_noopEmptyArray() {
    DefaultArray array = new DefaultArray();
    Array got = array.flatten();
    assertTrue(got.isNullOrEmpty());
  }

  @Test
  public void checkDataInvariants() {
    // empty array
    new DataImplementationSemanticsTest(DefaultArray::new).testAll();
    // non-empty array
    new DataImplementationSemanticsTest(() -> new DefaultArray(ImmutableList.of(mock(Data.class))))
        .testAll();
  }
}
