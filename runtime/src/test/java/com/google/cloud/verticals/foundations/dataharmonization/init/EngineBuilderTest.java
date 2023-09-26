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

package com.google.cloud.verticals.foundations.dataharmonization.init;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ConfigExtractorBase;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.TestConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.common.collect.ImmutableList;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for {@link Engine.Builder} */
@RunWith(JUnit4.class)
public class EngineBuilderTest {

  @Test
  public void testRegisterFromDefaultPlugins() throws IOException {

    String testPluginName = "testPlugin";
    String testLoaderName = "mockLoader";
    CallableFunction mockFunc = mock(CallableFunction.class);
    when(mockFunc.getName()).thenReturn("mockPluginFunc");
    Loader mockLoader = mock(Loader.class);
    when(mockLoader.getName()).thenReturn(testLoaderName);
    Plugin defaultPlugin =
        new Plugin() {

          @Override
          public ImmutableList<CallableFunction> getFunctions() {
            return ImmutableList.of(mockFunc);
          }

          @Override
          public ImmutableList<Loader> getLoaders() {
            return ImmutableList.of(mockLoader);
          }

          @Override
          public String getPackageName() {
            return testPluginName;
          }
        };
    // inject empty data into the Engine Builders to test if function from default plugin is
    // registered.
    ConfigExtractorBase placeHolder = TestConfigExtractor.of(PipelineConfig.getDefaultInstance());
    RuntimeContext initedContext =
        new Engine.Builder(placeHolder)
            .withDefaultPlugins(defaultPlugin)
            .initialize()
            .build()
            .getRuntimeContext();
    assertThat(initedContext.getRegistries().getFunctionRegistry(testPluginName).getAll())
        .contains(mockFunc);
    Assert.assertEquals(
        mockLoader, initedContext.getRegistries().getLoaderRegistry().get(testLoaderName));
  }

  @Test
  public void testInitializeWith_registerFuncs() throws IOException {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";

    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();

    RuntimeContext initedContext =
        new Engine.Builder(TestConfigExtractor.of(config))
            .initialize()
            .build()
            .getRuntimeContext();
    Set<CallableFunction> registeredFunc =
        initedContext.getRegistries().getFunctionRegistry(testPkgName).getAll();
    Assert.assertTrue(registeredFunc.stream().anyMatch(f -> f.getName().equals(testFuncName)));
  }

  @Test
  public void testBuild_multipleEngines() throws IOException {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";

    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();

    Engine.InitializedBuilder builder =
        new Engine.Builder(TestConfigExtractor.of(config)).initialize();
    Engine engine1 = builder.build();
    Engine engine2 = builder.build();

    Assert.assertNotSame(engine1, engine2);
    Assert.assertNotSame(
        engine1.getRuntimeContext().getMetaData(), engine2.getRuntimeContext().getMetaData());
    Assert.assertSame(
        engine1.getRuntimeContext().getRegistries(), engine2.getRuntimeContext().getRegistries());
    Assert.assertSame(
        engine1.getRuntimeContext().getCurrentPackageContext(),
        engine2.getRuntimeContext().getCurrentPackageContext());
    Assert.assertSame(
        engine1.getRuntimeContext().getImportProcessor(),
        engine2.getRuntimeContext().getImportProcessor());
  }

  @Test
  public void build_withWrappers_hasWrappedContext() throws IOException {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";

    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();

    Engine.InitializedBuilder builder =
        new Engine.Builder(TestConfigExtractor.of(config))
            .withWrapper(TestWrapper::new)
            .initialize();

    RuntimeContext got = builder.build().getRuntimeContext();
    assertThat(got).isInstanceOf(TestWrapper.class);
  }

  @Test
  public void test_getMainConfigProto() throws IOException {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";

    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();

    Engine.InitializedBuilder builder1 =
        new Engine.Builder(TestConfigExtractor.of(config)).initialize();
    Engine.InitializedBuilder builder2 =
        new Engine.Builder(TestConfigExtractor.of(config)).initialize();

    Assert.assertEquals(builder1.getMainConfigProto(), builder2.getMainConfigProto());
  }

  @Test
  public void test_serializeGetMainConfigProto() throws Exception {
    String testPkgName = "testPkg";
    String testFuncName = "testFunc";

    PipelineConfig config =
        PipelineConfig.newBuilder()
            .setPackageName(testPkgName)
            .addFunctions(FunctionDefinition.newBuilder().setName(testFuncName).build())
            .build();

    Engine.InitializedBuilder builder1 =
        new Engine.Builder(TestConfigExtractor.of(config)).initialize();

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(builder1);
    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    Engine.InitializedBuilder builder2 = (Engine.InitializedBuilder) in.readObject();

    Assert.assertEquals(builder1.getMainConfigProto(), builder2.getMainConfigProto());
  }

  private static class TestWrapper extends WrapperContext<TestWrapper> {

    public TestWrapper(RuntimeContext innerContext) {
      super(innerContext, TestWrapper.class);
    }

    @Override
    protected TestWrapper rewrap(RuntimeContext innerContext) {
      return new TestWrapper(innerContext);
    }
  }
}
