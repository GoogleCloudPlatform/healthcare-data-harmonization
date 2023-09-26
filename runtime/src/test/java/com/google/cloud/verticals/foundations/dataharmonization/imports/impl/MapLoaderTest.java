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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.common.collect.ImmutableMap;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public final class MapLoaderTest {

  private static final ImmutableMap<String, byte[]> fakeMap =
      ImmutableMap.of(
          "vfs://some/path/import1.wstl",
          new byte[] {1, 2, 3},
          "vfs://some/path/import2.wstl",
          new byte[] {1, 2, 3});
  private static final MapLoader mapLoader = new MapLoader(fakeMap);

  @Test
  public void load_validFile_returnsBytes() throws IOException {
    ImportPath testWhistleFile =
        ImportPath.of(
            MapLoader.NAME, Paths.get("some/path/import1.wstl"), Paths.get("vfs://some/path/"));
    byte[] actual = mapLoader.load(testWhistleFile);

    assertArrayEquals(new byte[] {1, 2, 3}, actual);
  }

  @Test
  public void load_nonExistingFile_throws() throws IOException {

    ImportPath testWhistleFile =
        ImportPath.of(
            MapLoader.NAME,
            Paths.get("some/path/does_not_exist.wstl"),
            Paths.get("vfs://some/path/"));

    assertThrows(FileNotFoundException.class, () -> mapLoader.load(testWhistleFile));
  }
}
