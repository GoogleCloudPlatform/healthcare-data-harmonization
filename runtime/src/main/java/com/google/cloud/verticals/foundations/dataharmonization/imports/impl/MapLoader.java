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

package com.google.cloud.verticals.foundations.dataharmonization.imports.impl;

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.common.collect.ImmutableMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/** Loads data from a map <path, file content byte> */
public class MapLoader implements Loader {
  public static final String NAME = "vfs";
  private final ImmutableMap<String, byte[]> map;

  public MapLoader(Map<String, byte[]> map) {
    this.map = ImmutableMap.copyOf(map);
  }

  @Override
  public byte[] load(ImportPath path) throws IOException {
    if (!this.map.containsKey(path.toString())) {
      throw new FileNotFoundException(String.format("%s was not found", path));
    }
    return this.map.get(path.toString());
  }

  @Override
  public String getName() {
    return NAME;
  }
}
