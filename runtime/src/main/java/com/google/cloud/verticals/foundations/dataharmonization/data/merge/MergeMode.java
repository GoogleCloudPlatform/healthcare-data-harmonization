/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data.merge;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import java.io.Serializable;

/** Specifies a mode of selecting a merge strategy for a given data. */
public enum MergeMode implements Serializable {
  MERGE(null),
  REPLACE(new ReplaceMergeStrategy()),
  APPEND(new AppendMergeStrategy()),
  EXTEND(new ExtendMergeStrategy());

  private final MergeStrategy forcedStrategy;

  MergeMode(MergeStrategy forcedStrategy) {
    this.forcedStrategy = forcedStrategy;
  }

  public MergeStrategy getStrategy(Data data) {
    if (forcedStrategy == null) {
      return data.getMergeStrategy();
    }
    return forcedStrategy;
  }
}
