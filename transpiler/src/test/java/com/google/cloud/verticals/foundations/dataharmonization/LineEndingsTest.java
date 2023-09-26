/*
 * Copyright 2023 Google LLC.
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

import static com.google.cloud.verticals.foundations.dataharmonization.TestHelper.clearMeta;
import static com.google.common.truth.Truth.assertThat;

import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.FileInfo;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class LineEndingsTest {

  @Test
  public void crlf_parses() {
    String content =
        "package test\r\n"
            + "\r\n"
            + "import \"../something.wstl\"\r\n"
            + "\r\n"
            + "\r\n"
            + "// Some text here\r\n"
            + "// Comment\r\n"
            + "def Function() \"hello\"\r\n";

    PipelineConfig result = new Transpiler().transpile(content, FileInfo.getDefaultInstance());

    assertThat(clearMeta(result))
        .isEqualTo(
            PipelineConfig.newBuilder()
                .setPackageName("test")
                .addImports(
                    Import.newBuilder()
                        .setValue(ValueSource.newBuilder().setConstString("../something.wstl")))
                .setRootBlock(
                    FunctionDefinition.newBuilder()
                        .setName("test_root_function")
                        .addArgs(Argument.newBuilder().setName("$root")))
                .addFunctions(
                    FunctionDefinition.newBuilder()
                        .setName("Function")
                        .addMapping(
                            FieldMapping.newBuilder()
                                .setValue(ValueSource.newBuilder().setConstString("hello"))))
                .build());
  }
}
