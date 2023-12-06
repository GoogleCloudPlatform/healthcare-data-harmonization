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

import java.io.Serializable;

/** MergeFunctionFactory is a factory for creating merge related functions. */
public interface MergeFunctionFactory extends Serializable {

  /**
   * Creates a {@link MergeResourcesFunc} for the given resource type.
   *
   * @param resourceType The resource type
   * @return The user-configured merge function corresponding to the given resource type.
   */
  MergeResourcesFunc createMergeFunc(String resourceType);

  /**
   * Creates a {@link MergeRuleFunc} for the given resource type.
   *
   * @param resourceType The resource type
   * @return The user configured merge rule function corresponding to the given resource type.
   */
  MergeRuleFunc createMergeRuleFunc(String resourceType);
}
