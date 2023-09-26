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

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.protobuf.TextFormat;
import com.google.protobuf.TextFormat.ParseException;

/** Parses a {@link PipelineConfig} out of a textproto (encoded string) representation. */
public class TextprotoParser extends ProtoParserBase {
  public static final String NAME = "textproto";
  private static final String TEXTPROTO_EXTENSION = ".textproto";

  @Override
  public PipelineConfig parseProto(byte[] data, ImportPath path) {
    try {
      String textProto = new String(data);
      return TextFormat.parse(textProto, PipelineConfig.class);
    } catch (ParseException e) {
      throw new IllegalArgumentException(
          "Invalid textproto format. Was expecting a text serialized Whistle proto.", e);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean canParse(ImportPath path) {
    return path.getAbsPath().toString().toLowerCase().endsWith(TEXTPROTO_EXTENSION);
  }
}
