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

import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.arrayOf;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.groupBy;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.join;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.listLen;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.range;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.reduce;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.sortBy;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.sortByDescending;
import static com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns.uniqueBy;
import static com.google.cloud.verticals.foundations.dataharmonization.data.impl.TestDataTypeImplementation.testDTI;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.arbitrary;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyArray;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.emptyContainer;
import static com.google.cloud.verticals.foundations.dataharmonization.mock.MockData.nul;
import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.options.SingleNullArrayExperiment;
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.impl.JsonSerializerDeserializer;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultOverloadSelector;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.TestContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockClosure;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig.Option;
import com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Answers;
import org.mockito.stubbing.Answer;

/** Tests for ArrayFns. */
@RunWith(JUnit4.class)
public class ArrayFnsTest {

  private static final JsonSerializerDeserializer JSON = new JsonSerializerDeserializer();

  private final RuntimeContext context = new TestContext();
  private Closure selfExtractor; // returns given Data object

  @Before
  public void setup() {
    // mock basic self-returning Closure function for use in sortBy testing
    selfExtractor = mock(Closure.class);
    when(selfExtractor.bindNextFreeParameter(any()))
        .thenAnswer(
            (Answer<Closure>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  Closure tmpClosure = mock(Closure.class);
                  when(tmpClosure.execute(any())).thenReturn((Data) args[0]);
                  return tmpClosure;
                });
  }

  @Test
  public void arrayOf_emptyArray() {
    Array expected = testDTI().emptyArray();
    Data actual = arrayOf(context);
    assertEquals(expected, actual);
  }

  @Test
  public void arrayOf_singleNullData_experiment() {
    Array expected = testDTI().arrayOf(nul());
    RuntimeContext context = new TestContext();
    context = new SingleNullArrayExperiment().enable(context, Option.getDefaultInstance());
    Data actual = arrayOf(context, nul());
    assertEquals(expected, actual);
  }

  @Test
  public void arrayOf_singleNullData() {
    Array expected = testDTI().emptyArray();
    Data actual = arrayOf(context, nul());
    assertEquals(expected, actual);
  }

  @Test
  public void arrayOf_singleItem() {
    Array expected = testDTI().arrayOf(testDTI().primitiveOf(1.));
    Data actual = arrayOf(context, testDTI().primitiveOf(1.));
    assertEquals(expected, actual);
  }

  @Test
  public void arrayOf_multipleNullData() {
    Array expected = testDTI().arrayOf(nul(), nul());
    Data actual = arrayOf(context, nul(), nul());
    assertEquals(expected, actual);
  }

