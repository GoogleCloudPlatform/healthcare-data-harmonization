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

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import java.io.Serializable;
import java.util.Set;

/** A common data structure to store all registries needed for engine execution. */
public interface Registries extends Serializable {

  /** Returns the function {@link PackageRegistry} for the specified package. */
  PackageRegistry<CallableFunction> getFunctionRegistry(String packageName);

  /** Register the {@link PackageRegistry} for the specified package. */
  void registerFunctionRegistry(String packageName, PackageRegistry<CallableFunction> registry);

  /** Returns the names of all registered packages. */
  Set<String> getAllRegisteredPackages();

  /** Returns the target {@link PackageRegistry} for the current context. */
  PackageRegistry<Constructor> getTargetRegistry();

  /** Returns the argument modifier {@link Registry} for the current context. */
  Registry<ArgModifier> getArgModifierRegistry();

  /**
   * Get the {@link Loader}s this processor can use. This should return a registry that can be
   * updated as plugins containing Loader implementations are imported.
   */
  Registry<Loader> getLoaderRegistry();

  /**
   * Get the {@link Parser}s this processor can use. This should return a registry that can be
   * updated as plugins containing Parser implementations are imported.
   */
  Registry<Parser> getParserRegistry();

  /** Returns the option {@link Registry} for the current context. */
  Registry<Option> getOptionRegistry();

  /**
   * Registers a loaded plugin. This does not load the plugin. The known loaded plugins can then be
   * retrieved with {@link #getLoadedPlugins}.
   */
  void addLoadedPlugin(Plugin plugin);

  /** Gets all registered plugins. */
  Set<Plugin> getLoadedPlugins();
}
