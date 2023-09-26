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
package com.google.cloud.verticals.foundations.dataharmonization.init.initializer;

import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRegistries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ProtoParserBase;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestLoaderRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestParserRegistry;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for {@link ExternalConfigExtractor}. */
@RunWith(JUnit4.class)
public class ExternalConfigExtractorTest {

  private Registries mockRegs;
  private static final byte[] mockFileContent = "test".getBytes(UTF_8);

  @Before
  public void init() throws IOException {
    Loader mockLoader = mock(Loader.class);
    when(mockLoader.getName()).thenReturn("testLoader");
    when(mockLoader.load(any())).thenReturn(mockFileContent);
    Parser mockParser = mock(Parser.class);
    when(mockParser.getName()).thenReturn("testParser");
    when(mockParser.canParse(any())).thenReturn(true);
    mockRegs = initRegistries(initLoaderReg(mockLoader), initParserReg(mockParser));
  }

  /** Initializes a mock loader registry with the given loader as the only entry. */
  private Registry<Loader> initLoaderReg(Loader loader) {
    Registry<Loader> memoryLoaderReg = mock(TestLoaderRegistry.class);
    when(memoryLoaderReg.get(loader.getName())).thenReturn(loader);
    return memoryLoaderReg;
  }

  /** Initializes a mock parser registry with the given parser as the only entry. */
  private Registry<Parser> initParserReg(Parser parser) {
    Registry<Parser> reg = mock(TestParserRegistry.class);
    when(reg.get(parser.getName())).thenReturn(parser);
    when(reg.getAll()).thenReturn(ImmutableSet.of(parser));
    return reg;
  }

  private Registries initRegistries(Registry<Loader> loaders, Registry<Parser> parsers) {
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getLoaderRegistry()).thenReturn(loaders);
    when(mockRegistries.getParserRegistry()).thenReturn(parsers);
    return mockRegistries;
  }

  @Test
  public void testGetFileContent_hasLoader() throws IOException {
    ImportPath iPath =
        ImportPath.of(
            "testLoader",
            FileSystems.getDefault().getPath("/testdir/test.wstl"),
            FileSystems.getDefault().getPath("/testdir/"));
    byte[] actual = new ExternalConfigExtractor(iPath).getFileContent(mockRegs);
    assertArrayEquals(mockFileContent, actual);
  }

  @Test
  public void testGetFileContent_noSuitableLoader_error() {
    ImportPath iPath =
        ImportPath.of(
            "nonExistLoader",
            FileSystems.getDefault().getPath("/testdir/test.wstl"),
            FileSystems.getDefault().getPath("/testdir/"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new ExternalConfigExtractor(iPath).getFileContent(mockRegs));
  }

  @Test
  public void testGetParser_resultNotAProtoParserBase_error() {
    ImportPath iPath =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new ExternalConfigExtractor(iPath)
                    .getParser(mockRegs, new DefaultImportProcessor()));

    assertThat(e)
        .hasMessageThat()
        .contains("Please make sure that the main file can be parsed into a PipelineConfig proto.");
  }

  @Test
  public void testGetFileContent_emptyReg() {
    String expectedLoader = "test";
    ImportPath iPath =
        ImportPath.of(
            expectedLoader,
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    Registries emptyRegistries = new DefaultRegistries();
    Exception e =
        assertThrows(
            IllegalArgumentException.class,
            () -> new ExternalConfigExtractor(iPath).getFileContent(emptyRegistries));
    assertThat(e)
        .hasMessageThat()
        .contains(String.format("Cannot find loader %s.", expectedLoader));
  }

  @Test
  public void testGetParser_emptyReg() {
    ImportPath iPath =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new ExternalConfigExtractor(iPath)
                .getParser(new DefaultRegistries(), new DefaultImportProcessor()));
  }

  @Test
  public void testGetParser_hasParser() {
    ImportPath iPath =
        ImportPath.of(
            "test",
            FileSystems.getDefault().getPath("test"),
            FileSystems.getDefault().getPath("test"));
    ProtoParserBase mockProtoParser = mock(ProtoParserBase.class);
    when(mockProtoParser.canParse(iPath)).thenReturn(true);
    Registries regs = initRegistries(null, initParserReg(mockProtoParser));

    ProtoParserBase actual =
        new ExternalConfigExtractor(iPath).getParser(regs, new DefaultImportProcessor());
    assertEquals(mockProtoParser, actual);
  }
}
