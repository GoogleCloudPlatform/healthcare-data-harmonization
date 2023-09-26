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

import static com.google.cloud.verticals.foundations.dataharmonization.imports.impl.ImportPathUtil.projectFile;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Loader;
import com.google.cloud.verticals.foundations.dataharmonization.imports.Parser;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.Mockito;

/** Tests for ClassLoader. */
@RunWith(JUnit4.class)
public class PluginClassParserTest {

  @Test
  public void parse_validUrl_callsPluginLoad() throws IOException {
    String url = TestValidPlugin.class.getName();
    byte[] encoded = url.getBytes(UTF_8);

    RuntimeContext ctx = RuntimeContextUtil.mockRuntimeContextWithRegistry();
    PackageRegistry<CallableFunction> fnReg = mock(TestFunctionPackageRegistry.class);
    when(ctx.getRegistries().getFunctionRegistry(new TestValidPlugin().getPackageName()))
        .thenReturn(fnReg);

    new PluginClassParser()
        .parse(
            encoded,
            ctx.getRegistries(),
            ctx.getMetaData(),
            Mockito.mock(ImportProcessor.class, Answers.RETURNS_MOCKS),
            projectFile(url));

    // Slightly roundabout way of asserting that Plugin.load got called, as we can't mock static
    // methods.
    verify(fnReg).register("test", TestValidPlugin.testFn);
  }

  @Test
  public void parse_badConstructor_throws() throws IOException {
    String url = TestBadConstructorPlugin.class.getName();
    byte[] encoded = url.getBytes(UTF_8);
    assertThrows(
        IOException.class,
        () ->
            new PluginClassParser()
                .parse(
                    encoded,
                    mock(Registries.class),
                    mock(MetaData.class),
                    mock(ImportProcessor.class),
                    projectFile(url)));
  }

  @Test
  public void parse_badVisibility_throws() throws IOException {
    String url = TestBadVisibilityPlugin.class.getName();
    byte[] encoded = url.getBytes(UTF_8);
    assertThrows(
        IOException.class,
        () ->
            new PluginClassParser()
                .parse(
                    encoded,
                    mock(Registries.class),
                    mock(MetaData.class),
                    mock(ImportProcessor.class),
                    projectFile(url)));
  }

  @Test
  public void getName() {
    assertEquals(PluginClassParser.NAME, new PluginClassParser().getName());
  }

  /** Test Plugin class that has the wrong visibility (non-public). */
  private static class TestBadVisibilityPlugin implements Plugin {

    @Override
    public List<Loader> getLoaders() {
      return ImmutableList.of();
    }

    @Override
    public List<Parser> getParsers() {
      return ImmutableList.of();
    }

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<CallableFunction> getFunctions() {
      return ImmutableList.of();
    }

    @Override
    public List<Constructor> getTargets() {
      return ImmutableList.of();
    }
  }

  /** Test Plugin class that has a constructor with too many arguments. */
  public static class TestBadConstructorPlugin implements Plugin {

    public TestBadConstructorPlugin(String foo) {}

    @Override
    public List<Loader> getLoaders() {
      return ImmutableList.of();
    }

    @Override
    public List<Parser> getParsers() {
      return ImmutableList.of();
    }

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<CallableFunction> getFunctions() {
      return ImmutableList.of();
    }

    @Override
    public List<Constructor> getTargets() {
      return ImmutableList.of();
    }
  }

  /** Test Plugin class that is valid. */
  public static class TestValidPlugin implements Plugin {

    public static final CallableFunction testFn;

    static {
      testFn = mock(CallableFunction.class);
      when(testFn.getName()).thenReturn("myFunc");
    }

    @Override
    public List<Loader> getLoaders() {
      return ImmutableList.of();
    }

    @Override
    public List<Parser> getParsers() {
      return ImmutableList.of();
    }

    @Override
    public String getPackageName() {
      return "test";
    }

    @Override
    public List<CallableFunction> getFunctions() {
      return Collections.singletonList(testFn);
    }

    @Override
    public List<Constructor> getTargets() {
      return ImmutableList.of();
    }
  }
}
