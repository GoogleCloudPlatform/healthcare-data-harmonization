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

package com.google.cloud.verticals.foundations.dataharmonization.function.java;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.mockWithClass;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.nul;
import static com.google.common.truth.Truth.assertThat;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultContainer;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.ExtendedTestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.IrrelevantWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.TestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableMap;
import java.util.Arrays;
import java.util.function.BiConsumer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Parameterized test for testing java function invocations. */
@RunWith(Parameterized.class)
public class JavaValidFunctionsTest {

  private final String functionName;
  private final TestParameters params;

  public JavaValidFunctionsTest(String functionName, TestParameters params) {
    this.functionName = functionName;
    this.params = params;
  }

  @Parameters(name = "{0}{1}")
  public static Iterable<Object[]> data() {
    Container a = mockWithClass(Container.class);
    Container b = mockWithClass(Container.class);
    ExtendedTestWrapperData w =
        new ExtendedTestWrapperData(
            new TestWrapperData(
                new IrrelevantWrapperData(new TestWrapperData(mockWithClass(Container.class)))));
    return Arrays.asList(
        new Object[][] {
          {"noArgs", new TestParameters(mock(Data.class))},
          {"oneArg", new TestParameters(mock(Data.class), mockWithClass(Container.class))},
          {
            "multipleArgs",
            new TestParameters(
                mock(Data.class), mockWithClass(Container.class), mockWithClass(Array.class))
          },
          {"specificReturnValue", new TestParameters(mock(Primitive.class))},
          {"variadicArgs", new TestParameters(mockWithClass(Data.class))},
          {"variadicArgs", new TestParameters(mock(Data.class), mockWithClass(Container.class))},
          {
            "variadicArgs",
            new TestParameters(
                mock(Data.class), mockWithClass(Container.class), mockWithClass(Container.class))
          },
          {
            "variadicArgs",
            new TestParameters(
                (p, d) -> assertArrayEquals(new Data[]{nul()}, d.args),
                mock(Data.class), nul())
          },
          {
            "variadicPrimitiveArgs",
            new TestParameters(
                (p, d) -> assertArrayEquals(new String[] {"hello1", "hello2"}, d.args),
                mock(Data.class),
                testDTI().primitiveOf("hello1"),
                testDTI().primitiveOf("hello2"))
          },
          {
            "variadicPrimitiveArgs",
            new TestParameters(
                  (p, d) -> assertArrayEquals(new String[] {null}, d.args),
                  mock(Data.class),
                  nul())
          },
          {
            "variadicArgs",
            new TestParameters(
                // Assert that the array got expanded
                (p, d) -> assertArrayEquals(new Data[] {a, b}, d.args),
                mock(Data.class),
                arrayOf(a, b))
          },
          {
            "variadicAndRegularArgs",
            new TestParameters(mock(Data.class), mockWithClass(Container.class))
          },
          {
            "variadicAndRegularArgs",
            new TestParameters(
                mock(Data.class), mockWithClass(Container.class), mockWithClass(Container.class))
          },
          {
            "variadicAndRegularArgs",
            new TestParameters(
                mock(Data.class),
                mockWithClass(Container.class),
                mockWithClass(Container.class),
                mockWithClass(Container.class))
          },
          {
            "variadicAndRegularArgs",
            new TestParameters(
                // Assert that the array got expanded
                (p, d) -> assertArrayEquals(new Data[] {a, a, b}, d.args),
                mock(Data.class),
                a,
                arrayOf(a, b))
          },
          {
            "variadicAndRegularArgs",
            new TestParameters(
                  // Assert that the array got expanded
                  (p, d) -> assertArrayEquals(new Data[] {a, nul()}, d.args),
                  mock(Data.class),
                  a,
                  nul())
          },
          {
            "runtimeContextParam",
            new TestParameters((p, d) -> assertNotNull(d.ctx), mock(Data.class))
          },
          {
            "runtimeContextParamWithOtherArgs",
            new TestParameters(
                ((BiConsumer<TestParameters, InvocationCapture>)
                        JavaValidFunctionsTest::assertArgsEqual)
                    .andThen((p, d) -> assertNotNull(d.ctx)),
                mock(Data.class),
                mockWithClass(Container.class))
          },
          {
            "specificImplArg",
            new TestParameters(mock(Data.class), mockWithClass(DefaultContainer.class))
          },
          {
            "primitiveArgs",
            new TestParameters(
                (p, d) -> {
                  assertEquals(true, d.args[0]);
                  assertEquals("hello", d.args[1]);
                  assertEquals(3.14, d.args[2]);
                },
                mock(Data.class),
                testDTI().primitiveOf(true),
                testDTI().primitiveOf("hello"),
                testDTI().primitiveOf(3.14))
          },
          {
            "specificImplArg", // tests that JavaFunction is able to unwrap the data wrapper when
            // passed into java function that only accepts inner data type
            new TestParameters(
                (p, d) -> {
                  assertThat(p.args[0]).isInstanceOf(TestWrapperData.class);
                  assertThat(d.args[0]).isInstanceOf(DefaultContainer.class);
                  AssertUtil.assertDCAPEquals(p.args[0], (Data) d.args[0]);
                },
                mock(Data.class),
                new TestWrapperData(
                    testDTI().containerOf(ImmutableMap.of("field1", testDTI().primitiveOf(1.0)))))
          },
          {
            "oneArg", // tests that JavaFunction is able to resolve the correct interface
            new TestParameters(
                (p, d) -> {
                  assertThat(p.args[0]).isInstanceOf(TestWrapperData.class);
                  assertThat(d.args[0]).isInstanceOf(TestWrapperData.class);
                  assertEquals(p.args[0], d.args[0]);
                },
                mock(Data.class),
                new TestWrapperData(mockWithClass(Container.class)))
          },
          {
            "specificWrapperDataArg", // when there are multiple wrappers, JavaFunction resolves to
            // the shallowest layer of wrappers that is or is the subset of the argument type.
            new TestParameters(
                (p, d) -> assertThat(d.args[0]).isSameInstanceAs(w),
                mock(Data.class),
                new IrrelevantWrapperData(new IrrelevantWrapperData(w)))
          }
        });
  }

