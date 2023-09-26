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

package com.google.cloud.verticals.foundations.dataharmonization.imports.impl;

import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Set;

/**
 * Loads and initializes a {@link Plugin}. The input bytes should be an encoding of the string,
 * representing the name. For example,
 * "com.google.cloud.verticals.foundations.dataharmonization.plugin.MyPlugin".
 */
public class PluginClassParser implements Parser {
  public static final String NAME = "class";

  private final ImmutableSet<String> allowlist;

  public PluginClassParser() {
    this(ImmutableSet.of());
  }

  public PluginClassParser(Set<String> allowlist) {
    this.allowlist = ImmutableSet.copyOf(allowlist);
  }

  @Override
  public void parse(
      byte[] data,
      Registries registries,
      MetaData metaData,
      ImportProcessor processor,
      ImportPath iPath)
      throws IOException {
    String name = new String(data);
    if (!allowlist.isEmpty() && !allowlist.contains(name)) {
      throw new IOException(
          String.format(
              "Plugin class %s is not known. Known plugins:\n  %s",
              name, allowlist.stream().sorted().collect(joining("\n  "))));
    }

    try {
      Class<? extends Plugin> clazz = Class.forName(name).asSubclass(Plugin.class);
      Plugin plugin = clazz.getDeclaredConstructor().newInstance();
      Plugin.load(plugin, registries, metaData);
    } catch (ClassNotFoundException e) {
      throw new IOException(
          String.format("Class not found: %s. Make sure it is in the current classpath.", name), e);
    } catch (ReflectiveOperationException e) {
      throw new IOException(String.format("Cannot instantiate plugin class %s.", name), e);
    }
  }

  @Override
  public boolean canParse(ImportPath path) {
    String name = path.getFileName();
    try {
      return Plugin.class.isAssignableFrom(Class.forName(name));
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public String getName() {
    return NAME;
  }
}
