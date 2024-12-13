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

package com.google.cloud.verticals.foundations.dataharmonization.function;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure.FreeParameter;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource.SourceCase;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.Var;
import java.util.List;
import java.util.function.BiFunction;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

/** Tests for FunctionCall. */
@RunWith(JUnit4.class)
public class DefaultClosureTest {

  private static Double[] freeArgIndices(DefaultClosure closure) {
    return closure.getField(DefaultClosure.FREE_ARG_INDICES).asArray().stream()
        .map(p -> p.asPrimitive().num())
        .toArray(Double[]::new);
  }

  @Test
  public void create_freeParams_createdAsFreeParams() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("test"))
            .addArgs(ValueSource.newBuilder().setFreeParameter("name"))
            .build();

    RuntimeContext context = mock(RuntimeContext.class);
    when(context.getDataTypeImplementation()).thenReturn(testDTI());

    DefaultClosure actual = DefaultClosure.create(context, proto);

    assertThat(actual.getArgs()).hasLength(1);
    assertThat(freeArgIndices(actual)).hasLength(1);
    assertEquals(new Closure.FreeParameter("name"), actual.getArgs()[0]);
  }

  @Test
  public void fields_hasKeys() {
    DefaultClosure closure = DefaultClosure.create(new DefaultClosure.FunctionReference("test"));

    assertEquals(
        ImmutableSet.of(
            DefaultClosure.FUNCTION_REF, DefaultClosure.ARGS, DefaultClosure.FREE_ARG_INDICES),
        closure.fields());
  }

  @Test
  public void create_functionCall_createsRecursively() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("outer"))
            .addArgs(
                ValueSource.newBuilder()
                    .setFunctionCall(
                        Pipeline.FunctionCall.newBuilder()
                            .setBuildClosure(true)
                            .setReference(
                                FunctionReference.newBuilder().setName("inner").setPackage("test"))
                            .addArgs(ValueSource.newBuilder().setConstString("inner arg"))))
            .addArgs(ValueSource.newBuilder().setConstString("outer arg"))
            .build();

    RuntimeContext context = mock(RuntimeContext.class);
    when(context.evaluate(ArgumentMatchers.any(ValueSource.class)))
        .then(
            invocation -> {
              ValueSource vs = invocation.getArgument(0);
              if (vs.getSourceCase() != SourceCase.CONST_STRING) {
                throw new UnsupportedOperationException(
                    String.format("Expected ValueSource with string, got %s", vs));
              }

              Primitive m = mock(Primitive.class, Answers.CALLS_REAL_METHODS);
              when(m.string()).thenReturn(vs.getConstString());
              when(m.bool()).thenReturn(null);
              when(m.num()).thenReturn(null);
              return m;
            });

    DefaultClosure actual = DefaultClosure.create(context, proto);
    assertEquals("outer", actual.getFunctionRef().getFunctionName());

    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    DefaultClosure.FUNCTION_REF,
                    testDTI()
                        .containerOf(
                            ImmutableMap.of(
                                DefaultClosure.FunctionReference.FUNCTION_NAME,
                                testDTI().primitiveOf("outer"),
                                DefaultClosure.FunctionReference.PACKAGE_NAME,
                                NullData.instance)),
                    DefaultClosure.ARGS,
                    testDTI()
                        .arrayOf(
                            ImmutableList.of(
                                testDTI()
                                    .containerOf(
                                        ImmutableMap.of(
                                            DefaultClosure.FUNCTION_REF,
                                            testDTI()
                                                .containerOf(
                                                    ImmutableMap.of(
                                                        DefaultClosure.FunctionReference
                                                                .FUNCTION_NAME,
                                                            testDTI().primitiveOf("inner"),
                                                        DefaultClosure.FunctionReference
                                                                .PACKAGE_NAME,
                                                            testDTI().primitiveOf("test"))),
                                            DefaultClosure.ARGS,
                                            testDTI()
                                                .arrayOf(
                                                    ImmutableList.of(
                                                        testDTI().primitiveOf("inner arg"))),
                                            DefaultClosure.FREE_ARG_INDICES,
                                            testDTI().emptyArray())),
                                testDTI().primitiveOf("outer arg"))),
                    DefaultClosure.FREE_ARG_INDICES,
                    testDTI().emptyArray()));

    assertDCAPEquals(expected, actual);
  }

  @Test
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void bindNextFreeParameter_removesFromList() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("test"))
            .addArgs(ValueSource.newBuilder().setConstString("one"))
            .addArgs(ValueSource.newBuilder().setFreeParameter("name1"))
            .addArgs(ValueSource.newBuilder().setConstString("two"))
            .addArgs(ValueSource.newBuilder().setFreeParameter("name2"))
            .build();

    RuntimeContext context = mock(RuntimeContext.class);
    when(context.getDataTypeImplementation()).thenReturn(testDTI());

    DefaultClosure fn = DefaultClosure.create(context, proto);

    assertThat(fn.getArgs()).hasLength(4);
    assertEquals(2, fn.getField(DefaultClosure.FREE_ARG_INDICES).asArray().size());
    assertArrayEquals(new Double[] {1., 3.}, freeArgIndices(fn));
    assertEquals(new FreeParameter("name1"), fn.getArgs()[1]);
    assertEquals(new FreeParameter("name2"), fn.getArgs()[3]);

    Data newValue1 = mock(Data.class);
    Data newValue2 = mock(Data.class);

    @Var DefaultClosure boundFn = fn.bindNextFreeParameter(newValue1);
    assertArrayEquals(new Double[] {1., 3.}, freeArgIndices(fn));
    assertArrayEquals(new Double[] {3.}, freeArgIndices(boundFn));

    boundFn = boundFn.bindNextFreeParameter(newValue2);
    assertThat(freeArgIndices(boundFn)).isEmpty();
    assertEquals(newValue1, boundFn.getArgs()[1]);
    assertEquals(newValue2, boundFn.getArgs()[3]);

    // Ensure original function call is unchanged.
    assertDCAPEquals(new FreeParameter("name1"), fn.getArgs()[1]);
    assertDCAPEquals(new FreeParameter("name2"), fn.getArgs()[3]);
  }

  @Test
  public void bindNextFreeParameter_noFreeParams_throws() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("test"))
            .addArgs(ValueSource.newBuilder().setConstString("one"))
            .addArgs(ValueSource.newBuilder().setConstString("two"))
            .build();

    RuntimeContext context = mock(RuntimeContext.class);

    Closure fn = DefaultClosure.create(context, proto);
    assertThrows(
        UnsupportedOperationException.class, () -> fn.bindNextFreeParameter(mock(Data.class)));
  }

  @Test
  public void execute_throwsWithFreeParams() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("test"))
            .addArgs(ValueSource.newBuilder().setFreeParameter("name"))
            .build();

    RuntimeContext context = mock(RuntimeContext.class);
    when(context.getDataTypeImplementation()).thenReturn(testDTI());
    when(context.top()).thenAnswer(Answers.RETURNS_MOCKS);

    Closure fn = DefaultClosure.create(context, proto);
    assertThrows(UnsupportedOperationException.class, () -> fn.execute(context));
  }

  @Test
  @SuppressWarnings(
      "unchecked") // This test uses some mocks and casts arguments from a reflection call.
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void execute_integratesContextComponents() {
    Pipeline.FunctionCall proto =
        Pipeline.FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("test").setPackage("pack"))
            .addArgs(ValueSource.newBuilder().setConstString("hello"))
            .build();

    Data evaluatedHello = mock(Data.class);
    Data wantResult = mock(Data.class);

    // Mock up function.
    CallableFunction function = mock(CallableFunction.class);
    when(function.call(any(), any(Data[].class))).thenReturn(wantResult);

    // Mock up registry.
    TestFunctionPackageRegistry registry = mock(TestFunctionPackageRegistry.class);
    when(registry.getOverloads(anySet(), anyString())).thenReturn(ImmutableSet.of(function));

    // Mock up overload selector.
    OverloadSelector selector = mock(DefaultOverloadSelector.class);
    when(selector.select(anyList(), any())).thenReturn(function);

    // Mock up context integrating all the above.
    RuntimeContext context = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    Registries registries = mock(Registries.class);
    when(context.getRegistries()).thenReturn(registries);
    when(context.wrap(any(), any(), any()))
        .then(
            inv ->
                ((BiFunction<RuntimeContext, Data[], Data>) inv.getArgument(2))
                    .apply(context, inv.getArgument(1)));
    when(context.evaluate(any())).thenReturn(evaluatedHello);
    when(context.getRegistries().getFunctionRegistry(proto.getReference().getPackage()))
        .thenReturn(registry);
    when(context.getCurrentPackageContext()).thenReturn(new PackageContext(ImmutableSet.of()));
    when(context.getOverloadSelector()).thenReturn(selector);

    // Create and execute the Closure.
    Closure fn = DefaultClosure.create(context, proto);
    Data gotResult = fn.execute(context);

    // Make sure all the above components got called correctly.
    verify(registry).getOverloads(ImmutableSet.of("pack"), "test");
    verify(selector).select(ImmutableList.of(function), new Data[] {evaluatedHello});
    verify(function).call(context, evaluatedHello);

    // Make sure the result made it back.
    assertEquals(wantResult, gotResult);
  }

  @Test
  @SuppressWarnings(
      "unchecked") // This test uses some mocks and casts arguments from a reflection call.
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void execute_callFunctionFromAllPackages() {
    FunctionCall proto =
        FunctionCall.newBuilder()
            .setReference(
                FunctionReference.newBuilder()
                    .setName("test")
                    .setPackage(DefaultClosure.FunctionReference.WILDCARD_PACKAGE_NAME)
                    .build())
            .addArgs(ValueSource.newBuilder().setConstString("arg").build())
            .build();

    Data wantResult = mock(Data.class);

    // Mock up function.
    CallableFunction function = mock(CallableFunction.class);
    when(function.call(any(), any(Data[].class))).thenReturn(wantResult);
    when(function.getName()).thenReturn(proto.getReference().getName());

    final String packageName = "google";
    // Create a package registry.
    PackageRegistry<CallableFunction> packageRegistry = new DefaultPackageRegistry<>();
    packageRegistry.register(packageName, function);

    // Mock up registries.
    Registries registries = mock(Registries.class);
    when(registries.getFunctionRegistry(packageName)).thenReturn(packageRegistry);
    when(registries.getAllRegisteredPackages()).thenReturn(ImmutableSet.of(packageName));

    // Mock up overload selector.
    OverloadSelector selector = mock(DefaultOverloadSelector.class);
    when(selector.select(anyList(), any()))
        .then(
            args -> {
              List<CallableFunction> overloads = args.getArgument(0);
              assertThat(overloads).contains(function);
              return function;
            });

    Data args = mock(Data.class);

    // Mock up runtime context.
    RuntimeContext context = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    when(context.getRegistries()).thenReturn(registries);
    when(context.getCurrentPackageContext()).thenReturn(new PackageContext(ImmutableSet.of()));
    when(context.getOverloadSelector()).thenReturn(selector);
    when(context.evaluate(any())).thenReturn(args);
    when(context.wrap(any(), any(), any()))
        .then(
            inv ->
                ((BiFunction<RuntimeContext, Data[], Data>) inv.getArgument(2))
                    .apply(context, inv.getArgument(1)));

    // Create and execute the Closure.
    Closure fn = DefaultClosure.create(context, proto);
    Data gotResult = fn.execute(context);

    // Make sure all the above components got called correctly.
    verify(registries).getFunctionRegistry(packageName);
    verify(selector).select(ImmutableList.of(function), new Data[] {args});
    verify(function).call(context, args);

    // Make sure the result is correct.
    assertEquals(wantResult, gotResult);
  }

  @Test
  @SuppressWarnings(
      "unchecked") // This test uses some mocks and casts arguments from a reflection call.
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void execute_multiFunctionsFromAllPackages() {
    // Create a function call with two arguments.
    ValueSource protoArgZero = ValueSource.newBuilder().setConstString("argZero").build();
    ValueSource protoArgOne = ValueSource.newBuilder().setConstString("argOne").build();
    FunctionCall proto =
        FunctionCall.newBuilder()
            .setReference(
                FunctionReference.newBuilder()
                    .setName("test")
                    .setPackage(DefaultClosure.FunctionReference.WILDCARD_PACKAGE_NAME)
                    .build())
            .addArgs(protoArgZero)
            .addArgs(protoArgOne)
            .build();

    Data wantOneArgFuncResult = mock(Data.class);
    Data wantTwoArgsFuncResult = mock(Data.class);

    // Mock up function.
    CallableFunction oneArgFunc = mock(CallableFunction.class);
    CallableFunction twoArgsFunc = mock(CallableFunction.class);
    when(oneArgFunc.call(any(RuntimeContext.class), any(Data[].class)))
        .thenReturn(wantOneArgFuncResult);
    when(twoArgsFunc.call(any(RuntimeContext.class), any(Data[].class)))
        .thenReturn(wantTwoArgsFuncResult);
    when(oneArgFunc.getName()).thenReturn(proto.getReference().getName());
    when(twoArgsFunc.getName()).thenReturn(proto.getReference().getName());

    // Create package registries.
    PackageRegistry<CallableFunction> packageOneRegistry = new DefaultPackageRegistry<>();
    packageOneRegistry.register("one", oneArgFunc);
    PackageRegistry<CallableFunction> packageTwoRegistry = new DefaultPackageRegistry<>();
    packageTwoRegistry.register("two", twoArgsFunc);

    // Mock up registries.
    Registries registries = mock(Registries.class);
    when(registries.getFunctionRegistry("one")).thenReturn(packageOneRegistry);
    when(registries.getFunctionRegistry("two")).thenReturn(packageTwoRegistry);
    when(registries.getAllRegisteredPackages()).thenReturn(ImmutableSet.of("one", "two"));

    // Mock up overload selector.
    OverloadSelector selector = mock(DefaultOverloadSelector.class);
    when(selector.select(anyList(), any()))
        .then(
            args -> {
              List<CallableFunction> overloads = args.getArgument(0);
              assertThat(overloads).contains(oneArgFunc);
              assertThat(overloads).contains(twoArgsFunc);
              Data[] funcArgs = args.getArgument(1);
              if (funcArgs.length == 1) {
                return oneArgFunc;
              } else if (funcArgs.length == 2) {
                return twoArgsFunc;
              }
              fail();
              return null;
            });

    Data argZero = mock(Data.class);
    Data argOne = mock(Data.class);

    // Mock up runtime context.
    RuntimeContext context = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    when(context.getRegistries()).thenReturn(registries);
    when(context.getCurrentPackageContext()).thenReturn(new PackageContext(ImmutableSet.of()));
    when(context.getOverloadSelector()).thenReturn(selector);
    when(context.evaluate(eq(protoArgZero))).thenReturn(argZero);
    when(context.evaluate(eq(protoArgOne))).thenReturn(argOne);
    when(context.wrap(any(), any(), any()))
        .then(
            inv ->
                ((BiFunction<RuntimeContext, Data[], Data>) inv.getArgument(2))
                    .apply(context, inv.getArgument(1)));

    // Create and execute the Closure.
    Closure fn = DefaultClosure.create(context, proto);
    Data gotResult = fn.execute(context);

    // Make sure all the above components got called correctly.
    verify(registries).getFunctionRegistry("one");
    verify(registries).getFunctionRegistry("two");
    ArgumentCaptor<List<CallableFunction>> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(selector).select(argumentCaptor.capture(), eq(new Data[] {argZero, argOne}));
    assertThat(argumentCaptor.getValue()).contains(oneArgFunc);
    assertThat(argumentCaptor.getValue()).contains(twoArgsFunc);
    verify(twoArgsFunc).call(context, new Data[] {argZero, argOne});

    // Make sure the result is correct.
    assertEquals(wantTwoArgsFuncResult, gotResult);
  }
}
