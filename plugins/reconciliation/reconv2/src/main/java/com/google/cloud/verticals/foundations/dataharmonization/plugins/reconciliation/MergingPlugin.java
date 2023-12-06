/*
 * Copyright 2023 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.ChoiceField;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.merge.FieldMergeMethods;

/** MergingPlugin defines Whistle merging functionalities for hdev2. */
public class MergingPlugin {

  private MergingPlugin() {}

  /**
   * Merges {@link Data} choiceX fields from an existing and an inbound resource by finding the
   * fields to merge from among the possible 'choiceFieldPaths' provided and applying the
   * 'mergeRule' logic, such that the merge produces exactly one of the provided choice fields in
   * the final resource.
   *
   * <p>There can be, at most, one choice field present in each resource. For example, one choiceX
   * field for the Patient FHIR resource is the 'deceased[x]' field with two possible field types:
   * 'deceasedBoolean' and 'deceasedDateTime'. Therefore, any given Patient resource can either have
   * a 'deceasedBoolean' field, have a 'deceasedDateTime' field, or have neither. A Patient resource
   * can never have both of the 'deceased[x]' fields present.
   *
   * <p>Applying this example, if we have inbound and existing Patient resources like the following:
   *
   * <p>existing: {
   *
   * <ul>
   *   <li>...some other fields...
   *   <li>deceasedBoolean: "false";
   *   <li>...some other fields...
   * </ul>
   *
   * }
   *
   * <p>inbound: {
   *
   * <ul>
   *   <li>...some other fields...
   *   <li>deceasedDateTime: "02/03/2010";
   *   <li>...some other fields...
   * </ul>
   *
   * }
   *
   * <p>and we apply the 'preferInbound' merge rule, then we first search the existing and inbound
   * resources for any of the 'deceased[x]' fields. Finding one of the fields in each, we then merge
   * the two fields using our merge rule - since there is a 'deceased[x]' field present in the
   * inbound resource, we prefer that value over the existing value. Therefore, our output field
   * added to the resource written to the final store will be {deceasedDateTime: "02/03/2010"}.
   *
   * <p>If in the previous example, the 'deceasedDateTime' hadn't been present in the inbound
   * resource, then the 'preferInbound' rule would've meant we would push {deceasedBoolean: "false"}
   * from the existing resource to the output resource written to the final store.
   *
   * <p>The 'preferInbound' rule means we:
   *
   * <ul>
   *   <li>- output the add field from the inbound resource if it has one
   *   <li>- otherwise, add the choiceX field from the existing resource if it has one
   *   <li>- otherwise, add no field to the output resource
   * </ul>
   *
   * <p>The pattern for the 'forceInbound' rule is similar, but with the second line removed - for
   * this rule, the choiceX field from the existing resource is never considered, even if it is
   * present.
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing {@link Data} field value from the older resource to merge.
   * @param inbound {@link Data} field value from the more recent resource to merge.
   * @param mergeRule {@link String} either "forceInbound" or "preferInbound".
   * @param choiceFieldPaths {@link java.util.List<String>} any number of choiceX field paths from
   *     which to source the field values to merge.
   * @return singleton {@link DefaultContainer} of the merged {choiceX field name : field value} or
   *     {@link NullData} if there is an empty merge result.
   */
  @PluginFunction
  public static Data choiceField(
      RuntimeContext ctx,
      Data existing,
      Data inbound,
      String mergeRule,
      String... choiceFieldPaths) {
    return ChoiceField.choiceField(ctx, existing, inbound, mergeRule, choiceFieldPaths);
  }

  /**
   * Merges two {@link Data} field values from an existing and an inbound resource, returning the
   * inbound value if it exists. Implements the "From IR" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing Data field value from the older resource to merge.
   * @param inbound Data field value from the more recent resource to merge.
   * @return Data merge result.
   */
  @PluginFunction
  public static Data forceInbound(RuntimeContext ctx, Data existing, Data inbound) {
    return FieldMergeMethods.forceInbound(ctx, existing, inbound);
  }

  /**
   * Merges two {@link Data} field values from an existing and an inbound resource, returning the
   * inbound value if it exists and the existing value otherwise. Implements the "From IR if exists
   * in IR, else from ER if exists in ER, else leave empty" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing Data field value from the older resource to merge.
   * @param inbound Data field value from the more recent resource to merge.
   * @return Data merge result.
   */
  @PluginFunction
  public static Data preferInbound(RuntimeContext ctx, Data existing, Data inbound) {
    return FieldMergeMethods.preferInbound(ctx, existing, inbound);
  }

  /**
   * Merges two {@link Array} field values from an existing and an inbound resource, returning the
   * union of the two values. Implements a version of the "Union of ER and IR" rule from
   * go/fhir-reconciliation-rules.
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing {@link Data} Array field value from the older resource to merge.
   * @param inbound Data Array field value from the more recent resource to merge.
   * @return Data merge result.
   */
  @PluginFunction
  public static Array union(RuntimeContext ctx, Data existing, Data inbound) {
    return FieldMergeMethods.union(ctx, existing, inbound);
  }

  /**
   * Merges two {@link Array} field values from an existing and an inbound resource, returning the
   * union of the two values, where the provided JSON-type paths points to fields within each Array
   * element to use for determining distinctness within the union operation. Implements a version of
   * the "Union of ER and IR" rule from go/fhir-reconciliation-rules.
   *
   * @param ctx {@link RuntimeContext} within which to execute merge rule.
   * @param existing {@link Data} Array field value from the older resource to merge.
   * @param inbound Data Array field value from the more recent resource to merge.
   * @param firstFieldPath Data Primitive string representing the relative JSON path within each
   *     entry of 'existing' and 'inbound' to extract keys from for determining distinct entries.
   * @param restFieldPaths list of Data Primitive strings representing relative JSON path within
   *     each entry of 'existing' and 'inbound' to extract keys for determining distinct entries.
   * @return Data merge result.
   */
  @PluginFunction
  public static Array unionByField(
      RuntimeContext ctx,
      Data existing,
      Data inbound,
      String firstFieldPath,
      String... restFieldPaths) {
    return FieldMergeMethods.unionByField(ctx, existing, inbound, firstFieldPath, restFieldPaths);
  }
}
