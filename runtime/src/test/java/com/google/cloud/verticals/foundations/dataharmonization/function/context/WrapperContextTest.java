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
package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.OverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportProcessor;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.ValueSource;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for WrapperContext. */
@RunWith(JUnit4.class)
public class WrapperContextTest {

  @Test
  public void wrap_forwardsMethods() {
    RuntimeContext inner = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    CallableFunction cf = mock(CallableFunction.class);
    Data[] args = new Data[] {mock(Data.class)};
    Data result = mock(Data.class);

    BiFunction<RuntimeContext, Data[], Data> delegate =
        (c, a) -> {
          // Make sure that the instance got re-wrapped.
          assertThat(c).isInstanceOf(TestWrapperContext.class);
          return result;
        };

    wrapperContext.wrap(cf, args, delegate);

    // Verify the delegate that is passed in rewraps by checking that it returns result,
    // which means the assertion above also gets checked.
    verify(inner).wrap(eq(cf), eq(args), argThat(fn -> fn.apply(inner, args) == result));
  }

  @Test
  public void evaluate_forwardsMethods() {
    RuntimeContext inner = mock(RuntimeContext.class, Answers.RETURNS_MOCKS);
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    ValueSource vs = ValueSource.getDefaultInstance();

    wrapperContext.evaluate(vs);

    verify(inner).evaluate(vs);
  }

  @Test
  public void evaluateClosure_forwardsMethods() {
    CallableFunction testF = mock(CallableFunction.class, Answers.CALLS_REAL_METHODS);
    when(testF.getSignature())
        .thenReturn(new Signature("testP", "testF", ImmutableList.of(Primitive.class), false));

    RuntimeContext inner =
        RuntimeContextUtil.mockSingleFunctionRuntimeContext("testP", "testF", testF);
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    ValueSource vs =
        ValueSource.newBuilder()
            .setFunctionCall(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setPackage("testP").setName("testF"))
                    .addArgs(ValueSource.newBuilder().setConstString("testC"))
                    .build())
            .build();

    wrapperContext.evaluate(vs);

