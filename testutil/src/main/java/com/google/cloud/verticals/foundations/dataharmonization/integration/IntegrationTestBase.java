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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 * Base class for integration tests in Whistle ELP. It provides basic functionality for loading
 * config files from resources folder and loading json from resources.
 *
 * <p>Assumptions:
 * <li>By default, it uses relative path to resources/ directory to locate a file. However, classes
 *     using {@link IntegrationTestBase} are welcome to allow short-handed representation of
 *     arguments by delegation or inheritance.
 * <li>By default, the {@link ImportPath}s of all files are constructed assuming the {@link
 *     ImportPath#importsRoot} is the parent directory of the path given. Classes with other
 *     requirements are welcome to overwrite {@link IntegrationTestBase#getMainPath(String)}
 *     {@link IntegrationTestBase#getMockPath(String)}, {@link
 *     IntegrationTestBase#getJSONPath(String)}
 * <li>All loading in this class is done using {@link ResourceLoader}.
 */
public class IntegrationTestBase {
  public static final Loader DEFAULT_LOADER = new ResourceLoader();
  public static final String DEFAULT_LOADER_NAME = DEFAULT_LOADER.getName();

  public static File getFileFromURL(URL url) {
    return new File(url.getPath());
  }

  public static URL getUrlFromResource(String path) {
    URL url = IntegrationTestBase.class.getResource(path);
    if (url == null) {
      throw new IllegalArgumentException(
          String.format("Cannot find folder %s from resource folder.", path));
    }
    return url;
  }

  public boolean existsInResources(String filePath) {
    URL url = IntegrationTestBase.class.getResource(filePath);
    return url != null;
  }

  public static List<String> listFilesWithSuffix(String dirPath, String suffix) {
    File dir = getFileFromURL(getUrlFromResource(dirPath));
    FileFilter fileFilter = new WildcardFileFilter(String.format("*%s", suffix));
    File[] fileFound = dir.listFiles(fileFilter);
    if (fileFound != null) {
      return stream(fileFound)
          .map(f -> String.join(File.separator, dirPath, f.getName()))
          .collect(Collectors.toList());
    }
    return ImmutableList.of();
  }

  public Engine.Builder initializeBuilder(String mainName, String... mockNames) throws IOException {
    Engine.Builder builder =
        new Engine.Builder(ExternalConfigExtractor.of(getMainPath(mainName)))
            .withDefaultPlugins(new TestLoaderPlugin());
    for (String mockName : mockNames) {
      builder.addMock(ExternalConfigExtractor.of(getMockPath(mockName)));
    }
    return builder;
  }

  public Engine initializeTestFile(String testFileName, String... mockFileNames)
      throws IOException {
    return initializeBuilder(testFileName, mockFileNames).initialize().build();
  }

  public Data loadJson(String jsonName) throws IOException {
    ImportPath ip = getJSONPath(jsonName);
    byte[] content = DEFAULT_LOADER.load(ip);
    if (content == null) {
      throw new IllegalArgumentException(
          String.format("Cannot find %s from resource directories.", ip));
    }
    return JsonSerializerDeserializer.jsonToData(content);
  }

  protected static ImportPath toImportPath(String filePath) {
    Path path = FileSystems.getDefault().getPath(filePath);
    return ImportPath.of(DEFAULT_LOADER_NAME, path, path.getParent());
  }

  protected ImportPath getMainPath(String mainName) {
    return toImportPath(mainName);
  }

  protected ImportPath getMockPath(String mockName) {
    return toImportPath(mockName);
  }

  protected ImportPath getJSONPath(String jsonName) {
    return toImportPath(jsonName);
  }

  public String loadText(String fileName) throws IOException {
    ImportPath ip = toImportPath(fileName);
    byte[] content = DEFAULT_LOADER.load(ip);
    return new String(content, StandardCharsets.UTF_8);
  }
}
