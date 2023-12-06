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

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.AND_OPERATOR;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.ARRAY_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD_FILTER;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PATHS;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PATH_OPERATOR;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.PRIMITIVE_FIELD_TYPE;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.VALID_FIELD_TYPES;
import static com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.StableIdConsts.VALID_OPERATORS;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.exceptions.MatchingCriteriaConfigException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid.match.PropertyValueFetcher.PathOperator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * MatchingCriteria is responsible for validating (private func ensureValid()) and creating
 * MatchingCriteria children by parsing the config file.
 */
public class MatchingCriteria implements Serializable {

  private final String field;
  private final String fieldType;
  private final List<String> fieldFilter;
  private final PathOperator pathOperator;
  private final List<MatchingCriteria> children;

  /**
   * MatchingCriteria is responsible for validating and parsing a matching criteria config file. The
   * function createChildren() results in a tree of MatchingCriteria, in other words, a given
   * MatchingCriteria may have multiple children MatchingCriteria representing the parsing rules for
   * property value extraction.
   *
   * @param config the config file from which we extract matching criteria
   */
  private MatchingCriteria(Container config) throws MatchingCriteriaConfigException {
    this.field = getField(config);
    this.fieldType = getFieldType(config);
    this.fieldFilter = getFieldFilter(config);
    this.pathOperator = getPathOperator(config);
    this.children = createChildren(config);
    ensureValid();
  }

  private MatchingCriteria(Builder builder) {
    this.field = builder.field;
    this.fieldType = builder.fieldType;
    this.fieldFilter = builder.fieldFilter;
    this.pathOperator = builder.pathOperator;
    this.children = new ArrayList<>(builder.children);
  }

  public static MatchingCriteria of(Container config) throws MatchingCriteriaConfigException {
    return new MatchingCriteria(config);
  }
  /**
   * Parses the given matching criteria iteratively such that each ConfigParser may have multiple
   * ConfigParsers.
   */
  private List<MatchingCriteria> createChildren(Container config)
      throws MatchingCriteriaConfigException {
    List<Container> paths = getPaths(config);
    List<MatchingCriteria> children = new ArrayList<>();
    for (Container p : paths) {
      children.add(new MatchingCriteria(p));
    }
    return children;
  }

  @Nonnull
  public String field() {
    return this.field;
  }

  @Nonnull
  public String fieldType() {
    return this.fieldType;
  }

  @Nonnull
  public List<String> fieldFilter() {
    return this.fieldFilter;
  }

  @Nonnull
  public PathOperator pathOperator() {
    return this.pathOperator;
  }

  @Nonnull
  public List<MatchingCriteria> children() {
    return this.children;
  }

  /**
   * Checks if a matching criterion is valid using predefined rules. This func validates the
   * combination of field values in a given matching criteria. For example: when matching criteria
   * is 'fieldType' = 'primitive', 'paths' field cannot exist.
   */
  private void ensureValid() throws MatchingCriteriaConfigException {
    if (fieldType.equals(PRIMITIVE_FIELD_TYPE)) {
      if (!children.isEmpty()) {
        throw new MatchingCriteriaConfigException(
            " Config with 'fieldType' = 'primitive' must have non-nil 'paths'. ");
      }
    } else if (children.isEmpty()) {
      throw new MatchingCriteriaConfigException(
          " Empty 'paths' cannot coexist with non-primitive 'fieldType'. ");
    } else if (!fieldFilter.isEmpty()) {
      throw new MatchingCriteriaConfigException(
          " 'fieldFilter' must coexist with 'fieldType' = 'primitive'. ");
    }

    if (fieldType.equals(ARRAY_FIELD_TYPE) && field.isEmpty()) {
      throw new MatchingCriteriaConfigException(
          " Config with 'fieldType': 'array' must have non-nil 'field'. ");
    }
  }

  /**
   * The following getXXX() methods validate each MatchingCriteria field value independent of other
   * field values.
   */
  private String getField(Container config) throws MatchingCriteriaConfigException {
    Data fieldData = config.getField(FIELD);
    if (fieldData.isNullOrEmpty()) {
      return "";
    }
    if (!fieldData.isPrimitive()) {
      throw new MatchingCriteriaConfigException(
          "Matching criteria 'field' must be primitive, the config file is malformed.");
    }
    return fieldData.asPrimitive().string();
  }

