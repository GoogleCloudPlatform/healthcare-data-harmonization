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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer.Harmonization.HARMONIZATION_SOURCE_LOCAL;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer.LocalHarmonizer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Initializes a {@link LocalHarmonizer} based on input json files with `.conceptmap.json` suffix.
 * The input bytes should be json file representing a ConceptMap. For example,
 * "github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_configs/hl7v2_fhir_stu3/code_harmonization/Allergy_Type.harmonization.json".
 */
public class LocalHarmonizationParser extends HarmonizationParserBase {
  static final String PATH_SUFFIX = ".harmonization.json";

  @Override
  public String getName() {
    return "harmonization_parser";
  }

  @Override
  public void parse(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath) {
    try {
      Reader reader = new InputStreamReader(new ByteArrayInputStream(data));
      JsonObject jsonObject = JsonParser.parseReader(reader).getAsJsonObject();
      LocalHarmonizer localHarmonizer =
          (LocalHarmonizer) getHarmonizer(metaData, HARMONIZATION_SOURCE_LOCAL);
      if (localHarmonizer == null) {
        localHarmonizer = new LocalHarmonizer(jsonObject);
      } else {
        localHarmonizer.addAnotherConceptmap(jsonObject);
      }
      addHarmonizer(metaData, HARMONIZATION_SOURCE_LOCAL, localHarmonizer);
    } catch (Exception e) {
      throw new JsonParseException(
          String.format("Failed to parse ConceptMap %s.", iPath.toString()), e);
    }
  }

  @Override
  public boolean canParse(ImportPath path) {
    return path.getFileName().endsWith(PATH_SUFFIX);
  }
}
