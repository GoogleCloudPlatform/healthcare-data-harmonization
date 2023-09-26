/*
 * Copyright 2023 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ArrayBuilderTest {
  private static final DataTypeImplementation dti = testDTI();

  @Test
  public void testAdd_nullDataElement() {
    Array actual = dti.arrayBuilder().add(NullData.instance).build();
    assertEquals(1, actual.size());
    assertEquals(NullData.instance, actual.getElement(0));
  }

  @Test
  public void testAdd_nulls() {
    Data nullData = null;
    String nullString = null;
    Double nullNumber = null;
    Boolean nullBoolean = null;
    Array actual =
        dti.arrayBuilder()
            .add(NullData.instance, nullData)
            .add(nullString)
            .add(nullNumber)
            .add(nullBoolean)
            .build();
    assertEquals(5, actual.size());
    actual.stream().forEach(el -> assertEquals(NullData.instance, el));
  }

  @Test
  public void testAddVarArgs_multipleData() {
    Array actual = dti.arrayBuilder().add(dti.primitiveOf("one"), dti.primitiveOf("two")).build();
    assertEquals(2, actual.size());
    assertEquals(dti.primitiveOf("one"), actual.getElement(0));
    assertEquals(dti.primitiveOf("two"), actual.getElement(1));
  }

  @Test
  public void testAdd_string() {
    Array actual = dti.arrayBuilder().add("just_a_string").build();
    assertEquals(1, actual.size());
    assertEquals(dti.primitiveOf("just_a_string"), actual.getElement(0));
  }

  @Test
  public void testAdd_double() {
    Array actual = testDTI().arrayBuilder().add(15.).build();
    assertEquals(1, actual.size());
    assertEquals(dti.primitiveOf(15.), actual.getElement(0));
  }

  @Test
  public void testAdd_boolean() {
    Array actual = testDTI().arrayBuilder().add(true).add(Boolean.TRUE).build();
    assertEquals(2, actual.size());
    assertEquals(dti.primitiveOf(true), actual.getElement(0));
    assertEquals(dti.primitiveOf(true), actual.getElement(1));
  }

  @Test
  public void testAdd_mixed() {
    Array actual = dti.arrayBuilder().add(NullData.instance).add("just_a_string").add(15.).build();
    assertEquals(3, actual.size());
    assertEquals(NullData.instance, actual.getElement(0));
    assertEquals(dti.primitiveOf("just_a_string"), actual.getElement(1));
    assertEquals(dti.primitiveOf(15.), actual.getElement(2));
  }

  @Test
  public void testAddAll_iterableOrStream() {
    ImmutableList<Container> toAdd = ImmutableList.of(NullData.instance, NullData.instance);
    Array actual = dti.arrayBuilder().addAll(toAdd).build();
    Array actualTwo = dti.arrayBuilder().addAll(toAdd.stream()).build();
    assertEquals(actual, actualTwo);
    assertEquals(2, actual.size());
    assertEquals(NullData.instance, actual.getElement(0));
    assertEquals(NullData.instance, actual.getElement(1));
  }

  @Test
  public void testAddAll_array() {
    Array seedArray = dti.arrayOf(ImmutableList.of(NullData.instance));
    Array actual = dti.arrayBuilder().addAll(seedArray).build();
    assertEquals(1, actual.size());
    assertEquals(NullData.instance, actual.getElement(0));
  }

  @Test
  public void testAdd_array() {
    Array nestedArray = dti.arrayOf(ImmutableList.of(NullData.instance));
    Array actual = dti.arrayBuilder().add(nestedArray).build();
    assertEquals(1, actual.size());
    assertEquals(nestedArray, actual.getElement(0));
  }

  @Test
  public void testAddStrings() {
    Array actual =
        dti.arrayBuilder().addAllStrings(ImmutableList.of("one", "two", "three", "four")).build();
    assertEquals(4, actual.size());
    assertEquals(dti.primitiveOf("one"), actual.getElement(0));
    assertEquals(dti.primitiveOf("two"), actual.getElement(1));
    assertEquals(dti.primitiveOf("three"), actual.getElement(2));
    assertEquals(dti.primitiveOf("four"), actual.getElement(3));
  }

  @Test
  public void testAddNumbers() {
    Array actual = dti.arrayBuilder().addAllNumbers(ImmutableList.of(1., 2., 3.)).build();
    assertEquals(3, actual.size());
    assertEquals(dti.primitiveOf(1.), actual.getElement(0));
    assertEquals(dti.primitiveOf(2.), actual.getElement(1));
    assertEquals(dti.primitiveOf(3.), actual.getElement(2));
  }

  @Test
  public void testAddBooleans() {
    Array actual = dti.arrayBuilder().addAllBooleans(ImmutableList.of(true, Boolean.TRUE)).build();
    assertEquals(2, actual.size());
    assertEquals(dti.primitiveOf(true), actual.getElement(0));
    assertEquals(dti.primitiveOf(true), actual.getElement(1));
  }

  @Test
  public void testAddAll_combineArrayBuilders() {
    ArrayBuilder builder = dti.arrayBuilder().add("a_string");
    Array actual = dti.arrayBuilder().add(1.).addAll(builder).build();
    assertEquals(2, actual.size());
    assertEquals(dti.primitiveOf(1.), actual.getElement(0));
    assertEquals(dti.primitiveOf("a_string"), actual.getElement(1));
  }

  @Test
  public void testAddAll_arrayBuilder_gracefulSelfReference() {
    ArrayBuilder builder = dti.arrayBuilder();
    Array actual = builder.add("a_string").addAll(builder).addAll(builder).build();
    assertEquals(1, actual.size());
    assertEquals(dti.primitiveOf("a_string"), actual.getElement(0));
  }
}
