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

import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.ExtendedTestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.IrrelevantWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.WrapperDataUtils.TestWrapperData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.MultipleMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.function.signature.Signature;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;

/** Tests for OverloadSelector. */
@RunWith(JUnit4.class)
public class DefaultOverloadSelectorTest {

  private static final Data DATA_IMPL = mockD(Data.class);
  private static final Data EXTENDS_DATA_IMPL =
      mock(DATA_IMPL.getClass(), Answers.CALLS_REAL_METHODS);
  private static final Container CONTAINER_IMPL = mockD(Container.class);
  private static final String DEFAULT_PKG_NAME = "testPkg";

  @SafeVarargs
  private static CallableFunction mockFn(
      String name, boolean variadic, Class<? extends Data>... argTypes) {
    CallableFunction fn = mock(CallableFunction.class);
    when(fn.getSignature())
        .thenReturn(
            new Signature(DEFAULT_PKG_NAME, name, ImmutableList.copyOf(argTypes), variadic));
    return fn;
  }

  @SafeVarargs
  private static CallableFunction mockFn(Class<? extends Data>... argTypes) {
    return mockFn("testFn", false, argTypes);
  }

  @SafeVarargs
  private static CallableFunction mockFn(String name, Class<? extends Data>... argTypes) {
    return mockFn(name, false, argTypes);
  }

  @Test
  public void distance_sameClass_returns0() {
    assertEquals(0, DefaultOverloadSelector.distance(DATA_IMPL.getClass(), DATA_IMPL), 0);
    assertEquals(
        0, DefaultOverloadSelector.distance(EXTENDS_DATA_IMPL.getClass(), EXTENDS_DATA_IMPL), 0);
    assertEquals(0, DefaultOverloadSelector.distance(CONTAINER_IMPL.getClass(), CONTAINER_IMPL), 0);
  }

