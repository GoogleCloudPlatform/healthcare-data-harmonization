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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for Operators. */
@RunWith(JUnit4.class)
public class OperatorsTest {

  @Test
  public void eq_twoData_true() {
    Data a = mock(Data.class);
    Primitive result = Operators.eq(new TestContext(), a, a);
    assertTrue(result.bool());
  }

  @Test
  public void eq_threeData_true() {
    Data a = mock(Data.class);
    Primitive result = Operators.eq(new TestContext(), a, a, a);
    assertTrue(result.bool());
  }

  @Test
  public void eq_twoDiffData_false() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Primitive result = Operators.eq(new TestContext(), a, b);
    assertFalse(result.bool());
  }

  @Test
  public void eq_threeDiffData_false() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Data c = mock(Data.class);
    Primitive result = Operators.eq(new TestContext(), a, b, c);
    assertFalse(result.bool());
  }

  @Test
  public void eq_threeDiffDataTwoUnqiue_false() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Primitive result = Operators.eq(new TestContext(), b, b, a);
    assertFalse(result.bool());
  }

  @Test
  public void neq_twoData_false() {
    Data a = mock(Data.class);
    Primitive result = Operators.neq(new TestContext(), a, a);
    assertFalse(result.bool());
  }

  @Test
  public void neq_threeData_false() {
    Data a = mock(Data.class);
    Primitive result = Operators.neq(new TestContext(), a, a, a);
    assertFalse(result.bool());
  }

  @Test
  public void neq_twoDiffData_true() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Primitive result = Operators.neq(new TestContext(), a, b);
    assertTrue(result.bool());
  }

  @Test
  public void neq_threeDiffData_true() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Data c = mock(Data.class);
    Primitive result = Operators.neq(new TestContext(), a, b, c);
    assertTrue(result.bool());
  }

  @Test
  public void neq_threeDiffDataTwoUnqiue_true() {
    Data a = mock(Data.class);
    Data b = mock(Data.class);
    Primitive result = Operators.neq(new TestContext(), b, b, a);
    assertTrue(result.bool());
  }

  @Test
  public void neq_nullAndTwoData_true() {
    Data a = mock(Data.class);
    Data b = NullData.instance;
    Primitive result = Operators.neq(new TestContext(), a, a, b);
    assertTrue(result.bool());
  }

  @Test
  public void neq_uniqueNulls_false() {
    Data a = NullData.instance;
    Data b = NullData.instance;
    Data c = NullData.instance;
    Primitive result = Operators.neq(new TestContext(), a, b, c);
    assertFalse(result.bool());
  }

  @Test
  public void mul_oneNum_self() {
    Primitive result = Operators.mul(new TestContext(), testDTI().primitiveOf(3.14));
    assertEquals(3.14, result.num(), 0.0);
  }

  @Test
  public void mul_null_zeros() {
    Primitive result = Operators.mul(new TestContext(), NullData.instance);
    assertEquals(0.0, result.num(), 0.0);

    result = Operators.mul(new TestContext(), NullData.instance, testDTI().primitiveOf(3.14));
    assertEquals(0.0, result.num(), 0.0);
  }

  @Test
  public void mul_twoNums_multiplies() {
    Primitive result =
        Operators.mul(new TestContext(), testDTI().primitiveOf(3.14), testDTI().primitiveOf(2.5));
    assertEquals(3.14 * 2.5, result.num(), 0.0);
  }

  @Test
  public void mul_threeNums_multiplies() {
    Primitive result =
        Operators.mul(
            new TestContext(),
            testDTI().primitiveOf(3.14),
            testDTI().primitiveOf(2.5),
            testDTI().primitiveOf(9.1));
    assertEquals(3.14 * 2.5 * 9.1, result.num(), 0.0);
  }

  @Test
  public void mul_nonNum_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Operators.mul(
                new TestContext(),
                testDTI().primitiveOf(3.14),
                testDTI().primitiveOf("hi"),
                testDTI().primitiveOf(9.1)));
  }

  @Test
  public void div_twoNums_divides() {
    Primitive result =
        Operators.div(new TestContext(), testDTI().primitiveOf(3.14), testDTI().primitiveOf(1.1));
    assertEquals(3.14 / 1.1, result.num(), 0.0);
  }

  @Test
  public void div_null_zeros() {
    Primitive result =
        Operators.div(new TestContext(), NullData.instance, testDTI().primitiveOf(3.14));
    assertEquals(0.0, result.num(), 0.0);

    result = Operators.div(new TestContext(), testDTI().primitiveOf(3.14), NullData.instance);
    assertTrue(Double.isInfinite(result.num()));
  }

  @Test
  public void div_byZero_inf() {
    Primitive result =
        Operators.div(new TestContext(), testDTI().primitiveOf(3.14), testDTI().primitiveOf(0.0));
    assertTrue(Double.isInfinite(result.num()));
  }

  @Test
  public void div_nonNum_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Operators.div(
                new TestContext(), testDTI().primitiveOf(3.14), testDTI().primitiveOf("hi")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Operators.div(
                new TestContext(), testDTI().primitiveOf("hi"), testDTI().primitiveOf(3.14)));
  }

  @Test
  public void sum_nums_adds() {
    Primitive result =
        Operators.sum(
            new TestContext(),
            testDTI().primitiveOf(3.14),
            testDTI().primitiveOf(1.),
            testDTI().primitiveOf(2.));
    assertEquals(6.14, result.num(), .000000000000001);
  }

  @Test
  public void sum_numsNull_addsAsZero() {
    Primitive result =
        Operators.sum(new TestContext(), testDTI().primitiveOf(3.14), NullData.instance);
    assertEquals(3.14, result.num(), 0.0);
  }

  @Test
  public void sum_strings_concats() {
    Primitive result =
        Operators.sum(
            new TestContext(), testDTI().primitiveOf("hello"), testDTI().primitiveOf("world"));
    assertEquals("helloworld", result.string());
  }

  @Test
  public void sum_stringsMixed_concats() {
    Primitive result =
        Operators.sum(
            new TestContext(),
            testDTI().primitiveOf(3.14),
            testDTI().primitiveOf("world"),
            NullData.instance,
            testDTI().primitiveOf(true));
    assertEquals("3.14worldtrue", result.string());
  }

  @Test
  public void sub_nums_subtract() {
    Primitive result =
        Operators.sub(
            new TestContext(),
            testDTI().primitiveOf(3.14),
            testDTI().primitiveOf(1.),
            testDTI().primitiveOf(2.5));
    assertEquals(-0.36, result.num(), 0.000000000001);
  }

  @Test
  public void sub_numsNull_zero() {
    Primitive result =
        Operators.sub(new TestContext(), NullData.instance, testDTI().primitiveOf(3.14));
    assertEquals(-3.14, result.num(), 0.00000001);
  }

  @Test
  public void sub_nonNum_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Operators.sub(
                new TestContext(), testDTI().primitiveOf(1.), testDTI().primitiveOf("foo")));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Operators.sub(
                new TestContext(), testDTI().primitiveOf("foo"), testDTI().primitiveOf(1.)));
  }

  @Test
  public void isNil_true() {
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNil(new TestContext(), testDTI().primitiveOf((Double) null)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNil(new TestContext(), testDTI().primitiveOf((Boolean) null)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNil(new TestContext(), testDTI().primitiveOf((String) null)));
    assertEquals(
        testDTI().primitiveOf(true), Operators.isNil(new TestContext(), testDTI().primitiveOf("")));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNil(new TestContext(), testDTI().emptyContainer()));
    assertEquals(
        testDTI().primitiveOf(true), Operators.isNil(new TestContext(), testDTI().emptyArray()));
  }

  @Test
  public void isNil_false() {
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(new TestContext(), testDTI().primitiveOf(0.0)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(new TestContext(), testDTI().primitiveOf(1.0)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(new TestContext(), testDTI().primitiveOf(true)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(new TestContext(), testDTI().primitiveOf(false)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(new TestContext(), testDTI().primitiveOf("something")));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(
            new TestContext(),
            testDTI().containerOf(ImmutableMap.of("foo", testDTI().primitiveOf("bar")))));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNil(
            new TestContext(), testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.0)))));
  }

  @Test
  public void isNotNil_true() {
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf(0.0)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf(1.0)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf(true)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf(false)));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf("something")));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(
            new TestContext(),
            testDTI().containerOf(ImmutableMap.of("foo", testDTI().primitiveOf("bar")))));
    assertEquals(
        testDTI().primitiveOf(true),
        Operators.isNotNil(
            new TestContext(), testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.0)))));
  }

  @Test
  public void isNotNil_false() {
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf((Double) null)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf((Boolean) null)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf((String) null)));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().primitiveOf("")));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().emptyContainer()));
    assertEquals(
        testDTI().primitiveOf(false),
        Operators.isNotNil(new TestContext(), testDTI().emptyArray()));
  }

  @Test
  public void or_empty_false() {
    assertEquals(testDTI().primitiveOf(false), Operators.or(new TestContext()));
  }

  @Test
  public void or_all_false() {
    Closure arg0 = mock(Closure.class);
    when(arg0.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Closure arg1 = mock(Closure.class);
    when(arg1.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Closure arg2 = mock(Closure.class);
    when(arg2.execute(any())).thenReturn(testDTI().primitiveOf(false));
    assertEquals(testDTI().primitiveOf(false), Operators.or(new TestContext(), arg0, arg1, arg2));
  }

  @Test
  public void or_lazy_eval() {
    Closure arg0 = mock(Closure.class);
    when(arg0.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Closure arg1 = mock(Closure.class);
    when(arg1.execute(any())).thenReturn(testDTI().primitiveOf(true));
    Closure arg2 = mock(Closure.class);
    when(arg2.execute(any())).thenReturn(testDTI().primitiveOf(false));
    assertEquals(testDTI().primitiveOf(true), Operators.or(new TestContext(), arg0, arg1, arg2));
    verify(arg1, times(1)).execute(any());
    verify(arg2, times(0)).execute(any());
  }

  @Test
  public void and_empty_false() {
    assertEquals(testDTI().primitiveOf(false), Operators.and(new TestContext()));
  }

  @Test
  public void and_all_true() {
    Closure arg0 = mock(Closure.class);
    when(arg0.execute(any())).thenReturn(testDTI().primitiveOf(true));
    Closure arg1 = mock(Closure.class);
    when(arg1.execute(any())).thenReturn(testDTI().primitiveOf(true));
    assertEquals(testDTI().primitiveOf(true), Operators.or(new TestContext(), arg0, arg1));
  }

  @Test
  public void and_lazy_eval() {
    Closure arg0 = mock(Closure.class);
    when(arg0.execute(any())).thenReturn(testDTI().primitiveOf(true));
    Closure arg1 = mock(Closure.class);
    when(arg1.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Closure arg2 = mock(Closure.class);
    when(arg2.execute(any())).thenReturn(testDTI().primitiveOf(true));
    assertEquals(testDTI().primitiveOf(false), Operators.and(new TestContext(), arg0, arg1, arg2));
    verify(arg1, times(1)).execute(any());
    verify(arg2, times(0)).execute(any());
  }

  @Test
  public void floor_double_with_decimal() {
    Primitive result = Operators.floor(new TestContext(), testDTI().primitiveOf(3.14159));
    assertEquals(3., result.num(), 0.0);
  }

  @Test
  public void floor_double_no_decimal() {
    Primitive result = Operators.floor(new TestContext(), testDTI().primitiveOf(3.));
    assertEquals(3., result.num(), 0.0);
  }

  @Test
  public void floor_non_numeric_throwsException() {
    assertThrows(
        IllegalArgumentException.class,
        () -> Operators.floor(new TestContext(), testDTI().primitiveOf(true)));
  }
}
