/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.imports;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import java.io.IOException;

/**
 * The Parser is responsible for processing the content of an imported resource in a meaningful way,
 * after this content has been read by a Loader.
 *
 * <p>This example parser handles any resources whose imported URL ends in ".example". It then takes
 * the content of the resource (whatever that may be, depends on the {@link Loader} that loaded it),
 * and sets it to the runtime's MetaData under the example key, perhaps for use by some plugin
 * function later.
 *
 * <p>Note that loaders and parsers may or may not be coupled. If a loader is intended to be coupled
 * with a parser, then the parser should check the scheme in {@link Parser#canParse}, rather than
 * other parts of the URL.
 */
public class ExampleParser implements Parser {

  @Override
  public void parse(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath)
      throws IOException {
    // This example parser does not do anything particularly interesting - it simply puts the
    // content as a string into the MetaData, perhaps for some other function in the plugin to use
    // later.
    // Note that this is a silly behaviour, because if multiple .example resources are imported,
    // they will overwrite each other!
    metaData.setMeta("example", new String(data, UTF_8));
  }

  @Override
  public boolean canParse(ImportPath path) {
    // Here we put the logic indicating that an imported resource should be handled by this parser.
    // For this example, this parser will handle any resource with a .example extension.
    // Note that if no parser matches a resource, or many do, an error is raised. Be very particular
    // about which paths you do or do not claim to parse.
    return path.getAbsPath().toString().endsWith(".example");
  }

  @Override
  public String getName() {
    // This is only used for registration, and not for matching.
    return "example";
  }
}
