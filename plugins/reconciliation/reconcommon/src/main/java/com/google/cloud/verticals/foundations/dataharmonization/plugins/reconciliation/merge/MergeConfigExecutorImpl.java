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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.DEFAULT_RESOURCE_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.LATEST;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE_RULES_PACKAGE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.RESOURCE_MERGE_RULES;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Optional;
import javax.naming.ConfigurationException;

/** Default implementation for MergeConfigExecutor. */
public class MergeConfigExecutorImpl implements MergeConfigExecutor {

  private final MergeFunctionFactory mergeFunctionFactory;

  /**
   * Construct a MergeConfigExecutorImpl
   * @param mergeFunctionFactory a factory for constructing whistle functions.
   */
  public MergeConfigExecutorImpl(MergeFunctionFactory mergeFunctionFactory) {
    this.mergeFunctionFactory = mergeFunctionFactory;
  }

  @Override
  public Data mergeResources(
      RuntimeContext ctx, String resourceType, Container existing, Container inbound) {
    MergeResourcesFunc func = mergeFunctionFactory.createMergeFunc(resourceType);
    return func.merge(ctx, existing, inbound);
  }

  @Override
  public String getMergeRule(RuntimeContext ctx, String resourceType)
      throws ConfigurationException {
    // Resource-level merge rules in order of preference (resourceType specific, else Default).
    String[] resourceTypesToTry = new String[] {resourceType, DEFAULT_RESOURCE_TYPE};
    for (String resourceTypeToTry : resourceTypesToTry) {
      Optional<String> rule = executeMergeRuleWhistleFunction(ctx, resourceTypeToTry);
      // If the merge rule exists, use it.
      if (rule.isPresent()) {
        return rule.get();
      }
    }
    // If neither resource-level merge rule exists, then no valid DefaultRule has been configured.
    throw new ConfigurationException(
        String.format(
            "Must define a default merge rule function '%s' with package '%s' in the"
                + " reconciliation configuration.",
            MergeRuleFunc.getFunctionName(DEFAULT_RESOURCE_TYPE), MERGE_RULES_PACKAGE));
  }

  private Optional<String> executeMergeRuleWhistleFunction(RuntimeContext ctx, String resourceType)
      throws ConfigurationException {
    MergeRuleFunc func = mergeFunctionFactory.createMergeRuleFunc(resourceType);
    Data mergeRule;
    try {
      mergeRule = func.getMergeRule(ctx);
    } catch (NoMatchingOverloadsException e) {
      return Optional.empty();
    }

    if (!isPrimitiveString(mergeRule)) {
      throw new ConfigurationException(
          String.format(
              "Configured resource-level merge rule functions must return string, but instead"
                  + " '%s:%s' returned '%s'.",
              MERGE_RULES_PACKAGE, MergeRuleFunc.getFunctionName(resourceType), mergeRule));
    }
    if (!RESOURCE_MERGE_RULES.contains(mergeRule.asPrimitive().string())) {
      throw new ConfigurationException(
          String.format(
              "Resource-level merge rule for the %s resource type must be a string with value of"
                  + " one of {'%s', '%s'}, but got '%s'",
              resourceType, LATEST, MERGE, mergeRule));
    }
    return Optional.of(mergeRule.asPrimitive().string());
  }

  private boolean isPrimitiveString(Data data) {
    return !data.isNullOrEmpty() && data.isPrimitive() && (data.asPrimitive().string() != null);
  }
}
