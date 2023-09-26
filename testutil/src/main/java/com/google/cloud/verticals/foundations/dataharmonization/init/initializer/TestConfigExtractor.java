/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.init.initializer;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * A fake implementation of {@link ConfigExtractorBase} so that {@link Engine} is initialized with
 * only PipelineConfig.
 */
public class TestConfigExtractor extends ConfigExtractorBase {
  PipelineConfig config;
  static ImportPath testPath =
      ImportPath.of("", FileSystems.getDefault().getPath(""), FileSystems.getDefault().getPath(""));

  private TestConfigExtractor(ImportPath ip, PipelineConfig config) {
    super(ip);
    this.config = config;
  }

  public static TestConfigExtractor of(ImportPath ip, PipelineConfig config) {
    return new TestConfigExtractor(ip, config);
  }

  public static TestConfigExtractor of(PipelineConfig config) {
    return new TestConfigExtractor(testPath, config);
  }

  public static TestConfigExtractor of() {
    return new TestConfigExtractor(testPath, PipelineConfig.newBuilder().build());
  }

  @Override
  byte[] getFileContent(Registries registries) throws IOException {
    return new byte[0];
  }

  @Override
  ProtoParserBase getParser(Registries registries, ImportProcessor importProcessor) {
    return new TestProtoParser();
  }

  class TestProtoParser extends ProtoParserBase {

    @Override
    public PipelineConfig parseProto(byte[] data, ImportPath iPath) {
      return config;
    }

    @Override
    public boolean canParse(ImportPath path) {
      return true;
    }

    @Override
    public String getName() {
      return null;
    }
  }
}
