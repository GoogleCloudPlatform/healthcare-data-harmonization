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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CREATE_TIME_URL;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DATA_TYPE_DATA_SOURCE_URL;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.EXTENSION_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.EXTERNAL_ID_URL_SUFFIX;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.ID_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.META_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RECON_TIMESTAMP_URL;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.REMOVE_FIELD_PLACEHOLDER;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RESOURCE_TYPE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.STABLE_ID_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.URL_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.IDENTIFIER_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.STABLE_ID_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.SYSTEM_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.TAG_FIELD;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMerger;
import java.io.Serializable;
import java.util.ArrayList;
import javax.naming.ConfigurationException;

/** MergeResourcesBase defines common merging utilities. */
public class MergeResourcesBase implements Serializable {
  private final MergeConfigExecutor mergeConfigExecutor;

  public MergeResourcesBase(MergeConfigExecutor mergeConfigExecutor) {
    this.mergeConfigExecutor = mergeConfigExecutor;
  }

  // Executes merge on pair of resources during the iterative reconciliation of resource snapshots.
  // Try and call two-parameter merge Closure given the package, resource name, existing resource,
  // and inbound resource. If there is no resource-type-specific merge rule configured, then a
  // per-field merge is completed using the default field-level merge rules. Merges returning empty
  // or non-Container results throw a ConfigurationException alerting improper configuration.
  // Set the default visibility to be package visibility.
  public Container merge(
      RuntimeContext ctx,
      Container existing,
      Container inbound,
      String resourceType,
      DefaultMerger defaultMerger)
      throws ConfigurationException {
    Data mergeResult;
    try {
      mergeResult = mergeConfigExecutor.mergeResources(ctx, resourceType, existing, inbound);
    } catch (NoMatchingOverloadsException e) {
      // If no resource-type-specific merge rule is configured, use the Default field-level rules.
      mergeResult = defaultMerger.mergeWithDefaultFieldRules(ctx, existing, inbound, resourceType);
    }

    return mergeResult.asContainer();
  }

  // Cleans the merged resource by sorting out the reconciliation-specific fields.
  public Container prepareFinalResource(RuntimeContext ctx, Container merged, String stableId) {
    merged = removeStableIdMeta(ctx, merged);
    Data metaTags = merged.getField(META_FIELD).asContainer().getField(TAG_FIELD);
    if (!metaTags.isNullOrEmpty()) {
      // Remove HDE reconciliation metadata (except checkpointing)
      merged = clearResourceMetadata(ctx, merged).asContainer();
    }

    // Remove the reconciliation timestamp field from the extension array.
    merged = clearResourceReconciliationTimestamp(ctx, merged).asContainer();
    // Assign stable-id to the 'id' field. If it's null, this is a bypass reconciliation
    // resource, and the id field should be left as-is.
    if (stableId != null) {
      merged = merged.setField(ID_FIELD, ctx.getDataTypeImplementation().primitiveOf(stableId));
    }

    return removeClearFieldPlaceholders(merged);
  }

  private static Container removeStableIdMeta(RuntimeContext ctx, Container resource) {
    // Remove stable-id field from the identifier array.
    Data identifiers = resource.getField(IDENTIFIER_FIELD);
    if (identifiers.isNullOrEmpty()) {
      return resource;
    }
    // TODO: b/287477741 create and use a list of resourceTypes with 0..1 or 0..* Identifier
    if (identifiers.isArray()) {
      resource =
          resource.setField(
              IDENTIFIER_FIELD,
              removeArrayElement(ctx, identifiers, SYSTEM_FIELD, STABLE_ID_SYSTEM));
    }
    // TODO: b/287079710 add integration test with non-Array identifier
    // If identifier is not 0...* (e.g. Composition, Bundle), check if it's a stable id
    if (identifiers.isContainer()) {
      Primitive system = identifiers.asContainer().getField(SYSTEM_FIELD).asPrimitive();
      if (system != null && system.string() != null && system.string().equals(STABLE_ID_SYSTEM)) {
        resource = resource.removeField(IDENTIFIER_FIELD);
      }
    }
    return resource;
  }

