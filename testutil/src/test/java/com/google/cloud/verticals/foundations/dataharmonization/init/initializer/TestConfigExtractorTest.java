/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.init.initializer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Import;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import java.io.IOException;
import java.nio.file.FileSystems;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentMatcher;

/** Test for {@link TestConfigExtractor}. */
@RunWith(JUnit4.class)
public class TestConfigExtractorTest {

  @Test
  public void initWithPipelineConfig_returnSame() throws IOException {
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName("testPkg")
            .addImports(
                Import.newBuilder()
                    .setValue(ValueSource.newBuilder().setConstString("test").build())
                    .build())
            .build();
    TestConfigExtractor t = TestConfigExtractor.of(config);
    PipelineConfig actual =
        t.getParser(mock(Registries.class), mock(ImportProcessor.class))
            .parseConfig(
                t.getFileContent(mock(Registries.class)),
                mock(Registries.class),
                mock(MetaData.class),
                mock(ImportProcessor.class),
                ImportPath.of(
                    "test",
                    FileSystems.getDefault().getPath("test"),
                    FileSystems.getDefault().getPath("test")));
    assertEquals(config, actual);
  }

  @Test
  public void initPipelineConfig_registersFunction() throws IOException {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();
    PackageRegistry<CallableFunction> mockFuncReg = mock(TestFunctionPackageRegistry.class);
    Registries mockRegs = mock(Registries.class);
    when(mockRegs.getFunctionRegistry(testPkgName)).thenReturn(mockFuncReg);
    TestConfigExtractor t = TestConfigExtractor.of(config);
    t.getParser(mockRegs, mock(ImportProcessor.class))
        .parseConfig(
            t.getFileContent(mockRegs),
            mockRegs,
            mock(MetaData.class),
            mock(ImportProcessor.class),
            ImportPath.of(
                "", FileSystems.getDefault().getPath(""), FileSystems.getDefault().getPath("")));
    verify(mockFuncReg)
        .register(
            eq(testPkgName),
            argThat(
                new ArgumentMatcher<CallableFunction>() {
                  @Override
                  public boolean matches(CallableFunction argument) {
                    return argument.getName().equals(testFuncName);
                  }
                }));
  }
}
