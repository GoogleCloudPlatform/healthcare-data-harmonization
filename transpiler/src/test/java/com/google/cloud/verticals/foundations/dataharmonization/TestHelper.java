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

package com.google.cloud.verticals.foundations.dataharmonization;

import static com.google.cloud.verticals.foundations.dataharmonization.TranspilerData.SYMBOLS_META_KEY;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SymbolReference;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Symbols;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.symbols.SymbolHelper;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Any;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.Message;
import java.util.List;
import java.util.stream.Collectors;

/** Helper functions for tests. */
public final class TestHelper {

  // message.toBuilder() will return a T Builder, which will then return a T when build() is called.
  @SuppressWarnings("unchecked")
  public static <T extends Message> T clearMeta(T message) {
    FieldDescriptor metaDesc =
        message.getAllFields().keySet().stream()
            .filter(f -> f.getJavaType().equals(JavaType.MESSAGE))
            .filter(f -> f.getMessageType().equals(Meta.getDescriptor()))
            .findFirst()
            .orElse(null);

    Message.Builder builder = message.toBuilder();
    if (metaDesc != null) {
      builder.clearField(metaDesc);
    }

    message.getAllFields().forEach((f, v) -> clearField(builder, f, v));

    return (T) builder.build();
  }

  private static void clearField(Message.Builder builder, FieldDescriptor f, Object v) {
    if ((v instanceof Message) && !f.getMessageType().equals(Meta.getDescriptor())) {
      Message newV = clearMeta((Message) v);
      builder.setField(f, newV);
    } else if (f.isRepeated()) {
      List<?> vList =
          ((List<?>) v)
              .stream()
                  .map(i -> i instanceof Message ? clearMeta((Message) i) : i)
                  .collect(Collectors.toList());
      for (int i = 0; i < vList.size(); i++) {
        builder.setRepeatedField(f, i, vList.get(i));
      }
    }
  }

  public static Meta sourceMeta(
      int startLine, int startCol, int endLine, int endCol, FunctionType type) {
    Meta.Builder builder =
        Meta.newBuilder()
            .putEntries(
                TranspilerData.SOURCE_META_KEY,
                Any.pack(
                    Source.newBuilder()
                        .setStart(
                            SourcePosition.newBuilder().setLine(startLine).setColumn(startCol))
                        .setEnd(SourcePosition.newBuilder().setLine(endLine).setColumn(endCol))
                        .build()));
    if (type != null) {
      builder.putEntries(
          TranspilerData.FUNCTION_INFO_META_KEY,
          Any.pack(FunctionInfo.newBuilder().setType(type).build()));
    }

    return builder.build();
  }

  public static Meta sourceMeta(int startLine, int startCol, int endLine, int endCol) {
    return sourceMeta(startLine, startCol, endLine, endCol, null);
  }

  public static Meta symbolMeta(
      int startLine,
      int startCol,
      int endLine,
      int endCol,
      String name,
      boolean def,
      SymbolReference.Type type) {
    return SymbolHelper.withSymbol(
            Meta.newBuilder(),
            ImmutableList.of(symbolRef(startLine, startCol, endLine, endCol, name, def, type)))
        .build();
  }

  public static Meta symbolMeta(
      int startLine,
      int startCol,
      int endLine,
      int endCol,
      String name,
      String environment,
      boolean def,
      boolean isWrite,
      SymbolReference.Type type) {
    return SymbolHelper.withSymbol(
            Meta.newBuilder(),
            ImmutableList.of(
                symbolRef(
                    startLine, startCol, endLine, endCol, name, environment, def, isWrite, type)))
        .build();
  }

  public static Meta symbolMeta(SymbolReference... symbols) {
    Symbols.Builder builder = Symbols.newBuilder();

    builder.addAllSymbols(ImmutableList.copyOf(symbols));

    return Meta.newBuilder().putEntries(SYMBOLS_META_KEY, Any.pack(builder.build())).build();
  }

  public static SymbolReference symbolRef(
      int startLine,
      int startCol,
      int endLine,
      int endCol,
      String name,
      boolean def,
      SymbolReference.Type type) {
    return SymbolReference.newBuilder()
        .setName(name)
        .setDefinition(def)
        .setPosition(
            Source.newBuilder()
                .setStart(SourcePosition.newBuilder().setLine(startLine).setColumn(startCol))
                .setEnd(SourcePosition.newBuilder().setLine(endLine).setColumn(endCol)))
        .setType(type)
        .build();
  }

  public static SymbolReference symbolRef(
      int startLine,
      int startCol,
      int endLine,
      int endCol,
      String name,
      String environment,
      boolean def,
      boolean isWrite,
      SymbolReference.Type type) {
    return SymbolReference.newBuilder()
        .setName(name)
        .setDefinition(def)
        .setIsWrite(isWrite)
        .setPosition(
            Source.newBuilder()
                .setStart(SourcePosition.newBuilder().setLine(startLine).setColumn(startCol))
                .setEnd(SourcePosition.newBuilder().setLine(endLine).setColumn(endCol)))
        .setEnvironment(environment)
        .setType(type)
        .build();
  }

  public static Meta mergeMeta(Meta... metas) {
    Meta.Builder merged = Meta.newBuilder();
    for (Meta meta : metas) {
      merged.mergeFrom(meta);
    }
    return merged.build();
  }

  public static Meta mergeMeta(List<Meta> metas) {
    Meta.Builder merged = Meta.newBuilder();
    for (Meta meta : metas) {
      merged.mergeFrom(meta);
    }
    return merged.build();
  }

  private TestHelper() {}
}
