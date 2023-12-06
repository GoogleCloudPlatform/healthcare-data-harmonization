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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.MergeConstants.REMOVE_FIELD_PLACEHOLDER;
import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultArray;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Builtin functions for merging fields of FHIR resources as part of HDE reconciliation.
 */
public class FieldMergeMethods {
  /**
   * Merges two {@link Data} field values from an existing and an inbound resource, returning the
   * inbound value if it exists. Implements the "From IR" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx      {@link RuntimeContext} within which to execute merge rule.
   * @param existing Data field value from the older resource to merge.
   * @param inbound  Data field value from the more recent resource to merge.
   * @return Data merge result.
   */
  public static Data forceInbound(RuntimeContext ctx, Data existing, Data inbound) {
    if (inbound.isNullOrEmpty()) {
      return REMOVE_FIELD_PLACEHOLDER;
    }
    return inbound;
  }

  /**
   * Merges two {@link Data} field values from an existing and an inbound resource, returning the
   * inbound value if it exists and the existing value otherwise. Implements the "From IR if exists
   * in IR, else from ER if exists in ER, else leave empty" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx      {@link RuntimeContext} within which to execute merge rule.
   * @param existing Data field value from the older resource to merge.
   * @param inbound  Data field value from the more recent resource to merge.
   * @return Data merge result.
   */
  public static Data preferInbound(RuntimeContext ctx, Data existing, Data inbound) {
    if (!inbound.isNullOrEmpty()) {
      return inbound;
    }
    return existing;
  }

  /**
   * Merges two {@link Array} field values from an existing and an inbound resource, returning the
   * union of the two values. Implements a version of the "Union of ER and IR" rule from
   * go/fhir-reconciliation-rules.
   *
   * @param ctx      {@link RuntimeContext} within which to execute merge rule.
   * @param existing {@link Data} Array field value from the older resource to merge.
   * @param inbound  Data Array field value from the more recent resource to merge.
   * @return Data merge result.
   */
  public static Array union(RuntimeContext ctx, Data existing, Data inbound) {
    return pathedUnion(ctx, existing, inbound, Collections.singletonList(Path.empty()));
  }

  /**
   * Merges two {@link Array} field values from an existing and an inbound resource, returning the
   * union of the two values, where the provided JSON-type paths points to fields within each
   * Array element to use for determining distinctness within the union operation. Implements a
   * version of the "Union of ER and IR" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx            {@link RuntimeContext} within which to execute merge rule.
   * @param existing       {@link Data} Array field value from the older resource to merge.
   * @param inbound        Data Array field value from the more recent resource to merge.
   * @param firstFieldPath Data Primitive string representing the relative JSON path within each
   *                       entry of 'existing' and 'inbound' to extract keys from for determining
   *                       distinct entries.
   * @param restFieldPaths list of Data Primitive strings representing relative JSON path within
   *                       each entry of 'existing' and 'inbound' to extract keys for determining
   *                       distinct entries.
   * @return Data merge result.
   */
  public static Array unionByField(
      RuntimeContext ctx, Data existing, Data inbound, String firstFieldPath,
      String... restFieldPaths) {
    List<String> allFieldPaths = new ArrayList<>();
    allFieldPaths.add(firstFieldPath);
    Collections.addAll(allFieldPaths, restFieldPaths);
    ImmutableList<Path> paths = allFieldPaths.stream().map(Path::parse).collect(toImmutableList());
    return pathedUnion(ctx, existing, inbound, paths);
  }

  /**
   * Merges two {@link Array} field values from an existing and an inbound resource, returning the
   * elements in the first array but not in the second, where the provided JSON-type paths points to
   * fields within each Array element to use for determining the diff between two arrays. Example:
   * diffByField([A,B], [B]) -> [A]
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing Data Array field value from the older resource to merge.
   * @param inbound Data Array field value from the more recent resource to merge.
   * @return Data merge result.
   */
  public static Array diff(RuntimeContext ctx, Array existing, Array inbound) {
    if (inbound.isNullOrEmpty() || existing.isNullOrEmpty()) {
      return existing.asArray();
    }

    Set<Data> distinctArrayElements =
        existing.asArray().stream().collect(Collectors.toCollection(LinkedHashSet::new));
    inbound.asArray().stream().forEach(distinctArrayElements::remove);

    return new DefaultArray(distinctArrayElements);
  }

  // Verifies union parameters, short-circuits empty-array edge cases, and executes a union of the
  // given Arrays.
  private static Array pathedUnion(RuntimeContext ctx, Data existing, Data inbound,
      List<Path> paths) {
    verifyBothParametersArray(existing, inbound);
    if (existing.isNullOrEmpty()) {
      return inbound.asArray();
    }
    if (inbound.isNullOrEmpty()) {
      return existing.asArray();
    }

    return performUnion(ctx, existing, inbound, paths);
  }

  // Verify the both parameters are Arrays.
  private static void verifyBothParametersArray(Data existing, Data inbound) {
    if (!existing.isArray() || !inbound.isArray()) {
      throw new IllegalArgumentException("Existing and inbound elements must be Arrays");
    }
  }

  // Executes union operation on input Arrays using 'path' to extract keys to determine distinct
  // entries.
  private static Array performUnion(RuntimeContext ctx, Data existing, Data inbound,
      List<Path> paths) {
    // Generate map of <entryKey, entry> for the entries of 'existing'.
    Map<Data, Data> distinctArrayElements =
        existing.asArray().stream()
            .collect(
                Collectors.toMap(e -> getCompoundPathKey(ctx, e, paths), e -> e, (x, y) -> y,
                    LinkedHashMap::new));
    // Perform union and overwrite any overlapping entryKeys with the 'inbound' entry values.
    inbound.asArray().stream()
        .forEach(
            e -> {
              Data pathKey = getCompoundPathKey(ctx, e, paths);
              distinctArrayElements.put(
                  pathKey,
                  preferInbound(ctx, distinctArrayElements.get(pathKey), e));
            });
    // Write union result back to an Array.
    return new DefaultArray(distinctArrayElements.values());
  }

  private static Data getCompoundPathKey(RuntimeContext ctx, Data root, List<Path> paths) {
    ImmutableList<Data> pathValues =
        paths.stream().map(p -> p.get(root)).collect(toImmutableList());
    return ctx.getDataTypeImplementation().arrayOf(pathValues);
  }

  private FieldMergeMethods() {
  }
}
