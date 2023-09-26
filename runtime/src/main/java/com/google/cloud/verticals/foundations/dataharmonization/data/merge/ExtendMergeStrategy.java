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
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.common.collect.Sets;
import java.util.Set;

/**
 * A merge strategy that concatenates arrays and adds missing key/value pairs to containers. If
 * types don't match or are not containers or arrays, fails.
 */
public class ExtendMergeStrategy implements MergeStrategy {

  @Override
  public Data merge(DataTypeImplementation dti, Data current, Data inbound, Path pathInCurrent) {
    Data target = pathInCurrent.get(current);
    if (target.isArray()) {
      if (!inbound.isArray()) {
        throw new IllegalArgumentException(
            String.format(
                "%s cannot be extended with %s. Did you mean to 'append'?",
                Core.prettyTypes(target), Core.prettyTypes(inbound)));
      }
      target = concat(target.asArray(), inbound.asArray());
    } else if (target.isContainer()) {
      if (!inbound.isContainer()) {
        throw new IllegalArgumentException(
            String.format(
                "%s cannot be extended with %s. Was the target meant to be an array?",
                Core.prettyTypes(target), Core.prettyTypes(inbound)));
      }
      target = extend(target.asContainer(), inbound.asContainer());
    } else {
      throw new IllegalArgumentException(
          String.format(
              "'extend' is not applicable to %s. Was it meant to be an array or container?",
              Core.prettyTypes(target)));
    }
    return pathInCurrent.set(dti, current, target);
  }

  public static Array concat(Array a, Array b) {
    for (int i = 0; i < b.size(); i++) {
      a = a.setElement(a.size(), b.getElement(i));
    }
    return a;
  }

  public static Container extend(Container a, Container b) {
    Set<String> uniqueInboundKeys = Sets.difference(b.fields(), a.fields());
    for (String uniqueInboundKey : uniqueInboundKeys) {
      a = a.setField(uniqueInboundKey, b.getField(uniqueInboundKey));
    }
    return a;
  }
}
