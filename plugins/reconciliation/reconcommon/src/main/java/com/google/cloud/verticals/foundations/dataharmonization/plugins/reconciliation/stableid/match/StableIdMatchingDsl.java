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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.ARRAY_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.CODE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.CODING_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.CONTAINER_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.EXTENSION_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD_FILTER;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.IDENTIFIER_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.META_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PATHS;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PATH_OPERATOR;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PRIMITIVE_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.REFERENCE_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.SYSTEM_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.TAG_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.URL_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.VALUESTRING_FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.VALUE_FIELD;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher.PathOperator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** MatchingDsl contains functions to build properly formatted matching criteria. */
public class StableIdMatchingDsl implements Serializable {

  private static final String FILTER_FIELD = "field";
  private static final String FILTER_VALUE = "value";

  private StableIdMatchingDsl() {}

  /**
   * Creates {@link Data} matching criteria for matching any identifier. Filters can be applied to
   * identifiers based on "system" or "value".
   *
   * @param filters Zero or more {@link Data} filters to apply. Filters are of the form: { "field":
   *     "field", // One of "system" or "value" "value": "value" }
   * @return {@link Data} matching criteria that can match against any identifier with filters
   *     applied.
   */
  public static Data anyIdentifier(RuntimeContext ctx, Data... filters) {
    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        ARRAY_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(IDENTIFIER_FIELD),
        allOf(
            ctx,
            applyFilter(ctx, primitive(ctx, SYSTEM_FIELD), Arrays.asList(filters)),
            applyFilter(ctx, primitive(ctx, VALUE_FIELD), Arrays.asList(filters))));
  }

  /**
   * Creates {@link Data} matching criteria for matching any coding. Filters can be applied to
   * codings.
   *
   * @param filters Zero or more {@link Data} filters to apply. Filters are of the form: { "field":
   *     "field", // One of "system" or "code" "value": "value" }
   * @return {@link Data} matching criteria that can match against any coding with filters applied.
   */
  public static Data anyCoding(RuntimeContext ctx, Data... filters) {
    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        ARRAY_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(CODING_FIELD),
        allOf(
            ctx,
            applyFilter(ctx, primitive(ctx, SYSTEM_FIELD), Arrays.asList(filters)),
            applyFilter(ctx, primitive(ctx, CODE_FIELD), Arrays.asList(filters))));
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
  public static Data anyOf(
      RuntimeContext ctx, Data matchingCriteria, Data... otherMatchingCriteria) {
    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        CONTAINER_FIELD_TYPE,
        NullData.instance,
        matchingCriteria,
        otherMatchingCriteria);
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
  public static Data arrayAnyOf(
      RuntimeContext ctx, String fieldName, Data matchingCriteria, Data... otherMatchingCriteria) {
    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        ARRAY_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(fieldName),
        matchingCriteria,
        otherMatchingCriteria);
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
  public static Data allOf(
      RuntimeContext ctx, Data matchingCriteria, Data... otherMatchingCriteria) {
    return createMatchingCriteria(
        ctx,
        PathOperator.AND,
        CONTAINER_FIELD_TYPE,
        NullData.instance,
        matchingCriteria,
        otherMatchingCriteria);
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
  public static Data arrayAllOf(
      RuntimeContext ctx, String fieldName, Data matchingCriteria, Data... otherMatchingCriteria) {
    return createMatchingCriteria(
        ctx,
        PathOperator.AND,
        ARRAY_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(fieldName),
        matchingCriteria,
        otherMatchingCriteria);
  }

  /**
   * Creates {@link Data} matching criteria that will match using a simple primitive comparison
   * using the given field.
   *
   * @param fieldName String representing the field name.
   * @return {@link Data} matching criteria that will match if values for the given field match.
   */
  public static Data primitive(RuntimeContext ctx, String fieldName) {
    Primitive fieldAsPrimitive = ctx.getDataTypeImplementation().primitiveOf(fieldName);
    return ctx.getDataTypeImplementation()
        .containerOf(
            ImmutableMap.of(
                FIELD,
                fieldAsPrimitive,
                FIELD_TYPE,
                ctx.getDataTypeImplementation().primitiveOf(PRIMITIVE_FIELD_TYPE)));
  }

  /**
   * Creates {@link Data} matching criteria that will matching using matching criteria rooted at the
   * given field name.
   *
   * @param fieldName String specifying the field for which the matching criteria is relative to.
   * @param matchingCriteria {@link Data} arbitrary matching criteria.
   * @return {@link Data} matching criteria which is applied relative to the given field name.
   */
  public static Data pathTo(RuntimeContext ctx, String fieldName, Data matchingCriteria) {
    Primitive fieldAsPrimitive = ctx.getDataTypeImplementation().primitiveOf(fieldName);
    return createMatchingCriteria(
        ctx, null, CONTAINER_FIELD_TYPE, fieldAsPrimitive, matchingCriteria);
  }

  /**
   * Creates a {@link Data} filter for filtering containers in an array.
   *
   * @param fieldName String naming the container field to filter
   * @param fieldValue {@link Data} Primitive specifying the value of the named field is filtered.
   * @return A {@link Data} filter.
   */
  public static Data filter(RuntimeContext ctx, String fieldName, Data fieldValue) {
    Primitive fieldAsPrimitive = ctx.getDataTypeImplementation().primitiveOf(fieldName);
    return ctx.getDataTypeImplementation()
        .containerOf(
            ImmutableMap.of(
                FILTER_FIELD, fieldAsPrimitive,
                FILTER_VALUE, fieldValue));
  }

  /**
   * Creates {@link Data} matching criteria to match fhir references rooted at the given field.
   *
   * @param fieldName String specifying the field for which the reference matching criteria is
   *     relative to.
   * @return {@link Data} reference matching criteria which is applied relative to the given field
   *     name.
   */
  public static Data referenceFor(RuntimeContext ctx, String fieldName) {
    return pathTo(ctx, fieldName, primitive(ctx, REFERENCE_FIELD));
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
  public static Data filterValue(RuntimeContext ctx, Data fieldValues) {
    return filterField(ctx, "", fieldValues);
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
  public static Data anyMetaTag(RuntimeContext ctx, Data... fieldFilters) {
    validateFieldFilters(fieldFilters);
    List<Data> systemFilters = getFilterValuesForFieldName(SYSTEM_FIELD, fieldFilters);
    List<Data> codeFilters = getFilterValuesForFieldName(CODE_FIELD, fieldFilters);

    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        CONTAINER_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(META_FIELD),
        createMatchingCriteria(
            ctx,
            PathOperator.OR,
            ARRAY_FIELD_TYPE,
            ctx.getDataTypeImplementation().primitiveOf(TAG_FIELD),
            allOf(
                ctx,
                createMatchingConfigWithFilters(
                    ctx, SYSTEM_FIELD, ctx.getDataTypeImplementation().arrayOf(systemFilters)),
                createMatchingConfigWithFilters(
                    ctx, CODE_FIELD, ctx.getDataTypeImplementation().arrayOf(codeFilters)))));
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
  public static Data anyMetaExtension(RuntimeContext ctx, Data... fieldFilters) {
    validateFieldFilters(fieldFilters);
    List<Data> urlFilters = getFilterValuesForFieldName(URL_FIELD, fieldFilters);
    List<Data> valueStringFilters = getFilterValuesForFieldName(VALUESTRING_FIELD, fieldFilters);

    return createMatchingCriteria(
        ctx,
        PathOperator.OR,
        CONTAINER_FIELD_TYPE,
        ctx.getDataTypeImplementation().primitiveOf(META_FIELD),
        createMatchingCriteria(
            ctx,
            PathOperator.OR,
            ARRAY_FIELD_TYPE,
            ctx.getDataTypeImplementation().primitiveOf(EXTENSION_FIELD),
            allOf(
                ctx,
                createMatchingConfigWithFilters(
                    ctx, URL_FIELD, ctx.getDataTypeImplementation().arrayOf(urlFilters)),
                createMatchingConfigWithFilters(
                    ctx,
                    VALUESTRING_FIELD,
                    ctx.getDataTypeImplementation().arrayOf(valueStringFilters)))));
  }

  /**
   * validates the fieldFilter fields. These are specific matching configs of the type `primitive`.
   * Currently only works with 'system' and 'code' fields.
   *
   * @param fieldFilters matching configs to be validated.
   * @throws IllegalArgumentException when matching config is not created by (or not similar to)
   *     recon::filterField(), or the field name is not `system` or `code`.
   */
  private static void validateFieldFilters(Data... fieldFilters) {
    if (fieldFilters.length == 0) {
      return;
    }
    for (int i = 0; i < fieldFilters.length; ++i) {
      if (!fieldFilters[i].isContainer()
          || !fieldFilters[i]
              .asContainer()
              .getField(FIELD_TYPE)
              .toString()
              .equals(PRIMITIVE_FIELD_TYPE)) {
        throw new IllegalArgumentException(
            "fieldFilters need to work with recon::filterField() plugin.");
      }
      Container cur = fieldFilters[i].asContainer();
      if (!cur.getField(FIELD).toString().equals(SYSTEM_FIELD)
          && !cur.getField(FIELD).toString().equals(CODE_FIELD)
          && !cur.getField(FIELD).toString().equals(URL_FIELD)
          && !cur.getField(FIELD).toString().equals(VALUESTRING_FIELD)) {
        throw new IllegalArgumentException(
            "Matching config field has to be one of \"system\", \"code\", \"url\" or"
                + " \"valueString\", got: "
                + cur.getField(FIELD));
      }
    }
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
  public static Data filterField(RuntimeContext ctx, String fieldName, Data fieldValues) {
    return createMatchingConfigWithFilters(ctx, fieldName, fieldValues);
  }

  private static Data applyFilter(RuntimeContext ctx, Data config, List<Data> filters) {
    if (!config.isContainer()) {
      throw new IllegalArgumentException("Config must be a container.");
    }

    Container container = config.asContainer();
    if (!container.getField(FIELD_TYPE).asPrimitive().string().equals(PRIMITIVE_FIELD_TYPE)) {
      throw new IllegalArgumentException("Filters can only be applied to primitive types.");
    }

    String field = container.getField(FIELD).asPrimitive().string();
    ImmutableList<Data> filtersToApply =
        filters.stream()
            .map(Data::asContainer)
            .filter(
                f -> {
                  Data filterField = f.getField(FILTER_FIELD);
                  if (filterField.isNullOrEmpty() && Strings.isNullOrEmpty(field)) {
                    return true;
                  }
                  return f.getField(FILTER_FIELD).asPrimitive().string().equals(field);
                })
            .map(f -> f.getField(FILTER_VALUE))
            .collect(toImmutableList());
    if (!filtersToApply.isEmpty()) {
      container =
          container.setField(FIELD_FILTER, ctx.getDataTypeImplementation().arrayOf(filtersToApply));
    }
    return container;
  }

  /**
   * Creates a matching criteria that works with primitive type only, with/without filters.
   *
   * @param fieldName field name for the primitive matching config, can be omitted if doesn't apply.
   * @param filterValues Can be either primitive or array of primitives. If empty, no filter values
   *     will be applied.
   * @return {@link Data} matching config which allows primitive type matching.
   */
  private static Data createMatchingConfigWithFilters(
      RuntimeContext ctx, String fieldName, Data filterValues) {
    if (filterValues.isContainer()) {
      throw new IllegalArgumentException(
          "Matching Criteria filter cannot be a container, got " + filterValues);
    }
    Container matchingConfig = ctx.getDataTypeImplementation().emptyContainer();
    matchingConfig.setField(
        FIELD_TYPE, ctx.getDataTypeImplementation().primitiveOf(PRIMITIVE_FIELD_TYPE));
    if (!fieldName.isBlank()) {
      matchingConfig.setField(FIELD, ctx.getDataTypeImplementation().primitiveOf(fieldName));
    }
    if (!filterValues.isNullOrEmpty()) {
      List<Data> filters = new ArrayList<>();
      if (filterValues.isPrimitive()) {
        filters.add(filterValues.asPrimitive());
      } else {
        for (int i = 0; i < filterValues.asArray().size(); ++i) {
          Data cur = filterValues.asArray().getElement(i);
          if (!cur.isPrimitive()) {
            throw new IllegalArgumentException(
                "fieldFilter has to be an array of primitives, got: " + cur);
          }
          filters.add(cur.asPrimitive());
        }
      }
      matchingConfig.setField(FIELD_FILTER, ctx.getDataTypeImplementation().arrayOf(filters));
    }
    return matchingConfig;
  }

  /**
   * Gets filter values from a number of matching configs that are created from recon::fieldFilter()
   * for a specific field name.
   *
   * @param fieldName the name of the field we are interested in
   * @param fieldFilters multiple matching configs.
   * @return A list of {@link Data} that represents filter values that are extracted from given
   *     matching configs.
   */
  private static List<Data> getFilterValuesForFieldName(String fieldName, Data... fieldFilters) {
    List<Data> result = new ArrayList<>();
    stream(fieldFilters)
        .filter(f -> f.asContainer().getField(FIELD).toString().equals(fieldName))
        .forEach(
            f ->
                result.addAll(
                    f.asContainer().getField(FIELD_FILTER).asArray().stream()
                        .collect(toImmutableList())));
    return result;
  }

  private static Container createMatchingCriteria(
      RuntimeContext ctx,
      PathOperator pathOperator,
      String fieldType,
      Primitive fieldName,
      Data path,
      Data... otherPaths) {
    Container container = ctx.getDataTypeImplementation().emptyContainer();
    if (!fieldName.isNullOrEmpty()) {
      container = container.setField(FIELD, fieldName);
    }
    container =
        container.setField(FIELD_TYPE, ctx.getDataTypeImplementation().primitiveOf(fieldType));
    if (pathOperator != null) {
      container =
          container.setField(
              PATH_OPERATOR, ctx.getDataTypeImplementation().primitiveOf(pathOperator.toString()));
    }
    container = container.setField(PATHS, arrayOf(ctx, path, otherPaths));
    return container;
  }

  private static Array arrayOf(RuntimeContext ctx, Data first, Data... rest) {
    List<Data> all = new ArrayList<>();
    all.add(first);
    Collections.addAll(all, rest);
    return ctx.getDataTypeImplementation().arrayOf(all);
  }
}
