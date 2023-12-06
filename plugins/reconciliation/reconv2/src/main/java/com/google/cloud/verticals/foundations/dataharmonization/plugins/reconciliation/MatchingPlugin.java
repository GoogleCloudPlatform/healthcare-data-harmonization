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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.MatchingCriteriaConfigException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.MatchingCriteria;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.StableIdMatchingDsl;
import java.util.List;

/** MatchingPlugin defines Whistle matching functionalities for hdev2. */
public class MatchingPlugin {
  private static final String FRAGMENTS_FIELD = "fragments";

  public MatchingPlugin() {}

  /**
   * Creates {@link Data} matching criteria for matching any identifier. Filters can be applied to
   * identifiers based on "system" or "value".
   *
   * @param filters Zero or more {@link Data} filters to apply. Filters are of the form: { "field":
   *     "field", // One of "system" or "value" "value": "value" }
   * @return {@link Data} matching criteria that can match against any identifier with filters
   *     applied.
   */
  @PluginFunction
  public static Data anyIdentifier(RuntimeContext ctx, Data... filters) {
    return StableIdMatchingDsl.anyIdentifier(ctx, filters);
  }

  /**
   * Creates {@link Data} matching criteria for matching any coding. Filters can be applied to
   * codings.
   *
   * @param filters Zero or more {@link Data} filters to apply. Filters are of the form: { "field":
   *     "field", // One of "system" or "code" "value": "value" }
   * @return {@link Data} matching criteria that can match against any coding with filters applied.
   */
  @PluginFunction
  public static Data anyCoding(RuntimeContext ctx, Data... filters) {
    return StableIdMatchingDsl.anyCoding(ctx, filters);
  }

  /**
   * Creates {@link Data} matching criteria that will match if any of the given matching criteria
   * match. This operation is assumed to be in the context of a container.
   *
   * @param matchingCriteria {@link Data} initial matching criteria to use in the result
   * @param otherMatchingCriteria Zero or more {@link Data} matching criteria to usein the result
   * @return {@link Data} matching criteria that will match if any of the given matching criteria
   *     match.
   */
  @PluginFunction
  public static Data anyOf(
      RuntimeContext ctx, Data matchingCriteria, Data... otherMatchingCriteria) {
    return StableIdMatchingDsl.anyOf(ctx, matchingCriteria, otherMatchingCriteria);
  }

  /**
   * Creates {@link Data} matching criteria that will match if any of the elements in the array
   * field match any of the given matching criteria.
   *
   * @param fieldName field name for an array field
   * @param matchingCriteria {@link Data} initial matching criteria to use in the resulta
   * @param otherMatchingCriteria Zero or more {@link Data} matching criteria to use in the result
   * @return {@link Data} matching criteria that will match if any of the given matching criteria
   *     match.
   */
  @PluginFunction
  public static Data arrayAnyOf(
      RuntimeContext ctx, String fieldName, Data matchingCriteria, Data... otherMatchingCriteria) {
    return StableIdMatchingDsl.arrayAnyOf(ctx, fieldName, matchingCriteria, otherMatchingCriteria);
  }

  /**
   * Creates {@link Data} matching criteria that will match if all of the given matching criteria
   * match. This operation is assumed to be in the context of a container.
   *
   * @param matchingCriteria {@link Data} initial matching criteria to use in the result
   * @param otherMatchingCriteria Zero or more {@link Data} matching criteria to use in the result
   * @return {@link Data} matching criteria that will match if all of the given matching criteria
   *     <p>match.
   */
  @PluginFunction
  public static Data allOf(
      RuntimeContext ctx, Data matchingCriteria, Data... otherMatchingCriteria) {
    return StableIdMatchingDsl.allOf(ctx, matchingCriteria, otherMatchingCriteria);
  }

  /**
   * Creates {@link Data} matching criteria that will match if all of the elements in the array
   * field match all of the given matching criteria.
   *
   * @param fieldName field name for an array field
   * @param matchingCriteria {@link Data} initial matching criteria to use in the result
   * @param otherMatchingCriteria Zero or more {@link Data} matching criteria to usein the result
   * @return {@link Data} matching criteria that will match if any of the given matching criteria
   *     match.
   */
  @PluginFunction
  public static Data arrayAllOf(
      RuntimeContext ctx, String fieldName, Data matchingCriteria, Data... otherMatchingCriteria) {
    return StableIdMatchingDsl.arrayAllOf(ctx, fieldName, matchingCriteria, otherMatchingCriteria);
  }

  /**
   * Creates {@link Data} matching criteria that will match using a simple primitive comparison
   * using the given field.
   *
   * @param fieldName String representing the field name.
   * @return {@link Data} matching criteria that will match if values for the given field match.
   */
  @PluginFunction
  public static Data primitive(RuntimeContext ctx, String fieldName) {
    return StableIdMatchingDsl.primitive(ctx, fieldName);
  }

  /**
   * Creates {@link Data} matching criteria that will matching using matching criteria rooted at the
   * given field name.
   *
   * @param fieldName String specifying the field for which the matching criteria is relative to.
   * @param matchingCriteria {@link Data} arbitrary matching criteria.
   * @return {@link Data} matching criteria which is applied relative to the given field name.
   */
  @PluginFunction
  public static Data pathTo(RuntimeContext ctx, String fieldName, Data matchingCriteria) {
    return StableIdMatchingDsl.pathTo(ctx, fieldName, matchingCriteria);
  }

  /**
   * Creates a {@link Data} filter for filtering containers in an array.
   *
   * @param fieldName String naming the container field to filter
   * @param fieldValue {@link Data} Primitive specifying the value of the named field is filtered.
   * @return A {@link Data} filter.
   */
  @PluginFunction
  public static Data filter(RuntimeContext ctx, String fieldName, Data fieldValue) {
    return StableIdMatchingDsl.filter(ctx, fieldName, fieldValue);
  }

