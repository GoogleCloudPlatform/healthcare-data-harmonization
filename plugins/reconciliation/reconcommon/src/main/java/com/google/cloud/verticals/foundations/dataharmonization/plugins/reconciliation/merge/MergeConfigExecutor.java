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

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;
import javax.naming.ConfigurationException;

/**
 * MergeConfigExecutor is a utility class that executes user-configured whistle functions for
 * merging.
 */
public interface MergeConfigExecutor extends Serializable {

  /**
   * Get the user-configured merge rule for the given resource type
   *
   * @param ctx The runtime context
   * @param resourceType The resource type for which the merge rule is configured.
   * @return The merge rule (either "latest" or "merge")
   */
  String getMergeRule(RuntimeContext ctx, String resourceType) throws ConfigurationException;

  /**
   * Merge two resources using the the user-configured merge function for the given resource type.
   *
   * @param ctx The runtime context
   * @param resourceType The resource type for which the merge function is configured.
   * @param existing The oldest resource in the pair of resources being merged.
   * @param inbound The newest resource in the pair of resources being merged.
   * @return The merged resource.
   */
  Data mergeResources(
      RuntimeContext ctx, String resourceType, Container existing, Container inbound);
}
