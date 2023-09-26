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
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class ContainerBuilderTest {
  private static final DataTypeImplementation dti = testDTI();
  private static final String KEY = "key";

  @Test
  public void testPut_dataValue() {
    Container actual = dti.containerBuilder().put(KEY, NullData.instance).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField(KEY));
  }

  @Test
  public void testPut_stringPrimitive() {
    Container actual = dti.containerBuilder().put(KEY, "a_string").build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf("a_string"), actual.getField(KEY));
  }

  @Test
  public void testPut_doublePrimitive() {
    Container actual = dti.containerBuilder().put(KEY, 10.).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf(10.), actual.getField(KEY));
  }

  @Test
  public void testPut_booleanPrimitive() {
    Container actual = dti.containerBuilder().put(KEY, true).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf(true), actual.getField(KEY));
  }

  @Test
  public void testPut_dataEntry() {
    Container actual = dti.containerBuilder().putEntry(Map.entry(KEY, NullData.instance)).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField(KEY));
  }

  @Test
  public void testPut_stringEntry() {
    Container actual = dti.containerBuilder().putStringEntry(Map.entry(KEY, "a_string")).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf("a_string"), actual.getField(KEY));
  }

  @Test
  public void testPut_doubleEntry() {
    Container actual = dti.containerBuilder().putNumberEntry(Map.entry(KEY, 10.)).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf(10.), actual.getField(KEY));
  }

  @Test
  public void testPut_booleanEntry() {
    Container actual = dti.containerBuilder().putBooleanEntry(Map.entry(KEY, false)).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(dti.primitiveOf(false), actual.getField(KEY));
  }

  @Test
  public void testPut_dataMap() {
    ImmutableMap<String, Data> map = ImmutableMap.of(KEY, NullData.instance);
    Container actual = dti.containerBuilder().putAll(map).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField(KEY));
  }

  @Test
  public void testPut_stringMap() {
    ImmutableMap<String, String> map =
        ImmutableMap.of(
            "one", "1",
            "two", "2");
    Container actual = dti.containerBuilder().putAllStrings(map).build();
    assertThat(actual.fields()).hasSize(2);
    assertEquals(dti.primitiveOf("1"), actual.getField("one"));
    assertEquals(dti.primitiveOf("2"), actual.getField("two"));
  }

  @Test
  public void testPut_booleanMap() {
    ImmutableMap<String, Boolean> map =
        ImmutableMap.of(
            "feature_one_enabled", false,
            "feature_two_enabled", true);
    Container actual = dti.containerBuilder().putAllBooleans(map).build();
    assertThat(actual.fields()).hasSize(2);
    assertEquals(dti.primitiveOf(false), actual.getField("feature_one_enabled"));
    assertEquals(dti.primitiveOf(true), actual.getField("feature_two_enabled"));
  }

  @Test
  public void testPut_numberMap() {
    ImmutableMap<String, Double> map =
        ImmutableMap.of(
            "error_one_count", 1.,
            "error_two_count", 123456.);
    Container actual = dti.containerBuilder().putAllNumbers(map).build();
    assertThat(actual.fields()).hasSize(2);
    assertEquals(dti.primitiveOf(1.), actual.getField("error_one_count"));
    assertEquals(dti.primitiveOf(123456.), actual.getField("error_two_count"));
  }

  @Test
  public void testCopy_differentField() {
    Container reference = dti.containerOf(ImmutableMap.of(KEY, NullData.instance));
    Container actual = dti.containerBuilder().copy(reference, KEY, "other_key").build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField("other_key"));
  }

  @Test
  public void testCopy_sameField() {
    Container reference = dti.containerOf(ImmutableMap.of(KEY, NullData.instance));
    Container actual = dti.containerBuilder().copy(reference, KEY).build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField(KEY));
  }

  @Test
  public void testCopy_fieldNotExist_setNull() {
    Container reference = dti.containerOf(ImmutableMap.of(KEY, NullData.instance));
    Container actual = dti.containerBuilder().copy(reference, "other_key").build();
    assertThat(actual.fields()).hasSize(1);
    assertEquals(NullData.instance, actual.getField("other_key"));
  }

  @Test
  public void testPut_nulls() {
    Container actual =
        dti.containerBuilder()
            .put("nullData", (Data) null)
            .put("nullString", (String) null)
            .put("nullDouble", (Double) null)
            .put("nullBoolean", (Boolean) null)
            .putEntry(null)
            .putAll((Map<String, Data>) null)
            .build();
    assertThat(actual.fields()).hasSize(4);
    assertEquals(NullData.instance, actual.getField("nullData"));
    assertEquals(NullData.instance, actual.getField("nullString"));
    assertEquals(NullData.instance, actual.getField("nullDouble"));
    assertEquals(NullData.instance, actual.getField("nullBoolean"));
  }

  @Test
  public void testPut_container() {
    Container one = dti.containerOf(ImmutableMap.of("one", dti.primitiveOf("1")));
    Container two = dti.containerOf(ImmutableMap.of("two", dti.primitiveOf("2")));
    Container empty = dti.containerOf(ImmutableMap.of());
    Container actual =
        dti.containerBuilder().putAll(one).putAll(two).putAll(empty).put("three", "3").build();
    assertThat(actual.fields()).hasSize(3);
    assertEquals(dti.primitiveOf("1"), actual.getField("one"));
    assertEquals(dti.primitiveOf("2"), actual.getField("two"));
    assertEquals(dti.primitiveOf("3"), actual.getField("three"));
  }

  @Test
  public void testPut_containerBuilder() {
    ContainerBuilder one = dti.containerBuilder().put("one", "1");
    Container actual = dti.containerBuilder().putAll(one).put("two", "2").build();
    assertThat(actual.fields()).hasSize(2);
    assertEquals(dti.primitiveOf("1"), actual.getField("one"));
    assertEquals(dti.primitiveOf("2"), actual.getField("two"));
  }
}
