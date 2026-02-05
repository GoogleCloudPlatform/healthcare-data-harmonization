/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.debug;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FunctionInfo.FunctionType;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.protobuf.Any;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DebugInfoTest {

  @Test
  public void getMetaEntry_source() {
    Source source =
        Source.newBuilder()
            .setStart(SourcePosition.newBuilder().setColumn(12).setLine(13).build())
            .setEnd(SourcePosition.newBuilder().setLine(55).setColumn(4).build())
            .build();
    Meta meta = Meta.newBuilder().putEntries("source", Any.pack(source)).build();

    Source result =
        DebugInfo.getMetaEntry(meta, "source", Source.class, Source.getDefaultInstance());

    assertThat(result).isEqualTo(source);
  }

  @Test
  public void getMetaEntry_fileInfo() {
    FileInfo fileInfo = FileInfo.newBuilder().setUrl("12345").build();
    Meta meta = Meta.newBuilder().putEntries("fileInfo", Any.pack(fileInfo)).build();

    FileInfo result =
        DebugInfo.getMetaEntry(meta, "fileInfo", FileInfo.class, FileInfo.getDefaultInstance());

    assertThat(result).isEqualTo(fileInfo);
  }

  @Test
  public void getMetaEntry_functionInfo() {
    FunctionInfo functionInfo = FunctionInfo.newBuilder().setType(FunctionType.BLOCK).build();
    Meta meta = Meta.newBuilder().putEntries("functionInfo", Any.pack(functionInfo)).build();

    FunctionInfo result =
        DebugInfo.getMetaEntry(
            meta, "functionInfo", FunctionInfo.class, FunctionInfo.getDefaultInstance());

    assertThat(result).isEqualTo(functionInfo);
  }

  @Test
  public void getMetaEntry_notRegisteredEntryType() {
    FunctionInfo functionInfo = FunctionInfo.newBuilder().setType(FunctionType.BLOCK).build();
    Meta meta = Meta.newBuilder().putEntries("functionInfo", Any.pack(functionInfo)).build();

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                DebugInfo.getMetaEntry(
                    meta, "functionInfo", FunctionCall.class, FunctionCall.getDefaultInstance()));

    assertThat(exception)
        .hasMessageThat()
        .contains("is not a valid Meta entry type");
  }
}
