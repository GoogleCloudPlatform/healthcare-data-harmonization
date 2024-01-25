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
package com.google.cloud.verticals.foundations.dataharmonization.tools.linter;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Default class for reading and writing to Whistle files in the linter. */
public class DefaultFileSystemShim implements FileSystemShim {

  /**
   * @param filePath {@link String} the path of the output file
   * @return {@link Writer} writes to the specified output file
   */
  @Override
  public Writer createWriter(String filePath) throws IOException {
    return new FileWriter(filePath, UTF_8);
  }

  /**
   * @param filePath {@link String} the path of the file to read
   * @return {@link String} the content of the specified file
   */
  @Override
  public String readString(String filePath) throws IOException {
    return Files.readString(Path.of(filePath));
  }
}
