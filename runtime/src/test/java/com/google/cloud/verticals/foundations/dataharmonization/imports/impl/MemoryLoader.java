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
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/** Loads data from a singleton mapping of URL -> Memory. Useful for testing. */
public class MemoryLoader implements Loader {
  public static final String NAME = "memory";
  private final Map<String, byte[]> entries;

  public MemoryLoader() {
    entries = new HashMap<>();
  }

  public void registerFile(String url, byte[] bytes) {
    entries.put(url, bytes);
  }

  @Override
  public byte[] load(ImportPath path) throws IOException {
    String absPath = path.getAbsPath().toString();
    if (!entries.containsKey(absPath)) {
      throw new IOException(String.format("Unknown entry %s", absPath));
    }
    return entries.get(absPath);
  }

  @Override
  public String getName() {
    return NAME;
  }
}
