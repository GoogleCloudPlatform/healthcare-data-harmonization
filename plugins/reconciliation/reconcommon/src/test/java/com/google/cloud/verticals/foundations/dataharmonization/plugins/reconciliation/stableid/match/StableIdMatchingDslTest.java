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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for StableIdMatchingDsl */
@RunWith(JUnit4.class)
public class StableIdMatchingDslTest {

  private final RuntimeContext ctx = RuntimeContextUtil.mockRuntimeContextWithRegistry();
  private final JsonSerializerDeserializer json = new JsonSerializerDeserializer();

  @Test
  public void simplePrimitiveMatch() {
    Data primitiveMatch = primitive(ctx, "somePrimitive");
    String expected =
        "{\n" + "  \"field\":\"somePrimitive\",\n" + "  \"fieldType\":\"primitive\"\n" + "}";

    assertEquals(toData(expected), primitiveMatch);
  }

  @Test
  public void simpleReferenceMatch() {
    Data referenceMatch = referenceFor(ctx, "patient");
    String expected =
        "{\n"
            + "  \"field\":\"patient\",\n"
            + "  \"fieldType\":\"container\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"field\":\"reference\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), referenceMatch);
  }

  @Test
  public void simpleIdentifiersMatch() {
    Data identifiersMatch = anyIdentifier(ctx);
    String expected =
        "{\n"
            + "  \"field\":\"identifier\",\n"
            + "  \"fieldType\":\"array\",\n"
            + "  \"pathOperator\":\"OR\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"fieldType\":\"container\",\n"
            + "      \"pathOperator\":\"AND\",\n"
            + "      \"paths\":[\n"
            + "        {\n"
            + "          \"field\":\"system\",\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        },\n"
            + "        {\n"
            + "          \"field\":\"value\",\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), identifiersMatch);
  }

  @Test
  public void identifiersMatchWithFilters() {
    Data identifiersMatch =
        anyIdentifier(
            ctx,
            filter(ctx, "system", testDTI().primitiveOf("unique-id")),
            filter(ctx, "system", testDTI().primitiveOf("unique-id2")),
            filter(ctx, "value", testDTI().primitiveOf("unique-value")));
    String expected =
        "{\n"
            + "  \"field\":\"identifier\",\n"
            + "  \"fieldType\":\"array\",\n"
            + "  \"pathOperator\":\"OR\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"fieldType\":\"container\",\n"
            + "      \"pathOperator\":\"AND\",\n"
            + "      \"paths\":[\n"
            + "        {\n"
            + "          \"field\":\"system\",\n"
            + "          \"fieldFilter\":[\n"
            + "            \"unique-id\",\n"
            + "            \"unique-id2\"\n"
            + "          ],\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        },\n"
            + "        {\n"
            + "          \"field\":\"value\",\n"
            + "          \"fieldFilter\":[\n"
            + "            \"unique-value\"\n"
            + "          ],\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), identifiersMatch);
  }

  @Test
  public void simpleCodingMatch() {
    Data codingMatch = anyCoding(ctx);
    String expected =
        "{\n"
            + "  \"field\":\"coding\",\n"
            + "  \"fieldType\":\"array\",\n"
            + "  \"pathOperator\":\"OR\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"fieldType\":\"container\",\n"
            + "      \"pathOperator\":\"AND\",\n"
            + "      \"paths\":[\n"
            + "        {\n"
            + "          \"field\":\"system\",\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        },\n"
            + "        {\n"
            + "          \"field\":\"code\",\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), codingMatch);
  }

  @Test
  public void codingMatchWithFilters() {
    Data codingMatch = anyCoding(ctx, filter(ctx, "system", testDTI().primitiveOf("unique-id")));
    String expected =
        "{\n"
            + "  \"field\":\"coding\",\n"
            + "  \"fieldType\":\"array\",\n"
            + "  \"pathOperator\":\"OR\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"fieldType\":\"container\",\n"
            + "      \"pathOperator\":\"AND\",\n"
            + "      \"paths\":[\n"
            + "        {\n"
            + "          \"field\":\"system\",\n"
            + "          \"fieldFilter\":[\n"
            + "            \"unique-id\"\n"
            + "          ],\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        },\n"
            + "        {\n"
            + "          \"field\":\"code\",\n"
            + "          \"fieldType\":\"primitive\"\n"
            + "        }\n"
            + "      ]\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), codingMatch);
  }

