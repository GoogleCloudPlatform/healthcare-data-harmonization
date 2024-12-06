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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.BIGQUERY_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.CLOUD_SPANNER_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.FHIR_VERSION_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.HL7V2_SOURCE_SYSTEM;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.LATEST;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RESOURCE_TYPE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.STABLE_ID_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.SYSTEM_FIELD;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMerger;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.defaults.DefaultMergerFactory;
import java.io.Serializable;
import javax.naming.ConfigurationException;

/** MergeResources defines merging plugin functions for hdev2. */
public class MergeResources extends MergeResourcesBase implements Serializable {
  private final MergeConfigExecutor mergeConfigExecutor;
  private final DefaultMergerFactory defaultMergerFactory;

  public MergeResources(
      MergeConfigExecutor mergeConfigExecutor, DefaultMergerFactory defaultMergerFactory) {
    super(mergeConfigExecutor);
    this.mergeConfigExecutor = mergeConfigExecutor;
    this.defaultMergerFactory = defaultMergerFactory;
  }

  /**
   * Builtin for reconciliation to merge an {@link Array} of temporally-sorted resource snapshots
   * according to the resource-level merge rule and the configured field-level merge function.
   * Returns a properly formatted, reconciled FHIR resource, ready to be written to the final FHIR
   * store.
   *
   * @param ctx - {@link RuntimeContext} within which to perform merge.
   * @param sortedSnapshots - {@link Data} Array of resource snapshots.
   * @param resourceInfo - {@link Data}Container holding 'resourceType' and 'stableId' fields.
   * @return {@link Data} Container containing the cleaned merge result resource.
   * @throws ConfigurationException when there are improper user configurations.
   */
  @PluginFunction
  public Data mergeResources(RuntimeContext ctx, Data sortedSnapshots, Data resourceInfo)
      throws ConfigurationException {
    validateResourceInfo(resourceInfo);
    String resourceType =
        resourceInfo.asContainer().getField(RESOURCE_TYPE_FIELD).asPrimitive().string();
    String stableId = resourceInfo.asContainer().getField(STABLE_ID_FIELD).asPrimitive().string();

    validateSnapshots(sortedSnapshots, resourceType, stableId);

    // Get configured resource-level merge rule for the given FHIR resource type, or if none
    // exists, the default resource-level merge rule.
    String resourceLevelRule = mergeConfigExecutor.getMergeRule(ctx, resourceType);

    Container mergedResource = mergeResource(ctx, resourceLevelRule, resourceType, sortedSnapshots);

    return prepareFinalResource(ctx, mergedResource, stableId);
  }

  // LINT.IfChange(source_systems)
  @Override
  public Container prepareFinalResource(RuntimeContext ctx, Container resource, String stableId) {
    Container prepared = super.prepareFinalResource(ctx, resource, stableId);
    Data cleared = clearSourceSystems(ctx, prepared);
    return cleared.asContainer();
  }

  private Data clearSourceSystems(RuntimeContext ctx, Data data) {
    String[] sourceSystems = {
      FHIR_VERSION_SOURCE_SYSTEM,
      HL7V2_SOURCE_SYSTEM,
      BIGQUERY_SOURCE_SYSTEM,
      CLOUD_SPANNER_SOURCE_SYSTEM
    };

    for (String sourceSystem : sourceSystems) {
      data = clearMetaTagField(ctx, data, SYSTEM_FIELD, sourceSystem);
    }
    return data;
  }

  // LINT.ThenChange()

  private Container mergeResource(
      RuntimeContext ctx, String resourceRule, String resourceType, Data snapshots)
      throws ConfigurationException {
    if (snapshots.isNullOrEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Attempted merge over empty array of resource snapshots with type '%s'.",
              resourceType));
    }
    if (resourceRule.equals(LATEST)) {
      return snapshots
          .deepCopy()
          .asArray()
          .getElement(snapshots.asArray().size() - 1)
          .asContainer();
    } else {
      return iterativeMerge(ctx, snapshots.asArray(), resourceType);
    }
  }

  // Performs iterative merge of Array of sorted resource snapshots.
  private Container iterativeMerge(RuntimeContext ctx, Array sortedSnapshots, String resourceType)
      throws ConfigurationException {
    DefaultMerger defaultMerger = defaultMergerFactory.create(ctx);

    Container incrementalMerge =
        sortedSnapshots.stream().findFirst().orElse(NullData.instance).deepCopy().asContainer();

    Container currentMerge;
    Container inbound;
    for (int i = 1; i < sortedSnapshots.size(); ++i) {
      inbound = sortedSnapshots.getElement(i).asContainer();
      currentMerge = merge(ctx, incrementalMerge, inbound, resourceType, defaultMerger);
      incrementalMerge =
          defaultMerger.processUnmergedFields(
              ctx, currentMerge, incrementalMerge, inbound, resourceType, false);
    }
    return incrementalMerge;
  }
}
