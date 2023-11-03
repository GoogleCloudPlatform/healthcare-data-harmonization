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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.imports;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import java.io.IOException;

/**
 * The ExampleLoader allows the loading of any resource in an import that starts with the scheme
 * {@code example://}. For the sake of this example, the loaded resource's "contents" will just be
 * the url that was imported, without the extension.
 *
 * <p>For example:
 *
 * <pre>
 * <code>
 *   import "example://hello-world-123.example"
 * </code>
 * </pre>
 *
 * Will load "/hello-world-123" as a string. Note however, that the resource URL must also be
 * accepted by a parser, independent of this loader. The {@link ExampleParser} will accept it (see
 * there for details).
 *
 * <p>The Loader is only responsible for reading the content of a given resource, the Parser is
 * responsible for processing this content in a meaningful way.
 *
 */
public class ExampleLoader implements Loader {

  @Override
  public byte[] load(ImportPath path) throws IOException {
    // Strip extension from path, and return that as the "contents".
    String resourcePath = path.getAbsPath().toString();
    resourcePath = resourcePath.replaceAll("\\.[^.]+$", "");

    return resourcePath.getBytes(UTF_8);
  }

  @Override
  public String getName() {
    // This name determines the scheme. If we return "blah" here then this Loader will be called
    // for all imports starting with blah://
    return "example";
  }
}
