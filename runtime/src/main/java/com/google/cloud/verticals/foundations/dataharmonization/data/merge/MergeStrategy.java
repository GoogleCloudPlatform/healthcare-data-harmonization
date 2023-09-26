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

package com.google.cloud.verticals.foundations.dataharmonization.data.merge;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.errorprone.annotations.Immutable;

/**
 * Interface for strategies that merges two {@link Data}. Primarily used by {@link Data}
 * implementations.
 */
@Immutable
public interface MergeStrategy {

  /**
   * Merge the inbound {@link Data} into the {@code pathInCurrent} of current {@link Data} and
   * returns the data root. Depending on the implementation, it may or may not be commutative (i.e.
   * merge(data1, data2) may or may not equal to merge(data2, data1)).
   *
   * @param dti used to supply missing intermediate data depending on the implementation.
   * @param current the root of the current data. The inbound data will be merged into the {@code
   *     pathInCurrent} under it.
   * @param inbound the inbound data. Can be {@link NullData}.
   * @param pathInCurrent the path under {@code current} data that inbound is merging into
   * @return the root data after merge.
   */
  Data merge(DataTypeImplementation dti, Data current, Data inbound, Path pathInCurrent);
}
