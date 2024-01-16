// Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/** Constants for merging module within FHIR reconciliation. */
public final class MergeConstants {
  public static final String ARRAY_TYPE = "Array";
  public static final String CONTAINER_TYPE = "Container";
  public static final String PRIMITIVE_TYPE = "Primitive";

  public static final String DEFAULT_FIELD_RULES_METHOD = "DefaultFieldRules";
  public static final String DEFAULT_RESOURCE_TYPE = "Default";
  public static final String MERGE_METHOD_SUFFIX = "Merge";
  public static final String RULE_METHOD_SUFFIX = "Rule";

  public static final String LATEST = "latest";
  public static final String MERGE = "merge";
  public static final ImmutableSet<String> RESOURCE_MERGE_RULES = ImmutableSet.of(LATEST, MERGE);

  public static final String FORCE_INBOUND_RULE = "forceInbound";
  public static final String PREFER_INBOUND_RULE = "preferInbound";
  public static final String UNION_BY_FIELD_RULE = "unionByField";
  public static final String UNION_RULE = "union";

  public static final String META_FIELD = "meta";
  public static final String EXTENSION_FIELD = "extension";
  public static final String PATH_FIELD = "path";
  public static final String ID_FIELD = "id";
  public static final String RESOURCE_TYPE_FIELD = "resourceType";
  public static final String RULE_FIELD = "rule";
  public static final String STABLE_ID_FIELD = "stableId";
  public static final String URL_FIELD = "url";
  public static final String MERGE_RULES_PACKAGE = "merge_rules";
  public static final String RECON_TIMESTAMP_URL = "urn:oid:google/reconciliation-timestamp";
  public static final String DATA_TYPE_DATA_SOURCE_URL = "urn:oid:data-type/data-source";
  public static final String EXTERNAL_ID_URL_SUFFIX = "reconciliation-external-id";
  public static final String CREATE_TIME_URL = "urn:oid:google/create-time";
  public static final String FINAL_LATEST_SOURCE_TS_URL = "urn:oid:google/latest-source-timestamp";
  public static final String FINAL_LATEST_SYSTEM_TS_URL = "urn:oid:google/latest-system-timestamp";
  public static final String LAST_RECON_TIMESTAMP_FIELD = "lastReconTimestamp";
  public static final String LAST_CREATE_TIME_FIELD = "lastCreateTime";
  public static final String LAST_DATA_SOURCE_FIELD = "lastDataSource";
  // The number of snapshots required to trigger fetching reconciliation checkpoint.
  // Represents trade-off between extra FHIR operations and reduced merge latency.
  public static final int RECON_CHECKPOINT_SNAPSHOT_CUTOFF_DEFAULT = 100;
  // The number of snapshots required to trigger fetching reconciliation checkpoint for clinical
  // FHIR history. Represents trade-off between extra FFS FHIR read operations and reduced merge
  // results/latency.
  public static final int RECON_CHECKPOINT_SNAPSHOT_CUTOFF_DEFAULT_FOR_HISTORY = 5;
  // Optional environment variable name to override the default snapshot cutoff
  public static final String RECON_CHECKPOINT_SNAPSHOT_CUTOFF = "RECON_CHECKPOINT_SNAPSHOT_CUTOFF";
  public static final String LIST_RESOURCE_TYPE = "List";
  public static final String LINKAGE_RESOURCE_TYPE = "Linkage";
  public static final String PERSON_RESOURCE_TYPE = "Person";

  public static final String VALUESTRING_FIELD = "valueString";
  public static final String VALUEINSTANT_FIELD = "valueInstant";
  public static final String ORIGINAL_PUBSUB_MESSAGE = "originalPubSubMessage";
  public static final String METADATA = "metadata";
  public static final String LIST_PURPOSE_URL = "list-purpose";
  public static final String LINKAGE_PURPOSE_VALUESTRING = "linkage-purpose";
  public static final String LINKAGE_IDS_FIELD = "linkageIds";
  public static final String LINKAGE_ID_FIELD = "linkageId";
  public static final String PERSON_PURPOSE_VALUESTRING = "person-purpose";
  public static final String MODE_FIELD = "mode";
  public static final String SNAPSHOT_MODE = "snapshot";
  public static final String RESOURCE_FIELD = "resource";
  public static final String TARGET_FIELD = "target";
  public static final String SOURCE_FIELD = "source";
  public static final String DELETED_RESOURCE_FIELD = "deletedResource";
  public static final String RESOURCE_INFO_FIELD = "resourceInfo";
  public static final String ACTION_FIELD = "action";
  public static final String LAST_UPDATED_FIELD = "lastUpdated";
  public static final String DELETE_RESOURCE = "DeleteResource";
  public static final String UPDATE_RESOURCE = "UpdateResource";
  public static final String DELETE_FINAL_RESOURCE_ID = "deleteFinalId";
  public static final String RECON_TIMESTAMP_FIELD = "reconTimestamp";
  public static final String DELETED_SNAPSHOTS = "deletedSnapshots";
  public static final String EARLIEST_MODIFIED_RECON_TIMESTAMP = "earliestReconTimestamp";
  public static final String IS_DELETED_SNAPSHOT_FIELD = "isDeletedSnapshot";

