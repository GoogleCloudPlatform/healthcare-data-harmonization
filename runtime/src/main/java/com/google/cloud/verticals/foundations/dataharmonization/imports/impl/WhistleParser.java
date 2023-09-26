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

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.Transpiler;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.common.base.Ascii;

/** Allows parsing Whistle Files. */
public class WhistleParser extends ProtoParserBase {

  public static final String NAME = "wstl";
  private static final String WSTL_EXTENSION = ".wstl";
  // This flag is used to toggle whether any syntax issues in the file being parsed
  // should throw a TranspilationException when being transpiled by the transpiler.
  // Set to true by default.
  private final Boolean throwTranspilationException;

  public WhistleParser() {
    this(true);
  }

  public WhistleParser(boolean throwTranspilationException) {
    this.throwTranspilationException = throwTranspilationException;
  }

  @Override
  public PipelineConfig parseProto(byte[] data, ImportPath iPath) {
    return new Transpiler(throwTranspilationException)
        .transpile(new String(data, UTF_8), iPath.toFileInfo());
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean canParse(ImportPath path) {
    return Ascii.toLowerCase(path.getAbsPath().toString()).endsWith(WSTL_EXTENSION);
  }
}