  private String getFieldType(Container config) throws MatchingCriteriaConfigException {
    Data fieldTypeData = config.getField(FIELD_TYPE);
    if (fieldTypeData.isNullOrEmpty()) {
      throw new MatchingCriteriaConfigException(" Missing value for 'fieldType'. ");
    }
    if (!fieldTypeData.isPrimitive()) {
      throw new MatchingCriteriaConfigException(
          " Matching criteria 'fieldType' must be a primitive, the config file is malformed. ");
    }
    String fieldTypeStr = fieldTypeData.asPrimitive().string();
    if (!VALID_FIELD_TYPES.contains(fieldTypeStr)) {
      throw new MatchingCriteriaConfigException(
          " Missing valid 'fieldType', has to be 'container'/'array'/'primitive'. ");
    }
    return fieldTypeStr;
  }

  private List<String> getFieldFilter(Container config) throws MatchingCriteriaConfigException {
    Data fieldFilterData = config.getField(FIELD_FILTER);
    if (!fieldFilterData.isArray()) {
      throw new MatchingCriteriaConfigException(
          " Matching criteria 'fieldFilter' must be an array, the config file is malformed. ");
    }
    List<String> fieldFilter = new ArrayList<>();
    for (int i = 0; i < fieldFilterData.asArray().size(); ++i) {
      Data element = fieldFilterData.asArray().getElement(i);
      if (!element.isPrimitive()) {
        throw new MatchingCriteriaConfigException(
            " Matching criteria 'fieldFilter' must be an array of primitives, the config file is"
                + " malformed. ");
      }
      fieldFilter.add(element.asPrimitive().string());
    }
    return fieldFilter;
  }

  private PathOperator getPathOperator(Container config) throws MatchingCriteriaConfigException {
    Data pathOperatorData = config.getField(PATH_OPERATOR);
    if (pathOperatorData.isNullOrEmpty()) {
      return PathOperator.OR;
    }
    if (!pathOperatorData.isPrimitive()
        || (pathOperatorData.isPrimitive()
            && !VALID_OPERATORS.contains(pathOperatorData.asPrimitive().string()))) {
      throw new MatchingCriteriaConfigException(
          String.format(
              " Matching criteria 'pathOperator' = '%s' must be a primitive of the value"
                  + " 'AND'/'OR'. ",
              pathOperatorData));
    }
    if (pathOperatorData.asPrimitive().string().equals(AND_OPERATOR)) {
      return PathOperator.AND;
    }
    return PathOperator.OR;
  }

  private List<Container> getPaths(Container config) throws MatchingCriteriaConfigException {
    Data pathsData = config.getField(PATHS);
    if (!pathsData.isArray()) {
      throw new MatchingCriteriaConfigException(
          " Matching criteria 'paths' must be an array of matching criteria, the config file is"
              + " malformed. ");
    }
    List<Container> paths = new ArrayList<>();
    for (int i = 0; i < pathsData.asArray().size(); ++i) {
      Data element = pathsData.asArray().getElement(i);
      if (!element.isContainer()) {
        throw new MatchingCriteriaConfigException(
            " Matching criteria 'paths' must be an array of matching criteria containers, the"
                + " config file is malformed. ");
      }
      paths.add(element.asContainer());
    }
    return paths;
  }

  /** Builds MatchingCriteria. */
  protected static class Builder {
    private String field = "";
    private String fieldType;
    private List<String> fieldFilter = new ArrayList<>();
    private PathOperator pathOperator = PathOperator.OR;
    private final List<MatchingCriteria> children = new ArrayList<>();

    public Builder setField(String field) {
      this.field = field;
      return this;
    }

    public Builder setFieldType(String fieldType) {
      this.fieldType = fieldType;
      return this;
    }

    public Builder setFieldFilter(List<String> fieldFilter) {
      this.fieldFilter = fieldFilter;
      return this;
    }

    public Builder setPathOperator(PathOperator op) {
      this.pathOperator = op;
      return this;
    }

    public Builder addChild(MatchingCriteria other) {
      this.children.add(other);
      return this;
    }

    public MatchingCriteria build() throws MatchingCriteriaConfigException {
      return new MatchingCriteria(this);
    }
  }
}
