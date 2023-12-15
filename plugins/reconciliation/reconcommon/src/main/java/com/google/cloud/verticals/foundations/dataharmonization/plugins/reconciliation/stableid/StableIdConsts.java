// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.plugins.reconciliation.stableid;

import com.google.common.collect.ImmutableSet;

/** Constants for Stable ID */
public final class StableIdConsts {
  // Matching constants.
  public static final String FIELD = "field";
  public static final String FIELD_TYPE = "fieldType";
  public static final String FIELD_FILTER = "fieldFilter";
  public static final String PATH_OPERATOR = "pathOperator";
  public static final String PATHS = "paths";
  public static final String CONTAINER_FIELD_TYPE = "container";
  public static final String PRIMITIVE_FIELD_TYPE = "primitive";
  public static final String ARRAY_FIELD_TYPE = "array";
  public static final String AND_CONNECTOR = "|";
  public static final String EQUALS_CONNECTOR = "=";
  public static final String AND_OPERATOR = "AND";
  public static final String OR_OPERATOR = "OR";
  public static final ImmutableSet<String> VALID_OPERATORS =
      ImmutableSet.of(AND_OPERATOR, OR_OPERATOR);
  public static final ImmutableSet<String> VALID_FIELD_TYPES =
      ImmutableSet.of(PRIMITIVE_FIELD_TYPE, ARRAY_FIELD_TYPE, CONTAINER_FIELD_TYPE);

  // FHIR constants.
  public static final String STABLE_ID_SYSTEM = "urn:oid:google/reconciliation-stable-id";
  public static final String SYSTEM_FIELD = "system";
  public static final String VALUE_FIELD = "value";
  public static final String URL_FIELD = "url";
  public static final String VALUESTRING_FIELD = "valueString";
  public static final String IDENTIFIER_FIELD = "identifier";
  public static final String META_FIELD = "meta";
  public static final String TAG_FIELD = "tag";
  public static final String EXTENSION_FIELD = "extension";
  public static final String ID_FIELD = "id";
  public static final String CODING_FIELD = "coding";
  public static final String CODE_FIELD = "code";
  public static final String REFERENCE_FIELD = "reference";
  public static final String RESOURCE_TYPE_FIELD = "resourceType";
  public static final String STABLE_ID_FIELD = "stableId";
  public static final String STABLE_ID_PROPERTY_VALUE_ID_FIELD = "stableIdPropertyValueId";
  public static final String TEXT_FIELD = "text";
  public static final String TYPE_FIELD = "type";
  public static final String AGENT_FIELD = "agent";
  public static final String WHO_FIELD = "who";
  public static final String DATA_SOURCE_NAME = "DATA_SOURCE_NAME";

  private StableIdConsts() {}
}
