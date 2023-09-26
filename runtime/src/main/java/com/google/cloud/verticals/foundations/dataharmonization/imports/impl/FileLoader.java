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
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.common.io.ByteStreams;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;

/** Loads data from a file on the local file system. */
public class FileLoader implements Loader {
  public static final String NAME = "file";

  @Override
  public byte[] load(ImportPath path) throws IOException {
    File file = createFileObj(path.getAbsPath());
    if (!file.exists()) {
      throw new FileNotFoundException(String.format("%s was not found.", file.getAbsolutePath()));
    }
    if (file.isDirectory()) {
      throw new IllegalArgumentException(
          String.format("%s is a directory. It must be a file.", file.getAbsolutePath()));
    }
    return readFile(file);
  }

  protected File createFileObj(Path path) {
    return path.toFile();
  }

  protected byte[] readFile(File file) throws IOException {
    return ByteStreams.toByteArray(new FileInputStream(file));
  }

  @Override
  public String getName() {
    return NAME;
  }
}
