// Copyright 2020 Google LLC.
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

/**
 * Meta data describing a top level entity/resource within a data model.
 */
export interface TopEntityMeta {
  /**
   * The name of the entity (e.g. Patient)
   */
  name: string;

  /**
   * A detailed description of the entity.
   */
  description: string;
}

/**
 * Meta data describing all sub entities/fields within a data model.
 */
export interface SubEntityMeta {
  /**
   * Name of the sub entity/field.
   */
  name: string;

  /**
   * A flag indicating whether the field is required.
   */
  required: boolean;

  /**
   * The type of the sub entity. The type could either be a primitive in the
   * data model (e.g. string, number), or a reference to another complex type
   * (e.g. Date, Identifier in FHIR).
   */
  type: string;

  /**
   * A detailed description of the sub entity or field.
   */
  description: string;

  /**
   * The JSON schema definition of the sub entity.
   */
  // tslint:disable-next-line:no-any
  schema?: any;

  /**
   * A flag indicating whether the field contains a complex type which can be
   * further explored or a primitive type that can not be further explored.
   */
  clickable: boolean;
}

/**
 * A result of a search query.
 */
export interface SearchResult {
  /**
   * The path of the search result, including the associated data model.
   * For example, FHIR > Patient > address.
   */
  path: string;

  /**
   * The name of the top level resource associated with the search result.
   */
  resource: string;

  /**
   * The field associated with the search result. If the selected search result
   * was for a top level entity, then the field will be empty.
   */
  field: string;
}
