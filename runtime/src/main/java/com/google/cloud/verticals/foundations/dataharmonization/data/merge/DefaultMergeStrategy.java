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

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import java.util.Set;

/**
 * Implements default merge strategy.
 *
 * <p>In this merge strategy:
 *
 * <ol>
 *   <li>If current and inbound data are both {@link Container}s, inbound is merged to the current
 *       Data recursively in a field-wise fashion (this may or may not be an in-place operation
 *       depending on the data implementation).
 *   <li>If current and inbound data are both {@link Array}s, inbound is concatenated into the
 *       existing array.
 *   <li>In other cases, inbound is directly returned unless it is null or empty when the current is
 *       an array or container, in which cases current data is returned. Since we want to make sure
 *       null == {} == [] doesn't override existing data like [1,2,3].
 * </ol>
 *
 * see go/dh-whistle-elp-merges for details.
 */
public final class DefaultMergeStrategy implements MergeStrategy {
  public static final DefaultMergeStrategy INSTANCE = new DefaultMergeStrategy();

  private DefaultMergeStrategy() {}

  @Override
  public Data merge(
      DataTypeImplementation dti, Data currentRoot, Data inbound, Path pathInCurrent) {
    Data current = pathInCurrent.get(currentRoot);

    Data mergeResult;
    if (current.isNullOrEmpty()) {
      mergeResult = inbound;
    } else if (current.isContainer() && inbound.isContainer()) {
      Container cc = current.asContainer();
      Container ic = inbound.asContainer();
      mergeResult = mergeContainer(cc, ic, dti);
    } else if (current.isArray() && inbound.isArray()) {
      Array ca = current.asArray();
      Array ia = inbound.asArray();
      mergeResult = mergeArrays(ca, ia, dti);
    } else if (inbound.isNullOrEmpty() && (current.isContainer() || current.isArray())) {
      // Since null === {} === [] we want to make sure we don't replace [1, 2, 3] with {}.
      mergeResult = current;
    } else {
      mergeResult = inbound;
    }
    return pathInCurrent.set(dti, currentRoot, mergeResult);
  }

  private static Data mergeArrays(
      Array current, Array inbound, DataTypeImplementation dataTypeImplementation) {
    verifyNotMergingIntoImmutable(current, inbound);
    if (current.isNullOrEmpty()) {
      return inbound;
    }

    if (inbound == current) {
      // Uh oh.
      inbound = inbound.deepCopy().asArray();
    }

    for (int i = 0; i < inbound.size(); i++) {
      if (inbound.isFixed(i)) {
        current =
            current.setFixedElement(
                i,
                current
                    .getElement(i)
                    .merge(inbound.getElement(i), dataTypeImplementation));
        continue;
      }
      current = current.setElement(current.size(), inbound.getElement(i));
    }
    return current;
  }

  private static Data mergeContainer(
      Container current, Container inbound, DataTypeImplementation dataTypeImplementation) {
    verifyNotMergingIntoImmutable(current, inbound);
    if (current.isNullOrEmpty()) {
      return inbound;
    }
    Set<String> ccFields = current.fields();
    for (String f : inbound.fields()) {
      current =
          current.setField(
              f,
              ccFields.contains(f)
                  ? current
                      .getField(f)
                      .merge(inbound.getField(f), dataTypeImplementation)
                  : inbound.getField(f));
    }
    return current;
  }

  private static void verifyNotMergingIntoImmutable(Data current, Data inbound) {
    if (!current.isWritable() && !current.isNullOrEmpty() && !inbound.isNullOrEmpty()) {
      throw new UnsupportedOperationException(
          String.format("Attempt to merge into immutable %s", current));
    }
  }
}
