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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.MERGE_METHOD_SUFFIX;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;

/** MergeResourceFunc represents a function for merging two resources together */
@FunctionalInterface
public interface MergeResourcesFunc {
  /** Merge the existing resource together with the inbound resource and return the result. */
  Data merge(RuntimeContext ctx, Data existing, Data inbound);

  /** Get the merge resources funcdtion name given the resource type. */
  static String getFunctionName(String resourceType) {
    return resourceType + MERGE_METHOD_SUFFIX;
  }
}
