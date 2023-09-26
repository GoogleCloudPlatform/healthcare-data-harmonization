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

import static com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ImportPathUtil.projectFile;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for FileLoader. */
@RunWith(JUnit4.class)
public class FileLoaderTest {

  private static final byte[] dummyFileBytes = new byte[] {1, 2, 3};

  private static File mockFile(boolean exists, boolean directory) {
    File file = mock(File.class);
    when(file.exists()).thenReturn(exists);
    when(file.isDirectory()).thenReturn(directory);
    return file;
  }

  private static FileLoader loaderWithFile(File file) throws IOException {
    FileLoader loader = mock(FileLoader.class);
    when(loader.createFileObj(any())).thenReturn(file);
    when(loader.readFile(any())).thenReturn(dummyFileBytes);
    when(loader.load(any())).thenCallRealMethod();
    return loader;
  }

  @Test
  public void load_nonExistingFile_throws() throws IOException {
    FileLoader loader = loaderWithFile(mockFile(false, false));

    assertThrows(FileNotFoundException.class, () -> loader.load(projectFile("/my-file")));
  }

  @Test
  public void load_directory_throws() throws IOException {
    FileLoader loader = loaderWithFile(mockFile(true, true));

    assertThrows(IllegalArgumentException.class, () -> loader.load(projectFile("/my-dir")));
  }

  @Test
  public void load_validFile_returnsBytes() throws IOException {
    FileLoader loader = loaderWithFile(mockFile(true, false));

    byte[] actual = loader.load(projectFile("/any.file"));
    assertArrayEquals(dummyFileBytes, actual);
  }
}