  @Test
  public void simpleAllMatch() {
    Data allMatch = allOf(ctx, primitive(ctx, "field1"), primitive(ctx, "field2"));
    String expected =
        "{\n"
            + "  \"fieldType\":\"container\",\n"
            + "  \"pathOperator\":\"AND\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"field\":\"field1\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"field\":\"field2\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), allMatch);
  }

  @Test
  public void simpleAnyMatch() {
    Data anyMatch = anyOf(ctx, primitive(ctx, "field1"), primitive(ctx, "field2"));
    String expected =
        "{\n"
            + "  \"fieldType\":\"container\",\n"
            + "  \"pathOperator\":\"OR\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"field\":\"field1\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"field\":\"field2\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), anyMatch);
  }

  @Test
  public void simplePathToMatch() {
    Data pathMatch = pathTo(ctx, "field1", primitive(ctx, "field2"));
    String expected =
        "{\n"
            + "  \"field\":\"field1\",\n"
            + "  \"fieldType\":\"container\",\n"
            + "  \"paths\":[\n"
            + "    {\n"
            + "      \"field\":\"field2\",\n"
            + "      \"fieldType\":\"primitive\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), pathMatch);
  }

  @Test
  public void simpleArrayAnyOfMatch() {
    Data arrayAnyOfMatch =
        arrayAnyOf(ctx, "udiCarrier", primitive(ctx, "deviceIdentifier"), primitive(ctx, "id"));
    String expected =
        "{\n"
            + "  \"fieldType\": \"array\",\n"
            + "  \"field\": \"udiCarrier\",\n"
            + "  \"pathOperator\": \"OR\",\n"
            + "  \"paths\": [\n"
            + "    {\n"
            + "      \"fieldType\": \"primitive\",\n"
            + "      \"field\": \"deviceIdentifier\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"fieldType\": \"primitive\",\n"
            + "      \"field\": \"id\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), arrayAnyOfMatch);
  }

  @Test
  public void simpleArrayAllOfMatch() {
    Data arrayAllOfMatch =
        arrayAllOf(ctx, "udiCarrier", primitive(ctx, "deviceIdentifier"), primitive(ctx, "id"));
    String expected =
        "{\n"
            + "  \"fieldType\": \"array\",\n"
            + "  \"field\": \"udiCarrier\",\n"
            + "  \"pathOperator\": \"AND\",\n"
            + "  \"paths\": [\n"
            + "    {\n"
            + "      \"fieldType\": \"primitive\",\n"
            + "      \"field\": \"deviceIdentifier\"\n"
            + "    },\n"
            + "    {\n"
            + "      \"fieldType\": \"primitive\",\n"
            + "      \"field\": \"id\"\n"
            + "    }\n"
            + "  ]\n"
            + "}";

    assertEquals(toData(expected), arrayAllOfMatch);
  }

  @Test
  public void simpleFilterField() {
    Data result = filterField(ctx, "code", testDTI().primitiveOf("MRN"));
    String expected =
        "{\n"
            + "  \"field\":\"code\",\n"
            + "  \"fieldFilter\":[\"MRN\"],\n"
            + "  \"fieldType\":\"primitive\"\n"
            + "}";
    assertEquals(toData(expected), result);
  }

  @Test
  public void filterFieldWithMultipleValues() {
    Data result =
        filterField(
            ctx,
            "code",
            testDTI()
                .arrayOf(testDTI().primitiveOf("MRN"), testDTI().primitiveOf("DriversLicense")));
    String expected =
        "{\n"
            + "  \"field\":\"code\",\n"
            + "  \"fieldFilter\":[\"MRN\", \"DriversLicense\"],\n"
            + "  \"fieldType\":\"primitive\"\n"
            + "}";
    assertEquals(toData(expected), result);
  }

