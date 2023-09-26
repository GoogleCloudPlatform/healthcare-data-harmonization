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

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.Transpiler;
import com.google.cloud.verticals.foundations.dataharmonization.error.TranspilationException;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import java.nio.file.FileSystems;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for WhistleParser. */
@RunWith(JUnit4.class)
public class WhistleParserTest {

  @Test
  public void getName() {
    assertEquals(WhistleParser.NAME, new WhistleParser().getName());
  }

  @Test
  public void transpile_callsTranspiler() {
    ImportPath path =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    PipelineConfig expected = new Transpiler().transpile("package foo\n", path.toFileInfo());
    assertEquals(expected, new WhistleParser().parseProto("package foo\n".getBytes(UTF_8), path));
  }

  @Test
  public void transpile_callsTranspiler_ignoreExceptionFlag() {
    ImportPath path =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    PipelineConfig expected =
        new Transpiler(false).transpile("Dont throw exception\n", path.toFileInfo());
    assertEquals(
        expected,
        new WhistleParser(false).parseProto("Dont throw exception\n".getBytes(UTF_8), path));
  }

  @Test
  public void transpile_callsTranspiler_throwsException() {
    ImportPath path =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));

    TranspilationException transpilationException =
        assertThrows(
            TranspilationException.class,
            () -> new WhistleParser().parseProto("Throw exception\n".getBytes(UTF_8), path));
    assertThat(transpilationException)
        .hasMessageThat()
        .isEqualTo(
            "Errors occurred during transpilation:\n"
                + "test://test\n"
                + "1:1-1:16 - unexpected input at Throw exception");
  }
}
