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

import static com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor.IMPORT_EXCEPTION_LIST_KEY;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.ImportInfo;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.Source;
import com.google.cloud.verticals.foundations.dataharmonization.debug.proto.Debug.SourcePosition;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.ImportException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.InitializationContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.Meta;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestLoaderRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestParserRegistry;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Any;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InOrder;
import org.mockito.Mockito;

/** Tests for DefaultImportProcessor. */
@RunWith(JUnit4.class)
public class DefaultImportProcessorTest {

  /**
   * mockParser is a placeholder for faking a parse. It can be extended to return "parsed"
   * representations of test/fake byte[] with further mock calls.
   */
  private Parser mockParser;

  /** A fake root path to a main config file. */
  private ImportPath rootPath;

  @Before
  public void initParser() {
    mockParser = when(mock(Parser.class).getName()).thenReturn("mock").getMock();
    when(mockParser.canParse(any())).thenReturn(true);
    rootPath = ImportPathUtil.projectFile("/root/project/file.wstl");
    // The parser can now be extended with
    // doAnswer(i -> {
    //               // Do something when byte[] bytes is parsed, for example, process other imports
    //               return null;
    //             })
    //         .when(mockParser)
    //         .parse(eq(bytes), any(), any());
  }

  /** Initialize a mock loader registry with the given loader as the only entry. */
  private Registry<Loader> initLoaderReg(Loader loader) {
    Registry<Loader> memoryLoaderReg = mock(TestLoaderRegistry.class);
    when(memoryLoaderReg.get(loader.getName())).thenReturn(loader);
    return memoryLoaderReg;
  }

  /** Initialize a mock parser registry with the given parser as the only entry. */
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

  private InitializationContext initRuntimeContext(Registries registries, MetaData metaData) {
    InitializationContext mockRuntimeContext = mock(InitializationContext.class);
    when(mockRuntimeContext.getRegistries()).thenReturn(registries);
    when(mockRuntimeContext.getMetaData()).thenReturn(metaData);
    return mockRuntimeContext;
  }

  private Meta createImportMeta(String pathCode) {
    return Meta.newBuilder()
        .putEntries(
            ImportInfo.getDescriptor().getFullName(),
            Any.pack(ImportInfo.newBuilder().setPathCode(pathCode).build()))
        .build();
  }

  private void mockRuntimeContextEvaluateConstStr(InitializationContext context) {
    doAnswer(
            args -> {
              Import importMsg = args.getArgument(0);
              ValueSource valueSource = importMsg.getValue();
              if (!valueSource.getConstString().isEmpty()) {
                return new DefaultPrimitive(valueSource.getConstString());
              }
              throw new IllegalArgumentException(
                  String.format("Undefined mocking for evaluating a ValueSource %s", valueSource));
            })
        .when(context)
        .evaluateImport(any(), any());
  }

  @Test
  public void processImports_noImports_noop() throws IOException {
    PipelineConfig config = PipelineConfig.newBuilder().setPackageName("myPackage").build();

    Registry<Loader> lr = mock(TestLoaderRegistry.class);
    Registry<Parser> pr = mock(TestParserRegistry.class);
    InitializationContext rtx = mock(InitializationContext.class);
    when(rtx.getMetaData()).thenReturn(mock(MetaData.class));

    new DefaultImportProcessor(rootPath.getAbsPath())
        .processImports(rootPath, rtx, config);

    // No imports means the registry should never have been even accessed.
    verify(lr, never()).get(anyString());
    verify(pr, never()).get(anyString());
  }

