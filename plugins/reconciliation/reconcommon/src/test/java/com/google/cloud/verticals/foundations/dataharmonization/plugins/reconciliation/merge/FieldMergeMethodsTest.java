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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.diff;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.forceInbound;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.preferInbound;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.union;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods.unionByField;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.REMOVE_FIELD_PLACEHOLDER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for FieldMergeMethods. */
@RunWith(JUnit4.class)
public class FieldMergeMethodsTest {

  private final RuntimeContext context = RuntimeContextUtil.mockRuntimeContextWithRegistry();

  @Test
  public void union_noOverlappingEntries() {
    Data existing =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.)));
    Data inbound =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(3.), testDTI().primitiveOf(4.)));

    Data expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));

    Data actual = union(context, existing, inbound);
    assertEquals(expected, actual);
  }

  @Test
  public void union_existingSameAsInbound() {
    Data existing =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.)));
    Data inbound =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.)));

    Data actual = union(context, existing, inbound);
    assertEquals(existing, actual);
  }

  @Test
  public void union_bidirectionalIncompleteOverlap() {
    Data existing =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.)));
    Data inbound =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(2.), testDTI().primitiveOf(3.)));

    Data expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.)));

    Data actual = union(context, existing, inbound);
    assertEquals(expected, actual);
  }

  @Test
  public void union_nonArrayInputs() {
    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", testDTI().primitiveOf("foo"))
                    .build());
    Data inbound = testDTI().primitiveOf("bar");

    IllegalArgumentException actualThrown =
        assertThrows(IllegalArgumentException.class, () -> union(context, existing, inbound));
    assertEquals("Existing and inbound elements must be Arrays", actualThrown.getMessage());
  }

  @Test
  public void union_emptyExisting() {
    Data existing = testDTI().emptyArray();
    Data inbound = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf("bar")));

    Data actual = union(context, existing, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void union_emptyInbound() {
    Data existing = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf("bar")));
    Data inbound = testDTI().emptyArray();

    Data actual = union(context, existing, inbound);
    assertEquals(existing, actual);
  }

  @Test
  public void union_emptyExistingAndInbound() {
    Data existing = testDTI().emptyArray();
    Data inbound = testDTI().emptyArray();

    Data actual = union(context, existing, inbound);
    assertEquals(testDTI().emptyArray(), actual);
    assertEquals(NullData.instance, actual);
  }

  // NOTE: only testing additional path functionality with unionByField unit tests since
  //        the union functionality is backed by the same helper as union() builtin
  @Test
  public void unionByField_basicPath() {
    Data existingEntryOne =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(0.))
                    .put("other", testDTI().primitiveOf("foo"))
                    .build());
    Data existingEntryTwo =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(1.))
                    .put("other", testDTI().primitiveOf("bar"))
                    .build());
    Data inboundEntryOne =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(1.))
                    .put("other", testDTI().primitiveOf("baz"))
                    .build());
    Data inboundEntryTwo =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(2.))
                    .put("other", testDTI().primitiveOf("oof"))
                    .build());
    Data existing = testDTI().arrayOf(ImmutableList.of(existingEntryOne, existingEntryTwo));
    Data inbound = testDTI().arrayOf(ImmutableList.of(inboundEntryOne, inboundEntryTwo));
    String path = "key";

    Data expected =
        testDTI().arrayOf(ImmutableList.of(existingEntryOne, inboundEntryOne, inboundEntryTwo));

    Data actual = unionByField(context, existing, inbound, path);
    assertEquals(expected, actual);
  }

  @Test
  public void unionByField_multiplePaths() {
    Data existingEntryOne = toData(
        "{"
            + "  \"key\":0,"
            + "  \"key2\":10,"
            + "  \"other\":\"foo\""
            + "}");
    Data existingEntryTwo = toData(
        "{"
            + "  \"key\":1,"
            + "  \"key2\":11,"
            + "  \"other\":\"bar\""
            + "}");
    Data inboundEntryOne = toData(
        "{"
            + "  \"key\":1,"
            + "  \"key2\":11,"
            + "  \"other\":\"baz\""
            + "}");
    Data inboundEntryTwo = toData(
        "{"
            + "  \"key\":2,"
            + "  \"key2\":22,"
            + "  \"other\":\"oof\""
            + "}");
    Data inboundEntryThree = toData(
        "{"
            + "  \"key\":1,"
            + "  \"key2\":12,"
            + "  \"other\":\"blah\""
            + "}");
    Data existing = testDTI().arrayOf(ImmutableList.of(existingEntryOne, existingEntryTwo));
    Data inbound = testDTI().arrayOf(ImmutableList.of(inboundEntryOne, inboundEntryTwo,
        inboundEntryThree));

    Data expected =
        testDTI().arrayOf(ImmutableList.of(existingEntryOne, inboundEntryOne, inboundEntryTwo,
            inboundEntryThree));

    Data actual = unionByField(context, existing, inbound, "key", "key2");
    assertEquals(expected, actual);
  }

  @Test
  public void unionByField_invalidPath_usesNull() {
    Data existingEntryOne =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(0.))
                    .put("other", testDTI().primitiveOf("foo"))
                    .build());
    Data inboundEntryOne =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("key", testDTI().primitiveOf(1.))
                    .put("other", testDTI().primitiveOf("baz"))
                    .build());
    Data existing = testDTI().arrayOf(ImmutableList.of(existingEntryOne));
    Data inbound = testDTI().arrayOf(ImmutableList.of(inboundEntryOne));
    String path = "system";

    Data expected = testDTI().arrayOf(ImmutableList.of(inboundEntryOne));

    Data actual = unionByField(context, existing, inbound, path);
    assertEquals(expected, actual);
  }

  @Test
  public void unionByField_emptyPathSameAsUnion() {
    Data existing =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.)));
    Data inbound =
        testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(2.), testDTI().primitiveOf(3.)));
    String path = "";

    Data expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.)));

    Data actual = unionByField(context, existing, inbound, path);
    assertEquals(expected, actual);
  }

  @Test
  public void unionByField_emptyExistingAndInbound() {
    Data existing = testDTI().emptyArray();
    Data inbound = testDTI().emptyArray();
    String path = "key";

    Data actual = unionByField(context, existing, inbound, path);
    assertEquals(testDTI().emptyArray(), actual);
    assertEquals(NullData.instance, actual);
  }

  @Test
  public void forceInbound_emptyInbound_returnsRemovePlaceholder() {
    Data anyExisting = testDTI().primitiveOf("Anything");
    Data emptyInbound = testDTI().primitiveOf("");

    Data actual = forceInbound(context, anyExisting, emptyInbound);

    assertEquals(REMOVE_FIELD_PLACEHOLDER, actual);
  }

  @Test
  public void diff_existingArrayElementRemove() {
    Data entryOne =
        toData(
            "{"
                + "  \"target\":{ "
                + "     \"reference\": \"Patient/patient_stableId1\""
                + "  }"
                + "}");
    Data entryTwo =
        toData(
            "{"
                + "  \"target\":{ "
                + "     \"reference\": \"Patient/patient_stableId2\""
                + "  }"
                + "}");
    Data existing = testDTI().arrayOf(ImmutableList.of(entryOne, entryTwo));
    Data inbound = testDTI().arrayOf(ImmutableList.of(entryOne));

    Data expected = testDTI().arrayOf(ImmutableList.of(entryTwo));

    Data actual = diff(context, existing.asArray(), inbound.asArray());
    assertEquals(expected, actual);
  }

  @Test
  public void diff_sameExistingAndElementArray() {
    Data entryOne =
        toData(
            "{"
                + "  \"target\":{ "
                + "     \"reference\": \"Patient/patient_stableId1\""
                + "  }"
                + "}");
    Data existing = testDTI().arrayOf(ImmutableList.of(entryOne));
    Data inbound = testDTI().arrayOf(ImmutableList.of(entryOne));

    Data expected = testDTI().emptyArray();

    Data actual = diff(context, existing.asArray(), inbound.asArray());
    assertEquals(expected, actual);
  }

  @Test
  public void diff_inboundArrayEmpty() {
    Data entryOne =
        toData(
            "{"
                + "  \"target\":{ "
                + "     \"reference\": \"Patient/patient_stableId1\""
                + "  }"
                + "}");
    Data existing = testDTI().arrayOf(ImmutableList.of(entryOne));
    Data inbound = testDTI().emptyArray();

    Data expected = testDTI().arrayOf(ImmutableList.of(entryOne));

    Data actual = diff(context, existing.asArray(), inbound.asArray());
    assertEquals(expected, actual);
  }

  @Test
  public void diff_existingArrayEmpty() {
    Data entryOne =
        toData(
            "{"
                + "  \"target\":{ "
                + "     \"reference\": \"Patient/patient_stableId1\""
                + "  }"
                + "}");
    Data existing = testDTI().emptyArray();
    Data inbound = testDTI().arrayOf(ImmutableList.of(entryOne));

    Data expected = testDTI().emptyArray();

    Data actual = diff(context, existing.asArray(), inbound.asArray());
    assertEquals(expected, actual);
  }

  @Test
  public void diff_bothArrayEmpty() {
    Data existing = testDTI().emptyArray();
    Data inbound = testDTI().emptyArray();

    Data expected = testDTI().emptyArray();

    Data actual = diff(context, existing.asArray(), inbound.asArray());
    assertEquals(expected, actual);
  }

  @Test
  public void forceInbound_nullInbound_returnsRemovePlaceholder() {
    Data anyExisting = testDTI().primitiveOf("Anything");
    Data nullInbound = NullData.instance;

    Data actual = forceInbound(context, anyExisting, nullInbound);
    assertEquals(REMOVE_FIELD_PLACEHOLDER, actual);
  }

  @Test
  public void forceInbound_primitiveType() {
    Data anyExisting = testDTI().primitiveOf("Anything");
    Data inbound = testDTI().primitiveOf("bar");

    Data actual = forceInbound(context, anyExisting, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void forceInbound_arrayType() {
    Data anyExisting = testDTI().primitiveOf("Anything");
    Data inbound = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf("bar")));

    Data actual = forceInbound(context, anyExisting, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void forceInbound_containerType() {
    Data anyExisting = testDTI().primitiveOf("Anything");
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());

    Data actual = forceInbound(context, anyExisting, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void preferInbound_emptyInbound() {
    Data existing = testDTI().primitiveOf("foo");
    Data emptyInbound = testDTI().primitiveOf("");
    Data nullInbound = NullData.instance;

    Data emptyActual = preferInbound(context, existing, emptyInbound);
    Data nullActual = preferInbound(context, existing, nullInbound);
    assertEquals(existing, emptyActual);
    assertEquals(existing, nullActual);
  }

  @Test
  public void preferInbound_emptyExisting() {
    Data existing = testDTI().primitiveOf("");
    Data inbound = testDTI().primitiveOf("bar");

    Data actual = preferInbound(context, existing, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void preferInbound_emptyExistingAndInbound() {
    Data existing = testDTI().primitiveOf("");
    Data inbound = testDTI().primitiveOf("");

    Data actual = preferInbound(context, existing, inbound);
    assertEquals(NullData.instance, actual);
  }

  @Test
  public void preferInbound_primitiveTypes() {
    Data existing = testDTI().primitiveOf("foo");
    Data inbound = testDTI().primitiveOf("bar");

    Data actual = preferInbound(context, existing, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void preferInbound_arrayTypes() {
    Data existing = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf("foo")));
    Data inbound = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf("bar")));

    Data actual = preferInbound(context, existing, inbound);
    assertEquals(inbound, actual);
  }

  @Test
  public void preferInbound_containerTypes() {
    Data existing =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", testDTI().primitiveOf("foo"))
                    .build());
    Data inbound =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("field1", testDTI().primitiveOf("bar"))
                    .build());

    Data actual = preferInbound(context, existing, inbound);
    assertEquals(inbound, actual);
  }

  private static Data toData(String json) {
    return JsonSerializerDeserializer.jsonToData(json.getBytes());
  }
}
