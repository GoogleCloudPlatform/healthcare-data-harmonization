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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for DefaultPrimitive. */
@RunWith(JUnit4.class)
public class DefaultPrimitiveTest {

  @Test
  public void isPrimitive_anyValue_returnsTrue() {
    assertTrue(new DefaultPrimitive(0.0).isPrimitive());
    assertTrue(new DefaultPrimitive("").isPrimitive());
    assertTrue(new DefaultPrimitive(false).isPrimitive());
  }

  @Test
  public void num_nonNumValues_returnsNull() {
    assertNull(new DefaultPrimitive("foo").num());
    assertNull(new DefaultPrimitive(true).num());
  }

  @Test
  public void num_numValue_returnsValue() {
    assertEquals(3.14, new DefaultPrimitive(3.14).num(), 0);
  }

  @Test
  public void rounded_decimalValue_returnsRounded() {
    assertEquals(3, new DefaultPrimitive(3.14).rounded(), 0);
    assertEquals(4, new DefaultPrimitive(3.5).rounded(), 0);
  }

  @Test
  public void fractionIsNegligible_fuzzyChecks() {
    assertTrue(new DefaultPrimitive(Math.nextUp(3.0)).isFractionNegligible());
    assertTrue(new DefaultPrimitive(Math.nextDown(3.0)).isFractionNegligible());
    assertFalse(new DefaultPrimitive(Math.nextUp(Math.nextUp(3.0))).isFractionNegligible());
    assertFalse(new DefaultPrimitive(Math.nextDown(Math.nextDown(3.0))).isFractionNegligible());
  }

  @Test
  public void string_nonStringValues_returnsNull() {
    assertNull(new DefaultPrimitive(true).string());
    assertNull(new DefaultPrimitive(3.14).string());
  }

  @Test
  public void string_stringValues_returnsValue() {
    assertEquals("foo", new DefaultPrimitive("foo").string());
  }

  @Test
  public void bool_nonBoolValues_returnsNull() {
    assertNull(new DefaultPrimitive("foo").bool());
    assertNull(new DefaultPrimitive(3.14).bool());
  }

  @Test
  public void bool_boolValues_returnsValue() {
    assertEquals(true, new DefaultPrimitive(true).bool());
  }

