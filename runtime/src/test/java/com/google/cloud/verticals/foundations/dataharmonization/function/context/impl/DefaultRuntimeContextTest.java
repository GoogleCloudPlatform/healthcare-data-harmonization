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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRuntimeContext.hashRegistries;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.PackageContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.Registries;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultRuntimeContext.DefaultImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame.DefaultBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.serialization.RuntimeContextComponentSerializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.impl.DefaultImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionDefinition.Argument;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestFunctionPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.TestTargetPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultPackageRegistry;
import com.google.cloud.verticals.foundations.dataharmonization.registry.impl.DefaultRegistry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

/** Tests for DefaultRuntimeContext. */
@RunWith(JUnit4.class)
public class DefaultRuntimeContextTest {
  private static final PackageContext emptyPackageContext = new PackageContext(ImmutableSet.of());

  static StackFrame generateStackFrame(String name, StackFrame parent, boolean inheritParentVars) {
    return new DefaultStackFrame.DefaultBuilder()
        .setName(name)
        .setParent(parent)
        .setInheritParentVars(inheritParentVars)
        .build();
  }

  @Test
  public void evaluate_freeParam_throws() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);
    assertThrows(
        IllegalArgumentException.class,
        () -> context.evaluate(ValueSource.newBuilder().setFreeParameter("foo").build()));
  }

  @Test
  public void evaluate_unsetCase_throws() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);
    assertThrows(
        IllegalArgumentException.class, () -> context.evaluate(ValueSource.getDefaultInstance()));
  }

  @Test
  public void evaluate_fromLocal_getsVar() {
    StackFrame sf = mock(StackFrame.class);
    Data inner = mock(Data.class);
    when(sf.getVar(anyString())).thenReturn(inner);

    RuntimeContext context =
        new DefaultRuntimeContext(emptyPackageContext, sf, new DefaultRegistries(), null);

    Data actual = context.evaluate(ValueSource.newBuilder().setFromLocal("foo").build());
    assertEquals(inner, actual);
    verify(sf).getVar("foo");
  }

  @Test
  public void evaluate_constString_getsConst() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    Data actual = context.evaluate(ValueSource.newBuilder().setConstString("hi").build());
    assertTrue(actual.isPrimitive());
    assertEquals("hi", actual.asPrimitive().string());
  }

  @Test
  public void evaluate_constInt_getsConst() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    Data actual = context.evaluate(ValueSource.newBuilder().setConstInt(100).build());
    assertTrue(actual.isPrimitive());
    assertEquals(100.0, actual.asPrimitive().num(), 0.0);
  }

  @Test
  public void evaluate_constFloat_getsConst() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    Data actual = context.evaluate(ValueSource.newBuilder().setConstFloat(Math.E).build());
    assertTrue(actual.isPrimitive());
    assertEquals(Math.E, actual.asPrimitive().num(), 0.0);
  }

  @Test
  public void evaluate_constBool_getsConst() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    Data actual = context.evaluate(ValueSource.newBuilder().setConstBool(true).build());
    assertTrue(actual.isPrimitive());
    assertTrue(actual.asPrimitive().bool());
  }

  @Test
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void evaluate_functionCall_executesIt() {
    StackFrame.Builder builder = mock(StackFrame.Builder.class, new ReturnsSelf());
    StackFrame sf = mock(StackFrame.class);
    when(builder.build()).thenReturn(sf);
    when(sf.newBuilder()).thenReturn(builder);

    Data funcRet = mock(Data.class);
    TestCallableFunc func = mock(TestCallableFunc.class);
    when(func.getSignature())
        .thenReturn(
            new Signature(
                "myPackage", "myFunc", /* variadic */ ImmutableList.of(Data.class), true));
    when(func.call(any(RuntimeContext.class), any(Data[].class))).thenReturn(funcRet);

    TestFunctionPackageRegistry fr = mock(TestFunctionPackageRegistry.class);
    when(fr.getOverloads(anySet(), eq("myFunc"))).thenReturn(ImmutableSet.of(func));

    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext,
            sf,
            new DefaultRegistries(
                fr, mock(TestTargetPackageRegistry.class), new DefaultRegistry<>()),
            null);

    FunctionCall fc =
        FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("myFunc"))
            .addArgs(ValueSource.newBuilder().setConstString("hello").build())
            .build();

    Data actual = context.evaluate(ValueSource.newBuilder().setFunctionCall(fc).build());
    assertEquals(funcRet, actual);
    verify(func)
        .call(
            any(),
            argThat((Data d) -> d.isPrimitive() && "hello".equals(d.asPrimitive().string())));
  }

  @Test
  public void evaluate_functionCallBuildingClosure_returnsIt() {
    RuntimeContext context =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    FunctionCall fc =
        FunctionCall.newBuilder()
            .setReference(FunctionReference.newBuilder().setName("myFunc"))
            .addArgs(ValueSource.newBuilder().setConstString("hello").build())
            .setBuildClosure(true)
            .build();

    Data actual = context.evaluate(ValueSource.newBuilder().setFunctionCall(fc).build());
    assertEquals(DefaultClosure.class, actual.getClass());

    DefaultClosure actualClosure = (DefaultClosure) actual;
    assertEquals("myFunc", actualClosure.getFunctionRef().getFunctionName());
    assertThat(actualClosure.getArgs()).hasLength(1);
    assertEquals("hello", actualClosure.getArgs()[0].asPrimitive().string());
  }

  @Test
  public void wrap_newPackageContext_becomesCurrentContext() {
    // This test asserts that if a CallbleFunction declares a new PackageContext, that
    // PackageContext becomes the "current" one in RuntimeContext when that function is called.
    StackFrame.Builder builder = mock(StackFrame.Builder.class, new ReturnsSelf());
    StackFrame sf = mock(StackFrame.class);
    when(builder.build()).thenReturn(sf);
    when(sf.newBuilder()).thenReturn(builder);

    PackageContext newPackageContext = new PackageContext(ImmutableSet.of("test"));
    Data funcRet = mock(Data.class);
    TestCallableFunc func = mock(TestCallableFunc.class);
    when(func.getSignature())
        .thenReturn(
            new Signature(
                "myPackage", "myFunc", /* variadic */ ImmutableList.of(Data.class), true));
    when(func.callInternal(any(RuntimeContext.class), any(Data[].class))).thenReturn(funcRet);
    // Return our new PackageContext specific to this function.
    when(func.getLocalPackageContext(any())).thenReturn(newPackageContext);

    TestFunctionPackageRegistry fr = mock(TestFunctionPackageRegistry.class);
    when(fr.getOverloads(anySet(), eq("myFunc"))).thenReturn(ImmutableSet.of(func));

    RuntimeContext context =
        new DefaultRuntimeContext(emptyPackageContext, sf, new DefaultRegistries(), null);

    Data actual =
        context.wrap(
            func,
            new Data[0],
            (ctx, args) -> {
              // Assert the RuntimeContext this function is called with has our custom
              // PackageContext
              assertEquals(newPackageContext, ctx.getCurrentPackageContext());
              return funcRet;
            });

    // assert the function got called
    assertEquals(funcRet, actual);
    // assert the original/parent RuntimeContext still has its original PackageContext.
    assertEquals(emptyPackageContext, context.getCurrentPackageContext());
  }

  @Test
  @SuppressWarnings("DirectInvocationOnMock")
  public void current_returnsTheCurrentActiveContextDuringWrap() {
    RuntimeContext mockContext = mock(RuntimeContext.class);
    RuntimeContext nextContext = mock(RuntimeContext.class);
    when(mockContext.newContextFromFrame(any(), any())).thenReturn(nextContext);
    StackFrame sf = new DefaultStackFrame.DefaultBuilder().setName("test").build();
    when(mockContext.top()).thenReturn(sf);
    when(mockContext.getCancellation()).thenReturn(new NoopCancellationToken());

    Data funcRet = mock(Data.class);
    TestCallableFunc func = mock(TestCallableFunc.class);
    PackageContext newPackageContext = new PackageContext(ImmutableSet.of("test"));
    when(func.getLocalPackageContext(any())).thenReturn(newPackageContext);
    when(func.callInternal(any(RuntimeContext.class), any(Data[].class))).thenReturn(funcRet);
    when(func.getSignature())
        .thenReturn(
            new Signature(
                "myPackage", "myFunc", /* variadic */ ImmutableList.of(Data.class), true));
    when(mockContext.wrap(any(), any(), any())).thenAnswer(Answers.CALLS_REAL_METHODS);

    Data actual =
        mockContext.wrap(
            func,
            new Data[0],
            (ctx, args) -> {
              assertEquals(nextContext, RuntimeContext.current());
              return funcRet;
            });
    verify(mockContext, times(1)).newContextFromFrame(any(), any());
    assertEquals(funcRet, actual);
    assertEquals(RuntimeContext.current(), mockContext);
  }

  @Test
  public void current_returnsInitiationContextWhenItsCreated() {
    InitializationContext context =
        new InitializationContext(
            new PackageContext(ImmutableSet.of("test")),
            new DefaultRegistries(),
            new DefaultImportProcessor(),
            new DefaultImplementation(),
            new DefaultBuilder(),
            new DefaultMetaData());
    assertEquals(context, RuntimeContext.current());
  }

  @Test
  public void setMeta_derivedContext_inheritsMeta() {
    Object rootMeta = mock(Object.class);
    Object derivedMeta = mock(Object.class);

    RuntimeContext root =
        new DefaultRuntimeContext(
            emptyPackageContext, mock(StackFrame.class), new DefaultRegistries(), null);

    RuntimeContext derived =
        root.newContextFromFrame(
            mock(StackFrame.Builder.class, new ReturnsSelf()), emptyPackageContext);

    // assert derived gets meta from root
    root.getMetaData().setMeta("root", rootMeta);
    assertEquals(rootMeta, derived.getMetaData().getMeta("root"));

    // assert root gets meta from derived
    derived.getMetaData().setMeta("derived", derivedMeta);
    assertEquals(derivedMeta, root.getMetaData().getMeta("derived"));

    // assert derived can overwrite meta on root
    derived.getMetaData().setMeta("root", derivedMeta);
    assertEquals(derivedMeta, root.getMetaData().getMeta("root"));

    // assert root can overwrite meta on derived
    root.getMetaData().setMeta("derived", rootMeta);
    assertEquals(rootMeta, derived.getMetaData().getMeta("derived"));
  }

  @Test
  public void equals_onDefaultRuntimeContext_identity() throws Exception {
    PackageContext pkgCtx =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    StackFrame top = generateStackFrame("testFrame1", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx, top, top);
    assertEquals(runtimeCtx1, runtimeCtx1);
  }

  @Test
  public void equals_onDefaultRuntimeContext_true() throws Exception {
    PackageContext pkgCtx1 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx1, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx1, top1, top1);

    assertEquals(runtimeCtx1, runtimeCtx2);
  }

  @Test
  public void equals_onDefaultRuntimeContext_differentPackageContext() throws Exception {
    PackageContext pkgCtx1 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    PackageContext pkgCtx2 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue4")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);
    StackFrame top2 = generateStackFrame("testFrame2", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx1, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx2, top2, top2);

    Assert.assertNotEquals(runtimeCtx1, runtimeCtx2);
  }

  @Test
  public void equals_onDefaultRuntimeContext_nullStackFrame() throws Exception {
    PackageContext pkgCtx1 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    PackageContext pkgCtx2 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue4")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx1, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx2, null, null);

    Assert.assertNotEquals(runtimeCtx1, runtimeCtx2);
  }

  @Test
  public void equals_onDefaultRuntimeContext_differentStackFrames() throws Exception {
    PackageContext pkgCtx1 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    PackageContext pkgCtx2 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue3")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);
    StackFrame top2 = generateStackFrame("testFrame2", null, false);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx1, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx2, top2, top2);

    Assert.assertNotEquals(runtimeCtx1, runtimeCtx2);
  }

  @Test
  public void equals_onDefaultRuntimeContext_emptyPackageContext() throws Exception {
    PackageContext pkgCtx1 = new PackageContext(new HashSet<>());
    PackageContext pkgCtx2 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue4")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);
    StackFrame top2 = generateStackFrame("testFrame1", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(pkgCtx1, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx2, top2, top2);

    Assert.assertNotEquals(runtimeCtx1, runtimeCtx2);
  }

  @Test
  public void equals_onDefaultRuntimeContext_nullPackageContext() throws Exception {
    PackageContext pkgCtx2 =
        new PackageContext(new HashSet<>(Arrays.asList("setValue1", "setValue2", "setValue4")));
    StackFrame top1 = generateStackFrame("testFrame1", null, true);
    StackFrame top2 = generateStackFrame("testFrame1", null, true);

    RuntimeContext runtimeCtx1 = generateRuntimeContext(null, top1, top1);
    RuntimeContext runtimeCtx2 = generateRuntimeContext(pkgCtx2, top2, top2);

    Assert.assertNotEquals(runtimeCtx1, runtimeCtx2);
  }

  private RuntimeContext generateRuntimeContext(
      PackageContext pkgCtx, StackFrame sf1, StackFrame sf2) {
    return new DefaultRuntimeContext(
        pkgCtx,
        sf1,
        sf2,
        new DefaultRegistries(),
        null,
        new DefaultMetaData(),
        new DefaultCancellationToken(),
        new HashSet<>());
  }

  @Test
  public void getFunctionRegistry_returnDefaultRegistry_noPackageSpecificRegistry() {
    PackageRegistry<CallableFunction> registry = new DefaultPackageRegistry<>();
    RuntimeContext context =
        new DefaultRuntimeContext(
            null,
            null,
            null,
            new DefaultRegistries(registry, null, null),
            null,
            new DefaultMetaData(),
            new DefaultCancellationToken(),
            new HashSet<>());
    Assert.assertEquals(registry, context.getRegistries().getFunctionRegistry("pkg"));
  }

  @Test
  public void registerFunctionRegistry_returnsRegisteredRegistry() {
    PackageRegistry<CallableFunction> registry = new DefaultPackageRegistry<>();
    RuntimeContext context =
        new DefaultRuntimeContext(
            null,
            null,
            null,
            new DefaultRegistries(),
            null,
            new DefaultMetaData(),
            new DefaultCancellationToken(),
            new HashSet<>());
    String pkg = "pkg";
    context.getRegistries().registerFunctionRegistry(pkg, registry);
    Assert.assertEquals(registry, context.getRegistries().getFunctionRegistry(pkg));
  }

  @Test
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void serialize_runtimeContext_success() throws IOException, ClassNotFoundException {
    RuntimeContext expected = createRuntimeContext();

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(expected);

    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    RuntimeContext actual = (RuntimeContext) in.readObject();
    assertEquals(expected, actual);
    assertEquals(expected.getMetaData(), actual.getMetaData());
    assertEquals(hashRegistries(expected.getRegistries()), hashRegistries(actual.getRegistries()));
    assertThat(actual.getRegistries()).isNotNull();
  }

  @Test
  @Ignore(
      "b/379148650 Disable this test due to a race condition"
          + " We aren't developing Whistle core features anymore.")
  public void serialize_runtimeContext_usesSuppliedRegistriesSerializer()
      throws IOException, ClassNotFoundException {
    RuntimeContext expected = createRuntimeContext();
    RegistriesSerializer serializer = new RegistriesSerializer();

    expected
        .getMetaData()
        .setSerializableMeta(DefaultRuntimeContext.REGISTRIES_SERIALIZER_KEY, serializer);

    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream out = new ObjectOutputStream(bytes);
    out.writeObject(expected);

    ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()));
    RuntimeContext actual = (RuntimeContext) in.readObject();

    assertEquals(expected, actual);
    assertEquals(expected.getMetaData(), actual.getMetaData());
    assertEquals(hashRegistries(expected.getRegistries()), hashRegistries(actual.getRegistries()));
    assertThat(actual.getRegistries()).isNotNull();
    assertThat(serializer.saved).isSameInstanceAs(expected.getRegistries());
  }

  private RuntimeContext createRuntimeContext() {
    List<String> list = Arrays.asList("setValue1", "setValue2", "setValue3");
    Set<String> packageSet1 = new HashSet<>(list);
    PackageContext pkgCtx = new PackageContext(packageSet1);

    Data value = mock(Data.class);
    StackFrame grandparent = new DefaultStackFrame.DefaultBuilder().build();
    grandparent.setVar("any", value);
    StackFrame parent =
        new DefaultStackFrame.DefaultBuilder()
            .setParent(grandparent)
            .setInheritParentVars(true)
            .build();
    StackFrame child =
        new DefaultStackFrame.DefaultBuilder().setParent(parent).setInheritParentVars(true).build();
    Map<String, Serializable> testMetadata = new HashMap<>();
    testMetadata.put("testKey1", testDTI().primitiveOf("testValue1"));
    testMetadata.put("testKey2", testDTI().primitiveOf("testValue2"));

    FunctionDefinition def =
        FunctionDefinition.newBuilder()
            .setName("w00t")
            .addArgs(Argument.newBuilder().setName("arg1"))
            .build();
    PipelineConfig config = PipelineConfig.newBuilder().setRootBlock(def).build();
    WhistleFunction testFunc = new WhistleFunction(def, config, emptyPackageContext);
    Registries regs = new DefaultRegistries();
    regs.getFunctionRegistry("test").register("test", testFunc);
    return new DefaultRuntimeContext(
        pkgCtx,
        child,
        parent,
        regs,
        null,
        new DefaultMetaData(new HashMap<>(), testMetadata),
        new DefaultCancellationToken(),
        new HashSet<>());
  }

  private static class RegistriesSerializer
      implements RuntimeContextComponentSerializer<Registries>, Serializable {
    private static volatile Registries saved;

    @Override
    public void serialize(Registries value, OutputStream outputStream) throws IOException {
      this.saved = value;
    }

    @Override
    public Registries deserialize(InputStream inputStream)
        throws IOException, ClassNotFoundException {
      return this.saved;
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof RegistriesSerializer;
    }

    @Override
    public int hashCode() {
      return getClass().hashCode();
    }
  }

  private static class ReturnsSelf implements Answer<Object> {

    @Override
    public Object answer(InvocationOnMock invocation) {
      return invocation
              .getMethod()
              .getReturnType()
              .isAssignableFrom(invocation.getMock().getClass())
          ? invocation.getMock()
          : null;
    }
  }

  private abstract static class TestCallableFunc extends CallableFunction {
    @Override
    public Data callInternal(RuntimeContext context, Data... args) {
      throw new UnsupportedOperationException(
          "This method is not really implemented. It's overridden only to expose it for mocking.");
    }
  }
}
