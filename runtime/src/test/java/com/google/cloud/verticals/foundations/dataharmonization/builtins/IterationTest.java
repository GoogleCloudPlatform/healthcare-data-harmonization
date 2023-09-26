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

package com.google.cloud.verticals.foundations.dataharmonization.builtins;

import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.datasetOf;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for iteration. */
@RunWith(JUnit4.class)
public class IterationTest {
  private static Closure mockClosure(int numFree, Function<List<Data>, Data> fn) {
    return mockClosure(new ArrayList<>(), numFree, fn);
  }

  private static Closure mockClosure(
      List<Data> bindings, int numFree, Function<List<Data>, Data> fn) {
    Closure closure = mock(Closure.class);
    when(closure.getNumFreeParams()).then(i -> numFree - bindings.size());
    when(closure.bindNextFreeParameter(any()))
        .then(
            i -> {
              List<Data> newBindings = new ArrayList<>(bindings);
              newBindings.add(i.getArgument(0));
              return mockClosure(newBindings, numFree - 1, fn);
            });
    when(closure.execute(any())).then(i -> fn.apply(bindings));

    return closure;
  }

  @Test
  public void iterate_noArgs_null() {
    assertTrue(Iteration.iterate(mock(Closure.class)).isNullOrEmpty());
  }

  @Test
  public void iterate_nulls_null() {
    assertTrue(Iteration.iterate(mock(Closure.class), NullData.instance).isNullOrEmpty());
    assertTrue(
        Iteration.iterate(mock(Closure.class), NullData.instance, NullData.instance)
            .isNullOrEmpty());
  }