  @Test
  public void isNullOrEmpty_nullValues_returnsTrue() {
    assertTrue(new DefaultPrimitive((Double) null).isNullOrEmpty());
    assertTrue(new DefaultPrimitive((String) null).isNullOrEmpty());
    assertTrue(new DefaultPrimitive((Boolean) null).isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_nonNullValues_returnsFalse() {
    assertFalse(new DefaultPrimitive(false).isNullOrEmpty());
    assertFalse(new DefaultPrimitive(0.0).isNullOrEmpty());
    assertFalse(new DefaultPrimitive(true).isNullOrEmpty());
    assertFalse(new DefaultPrimitive("foo").isNullOrEmpty());
  }

  // Ensure backwards compat with legacy Whistle.
  @Test
  public void isNullOrEmpty_emptyString_returnsTrue() {
    assertTrue(new DefaultPrimitive("").isNullOrEmpty());
  }

  @Test
  public void clone_returnsSameValue() {
    Primitive string = new DefaultPrimitive("foo");
    assertTrue(string.deepCopy().isPrimitive());
    assertEquals(string.string(), string.deepCopy().asPrimitive().string());

    Primitive num = new DefaultPrimitive(3.14);
    assertTrue(num.deepCopy().isPrimitive());
    assertEquals(num.num(), num.deepCopy().asPrimitive().num());

    Primitive bool = new DefaultPrimitive(true);
    assertTrue(bool.deepCopy().isPrimitive());
    assertEquals(bool.bool(), bool.deepCopy().asPrimitive().bool());
  }

  @Test
  public void clone_nullPrimitive_returnsNullValue() {
    Primitive nullPrim = new DefaultPrimitive((Double) null);
    assertTrue(nullPrim.deepCopy().isPrimitive());
    assertTrue(nullPrim.deepCopy().isNullOrEmpty());
  }

  @Test
  public void toString_num_returnsDoubleString() {
    Primitive numPrim = new DefaultPrimitive(1.25);
    assertEquals("1.25", numPrim.toString());
  }

  @Test
  public void toString_num_integerValue_returnsLongString() {
    Primitive integerNumPrim = new DefaultPrimitive(1.0);
    assertEquals("1", integerNumPrim.toString());
  }

  @Test
  public void toString_num_doubleValueWithinTolerance_returnsLongString() {
    Primitive integerNumPrim = new DefaultPrimitive(Math.nextUp(1.0));
    assertEquals("1", integerNumPrim.toString());
  }

  @Test
  public void toString_num_doubleValueOutsideTolerance_returnsDoubleString() {
    Primitive justUnderIntegerNumPrim = new DefaultPrimitive(1.0 - 1e-10);
    Primitive justOverIntegerNumPrim = new DefaultPrimitive(1.0 + 1e-10);
    assertEquals("0.9999999999", justUnderIntegerNumPrim.toString());
    assertEquals("1.0000000001", justOverIntegerNumPrim.toString());
  }

  @Test
  public void toString_str_returnsString() {
    Primitive strPrim = new DefaultPrimitive("one");
    assertEquals("one", strPrim.toString());
  }

  @Test
  public void toString_bool_returnsString() {
    Primitive boolPrim = new DefaultPrimitive(true);
    assertEquals("true", boolPrim.toString());
  }

  @Test
  public void toString_nullPrimitive_returnsNull() {
    Primitive nullPrim = new DefaultPrimitive((Double) null);
    assertNull(nullPrim.toString());
  }

  @Test
  public void equals_returnsFalse_nonPrimitiveType() {
    Array array = new DefaultArray(ImmutableList.of(new DefaultPrimitive(1.)));
    Primitive primitive = new DefaultPrimitive(1.);

    assertNotEquals(array, primitive);
    assertNotEquals(primitive, array);
  }

  @Test
  public void equals_returnsTrue_nonDefaultPrimitiveType() {
    Primitive nonDefaultPrimitive = mock(Primitive.class, Answers.CALLS_REAL_METHODS);
    when(nonDefaultPrimitive.num()).thenReturn(3.14);
    when(nonDefaultPrimitive.string()).thenReturn(null);
    when(nonDefaultPrimitive.bool()).thenReturn(null);
    Primitive defaultPrim = new DefaultPrimitive(3.14);

    assertEquals(defaultPrim, nonDefaultPrimitive);
  }

  @Test
  public void equals_returnsFalse_value() {
    Primitive boolTrue = new DefaultPrimitive(true);
    Primitive stringOne = new DefaultPrimitive("one");
    Primitive doubleOne = new DefaultPrimitive(1.);
    Primitive stringDoubleStyleOne = new DefaultPrimitive("1.");

    assertNotEquals(boolTrue, stringOne);
    assertNotEquals(stringOne, boolTrue);
    assertNotEquals(doubleOne, stringDoubleStyleOne);
    assertNotEquals(stringDoubleStyleOne, doubleOne);

    assertNotEquals(boolTrue.hashCode(), stringOne.hashCode());
    assertNotEquals(doubleOne.hashCode(), stringDoubleStyleOne.hashCode());
  }

  @Test
  public void equals_returnsTrue_value() {
    Primitive boolTrue = new DefaultPrimitive(true);
    Primitive boolTrueCopy = new DefaultPrimitive(true);
    Primitive stringOne = new DefaultPrimitive("one");
    Primitive stringOneCopy = new DefaultPrimitive("one");
    Primitive doubleOne = new DefaultPrimitive(1.);
    Primitive doubleOneCopy = new DefaultPrimitive(1.);

    assertEquals(boolTrue, boolTrueCopy);
    assertEquals(stringOne, stringOneCopy);
    assertEquals(doubleOne, doubleOneCopy);

    assertEquals(boolTrue.hashCode(), boolTrueCopy.hashCode());
    assertEquals(stringOne.hashCode(), stringOneCopy.hashCode());
    assertEquals(doubleOne.hashCode(), doubleOneCopy.hashCode());
  }

  @Test
  public void equals_returnsTrue_self() {
    Primitive doubleOne = new DefaultPrimitive(1.);
    assertEquals(doubleOne, doubleOne);
  }

  @Test
  public void equals_returnsTrue_isNullOrEmpty() {
    Primitive emptyString = new DefaultPrimitive("");
    Primitive nullData = NullData.instance;

    assertEquals(emptyString, nullData);
    assertEquals(nullData, emptyString);

    Primitive explicitEmptyString = new ExplicitEmptyString();
    assertNotEquals(explicitEmptyString, nullData);
  }

  @Test
  public void testDataAPIInvariant() {
    new DataImplementationSemanticsTest(() -> new DefaultPrimitive(1.0)).testAll();
    new DataImplementationSemanticsTest(() -> new DefaultPrimitive(true)).testAll();
    new DataImplementationSemanticsTest(() -> new DefaultPrimitive(false)).testAll();
    new DataImplementationSemanticsTest(() -> new DefaultPrimitive("hello")).testAll();
  }
}