  @Test
  public void processImports_orderedImports_processedInOrder() throws IOException {
    // Import one then two.
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(
                        ValueSource.newBuilder()
                            .setConstString("/root/project/x/y/one.bin")
                            .build())
                    .setMeta(createImportMeta("/root/project/x/y/one.bin"))
                    .build())
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("./two.bin").build())
                    .setMeta(createImportMeta("./two.bin"))
                    .build())
            .setPackageName("myPackage")
            .build();
    byte[] one = new byte[] {1};
    byte[] two = new byte[] {2};
    MemoryLoader memLoader = new MemoryLoader();
    memLoader.registerFile("/root/project/x/y/one.bin", one);
    memLoader.registerFile("/root/project/two.bin", two);

    InOrder inOrder = Mockito.inOrder(mockParser);

    Registry<Loader> lr = initLoaderReg(memLoader);
    Registry<Parser> pr = initParserReg(mockParser);
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    new DefaultImportProcessor(rootPath.getAbsPath()).processImports(rootPath, rtx, config);

    inOrder
        .verify(mockParser)
        .parse(eq(one), any(), any(), any(), ImportPathUtil.absPath("/root/project/x/y/one.bin"));
    inOrder
        .verify(mockParser)
        .parse(eq(two), any(), any(), any(), ImportPathUtil.absPath("/root/project/two.bin"));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void processImports_deepImports_processedDepthFirst() throws IOException {
    PipelineConfig oneProto =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("x/two.proto").build())
                    .setMeta(createImportMeta("x/two.proto"))
                    .build())
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("y/four.bin").build())
                    .setMeta(createImportMeta("y/four.bin"))
                    .build())
            .setPackageName("myPackage")
            .build();

    PipelineConfig twoProto =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("./three.bin"))
                    .setMeta(createImportMeta("./three.bin"))
                    .build())
            .setPackageName("myPackage")
            .build();

    byte[] two = twoProto.toByteArray();
    byte[] three = new byte[] {3};
    byte[] four = new byte[] {4};
    MemoryLoader memLoader = new MemoryLoader();
    memLoader.registerFile("/root/project/x/two.proto", two);
    memLoader.registerFile("/root/project/x/three.bin", three);
    memLoader.registerFile("/root/project/y/four.bin", four);

    // Pretend two imports three, by adding a hook to our mockParser.
    // When mockParser processes "file" two, it will call
    // ImportProcessor.processImports(..., twoProto), and twoProto specifies an import for three.bin
    // Thus, when two is parsed, three should be processed next, then four last.
    doAnswer(
            args -> {
              InitializationContext rtx =
                  initRuntimeContext(args.getArgument(1), args.getArgument(2));
              mockRuntimeContextEvaluateConstStr(rtx);
              ((ImportProcessor) args.getArgument(3))
                  .processImports(args.getArgument(4), rtx, twoProto);
              return null;
            })
        .when(mockParser)
        .parse(eq(two), any(), any(), any(), any());

    InOrder inOrder = Mockito.inOrder(mockParser);

    Registry<Loader> lr = initLoaderReg(memLoader);
    Registry<Parser> pr = initParserReg(mockParser);
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    new DefaultImportProcessor(rootPath.getAbsPath()).processImports(rootPath, rtx, oneProto);

    inOrder
        .verify(mockParser)
        .parse(eq(two), any(), any(), any(), ImportPathUtil.absPath("/root/project/x/two.proto"));
    inOrder
        .verify(mockParser)
        .parse(eq(three), any(), any(), any(), ImportPathUtil.absPath("/root/project/x/three.bin"));
    inOrder
        .verify(mockParser)
        .parse(eq(four), any(), any(), any(), ImportPathUtil.absPath("/root/project/y/four.bin"));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void processImports_deepCyclicalImports_ignoresDupes() throws IOException {
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("x/one.proto").build())
                    .setMeta(createImportMeta("x/one.proto"))
                    .build())
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("y/two.bin").build())
                    .setMeta(createImportMeta("y/two.bin"))
                    .build())
            .setPackageName("myPackage")
            .build();

    byte[] one = new byte[] {1};
    byte[] two = new byte[] {2};
    MemoryLoader memLoader = new MemoryLoader();
    memLoader.registerFile("/root/project/x/one.proto", one);
    memLoader.registerFile("/root/project/y/two.bin", two);

    // Pretend one imports config again
    doAnswer(
            i -> {
              InitializationContext rtx = initRuntimeContext(i.getArgument(1), i.getArgument(2));
              mockRuntimeContextEvaluateConstStr(rtx);

              ((ImportProcessor) i.getArgument(3)).processImports(i.getArgument(4), rtx, config);
              return null;
            })
        .when(mockParser)
        .parse(eq(one), any(), any(), any(), any());

    InOrder inOrder = Mockito.inOrder(mockParser);

    Registry<Loader> lr = initLoaderReg(memLoader);
    Registry<Parser> pr = initParserReg(mockParser);
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    new DefaultImportProcessor(rootPath.getAbsPath()).processImports(rootPath, rtx, config);

    inOrder
        .verify(mockParser, times(1))
        .canParse(ImportPathUtil.absPath("/root/project/x/one.proto"));
    inOrder
        .verify(mockParser, times(1))
        .parse(eq(one), any(), any(), any(), ImportPathUtil.absPath("/root/project/x/one.proto"));
    inOrder
        .verify(mockParser, times(1))
        .canParse(ImportPathUtil.absPath("/root/project/y/two.bin"));
    inOrder
        .verify(mockParser, times(1))
        .parse(eq(two), any(), any(), any(), ImportPathUtil.absPath("/root/project/y/two.bin"));
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void processImports_flatDuplicateImports_ignoresDupes() throws IOException {
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("x/one.proto").build())
                    .setMeta(createImportMeta("x/one.proto"))
                    .build())
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("y/two.bin").build())
                    .setMeta(createImportMeta("y/two.bin"))
                    .build())
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("x/one.proto").build())
                    .setMeta(createImportMeta("x/one.proto"))
                    .build())
            .setPackageName("myPackage")
            .build();

    byte[] one = new byte[] {1};
    byte[] two = new byte[] {2};
    MemoryLoader memLoader = new MemoryLoader();
    memLoader.registerFile("/root/project/x/one.proto", one);
    memLoader.registerFile("/root/project/y/two.bin", two);

    InOrder inOrder = Mockito.inOrder(mockParser);

    Registry<Loader> lr = initLoaderReg(memLoader);
    Registry<Parser> pr = initParserReg(mockParser);
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    new DefaultImportProcessor(rootPath.getAbsPath()).processImports(rootPath, rtx, config);

    inOrder
        .verify(mockParser, times(1))
        .canParse(ImportPathUtil.absPath("/root/project/x/one.proto"));
    inOrder
        .verify(mockParser, times(1))
        .parse(eq(one), any(), any(), any(), ImportPathUtil.absPath("/root/project/x/one.proto"));
    inOrder
        .verify(mockParser, times(1))
        .canParse(ImportPathUtil.absPath("/root/project/y/two.bin"));
    inOrder
        .verify(mockParser, times(1))
        .parse(eq(two), any(), any(), any(), ImportPathUtil.absPath("/root/project/y/two.bin"));
    inOrder.verifyNoMoreInteractions();
  }



  @Test
  public void processImports_classNotFound_throws() {
    String classPath = "com.this.does.not.exist.NotReal";
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(
                        ValueSource.newBuilder().setConstString("class://" + classPath).build())
                    .setMeta(createImportMeta("class://" + classPath))
                    .build())
            .setPackageName("myPackage")
            .build();

    Loader pluginClassLoader = new PluginClassLoader();
    Parser pluginClassParser = new PluginClassParser();

    Registry<Loader> lr = initLoaderReg(pluginClassLoader);
    Registry<Parser> pr = initParserReg(pluginClassParser);
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    DefaultImportProcessor defaultImportProcessor =
        new DefaultImportProcessor(rootPath.getAbsPath());

    Exception got =
        assertThrows(
            ImportException.class,
            () -> defaultImportProcessor.processImports(rootPath, rtx, config));
    assertThat(got).hasMessageThat().contains("Error processing import class:///" + classPath);
  }

  @Test
  public void processImports_classNotFound_noImportSourceMetaKey() throws IOException {
    String classPath = "com.this.does.not.exist.NotReal";
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(
                        ValueSource.newBuilder().setConstString("class://" + classPath).build())
                    .setMeta(createImportMeta("class://" + classPath))
                    .build())
            .setPackageName("myPackage")
            .build();

    Loader pluginClassLoader = new PluginClassLoader();
    Parser pluginClassParser = new PluginClassParser();

    Registry<Loader> lr = initLoaderReg(pluginClassLoader);
    Registry<Parser> pr = initParserReg(pluginClassParser);
    MetaData meta = new DefaultMetaData();
    meta.setMeta(IMPORT_EXCEPTION_LIST_KEY, new ArrayList<ImportException>());
    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), meta);
    mockRuntimeContextEvaluateConstStr(rtx);

    DefaultImportProcessor defaultImportProcessor =
        new DefaultImportProcessor(rootPath.getAbsPath());

    defaultImportProcessor.processImports(rootPath, rtx, config);
    List<ImportException> importExceptions = rtx.getMetaData().getMeta(IMPORT_EXCEPTION_LIST_KEY);
    // Check if the source for the only ImportException which gets added uses
    // Source.getDefaultInstance() and SourcePosition.getDefaultInstance() since we dont set
    // source metadata for the import.
    assertThat(importExceptions).hasSize(1);

    Source importSource = importExceptions.get(0).getSource();
    assertThat(importSource.hasStart()).isFalse();
    assertThat(importSource.hasEnd()).isFalse();
    // Check whether fetching getStart and getEnd returns the default SourcePosition values.
    SourcePosition start = importSource.getStart();
    SourcePosition end = importSource.getStart();

    assertThat(start.getLine()).isEqualTo(0);
    assertThat(start.getColumn()).isEqualTo(0);
    assertThat(end.getLine()).isEqualTo(0);
    assertThat(end.getColumn()).isEqualTo(0);
  }

  @Test
  public void processImports_dynamicImports_processedInOrder() throws IOException {
    Import funcImport =
        Import.newBuilder()
            .setValue(
                ValueSource.newBuilder()
                    .setFunctionCall(
                        FunctionCall.newBuilder()
                            .setReference(
                                FunctionReference.newBuilder().setName("path").setPackage("test"))
                            .build())
                    .build())
            .setMeta(createImportMeta("path()"))
            .build();
    PipelineConfig proto =
        PipelineConfig.newBuilder()
            .addImports(
                Import.newBuilder()
                    .setValue(
                        ValueSource.newBuilder().setConstString("/root/project/two.proto").build())
                    .setMeta(createImportMeta("/root/project/two.proto"))
                    .build())
            .addImports(funcImport)
            .setPackageName("test")
            .build();
    byte[] two = new byte[] {2};
    byte[] three = new byte[] {3};
    MemoryLoader memLoader = new MemoryLoader();
    memLoader.registerFile("/root/project/two.proto", two);
    memLoader.registerFile("/root/project/three.proto", three);

    Registry<Loader> lr = initLoaderReg(memLoader);
    Registry<Parser> pr = initParserReg(mockParser);

    InitializationContext rtx = initRuntimeContext(initRegistries(lr, pr), mock(MetaData.class));
    mockRuntimeContextEvaluateConstStr(rtx);

    doAnswer(
            args -> {
              // Updates the mocked `RuntimeContext` here to simulate that the function `path()` is
              // defined and ready to be evaluated only if `/root/project/two.proto` is imported.
              doAnswer(i -> new DefaultPrimitive("/root/project/three.proto"))
                  .when(rtx)
                  .evaluateImport(eq(funcImport), any());
              return null;
            })
        .when(mockParser)
        .parse(eq(two), any(), any(), any(), any());

    InOrder inOrder = Mockito.inOrder(mockParser);

    new DefaultImportProcessor(rootPath.getAbsPath()).processImports(rootPath, rtx, proto);

    inOrder
        .verify(mockParser)
        .parse(eq(two), any(), any(), any(), ImportPathUtil.absPath("/root/project/two.proto"));
    inOrder
        .verify(mockParser)
        .parse(eq(three), any(), any(), any(), ImportPathUtil.absPath("/root/project/three.proto"));
    inOrder.verifyNoMoreInteractions();
  }
}
