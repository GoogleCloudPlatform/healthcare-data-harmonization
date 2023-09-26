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

package com.google.cloud.verticals.foundations.dataharmonization.init.initializer;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import java.io.IOException;

/**
 * Base class to provide utilities for parsing a whistle config into {@link PipelineConfig}, resolve
 * all its imports and register its functions using {@link ProtoParserBase#parseConfig(byte[],
 * Registries, MetaData, ImportProcessor, ImportPath)}.
 */
public abstract class ConfigExtractorBase {

  private final ImportPath importPath;

  ConfigExtractorBase(ImportPath importPath) {
    this.importPath = importPath;
  }

  public ImportPath getImportPath() {
    return this.importPath;
  }

  /**
   * Extracts {@link PipelineConfig} from the current instance and register all functions from the
   * extracted {@link PipelineConfig} and its imports.
   *
   * @param registries the {@link Registries} to register all functions, loaders and parsers into.
   * @param metaData the {@link MetaData} to record all user-defined meta data needed for engine
   *     execution.
   * @param importProcessor the {@link ImportProcessor} that handles import resolution logic.
   * @return the extracted PipelineConfig.
   * @throws IOException if there's {@link IOException} when loading the content from the current
   *     instance.
   */
  public final PipelineConfig initialize(
      Registries registries, MetaData metaData, ImportProcessor importProcessor)
      throws IOException {
    return getParser(registries, importProcessor)
        .parseConfig(getFileContent(registries), registries, metaData, importProcessor, importPath);
  }

  /**
   * Returns the configs content in the current instance as byte[].
   *
   * @param registries provides already registered loaders.
   */
  abstract byte[] getFileContent(Registries registries) throws IOException;

  /**
   * Returns the suitable parser for the {@link this#importPath} from those registered in the {@code
   * registries}.
   *
   * @param importProcessor handles parser selection logic.
   */
  abstract ProtoParserBase getParser(Registries registries, ImportProcessor importProcessor);
}