  /**
   * Clears the reconciliation timestamp from the provided FHIR resource's 'extension' field, Clears
   * "urn:oid:data-type/data-source", "reconciliation-external-id" from meta.tag, Clears
   * "urn:oid:google/create-time" from meta.extension.
   *
   * @param ctx {@link RuntimeContext} object for access to a {@link
   *     com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation}.
   * @param resource {@link Data} Container object holding a FHIR resource.
   * @return The passed FHIR resource with no reconciliation timestamp.
   */
  public static Data clearResourceMetadata(RuntimeContext ctx, Data resource) {
    Container validatedResource = validateNonEmptyContainer(resource, "clearHDEMetadata");
    Data cleared = clearMetaExtensionField(ctx, validatedResource, URL_FIELD, CREATE_TIME_URL);
    cleared = clearMetaTagField(ctx, cleared, SYSTEM_FIELD, DATA_TYPE_DATA_SOURCE_URL);
    cleared = clearMetaTagField(ctx, cleared, SYSTEM_FIELD, EXTERNAL_ID_URL_SUFFIX);
    cleared = clearMetaTagField(ctx, cleared, SYSTEM_FIELD, STABLE_ID_SYSTEM);
    return cleared;
  }

  protected static void validateIncrementalResourceNotEmpty(Data resource, String resourceType) {
    if (resource.isNullOrEmpty() || !resource.isContainer()) {
      throw new IllegalArgumentException(
          String.format(
              "Result of merging operation on resource of type '%s' produced empty or non-Container"
                  + " result, indicating misconfiguration of the merge rules for this resource type"
                  + " or the default merge rules. Merge result: '%s'.",
              resourceType, resource));
    }
  }

  /**
   * Checks that a resource is a non-empty {@link Container} and is not {@link NullData}, and throws
   * IllegalArgumentException otherwise.
   *
   * @param resource a resource to check
   */
  public static Container validateNonEmptyContainer(Data resource, String operationOnResource) {
    if (resource.isNullOrEmpty() || !resource.isContainer()) {
      throw new IllegalArgumentException(
          String.format(
              "Input resource to %s must be a non-empty Container. Got: %s",
              operationOnResource, resource));
    }
    return resource.asContainer();
  }

  /**
   * Clears the reconciliation timestamp from the provided FHIR resource's 'extension' field.
   *
   * @param ctx {@link RuntimeContext} object for access to a {@link
   *     com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation}.
   * @param resource {@link Data} Container object holding a FHIR resource.
   * @return The passed FHIR resource with no reconciliation timestamp.
   */
  public static Data clearResourceReconciliationTimestamp(RuntimeContext ctx, Data resource) {
    Container resourceContainer =
        validateNonEmptyContainer(resource, "clearReconciliationTimestamp");
    Container meta =
        clearReconciliationTimestampFromRoot(
            ctx, resourceContainer.getField(META_FIELD).asContainer());
    if (!meta.isNullOrEmpty()) {
      resourceContainer.setField(META_FIELD, meta);
    }
    return clearReconciliationTimestampFromRoot(ctx, resourceContainer);
  }

  private static Container clearReconciliationTimestampFromRoot(
      RuntimeContext ctx, Container root) {
    Data extensions = root.asContainer().getField(EXTENSION_FIELD);
    if (!extensions.isNullOrEmpty()) {
      return root.asContainer()
          .setField(
              EXTENSION_FIELD, removeArrayElement(ctx, extensions, URL_FIELD, RECON_TIMESTAMP_URL));
    }
    return root;
  }

  public static Data clearMetaTagField(
      RuntimeContext ctx, Data resource, String keyField, String valueToFilter) {
    Container resourceContainer = resource.asContainer();
    Container meta =
        clearTagFieldFromRoot(
            ctx, resourceContainer.getField(META_FIELD).asContainer(), keyField, valueToFilter);
    if (!meta.isNullOrEmpty()) {
      resourceContainer.setField(META_FIELD, meta);
    }
    return resourceContainer;
  }

