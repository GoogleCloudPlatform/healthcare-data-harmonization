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

package com.google.cloud.verticals.foundations.dataharmonization.imports.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SOURCE_META_KEY;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.InitializationContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.protobuf.Any;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Default implementation of an {@link ImportProcessor}. Uses a {@link PipelineConfig#hashCode()} to
 * detect cycles, and simply skips cyclical imports.
 */
public class DefaultImportProcessor implements ImportProcessor {
  private final transient Set<Path> seenSet;
  private final transient Set<ImportPath> importPaths;
  public static final String IMPORT_EXCEPTION_LIST_KEY = "importExceptionListKey";

  public DefaultImportProcessor(Path initialPath) {
    this();
    seenSet.add(initialPath);
  }

  public DefaultImportProcessor() {
    this.seenSet = new HashSet<>();
    this.importPaths = new HashSet<>();
  }

  @Override
  public void processImports(ImportPath currentPath, RuntimeContext context, PipelineConfig config)
      throws IOException {
    processImports(
        currentPath, context, config, context.getMetaData().getMeta(IMPORT_EXCEPTION_LIST_KEY));
  }

  /**
   * ProcessImports overload which supports the language server in publishing {@link
   * ImportException}s to the language server.
   *
   * <p>Based on whether a list to store the relevant exceptions is present, this implementation
   * will either add any encountered exceptions to the appropriate list and not throw the exception,
   * or if no list is provided, throw the Exception.
   */
  void processImports(
      ImportPath currentPath,
      RuntimeContext context,
      PipelineConfig config,
      List<ImportException> importExceptions)
      throws IOException {
    if (!(context instanceof InitializationContext)) {
      throw new IllegalStateException(
          String.format(
              "Expect an instance of InitializationContext but got %s.",
              context.getClass().getName()));
    }
    InitializationContext initializationContext = (InitializationContext) context;

    seenSet.add(currentPath.getAbsPath());
    for (Import i : config.getImportsList()) {

      String path = initializationContext.evaluateImport(i, config).asPrimitive().string();
      ImportPath iPath = ImportPath.resolve(currentPath, path);
      // Avoid processing any imports seen before.
      if (!seenSet.add(iPath.getAbsPath())) {
        continue;
      }

      Registries registries = context.getRegistries();
      Loader loader = registries.getLoaderRegistry().get(iPath.getLoader());
      if (loader == null) {
        throw new IllegalArgumentException(
            String.format(
                "Loader %s for import %s not found. Do you need to import a plugin that contains"
                    + " it?",
                iPath.getLoader(), path));
      }

      try {
        byte[] loadBytes = loader.load(iPath);
        Parser parser = ImportProcessor.matchParser(iPath, registries.getParserRegistry());
        parser.parse(loadBytes, registries, context.getMetaData(), this, iPath);
        importPaths.add(iPath);
      } catch (RuntimeException | IOException e) {
        if (importExceptions == null) {
          // Case for when we are not storing ImportExceptions.
          throw new ImportException(iPath, e);
        }
        // Save any ImportExceptions to the provided list.
        Map<String, Any> metaMap = i.getMeta().getEntriesMap();
        Source source =
            metaMap.containsKey(SOURCE_META_KEY)
                ? metaMap.get(SOURCE_META_KEY).unpack(Source.class)
                : Source.getDefaultInstance();
        importExceptions.add(new ImportException(iPath, e, source, currentPath.toString()));
      }
    }
  }

  // TODO(): Make importPaths part of the ImportProcessor interface
  public Set<ImportPath> getImportPaths() {
    return importPaths;
  }
}
