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

package com.google.cloud.verticals.foundations.dataharmonization.utils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultDataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultMetaData;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.SideTarget;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.VarTarget;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.VarTarget.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestTargetPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.common.collect.ImmutableSet;
import java.nio.file.Path;
import java.util.Map;
import org.mockito.Answers;
import org.mockito.Mockito;

/** Utilities for mocking up RuntimeContexts and stack frames. */
public final class RuntimeContextUtil {

  public static RuntimeContext mockRuntimeContextWithMockedMetaData() {
    MetaData defaultMetadata = mock(MetaData.class);
    RuntimeContext context =
        mock(
            RuntimeContext.class,
            Mockito.withSettings().serializable().defaultAnswer(Answers.RETURNS_MOCKS));
    when(context.getMetaData()).thenReturn(defaultMetadata);
    return context;
  }

  /** Creates a mock RuntimeContext that is capable of storing metadata. */
  public static RuntimeContext mockRuntimeContextWithDefaultMetaData() {
    MetaData metaData = new DefaultMetaData();
    RuntimeContext context = mock(RuntimeContext.class);
    when(context.getMetaData()).thenReturn(metaData);
    return context;
  }

  /**
   * Create a mock RuntimeContext that is capable of storing vars.
   *
   * @param varStorage the map to store vars in.
   */
  public static RuntimeContext mockVarCapableRuntimeContext(final Map<String, Data> varStorage) {
    return mockVarCapableRuntimeContext(null, mockStackFrame(varStorage));
  }

  public static RuntimeContext mockVarCapableRuntimeContext(StackFrame bottom, StackFrame top) {
    RuntimeContext context = mockRuntimeContextWithRegistry();
    when(context.top()).thenReturn(top);
    when(context.bottom()).thenReturn(bottom);
    when(context.evaluate(argThat(vs -> vs.getConstString().length() > 0)))
        .then(
            i ->
                DefaultDataTypeImplementation.instance.primitiveOf(
                    ((ValueSource) i.getArgument(0)).getConstString()));
    return context;
  }

  public static StackFrame mockStackFrame(final Map<String, Data> varStorage) {
    StackFrame sf = mock(StackFrame.class);
    doAnswer(i -> varStorage.put(i.getArgument(0), i.getArgument(1))).when(sf).setVar(any(), any());
    doAnswer(i -> varStorage.getOrDefault(i.getArgument(0), NullData.instance))
        .when(sf)
        .getVar(any());
    doAnswer(i -> varStorage.keySet()).when(sf).getVars();
    return sf;
  }

  public static RuntimeContext mockRuntimeContextWithRegistry() {
    Registries mockRegistries = mock(Registries.class, Answers.RETURNS_MOCKS);
    RuntimeContext context =
        mock(
            RuntimeContext.class,
            Mockito.withSettings().serializable().defaultAnswer(Answers.RETURNS_MOCKS));

    PackageRegistry<Target.Constructor> targetPackageRegistry =
        mock(TestTargetPackageRegistry.class);
    when(targetPackageRegistry.getOverloads(
            argThat(s -> s != null && s.contains(Builtins.PACKAGE_NAME)),
            eq(Constructor.TARGET_NAME)))
        .thenReturn(ImmutableSet.of(new VarTarget.Constructor()));
    when(targetPackageRegistry.getOverloads(
            argThat(s -> s != null && s.contains(Builtins.PACKAGE_NAME)),
            eq(SideTarget.Constructor.TARGET_NAME)))
        .thenReturn(ImmutableSet.of(new SideTarget.Constructor()));
    when(mockRegistries.getTargetRegistry()).thenReturn(targetPackageRegistry);
    when(context.getMetaData()).thenReturn(new DefaultMetaData());
    when(context.getRegistries()).thenReturn(mockRegistries);
    when(context.getDataTypeImplementation()).thenReturn(new DefaultDataTypeImplementation());
    return context;
  }

  public static RuntimeContext mockSingleFunctionRuntimeContext(
      String pkg, String name, CallableFunction fn) {
    RuntimeContext rtx = mockRuntimeContextWithRegistry();
    PackageRegistry<CallableFunction> r = mock(TestFunctionPackageRegistry.class);
    when(r.getOverloads(anySet(), eq(name))).thenReturn(ImmutableSet.of(fn));
    when(rtx.getRegistries().getFunctionRegistry(pkg)).thenReturn(r);

    OverloadSelector os =
        (lst, args) -> {
          if (lst.size() == 1) {
            return lst.get(0);
          }
          throw new IllegalArgumentException("Test Overload Selector only accepts one argument");
        };
    when(rtx.getOverloadSelector()).thenReturn(os);

    return rtx;
  }

  /** Returns a realistic, mostly functional RuntimeContext. See {@link TestRuntimeContext}. */
  public static RuntimeContext testContext() {
    return testContext(Path.of("/x/y/z/test.wstl"), Path.of("/x"));
  }

  /** Returns a realistic, mostly functional RuntimeContext. See {@link TestRuntimeContext}. */
  public static RuntimeContext testContext(Path file, Path imports) {
    StackFrame sf = new DefaultStackFrame.DefaultBuilder().setName("root").build();
    return new TestRuntimeContext(
        sf,
        sf,
        new PackageContext(ImmutableSet.of(), "test", ImportPath.of("test", file, imports)));
  }

  private RuntimeContextUtil() {}
}
