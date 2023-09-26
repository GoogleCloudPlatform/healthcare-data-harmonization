// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for Preconditions utility class. */
@RunWith(JUnit4.class)
public class PreconditionsTest {

  @Test
  public void requireNonEmptyString_wrongPrim() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNonEmptyString(testDTI().primitiveOf(1.), "test"));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected a non-empty string for test but got a non-string primitive 1");
  }

  @Test
  public void requireNonEmptyString_null() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNonEmptyString(NullData.instance, "test"));
    assertThat(ex).hasMessageThat().contains("Expected a non-empty string for test but got null");
  }

  @Test
  public void requireNonEmptyString_wrongType() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Preconditions.requireNonEmptyString(
                    testDTI().containerOf(ImmutableMap.of("a", testDTI().primitiveOf(true))),
                    "test"));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected a non-empty string for test but got a Container");
  }

  @Test
  public void requireNonEmptyString_emptyString() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNonEmptyString(testDTI().primitiveOf(""), "test"));
    assertThat(ex).hasMessageThat().contains("Expected a non-empty string for test but got null");
  }

  @Test
  public void requireNonEmptyString_valid() {
    String actual = Preconditions.requireNonEmptyString(testDTI().primitiveOf("woop"), "test");
    assertThat(actual).isEqualTo("woop");
  }

  @Test
  public void requireNum_wrongPrim() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> Preconditions.requireNum(testDTI().primitiveOf("woop"), "test"));
    assertThat(ex)
        .hasMessageThat()
        .contains("Expected a number for test but got a non-numeric primitive woop");
  }

  @Test
  public void requireNum_wrongType() {
    Exception ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Preconditions.requireNum(
                    testDTI().containerOf(ImmutableMap.of("a", testDTI().primitiveOf(true))),
                    "test"));
    assertThat(ex).hasMessageThat().contains("Expected a number for test but got a Container");
  }

  @Test
  public void requireNum_nullDouble() {
    double actual = Preconditions.requireNum(testDTI().primitiveOf((Double) null), "test");
    assertThat(actual).isEqualTo(0);
  }

  @Test
  public void requireNum_nullData() {
    double actual = Preconditions.requireNum(NullData.instance, "test");
    assertThat(actual).isEqualTo(0);
  }

  @Test
  public void requireNum_valid() {
    double actual = Preconditions.requireNum(testDTI().primitiveOf(99.9), "test");
    assertThat(actual).isEqualTo(99.9);
  }
}
