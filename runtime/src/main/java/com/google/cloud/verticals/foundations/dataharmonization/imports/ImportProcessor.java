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

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import java.io.IOException;
import java.io.Serializable;

/**
 * An import processor updates context registries with the functions defined in the given {@link
 * PipelineConfig} proto.
 */
public interface ImportProcessor extends Serializable {

  /**
   * Finds and returns the parser that can parse the given {@link ImportPath} from the provided
   * parser registry. This method will throw {@link IllegalArgumentException} when there's no or
   * more than one matching parser in the registry.
   *
   * @param iPath the ImportPath to find parser for.
   * @param parsers the collection of parsers to search against.
   * @return the matched parser for iPath.
   */
  static Parser matchParser(ImportPath iPath, Registry<Parser> parsers) {
    Parser[] matchingParsers =
        parsers.getAll().stream().filter(p -> p.canParse(iPath)).toArray(Parser[]::new);
    if (matchingParsers.length == 0) {
      throw new IllegalArgumentException(
          String.format(
              "No supporting parser found for %s. Make sure all needed Plugins are imported.",
              iPath.getAbsPath()));
    }
    if (matchingParsers.length > 1) {
      throw new IllegalArgumentException(
          String.format(
              "Multiple supporting parsers found for %s. This may be due to multiple plugins"
                  + " supplying conflicting parsers. Either change the order in which you import"
                  + " your plugins, or split up your config file, with each file using one of the"
                  + " conflicting plugins.",
              iPath.getAbsPath()));
    }

    return matchingParsers[0];
  }

  /**
   * Update the function registry in the given {@link RuntimeContext} with the functions in the
   * given {@link PipelineConfig#getImportsList()}.
   *
   * <p>Should recursively process imports for the {@link PipelineConfig}. Cycle detection is up to
   * the implementation.
   *
   * @param currentPath the path of the config to process.
   * @param context the runtime context to use and update
   * @param config the config to process.
   * @throws IOException if (recursively) loading/parsing an import throws an exception.
   */
  void processImports(ImportPath currentPath, RuntimeContext context, PipelineConfig config)
      throws IOException;
}