  public static final String SYNTHETIC_FIELD = "synthetic";
  public static final String OUTDATED_SYNTHETIC_FIELD = "outdatedSynthetic";

  public static final String REFERENCE_FIELD = "reference";
  public static final String ENTRY_FIELD = "entry";
  public static final String ITEM_FIELD = "item";
  public static final String STATUS_FIELD = "status";
  public static final String VERSION_ID_FIELD = "versionId";

  public static final String SERVICE_REQUEST_RESOURCE_TYPE = "ServiceRequest";

  public static final String REASON_REFERENCE_FIELD = "reasonReference";

  public static final String ENTERED_IN_ERROR_STATUS = "entered-in-error";

  public static final String REVOKED_STATUS = "revoked";

  public static final String ENABLE_MAPPING_SIDE_EFFECTS = "ENABLE_MAPPING_SIDE_EFFECTS";

  public static final String ENABLE_CLINICAL_HISTORY = "ENABLE_CLINICAL_HISTORY";

  public static final String RECON_ERRORS_LIST_FIELD = "reconciliationErrorsList";
  public static final String RECON_ERROR_MESSAGE = "reconciliationErrorMessage";
  public static final String NEXT_TIMESTAMP_FIELD = "nextTimestamp";
  public static final String CURRENT_TIMESTAMP_FIELD = "currentTimestamp";
  public static final String CURRENT_MASTER_ID_FIELD = "currentMasterId";
  public static final String PREVIOUS_MASTER_ID_FIELD = "previousMasterId";
  public static final String INVALID_CUTOFF_ERROR_STRING =
      "RECON_CHECKPOINT_SNAPSHOT_CUTOFF is not an valid number, got \"%s\".";
  public static final String RI_ASSERT_NONNULL_ERROR_STRING =
      "ResourceInfo must be non-null; check input dataset.";
  public static final String RI_ASSERT_ARRAY = "ResourceInfo must be Array, got : \"%s\".";
  public static final String RI_ASSERT_CONTAINER_ERROR_STRING =
      "ResourceInfo element must be a non-empty container, got element: %s from resourceInfo: %s";
  public static final String ASSERT_RECON_TS_ERROR_STRING =
      "Could not determine reconciliation timestamp of snapshot \"%s\".";
  public static final String NULL_SEARCH_INPUT_ERROR_STRING =
      "Input to map intermediate snapshots was null, check previous reconciliation steps.";
  public static final String ASSERT_NONNULL_SNAPSHOT_ITERABLE_ERROR_STRING =
      "Iterable in PCollectionDataset is null. Check previous pipeline steps.";
  public static final String ASSERT_NONNULL_SNAPSHOT_ERROR_STRING =
      "Snapshot is null, check previous pipeline steps. This could indicate an issue"
          + " with the mapping of the iterable in a previous DoFn.";
  public static final String ASSERT_NONEMPTY_RESOURCE_SNAPSHOT_MAP_ERROR_STRING =
      "Expected non-empty \"resource\" field on intermediate resource snapshot, got: %s";
  public static final String PARAM_MAPPING_FAILED_FIELD =
      "Mapping ResourceInfos to FhirSearchParameters failed.";
  public static final String SNAPSHOT_SEARCH_FAILED_FIELD =
      "Mapping FhirSearchParameters to intermediate snapshots failed.";
  public static final String MERGE_RESOURCES_FAILED_FIELD =
      "Merging intermediate snapshots failed.";
  public static final String LIST_PURPOSE_VALUESTRING =
      "urn:oid:google/hl7v2/allergy-intolerance-list";
  public static final String LINKAGE_PURPOSE_URL =
      "urn:oid:google/streaming-empi/patient-masterid-linkage";
  public static final String PERSON_PURPOSE_URL = "urn:oid:google/empi-person";
  public static final String EMPI_INFO_FIELD = "empi_info";
  public static final String FHIR_VERSION_SOURCE_SYSTEM =
      "urn:oid:google/source-fhir-resource-version-name";

  public static final String FIRST_MERGE_NULL_SNAPSHOT = "enable_first_merge_null_snapshot";

  public static final Data REMOVE_FIELD_PLACEHOLDER =
      new DefaultContainer(
          ImmutableMap.of("merge-placeholder", new DefaultPrimitive("REMOVE_FIELD_PLACEHOLDER")));

