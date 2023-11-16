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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer.Harmonizer;
import java.util.HashMap;

/** Base class for harmonization import parsers. */
public abstract class HarmonizationParserBase implements Parser {
  public static final String HARMONIZATION_META_KEY = "Harmonizers";

  protected static void addHarmonizer(MetaData metaData, String name, Harmonizer harmonizer) {
    HashMap<String, Harmonizer> storeToHarmonizer =
        metaData.getSerializableMeta(HARMONIZATION_META_KEY);
    if (storeToHarmonizer == null) {
      storeToHarmonizer = new HashMap<>();
    }

    storeToHarmonizer.put(name, harmonizer);

    metaData.setSerializableMeta(HARMONIZATION_META_KEY, storeToHarmonizer);
  }

  public static Harmonizer getHarmonizer(MetaData metaData, String name) {
    HashMap<String, Harmonizer> storeToHarmonizer =
        metaData.getSerializableMeta(HARMONIZATION_META_KEY);
    if (storeToHarmonizer == null) {
      return null;
    }

    return storeToHarmonizer.get(name);
  }
}
