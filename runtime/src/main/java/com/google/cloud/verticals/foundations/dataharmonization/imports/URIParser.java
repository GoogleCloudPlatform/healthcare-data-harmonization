/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.FileLoader;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/** Helper class to facilitate resource URI parsing. */
public final class URIParser {
  private static final String FILE_SCHEME = FileLoader.NAME;

  /**
   * Extract the schema from the resource {@link URI}. If no schema is found in the resource URI,
   * defaults to `file`.
   *
   * @param uri {@link URI} the resource URI.
   * @return {@link String} the schema.
   */
  public static String getSchema(URI uri) {
    return (uri.getScheme() == null) ? FILE_SCHEME : uri.getScheme();
  }

  /**
   * Extract the file path from the resource {@link URI}.
   *
   * @param uri {@link URI} the resource URI.
   * @return {@link Path} the file path within the given file system.
   */
  public static Path getPath(URI uri) {
    if (getSchema(uri).equals(FILE_SCHEME)) {
      // file URI is in the format of file:///path/to/file or /path/to/file, we want the result path
      // to be /path/to/file.
      return FileSystems.getDefault().getPath(uri.getSchemeSpecificPart());
    }
    // Other types of URI (e.g. gcs) is in the format of gs://bucket/path/to/object, we want the
    // result path to be /bucket/path/to/object.
    return FileSystems.getDefault().getPath("/", uri.getSchemeSpecificPart());
  }

  private URIParser() {}
}
