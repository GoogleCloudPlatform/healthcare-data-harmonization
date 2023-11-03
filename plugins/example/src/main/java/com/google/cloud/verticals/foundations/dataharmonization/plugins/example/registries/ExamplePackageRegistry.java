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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.example.registries.functions.EchoFunction;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.util.StringSimilarity;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * This class shows an example implementation of a package function registry. If not implemented,
 * functions returned by {@link Plugin#getFunctions} will be registered in the same {@link
 * DefaultPackageRegistry} as builtin functions and all other plugins that don't provide their
 * PackageRegistry implementation. A custom package registry may be useful when a plugin author
 * requires custom behaviour during function registration and/or function lookup. This example
 * implementation will first attempt to find any registered functions, if no functions are found,
 * and the unknown function being searched for ends with '_echo', an {@link EchoFunction} will be
 * auto-generated. An {@link EchoFunction} simply returns it's own name when called. Whistle config
 * usage of this would be {@code example::unRegisteredFunction_echo()}. This function is not part of
 * the registered functions and ends with '_echo'. When looked up during Closure execution, an
 * instance of {@link EchoFunction} will be returned, with the {@link Signature} name set to
 * 'unRegisteredFunction_echo'.
 *
 * <p>A real world use-case for defining custom behaviour for the function registry may be to enable
 * calling Native Java or Python functions from a Whistle config file. Such an implementation might
 * involve using reflection to generate these functions during function lookup time, then returning
 * instances of these functions wrapped around a {@link CallableFunction}.
 */
public class ExamplePackageRegistry implements PackageRegistry<CallableFunction> {

  // This Package Registry is backed by a default package registry for 'actual' functions which are
  // defined for this plugin. These functions get registered to this backing registry at engine
  // initialization.
  PackageRegistry<CallableFunction> backing = new DefaultPackageRegistry<>();

  /**
   * This method is called during {@link Engine.Builder#initialize(MetaData) Engine} initialization,
   * and is responsible for registering the plugin functions to this package registry.
   *
   * @param packageName Name of the package associated with the functions. Which for this plugin is
   *     'Example'
   * @param registrant The functions to register to this registry.
   */
  @Override
  public void register(@Nonnull String packageName, @Nonnull CallableFunction registrant) {
    backing.register(packageName, registrant);
  }

  /**
   * This method gets called whenever a {@link Closure#execute(RuntimeContext) Closure} is executed,
   * and is responsible for returning the plugin function to be executed via {@link
   * CallableFunction#call(RuntimeContext, Data...)}. We use default behaviour from {@link
   * DefaultPackageRegistry}, for any functions that are registered, but define custom behaviour for
   * functions which are not registered to our package. In this example, we return an instance of
   * EchoFunction should the function name end with '_echo'.
   *
   * @param packageNames Name of the package being requested
   * @param name Name of the function being requested
   * @return Set of functions which match the requested package name, and function name
   */
  @Override
  public Set<CallableFunction> getOverloads(
      @Nonnull Set<String> packageNames, @Nonnull String name) {
    Set<CallableFunction> res = backing.getOverloads(packageNames, name);

    // To illustrate custom behaviour when a registered function is not found
    if (res.isEmpty() && name.endsWith("_echo")) {
      return ImmutableSet.of(new EchoFunction(name));
    }

    return res;
  }

  /**
   * This method is used to generate useful error messages and is called whenever {@link
   * ExamplePackageRegistry#getOverloads(Set, String)} returns an empty set. It attempts to return
   * suggested functions which may be a best match, should an exact match not be returned by {@link
   * ExamplePackageRegistry#getOverloads(Set, String)}. An example where this may prove useful is
   * when a user mis-spells a function name in their Whistle config file, in which case {@link
   * ExamplePackageRegistry#getOverloads(Set, String)} will return an empty set. This function would
   * be implemented to return a set of suggested registered functions, which the user may have been
   * intending to call instead.
   *
   * @param packageNames Set of package names to search through
   * @param functionName Name of the function to search
   * @param similarity Used to define a threshold for when to return any matched functions.
   * @return Returns a map where package names are keys and values are a set of {@link
   *     CallableFunction}. This result represents the closest matching packages and functions found
   *     registered to this plugin, should an exact match not be found.
   */
  @Override
  public Map<String, Set<CallableFunction>> getBestMatchOverloads(
      @Nonnull Set<String> packageNames,
      @Nonnull String functionName,
      @Nonnull StringSimilarity similarity) {

    // We use default behaviour for this example, and also add in our own suggestion for a function.
    // to demonstrate how this method may be customized.
    Map<String, Set<CallableFunction>> res =
        backing.getBestMatchOverloads(packageNames, functionName, similarity);

    // And add in a suggested result to showcase what a response may look-like
    EchoFunction suggestedFunction = new EchoFunction(functionName + "_echo");
    res.computeIfAbsent("example", k -> new HashSet<>()).add(suggestedFunction);

    return res;
  }

  // Returns all registered functions. Will not return instances of EchoFunctions which are not
  // registered.
  @Override
  public Set<CallableFunction> getAll() {
    return backing.getAll();
  }

  // Returns all registered packages.
  @Override
  public Set<String> getAllRegisteredPackages() {
    return backing.getAllRegisteredPackages();
  }

  @Override
  public Set<CallableFunction> getAllInPackage(String packageNames) {
    return backing.getAllInPackage(packageNames);
  }
}
