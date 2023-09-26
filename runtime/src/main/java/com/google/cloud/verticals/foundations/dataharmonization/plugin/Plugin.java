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

package com.google.cloud.verticals.foundations.dataharmonization.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import javax.annotation.Nullable;

/**
 * A Whistle Plugin. This class points to functions, targets, loaders, and parsers provided by a
 * given plugin.
 */
public interface Plugin extends AutoCloseable, Serializable {

  /** Load all of the contents of this plugin into the given Registries. */
  static void load(Plugin plugin, Registries registries, MetaData metaData) {
    registerLoaderParser(plugin, registries);

    String pkgName = plugin.getPackageName();
    // Register the PackageRegistry to the runtime if provided.
    if (plugin.getFunctionRegistry() != null) {
      registries.registerFunctionRegistry(pkgName, plugin.getFunctionRegistry());
    }
    plugin
        .getFunctions()
        .forEach(fn -> registries.getFunctionRegistry(pkgName).register(pkgName, fn));

    plugin
        .getTargets()
        .forEach(t -> registries.getTargetRegistry().register(plugin.getPackageName(), t));

    plugin
        .getArgModifiers()
        .forEach(argMod -> registries.getArgModifierRegistry().register(argMod));

    plugin.getOptions().forEach(registries.getOptionRegistry()::register);

    registries.addLoadedPlugin(plugin);
    plugin.onLoaded(registries, metaData);
  }

  /**
   * Loads all loaders and parsers from the given {@link Plugin} into the given {@link Registries}.
   *
   * @param plugin {@link Plugin} that might contains loaders and parsers.
   * @param registries {@link Registries} to load into.
   */
  static void registerLoaderParser(Plugin plugin, Registries registries) {
    plugin.getLoaders().forEach(registries.getLoaderRegistry()::register);
    plugin.getParsers().forEach(registries.getParserRegistry()::register);
  }

  default List<Loader> getLoaders() {
    return ImmutableList.of();
  }

  default List<Parser> getParsers() {
    return ImmutableList.of();
  }

  default List<Option> getOptions() {
    return ImmutableList.of();
  }

  String getPackageName();

  /**
   * Plugins can provide a custom {@code PackageRegistry} to alter the function lookup behavior. A
   * default implementation will be provided otherwise.
   *
   * <p>TODO(lastomato): should each plugin have its own registry?
   */
  @Nullable
  default PackageRegistry<CallableFunction> getFunctionRegistry() {
    return null;
  }

  default List<CallableFunction> getFunctions() {
    return ImmutableList.of();
  }

  default List<Target.Constructor> getTargets() {
    return ImmutableList.of();
  }

  default List<ArgModifier> getArgModifiers() {
    return ImmutableList.of();
  }

  @Override
  default void close() {}

  default void onLoaded(Registries registries, MetaData metaData) {}
}
