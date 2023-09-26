// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.integration.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/** Plugin to support loading files from java resources. */
public class TestLoaderPlugin implements Plugin {
  public static final String PACKAGE_NAME = "integration_test_loader_plugin";

  @Override
  public List<Loader> getLoaders() {
    return ImmutableList.of(new ResourceLoader());
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return new ArrayList<>(
        JavaFunction.ofPluginFunctionsInClass(TestPluginFunctions.class, PACKAGE_NAME));
  }

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  /** {@link Loader} that loads file from resource folder. */
  public static class ResourceLoader implements Loader {

    public static final String TEST_LOADER = "res";

    @Override
    public byte[] load(ImportPath path) throws IOException {
      InputStream stream = TestLoaderPlugin.class.getResourceAsStream(path.getAbsPath().toString());
      if (stream == null) {
        throw new FileNotFoundException(String.format("%s was not found.", path.getAbsPath()));
      }
      return ByteStreams.toByteArray(stream);
    }

    @Override
    public String getName() {
      return TEST_LOADER;
    }
  }
}
