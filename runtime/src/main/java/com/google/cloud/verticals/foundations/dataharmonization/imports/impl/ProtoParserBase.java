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

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRuntimeContext.DefaultImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame.DefaultBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.InitializationContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;

/**
 * Base class to provide utilities to register all functions defined in a {@link PipelineConfig}.
 */
public abstract class ProtoParserBase implements Parser {

  public static void registerFunctions(
      Registries registries, PipelineConfig config, ImportPath configPath) {
    PackageContext pkg =
        new PackageContext(
            ImmutableSet.of(config.getPackageName()), config.getPackageName(), configPath);
    config.getFunctionsList().stream()
        .map(p -> new WhistleFunction(p, config, pkg))
        .forEach(
            fn ->
                registries
                    .getFunctionRegistry(config.getPackageName())
                    .register(config.getPackageName(), fn));
  }

  @Override
  public void parse(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath)
      throws IOException {
    parseConfig(data, registries, metaData, processor, iPath);
  }

  /**
   * Parses the given data, register it as appropriate into the given registries and returns a
   * {@link PipelineConfig} representation of the given data. If the given data represents something
   * that can import other things (like a Whistle file that can import other Whistle files or
   * plugins), then it should do so using the given ImportProcessor.
   */
  public PipelineConfig parseConfig(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath)
      throws IOException {
    PipelineConfig config = parseProto(data, iPath);
    RuntimeContext context =
        new InitializationContext(
            new PackageContext(
                ImmutableSet.of(config.getPackageName()), config.getPackageName(), iPath),
            registries,
            processor,
            new DefaultImplementation(),
            new DefaultBuilder(),
            metaData);
    processor.processImports(iPath, context, config);
    Option.runEngineInitTimeOptions(context, config);
    registerFunctions(registries, config, iPath);
    return config;
  }
  public abstract PipelineConfig parseProto(byte[] data, ImportPath iPath);
}
