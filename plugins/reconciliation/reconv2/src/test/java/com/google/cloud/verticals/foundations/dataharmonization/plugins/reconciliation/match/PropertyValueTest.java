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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.match;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.MatchingPlugin.extractPropertyValues;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.match.MatchingTestUtils.readJsonFile;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.allOf;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.anyCoding;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.anyIdentifier;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.anyMetaTag;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.anyOf;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.arrayAllOf;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.arrayAnyOf;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.filter;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.filterField;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.filterValue;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.pathTo;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.primitive;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl.referenceFor;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests to verify that building matching criteria with the matching dsl produces the correct
 * property values.
 */
@RunWith(JUnit4.class)
public class PropertyValueTest {
  private final RuntimeContext ctx = RuntimeContextUtil.mockRuntimeContextWithRegistry();

  @Test
  public void simpleMatch_producesPropertyValues() throws Exception {
    Container config = anyOf(ctx, primitive(ctx, "id"), anyIdentifier(ctx)).asContainer();
    Container resource = readJsonFile("patient-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of(
            "id=1", "identifier=system=MRN|value=456", "identifier=system=SSN|value=123");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void advancedMatch_producesPropertyValues() throws Exception {
    Container config =
        anyOf(
                ctx,
                anyIdentifier(ctx, filter(ctx, "system", testDTI().primitiveOf("unique-id"))),
                allOf(ctx, referenceFor(ctx, "patient"), pathTo(ctx, "code", anyCoding(ctx))))
            .asContainer();
    Container resource = readJsonFile("allergyIntolerance-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of(
            "identifier=system=unique-id|value=456",
            "code=coding=code=763875007|system=http://snomed.info/sct|patient=reference=Patient/example",
            "code=coding=code=227493005|system=http://snomed.info/sct|patient=reference=Patient/example");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void advancedMatch_withMultiplePathsAndReferences_producesPropertyValues()
      throws Exception {
    Container config =
        anyOf(
                ctx,
                allOf(
                    ctx,
                    referenceFor(ctx, "subject"),
                    pathTo(
                        ctx,
                        "medication",
                        anyOf(
                            ctx,
                            pathTo(ctx, "medicationCodeableConcept", anyCoding(ctx)),
                            referenceFor(ctx, "medicationReference")))),
                anyIdentifier(ctx))
            .asContainer();
    Container resource = readJsonFile("medicationRequest-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of(
            "identifier=system=idtype2|value=456",
            "identifier=system=idtype1|value=123",
            "medication=medicationCodeableConcept=coding=code=763875007|system=http://snomed.info/sct|subject=reference=Patient/example",
            "medication=medicationCodeableConcept=coding=code=227493005|system=http://snomed.info/sct|subject=reference=Patient/example",
            "medication=medicationReference=reference=Medication/example|subject=reference=Patient/example");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_differentOrderedAllOf_samePropertyValues() throws Exception {
    Container config1 =
        allOf(ctx, pathTo(ctx, "code", anyCoding(ctx)), referenceFor(ctx, "patient")).asContainer();
    Container config2 =
        allOf(ctx, referenceFor(ctx, "patient"), pathTo(ctx, "code", anyCoding(ctx))).asContainer();
    Container resource = readJsonFile("allergyIntolerance-resource.json");
    List<String> propertyValues1 =
        extractPropertyValues(ctx, config1, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> propertyValues2 =
        extractPropertyValues(ctx, config2, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of(
            "code=coding=code=763875007|system=http://snomed.info/sct|patient=reference=Patient/example",
            "code=coding=code=227493005|system=http://snomed.info/sct|patient=reference=Patient/example");
    assertPropertyValues(expected, propertyValues1);
    assertPropertyValues(expected, propertyValues2);
  }

  @Test
  public void match_arrayAnyOf_producesPropertyValues() throws Exception {
    Container config =
        arrayAnyOf(ctx, "udiCarrier", primitive(ctx, "deviceIdentifier"), primitive(ctx, "id"))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of("udiCarrier=deviceIdentifier=19088", "udiCarrier=id=deviceId073");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_arrayAllOf_producesPropertyValues() throws Exception {
    Container config =
        arrayAllOf(
                ctx,
                "udiCarrier",
                allOf(ctx, primitive(ctx, "deviceIdentifier"), primitive(ctx, "id")))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected = ImmutableList.of("udiCarrier=deviceIdentifier=19088|id=deviceId073");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_arrayAllOfPrimitives_producesPropertyValues() throws Exception {
    Container config =
        arrayAnyOf(ctx, "name", arrayAllOf(ctx, "given", primitive(ctx, ""))).asContainer();
    Container resource = readJsonFile("patient-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of("name=given=Alice|Bob|James", "name=given=Jim", "name=given=Bob");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_parsePropertyValues_throwsExceptionForMalformedArray() throws Exception {
    Container config =
        arrayAnyOf(ctx, "id", pathTo(ctx, "coding", primitive(ctx, "code"))).asContainer();
    Container resource = readJsonFile("device-resource.json");
    PropertyValueFetcherException expected =
        new PropertyValueFetcherException(
            " Config has 'field' = 'id' but 'resource' = '123' does not match 'fieldType' ="
                + " 'array'. ");
    Exception got =
        Assert.assertThrows(
            PropertyValueFetcherException.class,
            () -> extractPropertyValues(ctx, config, resource));
    assertEquals(expected.getClass(), got.getClass());
    assertEquals(expected.getMessage(), got.getMessage());
  }

  @Test
  public void match_parsePropertyValues_throwsExceptionForMalformedPrimitive() throws Exception {
    Container config = anyOf(ctx, primitive(ctx, "udiCarrier"), anyIdentifier(ctx)).asContainer();
    Container resource = readJsonFile("device-resource.json");
    PropertyValueFetcherException expected =
        new PropertyValueFetcherException(
            " Config has 'field' = 'udiCarrier' but 'resource' ="
                + " '[{carrierHRF=(01)09504000059118(17)141120(10)7654321D(21)10987654d321,"
                + " deviceIdentifier=19088, entryType=barcode, id=deviceId073,"
                + " issuer=http://hl7.org/fhir/NamingSystem/gs1-di,"
                + " jurisdiction=http://hl7.org/fhir/NamingSystem/fda-udi}]' does not match"
                + " 'fieldType' = 'primitive'. ");
    Exception got =
        Assert.assertThrows(
            PropertyValueFetcherException.class,
            () -> extractPropertyValues(ctx, config, resource));
    assertEquals(expected.getClass(), got.getClass());
    assertEquals(expected.getMessage(), got.getMessage());
  }

  @Test
  public void match_simpleFilterField_matches() throws Exception {
    Container config =
        filterField(ctx, "distinctIdentifier", testDTI().primitiveOf("I am a distinct identifier"))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected = ImmutableList.of("distinctIdentifier=I am a distinct identifier");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_simpleFilterField_noMatch() throws Exception {
    Container config =
        filterField(ctx, "distinctIdentifier", testDTI().primitiveOf("no match")).asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    assertPropertyValues(ImmutableList.of(), propertyValues);
  }

  @Test
  public void match_allOfFilterField_match() throws Exception {
    Container config =
        allOf(
                ctx,
                filterField(
                    ctx, "distinctIdentifier", testDTI().primitiveOf("I am a distinct identifier")),
                primitive(ctx, "lotNumber"))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of("distinctIdentifier=I am a distinct identifier|lotNumber=35");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_allOfFilterField_noMatch() throws Exception {
    Container config =
        allOf(
                ctx,
                filterField(ctx, "distinctIdentifier", testDTI().primitiveOf("no match")),
                primitive(ctx, "lotNumber"))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    assertPropertyValues(ImmutableList.of(), propertyValues);
  }

  @Test
  public void match_arrayAllOfPrimitivesWithFilter_producesPropertyValues() throws Exception {
    Container config =
        arrayAnyOf(
                ctx,
                "name",
                arrayAnyOf(
                    ctx,
                    "given",
                    filterValue(
                        ctx,
                        testDTI()
                            .arrayOf(
                                testDTI().primitiveOf("Alice"), testDTI().primitiveOf("James")))))
            .asContainer();
    Container resource = readJsonFile("patient-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    ImmutableList<String> expected = ImmutableList.of("name=given=Alice", "name=given=James");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_anyMetaTag_producesPropertyValues() throws Exception {
    Container config = anyMetaTag(ctx).asContainer();
    Container resource = readJsonFile("patient-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected =
        ImmutableList.of(
            "meta=tag=code=123|system=urn:oid:datasource1/reconciliation-external-id",
            "meta=tag=code=456|system=anonymous_system");
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_anyMetaTagWithFilter_producesPropertyValues() throws Exception {
    Container config =
        anyMetaTag(ctx, filterField(ctx, "system", testDTI().primitiveOf("datasource1_Id")))
            .asContainer();
    Container resource = readJsonFile("patient-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    ImmutableList<String> expected = ImmutableList.of();
    assertPropertyValues(expected, propertyValues);
  }

  @Test
  public void match_anyMetaTagWithFilter_noPropertyValue() throws Exception {
    Container config =
        anyMetaTag(ctx, filterField(ctx, "system", testDTI().primitiveOf("datasource1_Id")))
            .asContainer();
    Container resource = readJsonFile("device-resource.json");
    List<String> propertyValues =
        extractPropertyValues(ctx, config, resource).asArray().stream()
            .map(i -> i.asPrimitive().toString())
            .collect(toImmutableList());
    List<String> expected = ImmutableList.of();
    assertPropertyValues(expected, propertyValues);
  }

  private static void assertPropertyValues(
      List<String> expectedPropertyValues, List<String> actualPropertyValues) {
    assertEquals(
        ImmutableList.sortedCopyOf(expectedPropertyValues),
        ImmutableList.sortedCopyOf(actualPropertyValues));
  }
}
