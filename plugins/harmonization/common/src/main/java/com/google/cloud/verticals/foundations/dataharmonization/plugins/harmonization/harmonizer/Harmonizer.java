/*
 * Copyright 2020 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import java.util.List;

/**
 * Code Harmonization is the mechanism for mapping a code in one terminology to another. This
 * Harmonization interface is used by both local and remote harmonizers. Here is an example of a
 * local ConceptMap:
 * github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/hl7v2_fhir_stu3/code_harmonization/Gender.harmonization.json
 * To find out how to send requests to a remote Harmonizer please refer to this page:
 * cloud.google.com/healthcare/docs/reference/rest/v1beta1/projects.locations.datasets.fhirStores.fhir/ConceptMap-translate
 */
public interface Harmonizer {
  /** Looks up given (code, system) in the ConceptMap identified by conceptmapID. */
  List<HarmonizedCode> harmonize(String sourceCode, String sourceSystem, String conceptmapId)
      throws IllegalArgumentException;

  /** Looks up given (code, sourceSys, targetSys) in the ConceptMap identified by conceptmapID. */
  List<HarmonizedCode> harmonizeWithTarget(
      String sourceCode, String sourceSystem, String targetSystem, String conceptmapId)
      throws IllegalArgumentException;
}
