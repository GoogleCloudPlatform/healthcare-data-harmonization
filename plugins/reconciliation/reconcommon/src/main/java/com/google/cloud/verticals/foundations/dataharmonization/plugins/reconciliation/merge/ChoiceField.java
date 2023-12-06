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

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import java.util.stream.Stream;

/** Class for holding choiceX field merge method and helpers for HDE FHIR reconciliation. */
public class ChoiceField {

  private static final String FORCE_INBOUND = "forceInbound";
  private static final String PREFER_INBOUND = "preferInbound";

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
  public static Data choiceField(
      RuntimeContext ctx,
      Data existing,
      Data inbound,
      String mergeRule,
      String... choiceFieldPaths) {
    verifyRuleIsForceOrPreferInbound(mergeRule);
    Optional<Data> inboundField = getChoiceFieldFromResource(ctx, inbound, choiceFieldPaths);
    Optional<Data> existingField = getChoiceFieldFromResource(ctx, existing, choiceFieldPaths);

    // If using the forceInbound rule, then return the choice field in inbound or empty if none.
    if (mergeRule.equals(FORCE_INBOUND)) {
      return inboundField.orElse(NullData.instance);
    }

    // Using the preferInbound rule, return inbound choice field if present, else existing choice
    // field if present, else empty (NullData) if neither present.
    return Stream.of(inboundField, existingField)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .findFirst()
        .orElse(NullData.instance);
  }

  // Checks if a valid merge rule was chosen (from among "preferInbound" and "forceInbound").
  private static void verifyRuleIsForceOrPreferInbound(String mergeRule) {
    if (!mergeRule.equals(FORCE_INBOUND) && !mergeRule.equals(PREFER_INBOUND)) {
      throw new IllegalArgumentException(
          String.format(
              "Illegal merge method selected: '%s', valid options are {\"%s\"," + " \"%s\"}.",
              mergeRule, FORCE_INBOUND, PREFER_INBOUND));
    }
  }

  // Retrieves the choice field from the given resource as a singleton Container of
  // fieldName:fieldValue, if one exists in the given resource.
  private static Optional<Data> getChoiceFieldFromResource(
      RuntimeContext ctx, Data resource, String[] paths) {
    Optional<Path> path = getChoiceField(resource, paths);
    return path.map(
        value ->
            ctx.getDataTypeImplementation()
                .containerOf(ImmutableMap.of(value.toString().substring(1), value.get(resource))));
  }

  // Returns the choice field present in the given resource or an empty string if none is present.
  private static Optional<Path> getChoiceField(Data resource, String[] paths) {
    for (String path : paths) {
      if (!Path.parse(path).get(resource).isNullOrEmpty()) {
        return Optional.of(Path.parse(path));
      }
    }
    return Optional.empty();
  }

  private ChoiceField() {}
}
