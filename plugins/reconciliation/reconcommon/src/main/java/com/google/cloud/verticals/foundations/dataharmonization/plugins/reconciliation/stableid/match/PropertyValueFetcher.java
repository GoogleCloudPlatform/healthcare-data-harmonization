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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.AND_CONNECTOR;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.ARRAY_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.CONTAINER_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.EQUALS_CONNECTOR;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PRIMITIVE_FIELD_TYPE;
import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.PropertyValueFetcherException;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * PropertyValueFetchers are responsible for: (1) Identifying mismatches between MatchingCriteria
 * {@link MatchingCriteria} and a given resource; (2) Fetching primitive propertyValues; (3) Merging
 * propertyValues by applying pathOperators;
 */
public class PropertyValueFetcher implements Serializable {

  private final Data resource;
  private final String field;
  private final String fieldType;
  private final List<String> fieldFilter;
  private final PathOperator pathOperator;
  private final List<List<PropertyValueFetcher>> children;

  /** Merges individual child propertyValues and outputs final fetched results. */
  public List<String> fetch() {
    if (fieldType.equals(PRIMITIVE_FIELD_TYPE)) {
      return fetchPrimitive();
    }
    List<List<String>> propertyValues = new ArrayList<>();
    for (List<PropertyValueFetcher> fetchers : this.children) {
      List<String> res = new ArrayList<>();
      for (PropertyValueFetcher fetcher : fetchers) {
        res.addAll(fetcher.fetch());
      }
      propertyValues.add(res);
    }
    return pathOperator.merge(propertyValues, field).stream()
        .distinct()
        .collect(toCollection(ArrayList::new));
  }

  private List<String> fetchPrimitive() {
    List<String> res = new ArrayList<>();
    String value = resource.asPrimitive().toString();
    if (Strings.isNullOrEmpty(value) || (!fieldFilter.isEmpty() && !fieldFilter.contains(value))) {
      return res;
    }
    res.add(addFieldPrefix(field, value));
    return res;
  }

  private static String addFieldPrefix(String field, String value) {
    return Strings.isNullOrEmpty(field) ? value : field + EQUALS_CONNECTOR + value;
  }

  private boolean resourceMatchesFieldType(Data resource, String fieldType)
      throws PropertyValueFetcherException {
    switch (fieldType) {
      case ARRAY_FIELD_TYPE:
        return resource.isArray();
      case PRIMITIVE_FIELD_TYPE:
        return resource.isPrimitive();
      case CONTAINER_FIELD_TYPE:
        return resource.isContainer();
      default:
        throw new PropertyValueFetcherException(
            String.format(
                " 'resource' = '%s' does not match the expected 'fieldType' = '%s'. ",
                resource, fieldType));
    }
  }

  private Data fetchAsFieldType(Data resource, String fieldType)
      throws PropertyValueFetcherException {
    switch (fieldType) {
      case ARRAY_FIELD_TYPE:
        return resource.asArray();
      case PRIMITIVE_FIELD_TYPE:
        return resource.asPrimitive();
      case CONTAINER_FIELD_TYPE:
        return resource.asContainer();
      default:
        throw new PropertyValueFetcherException(
            String.format(
                " Failed to fetch 'resource' = '%s' as 'fieldType' = '%s'. ", resource, fieldType));
    }
  }

  /**
   * (1) Identifies mismatch between resource format and expected fieldType (ultimately specified in
   * the Matching Criteria Config Files).
   * (2) When 'field' presents, returns the value of that field; otherwise the current working
   * resource is the input resource.
   */
  private Data getWorkingResource(Data resource) throws PropertyValueFetcherException {
    if (Strings.isNullOrEmpty(field)) {
      if (!resourceMatchesFieldType(resource, fieldType)) {
        throw new PropertyValueFetcherException(
            String.format(
                " Config has empty 'field', given 'resource' = '%s' does not match 'fieldType' ="
                    + " '%s'. ",
                resource, fieldType));
      }
      return resource;
    }

    // When 'field' is present, the resource is expected to be a container and fieldType should
    // match field value.
    if (!resourceMatchesFieldType(resource, CONTAINER_FIELD_TYPE)) {
      throw new PropertyValueFetcherException(
          String.format(
              " Config has 'field' = '%s' but 'resource' = '%s' is not a container. ",
              field, resource));
    }
    Data subResource = resource.asContainer().getField(field);
    if (!resourceMatchesFieldType(subResource, fieldType)) {
      throw new PropertyValueFetcherException(
          String.format(
              " Config has 'field' = '%s' but 'resource' = '%s' does not match 'fieldType' = '%s'."
                  + " ",
              field, subResource, fieldType));
    }
    return fetchAsFieldType(subResource, fieldType);
  }

  private PropertyValueFetcher(Data input, MatchingCriteria mc)
      throws PropertyValueFetcherException {
    this.field = mc.field();
    this.fieldType = mc.fieldType();
    this.fieldFilter = mc.fieldFilter();
    this.pathOperator = mc.pathOperator();
    this.children = new ArrayList<>();
    this.resource = getWorkingResource(input);

    if (fieldType.equals(ARRAY_FIELD_TYPE)) {
      // With array resource, apply child matching criteria to each resource element.
      for (int i = 0; i < resource.asArray().size(); ++i) {
        List<PropertyValueFetcher> fetchers = new ArrayList<>();
        for (MatchingCriteria child : mc.children()) {
          PropertyValueFetcher fetcher =
              new PropertyValueFetcher(resource.asArray().getElement(i), child);
          fetchers.add(fetcher);
        }
        children.add(fetchers);
      }
    } else {
      for (MatchingCriteria child : mc.children()) {
        PropertyValueFetcher fetcher = new PropertyValueFetcher(resource, child);
        children.add(ImmutableList.of(fetcher));
      }
    }
  }

  public static PropertyValueFetcher from(Data input, MatchingCriteria mc)
      throws PropertyValueFetcherException {
    return new PropertyValueFetcher(input, mc);
  }

  /** Represents operations to be performed on matching criteria paths. */
  public enum PathOperator {
    AND {
      @Override
      protected ImmutableList<String> merge(List<List<String>> propertyValues, String fieldPrefix) {
        // For an AND, we want to join all the elements into a single string using the
        // AND_CONNECTOR. However, if we failed to find any values (aka if there are any empty
        // lists), we can't combine the value to create unique matching criteria.
        boolean containsEmptyList = propertyValues.stream().anyMatch(List::isEmpty);
        if (!containsEmptyList) {
          // Return all combinations of the property-values.
          return Lists.cartesianProduct(propertyValues).stream()
              .map(
                  pvs ->
                      addFieldPrefix(
                          fieldPrefix, pvs.stream().sorted().collect(joining(AND_CONNECTOR))))
              .collect(toImmutableList());
        }
        return ImmutableList.of();
      }
    },
    OR {
      @Override
      protected ImmutableList<String> merge(List<List<String>> propertyValues, String fieldPrefix) {
        // For an OR, we return every property-value by simply flattening the List<List<String>>
        // into a List<String>.
        return propertyValues.stream()
            .filter(s -> !s.isEmpty())
            .flatMap(List::stream)
            .map(pv -> addFieldPrefix(fieldPrefix, pv))
            .collect(toImmutableList());
      }
    };

    protected abstract List<String> merge(List<List<String>> propertyValues, String fieldPrefix);
  }
}
