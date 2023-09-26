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

import static org.mockito.ArgumentMatchers.argThat;

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import java.nio.file.FileSystems;

/** Test utilities for ImportPath. */
public final class ImportPathUtil {
  private ImportPathUtil() {}
  /**
   * Creates an import path for the given file. The project root is the parent (directory) of the
   * given file.
   */
  public static ImportPath projectFile(String path) {
    return ImportPath.of(
        MemoryLoader.NAME,
        FileSystems.getDefault().getPath(path),
        FileSystems.getDefault().getPath(path).getParent());
  }

  /** Creates a mockito arg matcher that will match an ImportPath to the given absolute path. */
  public static ImportPath absPath(String path) {
    return argThat(ip -> FileSystems.getDefault().getPath(path).equals(ip.getAbsPath()));
  }
}