  public static void assertArgsEqual(TestParameters params, InvocationCapture delegate) {
    assertArrayEquals(params.args, delegate.args);
  }

  @Test
  public void test() {
    InvocationCapture delegate = new InvocationCapture(params.want);
    JavaFunction fn = new JavaFunction(TestMethods.get(functionName), new TestMethods(delegate));

    Data got = fn.call(new TestContext(), params.args);
    assertEquals(params.want, got);
    assertTrue(delegate.invoked);
    params.argsAssertion.accept(params, delegate);
  }

  @Test
  public void test_methodHandleImpl() {
    InvocationCapture delegate = new InvocationCapture(params.want);
    MethodHandleJavaFunction fn =
        new MethodHandleJavaFunction(TestMethods.get(functionName), new TestMethods(delegate));

    Data got = fn.call(new TestContext(), params.args);
    assertEquals(params.want, got);
    assertTrue(delegate.invoked);
    params.argsAssertion.accept(params, delegate);
  }

  private static final class TestParameters {

    private final BiConsumer<TestParameters, InvocationCapture> argsAssertion;
    private final Data want;
    private final Data[] args;

    public TestParameters(Data want, Data... args) {
      this(JavaValidFunctionsTest::assertArgsEqual, want, args);
    }

    public TestParameters(
        BiConsumer<TestParameters, InvocationCapture> argsAssertion, Data want, Data... args) {
      this.argsAssertion = argsAssertion;
      this.args = args;
      this.want = want;
    }

    @Override
    public String toString() {
      return String.format(
          "(%s) => %s",
          stream(args)
              .map(a -> a == null ? "null" : a.getClass().getSimpleName())
              .collect(joining(", ")),
          want == null ? "null" : want.getClass().getSimpleName());
    }
  }
}
