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
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.testutil.StableIdAsserts.assertPropertyValues;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.MatchingCriteriaConfigException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher.PathOperator;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests to verify that the ConfigParser {@link PropertyValueFetcher} class produces reasonable
 * parsers and error messages.
 */
@RunWith(Enclosed.class)
public class PropertyValueFetcherTest {
  /*** Success cases. */
  @RunWith(Parameterized.class)
  public static class FetchTests {
    @Parameter public String testName;

    @Parameter(1)
    public MatchingCriteria matchingCriteria;

    @Parameter(2)
    public String resourceFileName;

    @Parameter(3)
    public List<String> expectedPropertyValues;

    @Parameters(name = "{0}")
    public static Collection<Object[]> resources() throws MatchingCriteriaConfigException {
      return Arrays.asList(
          new Object[][] {
            {
              "emptyResource_noPropertyValue",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("system")
                                  .setFieldType("primitive")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("value")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "empty-resource",
              ImmutableList.of()
            },
            {
              "anySystemOrValueMatch_anySingleTokens",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.OR)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("system")
                                  .setFieldType("primitive")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("value")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of(
                  "identifier=system=SSN",
                  "identifier=value=123",
                  "identifier=system=MRN",
                  "identifier=value=456")
            },
            {
              "allSystemAndValueMatch_combineAllTokens",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.AND)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("system")
                                  .setFieldType("primitive")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("value")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of(
                  "identifier=system=MRN|value=456|system=MRN|value=456|system=SSN|value=123")
            },
            {
              "allIdentifiersMatch_noIdentifiers_noPropertyValues",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.AND)
                  .build(),
              "patient_with_no_identifiers",
              ImmutableList.of()
            },
            {
              "allIdentifiersAndNamesMatch_noIdentifiers_noPropertyValues",
              new MatchingCriteria.Builder()
                  .setFieldType("container")
                  .setPathOperator(PathOperator.AND)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setField("identifier")
                          .setFieldType("array")
                          .setPathOperator(PathOperator.OR)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("container")
                                  .setPathOperator(PathOperator.AND)
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("system")
                                          .setFieldType("primitive")
                                          .build())
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("value")
                                          .setFieldType("primitive")
                                          .build())
                                  .build())
                          .build())
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setField("name")
                          .setFieldType("array")
                          .setPathOperator(PathOperator.OR)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("container")
                                  .setPathOperator(PathOperator.AND)
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("family")
                                          .setFieldType("primitive")
                                          .build())
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("use")
                                          .setFieldType("primitive")
                                          .build())
                                  .build())
                          .build())
                  .build(),
              "patient_with_no_identifiers",
              ImmutableList.of()
            },
            {
              "resourceDoNotHaveExpectedField_noPropertyValue",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.AND)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("doNotExist")
                                  .setFieldType("primitive")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("wrongFieldName")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of()
            },
            {
              "simpleMatchingCriteria_noDuplication",
              new MatchingCriteria.Builder()
                  .setFieldType("array")
                  .setField("identifier")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("system")
                                  .setFieldType("primitive")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("value")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of("identifier=system=SSN|value=123", "identifier=system=MRN|value=456")
            },
            {
              "complexConfig_fetchAndFilter",
              new MatchingCriteria.Builder()
                  .setFieldType("container")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setField("identifier")
                          .setFieldType("array")
                          .setPathOperator(PathOperator.OR)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("container")
                                  .setPathOperator(PathOperator.AND)
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("system")
                                          .setFieldType("primitive")
                                          .setFieldFilter(ImmutableList.of("unique-id"))
                                          .build())
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("value")
                                          .setFieldType("primitive")
                                          .build())
                                  .build())
                          .build())
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("patient")
                                  .setFieldType("container")
                                  .addChild(
                                      new MatchingCriteria // third level 0
                                              .Builder()
                                          .setField("reference")
                                          .setFieldType("primitive")
                                          .build())
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("code")
                                  .setFieldType("container")
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("coding")
                                          .setFieldType("array")
                                          .setPathOperator(PathOperator.OR)
                                          .addChild(
                                              new MatchingCriteria.Builder()
                                                  .setFieldType("container")
                                                  .setPathOperator(PathOperator.AND)
                                                  .addChild(
                                                      new MatchingCriteria.Builder()
                                                          .setField("system")
                                                          .setFieldType("primitive")
                                                          .build())
                                                  .addChild(
                                                      new MatchingCriteria.Builder()
                                                          .setField("code")
                                                          .setFieldType("primitive")
                                                          .build())
                                                  .build())
                                          .build())
                                  .build())
                          .build())
                  .build(),
              "allergyIntolerance-resource",
              ImmutableList.of(
                  "identifier=system=unique-id|value=456",
                  "code=coding=code=763875007|system=http://snomed.info/sct|patient=reference=Patient/example",
                  "code=coding=code=227493005|system=http://snomed.info/sct|patient=reference=Patient/example")
            },
            {
              "configWithFieldFilter_fetchPrimitiveArray",
              new MatchingCriteria.Builder()
                  .setField("name")
                  .setFieldType("array")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("given")
                                  .setFieldType("array")
                                  .setPathOperator(PathOperator.OR)
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setFieldType("primitive")
                                          .setFieldFilter(ImmutableList.of("Bob", "Alice"))
                                          .build())
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setField("family")
                                  .setFieldType("primitive")
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of(
                  "name=family=Chalmers|given=Bob",
                  "name=family=Chalmers|given=Alice",
                  "name=family=Windsor|given=Bob")
            },
            {
              "resourceWithFloats_fetchFloats",
              new MatchingCriteria.Builder()
                  .setField("age")
                  .setFieldType("array")
                  .setPathOperator(PathOperator.AND)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setFieldType("container")
                          .setPathOperator(PathOperator.AND)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("primitive")
                                  .setField("unit")
                                  .build())
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("primitive")
                                  .setField("value")
                                  .build())
                          .build())
                  .build(),
              "nonstring-resource",
              ImmutableList.of(
                  "age=unit=months|value=13.4|unit=seconds|value=3.52385964E7|unit=years|value=1.1")
            },
            {
              "defaultMetaTag",
              new MatchingCriteria.Builder()
                  .setField("meta")
                  .setFieldType("container")
                  .setPathOperator(PathOperator.OR)
                  .addChild(
                      new MatchingCriteria.Builder()
                          .setField("tag")
                          .setFieldType("array")
                          .setPathOperator(PathOperator.OR)
                          .addChild(
                              new MatchingCriteria.Builder()
                                  .setFieldType("container")
                                  .setPathOperator(PathOperator.AND)
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("system")
                                          .setFieldType("primitive")
                                          .build())
                                  .addChild(
                                      new MatchingCriteria.Builder()
                                          .setField("code")
                                          .setFieldType("primitive")
                                          .build())
                                  .build())
                          .build())
                  .build(),
              "patient-resource",
              ImmutableList.of(
                  "meta=tag=code=123|system=urn:oid:datasource1/reconciliation-external-id",
                  "meta=tag=code=456|system=anonymous_system")
            }
          });
    }

    @Test
    public void fetchPropertyValue() throws Exception {
      Container resource = readJsonFile(resourceFileName + ".json");
      PropertyValueFetcher fetcher = PropertyValueFetcher.from(resource, matchingCriteria);
      List<String> got = fetcher.fetch();
      assertPropertyValues(expectedPropertyValues, got);
    }
  }

  /** Failed cases. */
  @RunWith(Parameterized.class)
  public static class ThrowFetcherExceptionTest {

    @Parameter public String prefix;

    @Parameter(1)
    public String expectedErrorMessage;

    @Parameters(name = "{0}")
    public static Collection<Object[]> resources() {
      return Arrays.asList(
          new Object[][] {
            {
              "emptyFieldFieldTypeDoesNotMatchResource",
              " Config has empty 'field', given 'resource' = 'wrong' does not match 'fieldType' ="
                  + " 'container'. "
            },
            {
              "fieldPresentWithoutContainerResource",
              " Config has 'field' = 'key' but 'resource' = 'wrong' is not a container. "
            },
            {
              "fieldValueDoesNotMatchExpectedType",
              " Config has 'field' = 'containerField' but 'resource' = '[wrong, value, type]' does"
                  + " not match 'fieldType' = 'container'. "
            }
          });
    }

    @Test
    public void errorMessageCases() throws Exception {
      Container config = readJsonFile("error/" + prefix + "-config.json");
      Container resource = readJsonFile("error/" + prefix + ".json");
      MatchingCriteria mc = MatchingCriteria.of(config);
      Exception got =
          Assert.assertThrows(
              PropertyValueFetcherException.class, () -> PropertyValueFetcher.from(resource, mc));
      assertEquals(expectedErrorMessage, got.getMessage());
    }
  }
}
