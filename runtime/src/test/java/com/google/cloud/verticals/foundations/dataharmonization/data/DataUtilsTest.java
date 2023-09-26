/*
 * Copyright 2023 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DataUtilsTest {
  public static final DataTypeImplementation dti = testDTI();

  @Test
  public void testTruthy_nullData() {
    assertFalse(DataUtils.isTruthy(NullData.instance));
  }

  @Test
  public void testTruthy_nonEmptyPrimitive() {
    assertTrue(DataUtils.isTruthy(dti.primitiveOf("a_string")));
  }

  @Test
  public void testTruthy_falsePrimitive() {
    assertFalse(DataUtils.isTruthy(dti.primitiveOf(false)));
  }

  @Test
  public void testTruePrimitive_nonBoolPrimitive() {
    assertFalse(DataUtils.isTruePrimitive(dti.primitiveOf("a_string")));
  }

  @Test
  public void testTruePrimitive_truePrimitive() {
    assertTrue(DataUtils.isTruePrimitive(dti.primitiveOf(true)));
  }

  @Test
  public void testIsString_notString() {
    assertFalse(DataUtils.isString(dti.primitiveOf(true)));
  }

  @Test
  public void testIsString_isString() {
    assertTrue(DataUtils.isString(dti.primitiveOf("a_string")));
  }

  @Test
  public void testIsNumber_notNumber() {
    assertFalse(DataUtils.isNumber(dti.primitiveOf(true)));
  }

  @Test
  public void testIsNumber_isNumber() {
    assertTrue(DataUtils.isNumber(dti.primitiveOf(1.)));
  }

  @Test
  public void testIsBool_notBool() {
    assertFalse(DataUtils.isBoolean(dti.primitiveOf(1.)));
  }

  @Test
  public void testIsBool_isBool() {
    assertTrue(DataUtils.isBoolean(dti.primitiveOf(false)));
  }

  @Test
  public void testStringEquals_isNotString() {
    assertFalse(DataUtils.equals(dti.primitiveOf(true), "a_string"));
  }

  @Test
  public void testStringEquals_nonEqualString() {
    assertFalse(DataUtils.equals(dti.primitiveOf("a_string"), "a_different_string"));
  }

  @Test
  public void testStringEquals_equalString() {
    assertTrue(DataUtils.equals(dti.primitiveOf("a_string"), "a_string"));
  }

  @Test
  public void testNumEquals_isNotNum() {
    assertFalse(DataUtils.equals(dti.primitiveOf(1.), "a_string"));
  }

  @Test
  public void testNumEquals_nonEqualNum() {
    assertFalse(DataUtils.equals(dti.primitiveOf(1.), 2.));
  }

  @Test
  public void testNumEquals_equalNum() {
    assertTrue(DataUtils.equals(dti.primitiveOf(1.), 1.));
  }

  @Test
  public void testisNonEmpty_isNull() {
    assertFalse(DataUtils.isNonEmpty(null));
  }

  @Test
  public void testisNonEmpty_isNullData() {
    assertFalse(DataUtils.isNonEmpty(NullData.instance));
  }

  @Test
  public void testisNonEmpty_isEmpty() {
    assertFalse(DataUtils.isNonEmpty(dti.containerOf(ImmutableMap.of())));
  }

  @Test
  public void testIsNonEmpty_isNonEmpty() {
    assertTrue(DataUtils.isNonEmpty(dti.primitiveOf("a_string")));
  }
}