  @Test
  public void filterFieldContainer_throwsException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> filterField(ctx, "fieldName", testDTI().emptyContainer()));
    assertEquals("Matching Criteria filter cannot be a container, got {}", ex.getMessage());
  }

  @Test
  public void filterFieldArrayWithContainer_throwsException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                filterField(
                    ctx,
                    "fieldName",
                    testDTI().arrayOf(testDTI().primitiveOf("test"), testDTI().emptyContainer())));
    assertEquals("fieldFilter has to be an array of primitives, got: {}", ex.getMessage());
  }

  @Test
  public void simpleFilterValue() {
    Data result = filterValue(ctx, testDTI().primitiveOf("test"));
    String expected =
        "{\n" + "  \"fieldFilter\":[\"test\"]," + "  \"fieldType\":\"primitive\"\n" + "}";
    assertEquals(toData(expected), result);
  }

  @Test
  public void filterArrayOfValues() {
    Data result =
        filterValue(
            ctx, testDTI().arrayOf(testDTI().primitiveOf("test1"), testDTI().primitiveOf("test2")));
    String expected =
        "{\n"
            + "  \"field\":\"\",\n"
            + "  \"fieldFilter\":[\"test1\", \"test2\"],"
            + "  \"fieldType\":\"primitive\"\n"
            + "}";
    assertEquals(toData(expected), result);
  }

  @Test
  public void filterValueContainer_throwsException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class, () -> filterValue(ctx, testDTI().emptyContainer()));
    assertEquals("Matching Criteria filter cannot be a container, got {}", ex.getMessage());
  }

  @Test
  public void filterValueArrayWithContainer_throwsException() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                filterValue(
                    ctx,
                    testDTI().arrayOf(testDTI().primitiveOf("test"), testDTI().emptyContainer())));
    assertEquals("fieldFilter has to be an array of primitives, got: {}", ex.getMessage());
  }

  @Test
  public void anyMetaTag_without_filter() {
    Data anyMetaTag = anyMetaTag(ctx);
    String expected =
        "{\n"
            + "\"field\": \"meta\",\n "
            + "\"fieldType\" : \"container\",\n"
            + "\"pathOperator\":\"OR\",\n"
            + "\"paths\": [\n"
            + "{\n"
            + "\"field\":\"tag\",\n"
            + "\"fieldType\" : \"array\",\n"
            + "\"pathOperator\":\"OR\",\n"
            + "\"paths\": [\n"
            + "{\n"
            + "\"fieldType\" :\"container\",\n"
            + "\"pathOperator\": \"AND\",\n"
            + "\"paths\": [ \n"
            + "{\n"
            + "\"field\": \"system\", \n"
            + "\"fieldType\": \"primitive\"\n"
            + "             },\n"
            + "           {\n"
            + "              \"field\":\"code\",\n"
            + "              \"fieldType\":\"primitive\"\n"
            + "            }\n"
            + "         ]\n"
            + "       }\n"
            + "     ]\n"
            + "   }\n"
            + " ]\n"
            + "}";
    assertEquals(toData(expected), anyMetaTag);
  }

  @Test
  public void anyMetaTag_with_filter() {
    Data anyMetaTag =
        anyMetaTag(
            ctx,
            filterField(ctx, "system", testDTI().primitiveOf("datasource1")),
            filterField(ctx, "code", testDTI().primitiveOf("code1")),
            filterField(ctx, "system", testDTI().primitiveOf("datasource2")));
    String expected =
        "{\n"
            + "\"field\": \"meta\",\n "
            + "\"fieldType\" : \"container\",\n"
            + "\"pathOperator\":\"OR\",\n"
            + "\"paths\": [\n"
            + "{\n"
            + "\"field\":\"tag\",\n"
            + "\"fieldType\" : \"array\",\n"
            + "\"pathOperator\":\"OR\",\n"
            + "\"paths\": [\n"
            + "{\n"
            + "\"fieldType\" :\"container\",\n"
            + "\"pathOperator\": \"AND\",\n"
            + "\"paths\": [ \n"
            + "{\n"
            + "\"field\": \"system\", \n"
            + "\"fieldType\": \"primitive\",\n"
            + "\"fieldFilter\" : [\"datasource1\", \"datasource2\"]\n"
            + "             },\n"
            + "           {\n"
            + "              \"field\":\"code\",\n"
            + "              \"fieldType\":\"primitive\",\n"
            + "\"fieldFilter\" : [\"code1\"]\n"
            + "            }\n"
            + "         ]\n"
            + "       }\n"
            + "     ]\n"
            + "   }\n"
            + " ]\n"
            + "}";
    assertEquals(toData(expected), anyMetaTag);
  }

  @Test
  public void anyMetaTag_wrongFieldName_exception() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> anyMetaTag(ctx, filterField(ctx, "value", testDTI().primitiveOf("code1"))));
    assertEquals(
        "Matching config field has to be one of \"system\", \"code\", \"url\" or \"valueString\","
            + " got: value",
        ex.getMessage());
  }

  @Test
  public void anyMetaTag_wrongFilter_exception() {
    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> anyMetaTag(ctx, filter(ctx, "value", testDTI().primitiveOf("code1"))));
    assertEquals("fieldFilters need to work with recon::filterField() plugin.", ex.getMessage());
  }

  private Data toData(String s) {
    return json.deserialize(s.getBytes());
  }
}
