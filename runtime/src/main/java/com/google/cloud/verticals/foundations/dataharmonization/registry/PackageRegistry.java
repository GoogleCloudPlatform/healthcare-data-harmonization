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

package com.google.cloud.verticals.foundations.dataharmonization.registry;

import com.google.cloud.verticals.foundations.dataharmonization.registry.util.StringSimilarity;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/** A simple mapping of {@link Registrable}s to their names, organized by package. */
public interface PackageRegistry<RegistrantT extends Registrable> extends Serializable {

  /** Register the given registrant under the given package name. */
  void register(@Nonnull String packageName, @Nonnull RegistrantT registrant);

  /**
   * Finds all registrants with the given name, and present in any of the given packages. A blank
   * name is a valid entry.
   */
  Set<RegistrantT> getOverloads(@Nonnull Set<String> packageNames, @Nonnull String name);

  /** Finds all registrants similar with the given name and packages under the given similarity. */
  default Map<String, Set<RegistrantT>> getBestMatchOverloads(
      @Nonnull Set<String> packageNames,
      @Nonnull String functionName,
      @Nonnull StringSimilarity similarity) {
    return new HashMap<>();
  }

  /** Returns all unique registrants in the Registry. */
  Set<RegistrantT> getAll();

  /** Returns the names of all registered packages. */
  Set<String> getAllRegisteredPackages();

  /** Returns a set of all the overloads for a given package */
  Set<RegistrantT> getAllInPackage(String packageNames);
}
