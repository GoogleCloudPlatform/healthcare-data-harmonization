/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.testutil;

import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.MatchingCriteria;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher.PathOperator;
import java.util.List;
import java.util.stream.Collectors;

/** Assertion methods for Stable ID identifiers */
public class StableIdAsserts {
  public static void assertPropertyValues(
      List<String> expectedPropertyValues, List<String> actualPropertyValues) {
    assertEquals(
        expectedPropertyValues.stream().sorted().collect(Collectors.toList()),
        actualPropertyValues.stream().sorted().collect(Collectors.toList()));
  }

  public static void assertEqualMatchingCriteriaValues(
      MatchingCriteria got,
      String expectedField,
      String expectedFieldType,
      PathOperator expectedOperator,
      List<String> expectedFieldFilter,
      int expectedChildrenSize) {
    assertEquals(expectedField, got.field());
    assertEquals(expectedFieldType, got.fieldType());
    assertEquals(expectedFieldFilter, got.fieldFilter());
    assertEquals(expectedOperator, got.pathOperator());
    assertEquals(expectedChildrenSize, got.children().size());
  }
}
