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

package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for PackageContext equals() method. */
@RunWith(JUnit4.class)
public class PackageContextTest {
  @Test
  public void equals_onPackageContext_identity() {

    List<String> list = Arrays.asList("setValue1", "setValue2", "setValue3");
    Set<String> packageSet1 = new HashSet<>(list);

    PackageContext pkgCtx = new PackageContext(packageSet1);

    assertEquals(pkgCtx, pkgCtx);
  }

  @Test
  public void equals_onPackageContext_sameValues() {

    List<String> list1 = Arrays.asList("setValue1", "setValue2", "setValue3");
    List<String> list2 = Arrays.asList("setValue1", "setValue2", "setValue3");
    Set<String> packageSet1 = new HashSet<>(list1);
    Set<String> packageSet2 = new HashSet<>(list2);

    PackageContext pkgCtx1 = new PackageContext(packageSet1);
    PackageContext pkgCtx2 = new PackageContext(packageSet2);
    assertEquals(pkgCtx2, pkgCtx1);
  }

  @Test
  public void equals_onPackageContext_differentValues() {

    List<String> list = Arrays.asList("setValue1", "setValue2", "setValue3");
    List<String> list2 = Arrays.asList("setValue1", "setValue2", "setValue4");
    Set<String> packageSet1 = new HashSet<>(list);
    Set<String> packageSet2 = new HashSet<>(list2);

    PackageContext pkgCtx1 = new PackageContext(packageSet1);
    PackageContext pkgCtx2 = new PackageContext(packageSet2);
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_differentCurrent() {

    List<String> list1 = Arrays.asList("setValue1", "setValue2", "setValue3");
    List<String> list2 = Arrays.asList("setValue1", "setValue2", "setValue3");
    Set<String> packageSet1 = new HashSet<>(list1);
    Set<String> packageSet2 = new HashSet<>(list2);

    PackageContext pkgCtx1 =
        new PackageContext(
            packageSet1, "a", ImportPath.of("file", Path.of("/a/b"), Path.of("/a/b")));
    PackageContext pkgCtx2 =
        new PackageContext(
            packageSet2, "b", ImportPath.of("file", Path.of("/a/b"), Path.of("/a/b")));
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_differentPath() {

    List<String> list1 = Arrays.asList("setValue1", "setValue2", "setValue3");
    List<String> list2 = Arrays.asList("setValue1", "setValue2", "setValue3");
    Set<String> packageSet1 = new HashSet<>(list1);
    Set<String> packageSet2 = new HashSet<>(list2);

    PackageContext pkgCtx1 =
        new PackageContext(
            packageSet1, "a", ImportPath.of("file", Path.of("/a/b"), Path.of("/a/b")));
    PackageContext pkgCtx2 =
        new PackageContext(
            packageSet2, "a", ImportPath.of("file", Path.of("/a/z"), Path.of("/a/b")));
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_differentLengths() {

    List<String> list1 = Arrays.asList("setValue1", "setValue2", "setValue3");
    List<String> list2 = Arrays.asList("setValue1", "setValue2");
    Set<String> packageSet1 = new HashSet<>(list1);
    Set<String> packageSet2 = new HashSet<>(list2);

    PackageContext pkgCtx1 = new PackageContext(packageSet1);
    PackageContext pkgCtx2 = new PackageContext(packageSet2);
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_firstContextEmpty() {

    Set<String> packageSet1 = new HashSet<>();
    Set<String> packageSet2 = new HashSet<>(Arrays.asList("setValue1", "setValue2"));

    PackageContext pkgCtx1 = new PackageContext(packageSet1);
    PackageContext pkgCtx2 = new PackageContext(packageSet2);
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_secondContextEmpty() {

    Set<String> packageSet1 = new HashSet<>();
    Set<String> packageSet2 = new HashSet<>(Arrays.asList("setValue1", "setValue2"));

    PackageContext pkgCtx1 = new PackageContext(packageSet2);
    PackageContext pkgCtx2 = new PackageContext(packageSet1);
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void equals_onPackageContext_secondContextNull() {
    Set<String> packageSet = new HashSet<>(Arrays.asList("setValue1", "setValue2"));

    PackageContext pkgCtx1 = new PackageContext(packageSet);
    PackageContext pkgCtx2 = null;
    assertNotEquals(pkgCtx1, pkgCtx2);
  }

  @Test
  public void getCurrentPackage_singleAlias_returnsIt() {
    ImmutableSet<String> packageSet = ImmutableSet.of("it");

    PackageContext pkgCtx1 = new PackageContext(packageSet);
    assertEquals("it", pkgCtx1.getCurrentPackage());
  }

  @Test
  public void getCurrentPackage_noAliases_returnsWildcard() {
    ImmutableSet<String> packageSet = ImmutableSet.of();

    PackageContext pkgCtx1 = new PackageContext(packageSet);
    assertEquals(FunctionReference.WILDCARD_PACKAGE_NAME, pkgCtx1.getCurrentPackage());
  }

  @Test
  public void getCurrentPackage_manyAliases_returnsWildcard() {
    ImmutableSet<String> packageSet = ImmutableSet.of("a", "b");

    PackageContext pkgCtx1 = new PackageContext(packageSet);
    assertEquals(FunctionReference.WILDCARD_PACKAGE_NAME, pkgCtx1.getCurrentPackage());
  }

  @Test
  public void getCurrentPackage_manyAliasesWithSetPackage_returnsIt() {
    ImmutableSet<String> packageSet = ImmutableSet.of("a", "b");

    PackageContext pkgCtx1 =
        new PackageContext(
            packageSet, "it", ImportPath.of("file", Path.of("/a/b"), Path.of("/a/b")));
    assertEquals("it", pkgCtx1.getCurrentPackage());
  }

  @Test
  public void getCurrentPath_defaultPath_returnsRoot() {
    ImmutableSet<String> packageSet = ImmutableSet.of("a", "b");

    PackageContext pkgCtx1 = new PackageContext(packageSet);
    assertEquals(ImportPath.of("file", Path.of("/"), Path.of("/")), pkgCtx1.getCurrentImportPath());
  }
}
