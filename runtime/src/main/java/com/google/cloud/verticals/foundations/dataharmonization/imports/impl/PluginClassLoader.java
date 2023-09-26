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

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import java.io.IOException;
import java.util.Arrays;

/**
 * "Loads" a class from the current class path. This method simply checks if the class exists (by
 * name), then returns the bytes representing the string of the name. The class must implement
 * {@link Plugin}.
 */
public class PluginClassLoader implements Loader {
  public static final String NAME = "class";

  @Override
  public byte[] load(ImportPath importPath) throws IOException {
    String className = importPath.getFileName();
    try {
      Class<?> clazz = Class.forName(className);
      if (Arrays.stream(clazz.getInterfaces()).noneMatch(Plugin.class::equals)) {
        throw new IOException(
            String.format("Class %s does not implement %s.", className, Plugin.class.getName()));
      }
    } catch (ClassNotFoundException e) {
      throw new IOException(
          String.format(
              "Class not found: %s. Make sure it is in the current classpath.", className));
    }

    return className.getBytes();
  }

  @Override
  public String getName() {
    return NAME;
  }
}
