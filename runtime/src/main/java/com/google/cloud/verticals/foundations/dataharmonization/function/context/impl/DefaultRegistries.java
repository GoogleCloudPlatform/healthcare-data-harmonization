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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Default {@link Registries} implementation. */
public class DefaultRegistries implements Registries {

  private final Map<Class<? extends Plugin>, Plugin> loadedPlugins;
  private final PackageRegistry<CallableFunction> functionPackageRegistry;
  private final Map<String, PackageRegistry<CallableFunction>> pluginFunctionRegistries;
  private final PackageRegistry<Constructor> targetPackageRegistry;
  private final Registry<ArgModifier> argModifierRegistry;
  private final Registry<Loader> loaderRegistry;
  private final Registry<Parser> parserRegistry;
  private final Registry<Option> optionRegistry;

  public DefaultRegistries() {
    this(
        new DefaultPackageRegistry<CallableFunction>(),
        new DefaultPackageRegistry<Constructor>(),
        new DefaultRegistry<ArgModifier>());
  }

  public DefaultRegistries(
      PackageRegistry<CallableFunction> functionPackageRegistry,
      PackageRegistry<Constructor> targetPackageRegistry,
      Registry<ArgModifier> argModifierRegistry) {
    this(
        functionPackageRegistry,
        new HashMap<String, PackageRegistry<CallableFunction>>(),
        targetPackageRegistry,
        argModifierRegistry,
        new DefaultRegistry<>(),
        new DefaultRegistry<>(),
        new DefaultRegistry<>());
  }

  public DefaultRegistries(
      PackageRegistry<CallableFunction> functionPackageRegistry,
      Map<String, PackageRegistry<CallableFunction>> pluginFunctionRegistries,
      PackageRegistry<Constructor> targetPackageRegistry,
      Registry<ArgModifier> argModifierRegistry,
      Registry<Loader> loaderRegistry,
      Registry<Parser> parserRegistry,
      Registry<Option> optionRegistry) {

    this.loadedPlugins = new HashMap<>();

    this.functionPackageRegistry = functionPackageRegistry;
    this.pluginFunctionRegistries = pluginFunctionRegistries;
    this.targetPackageRegistry = targetPackageRegistry;
    this.argModifierRegistry = argModifierRegistry;
    this.loaderRegistry = loaderRegistry;
    this.parserRegistry = parserRegistry;
    this.optionRegistry = optionRegistry;
  }

  @Override
  public PackageRegistry<CallableFunction> getFunctionRegistry(String packageName) {
    PackageRegistry<CallableFunction> funcReg = pluginFunctionRegistries.get(packageName);
    if (funcReg == null) {
      return functionPackageRegistry;
    }
    return funcReg;
  }

  @Override
  public void registerFunctionRegistry(
      String packageName, PackageRegistry<CallableFunction> registry) {
    pluginFunctionRegistries.put(packageName, registry);
  }

  @Override
  public Set<String> getAllRegisteredPackages() {
    Set<String> packageNames = new HashSet<>(pluginFunctionRegistries.keySet());
    packageNames.addAll(functionPackageRegistry.getAllRegisteredPackages());
    return packageNames;
  }

  @Override
  public PackageRegistry<Constructor> getTargetRegistry() {
    return targetPackageRegistry;
  }

  @Override
  public Registry<ArgModifier> getArgModifierRegistry() {
    return argModifierRegistry;
  }

  @Override
  public Registry<Loader> getLoaderRegistry() {
    return loaderRegistry;
  }

  @Override
  public Registry<Parser> getParserRegistry() {
    return parserRegistry;
  }

  @Override
  public Registry<Option> getOptionRegistry() {
    return optionRegistry;
  }

  @Override
  public void addLoadedPlugin(Plugin plugin) {
    loadedPlugins.put(plugin.getClass(), plugin);
  }

  @Override
  public Set<Plugin> getLoadedPlugins() {
    return ImmutableSet.copyOf(loadedPlugins.values());
  }
}