  @Test
  public void arrayOf_multipleItems() {
    Array expected = testDTI().arrayOf(testDTI().primitiveOf(1.), testDTI().primitiveOf(2.));
    Data actual = arrayOf(context, testDTI().primitiveOf(1.), testDTI().primitiveOf(2.));
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_emptyArray() {
    Array toSort = testDTI().emptyArray();
    Array expected = testDTI().emptyArray();
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayWithSinglePrimitiveDouble() {
    Array toSort = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(5.)));
    Array expected = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(5.)));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfPrimitiveDoubles() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(5.),
                    testDTI().primitiveOf(102.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(12.)));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(5.),
                    testDTI().primitiveOf(12.),
                    testDTI().primitiveOf(102.)));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_sortedArrayOfPrimitiveDoubles() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfPrimitiveStrings() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf("abc"),
                    testDTI().primitiveOf("bca"),
                    testDTI().primitiveOf("cab"),
                    testDTI().primitiveOf("aab")));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf("aab"),
                    testDTI().primitiveOf("abc"),
                    testDTI().primitiveOf("bca"),
                    testDTI().primitiveOf("cab")));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfPrimitiveBooleans() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(true),
                    testDTI().primitiveOf(false),
                    testDTI().primitiveOf(true),
                    testDTI().primitiveOf(false)));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(false),
                    testDTI().primitiveOf(false),
                    testDTI().primitiveOf(true),
                    testDTI().primitiveOf(true)));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfArrays_firstElementAsKey() {
    Array first =
        testDTI()
            .arrayOf(ImmutableList.of(testDTI().primitiveOf(3.), testDTI().primitiveOf("bar")));
    Array second =
        testDTI()
            .arrayOf(ImmutableList.of(testDTI().primitiveOf(5.), testDTI().primitiveOf("foo")));
    Array third =
        testDTI()
            .arrayOf(ImmutableList.of(testDTI().primitiveOf(12.), testDTI().primitiveOf("oof")));
    Array fourth =
        testDTI()
            .arrayOf(ImmutableList.of(testDTI().primitiveOf(102.), testDTI().primitiveOf("baz")));
    Array toSort = testDTI().arrayOf(ImmutableList.of(second, first, fourth, third));
    Array expected = testDTI().arrayOf(ImmutableList.of(first, second, third, fourth));
    Closure basicArrayExtractor = mock(Closure.class); // returns first Array element
    when(basicArrayExtractor.bindNextFreeParameter(any()))
        .thenAnswer(
            (Answer<Closure>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  Closure tmpClosure = mock(Closure.class);
                  when(tmpClosure.execute(any())).thenReturn(((Array) args[0]).getElement(0));
                  return tmpClosure;
                });
    Data actual = sortBy(context, toSort, basicArrayExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfContainers_idFieldAsKey() {
    Container first =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("id", testDTI().primitiveOf(3.))
                    .put("data", testDTI().primitiveOf("bar"))
                    .buildOrThrow());
    Container second =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("id", testDTI().primitiveOf(5.))
                    .put("data", testDTI().primitiveOf("foo"))
                    .buildOrThrow());
    Container third =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("id", testDTI().primitiveOf(12.))
                    .put("data", testDTI().primitiveOf("oof"))
                    .buildOrThrow());
    Container fourth =
        testDTI()
            .containerOf(
                ImmutableMap.<String, Data>builder()
                    .put("id", testDTI().primitiveOf(102.))
                    .put("data", testDTI().primitiveOf("baz"))
                    .buildOrThrow());
    Array toSort = testDTI().arrayOf(ImmutableList.of(second, first, fourth, third));
    Array expected = testDTI().arrayOf(ImmutableList.of(first, second, third, fourth));
    Closure basicContainerExtractor = mock(Closure.class); // returns "id" field value
    when(basicContainerExtractor.bindNextFreeParameter(any()))
        .thenAnswer(
            (Answer<Closure>)
                invocation -> {
                  Object[] args = invocation.getArguments();
                  Closure tmpClosure = mock(Closure.class);
                  when(tmpClosure.execute(any())).thenReturn(((Container) args[0]).getField("id"));
                  return tmpClosure;
                });
    Data actual = sortBy(context, toSort, basicContainerExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfPrimitiveDoublesAndNullData_nullDataFirstOrder() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    NullData.instance,
                    testDTI().primitiveOf(3.),
                    NullData.instance));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    NullData.instance,
                    NullData.instance,
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf(3.)));
    Data actual = sortBy(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortBy_arrayOfMixedPrimitiveDoublesAndStrings_throws() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().primitiveOf("two"),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));
    assertThrows(UnsupportedOperationException.class, () -> sortBy(context, toSort, selfExtractor));
  }

  @Test
  public void sortBy_arrayOfMixedPrimitiveDoublesAndArrays_throws() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(2.))),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(4.)));
    assertThrows(UnsupportedOperationException.class, () -> sortBy(context, toSort, selfExtractor));
  }

  @Test
  public void sortByDescending_arrayOfPrimitiveDoubles() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(5.),
                    testDTI().primitiveOf(102.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(12.)));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(102.),
                    testDTI().primitiveOf(12.),
                    testDTI().primitiveOf(5.),
                    testDTI().primitiveOf(3.)));
    Data actual = sortByDescending(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortByDescending_sortedArrayOfPrimitiveDoubles() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(4.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(1.)));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(4.),
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(2.),
                    testDTI().primitiveOf(1.)));
    Data actual = sortByDescending(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void sortByDescending_arrayOfPrimitiveDoublesAndNullData_nullDataFirstOrder() {
    Array toSort =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.),
                    NullData.instance,
                    testDTI().primitiveOf(3.),
                    NullData.instance));
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(3.),
                    testDTI().primitiveOf(1.),
                    NullData.instance,
                    NullData.instance));
    Data actual = sortByDescending(context, toSort, selfExtractor);
    assertEquals(expected, actual);
  }

  @Test
  public void unique_array_duplicateRemoved() {
    List<Data> data =
        Arrays.asList(
            testDTI().primitiveOf("a"),
            testDTI().primitiveOf("b"),
            testDTI().primitiveOf(1.1),
            testDTI().primitiveOf("a"),
            testDTI().primitiveOf(1.1));
    Array input = testDTI().arrayOf(data);
    Data output = ArrayFns.unique(new TestContext(), input);
    assertTrue(output.isArray());
    Array ary = output.asArray();
    assertEquals(3, ary.size());
    assertEquals(
        ImmutableSet.of(
            testDTI().primitiveOf("a"), testDTI().primitiveOf("b"), testDTI().primitiveOf(1.1)),
        ImmutableSet.of(ary.getElement(0), ary.getElement(1), ary.getElement(2)));
  }

  @Test
  public void unique_array_orderPreserved() {
    // Generate a list of unsorted numbers. The chance of them being randomly sorted correctly is in
    // the order of 1/(1000!) so this test should be pretty reliable.
    List<Data> inputUnsortedNums =
        IntStream.range(0, 1000)
            .mapToDouble(Double::valueOf)
            .mapToObj(testDTI()::primitiveOf)
            // Sort "randomly" just to ensure unique isn't doing any sneaky sorting.
            .sorted(Comparator.comparing(Object::hashCode))
            .collect(Collectors.toList());

    Array inputArray = testDTI().arrayOf(inputUnsortedNums);
    Data actual = ArrayFns.unique(new TestContext(), inputArray);
    assertTrue(actual.isArray());
    Array actualArray = actual.asArray();
    assertEquals(1000, actualArray.size());
    for (int i = 0; i < inputUnsortedNums.size(); i++) {
      assertEquals(inputUnsortedNums.get(i), actualArray.getElement(i));
    }
  }

  @Test
  public void uniqueBy_duplicatesRemoved_existingVersionsOfDuplicateEntriesMaintainedWithOrder() {
    Data entryOne =
        toData("{ \"key1\": \"0.\", \"key2\": \"zero\", \"valueField\": \"valueOne\" }");
    Data entryTwo =
        toData("{ \"key1\": \"1.\", \"key2\": \"one\",  \"valueField\": \"valueTwo\" }");
    Data entryThree =
        toData("{ \"key1\": \"0.\", \"key2\": \"zero\", \"valueField\": \"valueThree\" }");
    Data entryFour =
        toData("{ \"key1\": \"1.\", \"key2\": \"one\",  \"valueField\": \"valueFour\" }");
    Data entryFive =
        toData("{ \"key1\": \"2.\", \"key2\": \"one\",  \"valueField\": \"valueFive\" }");
    Data entrySix =
        toData("{ \"key1\": \"1.\", \"key2\": \"two\",  \"valueField\": \"valueSix\" }");
    Data entrySeven =
        toData("{ \"key1\": \"0.\", \"key2\": \"zero\", \"valueField\": \"valueSeven\" }");

    Array inputArray =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    entryOne, entryTwo, entryThree, entryFour, entryFive, entrySix, entrySeven));
    Array expected = testDTI().arrayOf(ImmutableList.of(entryOne, entryTwo, entryFive, entrySix));

    Closure keySelector = twoKeySelector();

    assertEquals(expected, uniqueBy(context, inputArray, keySelector));
  }

  @Test
  public void uniqueBy_noDuplicates_returnsInputArrayExactly() {
    Data entryOne =
        toData("{ \"key1\": \"0.\", \"key2\": \"oof\", \"valueField\": \"valueOne\"   }");
    Data entryTwo =
        toData("{ \"key1\": \"2.\", \"key2\": \"oof\", \"valueField\": \"valueTwo\"   }");
    Data entryThree =
        toData("{ \"key1\": \"3.\", \"key2\": \"oof\", \"valueField\": \"valueThree\" }");
    Data entryFour =
        toData("{ \"key1\": \"4.\", \"key2\": \"oof\", \"valueField\": \"valueFour\"  }");

    Array inputArray =
        testDTI().arrayOf(ImmutableList.of(entryOne, entryTwo, entryThree, entryFour));
    Array expected = testDTI().arrayOf(ImmutableList.of(entryOne, entryTwo, entryThree, entryFour));

    Closure keySelector = twoKeySelector();

    assertEquals(expected, uniqueBy(context, inputArray, keySelector));
  }

  @Test
  public void uniqueBy_emptyArray_returnsEmptyArray() {
    Array emptyArray = testDTI().emptyArray();
    Closure keySelector = twoKeySelector();

    assertEquals(testDTI().emptyArray(), uniqueBy(context, emptyArray, keySelector));
  }

  @Test
  public void where_emptyArray_noop() {
    Array result = ArrayFns.where(new TestContext(), emptyArray(), mock(Closure.class));
    assertTrue(result.isNullOrEmpty());
  }

  @Test
  public void where_closure_filters() {
    Closure filter = mock(Closure.class);
    Data[] elem = new Data[1];
    when(filter.bindNextFreeParameter(any()))
        .then(
            i -> {
              elem[0] = i.getArgument(0);
              return filter;
            });
    when(filter.execute(any())).then(i -> testDTI().primitiveOf(elem[0].asPrimitive().num() > 1.0));
    Array result =
        ArrayFns.where(
            new TestContext(),
            arrayOf(
                new TestContext(),
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf(2.0),
                testDTI().primitiveOf(3.0)),
            filter);

    assertDCAPEquals(
        arrayOf(new TestContext(), testDTI().primitiveOf(2.0), testDTI().primitiveOf(3.0)), result);
  }

  @Test
  public void where_closure_filtersAll() {
    Closure filter = mock(Closure.class);
    when(filter.bindNextFreeParameter(any())).thenReturn(filter);
    when(filter.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Array result =
        ArrayFns.where(
            new TestContext(),
            arrayOf(
                new TestContext(),
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf(2.0),
                testDTI().primitiveOf(3.0)),
            filter);

    assertDCAPEquals(emptyArray(), result);
  }

  @Test
  public void where_closure_filtersNone() {
    Closure filter = mock(Closure.class);
    when(filter.bindNextFreeParameter(any())).thenReturn(filter);
    when(filter.execute(any())).thenReturn(testDTI().primitiveOf(true));
    Array result =
        ArrayFns.where(
            new TestContext(),
            arrayOf(
                new TestContext(),
                testDTI().primitiveOf(1.0),
                testDTI().primitiveOf(2.0),
                testDTI().primitiveOf(3.0)),
            filter);

    assertDCAPEquals(
        arrayOf(
            new TestContext(),
            testDTI().primitiveOf(1.0),
            testDTI().primitiveOf(2.0),
            testDTI().primitiveOf(3.0)),
        result);
  }

  @Test
  public void whereContainer_emptyContainer_noop() {
    Container result = ArrayFns.where(new TestContext(), emptyContainer(), mock(Closure.class));
    assertTrue(result.isNullOrEmpty());
  }

  @Test
  public void whereContainer_closure_filters() {
    Closure filter = mock(Closure.class);
    Data[] elem = new Data[1];
    when(filter.bindNextFreeParameter(any()))
        .then(
            i -> {
              elem[0] = i.getArgument(0);
              return filter;
            });
    when(filter.execute(any()))
        .then(
            i ->
                testDTI()
                    .primitiveOf(
                        !elem[0]
                            .asContainer()
                            .getField("field")
                            .asPrimitive()
                            .string()
                            .equals("delete")));

    Container inputContainer =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "keepA",
                    testDTI().primitiveOf("keepA Value"),
                    "keepB",
                    testDTI().primitiveOf("keepB Value"),
                    "delete",
                    testDTI().primitiveOf("goodbye")));

    Container actual = ArrayFns.where(new TestContext(), inputContainer, filter);
    Container expected =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "keepA",
                    testDTI().primitiveOf("keepA Value"),
                    "keepB",
                    testDTI().primitiveOf("keepB Value")));

    assertDCAPEquals(expected, actual);
  }

  @Test
  public void whereContainer_closure_filtersAll() {
    Closure filter = mock(Closure.class);
    when(filter.bindNextFreeParameter(any())).thenReturn(filter);
    when(filter.execute(any())).thenReturn(testDTI().primitiveOf(false));
    Container actual =
        ArrayFns.where(
            new TestContext(),
            testDTI()
                .containerOf(
                    ImmutableMap.of(
                        "A",
                        testDTI().primitiveOf("aaa"),
                        "B",
                        testDTI().primitiveOf("bbb"),
                        "C",
                        testDTI().primitiveOf("ccc"))),
            filter);

    assertDCAPEquals(emptyContainer(), actual);
  }

  @Test
  public void whereContainer_closure_filtersNone() {
    Closure filter = mock(Closure.class);
    when(filter.bindNextFreeParameter(any())).thenReturn(filter);
    when(filter.execute(any())).thenReturn(testDTI().primitiveOf(true));
    Container sampleContainer =
        testDTI()
            .containerOf(
                ImmutableMap.of(
                    "A",
                    testDTI().primitiveOf("aaa"),
                    "B",
                    testDTI().primitiveOf("bbb"),
                    "C",
                    testDTI().primitiveOf("ccc")));
    Container actual = ArrayFns.where(new TestContext(), sampleContainer, filter);

    assertDCAPEquals(sampleContainer, actual);
  }

  @Test
  public void whereNull_returnsNull() {
    NullData result = ArrayFns.where(NullData.instance, mock(Closure.class));
    assertTrue(result.isNullOrEmpty());

    // Ensure it gets chosen when null is given.
    CallableFunction got =
        new DefaultOverloadSelector()
            .select(
                JavaFunction.ofPluginFunctionsInClass(ArrayFns.class, Builtins.PACKAGE_NAME)
                    .stream()
                    .filter(fn -> fn.getSignature().getName().equals("where"))
                    .collect(Collectors.toList()),
                new Data[] {NullData.instance, mock(Closure.class, Answers.CALLS_REAL_METHODS)});
    assertNotNull(got);
  }

  @Test
  public void groupBy_null_returnsEmpty() {
    Closure closure = mock(Closure.class);
    assertThat(groupBy(new TestContext(), NullData.instance, closure).isNullOrEmpty()).isTrue();
  }

  @Test
  public void groupBy_closure_groupsAll() {
    Closure closure = mock(Closure.class);
    when(closure.bindNextFreeParameter(any())).thenReturn(closure);
    Data key = testDTI().containerOf(ImmutableMap.of("hello", testDTI().primitiveOf("world")));
    when(closure.execute(any())).thenReturn(key);

    Array array =
        testDTI()
            .arrayOf(
                testDTI().primitiveOf(1.0), testDTI().primitiveOf(2.0), testDTI().primitiveOf(3.0));

    Data result = groupBy(new TestContext(), array, closure);

    assertDCAPEquals(
        testDTI().arrayOf(testDTI().containerOf(ImmutableMap.of("key", key, "elements", array))),
        result);
  }

  @Test
  public void groupBy_closure_groups() {
    Closure closure = mock(Closure.class);
    Data[] elem = new Data[1];
    when(closure.bindNextFreeParameter(any()))
        .then(
            i -> {
              elem[0] = i.getArgument(0);
              return closure;
            });
    when(closure.execute(any())).then(i -> elem[0].asContainer().getField("f1"));

    Container container1 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0), "G", mock(Data.class)));
    Container container2 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(2.0), "R", mock(Data.class)));
    Container container3 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0), "O", mock(Data.class)));
    Container container4 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(2.0), "U", mock(Data.class)));
    Container container5 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(1.0), "P", mock(Data.class)));
    Container container6 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(2.0), "B", mock(Data.class)));
    Container container7 =
        testDTI()
            .containerOf(ImmutableMap.of("f1", testDTI().primitiveOf(2.0), "Y", mock(Data.class)));

    Array inputArray =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    container1,
                    container2,
                    container3,
                    container4,
                    container5,
                    container6,
                    container7));

    Array expectedResult =
        testDTI()
            .arrayOf(
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "key",
                            testDTI().primitiveOf(2.0),
                            "elements",
                            testDTI().arrayOf(container2, container4, container6, container7))),
                testDTI()
                    .containerOf(
                        ImmutableMap.of(
                            "key",
                            testDTI().primitiveOf(1.0),
                            "elements",
                            testDTI().arrayOf(container1, container3, container5))));

    Data result = groupBy(new TestContext(), inputArray, closure);

    assertArraySetEquals(expectedResult, result.asArray());
  }

  private void assertArraySetEquals(Array expected, Array actual) {
    assertThat(ImmutableSet.copyOf(actual.stream().collect(ImmutableList.toImmutableList())))
        .containsExactlyElementsIn(expected.stream().toArray());
  }

  @Test
  public void listLen_presetArray_keepsSize() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(5.0)));
    assertEquals(testDTI().primitiveOf(1.), listLen(new TestContext(), array));
  }

  @Test
  public void listLen_nullPadding_countsInSize() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(5.0)));
    array = array.setElement(123, testDTI().primitiveOf(5.0));
    assertEquals(testDTI().primitiveOf(124.), listLen(new TestContext(), array));
  }

  @Test
  public void listLen_allNulls_countInSize() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(5.0)));
    array = array.setElement(123, NullData.instance);
    assertEquals(testDTI().primitiveOf(124.), listLen(new TestContext(), array));
  }

  @Test
  public void range_startEnd_normal() {
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(2.0),
                    testDTI().primitiveOf(3.0),
                    testDTI().primitiveOf(4.0)));
    Array got = range(new TestContext(), testDTI().primitiveOf(2.0), testDTI().primitiveOf(5.0));
    AssertUtil.assertDCAPEquals(expected, got);
  }

  @Test
  public void range_size_normal() {
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(0.0),
                    testDTI().primitiveOf(1.0),
                    testDTI().primitiveOf(2.0)));
    Array got = range(new TestContext(), testDTI().primitiveOf(3.0));
    AssertUtil.assertDCAPEquals(expected, got);
  }

  @Test
  public void range_startEnd_nonInteger_roundDown() {
    Array expected =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(2.0),
                    testDTI().primitiveOf(3.0),
                    testDTI().primitiveOf(4.0)));
    Array got = range(new TestContext(), testDTI().primitiveOf(2.01), testDTI().primitiveOf(5.99));
    AssertUtil.assertDCAPEquals(expected, got);
  }

  @Test
  public void range_startEnd_startBigger_emtpy() {
    Array expected = testDTI().arrayOf(ImmutableList.of());
    Array got = range(new TestContext(), testDTI().primitiveOf(20.0), testDTI().primitiveOf(5.0));
    AssertUtil.assertDCAPEquals(expected, got);
  }

  @Test
  public void reduce_withNonEmptyArray() {
    Array array =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.0),
                    testDTI().primitiveOf(2.0),
                    testDTI().primitiveOf(3.0)));
    Data value = reduce(new TestContext(), array, sumNumberAccumulator());
    assertEquals(testDTI().primitiveOf(6.0), value);
  }

  @Test
  public void reduce_withEmptyArray() {
    Data value = reduce(new TestContext(), testDTI().emptyArray(), sumNumberAccumulator());
    assertEquals(NullData.instance, value);
  }

  @Test
  public void reduce_withSingleItemArray() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.0)));
    Data value = reduce(new TestContext(), array, sumNumberAccumulator());
    assertEquals(testDTI().primitiveOf(1.0), value);
  }

  @Test
  public void reduce_withNonProcessingClosure_returnsFirstItem() {
    Data expected = testDTI().primitiveOf(1.0);
    Array array = testDTI().arrayOf(ImmutableList.of(expected));
    Data value = reduce(new TestContext(), array, constReturn(expected));
    assertEquals(expected, value);
  }

  @Test
  public void reduce_withSeedAndNonEmptyArray() {
    Array array =
        testDTI()
            .arrayOf(
                ImmutableList.of(
                    testDTI().primitiveOf(1.0),
                    testDTI().primitiveOf(2.0),
                    testDTI().primitiveOf(3.0)));
    Data value =
        reduce(new TestContext(), array, testDTI().primitiveOf(100.0), sumNumberAccumulator());
    assertEquals(testDTI().primitiveOf(106.0), value);
  }

  @Test
  public void reduce_withSeedAndEmptyArray() {
    Data seed = mock(Data.class);
    Data value = reduce(new TestContext(), testDTI().emptyArray(), seed, sumNumberAccumulator());
    assertEquals(seed, value);
  }

  @Test
  public void reduce_withSeedAndSingleItemArray() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.0)));
    Data value =
        reduce(new TestContext(), array, testDTI().primitiveOf(10.0), sumNumberAccumulator());
    assertEquals(testDTI().primitiveOf(11.0), value);
  }

  @Test
  public void reduce_withSeedAndNonProcessingClosure_returnsClosureResult() {
    Array array = testDTI().arrayOf(ImmutableList.of(testDTI().primitiveOf(1.0)));
    Data expected = mock(Data.class);
    Data value = reduce(new TestContext(), array, mock(Data.class), constReturn(expected));
    assertEquals(expected, value);
  }

  @Test
  public void join_emptyArrays_empty() {
    Array got = join(context, emptyArray(), emptyArray(), constReturn(NullData.instance));
    assertThat(got.isNullOrEmpty()).isTrue();
  }

  @Test
  public void join_emptyLeftNonEmptyRight() {
    Array right = testDTI().arrayOf(arbitrary(), arbitrary());
    Array got = join(context, emptyArray(), right, constReturn(NullData.instance));
    assertThat(got.size()).isEqualTo(right.size());
    assertThat(got.getElement(0))
        .isEqualTo(testDTI().arrayOf(NullData.instance, right.getElement(0)));
    assertThat(got.getElement(1))
        .isEqualTo(testDTI().arrayOf(NullData.instance, right.getElement(1)));
  }

  @Test
  public void join_nonEmptyLeftEmptyRight() {
    Array left = testDTI().arrayOf(arbitrary(), arbitrary());
    Array got = join(context, left, emptyArray(), constReturn(NullData.instance));
    assertThat(got.size()).isEqualTo(left.size());
    assertThat(got.getElement(0))
        .isEqualTo(testDTI().arrayOf(left.getElement(0), NullData.instance));
    assertThat(got.getElement(1))
        .isEqualTo(testDTI().arrayOf(left.getElement(1), NullData.instance));
  }

  @Test
  public void join_fullJoin() {
    Array left = testDTI().arrayOf(arbitrary(), arbitrary());
    Array right = testDTI().arrayOf(left.getElement(1), left.getElement(0));

    Array got = join(context, left, right, equality());
    assertThat(got.size()).isEqualTo(2);
    assertThat(got.getElement(0))
        .isEqualTo(testDTI().arrayOf(left.getElement(0), right.getElement(1)));
    assertThat(got.getElement(1))
        .isEqualTo(testDTI().arrayOf(left.getElement(1), right.getElement(0)));
  }

  @Test
  public void join_partialJoin() {
    Array left = testDTI().arrayOf(arbitrary(), arbitrary());
    Array right = testDTI().arrayOf(arbitrary(), left.getElement(0));

    Array got = join(context, left, right, equality());
    assertThat(got.size()).isEqualTo(3);
    assertThat(got.getElement(0))
        .isEqualTo(testDTI().arrayOf(left.getElement(0), right.getElement(1)));
    assertThat(got.getElement(1))
        .isEqualTo(testDTI().arrayOf(left.getElement(1), NullData.instance));
    assertThat(got.getElement(2))
        .isEqualTo(testDTI().arrayOf(NullData.instance, right.getElement(0)));
  }

  @Test
  public void join_mutexJoin() {
    Array left = testDTI().arrayOf(arbitrary(), arbitrary());
    Array right = testDTI().arrayOf(arbitrary(), arbitrary());

    Array got = join(context, left, right, equality());
    assertThat(got.size()).isEqualTo(4);
    assertThat(got.getElement(0))
        .isEqualTo(testDTI().arrayOf(left.getElement(0), NullData.instance));
    assertThat(got.getElement(1))
        .isEqualTo(testDTI().arrayOf(left.getElement(1), NullData.instance));
    assertThat(got.getElement(2))
        .isEqualTo(testDTI().arrayOf(NullData.instance, right.getElement(0)));
    assertThat(got.getElement(3))
        .isEqualTo(testDTI().arrayOf(NullData.instance, right.getElement(1)));
  }

  private Closure constReturn(Data ret) {
    return new MockClosure(2, (args, ctx) -> ret);
  }

  private Closure equality() {
    return new MockClosure(
        2, (args, ctx) -> testDTI().primitiveOf(args.get(0).equals(args.get(1))));
  }

  private Closure sumNumberAccumulator() {
    return new MockClosure(
        2, (args, ctx) -> Operators.sum(ctx, args.get(0).asPrimitive(), args.get(1).asPrimitive()));
  }

  private Closure twoKeySelector() {
    Closure keySelector = mock(Closure.class);
    List<Data> elementSingleton = new ArrayList<>();
    AtomicInteger elementIdx = new AtomicInteger(-1);
    when(keySelector.bindNextFreeParameter(any()))
        .then(
            i -> {
              elementSingleton.add(i.getArgument(0));
              elementIdx.addAndGet(1);
              return keySelector;
            });
    when(keySelector.execute(any()))
        .then(
            i -> {
              Data keyOneVal =
                  elementSingleton.get(elementIdx.get()).asContainer().getField("key1");
              Data keyTwoVal =
                  elementSingleton.get(elementIdx.get()).asContainer().getField("key2");
              return testDTI()
                  .containerOf(ImmutableMap.of("keyOne", keyOneVal, "keyTwo", keyTwoVal));
            });
    return keySelector;
  }

  private Data toData(String s) {
    return JSON.deserialize(s.getBytes(UTF_8));
  }
}
