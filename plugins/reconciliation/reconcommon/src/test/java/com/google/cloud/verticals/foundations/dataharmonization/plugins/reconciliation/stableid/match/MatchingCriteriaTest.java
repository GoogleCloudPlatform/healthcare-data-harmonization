/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.testutil.MatchingTestUtil.readJsonFile;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.testutil.StableIdAsserts.assertEqualMatchingCriteriaValues;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.MatchingCriteriaConfigException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher.PathOperator;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests to verify that the MatchingCriteria {@link MatchingCriteria} class produces reasonable
 * parsers and error messages.
 */
@RunWith(Parameterized.class)
public class MatchingCriteriaTest {

  @Test
  public void parse_complexConfig_producesParsersIteratively()
      throws Exception {
    Container config = readJsonFile("allergyIntolerance-complex-config.json");
    MatchingCriteria got = MatchingCriteria.of(config);

    assertEqualMatchingCriteriaValues(
        got,
        /* expectedField= */ "",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 2);
    assertEqualMatchingCriteriaValues(
        got.children().get(0),
        /* expectedField= */ "identifier",
        /* expectedFieldType= */ "array",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 1);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(0),
        /* expectedField= */ "",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.AND,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 2);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(0).children().get(0),
        /* expectedField= */ "system",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of("unique-id"),
        /* expectedChildrenSize= */ 0);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(0).children().get(1),
        /* expectedField= */ "value",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 0);
    assertEqualMatchingCriteriaValues(
        got.children().get(1),
        /* expectedField= */ "",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.AND,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 2);
    assertEqualMatchingCriteriaValues(
        got.children().get(1).children().get(0),
        /* expectedField= */ "patient",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 1);
    assertEqualMatchingCriteriaValues(
        got.children().get(1).children().get(0).children().get(0),
        /* expectedField= */ "reference",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 0);
    assertEqualMatchingCriteriaValues(
        got.children().get(1).children().get(1),
        /* expectedField= */ "code",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 1);
    assertEqualMatchingCriteriaValues(
        got.children().get(1).children().get(1).children().get(0),
        /* expectedField= */ "coding",
        /* expectedFieldType= */ "array",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 1);
    assertEqualMatchingCriteriaValues(
        got.children().get(1).children().get(1).children().get(0).children().get(0),
        /* expectedField= */ "",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.AND,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 2);
    assertEqualMatchingCriteriaValues(
        got.children()
            .get(1)
            .children()
            .get(1)
            .children()
            .get(0)
            .children()
            .get(0)
            .children()
            .get(0),
        /* expectedField= */ "system",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 0);
    assertEqualMatchingCriteriaValues(
        got.children()
            .get(1)
            .children()
            .get(1)
            .children()
            .get(0)
            .children()
            .get(0)
            .children()
            .get(1),
        /* expectedField= */ "code",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 0);
  }

  @Test
  public void parse_configWithPrimitiveArray_producesMatchingCriteriaWithFieldFilter()
      throws Exception {
    Container config = readJsonFile("primitiveArray-config.json");
    MatchingCriteria got = MatchingCriteria.of(config);
    assertEqualMatchingCriteriaValues(
        got,
        /* expectedField= */ "name",
        /* expectedFieldType= */ "array",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 1);
    assertEqualMatchingCriteriaValues(
        got.children().get(0),
        /* expectedField= */ "",
        /* expectedFieldType= */ "container",
        /* expectedOperator= */ PathOperator.AND,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 2);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(0),
        /* expectedField= */ "given",
        /* expectedFieldType= */ "array",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        1);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(0).children().get(0),
        /* expectedField= */ "",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of("Bob", "Alice"),
        /* expectedChildrenSize= */ 0);
    assertEqualMatchingCriteriaValues(
        got.children().get(0).children().get(1),
        /* expectedField= */ "family",
        /* expectedFieldType= */ "primitive",
        /* expectedOperator= */ PathOperator.OR,
        /* expectedFieldFilter= */ ImmutableList.of(),
        /* expectedChildrenSize= */ 0);
  }

  @Parameter() public String configFile;

  @Parameter(1)
  public String expectedErrorMessage;

  @Parameters(name = "{0}")
  public static Collection<Object[]> resources() {
    return Arrays.asList(
        new Object[][] {
          {"arrayWithoutField", " Config with 'fieldType': 'array' must have non-nil 'field'. "},
          {"arrayWithoutPaths", " Empty 'paths' cannot coexist with non-primitive 'fieldType'. "},
          {
            "containerWithoutPaths",
            " Empty 'paths' cannot coexist with non-primitive 'fieldType'. "
          },
          {"missingFieldType", " Missing value for 'fieldType'. "},
          {
            "invalidFieldType",
            " Missing valid 'fieldType', has to be 'container'/'array'/'primitive'. "
          },
          {
            "primitiveWithPaths",
            " Config with 'fieldType' = 'primitive' must have non-nil 'paths'. "
          },
          {
            "nonArrayPaths",
            " Matching criteria 'paths' must be an array of matching criteria, the config file is"
                + " malformed. "
          },
          {
            "nonContainerPathsElement",
            " Matching criteria 'paths' must be an array of matching criteria containers, the "
                + "config file is malformed. "
          },
          {
            "nonPrimitivePathOperator",
            " Matching criteria 'pathOperator' = '[AND, OR]' must be a primitive of the value"
                + " 'AND'/'OR'. "
          },
          {
            "invalidPathOperator",
            " Matching criteria 'pathOperator' = 'NOT' must be a primitive of the value 'AND'/'OR'."
                + " "
          },
          {
            "nonArrayFieldFilter",
            " Matching criteria 'fieldFilter' must be an array, the config file is malformed. "
          },
          {
            "nonPrimitiveFieldFilterElement",
            " Matching criteria 'fieldFilter' must be an array of primitives, the config file is"
                + " malformed. "
          },
          {
            "nonPrimitiveFieldType",
            " Matching criteria 'fieldType' must be a primitive, the config file is malformed. "
          },
          {
            "fieldFilterCoexistWithContainer",
            " 'fieldFilter' must coexist with 'fieldType' = 'primitive'. "
          },
          {
            "fieldFilterCoexistWithArray",
            " 'fieldFilter' must coexist with 'fieldType' = 'primitive'. "
          }
        });
  }

  @Test
  public void errorMessageCases() throws IOException {
    Container config = readJsonFile("error/" + configFile + "-config.json");
    Exception got =
        Assert.assertThrows(
            MatchingCriteriaConfigException.class, () -> MatchingCriteria.of(config));
    assertEquals(expectedErrorMessage, got.getMessage());
  }
}