    verify(inner).wrap(eq(testF), any(), any());
  }

  @Test
  public void evaluateBuildClosure_forwardsMethods() {
    RuntimeContext inner = RuntimeContextUtil.mockRuntimeContextWithRegistry();
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    ValueSource vs =
        ValueSource.newBuilder()
            .setFunctionCall(
                FunctionCall.newBuilder()
                    .setReference(
                        FunctionReference.newBuilder().setPackage("testP").setName("testF"))
                    .addArgs(ValueSource.newBuilder().setConstString("testC"))
                    .setBuildClosure(true)
                    .build())
            .build();

    Data ret = wrapperContext.evaluate(vs);
    assertThat(ret).isInstanceOf(Closure.class);
    Data[] args = ((Closure) ret).getArgs();
    assertThat(args).hasLength(1);
  }

  @Test
  public void getterMethods_forwardsMethods() {
    testGetter(Registries.class, RuntimeContext::getRegistries);
    testGetter(MetaData.class, RuntimeContext::getMetaData);

    // PackageContext is a final class, we need a separate test.
    testGetterWithInstance(
        new PackageContext(ImmutableSet.of("test")), RuntimeContext::getCurrentPackageContext);

    testGetter(OverloadSelector.class, RuntimeContext::getOverloadSelector);
    testGetter(StackFrame.class, RuntimeContext::top);
    testGetter(StackFrame.class, RuntimeContext::bottom);
    testGetter(ImportProcessor.class, RuntimeContext::getImportProcessor);
    testGetter(DataTypeImplementation.class, RuntimeContext::getDataTypeImplementation);
  }

  @Test
  public void newContextFromFrame_rewraps() {
    RuntimeContext inner = mock(RuntimeContext.class);
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    StackFrame.Builder builder = mock(StackFrame.Builder.class);
    PackageContext pkgCtx = new PackageContext(ImmutableSet.of("test"));
    RuntimeContext got = wrapperContext.newContextFromFrame(builder, pkgCtx);

    assertThat(got).isInstanceOf(TestWrapperContext.class);
    verify(inner).newContextFromFrame(builder, pkgCtx);
  }

  @Test
  public void getWrapper_singleItemChain_noop() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", tail);

    assertThat(a.toString()).isEqualTo("A->tail");

    TestWrapperContext got = WrapperContext.getWrapper(a, TestWrapperContext.class, aa -> aa == a);

    assertThat(got.toString()).isEqualTo("A->tail");
    assertThat(got).isSameInstanceAs(a);
  }

  @Test
  public void getWrapper_multiItemChain_surfaces() {
    RuntimeContext tail = tail();

    TestWrapperContext c = wrap("C", wrap("D", tail));
    TestWrapperContext a = wrap("A", wrap("B", c));

    assertThat(a.toString()).isEqualTo("A->B->C->D->tail");

    TestWrapperContext got = WrapperContext.getWrapper(a, TestWrapperContext.class, cc -> cc == c);

    assertThat(got.toString()).isEqualTo("C'->A'->B'->D->tail");
  }

  @Test
  public void getWrapper_multiItemChain_firstItem_noop() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", wrap("B", wrap("C", wrap("D", tail))));

    assertThat(a.toString()).isEqualTo("A->B->C->D->tail");

    TestWrapperContext got = WrapperContext.getWrapper(a, TestWrapperContext.class, aa -> aa == a);

    assertThat(got.toString()).isEqualTo("A->B->C->D->tail");
    assertThat(got).isSameInstanceAs(a);
  }

  @Test
  public void getWrapper_notAWrapper_null() {
    RuntimeContext tail = mock(RuntimeContext.class);
    TestWrapperContext got = WrapperContext.getWrapper(tail, TestWrapperContext.class, x -> true);
    assertThat(got).isNull();
  }

  @Test
  public void getWrapper_singleItemChain_null() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", tail);

    assertThat(a.toString()).isEqualTo("A->tail");

    TestWrapperContext got = WrapperContext.getWrapper(a, TestWrapperContext.class, x -> false);
    assertThat(got).isNull();
  }

  @Test
  public void getWrapper_multiItemChain_null() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", wrap("B", wrap("C", wrap("D", tail))));

    assertThat(a.toString()).isEqualTo("A->B->C->D->tail");

    TestWrapperContext got = WrapperContext.getWrapper(a, TestWrapperContext.class, x -> false);
    assertThat(got).isNull();
  }

  @Test
  public void pushToBottom_singleItemChain_noop() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", tail);

    assertThat(a.toString()).isEqualTo("A->tail");

    RuntimeContext got = a.pushToBottom();

    assertThat(got.toString()).isEqualTo("A->tail");
    assertThat(got).isSameInstanceAs(a);
  }

  @Test
  public void pushToBottom_multiItemChain_pushes() {
    RuntimeContext tail = tail();

    TestWrapperContext a = wrap("A", wrap("B", wrap("C", wrap("D", tail))));

    RuntimeContext got = a.pushToBottom();

    assertThat(got.toString()).isEqualTo("B'->C'->D'->A'->tail");

    // Original A unmodified.
    assertThat(a.toString()).isEqualTo("A->B->C->D->tail");
  }

  private static <T> void testGetter(Class<T> t, Function<RuntimeContext, T> method) {
    testGetterWithInstance(mock(t), method);
  }

  private static <T> void testGetterWithInstance(T sampleT, Function<RuntimeContext, T> method) {
    RuntimeContext inner = mock(RuntimeContext.class);
    TestWrapperContext wrapperContext = new TestWrapperContext(inner);

    // inner context will return our sample value.
    when(method.apply(inner)).thenReturn(sampleT);

    // Call the getter on the wrapper, make sure it returned the value we set on inner.
    T got = method.apply(wrapperContext);
    assertThat(got).isSameInstanceAs(sampleT);

    // Verify inner's getter got called (just in case).
    got = method.apply(verify(inner));
    // Appease the linter - it gets mad if we don't use the result of .apply
    assertThat(got).isNull();
  }

  private static RuntimeContext tail() {
    RuntimeContext tail = mock(RuntimeContext.class);
    when(tail.toString()).thenReturn("tail");
    return tail;
  }

  /** Factory method for building a new TestWrapperContext. */
  public static TestWrapperContext wrap(String id, RuntimeContext inner) {
    return new TestWrapperContext(id, inner);
  }

  /** Simple WrapperContext with an id string. When rewrap, appends a ' (single quote) to the id. */
  public static class TestWrapperContext extends WrapperContext<TestWrapperContext> {
    private final String id;

    public TestWrapperContext(String id, RuntimeContext innerContext) {
      super(innerContext, TestWrapperContext.class);
      this.id = id;
    }

    public TestWrapperContext(RuntimeContext innerContext) {
      this(UUID.randomUUID().toString(), innerContext);
    }

    @Override
    protected TestWrapperContext rewrap(RuntimeContext innerContext) {
      return new TestWrapperContext(id + "'", innerContext);
    }

    @Override
    public String toString() {
      return String.format("%s->%s", id, getInnerContext());
    }
  }
}
