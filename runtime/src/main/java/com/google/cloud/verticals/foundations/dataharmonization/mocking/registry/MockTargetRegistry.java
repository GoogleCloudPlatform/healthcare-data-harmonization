/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.mocking.registry;

import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.MockTarget;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** Target registry when mocking is enabled. */
public class MockTargetRegistry implements PackageRegistry<Target.Constructor> {
  PackageRegistry<Target.Constructor> backing;
  Map<FunctionReference, List<Mock>> originalToMocks;

  public MockTargetRegistry(Map<FunctionReference, List<Mock>> originalToMocks) {
    this(originalToMocks, new DefaultPackageRegistry<>());
  }

  MockTargetRegistry(
      Map<FunctionReference, List<Mock>> originalToMocks,
      PackageRegistry<Target.Constructor> backing) {
    this.originalToMocks = originalToMocks;
    this.backing = backing;
  }

  @Override
  public void register(@Nonnull String packageName, @Nonnull Target.Constructor registrant) {
    backing.register(packageName, registrant);
  }

  @Override
  public Set<Constructor> getOverloads(@Nonnull Set<String> packageNames, @Nonnull String name) {
    ImmutableSet.Builder<Constructor> finalSet = ImmutableSet.builder();
    for (String pkg : packageNames) {
      FunctionReference targetRef = new FunctionReference(pkg, name);
      Set<Constructor> originalOverloads = backing.getOverloads(ImmutableSet.of(pkg), name);
      if (!originalToMocks.containsKey(targetRef)) {
        finalSet.addAll(originalOverloads);
      } else {
        finalSet.addAll(
            originalOverloads.stream()
                .map(
                    constructor ->
                        new MockTarget.Constructor(constructor, originalToMocks.get(targetRef)))
                .collect(Collectors.toSet()));
      }
    }
    return finalSet.build();
  }

  @Override
  public Set<Constructor> getAll() {
    return backing.getAll();
  }

  @Override
  public Set<String> getAllRegisteredPackages() {
    return backing.getAllRegisteredPackages();
  }

  @Override
  public Set<Constructor> getAllInPackage(String packageNames) {
    return backing.getAllInPackage(packageNames);
  }
}
