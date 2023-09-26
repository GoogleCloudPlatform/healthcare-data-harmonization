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

package com.google.cloud.verticals.foundations.dataharmonization.function.whistle;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.containerOf;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.mockStackFrame;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.mockVarCapableRuntimeContext;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.modifier.arg.ArgModifier;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.FieldTarget.FieldType;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FieldMapping.VariableTarget;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestOptionRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestTargetPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableSet;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for WhistleFunction */
@RunWith(JUnit4.class)
public class WhistleFunctionTest {
  private static final String DEFAULT_PKG_NAME = "testPkgName";

  private static PackageContext getPlaceHolderPackageContext() {
    return new PackageContext(
        ImmutableSet.of(DEFAULT_PKG_NAME), DEFAULT_PKG_NAME, ImportPath.of(null, null, null));
  }

  @Test
  public void signature_maintainsName() {
    FunctionDefinition def = FunctionDefinition.newBuilder().setName("w00t").build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Assert.assertEquals("w00t", fn.getSignature().getName());
  }

  @Test
  public void signature_hasRightNumberOfArgs() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1"))
            .addArgs(Argument.newBuilder().setName("arg2"))
            .addArgs(Argument.newBuilder().setName("arg3"))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    assertThat(fn.getSignature().getArgs()).hasSize(def.getArgsCount());
  }

  @Test
  public void call_tooManyArgs_throws() {
    FunctionDefinition def = FunctionDefinition.newBuilder().setName("w00t").build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    assertThrows(
        IllegalArgumentException.class, () -> fn.callInternal(new TestContext(), mock(Data.class)));
  }

  @Test
  public void call_notEnoughArgs_throws() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1"))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    assertThrows(IllegalArgumentException.class, () -> fn.callInternal(new TestContext()));
  }

  @Test
  public void call_bindsArgs() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1"))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    RuntimeContext context = mock(TestContext.class);
    StackFrame sf = mock(StackFrame.class);
    when(context.top()).thenReturn(sf);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getArgModifierRegistry()).thenReturn(new DefaultRegistry<>());
    when(context.getRegistries()).thenReturn(mockRegistries);
    Data arg = mock(Data.class);
    fn.callInternal(context, arg);

    verify(sf).setVar("arg1", arg);
  }

  @Test
  public void bind_modifiedArgs() {
    String testArgModName = "testArgMod";
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1").setModifier(testArgModName))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    RuntimeContext context = mock(TestContext.class);
    ArgModifier testArgMod = mock(ArgModifier.class);
    StackFrame sf = mock(StackFrame.class);
    when(context.top()).thenReturn(sf);
    when(testArgMod.getName()).thenReturn(testArgModName);
    when(testArgMod.canShortCircuit(any())).thenReturn(false);
    Registry<ArgModifier> testArgModReg = new DefaultRegistry<>();
    testArgModReg.register(testArgMod);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getArgModifierRegistry()).thenReturn(testArgModReg);
    when(context.getRegistries()).thenReturn(mockRegistries);

    Data arg = mock(Data.class);
    fn.callInternal(context, arg);

    verify(testArgMod).modifyArgValue(any());
  }

  @Test
  public void return_shortCircuitedVal() {
    String testArgModName = "testArgMod";
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1").setModifier(testArgModName))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    RuntimeContext context = mock(TestContext.class);
    ArgModifier testArgMod = mock(ArgModifier.class);
    StackFrame sf = mock(StackFrame.class);
    when(context.top()).thenReturn(sf);
    when(testArgMod.getName()).thenReturn(testArgModName);
    when(testArgMod.canShortCircuit(any())).thenReturn(true);
    when(testArgMod.getShortCircuitValue(any()))
        .thenReturn(testDTI().primitiveOf("defaultReturnVal"));
    Registry<ArgModifier> testArgModReg = new DefaultRegistry<>();
    testArgModReg.register(testArgMod);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getArgModifierRegistry()).thenReturn(testArgModReg);
    when(context.getRegistries()).thenReturn(mockRegistries);

    Data arg = mock(Data.class);
    AssertUtil.assertDCAPEquals(
        testDTI().primitiveOf("defaultReturnVal"), fn.callInternal(context, arg));

    verify(sf, times(0)).setVar("arg1", arg);
    verify(testArgMod, times(0)).modifyArgValue(arg);
  }

  @Test
  public void call_varTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setVar(VariableTarget.newBuilder().setName("v1"))
                    .setValue(ValueSource.newBuilder().setConstString("aaa")))
            .addMapping(
                FieldMapping.newBuilder()
                    .setVar(VariableTarget.newBuilder().setName("v2").setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping(
                FieldMapping.newBuilder()
                    .setVar(VariableTarget.newBuilder().setName("v2").setPath("[].field"))
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    // Mock up a runtime context
    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);
    assertTrue(result.isNullOrEmpty());

    assertDCAPEquals(testDTI().primitiveOf("aaa"), vars.get("v1"));
    assertDCAPEquals(
        arrayOf(
            NullData.instance,
            containerOf("field", testDTI().primitiveOf("bbb")),
            containerOf("field", testDTI().primitiveOf("ccc"))),
        vars.get("v2"));
  }

  @Test
  public void call_fieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[].field"))
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);

    assertTrue(result.isArray());
    assertDCAPEquals(
        arrayOf(
            NullData.instance,
            containerOf("field", testDTI().primitiveOf("bbb")),
            containerOf("field", testDTI().primitiveOf("ccc"))),
        result);
  }

  @Test
  public void call_blankFieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping( // This one should overwrite.
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath(""))
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);

    assertTrue(result.isPrimitive());
    assertDCAPEquals(testDTI().primitiveOf("ccc"), result);
  }

  @Test
  public void call_omittedFieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping( // This one should overwrite.
                FieldMapping.newBuilder().setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);

    assertTrue(result.isPrimitive());
    assertDCAPEquals(testDTI().primitiveOf("ccc"), result);
  }

  @Test
  public void call_emptyFieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping( // This one should overwrite.
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder())
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);

    assertTrue(result.isPrimitive());
    assertDCAPEquals(testDTI().primitiveOf("ccc"), result);
  }

  @Test
  public void call_thisFieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("[1].field"))
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .addMapping( // This one should overwrite.
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("$this"))
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);

    Data result = fn.callInternal(context);

    assertTrue(result.isPrimitive());
    assertDCAPEquals(testDTI().primitiveOf("ccc"), result);
  }

  @Test
  public void call_sideFieldTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setField(FieldTarget.newBuilder().setPath("field").setType(FieldType.SIDE))
                    .setValue(ValueSource.newBuilder().setConstString("ccc")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Map<String, Data> vars = new HashMap<>();
    Map<String, Data> rootVars = new HashMap<>();
    RuntimeContext context =
        mockVarCapableRuntimeContext(mockStackFrame(rootVars), mockStackFrame(vars));

    Data result = fn.callInternal(context);

    assertTrue(result.isNullOrEmpty());
    assertDCAPEquals(
        containerOf("field", testDTI().primitiveOf("ccc")),
        rootVars.get(WhistleFunction.OUTPUT_VAR));
  }

  @Test
  public void call_customTarget() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setCustomSink(
                        FunctionCall.newBuilder()
                            .setReference(
                                FunctionReference.newBuilder()
                                    .setName("testSink")
                                    .setPackage("testpkg"))
                            .addArgs(ValueSource.newBuilder().setConstString("sink arg"))
                            .build())
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Constructor mockTargetCtor = mock(Constructor.class);
    Target mockTarget = mock(Target.class);
    when(mockTargetCtor.construct(any(), any())).thenReturn(mockTarget);

    PackageRegistry<Constructor> mockReg = mock(TestTargetPackageRegistry.class);
    when(mockReg.getOverloads(any(), any())).thenReturn(ImmutableSet.of(mockTargetCtor));

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getTargetRegistry()).thenReturn(mockReg);
    when(context.getRegistries()).thenReturn(mockRegistries);

    fn.callInternal(context);

    verify(mockTargetCtor)
        .construct(any(), argThat((Data i) -> i.asPrimitive().string().equals("sink arg")));
    verify(mockTarget).write(any(), argThat(i -> i.asPrimitive().string().equals("bbb")));
  }

  @Test
  public void call_customTargetIteratedWithNonIterable_throws() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setCustomSink(
                        FunctionCall.newBuilder()
                            .setReference(
                                FunctionReference.newBuilder()
                                    .setName("testSink")
                                    .setPackage("testpkg"))
                            .addArgs(ValueSource.newBuilder().setConstString("sink arg"))
                            .build())
                    .setValue(ValueSource.newBuilder().setConstString("bbb"))
                    .setIterateSource(true))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    Constructor mockTargetCtor = mock(Constructor.class);
    Target mockTarget = mock(Target.class);
    when(mockTargetCtor.construct(any(), any())).thenReturn(mockTarget);

    PackageRegistry<Constructor> mockReg = mock(TestTargetPackageRegistry.class);
    when(mockReg.getOverloads(any(), any())).thenReturn(ImmutableSet.of(mockTargetCtor));

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getTargetRegistry()).thenReturn(mockReg);
    when(context.getRegistries()).thenReturn(mockRegistries);

    Exception ex = assertThrows(IllegalArgumentException.class, () -> fn.callInternal(context));
    assertThat(ex).hasMessageThat().contains("Cannot iterate");
    assertThat(ex).hasMessageThat().contains("non-iterable");
  }

  @Test
  public void call_customTargetNonExisting_throws() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setCustomSink(
                        FunctionCall.newBuilder()
                            .setReference(
                                FunctionReference.newBuilder()
                                    .setName("testSink")
                                    .setPackage("testpkg"))
                            .addArgs(ValueSource.newBuilder().setConstString("sink arg"))
                            .build())
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    PackageRegistry<Target.Constructor> mockTargReg = mock(TestTargetPackageRegistry.class);
    PackageRegistry<CallableFunction> mockFnReg = mock(TestFunctionPackageRegistry.class);
    when(mockTargReg.getOverloads(any(), any())).thenReturn(ImmutableSet.of());
    when(mockFnReg.getOverloads(any(), any())).thenReturn(ImmutableSet.of());

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getTargetRegistry()).thenReturn(mockTargReg);
    when(mockRegistries.getFunctionRegistry(any())).thenReturn(mockFnReg);
    when(context.getRegistries()).thenReturn(mockRegistries);
    assertThrows(NoMatchingOverloadsException.class, () -> fn.callInternal(context));
  }

  @Test
  public void call_customTargetTooManyOverloads_throws() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setCustomSink(
                        FunctionCall.newBuilder()
                            .setReference(
                                FunctionReference.newBuilder()
                                    .setName("testSink")
                                    .setPackage("testpkg"))
                            .addArgs(ValueSource.newBuilder().setConstString("sink arg"))
                            .build())
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .build();
    WhistleFunction fn =
        new WhistleFunction(
            def, PipelineConfig.getDefaultInstance(), getPlaceHolderPackageContext());

    PackageRegistry<Target.Constructor> mockReg = mock(TestTargetPackageRegistry.class);
    when(mockReg.getOverloads(any(), any()))
        .thenReturn(
            ImmutableSet.of(mock(Target.Constructor.class), mock(Target.Constructor.class)));

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getTargetRegistry()).thenReturn(mockReg);
    when(context.getRegistries()).thenReturn(mockRegistries);

    assertThrows(UnsupportedOperationException.class, () -> fn.callInternal(context));
  }

  @Test
  public void call_customTargetGlobalPackage() {
    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addMapping(
                FieldMapping.newBuilder()
                    .setCustomSink(
                        FunctionCall.newBuilder()
                            // No package on the sink reference means use global package.
                            .setReference(FunctionReference.newBuilder().setName("testSink"))
                            .addArgs(ValueSource.newBuilder().setConstString("sink arg"))
                            .build())
                    .setValue(ValueSource.newBuilder().setConstString("bbb")))
            .build();
    PackageContext pkgCtx = new PackageContext(ImmutableSet.of("testpkg"));
    WhistleFunction fn = new WhistleFunction(def, PipelineConfig.getDefaultInstance(), pkgCtx);

    Target.Constructor mockTargetCtor = mock(Target.Constructor.class);
    Target mockTarget = mock(Target.class);
    when(mockTargetCtor.construct(any(), any())).thenReturn(mockTarget);

    PackageRegistry<Target.Constructor> mockReg = mock(TestTargetPackageRegistry.class);
    when(mockReg.getOverloads(any(), any())).thenReturn(ImmutableSet.of(mockTargetCtor));

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getTargetRegistry()).thenReturn(mockReg);
    when(context.getRegistries()).thenReturn(mockRegistries);
    when(context.getCurrentPackageContext()).thenReturn(pkgCtx);

    fn.callInternal(context);

    verify(mockReg).getOverloads(argThat(i -> i.contains("testpkg")), eq("testSink"));
  }

  @Test
  public void call_enablesNewOptions() {
    FunctionDefinition def = FunctionDefinition.newBuilder().setName("options").build();
    PipelineConfig config =
        PipelineConfig.newBuilder()
            .addOptions(PipelineConfig.Option.newBuilder().setName("test"))
            .addOptions(PipelineConfig.Option.newBuilder().setName("common"))
            .build();

    Option test = mock(Option.class);
    when(test.enable(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(test.disable(any())).thenAnswer(i -> i.getArgument(0));

    Option disable = mock(Option.class);
    when(disable.enable(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(disable.disable(any())).thenAnswer(i -> i.getArgument(0));

    Option common = mock(Option.class);
    when(common.enable(any(), any())).thenAnswer(i -> i.getArgument(0));
    when(common.disable(any())).thenAnswer(i -> i.getArgument(0));

    Registry<Option> options = mock(TestOptionRegistry.class);
    when(options.get(eq("test"))).thenReturn(test);
    when(options.get(eq("common"))).thenReturn(common);

    Registries mockRegistries = mock(Registries.class);
    when(mockRegistries.getOptionRegistry()).thenReturn(options);

    Map<String, Data> vars = new HashMap<>();
    RuntimeContext context = mockVarCapableRuntimeContext(vars);
    when(context.enabledOptions()).thenReturn(ImmutableSet.of(disable, common));
    when(context.getRegistries()).thenReturn(mockRegistries);

    PackageContext pkgCtx = new PackageContext(ImmutableSet.of("testpkg"));
    WhistleFunction fn = new WhistleFunction(def, config, pkgCtx);

    fn.callInternal(context);

    verify(test).enable(eq(context), any());
    verify(disable).disable(eq(context));
    verify(common, never()).enable(any(), any());
    verify(common, never()).disable(any());
  }
}
