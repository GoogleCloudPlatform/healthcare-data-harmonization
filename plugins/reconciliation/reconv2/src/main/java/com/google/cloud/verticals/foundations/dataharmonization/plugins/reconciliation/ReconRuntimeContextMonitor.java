// Copyright 2023 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.MatchingPlugin.extractPropertyValues;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.random.RandomUUIDGenerator;
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.ContainerBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContextMonitor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Adds post processing hook for updating the transformed result from Whistle. Given a mapped output
 * of { resources: [{resourceType: Patient, ...}, ...] } This hook will insert the list of fragments
 * and property values of both fragments and resources into the returnData.
 */
public class ReconRuntimeContextMonitor implements RuntimeContextMonitor {

  public ReconRuntimeContextMonitor() {}

  private static final String MATCHING_PACKAGE = "matching_rules";

  private static final RandomUUIDGenerator uuidGenerator = new RandomUUIDGenerator();

  private boolean enabled = true;

  @Override
  @CanIgnoreReturnValue
  public Data onRuntimeContextFinish(RuntimeContext context, Data returnData) {
    if (!enabled) {
      return returnData;
    }
    // TODO(): Support returnData as a bundle resource.
    if (returnData.isNullOrEmpty()
        || !returnData.isContainer()
        || returnData.asContainer().getField("resources").isNullOrEmpty()) {
      return returnData;
    }

    Container returnContainer = returnData.asContainer();
    Container newReturnContainer = returnData.deepCopy().asContainer();

    Array fragments = context.getMetaData().getSerializableMeta("fragments");

    Container propertyValues = ContainerBuilder.fromContext(context).build();

    if (fragments != null) {
      newReturnContainer.setField("fragments", fragments);

      fragments.asArray().stream().forEach(f -> populatePropertyValues(context, propertyValues, f));
    }

    returnContainer.getField("resources").asArray().stream()
        .forEach(r -> insertIdIfNecessary(context, r));

    returnContainer.getField("resources").asArray().stream()
        .forEach(r -> populatePropertyValues(context, propertyValues, r));

    newReturnContainer.setField("propertyValues", propertyValues);
    return newReturnContainer;
  }

  private void insertIdIfNecessary(RuntimeContext context, Data resource) {
    if (!resource.isContainer()) {
      throw new PropertyValueFetcherException(
          String.format(
              "Resource is a %s, must be a Container.", resource.getClass().getSimpleName()));
    }

    if (!resource.asContainer().getField("id").isNullOrEmpty()) {
      return;
    }
    String uuid = uuidGenerator.next();
    resource.asContainer().setField("id", context.getDataTypeImplementation().primitiveOf(uuid));
  }

  private void populatePropertyValues(
      RuntimeContext context,
      Container propertyValues,
      Data resource) {
    validateResource(resource);
    String resourceType = resource.asContainer().getField("resourceType").asPrimitive().string();
    String id = resource.asContainer().getField("id").asPrimitive().string();

    Data matchingConfigs = getMatchingConfigs(context, resourceType);
    if (matchingConfigs == null) {
      return;
    }

    Data pv = extractPropertyValues(context, matchingConfigs, resource);

    String resourceId = String.format("%s/%s", resourceType, id);
    propertyValues.setField(resourceId, pv);
  }

  private void validateResource(Data resource) {
    if (!resource.isContainer()) {
      throw new PropertyValueFetcherException(
          String.format(
              "Resource is a %s, must be a Container.", resource.getClass().getSimpleName()));
    }

    if (resource.asContainer().getField("resourceType").isNullOrEmpty()) {
      throw new PropertyValueFetcherException("Mapped resource is missing the resourceType field.");
    }
  }

  @Nullable
  private Data getMatchingConfigs(RuntimeContext context, String resourceType) {
    Set<CallableFunction> functionMatches =
        context
            .getRegistries()
            .getFunctionRegistry(MATCHING_PACKAGE)
            .getOverloads(
                ImmutableSet.of(MATCHING_PACKAGE), String.format("%sConfig", resourceType));

    if (functionMatches.isEmpty()) {
      return null;
    }

    return functionMatches.iterator().next().call(context);
  }

  /** enable enables the ReconRuntimeContextMonitor's onRuntimeContextFinish function. */
  public void enable() {
    this.enabled = true;
  }

  /**
   * disable disables the ReconRuntimeContextMonitor's onRuntimeContextFinish function, making it a
   * no-op.
   */
  public void disable() {
    this.enabled = false;
  }
}