  // All choiceX fields, by resource type, for each of the FHIR resources (145) in the form:
  //    Map<String of the FHIR resource type, ChoiceFieldGroup of choiceX fields for the resource>
  public static final ImmutableMap<String, ChoiceFieldGroup> CHOICE_FIELDS =
      ImmutableMap.<String, ChoiceFieldGroup>builder()
          // ActivityDefinition
          .put(
              "ActivityDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .withGroup(
                      "timingTiming",
                      "timingDateTime",
                      "timingAge",
                      "timingPeriod",
                      "timingRange",
                      "timingDuration")
                  .withGroup("productReference", "productCodeableConcept")
                  .build())
          // AllergyIntolerance
          .put(
              "AllergyIntolerance",
              ChoiceFieldGroup.builder()
                  .withGroup(
                      "onsetDateTime", "onsetAge", "onsetPeriod", "onsetRange", "onsetString")
                  .build())
          // ChargeItem
          .put(
              "ChargeItem",
              ChoiceFieldGroup.builder()
                  .withGroup("occurrencePeriod", "occurrenceDateTime", "occurrenceTiming")
                  .withGroup("productReference", "productCodeableConcept")
                  .build())
          // ClinicalImpression
          .put(
              "ClinicalImpression",
              ChoiceFieldGroup.builder().withGroup("effectiveDateTime", "effectivePeriod").build())
          // CommunicationRequest
          .put(
              "CommunicationRequest",
              ChoiceFieldGroup.builder()
                  .withGroup("occurrencePeriod", "occurrenceDateTime")
                  .build())
          // ConceptMap
          .put(
              "ConceptMap",
              ChoiceFieldGroup.builder()
                  .withGroup("sourceUri", "sourceCanonical")
                  .withGroup("targetUri", "targetCanonical")
                  .build())
          // Condition
          .put(
              "Condition",
              ChoiceFieldGroup.builder()
                  .withGroup(
                      "onsetDateTime", "onsetAge", "onsetPeriod", "onsetRange", "onsetString")
                  .withGroup(
                      "abatementDateTime",
                      "abatementAge",
                      "abatementPeriod",
                      "abatementRange",
                      "abatementString")
                  .build())
          // Consent
          .put(
              "Consent",
              ChoiceFieldGroup.builder().withGroup("sourceAttachment", "sourceReference").build())
          // Contract
          .put(
              "Contract",
              ChoiceFieldGroup.builder()
                  .withGroup("topicCodeableConcept", "topicReference")
                  .withGroup("legallyBindingAttachment", "legallyBindingReference")
                  .build())
          // CoverageEligibilityRequest
          .put(
              "CoverageEligibilityRequest",
              ChoiceFieldGroup.builder().withGroup("servicedDate", "servicedPeriod").build())
          // CoverageEligibilityResponse
          .put(
              "CoverageEligibilityResponse",
              ChoiceFieldGroup.builder().withGroup("servicedDate", "servicedPeriod").build())
          // DetectedIssue
          .put(
              "DetectedIssue",
              ChoiceFieldGroup.builder()
                  .withGroup("identifiedDateTime", "identifiedPeriod")
                  .build())
          // DeviceDefinition
          .put(
              "DeviceDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("manufacturerString", "manufacturerReference")
                  .build())
          // DeviceRequest
          .put(
              "DeviceRequest",
              ChoiceFieldGroup.builder()
                  .withGroup("codeReference", "codeCodeableConcept")
                  .withGroup("valueQuantity", "valueCodeableConcept", "valueBoolean", "valueRange")
                  .withGroup("occurrencePeriod", "occurrenceDateTime", "occurrenceTiming")
                  .build())
          // DeviceUseStatement
          .put(
              "DeviceUseStatement",
              ChoiceFieldGroup.builder()
                  .withGroup("timingTiming", "timingPeriod", "timingDateTime")
                  .build())
          // DiagnosticReport
          .put(
              "DiagnosticReport",
              ChoiceFieldGroup.builder().withGroup("effectiveDateTime", "effectivePeriod").build())
          // EventDefinition
          .put(
              "EventDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .build())
          // FamilyMemberHistory
          .put(
              "FamilyMemberHistory",
              ChoiceFieldGroup.builder()
                  .withGroup("bornPeriod", "bornDate", "bornString")
                  .withGroup("ageAge", "ageRange", "ageString")
                  .withGroup(
                      "deceasedBoolean",
                      "deceasedAge",
                      "deceasedRange",
                      "deceasedDate",
                      "deceasedString")
                  .build())
          // Goal
          .put(
              "Goal",
              ChoiceFieldGroup.builder().withGroup("startDate", "startCodeableConcept").build())
          // GuidanceResponse
          .put(
              "GuidanceResponse",
              ChoiceFieldGroup.builder()
                  .withGroup("moduleUri", "moduleCanonical", "moduleCodeableConcept")
                  .build())
          // Immunization
          .put(
              "Immunization",
              ChoiceFieldGroup.builder().withGroup("occurredString", "occurredDateTime").build())
          // ImmunizationEvaluation
          .put(
              "ImmunizationEvaluation",
              ChoiceFieldGroup.builder()
                  .withGroup("doseNumberPositiveInt", "doseNumberString")
                  .withGroup("seriesDosesPositiveInt", "seriesDosesString")
                  .build())
          // Library
          .put(
              "Library",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .build())
          // Measure
          .put(
              "Measure",
              ChoiceFieldGroup.builder().withGroup("occurredPeriod", "occurredDateTime").build())
          // Media
          .put(
              "Media",
              ChoiceFieldGroup.builder().withGroup("createdDateTime", "createdPeriod").build())
          // MedicationAdministration
          .put(
              "MedicationAdministration",
              ChoiceFieldGroup.builder()
                  .withGroup("medicationCodeableConcept", "medicationReference")
                  .withGroup("effectiveDateTime", "effectivePeriod")
                  .build())
          // MedicationDispense
          .put(
              "MedicationDispense",
              ChoiceFieldGroup.builder()
                  .withGroup("statusReasonCodeableConcept", "statusReasonReference")
                  .withGroup("medicationCodeableConcept", "medicationReference")
                  .build())
          // MedicationRequest
          .put(
              "MedicationRequest",
              ChoiceFieldGroup.builder()
                  .withGroup("reportedBoolean", "reportedReference")
                  .withGroup("medicationCodeableConcept", "medicationReference")
                  .build())
          // MedicationStatement
          .put(
              "MedicationStatement",
              ChoiceFieldGroup.builder()
                  .withGroup("medicationCodeableConcept", "medicationReference")
                  .withGroup("effectiveDateTime", "effectivePeriod")
                  .build())
          // MessageDefinition
          .put(
              "MessageDefinition",
              ChoiceFieldGroup.builder().withGroup("eventCoding", "eventUri").build())
          // MessageHeader
          .put(
              "MessageHeader",
              ChoiceFieldGroup.builder().withGroup("eventCoding", "eventUri").build())
          // Observation
          .put(
              "Observation",
              ChoiceFieldGroup.builder()
                  .withGroup(
                      "effectiveDateTime", "effectivePeriod", "effectiveTiming", "effectiveInstant")
                  .withGroup(
                      "valueQuantity",
                      "valueCodeableConcept",
                      "valueString",
                      "valueBoolean",
                      "valueInteger",
                      "valueRange",
                      "valueRatio",
                      "valueSampledData",
                      "valueTime",
                      "valueDateTime",
                      "valuePeriod")
                  .build())
          // Patient
          .put(
              "Patient",
              ChoiceFieldGroup.builder()
                  .withGroup("deceasedBoolean", "deceasedDateTime")
                  .withGroup("multipleBirthBoolean", "multipleBirthInteger")
                  .build())
          // PlanDefinition
          .put(
              "PlanDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .build())
          // Procedure
          .put(
              "Procedure",
              ChoiceFieldGroup.builder()
                  .withGroup(
                      "performedDateTime",
                      "performedPeriod",
                      "performedString",
                      "performedAge",
                      "performedRange")
                  .build())
          // Provenance
          .put(
              "Provenance",
              ChoiceFieldGroup.builder().withGroup("occurredPeriod", "occurredDateTime").build())
          // ResearchDefinition
          .put(
              "ResearchDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .build())
          // ResearchElementDefinition
          .put(
              "ResearchElementDefinition",
              ChoiceFieldGroup.builder()
                  .withGroup("subjectCodeableConcept", "subjectReference")
                  .build())
          // RiskAssessment
          .put(
              "RiskAssessment",
              ChoiceFieldGroup.builder().withGroup("occurredPeriod", "occurredDateTime").build())
          // ServiceRequest
          .put(
              "ServiceRequest",
              ChoiceFieldGroup.builder()
                  .withGroup("occurrencePeriod", "occurrenceDateTime", "occurrenceTiming")
                  .withGroup("quantityQuantity", "quantityRatio", "quantityRange")
                  .withGroup("asNeededBoolean", "asNeededCodeableConcept")
                  .build())
          // Structure
          .put(
              "SupplyRequest",
              ChoiceFieldGroup.builder()
                  .withGroup("itemCodeableConcept", "itemReference")
                  .withGroup("occurrencePeriod", "occurrenceDateTime", "occurrenceTiming")
                  .build())
          // SupplyDelivery
          .put(
              "SupplyDelivery",
              ChoiceFieldGroup.builder()
                  .withGroup("itemCodeableConcept", "itemReference")
                  .withGroup("occurrencePeriod", "occurrenceDateTime", "occurrenceTiming")
                  .build())
          .buildOrThrow();

  private MergeConstants() {}
}
