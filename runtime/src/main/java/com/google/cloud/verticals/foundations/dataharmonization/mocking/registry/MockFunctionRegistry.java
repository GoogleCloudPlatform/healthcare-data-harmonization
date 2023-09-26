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

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.Mock;
import com.google.cloud.verticals.foundations.dataharmonization.mocking.wrappers.MockFunction;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** Function registry when mocking is enabled. */
public class MockFunctionRegistry implements PackageRegistry<CallableFunction> {
  private final PackageRegistry<CallableFunction> backing;
  private final ImmutableMap<FunctionReference, List<Mock>> originalToMocks;

  public MockFunctionRegistry(Map<FunctionReference, List<Mock>> originalToMock) {
    this(originalToMock, new DefaultPackageRegistry<>());
  }

  /**
   * @param originalToMocks a map from a {@link FunctionReference} to a list of {@link Mock}s that
   *     are registered for it in the mock config file;
   * @param backing the {@link PackageRegistry} that carries all functions registered from both main
   *     config and mock config.
   */
  MockFunctionRegistry(
      Map<FunctionReference, List<Mock>> originalToMocks,
      PackageRegistry<CallableFunction> backing) {
    this.originalToMocks = ImmutableMap.copyOf(originalToMocks);
    this.backing = backing;
  }

  @Override
  public void register(@Nonnull String packageName, @Nonnull CallableFunction callableFunction) {
    backing.register(packageName, callableFunction);
  }

  private boolean sigMatches(Signature mock, Signature original) {
    return mock.getArgs().size() == original.getArgs().size()
        // when original is a variadic function, the mock function can have multiple arguments in
        // place of the last variadic argument.
        || (original.isVariadic() && (mock.getArgs().size() >= original.getArgs().size() - 1))
        || (mock.isVariadic() && (original.getArgs().size() >= mock.getArgs().size() - 1));
  }

  /**
   * Finds all {@link CallableFunction} with the given name, that exists in any of the given
   * packages. If any of those accessed functions is mocked in the mock config, this method looks
   * through all Mocks that are associated with it and wraps the function into {@link MockFunction}
   * together with all {@link Mock}s that match to the signature of the original function. Currently
   * no signature check is done on selector functions.
   */
  @Override
  public ImmutableSet<CallableFunction> getOverloads(
      @Nonnull Set<String> packageNames, @Nonnull String name) {
    ImmutableSet.Builder<CallableFunction> finalSet = ImmutableSet.builder();
    for (String pkg : packageNames) {
      FunctionReference fr = new FunctionReference(pkg, name);
      if (!originalToMocks.containsKey(fr)) {
        finalSet.addAll(backing.getOverloads(ImmutableSet.of(pkg), name));
        continue;
      }
      Set<CallableFunction> overloads = backing.getOverloads(ImmutableSet.of(pkg), name);
      List<Mock> mocks = originalToMocks.get(fr);
      for (CallableFunction overload : overloads) {
        // find mocks on each of the overload and filter out all that don't match with the
        // signature of the original overload.
        List<Mock> matchedMocks =
            mocks.stream()
                .filter(
                    mock ->
                        backing
                            .getOverloads(
                                ImmutableSet.of(mock.getMockRef().getPackageName()),
                                mock.getMockRef().getFunctionName())
                            .stream()
                            .map(CallableFunction::getSignature)
                            .anyMatch(s -> sigMatches(s, overload.getSignature())))
                .collect(Collectors.toList());
        if (matchedMocks.isEmpty()) {
          finalSet.add(overload);
        } else {
          finalSet.add(new MockFunction(overload, ImmutableList.copyOf(matchedMocks)));
        }
      }
    }
    return finalSet.build();
  }

  @Override
  public Set<CallableFunction> getAll() {
    return backing.getAll();
  }

  @Override
  public Set<String> getAllRegisteredPackages() {
    return backing.getAllRegisteredPackages();
  }

  @Override
  public Set<CallableFunction> getAllInPackage(String packageNames) {
    return backing.getAllInPackage(packageNames);
  }
}
