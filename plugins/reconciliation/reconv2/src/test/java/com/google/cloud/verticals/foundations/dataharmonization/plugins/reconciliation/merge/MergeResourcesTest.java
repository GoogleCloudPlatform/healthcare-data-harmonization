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
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.BIGQUERY_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CLOUD_SPANNER_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_FIELD_RULES_METHOD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_RESOURCE_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.FHIR_VERSION_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.HL7V2_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.META_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RULE_METHOD_SUFFIX;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergingTestUtils.addStableIdToNewResource;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergingTestUtils.buildMockContextMergeFunction;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergingTestUtils.setWstlFunctionInContext;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergingTestUtils.toData;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.TAG_FIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMergeRuleFactory;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMergerFactory;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.MergedChoiceFieldFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import javax.naming.ConfigurationException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for MergeResources. */
@RunWith(JUnit4.class)
public class MergeResourcesTest {

  // Testing strategy for the mergeResource builtin is to provide high-level tests to demonstrate
  // the builtin's functionality and top-level logic, followed by detailed tests targeting the
  // various private helpers.
  private MergeResources mergeResources;
  // ********** Test data. **********
  private RuntimeContext context;
  private static final Data patientSnapshotOne =
      toData("{    \"fieldOne\": 1.,    \"fieldTwo\": \"a\"    }");
  private static final Data patientSnapshotTwo =
      toData("{    \"fieldOne\": 2.,    \"fieldTwo\": \"b\"    }");
  private static final Data patientSnapshotThree =
      toData("{    \"fieldOne\": 3.,    \"fieldTwo\": \"\"     }");
  private static final Data patientSnapshots =
      testDTI()
          .arrayOf(ImmutableList.of(patientSnapshotOne, patientSnapshotTwo, patientSnapshotThree));
  // ********** End of Test Data. **********

  @Before
  public void setup() {
    context = buildMockContextMergeFunction();
    MergeResultAnnotator annotator = new MergeResultAnnotator();
    this.mergeResources =
        new MergeResources(
            new MergeConfigExecutorImpl(new MergeFunctionFactoryImpl(annotator)),
            new DefaultMergerFactory(
                new DefaultMergeRuleFactory(annotator), new MergedChoiceFieldFactory(annotator)));
  }

  @Test
  public void mergeResources_latestRule_returnsLatestSnapshot() throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    // input snapshots - patientSnapshots:
    // [ {"fieldOne": 1., "fieldTwo": "a" },
    //   {"fieldOne": 2., "fieldTwo": "b" },
    //   {"fieldOne": 3., "fieldTwo": NullData }]

    // expected:
    // {"fieldOne": 3., "fieldTwo": NullData, "id": "001"}
    Data expected = addStableIdToNewResource(patientSnapshotThree, stableId);

