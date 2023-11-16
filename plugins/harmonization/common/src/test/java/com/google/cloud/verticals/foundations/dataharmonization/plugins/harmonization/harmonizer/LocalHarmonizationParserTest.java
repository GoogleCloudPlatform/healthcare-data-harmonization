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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.harmonizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.harmonization.LocalHarmonizationParser;
import com.google.gson.JsonParseException;
import java.nio.file.Path;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link LocalHarmonizationParser} */
@RunWith(JUnit4.class)
public class LocalHarmonizationParserTest {

  @Test
  public void harmonizationParser_canParse_true() {
    ImportPath importPath =
        ImportPath.of(
            "file",
            Path.of("/tmp/conceptMaps/test1.harmonization.json"),
            Path.of("/tmp/conceptMaps"));
    LocalHarmonizationParser parser = new LocalHarmonizationParser();
    assertTrue(parser.canParse(importPath));
  }

  @Test
  public void harmonizationParser_canParse_false() {
    ImportPath importPath =
        ImportPath.of(
            "file",
            Path.of("/tmp/conceptMaps/test1.nonconceptmap.json"),
            Path.of("/tmp/conceptMaps"));
    LocalHarmonizationParser parser = new LocalHarmonizationParser();
    assertFalse(parser.canParse(importPath));
  }

  @Test
  public void harmonizationParser_parse_throws() {
    ImportPath importPath =
        ImportPath.of("test", Path.of("/testPath/testFile.conceptmap.json"), Path.of("/testPath"));
    byte[] conceptMapBytes =
        new String("{\n" + "\t\"targetField1\": \"targetValue1\",\n" + "\t]\n" + "}").getBytes();
    JsonParseException thrown =
        assertThrows(
            JsonParseException.class,
            () ->
                new LocalHarmonizationParser()
                    .parse(
                        conceptMapBytes,
                        mock(Registries.class),
                        mock(MetaData.class),
                        mock(ImportProcessor.class),
                        importPath));
    assertEquals(
        String.format("Failed to parse ConceptMap %s.", importPath.toString()),
        thrown.getMessage());
  }
}
