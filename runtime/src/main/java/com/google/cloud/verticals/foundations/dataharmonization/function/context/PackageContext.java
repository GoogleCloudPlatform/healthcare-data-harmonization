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

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.common.collect.ImmutableSet;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;

/** Data class for storing package-specific information. */
public final class PackageContext implements Serializable {
  private final ImmutableSet<String> globallyAliasedPackages;
  private final String currentPackage;
  private final ImportPath currentImportPath;

  public PackageContext(Set<String> globallyAliasedPackages) {
    this(
        globallyAliasedPackages,
        globallyAliasedPackages.size() == 1
            ? globallyAliasedPackages.iterator().next()
            : FunctionReference.WILDCARD_PACKAGE_NAME,
        ImportPath.of("file", Path.of("/"), Path.of("/")));
  }

  public PackageContext(
      Set<String> globallyAliasedPackages, String currentPackage, ImportPath currentImportPath) {
    this.globallyAliasedPackages =
        ImmutableSet.<String>builder()
            .addAll(globallyAliasedPackages)
            // Always add Builtins by default.
            .add(Builtins.PACKAGE_NAME)
            .build();
    this.currentPackage = currentPackage;
    this.currentImportPath = currentImportPath;
  }

  /**
   * Returns a set of all packages imported into the global namespace. This is the set of packages
   * whose functions can be accessed without a package indicator.
   */
  public ImmutableSet<String> getGloballyAliasedPackages() {
    return globallyAliasedPackages;
  }

  /** Returns the current package that this context was created for. */
  public String getCurrentPackage() {
    return currentPackage;
  }

  /**
   * Returns the import path that imported the current file. Note that this may differ between files
   * in the same package, as it refers to the individual file not the package as a whole.
   */
  public ImportPath getCurrentImportPath() {
    return currentImportPath;
  }

  /**
   * Override of equals method to provide a logical equivalence check for PackageContext.
   *
   * @param object The object on which to execute the comparison
   * @return true if equal, otherwise false
   */
  @Override
  public boolean equals(@Nullable Object object) {
    if (!(object instanceof PackageContext)) {
      return false;
    }
    PackageContext that = (PackageContext) object;
    return Objects.equals(this.globallyAliasedPackages, that.globallyAliasedPackages)
        && Objects.equals(this.currentPackage, that.currentPackage)
        && Objects.equals(this.currentImportPath, that.currentImportPath);
  }

  /**
   * Override of hashCode on the class to provide the basis for testing for logical equivalence.
   *
   * @return int hash value of the class.
   */
  @Override
  public int hashCode() {
    return Objects.hash(globallyAliasedPackages, currentPackage, currentImportPath);
  }
}
