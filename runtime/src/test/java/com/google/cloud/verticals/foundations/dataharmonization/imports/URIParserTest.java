/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.imports;

import static junit.framework.TestCase.assertEquals;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/** Tests for URIParser helper class. */
@RunWith(Parameterized.class)
public class URIParserTest {
  @Parameter public String testCaseName;

  @Parameter(1)
  public String uri;

  @Parameter(2)
  public String expectedScheme;

  @Parameter(3)
  public String expectedPath;

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(
        new Object[][] {
          {"empty", "", "file", ""},
          {"file path without scheme", "/path/to/file", "file", "/path/to/file"},
          // The relative path can be used when running with local fs whistle config. The path is
          // relative to the current working directory of the process.
          {
            "relative file path without scheme",
            "relative/path/to/file",
            "file",
            "relative/path/to/file"
          },
          {"file path", "file:///path/to/file", "file", "/path/to/file"},
          {"gcs path", "gs://bucket/path/to/object", "gs", "/bucket/path/to/object"},
          {
            "file path whistle config",
            "file:///path/to/whistle/config.wstl",
            "file",
            "/path/to/whistle/config.wstl"
          },
          {
            "file path json input",
            "file:///path/to/json/input.json",
            "file",
            "/path/to/json/input.json"
          },
          {
            "gcs path whistle config",
            "gs://bucket/path/to/whistle/config.wstl",
            "gs",
            "/bucket/path/to/whistle/config.wstl"
          },
          {
            "gcs path json input",
            "gs://bucket/path/to/json/input.json",
            "gs",
            "/bucket/path/to/json/input.json"
          },
        });
  }

  @Test
  public void testGetScheme() throws URISyntaxException {
    assertEquals(expectedScheme, URIParser.getSchema(new URI(uri)));
  }

  @Test
  public void testGetPath() throws URISyntaxException {
    assertEquals(FileSystems.getDefault().getPath(expectedPath), URIParser.getPath(new URI(uri)));
  }
}
