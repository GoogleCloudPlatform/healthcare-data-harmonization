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

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.FileSystems;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for import path. */
@RunWith(JUnit4.class)
public class ImportPathTest {

  @Test
  public void resolve_absolutePathWithLoaderAndLeadingSlash_setsLoaderAndPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "newloader:///new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/new/path/y.file"), actual.getAbsPath());
    assertEquals("newloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_absolutePathWithLoader_setsLoaderAndPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "newloader://new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/new/path/y.file"), actual.getAbsPath());
    assertEquals("newloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_absolutePathWithSameLoader_setsLoaderAndPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "baseloader://new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/new/path/y.file"), actual.getAbsPath());
    assertEquals("baseloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_absolutePath_setsPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "/new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/new/path/y.file"), actual.getAbsPath());
    assertEquals("baseloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_rootRelativePath_setsPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/base/new/path/y.file"), actual.getAbsPath());
    assertEquals("baseloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_rootRelativeName_setsPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(FileSystems.getDefault().getPath("/base/y.file"), actual.getAbsPath());
    assertEquals("baseloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void resolve_fileRelativePath_setsPath() {
    ImportPath current =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));

    String next = "./new/path/y.file";
    ImportPath actual = ImportPath.resolve(current, next);
    assertEquals(
        FileSystems.getDefault().getPath("/base/other/new/path/y.file"), actual.getAbsPath());
    assertEquals("baseloader", actual.getLoader());
    assertEquals(FileSystems.getDefault().getPath("/base/"), actual.getImportsRoot());
  }

  @Test
  public void serialize_importPath_fullyPopulated() {
    ImportPath expected =
        ImportPath.of(
            "baseloader",
            FileSystems.getDefault().getPath("/base/other/x.file"),
            FileSystems.getDefault().getPath("/base/"));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(expected);
      objectOutputStream.flush();
      ObjectInputStream ois =
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

      ImportPath actual = (ImportPath) ois.readObject();
      byteArrayOutputStream.close();
      ois.close();
      Assert.assertEquals(expected, actual);
    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
    }
  }

  @Test
  public void serialize_importPath_nullObject() {
    ImportPath expected = null;
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(expected);
      objectOutputStream.flush();
      ObjectInputStream ois =
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

      ImportPath actual = (ImportPath) ois.readObject();
      byteArrayOutputStream.close();
      ois.close();
      Assert.assertEquals(expected, actual);
    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
    }
  }

  @Test
  public void serialize_importPath_oneNullField() {
    ImportPath expected =
        ImportPath.of("baseloader", null, FileSystems.getDefault().getPath("/base/"));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(expected);
      objectOutputStream.flush();
      ObjectInputStream ois =
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

      ImportPath actual = (ImportPath) ois.readObject();
      byteArrayOutputStream.close();
      ois.close();
      Assert.assertEquals(expected, actual);
    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
    }
  }

  @Test
  public void serialize_importPath_twoNullFields() {
    ImportPath expected = ImportPath.of(null, null, FileSystems.getDefault().getPath("/base/"));
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    try {
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
      objectOutputStream.writeObject(expected);
      objectOutputStream.flush();
      ObjectInputStream ois =
          new ObjectInputStream(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()));

      ImportPath actual =
          ImportPath.of(
              (String) ois.readObject(),
              FileSystems.getDefault().getPath(ois.readObject().toString()),
              FileSystems.getDefault().getPath(ois.readObject().toString()));
      byteArrayOutputStream.close();
      ois.close();
      Assert.assertEquals(expected, actual);
    } catch (Exception e) {
      System.out.println("Exception Caught: " + e.getClass().getCanonicalName());
    }
  }
}
