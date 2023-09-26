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

import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;
import com.google.cloud.verticals.foundations.dataharmonization.registry.util.StringSimilarity;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * A default implementation of a PackageRegistry, backed by HashMaps. Many registrants can share a
 * name and package, as long as their {@link Object#hashCode()} method returns different values.
 *
 * <p>Note that if multiple Registrants have the same {@link Object#hashCode()} and name but are in
 * different packages, and both packages are requested in {@link
 * DefaultPackageRegistry#getOverloads()} only one of these registrants will be returned. Which one
 * is undefined.
 *
 * @param <RegistrantT> the type of registrant to hold.
 */
public class DefaultPackageRegistry<RegistrantT extends Registrable>
    implements PackageRegistry<RegistrantT> {
  private final Map<String, Map<String, Set<RegistrantT>>> packageNameToRegNameToReg =
      new HashMap<>();

  @Override
  public void register(@Nonnull String packageName, @Nonnull RegistrantT reg) {
    if (packageName.trim().length() == 0) {
      throw new IllegalArgumentException("packageName cannot be blank.");
    }
    Map<String, Set<RegistrantT>> regNameToReg =
        packageNameToRegNameToReg.getOrDefault(packageName, new HashMap<>());
    Set<RegistrantT> regs = regNameToReg.getOrDefault(reg.getName(), new HashSet<>());
    if (!regs.add(reg)) {
      throw new IllegalArgumentException(
          "Item with name '" + reg.getName() + "' already exists in package '" + packageName + "'");
    }
    regNameToReg.put(reg.getName(), regs);
    packageNameToRegNameToReg.put(packageName, regNameToReg);
  }

  @Override
  public Set<RegistrantT> getOverloads(@Nonnull Set<String> packageNames, @Nonnull String name) {
    Set<RegistrantT> result = ImmutableSet.of();
    for (String packageName : packageNames) {
      if (!packageNameToRegNameToReg.containsKey(packageName)) {
        continue;
      }

      Map<String, Set<RegistrantT>> regNameToReg = packageNameToRegNameToReg.get(packageName);
      if (!regNameToReg.containsKey(name)) {
        continue;
      }

      result = Sets.union(result, regNameToReg.get(name));
    }

    return result;
  }

  @Override
  public Map<String, Set<RegistrantT>> getBestMatchOverloads(
      @Nonnull Set<String> packageNames,
      @Nonnull String functionName,
      @Nonnull StringSimilarity similarity) {
    Map<String, Map<String, Set<RegistrantT>>> matched = new HashMap<>();
    for (String packageName : packageNames) {
      for (String name : similarity.pick(packageNameToRegNameToReg.keySet(), packageName)) {
        if (similarity.accept(name, packageName)) {
          matched.putIfAbsent(name, packageNameToRegNameToReg.get(name));
        }
      }
    }

    Map<String, Set<RegistrantT>> bestMatchOverloads = new HashMap<>();
    for (Entry<String, Map<String, Set<RegistrantT>>> entry : matched.entrySet()) {
      bestMatchOverloads.put(entry.getKey(), new HashSet<>());
      for (String name : similarity.pick(entry.getValue().keySet(), functionName)) {
        if (similarity.accept(name, functionName)) {
          bestMatchOverloads.get(entry.getKey()).addAll(entry.getValue().get(name));
        }
      }
    }

    return bestMatchOverloads;
  }

  @Override
  public Set<RegistrantT> getAll() {
    return packageNameToRegNameToReg.values().stream()
        .flatMap(map -> map.values().stream())
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  @Override
  public Set<String> getAllRegisteredPackages() {
    return packageNameToRegNameToReg.keySet();
  }

  @Override
  public Set<RegistrantT> getAllInPackage(String packageName) {
    Map<String, Set<RegistrantT>> packageOverloads =
        packageNameToRegNameToReg.getOrDefault(packageName, new HashMap<>());
    return packageOverloads.values().stream().flatMap(e -> e.stream()).collect(Collectors.toSet());
  }
}