    assertEquals(expected, merge(context, patientSnapshots.deepCopy(), resourceInfo));
  }

  @Test
  public void mergeResources_mergeRule_iterativeMergeOfSnapshots() throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    // input snapshots - patientSnapshots:
    // [ {"fieldOne": 1., "fieldTwo": "a" },
    //   {"fieldOne": 2., "fieldTwo": "b" },
    //   {"fieldOne": 3., "fieldTwo": NullData }]

    // expected:
    // {"resource": {"fieldOne": 3., "fieldTwo": "b", "id": "001"}}
    Data expected = addStableIdToNewResource(patientSnapshotThree, stableId);
    expected = expected.asContainer().setField("fieldTwo", testDTI().primitiveOf("b"));

    assertEquals(expected, merge(context, patientSnapshots.deepCopy(), resourceInfo));
  }


  @Test
  public void mergeResources_iterativeMerge_singleSnapshot() throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    // input snapshot:
    // [ {"fieldOne": 1., "fieldTwo": "a" } ]
    Data singlePatientSnapshot = testDTI().arrayOf(ImmutableList.of(patientSnapshotOne));

    // expected:
    // {"resource": {"fieldOne": 1., "fieldTwo": "a", id: "001"}}
    Data expected = addStableIdToNewResource(patientSnapshotOne, stableId);

    assertEquals(expected, merge(context, singlePatientSnapshot.deepCopy(), resourceInfo));
  }

  @Test
  public void mergeResources_emptySnapshotsArray() {
    Data resourceInfo = buildResourceInfo("Patient", "001");
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data emptySnapshots = toData("[]");

    IllegalArgumentException actualThrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> merge(context, emptySnapshots.deepCopy(), resourceInfo));

    assertEquals(
        "Attempted reconciliation with an empty array of snapshots for resource with id '001' and"
            + " type 'Patient'. This could indicate an error in querying the resource's snapshots"
            + " or an issue with the intermediate store.",
        actualThrown.getMessage());
  }

  @Test
  public void mergeResources_nullDataArray() {
    Data resourceInfo = buildResourceInfo("Patient", "001");
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data nullSnapshots = NullData.instance;

    IllegalArgumentException actualThrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> merge(context, nullSnapshots.deepCopy(), resourceInfo));

    assertEquals(
        "Attempted reconciliation with an empty array of snapshots for resource with id '001' and"
            + " type 'Patient'. This could indicate an error in querying the resource's snapshots"
            + " or an issue with the intermediate store.",
        actualThrown.getMessage());
  }

  @Test
  public void mergeResources_nonArray() {
    Data resourceInfo = buildResourceInfo("Patient", "001");
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data snapshotsContainer = toData("{ \"not\": \"anArray\" }");

    IllegalArgumentException actualThrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> merge(context, snapshotsContainer.deepCopy(), resourceInfo));

    assertEquals(
        String.format(
            "Reconciliation can only be performed on an Array of Container resource snapshots, but"
                + " provided: '%s'",
            snapshotsContainer),
        actualThrown.getMessage());
  }

  @Test
  public void mergeResources_arrayOfNonContainers() {
    Data resourceInfo = buildResourceInfo("Patient", "001");
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data nonContainerSnapshotElements = toData("[just, some, Primitive, strings]");

    IllegalArgumentException actualThrown =
        assertThrows(
            IllegalArgumentException.class,
            () -> merge(context, nonContainerSnapshotElements.deepCopy(), resourceInfo));

    assertEquals(
        String.format(
            "Reconciliation can only be performed on an Array of Container resource snapshots, but"
                + " provided: '%s'",
            nonContainerSnapshotElements),
        actualThrown.getMessage());
  }

  @Test
  public void mergeResources_defaultRuleByFieldName_inExistingOnly_removeFieldPlaceholderSuccess()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Container defaultRules =
        toData(
                "{ "
                    + "\"Primitive\": { \"rule\": \"preferInbound\"},"
                    + "\"fieldFour\": { \"rule\": \"forceInbound\"} }")
            .asContainer();
    setWstlFunctionInContext(context, DEFAULT_FIELD_RULES_METHOD, defaultRules);
    setWstlFunctionInContext(
        context, DEFAULT_RESOURCE_TYPE + RULE_METHOD_SUFFIX, testDTI().primitiveOf("merge"));

    Data fieldFourOnlyInFirstSnapshot =
        toData(
            "["
                + "  {"
                + "    \"fieldOne\": 1.0,"
                + "    \"fieldFour\": \"a\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 2.0"
                + "  }"
                + "]");

    Data expected =
        toData("{" + "\"id\": \"001\"," + "\"fieldOne\": 2.0," + "\"fieldTwo\": \"\"" + "}");

    assertEquals(expected, merge(context, fieldFourOnlyInFirstSnapshot, resourceInfo));
  }

  @Test
  public void mergeResources_unConfiguredField_defaultRuleByFieldName()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data oneSnapshotWithUnmergedField =
        toData(
            "["
                + "  {"
                + "    \"fieldOne\": 1.0,"
                + "    \"fieldTwo\": \"a\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 2.0,"
                + "    \"fieldTwo\": \"b\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 3.0,"
                // default 'fieldThree' field merge rule defined in mockDefaultFieldRulesWstlFn()
                + "    \"fieldThree\": \"ope\""
                + "  }"
                + "]");

    Data expected =
        toData(
            "{"
                + "\"id\": \"001\","
                + "\"fieldOne\": 3.0,"
                + "\"fieldTwo\": \"b\","
                + "\"fieldThree\": \"ope\""
                + "}");

    assertEquals(expected, merge(context, oneSnapshotWithUnmergedField.deepCopy(), resourceInfo));
  }

  @Test
  public void mergeResources_unConfiguredField_inMultipleSnapshotsOfAMerge()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("merge"));

    Data multipleSnapshotsWithUnmergedField =
        toData(
            "["
                + "  {"
                + "    \"fieldOne\": 1.0,"
                + "    \"fieldTwo\": \"a\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 2.0,"
                + "    \"fieldTwo\": \"b\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 3.0,"
                // default 'fieldThree' field merge rule defined in mockDefaultFieldRulesWstlFn()
                + "    \"fieldThree\": \"ope\""
                + "  },"
                + "  {"
                + "    \"fieldOne\": 3.0,"
                // default 'fieldThree' field merge rule defined in mockDefaultFieldRulesWstlFn()
                + "    \"fieldThree\": \"oof\""
                + "  }"
                + "]");

    Data expected =
        toData(
            "{"
                + "\"id\": \"001\","
                + "\"fieldOne\": 3.0,"
                + "\"fieldTwo\": \"b\","
                + "\"fieldThree\": \"oof\""
                + "}");

    assertEquals(
        expected, merge(context, multipleSnapshotsWithUnmergedField.deepCopy(), resourceInfo));
  }

  @Test
  public void mergeResources_unConfiguredResourceMergeRule_mergeWithDefaultFieldRules()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("ActivityDefinition", stableId);

    Container defaultRules =
        toData(
                "{ "
                    + "\"Primitive\": { \"rule\": \"preferInbound\"},"
                    + "\"fieldOne\": { \"rule\": \"preferInbound\"} }")
            .asContainer();
    setWstlFunctionInContext(context, DEFAULT_FIELD_RULES_METHOD, defaultRules);
    setWstlFunctionInContext(
        context, DEFAULT_RESOURCE_TYPE + RULE_METHOD_SUFFIX, testDTI().primitiveOf("merge"));

    Data multipleSnapshotsWithUnmergedField =
        toData(
            "["
                + "  {"
                + "    \"fieldOne\": 1.0," // Merges with 'fieldOne' default field merge rule.
                + "    \"fieldTwo\": \"a\"," // Merges with 'Primitive' default type merge rule.
                + "    \"subjectReference\": 1.0," // Merges by itself - no choice field matches.
                + "    \"timingAge\": 25.0" // Merges with 'timingDateTime' from second snapshot.
                + "  },"
                + "  {"
                + "    \"fieldOne\": 2.0,"
                + "    \"fieldTwo\": \"b\","
                + "    \"timingDateTime\": \"08/18/2021\","
                + "    \"productReference\": 2.0"
                + "  }"
                + "]");

    Data expected =
        toData(
            "{"
                + "\"id\": \"001\","
                + "\"fieldOne\": 2.0,"
                + "\"fieldTwo\": \"b\","
                + "\"subjectReference\": 1.0,"
                + "\"timingDateTime\": \"08/18/2021\","
                + "\"productReference\": 2.0"
                + "}");

    assertEquals(
        expected, merge(context, multipleSnapshotsWithUnmergedField.deepCopy(), resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_identifierSystemFieldPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "["
                + "  {"
                + "    \"identifier\": ["
                + "      {"
                + "        \"system\":\"urn:oid:google/reconciliation-stable-id\""
                + "      }"
                + "    ],"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"A\""
                + "  }"
                + "]");

    // expected:
    // {"resource": {"fieldOne": 0., "fieldTwo": "A", "id": "001", "identifier": [] }}
    // ** NOTE ** : The "identifier" field mapped to an empty Array (NullData) will be removed by
    //              wstl automagically when it leaves the Java environment (before writing to the
    //              final FHIR store).
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);
    expected = expected.asContainer().setField("identifier", testDTI().emptyArray());

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_fhirVersionSystemPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "["
                + "  {"
                + "    \"identifier\": ["
                + "      {"
                + "        \"system\":\"urn:oid:google/reconciliation-stable-id\""
                + "      }"
                + "    ],"
                + "    \"meta\": {"
                + "      \"tag\": ["
                + "        {"
                + String.format("          \"system\": \"%s\"", FHIR_VERSION_SOURCE_SYSTEM)
                + "        }"
                + "      ]"
                + "    },"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"A\""
                + "  }"
                + "]");

    // expected:
    // {"resource": {"fieldOne": 0., "fieldTwo": "A", "id": "001", "identifier": [],
    // "meta": {"tag": []} }}
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);
    expected =
        expected
            .asContainer()
            .setField("identifier", testDTI().emptyArray())
            .setField(
                META_FIELD,
                testDTI().containerOf(ImmutableMap.of(TAG_FIELD, testDTI().emptyArray())));

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  // LINT.IfChange(source_systems)
  @Test
  public void clearHDEMetadata_inputResourceWithSourceSystem_clears() {
    Data resourceWithHdeMetadata =
        toData(
            "{"
                + "  \"meta\": {"
                + "    \"tag\": ["
                + "      {"
                + String.format("        \"system\": \"%s\"", FHIR_VERSION_SOURCE_SYSTEM)
                + "      },"
                + "      {"
                + String.format("        \"system\": \"%s\"", HL7V2_SOURCE_SYSTEM)
                + "      },"
                + "      {"
                + String.format("        \"system\": \"%s\"", CLOUD_SPANNER_SOURCE_SYSTEM)
                + "      },"
                + "      {"
                + String.format("        \"system\": \"%s\"", BIGQUERY_SOURCE_SYSTEM)
                + "      }"
                + "    ]"
                + "  },"
                + "  \"fieldOne\":0.0,"
                + "  \"fieldTwo\":\"C\""
                + "}");

    Data expected =
        toData(
            "{"
                + "  \"meta\": {"
                + "    \"tag\": ["
                + "    ]"
                + "  },"
                + "  \"fieldOne\":0.0,"
                + "  \"fieldTwo\":\"C\""
                + "}");

    assertEquals(
        expected,
        mergeResources.prepareFinalResource(context, resourceWithHdeMetadata.asContainer(), null));
  }

  // LINT.ThenChange()

  @Test
  public void mergeResources_cleanResource_singleIdentifierStableId_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "["
                + "  {"
                + "    \"identifier\": {"
                + "        \"system\":\"urn:oid:google/reconciliation-stable-id\""
                + "    },"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"A\""
                + "  }"
                + "]");
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);
    expected = expected.asContainer().removeField("identifier");

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_singleIdentifierNotStableId_notFiltered()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "["
                + "  {"
                + "    \"identifier\": {"
                + "        \"system\":\"urn:oid:google/not-stable-id\""
                + "    },"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"A\""
                + "  }"
                + "]");
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_metaTagfilteredOut() throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithMetaTagField =
        toData(
            "["
                + "  {"
                + "  \"meta\": {"
                + "    \"tag\": ["
                + "      {"
                + "        \"system\":\"urn:oid:data-type/data-source\","
                + "        \"code\":\"csv/datasource1\""
                + "      }"
                + "    ]"
                + "  },"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"A\""
                + "  }"
                + "]");

    // expected:
    // {"resource":{"fieldOne": 0., "fieldTwo": "A", "id": "001", "meta": {tag[]} }}
    Data expected =
        addStableIdToNewResource(snapshotWithMetaTagField.asArray().getElement(0), stableId);
    expected
        .asContainer()
        .getField(META_FIELD)
        .asContainer()
        .setField(TAG_FIELD, testDTI().emptyArray());

    assertEquals(expected, merge(context, snapshotWithMetaTagField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_metaTagSystemFieldPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "["
                + "  {"
                + "    \"meta\":{"
                + "      \"tag\":["
                + "        {"
                + "          \"system\":\"urn:oid:google/reconciliation-stable-id\","
                + "          \"code\":\"1234\""
                + "        }"
                + "      ]"
                + "    },"
                + "    \"field1\":0,"
                + "    \"field2\":\"A\","
                + "    \"id\":\"001\""
                + "  }"
                + "]");

    // expected:
    // {"resource": {"fieldOne": 0., "fieldTwo": "A", "id": "001", "meta": {"tag": []} }}
    // ** NOTE ** : The "identifier" field mapped to an empty Array (NullData) will be removed by
    //              wstl automagically when it leaves the Java environment (before writing to the
    //              final FHIR store).
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);
    expected
        .asContainer()
        .getField(META_FIELD)
        .asContainer()
        .setField(TAG_FIELD, testDTI().emptyArray());

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_identifierSystemFieldNotPresent_noFiltering()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithStableIdField =
        toData(
            "[\n"
                + "  {\n"
                + "    \"identifier\": [\n"
                + "      {\n"
                + "        \"notSystem\":\"urn:oid:google/reconciliation-stable-id\"\n"
                + "      }\n"
                + "    ],\n"
                + "    \"fieldOne\":0.0,\n"
                + "    \"fieldTwo\":\"A\"\n"
                + "  }\n"
                + "]");

    // expected:
    // {"resource":
    //  {"fieldOne": 0.,
    //  "fieldTwo": "A",
    //  "id": "001",
    //  "identifier": [{"notSystem": "urn:oid:google/reconciliation-stable-id"}] }}
    Data expected =
        addStableIdToNewResource(snapshotWithStableIdField.asArray().getElement(0), stableId);

    assertEquals(expected, merge(context, snapshotWithStableIdField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_extensionTimestampFieldPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithTimestampExtensionField =
        toData(
            "["
                + "  {"
                + "    \"extension\": ["
                + "      {"
                + "        \"url\":\"urn:oid:google/reconciliation-timestamp\""
                + "      }"
                + "    ],"
                + ""
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"C\""
                + "  }"
                + "]");

    // expected:
    // {"resource": {"fieldOne": 0., "fieldTwo": "C", id: "001", "extension": [] }}
    // ** NOTE ** : The "extension" field mapped to an empty Array (NullData) will be removed by
    //              wstl automagically when it leaves the Java environment (before writing to the
    //              final FHIR store).
    Data expected =
        addStableIdToNewResource(
            snapshotWithTimestampExtensionField.asArray().getElement(0), stableId);
    expected = expected.asContainer().setField("extension", testDTI().emptyArray());

    assertEquals(expected, merge(context, snapshotWithTimestampExtensionField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_metaExtensionTimestampFieldPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithTimestampExtensionField =
        toData(
            "["
                + "  {"
                + "    \"meta\": {"
                + "      \"extension\": ["
                + "        {"
                + "          \"url\":\"urn:oid:google/reconciliation-timestamp\""
                + "        }"
                + "      ]"
                + "    },"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"C\""
                + "  }"
                + "]");

    Data expected =
        addStableIdToNewResource(
            snapshotWithTimestampExtensionField.asArray().getElement(0), stableId);
    expected = expected.asContainer().removeField("meta");

    assertEquals(expected, merge(context, snapshotWithTimestampExtensionField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanTwoResource_bothTimestampFieldsPresent_filteredOut()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    // Technically no single snapshot will look like this but the merge result from two snapshots
    // could. This test avoids the merge step and just provides input as a single resource.
    Data snapshotsWithTimestampExtensionField =
        toData(
            "["
                + "  {"
                + "    \"meta\": {"
                + "      \"extension\": ["
                + "        {"
                + "          \"url\":\"urn:oid:google/reconciliation-timestamp\""
                + "        }"
                + "      ]"
                + "    },"
                + "    \"extension\": ["
                + "      {"
                + "        \"url\":\"urn:oid:google/reconciliation-timestamp\""
                + "      }"
                + "    ],"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"C\""
                + "  }"
                + "]");

    Data expected =
        addStableIdToNewResource(
            snapshotsWithTimestampExtensionField.asArray().getElement(0), stableId);
    expected = expected.asContainer().removeField("meta").asContainer().removeField("extension");

    assertEquals(expected, merge(context, snapshotsWithTimestampExtensionField, resourceInfo));
  }

  @Test
  public void mergeResources_cleanResource_extensionUrlFieldNotPresent_noFiltering()
      throws ConfigurationException {
    String stableId = "001";
    Data resourceInfo = buildResourceInfo("Patient", stableId);
    setWstlFunctionInContext(context, "PatientRule", testDTI().primitiveOf("latest"));

    Data snapshotWithTimestampExtensionField =
        toData(
            "["
                + "  {"
                + "    \"extension\": ["
                + "      {"
                + "        \"notUrl\":\"anyValue\""
                + "      }"
                + "    ],"
                + "    \"fieldOne\":0.0,"
                + "    \"fieldTwo\":\"C\""
                + "  }"
                + "]");

    // expected:
    // {"resource": {"fieldOne": 0., "fieldTwo": "C", id: "001", "extension": [{"notUrl":
    // "anyValue"] }}
    // ** NOTE ** : The "extension" field mapped to an empty Array (NullData) will be removed by
    //              wstl automagically when it leaves the Java environment (before writing to the
    //              final FHIR store).
    Data expected =
        addStableIdToNewResource(
            snapshotWithTimestampExtensionField.asArray().getElement(0), stableId);

    assertEquals(expected, merge(context, snapshotWithTimestampExtensionField, resourceInfo));
  }

  private Data merge(RuntimeContext ctx, Data sortedSnapshots, Data resourceInfo)
      throws ConfigurationException {
    return mergeResources.mergeResources(ctx, sortedSnapshots, resourceInfo);
  }

  private Data buildResourceInfo(String resourceType, String stableId) {
    return testDTI()
        .containerOf(
            ImmutableMap.of(
                "resourceType",
                testDTI().primitiveOf(resourceType),
                "stableId",
                testDTI().primitiveOf(stableId)));
  }
}