  @Test
  public void iterate_unevenlySizedArrays_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Iteration.iterate(
                new TestContext(),
                mockClosure(2, x -> mock(Data.class)),
                arrayOf(mock(Data.class), 1),
                arrayOf(mock(Data.class), 3)));
  }

  @Test
  public void iterate_invalidClosure_throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            Iteration.iterate(
                new TestContext(),
                mock(Closure.class), // No free params - invalid
                arrayOf(mock(Data.class), 3)));
  }

  @Test
  public void iterate_emptyArray_null() {
    Array result = Iteration.iterate(new TestContext(), mockClosure(1, x -> null), emptyArray());
    assertTrue(result.isNullOrEmpty());
  }

  @Test
  public void iterate_zippedWithEmptyArrays_null() {
    Array result =
        Iteration.iterate(
            new TestContext(), mockClosure(3, x -> null), emptyArray(), emptyArray(), emptyArray());
    assertTrue(result.isNullOrEmpty());
  }

  @Test
  public void iterate_singleArray_iterates() {
    Data value = mock(Data.class);
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(1, x -> value),
            arrayOf(
                testDTI().primitiveOf(1.), testDTI().primitiveOf(2.), testDTI().primitiveOf(3.)));
    assertDCAPEquals(arrayOf(value, 3), result);
  }

  @Test
  public void iterate_mixedNulls_filtersNulls() {
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(1, x -> x.get(0)),
            arrayOf(testDTI().primitiveOf(1.), NullData.instance, testDTI().primitiveOf(3.)));
    assertDCAPEquals(arrayOf(testDTI().primitiveOf(1.), testDTI().primitiveOf(3.)), result);
  }

  @Test
  public void iterate_onlyNulls_filtersNulls() {
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(1, x -> x.get(0)),
            arrayOf(NullData.instance, NullData.instance, NullData.instance));
    assertDCAPEquals(emptyArray(), result);
  }

  @Test
  public void iterate_manyNulls_filtersNulls() {
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(1, x -> x.get(0)),
            arrayOf(NullData.instance, testDTI().primitiveOf(1.), NullData.instance));
    assertDCAPEquals(arrayOf(testDTI().primitiveOf(1.)), result);
  }

  @Test
  public void iterate_zippedArray_iterates() {
    Array one =
        arrayOf(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.), testDTI().primitiveOf(3.));
    Array two =
        arrayOf(testDTI().primitiveOf(10.), testDTI().primitiveOf(20.), testDTI().primitiveOf(30.));
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                2,
                x ->
                    testDTI()
                        .primitiveOf(x.get(0).asPrimitive().num() + x.get(1).asPrimitive().num())),
            one,
            two);
    assertDCAPEquals(
        arrayOf(
            testDTI().primitiveOf(11.), testDTI().primitiveOf(22.0), testDTI().primitiveOf(33.0)),
        result);
  }

  @Test
  public void iterate_zippedArrayWithEmpty_iterates() {
    Array one =
        arrayOf(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.), testDTI().primitiveOf(3.));
    Array two =
        arrayOf(testDTI().primitiveOf(10.), testDTI().primitiveOf(20.), testDTI().primitiveOf(30.));
    Array result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                3,
                x ->
                    testDTI()
                        .primitiveOf(x.get(0).asPrimitive().num() + x.get(1).asPrimitive().num())),
            one,
            two,
            emptyArray());
    assertDCAPEquals(
        arrayOf(
            testDTI().primitiveOf(11.), testDTI().primitiveOf(22.0), testDTI().primitiveOf(33.0)),
        result);
  }

  @Test
  public void iterate_dataset_maps() {
    Data sampleResult = mock(Data.class);
    Dataset input =
        datasetOf(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.), testDTI().primitiveOf(3.));
    Dataset result = Iteration.iterate(new TestContext(), mockClosure(1, x -> sampleResult), input);
    assertDCAPEquals(datasetOf(sampleResult, sampleResult, sampleResult), result);
  }

  /**
   * let a = {k1: 100, k2: 200, k3: 300} and x = 25 then the given function (closure) will be called
   * like:
   *
   * <ul>
   *   <li>fn(a[k1], 25)
   *   <li>fn(a[k2], 25)
   *   <li>fn(a[k3], 25)
   * </ul>
   */
  @Test
  public void iterate_container_iterates() {
    ImmutableMap<String, Data> a =
        ImmutableMap.of(
            "k1",
            testDTI().primitiveOf(100.),
            "k2",
            testDTI().primitiveOf(200.),
            "k3",
            testDTI().primitiveOf(300.));
    ImmutableMap<String, Data> ans =
        ImmutableMap.of(
            "k1",
            testDTI().primitiveOf(125.),
            "k2",
            testDTI().primitiveOf(225.),
            "k3",
            testDTI().primitiveOf(325.));
    Container one = testDTI().containerOf(a);
    Container res = testDTI().containerOf(ans);

    Container result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                1, dataList -> testDTI().primitiveOf(dataList.get(0).asPrimitive().num() + 25)),
            one);
    assertDCAPEquals(res, result);
  }

  /**
   * let a = {k1: 100, k3: 300} and b = {k1: 110, k2: 200} and c = {k1: 90}, then the given function
   * will be called like:
   *
   * <ul>
   *   <li>fn(a[k1], b[k1], c[k1], x, y)
   *   <li>fn(a[k2], b[k2], c[k2], x, y)
   *   <li>fn(a[k3], b[k3], c[k3], x, y)
   * </ul>
   */
  @Test
  public void iterate_zippedContainers_iterates() {
    ImmutableMap<String, Data> a =
        ImmutableMap.of("k1", testDTI().primitiveOf(100.), "k3", testDTI().primitiveOf(300.));
    ImmutableMap<String, Data> b =
        ImmutableMap.of("k1", testDTI().primitiveOf(110.), "k2", testDTI().primitiveOf(200.));
    ImmutableMap<String, Data> c = ImmutableMap.of("k1", testDTI().primitiveOf(90.));
    Container one = testDTI().containerOf(a);
    Container two = testDTI().containerOf(b);
    Container three = testDTI().containerOf(c);

    ImmutableMap<String, Data> ans =
        ImmutableMap.of(
            "k1", testDTI().primitiveOf(300.),
            "k2", testDTI().primitiveOf(200.),
            "k3", testDTI().primitiveOf(300.));
    Container res = testDTI().containerOf(ans);

    Container result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                3,
                dataList ->
                    testDTI()
                        .primitiveOf(
                            num(dataList.get(0)) + num(dataList.get(1)) + num(dataList.get(2)))),
            one,
            two,
            three);
    assertDCAPEquals(res, result);
  }

  @Test
  public void iterate_zippedEmptyContainers_iterates() {
    Container one = testDTI().emptyContainer();
    Container two = testDTI().emptyContainer();
    testZippedContainersWithEmptyResultingContainer(one, two);
  }

  @Test
  public void iterate_zippedEmptyAndFullContainers_iterates() {
    ImmutableMap<String, Data> a =
        ImmutableMap.of("k1", testDTI().primitiveOf(100.), "k3", testDTI().primitiveOf(300.));
    ImmutableMap<String, Data> b =
        ImmutableMap.of("k1", testDTI().primitiveOf(110.), "k2", testDTI().primitiveOf(200.));
    Container one = testDTI().containerOf(a);
    Container two = testDTI().containerOf(b);

    ImmutableMap<String, Data> ans =
        ImmutableMap.of(
            "k1", testDTI().primitiveOf(210.),
            "k2", testDTI().primitiveOf(200.),
            "k3", testDTI().primitiveOf(300.));
    Container res = testDTI().containerOf(ans);
    Container result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                3,
                dataList ->
                    testDTI()
                        .primitiveOf(
                            num(dataList.get(0)) + num(dataList.get(1)) + num(dataList.get(2)))),
            one,
            two,
            testDTI().emptyContainer());
    assertDCAPEquals(res, result);
  }

  private static double num(Data data) {
    if (data.isNullOrEmpty()) {
      return 0;
    }
    return data.asPrimitive().num();
  }

  private void testZippedContainersWithEmptyResultingContainer(Container one, Container two) {
    Container three = testDTI().emptyContainer();
    Container res = testDTI().emptyContainer();

    Container result =
        Iteration.iterate(
            new TestContext(),
            mockClosure(
                3,
                dataList ->
                    testDTI()
                        .primitiveOf(
                            num(dataList.get(0)) + num(dataList.get(1)) + num(dataList.get(2)))),
            one,
            two,
            three);
    assertDCAPEquals(res, result);
  }
}
