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

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultContainer. */
@RunWith(JUnit4.class)
public class DefaultContainerTest {

  @Test
  public void isContainer_returnsTrue() {
    assertTrue(new DefaultContainer().isContainer());
  }

  @Test
  public void isNullOrEmpty_emptyContainer_returnsTrue() {
    assertTrue(new DefaultContainer().isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_presetContainer_returnsFalse() {
    Container container = new DefaultContainer(ImmutableMap.of("test", new DefaultPrimitive(5.0)));
    assertFalse(container.isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_setContainer_returnsFalse() {
    Container container = new DefaultContainer().setField("test", new DefaultPrimitive(5.0));
    assertFalse(container.isNullOrEmpty());
  }

  @Test
  public void isNullOrEmpty_nullDataValue_returnsTrue() {
    Container container = new DefaultContainer().setField("test", NullData.instance);
    assertTrue(container.isNullOrEmpty());
  }

  @Test
  public void getField_nonExistingField_returnsNullValue() {
    Container container = new DefaultContainer();
    // Make sure it is not java null, i.e. no NPEs
    assertNotNull(container.getField("test"));
    assertTrue(container.getField("test").isNullOrEmpty());
  }

  @Test
  public void getField_presetField_returnsIt() {
    Container container = new DefaultContainer(ImmutableMap.of("test", new DefaultPrimitive(5.0)));
    assertEquals(5.0, container.getField("test").asPrimitive().num(), 0.0);
  }

  @Test
  public void setField_nonExistingField_setsIt() {
    Container container = new DefaultContainer().setField("test", new DefaultPrimitive(5.0));
    assertEquals(5.0, container.getField("test").asPrimitive().num(), 0.0);
  }

  @Test
  public void setField_existingField_overwrites() {
    Container container =
        new DefaultContainer()
            .setField("test", new DefaultPrimitive(1.0))
            .setField("test", new DefaultPrimitive(5.0));
    assertEquals(5.0, container.getField("test").asPrimitive().num(), 0.0);
  }

  @Test
  public void removeField_existingField_removesIt() {
    Container container = new DefaultContainer();
    String fieldName = "test";
    container.setField(fieldName, new DefaultPrimitive(5.0));
    container = container.removeField(fieldName);
    assertThat(container.fields()).isEmpty();
  }

  @Test
  public void removeField_nonExistentField_isNoOp() {
    Container container = new DefaultContainer();
    container = container.removeField("test");
    assertThat(container.fields()).isEmpty();
  }

  @Test
  public void fields_emptyContainer_returnsEmptySet() {
    Container container = new DefaultContainer();
    assertNotNull(container.fields());
    assertTrue(container.fields().isEmpty());
  }

  @Test
  public void fields_presetContainer_returnsKeySet() {
    Container container = new DefaultContainer(ImmutableMap.of("test", new DefaultPrimitive(5.0)));
    assertEquals(ImmutableSet.of("test"), container.fields());
  }

  @Test
  public void fields_setsOnContainer_returnsKeySet() {
    Container container = new DefaultContainer(ImmutableMap.of("test", new DefaultPrimitive(5.0)));
    container = container.setField("test2", new DefaultPrimitive(1.0));
    assertEquals(ImmutableSet.of("test", "test2"), container.fields());
  }

  @Test
  public void nonNullFields_emptyContainer_returnsEmptySet() {
    Container container = new DefaultContainer();
    assertNotNull(container.nonNullFields());
    assertThat(container.nonNullFields()).isEmpty();
  }

  @Test
  public void nonNullFields_containerWithNullFields_returnsNonNullFieldsOnly() {
    Container container =
        new DefaultContainer(
            ImmutableMap.of(
                "test1", new DefaultPrimitive(5.0),
                "test2", new DefaultPrimitive(6.0),
                "nullData", NullData.instance));
    container = container.setField("nullValue", null);
    assertEquals(ImmutableSet.of("test1", "test2"), container.nonNullFields());
  }

  @Test
  public void clone_clonesElementsDeeply() {
    Container inner = new DefaultContainer();
    Container original = new DefaultContainer(ImmutableMap.of("inner", inner));
    Container clone = original.deepCopy().asContainer();

    assertNotNull(clone);
    assertTrue(clone.getField("inner").isContainer());

    inner.setField("value", new DefaultPrimitive(5.0));
    assertEquals(
        5.0, original.getField("inner").asContainer().getField("value").asPrimitive().num(), 0.0);
    assertTrue(clone.getField("inner").asContainer().getField("value").isNullOrEmpty());
  }

  @Test
  public void toString_emptyContainer() {
    Container container = new DefaultContainer();
    assertEquals("{}", container.toString());
  }

  @Test
  public void toString_nonEmptyContainer() {
    Container container = new DefaultContainer();
    container =
        container
            .setField("one", new DefaultPrimitive("foo"))
            .setField("two", new DefaultPrimitive(1.));
    assertTrue(container.toString().contains("one=foo"));
    assertTrue(container.toString().contains("two=1"));
  }

  @Test
  public void equals_returnsTrue_fields() {
    Container one = new DefaultContainer();
    one = one.setField("one", new DefaultPrimitive(true));
    Container oneCopy = new DefaultContainer();
    oneCopy = oneCopy.setField("one", new DefaultPrimitive(true));
    Container two = new DefaultContainer();
    two = two.setField("two", new DefaultPrimitive("one"));
    Container twoCopy = new DefaultContainer();
    twoCopy = twoCopy.setField("two", new DefaultPrimitive("one"));
    Container three = new DefaultContainer();
    three = three.setField("three", new DefaultPrimitive(1.));
    Container threeCopy = new DefaultContainer();
    threeCopy = threeCopy.setField("three", new DefaultPrimitive(1.));

    assertEquals(one, oneCopy);
    assertEquals(two, twoCopy);
    assertEquals(three, threeCopy);
    assertEquals(oneCopy, one);
    assertEquals(twoCopy, two);
    assertEquals(threeCopy, three);

    assertEquals(one.hashCode(), oneCopy.hashCode());
    assertEquals(two.hashCode(), twoCopy.hashCode());
    assertEquals(three.hashCode(), threeCopy.hashCode());
  }

  @Test
  public void equals_returnsTrue_self() {
    Container one = new DefaultContainer();
    one = one.setField("foo", new DefaultPrimitive("bar"));

    assertEquals(one, one);
  }

  @Test
  public void equals_returnsFalse_fields() {
    Container boolTrue = new DefaultContainer();
    boolTrue = boolTrue.setField("foo", new DefaultPrimitive(true));
    Container stringTrue = new DefaultContainer();
    stringTrue = stringTrue.setField("foo", new DefaultPrimitive("true"));
    Container doubleOne = new DefaultContainer();
    doubleOne = doubleOne.setField("bar", new DefaultPrimitive(1.));
    Container stringDoubleStyleOne = new DefaultContainer();
    stringDoubleStyleOne = stringDoubleStyleOne.setField("bar", new DefaultPrimitive("1."));

    assertNotEquals(boolTrue, stringTrue);
    assertNotEquals(stringTrue, boolTrue);
    assertNotEquals(doubleOne, stringDoubleStyleOne);
    assertNotEquals(stringDoubleStyleOne, doubleOne);

    assertNotEquals(boolTrue.hashCode(), stringTrue.hashCode());
    assertNotEquals(doubleOne.hashCode(), stringDoubleStyleOne.hashCode());
  }

  @Test
  public void equals_returnsFalse_nonContainerType() {
    Primitive primitive = new DefaultPrimitive(1.);
    Container defaultContainer = new DefaultContainer().setField("foo", new DefaultPrimitive(1.));

    assertNotEquals(primitive, defaultContainer);
    assertNotEquals(defaultContainer, primitive);
  }

  @Test
  public void equals_returnsTrue_nonDefaultContainer_fields() {
    Container nonDefaultContainer = containerOf("one", new DefaultPrimitive(1.));
    Container defaultContainer = new DefaultContainer().setField("one", new DefaultPrimitive(1.));

    assertEquals(defaultContainer, nonDefaultContainer);
  }

  @Test
  public void equals_returnsFalse_nonDefaultContainer_fields() {
    Container nonDefaultContainer = containerOf("one", new DefaultPrimitive(1.));
    Container defaultContainer = new DefaultContainer().setField("one", new DefaultPrimitive(0.));

    assertNotEquals(defaultContainer, nonDefaultContainer);
  }

  @Test
  public void equals_returnsFalse_nonDefaultContainer_size() {
    Container nonDefaultContainer = containerOf("one", new DefaultPrimitive(1.));
    Container defaultContainer = new DefaultContainer();
    defaultContainer =
        defaultContainer
            .setField("one", new DefaultPrimitive(1.))
            .setField("two", new DefaultPrimitive(2.));

    assertNotEquals(defaultContainer, nonDefaultContainer);
  }

  @Test
  public void equals_returnsTrue_isNullOrEmpty() {
    DefaultContainer emptyContainer = new DefaultContainer();
    Container nullData = NullData.instance;

    assertEquals(emptyContainer, nullData);
    assertEquals(nullData, emptyContainer);
  }

  @Test
  public void checkDataInvariants() {
    // empty container
    new DataImplementationSemanticsTest(DefaultContainer::new).testAll();
    // non-empty container
    new DataImplementationSemanticsTest(
            () -> new DefaultContainer(ImmutableMap.of("field1", mock(Data.class))))
        .testAll();
  }
}
