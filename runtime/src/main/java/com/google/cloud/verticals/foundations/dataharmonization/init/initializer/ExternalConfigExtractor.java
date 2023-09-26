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

import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.URIParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/** Initializes Whistle config that exists as a file externally. */
public class ExternalConfigExtractor extends ConfigExtractorBase {

  ExternalConfigExtractor(ImportPath mainPath) {
    super(mainPath);
  }

  public static ExternalConfigExtractor of(ImportPath importPath) {
    return new ExternalConfigExtractor(importPath);
  }

  public static ExternalConfigExtractor of(URI whistleConfig, URI importsRoot) {
    String configLoader = URIParser.getSchema(whistleConfig);
    String importsLoader = URIParser.getSchema(importsRoot);
    Path whistleConfigPath = URIParser.getPath(whistleConfig);
    Path importsRootPath = URIParser.getPath(importsRoot);

    // TODO() Supports different schemes in config and imports root.
    if (!importsRootPath.toString().isEmpty() && !configLoader.equals(importsLoader)) {
      throw new IllegalArgumentException(
          String.format(
              "Whistle config scheme (%s) is different from imports root scheme (%s).",
              configLoader, importsLoader));
    }
    if (importsRootPath.toString().isEmpty()) {
      // Default imports root to the folder that contains the whistle main config.
      importsRootPath = whistleConfigPath.getParent();
    }
    ImportPath ipath = ImportPath.of(configLoader, whistleConfigPath, importsRootPath);
    return ExternalConfigExtractor.of(ipath);
  }

  @Override
  byte[] getFileContent(Registries registries) throws IOException {
    Loader loader = registries.getLoaderRegistry().get(getImportPath().getLoader());
    if (loader == null) {
      throw new IllegalArgumentException(
          String.format("Cannot find loader %s.", getImportPath().getLoader()));
    }
    return loader.load(getImportPath());
  }

  @Override
  ProtoParserBase getParser(Registries registries, ImportProcessor importProcessor) {
    Parser parser = ImportProcessor.matchParser(getImportPath(), registries.getParserRegistry());
    if (!(parser instanceof ProtoParserBase)) {
      throw new IllegalArgumentException(
          String.format(
              "The main file can only be parsed by %s, however it must be a proto or equivalent"
                  + " (Whistle, Textproto, etc). Please make sure that the main file can be parsed"
                  + " into a PipelineConfig proto.",
              parser.getName()));
    }
    return (ProtoParserBase) parser;
  }
}