  private static Container clearTagFieldFromRoot(
      RuntimeContext ctx, Container root, String keyField, String valueToFilter) {
    Data tag = root.asContainer().getField(TAG_FIELD);
    if (!tag.isNullOrEmpty()) {
      return root.asContainer()
          .setField(TAG_FIELD, removeArrayElement(ctx, tag, keyField, valueToFilter));
    }
    return root;
  }

  private static Data clearMetaExtensionField(
      RuntimeContext ctx, Container resourceContainer, String keyField, String valueToFilter) {
    Container meta =
        clearExtensionFieldFromRoot(
            ctx, resourceContainer.getField(META_FIELD).asContainer(), keyField, valueToFilter);
    if (!meta.isNullOrEmpty()) {
      resourceContainer.setField(META_FIELD, meta);
    }
    return resourceContainer;
  }

  private static Container clearExtensionFieldFromRoot(
      RuntimeContext ctx, Container root, String keyField, String valueToFilter) {
    Data tag = root.asContainer().getField(EXTENSION_FIELD);
    if (!tag.isNullOrEmpty()) {
      return root.asContainer()
          .setField(EXTENSION_FIELD, removeArrayElement(ctx, tag, keyField, valueToFilter));
    }
    return root;
  }

  /**
   * Clear field placeholders are added any time we write null to a field when applying merge rules
   * this is done so we can tell the difference between a merge rule not being configured for a
   * field vs a merge rule purposely writing null to a field.
   */
  private static Container removeClearFieldPlaceholders(Container resource) {
    ArrayList<String> fieldsToRemove = new ArrayList<>();
    for (String field : resource.fields()) {
      if (resource.getField(field).equals(REMOVE_FIELD_PLACEHOLDER)) {
        fieldsToRemove.add(field);
      }
    }
    for (String field : fieldsToRemove) {
      resource = resource.removeField(field);
    }
    return resource;
  }

  // Trims a specific element from an Array field based on key name and the value to filter out.
  private static Data removeArrayElement(
      RuntimeContext ctx, Data array, String keyField, String valueToFilter) {
    return ctx.getDataTypeImplementation()
        .arrayOf(
            array.asArray().stream()
                .filter(
                    e ->
                        e.asContainer().getField(keyField).asPrimitive().isNullOrEmpty()
                            || !e.asContainer()
                                .getField(keyField)
                                .asPrimitive()
                                .string()
                                .contains(valueToFilter))
                .collect(toImmutableList()));
  }

  public static void validateResourceInfo(Data resourceInfo) {
    if (!resourceInfo.isContainer()
        || !isPrimitiveString(resourceInfo.asContainer().getField(RESOURCE_TYPE_FIELD))
        || !isPrimitiveString(resourceInfo.asContainer().getField(STABLE_ID_FIELD))) {
      throw new IllegalArgumentException(
          String.format(
              "resourceInfo parameter must be a Container with keys 'resourceType' and 'stableId',"
                  + " each containing string values describing those value. Instead, supplied"
                  + " 'resourceInfo': '%s'",
              resourceInfo));
    }
  }

  public static void validateSnapshots(Data snapshots, String resourceType, String stableId) {
    if (!snapshots.isArray() || !snapshots.asArray().getElement(0).isContainer()) {
      throw new IllegalArgumentException(
          String.format(
              "Reconciliation can only be performed on an Array of Container resource snapshots,"
                  + " but provided: '%s'",
              snapshots));
    }

    if (snapshots.isNullOrEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Attempted reconciliation with an empty array of snapshots for resource with id '%s'"
                  + " and type '%s'. This could indicate an error in querying the resource's"
                  + " snapshots or an issue with the intermediate store.",
              stableId, resourceType));
    }
  }

  private static boolean isPrimitiveString(Data data) {
    return !data.isNullOrEmpty() && data.isPrimitive() && (data.asPrimitive().string() != null);
  }
}
