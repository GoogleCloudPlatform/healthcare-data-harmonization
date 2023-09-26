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
import com.google.protobuf.InvalidProtocolBufferException;

/** Deserializes a {@link PipelineConfig}'s bytes. */
public class ProtoParser extends ProtoParserBase {
  public static final String NAME = "proto";
  private static final String PROTO_EXTENSION = ".pb";

  @Override
  public PipelineConfig parseProto(byte[] data, ImportPath path) {
    try {
      return PipelineConfig.parseFrom(data);
    } catch (InvalidProtocolBufferException e) {
      throw new IllegalArgumentException(
          "Invalid data format. Was expecting a binary serialized Whistle proto.", e);
    }
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public boolean canParse(ImportPath path) {
    return path.getAbsPath().toString().toLowerCase().endsWith(PROTO_EXTENSION);
  }
}
