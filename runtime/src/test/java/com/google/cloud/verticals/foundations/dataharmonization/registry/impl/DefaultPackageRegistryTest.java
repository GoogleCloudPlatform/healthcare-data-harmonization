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

package com.google.cloud.verticals.foundations.dataharmonization.registry.impl;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for DefaultRegistry. */
@RunWith(JUnit4.class)
public class DefaultPackageRegistryTest {

  private static Registrable namedReg(String name) {
    return () -> name;
  }

  @Test
  public void register_blankName_throws() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();

    assertThrows(IllegalArgumentException.class, () -> reg.register("", mock(Registrable.class)));
  }

  @Test
  public void getOverloads_unknownPackage_returnsEmptyList() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("unknown"), "any");

    assertNotNull(overloads);
    assertThat(overloads).isEmpty();
  }

  @Test
  public void getOverloads_unknownName_returnsEmptyList() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("known", namedReg("one"));

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("known"), "two");

    assertNotNull(overloads);
    assertThat(overloads).isEmpty();
  }

  @Test
  public void getOverloads_blankName_returnsEntry() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("known", namedReg("one"));
    reg.register("known", namedReg(""));

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("known"), "");

    assertThat(overloads).hasSize(1);
    assertThat(overloads.iterator().next().getName()).isEmpty();
  }

  @Test
  public void getOverloads_singlePackage_returnsEntry() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("known", namedReg("right"));
    reg.register("known", namedReg("wrong"));

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("known"), "right");

    assertThat(overloads).hasSize(1);
    assertEquals("right", overloads.iterator().next().getName());
  }

  @Test
  public void getOverloads_multiplePackages_returnsEntry() {
    Registrable right1 = namedReg("right");
    Registrable right2 = namedReg("right");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right1);
    reg.register("pkg1", namedReg("wrong"));
    reg.register("pkg2", namedReg("wrong"));
    reg.register("pkg2", right2);

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("pkg1", "pkg2"), "right");

    assertEquals(ImmutableSet.of(right1, right2), overloads);
  }

  @Test
  public void getOverloads_singlePackageAndSameName_returnsAllEntries() {
    Registrable right1 = namedReg("right");
    Registrable right2 = namedReg("right");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right1);
    reg.register("pkg1", right2);
    reg.register("pkg1", namedReg("wrong"));

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("pkg1"), "right");

    assertEquals(ImmutableSet.of(right1, right2), overloads);
  }

  @Test
  public void getOverloads_multiplePackagesSameHashCode_returnsSingle() {
    Registrable right = namedReg("right");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right);
    reg.register("pkg1", namedReg("wrong"));
    reg.register("pkg2", namedReg("wrong"));
    reg.register("pkg2", right);

    Set<Registrable> overloads = reg.getOverloads(ImmutableSet.of("pkg1", "pkg2"), "right");

    assertEquals(Collections.singleton(right), overloads);
  }

  @Test
  public void getOverloads_singlePackageSameHashCode_throwsIllegalArgumentException() {
    Registrable right = namedReg("right");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right);
    IllegalArgumentException error =
        assertThrows(IllegalArgumentException.class, () -> reg.register("pkg1", right));

    assertThat(error)
        .hasMessageThat()
        .contains("Item with name 'right' already exists in package 'pkg1'");
  }

  @Test
  public void getAll_emptyRegistry_returnsEmptyList() {
    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();

    Set<Registrable> overloads = reg.getAll();

    assertNotNull(overloads);
    assertThat(overloads).isEmpty();
  }

  @Test
  public void getAll_returnsAllEntries() {
    Registrable right1 = namedReg("right");
    Registrable right2 = namedReg("right");
    Registrable wrong = namedReg("wrong");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right1);
    reg.register("pkg1", right2);
    reg.register("pkg2", wrong);

    Set<Registrable> overloads = reg.getAll();

    assertEquals(ImmutableSet.of(right1, right2, wrong), overloads);
  }

  @Test
  public void getAll_duplicateRegistrant_returnsSingleEntry() {
    Registrable right1 = namedReg("right");

    PackageRegistry<Registrable> reg = new DefaultPackageRegistry<>();
    reg.register("pkg1", right1);
    reg.register("pkg2", right1);

    Set<Registrable> overloads = reg.getAll();

    assertThat(overloads).hasSize(1);
  }
}
