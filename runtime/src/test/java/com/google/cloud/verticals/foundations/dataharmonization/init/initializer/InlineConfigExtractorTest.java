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

package com.google.cloud.verticals.foundations.dataharmonization.init.initializer;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRegistries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.WhistleParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class InlineConfigExtractorTest {

  private static final String MOCK_FILE_CONTENT = "def hello(name) {\n\thello: name\n}";
  private static final boolean THROW_TRANSPILATION_EXCEPTION = true;

  @Test
  public void getFileContent_emptyFile() {
    InlineConfigExtractor inlineExtractor =
        InlineConfigExtractor.of("", /* importPath= */ null, THROW_TRANSPILATION_EXCEPTION);
    byte[] actual = inlineExtractor.getFileContent(new DefaultRegistries());
    assertArrayEquals(new byte[0], actual);
  }

  @Test
  public void getFileContent_returnsContents() {
    InlineConfigExtractor inlineExtractor =
        InlineConfigExtractor.of(
            MOCK_FILE_CONTENT, /* importPath= */ null, THROW_TRANSPILATION_EXCEPTION);
    byte[] actual = inlineExtractor.getFileContent(new DefaultRegistries());
    assertArrayEquals(MOCK_FILE_CONTENT.getBytes(UTF_8), actual);
  }

  @Test
  public void getParser_returnsWhistleParser() {
    InlineConfigExtractor inlineExtractor =
        InlineConfigExtractor.of(
            MOCK_FILE_CONTENT, /* importPath= */ null, THROW_TRANSPILATION_EXCEPTION);
    ProtoParserBase actual =
        inlineExtractor.getParser(new DefaultRegistries(), new DefaultImportProcessor());
    assertEquals(WhistleParser.NAME, actual.getName());
  }
}
