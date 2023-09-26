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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.URIParser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.WhistleParser;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/** Initializes Whistle Config in the form of an inline string. */
public class InlineConfigExtractor extends ConfigExtractorBase {
  private final String inlineWhistleConfig;
  private final Boolean throwTranspilationException;

  InlineConfigExtractor(
      ImportPath mainPath, String inlineWhistleConfig, Boolean throwTranspilationException) {
    super(mainPath);
    this.inlineWhistleConfig = inlineWhistleConfig;
    this.throwTranspilationException = throwTranspilationException;
  }

  public static InlineConfigExtractor of(
      String inlineWhistleConfig, ImportPath importPath, Boolean throwTranspilationException) {
    return new InlineConfigExtractor(importPath, inlineWhistleConfig, throwTranspilationException);
  }

  public static InlineConfigExtractor of(String inlineWhistleConfig, URI importsRoot) {
    String loader = URIParser.getSchema(importsRoot);
    Path importsRootPath = URIParser.getPath(importsRoot);

    ImportPath iPath =
        ImportPath.of(
            loader, FileSystems.getDefault().getPath("/inline-config.wstl"), importsRootPath);
    return new InlineConfigExtractor(iPath, inlineWhistleConfig, true);
  }

  /**
   * Returns the inline config content in byte[].
   *
   * @param unusedRegistries since the config is provided as {@link String} directly so no loader
   *     from {@link Registries} is needed.
   */
  @Override
  byte[] getFileContent(Registries unusedRegistries) {
    return inlineWhistleConfig.getBytes(UTF_8);
  }

  /**
   * Returns {@link WhistleParser} as the default parser for parsing inline config. Therefore,
   * {@link Registries} and {@link ImportProcessor} is not used in this case.
   */
  @Override
  ProtoParserBase getParser(Registries unusedRegistries, ImportProcessor unusedImportProcessor) {
    return new WhistleParser(throwTranspilationException);
  }
}
