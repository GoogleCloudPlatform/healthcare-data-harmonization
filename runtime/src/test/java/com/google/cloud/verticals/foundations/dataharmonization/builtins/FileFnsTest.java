/*
 * Copyright 2022 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.jimfs.Jimfs;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for file functions. */
@RunWith(JUnit4.class)
public class FileFnsTest {
  private RuntimeContext context;
  private FileSystem testFs;

  @Before
  public void setup() throws IOException {
    context = RuntimeContextUtil.testContext();
    Plugin.load(new TestLoaderPlugin(), context.getRegistries(), context.getMetaData());
  }

  @Test
  public void test_loadJson_existingFile() {
    Data code = FileFns.loadJson(context, "res://tests/data.json");

    assertDCAPEquals(arrayOf(testDTI().primitiveOf("hello, I am data.")), code);
  }

  @Test
  public void test_loadJson_missingFile() {
    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> FileFns.loadJson(context, "res://tests/missing.json"));
    assertThat(ex).hasMessageThat().contains("not found");
  }

  @Test
  public void test_loadJson_nonJsonFile() {
    RuntimeException ex =
        assertThrows(
            RuntimeException.class, () -> FileFns.loadJson(context, "res://tests/reflection.wstl"));
    assertThat(ex).hasMessageThat().contains("malformed JSON");
  }

  @Test
  public void test_loadText_existingFile() {
    Data code = FileFns.loadText(context, "res://tests/data.json");

    assertDCAPEquals(testDTI().primitiveOf("[\"hello, I am data.\"]"), code);
  }

  @Test
  public void test_loadText_missingFile() {
    WhistleRuntimeException ex =
        assertThrows(
            WhistleRuntimeException.class,
            () -> FileFns.loadText(context, "res://tests/missing.json"));
    assertThat(ex).hasMessageThat().contains("not found");
  }

  private void setupFs() throws IOException {
    testFs = Jimfs.newFileSystem();
    Files.createDirectories(testFs.getPath("/pwd/one/foo"));
    Files.createDirectories(testFs.getPath("/pwd/one/bar"));
    Files.createDirectories(testFs.getPath("/pwd/two"));
    Files.write(testFs.getPath("/pwd/aaa.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/bbb.zzz.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/foo/ccc.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/foo/ddd.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/foo/eee.zzz.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/bar/fff.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/one/bar/ggg.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/two/hhh.zzz.json"), new byte[1]);
    Files.write(testFs.getPath("/pwd/two/iii.json"), new byte[1]);

    context =
        RuntimeContextUtil.testContext(
            testFs.getPath("/pwd/current.wstl"), testFs.getPath("/pwd/one"));
    FileFns.setFs(testFs);
    ImportPath.setFs(testFs);
  }

  @Test
  public void test_listFiles_single() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "/pwd/*.json");
    Array want = testDTI().arrayOf(testDTI().primitiveOf("/pwd/aaa.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_relative() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "./*.json");
    Array want = testDTI().arrayOf(testDTI().primitiveOf("/pwd/aaa.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_noPrefix() throws IOException, URISyntaxException {
    setupFs();

    // This is relative to import root /pwd/one/
    Array actual = FileFns.listFiles(context, "*.json");
    Array want = testDTI().arrayOf(testDTI().primitiveOf("/pwd/one/bbb.zzz.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_noPrefixDoubleStar() throws IOException, URISyntaxException {
    setupFs();

    // This is relative to import root /pwd/one/
    Array actual = FileFns.listFiles(context, "**/*.json");
    Array want =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf("/pwd/one/bar/fff.json"),
                testDTI().primitiveOf("/pwd/one/bar/ggg.json"),
                testDTI().primitiveOf("/pwd/one/foo/ccc.json"),
                testDTI().primitiveOf("/pwd/one/foo/ddd.json"),
                testDTI().primitiveOf("/pwd/one/foo/eee.zzz.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_glob() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "/pwd/**/*.json");
    Array want =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf("/pwd/one/bar/fff.json"),
                testDTI().primitiveOf("/pwd/one/bar/ggg.json"),
                testDTI().primitiveOf("/pwd/one/bbb.zzz.json"),
                testDTI().primitiveOf("/pwd/one/foo/ccc.json"),
                testDTI().primitiveOf("/pwd/one/foo/ddd.json"),
                testDTI().primitiveOf("/pwd/one/foo/eee.zzz.json"),
                testDTI().primitiveOf("/pwd/two/hhh.zzz.json"),
                testDTI().primitiveOf("/pwd/two/iii.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_globWithChars() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "/**/*.zz?.json");
    Array want =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf("/pwd/one/bbb.zzz.json"),
                testDTI().primitiveOf("/pwd/one/foo/eee.zzz.json"),
                testDTI().primitiveOf("/pwd/two/hhh.zzz.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_fixed() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "/pwd/one/b*.json");
    Array want = testDTI().arrayOf(testDTI().primitiveOf("/pwd/one/bbb.zzz.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_listFiles_noWildcards() throws IOException, URISyntaxException {
    setupFs();

    Array actual = FileFns.listFiles(context, "../pwd/one/bbb.zzz.json");
    Array want = testDTI().arrayOf(testDTI().primitiveOf("/pwd/one/bbb.zzz.json"));

    assertDCAPEquals(want, actual);
  }

  @Test
  public void test_fileExists_existingFile_returnsTrue() throws IOException, URISyntaxException {
    setupFs();

    Primitive result = FileFns.fileExists(context, "/pwd/one/bar/fff.json");
    assertThat(result.bool()).isTrue();
  }

  @Test
  public void test_fileExists_existingDir_returnsTrue() throws IOException, URISyntaxException {
    setupFs();

    Primitive result = FileFns.fileExists(context, "/pwd/one");
    assertThat(result.bool()).isTrue();
  }

  @Test
  public void test_fileExists_nonExistingFile_returnsFalse()
      throws IOException, URISyntaxException {
    setupFs();

    Primitive result = FileFns.fileExists(context, "/pwd/aaaaaaaaaaa");
    assertThat(result.bool()).isFalse();
  }

  @Test
  public void test_fileName_emptyPath() {
    Primitive got = FileFns.fileName(context, "");
    assertThat(got.string()).isEmpty();
  }

  @Test
  public void test_fileName_singleSegment() {
    Primitive got = FileFns.fileName(context, "hello");
    assertThat(got.string()).isEqualTo("hello");
  }

  @Test
  public void test_fileName_leadingSlash() {
    Primitive got = FileFns.fileName(context, "/hello");
    assertThat(got.string()).isEqualTo("hello");
  }

  @Test
  public void test_fileName_trailingSlash() {
    Primitive got = FileFns.fileName(context, "hello/");
    assertThat(got.string()).isEmpty();
  }

  @Test
  public void test_fileName_multipleSegments() {
    Primitive got = FileFns.fileName(context, "/hello/world/this/is/a/path.json.zip.tar.gz");
    assertThat(got.string()).isEqualTo("path.json.zip.tar.gz");
  }

  @After
  public void after() {
    FileFns.setFs(FileSystems.getDefault());
    ImportPath.setFs(FileSystems.getDefault());
  }
}
