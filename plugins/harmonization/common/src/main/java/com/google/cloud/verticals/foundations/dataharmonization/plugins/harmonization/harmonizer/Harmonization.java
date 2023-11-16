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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.HarmonizationParserBase;
import java.util.List;
import java.util.regex.Pattern;

/** Class to handle harmonization, utilizes an instance of LocalHarmonizer in the context. */
public final class Harmonization {
  public static final String HARMONIZATION_SOURCE_LOCAL = "$Local";

  private static final String CODE = "code";
  private static final String DISPLAY = "display";
  private static final String SYSTEM = "system";
  private static final String VERSION = "version";

  /**
   * Maps the given (sourceCode, sourceSystem) according to the ConceptMap identified by
   * conceptmapID.
   *
   * @param ctx - RuntimeContext containing the {@link LocalHarmonizer}
   * @param harmonizationSource - "$Local" for all conceptmaps loaded from Json, or the name of
   *     remote FHIR store (only supported in some environments).
   * @param sourceCode - the sourceCode to be harmonized.
   * @param sourceSystem - the sourceSystem to find which Group in the ConceptMap should be used.
   * @param conceptmapId - the id of ConceptMap used for harmonization.
   * @return {@code Array} representing FHIR [Coding](https://build.fhir.org/datatypes.html#Coding)
   *     datatypes.
   */
  @PluginFunction
  public static Data harmonize(
      RuntimeContext ctx,
      String harmonizationSource,
      String sourceCode,
      String sourceSystem,
      String conceptmapId)
      throws IllegalArgumentException {
    if (harmonizationSource.isBlank()) {
      throw new IllegalArgumentException("The harmonization source cannot be empty");
    }

    Harmonizer harmonizer = getHarmonizer(ctx, harmonizationSource);
    List<HarmonizedCode> result = harmonizer.harmonize(sourceCode, sourceSystem, conceptmapId);
    return convertToArray(ctx, result);
  }

  // TODO(): Remove this function.
  private static Harmonizer getHarmonizer(RuntimeContext ctx, String harmonizationSource) {
    Harmonizer harmonizer =
        HarmonizationParserBase.getHarmonizer(ctx.getMetaData(), harmonizationSource);
    if (harmonizer == null) {
        throw new IllegalArgumentException(
            "No ConceptMap has been imported. You probably need to import some"
                + " `*.harmonization.json` in your config file.");
    }

    return harmonizer;
  }

  /**
   * Maps the given (sourceCode, sourceSystem, targetSystem) according to the ConceptMap identified
   * by conceptmapID.
   *
   * @param ctx - RuntimeContext containing the {@link LocalHarmonizer}
   * @param harmonizationSource - "$Local" for all conceptmaps loaded from Json. Can also be the
   *     name of remote FHIR store in certain environments.
   * @param sourceCode - the sourceCode to be harmonized.
   * @param sourceSystem - the sourceSystem to find which Group in the ConceptMap should be used.
   * @param targetSystem - the targetSystem to find which Group in the ConceptMap should be used.
   * @param conceptmapId - the id of ConceptMap used for harmonization.
   * @return {@code Array} representing FHIR [Coding](https://build.fhir.org/datatypes.html#Coding)
   *     datatypes.
   */
  @PluginFunction
  public static Data harmonizeWithTarget(
      RuntimeContext ctx,
      String harmonizationSource,
      String sourceCode,
      String sourceSystem,
      String targetSystem,
      String conceptmapId) {
    if (harmonizationSource.isBlank()) {
      throw new IllegalArgumentException("The harmonization source cannot be empty");
    }
    Harmonizer harmonizer = getHarmonizer(ctx, harmonizationSource);
    List<HarmonizedCode> result =
        harmonizer.harmonizeWithTarget(sourceCode, sourceSystem, targetSystem, conceptmapId);
    return convertToArray(ctx, result);
  }

  private static Array convertToArray(RuntimeContext ctx, List<HarmonizedCode> harmonizedCodes) {
    Array result = ctx.getDataTypeImplementation().emptyArray();
    for (int i = 0; i < harmonizedCodes.size(); i++) {
      result = result.setElement(i, convertToContainer(ctx, harmonizedCodes.get(i)));
    }
    return result;
  }

  private static Container convertToContainer(RuntimeContext ctx, HarmonizedCode harmonizedCode) {
    Container result = ctx.getDataTypeImplementation().emptyContainer();
    result =
        result.setField(CODE, ctx.getDataTypeImplementation().primitiveOf(harmonizedCode.code()));
    result =
        result.setField(
            DISPLAY, ctx.getDataTypeImplementation().primitiveOf(harmonizedCode.display()));
    result =
        result.setField(
            SYSTEM, ctx.getDataTypeImplementation().primitiveOf(harmonizedCode.system()));
    result =
        result.setField(
            VERSION, ctx.getDataTypeImplementation().primitiveOf(harmonizedCode.version()));
    return result;
  }
}