  /**
   * Creates {@link Data} matching criteria to match fhir references rooted at the given field.
   *
   * @param fieldName String specifying the field for which the reference matching criteria is
   *     relative to.
   * @return {@link Data} reference matching criteria which is applied relative to the given field
   *     name.
   */
  @PluginFunction
  public static Data referenceFor(RuntimeContext ctx, String fieldName) {
    return StableIdMatchingDsl.referenceFor(ctx, fieldName);
  }

  /**
   * Creates {@link Data} matching criteria for a primitive field that applies only when the value
   * matches the given filter. This is best used in conjunction with other rules. For example:
   *
   * <p>recon::arrayAnyOf("myArray", recon::filterValue(["value1", "value2"]))
   *
   * <p>The above config will match entries in an array of primitives where values match value1 or
   * value2.
   *
   * @param fieldValues The field values to filter on. This can be an array of primitives or a
   *     primitive.
   * @return {@link Data} representing the matching criteria.
   */
  @PluginFunction
  public static Data filterValue(RuntimeContext ctx, Data fieldValues) {
    return StableIdMatchingDsl.filterValue(ctx, fieldValues);
  }

  /**
   * Creates {@link Data} matching criteria for matching any meta.tag. Filters can be applied to
   * identifiers based on "system" or "code".
   *
   * @param fieldFilters Zero or more {@link Data} filters to apply. FieldFilters are of the form:
   *     recon::filterField{ "system", ["system1", "system2"] }
   * @return {@link Data} matching criteria that can match against any identifier with filters
   *     applied.
   */
  @PluginFunction
  public static Data anyMetaTag(RuntimeContext ctx, Data... fieldFilters) {
    return StableIdMatchingDsl.anyMetaTag(ctx, fieldFilters);
  }

  /**
   * Creates {@link Data} matching criteria for matching any meta.extension. Filters can be applied
   * to identifiers based on "url" or "valueString".
   *
   * @param fieldFilters Zero or more {@link Data} filters to apply. FieldFilters are of the form:
   *     recon::filterField{ "system", ["system1", "system2"] }
   * @return {@link Data} matching criteria that can match against any identifier with filters
   *     applied.
   */
  @PluginFunction
  public static Data anyMetaExtension(RuntimeContext ctx, Data... fieldFilters) {
    return StableIdMatchingDsl.anyMetaExtension(ctx, fieldFilters);
  }

  /**
   * Creates {@link Data} matching criteria for a primitive field that applies only when the field
   * value matches the given filter. This is best used in conjunction with other rules. For example:
   *
   * <p>recon::arrayAnyOf("myArray", recon::filterField("field1", ["value1"]))
   *
   * <p>The above config will match entries in myArray where field1 matches value1.
   *
   * @param fieldName The field name to match
   * @param fieldValues The field values to filter on. This can be an array of primitives or a
   *     primitive.
   * @return {@link Data} representing the matching criteria.
   */
  @PluginFunction
  public static Data filterField(RuntimeContext ctx, String fieldName, Data fieldValues) {
    return StableIdMatchingDsl.filterField(ctx, fieldName, fieldValues);
  }

  /**
   * extractPropertyValues takes in a FHIR resource and matching configuration for its resourceType
   * and extracts property values for this resource.
   *
   * @param ctx {@link RuntimeContext} object to allow access to {@link Pipeline} object.
   * @param config the matching criteria config.
   * @param resource the FHIR resource.
   * @return Array of parsed property-values.
   * @throws MatchingCriteriaConfigException when input matching config is invalid.
   * @throws PropertyValueFetcherException when property-values fetching fails.
   */
  @PluginFunction
  public Data extractPropertyValues(RuntimeContext ctx, Data config, Data resource)
      throws MatchingCriteriaConfigException, PropertyValueFetcherException {
    List<String> propertyValues =
        PropertyValueFetcher.from(resource, MatchingCriteria.of(config.asContainer())).fetch();

    if (propertyValues.isEmpty()) {
      return NullData.instance;
    }

    return ctx.getDataTypeImplementation()
        .arrayOf(
            propertyValues.stream()
                .map(ctx.getDataTypeImplementation()::primitiveOf)
                .collect(toImmutableList()));
  }

  /**
   * buildReferenceFor takes in a reference fragment resource and stores it under runtime context
   * metadata for post-processing.
   *
   * <p><b>Example Usage:</b>
   *
   * <pre><code>
   *   subject.reference: recon::buildReferenceFor({
   *     resourceType: Patient,
   *     id: patient-id,
   *     identifier: [{identifier value}]
   *   });
   * </code></pre>
   *
   * @param ctx {@link RuntimeContext} object to allow access to {@link Pipeline} object.
   * @param referenceFragment the fragment resource for reference building.
   * @return reference in format {resourceType/resourceId}
   */
  @PluginFunction
  public Primitive buildReferenceFor(RuntimeContext ctx, Data referenceFragment) {
    Array fragments = ctx.getMetaData().getSerializableMeta(FRAGMENTS_FIELD);
    if (fragments == null) {
      fragments = ctx.getDataTypeImplementation().emptyArray();
    }

    // Append the reference fragment into the fragments array.
    fragments.setElement(fragments.size(), referenceFragment);
    ctx.getMetaData().setSerializableMeta(FRAGMENTS_FIELD, fragments);

    String resourceType =
        referenceFragment.asContainer().getField("resourceType").asPrimitive().string();
    String resourceId = referenceFragment.asContainer().getField("id").asPrimitive().string();

    return ctx.getDataTypeImplementation()
        .primitiveOf(String.format("%s/%s", resourceType, resourceId));
  }
}
