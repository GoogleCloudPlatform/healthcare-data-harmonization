/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.lsp;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.tools.linter.FileSystemShim;
import com.google.common.io.ByteStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

/** Used in integration tests for reading Whistle files and writing to test output. */
public class TestFileSystemShim implements FileSystemShim {
  public StringWriter testOutput = new StringWriter();

  /**
   * @param filePath {@link String} the path of the output file
   * @return {@link StringWriter} the test output, containing the path of the file, followed by the
   *     content of the file
   */
  @Override
  public Writer createWriter(String filePath) throws IOException {
    testOutput.write(filePath);
    testOutput.write('\n');
    testOutput.write("");
    return testOutput;
  }

  /**
   * @param filePath {@link String} the path of the resource file to read
   * @return {@link String} the content of the specified file
   */
  @Override
  public String readString(String filePath) throws IOException {
    InputStream stream = TestFileSystemShim.class.getResourceAsStream(filePath);
    if (stream == null) {
      throw new FileNotFoundException(String.format("%s was not found.", filePath));
    }
    return new String(ByteStreams.toByteArray(stream), UTF_8);
  }
}