  @Test
  public void distance_differentClass_returnsInfinite() {
    Class<? extends Data> want = TestDataImpl1.class;
    Data got = mockD(TestDataImpl2.class);

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(want, got), 0);
    assertEquals(
        Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(Container.class, DATA_IMPL), 0);
  }

  @Test
  public void distance_interfaceVsImplClass_returns1() {
    assertEquals(1, DefaultOverloadSelector.distance(Container.class, CONTAINER_IMPL), 0);
  }

  @Test
  @Ignore("b/379148650 JDK upgrade changes the behavior of test.")
  public void distance_interfaceVsDistantImplClass_returns2() {
    assertEquals(2, DefaultOverloadSelector.distance(Data.class, EXTENDS_DATA_IMPL), 0);

    Data deepExtends =
        mock(
            mock(
                    mock(EXTENDS_DATA_IMPL.getClass(), Answers.CALLS_REAL_METHODS).getClass(),
                    Answers.CALLS_REAL_METHODS)
                .getClass(),
            Answers.CALLS_REAL_METHODS);
    assertEquals(2, DefaultOverloadSelector.distance(Data.class, deepExtends), 0);
  }

  @Test
  public void distance_dataVsInterfaceImplClass_returns2() {
    assertEquals(2, DefaultOverloadSelector.distance(Data.class, CONTAINER_IMPL), 0);
  }

  @Test
  public void distance_wantTheOutMostWrapper_returns0() {
    Data data = new IrrelevantWrapperData(new TestWrapperData(mock(Container.class)));
    assertEquals(0.0, DefaultOverloadSelector.distance(IrrelevantWrapperData.class, data), 0);
  }

  @Test
  public void distance_wantSuperClassOfOutMostWrapper_returns2() {
    Data data = new ExtendedTestWrapperData(new TestWrapperData(mock(Container.class)));
    assertEquals(2.0, DefaultOverloadSelector.distance(TestWrapperData.class, data), 0);
  }

  @Test
  public void distance_wantInnerWrapper_returnEpsilon() {
    Data data = new IrrelevantWrapperData(new TestWrapperData(mock(Container.class)));
    assertEquals(
        DefaultOverloadSelector.WRAPPER_EPSILON,
        DefaultOverloadSelector.distance(TestWrapperData.class, data),
        0);
  }

  @Test
  public void distance_wantSuperClassOfInnerWrapper_returnEpsilonPlus2() {
    Data data = new IrrelevantWrapperData(new ExtendedTestWrapperData(mock(Container.class)));
    assertEquals(
        2 + DefaultOverloadSelector.WRAPPER_EPSILON,
        DefaultOverloadSelector.distance(TestWrapperData.class, data),
        0);
  }

  @Test
  public void distance_wantWrappedDataClass_returnsEpsilon() {
    Data data = new IrrelevantWrapperData(new ExtendedTestWrapperData(DATA_IMPL));
    assertEquals(
        DefaultOverloadSelector.WRAPPER_EPSILON,
        DefaultOverloadSelector.distance(DATA_IMPL.getClass(), data),
        0);
  }

  @Test
  @Ignore("b/379148650 JDK upgrade changes the behavior of test.")
  public void distance_wantSuperOfWrapperDataClass_returnsEpsilonPlus2() {
    Data data = new IrrelevantWrapperData(new ExtendedTestWrapperData(EXTENDS_DATA_IMPL));
    assertEquals(
        2 + DefaultOverloadSelector.WRAPPER_EPSILON,
        DefaultOverloadSelector.distance(DATA_IMPL.getClass(), data),
        0);
  }

  @Test
  public void distance_getInterfaceOfWrappedData_return1() {
    Data data =
        new IrrelevantWrapperData(
            new ExtendedTestWrapperData(mock(Container.class, Answers.CALLS_REAL_METHODS)));
    assertEquals(1.0, DefaultOverloadSelector.distance(Container.class, data), 0);
  }

  @Test
  public void distance_wrapperSequenceDoesMatter_outMostGets0() {
    Data data = new TestWrapperData(new IrrelevantWrapperData(mock(Container.class)));
    assertEquals(0, DefaultOverloadSelector.distance(TestWrapperData.class, data), 0);
  }

  @Test
  public void distance_insideWrapperSequenceDoesNotMatter() {
    Data data1 =
        new IrrelevantWrapperData(
            new IrrelevantWrapperData(new TestWrapperData(mock(Container.class))));
    Data data2 =
        new IrrelevantWrapperData(
            new TestWrapperData(new IrrelevantWrapperData(mock(Container.class))));
    assertEquals(
        DefaultOverloadSelector.distance(TestWrapperData.class, data2),
        DefaultOverloadSelector.distance(TestWrapperData.class, data1),
        0);
  }

  @Test
  public void distance_emptySigEmptyArgs_returns0() {
    Signature sig = new Signature(DEFAULT_PKG_NAME, "test", ImmutableList.of(), false);
    Data[] args = new Data[0];

    assertEquals(0, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_emptySigTooManyArgs_returnsInfinite() {
    Signature sig = new Signature(DEFAULT_PKG_NAME, "test", ImmutableList.of(), false);
    Data[] args = new Data[] {mockD(Data.class) /* extra arg */};

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_rightArgs_returns0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Collections.singletonList(DATA_IMPL.getClass()), false);
    Data[] args = new Data[] {DATA_IMPL /* 0 */};

    assertEquals(0, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_distantArgs_returnsSum() {
    Signature sig =
        new Signature(DEFAULT_PKG_NAME, "test", Arrays.asList(Data.class, Data.class), false);
    Data[] args = new Data[] {CONTAINER_IMPL /* 2 */, DATA_IMPL /* 1 */};

    assertEquals(3, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_nullArgs_skipsNull() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(DATA_IMPL.getClass(), Data.class), false);
    Data[] args = new Data[] {DATA_IMPL /* 0 */, null /* 0 */};

    assertEquals(0, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_omittedVariadicArg_countsAs0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args = new Data[] {CONTAINER_IMPL /* 1 */};

    assertEquals(1, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_wrongVariadicArg_countsAsInfinite() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args = new Data[] {CONTAINER_IMPL /* 1 */, mockD(Primitive.class) /* inf */};

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_wrongVariadicArrayArg_countsAsInfinite() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args = new Data[] {CONTAINER_IMPL /* 1 */, arrayOf(mockD(Primitive.class) /* inf */)};

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_wrongVariadicExtraArg_countsAsInfinite() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args =
        new Data[] {
          CONTAINER_IMPL /* 1 */, CONTAINER_IMPL /* 1 */, mockD(Primitive.class) /* inf */
        };

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_rightVariadicArg_countsAs0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME,
            "test",
            Arrays.asList(Container.class, CONTAINER_IMPL.getClass()),
            true);
    Data[] args = new Data[] {CONTAINER_IMPL /* 1 */, CONTAINER_IMPL /* 0 */};

    assertEquals(1, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_rightVariadicArrayArg_countsAs0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME,
            "test",
            Arrays.asList(Container.class, CONTAINER_IMPL.getClass()),
            true);
    Data[] args = new Data[] {CONTAINER_IMPL /* 1 */, arrayOf(CONTAINER_IMPL /* 0 */)};

    assertEquals(1.1, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_rightVariadicExtraArg_countsAs0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args =
        new Data[] {
          CONTAINER_IMPL /* 1 */, CONTAINER_IMPL /* 1 (ignored) */, CONTAINER_IMPL /* 1 */
        };
    // Note the last two above are for the variadic param, so only one is counted.

    assertEquals(2, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  @Ignore("b/379148650 JDK upgrade changes the behavior of test.")
  public void distance_distantVariadicArg_takesMax() {
    Signature sig =
        new Signature(DEFAULT_PKG_NAME, "test", Arrays.asList(Data.class, Data.class), true);
    Data[] args =
        new Data[] {DATA_IMPL /* 1 */, EXTENDS_DATA_IMPL, /* 2 */ DATA_IMPL /* 1 (ignored) */};

    assertEquals(3, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  @Ignore("b/379148650 JDK upgrade changes the behavior of test.")
  public void distance_multipleDistantVariadicAndRegularArgs_takesMax() {
    Signature sig =
        new Signature(DEFAULT_PKG_NAME, "test", Arrays.asList(Data.class, Data.class), true);
    Data[] args =
        new Data[] {
          EXTENDS_DATA_IMPL /* 2 */,

          // Variadics should all count as a single score of 2
          EXTENDS_DATA_IMPL /* 2 (ignored) */,
          EXTENDS_DATA_IMPL /* 2 (ignored) */,
          EXTENDS_DATA_IMPL /* 2 */,
        };

    assertEquals(4, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_wrongVariadicArg_takesMax() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args =
        new Data[] {CONTAINER_IMPL /* 1 */, CONTAINER_IMPL /* 1 (ignored) */, DATA_IMPL /* inf */};

    assertEquals(Double.POSITIVE_INFINITY, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_nullArg_returns0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), false);
    Data[] args = new Data[] {null, null};

    assertEquals(0, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  public void distance_nullVariadicArg_returns0() {
    Signature sig =
        new Signature(
            DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Container.class), true);
    Data[] args = new Data[] {null, null, null};

    assertEquals(0, DefaultOverloadSelector.distance(sig, args), 0);
  }

  @Test
  @Ignore("b/379148650 JDK upgrade changes the behavior of test.")
  public void distance_partialNullVariadicArg_returnsMax() {
    Signature sig =
        new Signature(DEFAULT_PKG_NAME, "test", Arrays.asList(Container.class, Data.class), true);
    Data[] args = new Data[] {null, null, null, EXTENDS_DATA_IMPL /* 2 */};
    double distance = DefaultOverloadSelector.distance(sig, args);
    assertEquals(2, distance, 0);
  }

  @Test
  public void select_noCandidates_throws() {
    OverloadSelector os = new DefaultOverloadSelector();

    assertThrows(
        NoMatchingOverloadsException.class, () -> os.select(ImmutableList.of(), new Data[0]));
  }

  @Test
  public void select_noMatches_throws() {
    OverloadSelector os = new DefaultOverloadSelector();

    assertThrows(
        NoMatchingOverloadsException.class,
        () -> os.select(Collections.singletonList(mockFn(Container.class)), new Data[0]));
  }

  @Test
  public void select_ambiguousMatches_throws() {
    OverloadSelector os = new DefaultOverloadSelector();
    assertThrows(
        MultipleMatchingOverloadsException.class,
        () ->
            os.select(
                ImmutableList.of(
                    mockFn(Data.class, Container.class), mockFn(Container.class, Data.class)),
                new Data[] {mockD(Container.class), mockD(Container.class)}));
  }

  @Test
  public void select_singleValidCandidate_returnsIt() {
    OverloadSelector os = new DefaultOverloadSelector();

    CallableFunction selected =
        os.select(
            ImmutableList.of(mockFn("right", Container.class, Container.class)),
            new Data[] {mockD(Container.class), mockD(Container.class)});

    assertEquals("right", selected.getSignature().getName());
  }

  @Test
  public void select_multipleMismatchingCandidates_throws() {
    OverloadSelector os = new DefaultOverloadSelector();
    assertThrows(
        NoMatchingOverloadsException.class,
        () ->
            os.select(
                ImmutableList.of(
                    mockFn("wrong", Primitive.class, Container.class),
                    mockFn("wrong", Container.class, Primitive.class)),
                new Data[] {mockD(Container.class), mockD(Container.class)}));
  }

  @Test
  public void select_validCandidates_returnsClosest() {
    OverloadSelector os = new DefaultOverloadSelector();

    // Note, in practice, overloads will probably have the same name.
    CallableFunction selected =
        os.select(
            ImmutableList.of(
                mockFn("right", Container.class, Container.class),
                mockFn("wrong", Container.class, Data.class)),
            new Data[] {mockD(Container.class), mockD(Container.class)});

    assertEquals("right", selected.getSignature().getName());
  }

  @Test
  public void select_variadicAmbiguousCandidates_throws() {
    OverloadSelector os = new DefaultOverloadSelector();
    MultipleMatchingOverloadsException ex =
        assertThrows(
            MultipleMatchingOverloadsException.class,
            () ->
                os.select(
                    ImmutableList.of(
                        mockFn("wrong", true, Container.class, Primitive.class),
                        mockFn("wrong", Container.class),
                        mockFn("wrong", Container.class, Primitive.class)),
                    new Data[] {mockD(Container.class)}));
    assertThat(ex)
        .hasMessageThat()
        .contains("Matching overloads:\n\twrong(Container, Primitive...)\n\twrong(Container)");
  }

  @Test
  public void select_variadicNonAmbiguousCandidates_returnsClosest() {
    OverloadSelector os = new DefaultOverloadSelector();

    CallableFunction selected =
        os.select(
            ImmutableList.of(
                mockFn("right", true, Container.class, Primitive.class),
                mockFn("wrong", true, Container.class, Data.class)),
            new Data[] {mockD(Container.class), mockD(Primitive.class), mockD(Primitive.class)});
    assertEquals("right", selected.getSignature().getName());

    selected =
        os.select(
            ImmutableList.of(
                mockFn("right", true, Container.class, Data.class),
                mockFn("wrong", true, Container.class, Primitive.class)),
            new Data[] {mockD(Container.class), mockD(Primitive.class), mockD(Array.class)});
    assertEquals("right", selected.getSignature().getName());
  }

  private abstract static class TestDataImpl1 implements Data {}

  private abstract static class TestDataImpl2 implements Data {}

  /** Wrapper on mock with CALLS_REAL_METHODS as default. */
  private static <T extends Data> T mockD(Class<T> clazz) {
    return mock(clazz, Answers.CALLS_REAL_METHODS);
  }
}
