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

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Core;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;

/**
 * A merge strategy that appends the inbound item to the array at the given path in current. Fails
 * if it is not an array.
 */
public class AppendMergeStrategy implements MergeStrategy {

  @Override
  public Data merge(DataTypeImplementation dti, Data current, Data inbound, Path pathInCurrent) {
    Data target = pathInCurrent.get(current);
    if (!target.isArray()) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot append to %s. Was it meant to be an array?", Core.prettyTypes(target)));
    }
    target = target.asArray().setElement(target.asArray().size(), inbound);
    return pathInCurrent.set(dti, current, target);
  }
}
