/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.mock;

import static java.lang.Math.max;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/** Static methods for creating mock Data. */
public final class MockData {
  public static Array arrayOf(Data elementToRepeat, int size) {
    Array mock = emptyArray();
    when(mock.size()).thenReturn(size);
    when(mock.getElement(anyInt())).thenReturn(elementToRepeat);
    return mock;
  }

  public static Array arrayOf(Data... elements) {
    Array mock = emptyArray();
    when(mock.size()).thenReturn(elements.length);
    when(mock.getElement(anyInt())).thenReturn(NullData.instance);
    for (int i = 0; i < elements.length; i++) {
      when(mock.getElement(i)).thenReturn(elements[i]);
    }
    if (elements.length > 0) {
      String elementString = Arrays.toString(elements);
      when(mock.toString()).thenReturn("Mock Array of " + elementString);
    }
    return mock;
  }

  public static Array emptyArray() {
    Array array = mock(Array.class, withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    when(array.size()).thenReturn(0);
    when(array.getThrough(any()))
        .then(
            i -> {
              Path p = i.getArgument(0);
              return arrayOf(array.stream().map(p::get).toArray(Data[]::new));
            });
    when(array.flatten())
        .then(
            i ->
                arrayOf(
                    array.stream().map(Data::asArray).flatMap(Array::stream).toArray(Data[]::new)));
    when(array.setElement(anyInt(), any())).thenReturn(array);
    when(array.toString()).thenReturn("Empty Mock Array");
    return array;
  }

  public static Container containerOf(Data elementToRepeat) {
    Container mock = emptyContainer();
    when(mock.getField(ArgumentMatchers.anyString())).thenReturn(elementToRepeat);
    when(mock.isNullOrEmpty()).thenReturn(false);
    return mock;
  }

  public static Container containerOf(String key, Data element) {
    Container mock = emptyContainer();
    when(mock.getField(ArgumentMatchers.eq(key))).thenReturn(element);
    when(mock.fields()).thenReturn(Collections.singleton(key));
    when(mock.isNullOrEmpty()).thenReturn(false);
    return mock;
  }

  public static Container mutableContainerOf(Consumer<ContainerInitializer> init) {
    Map<String, Data> backingMap = new HashMap<>();
    init.accept(backingMap::put);
    Container container = Mockito.spy(Container.class);
    when(container.asContainer()).thenReturn(container);
    when(container.isWritable()).thenReturn(true);
    when(container.fields()).thenReturn(backingMap.keySet());
    when(container.isNullOrEmpty()).then(i -> backingMap.isEmpty());
    when(container.getField(ArgumentMatchers.anyString()))
        .then(i -> backingMap.getOrDefault((String) i.getArgument(0), NullData.instance));
    Mockito.doAnswer(
            i -> {
              backingMap.put(i.getArgument(0), i.getArgument(1));
              return container;
            })
        .when(container)
        .setField(ArgumentMatchers.anyString(), ArgumentMatchers.any());

    return container;
  }

  public static Array mutableArrayOf(Data... initialElements) {
    List<Data> backingList = new ArrayList<>(Arrays.asList(initialElements));
    Array array =
        mock(Array.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    when(array.isNullOrEmpty()).then(i -> backingList.isEmpty());
    when(array.isWritable()).thenReturn(true);
    when(array.size()).then(i -> backingList.size());
    when(array.getElement(anyInt()))
        .then(
            i ->
                (int) i.getArgument(0) < backingList.size()
                    ? backingList.get(i.getArgument(0))
                    : NullData.instance);
    Mockito.doAnswer(
            i -> {
              int index = i.getArgument(0);
              Data value = i.getArgument(1);
              int numToAdd = max(0, index - backingList.size() + 1);
              backingList.addAll(Collections.nCopies(numToAdd, NullData.instance));
              backingList.set(index, value);
              return array;
            })
        .when(array)
        .setElement(anyInt(), ArgumentMatchers.any());

    return array;
  }

  public static Container emptyContainer() {
    Container container =
        mock(Container.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
    when(container.fields()).thenReturn(ImmutableSet.of());
    when(container.setField(any(), any())).thenReturn(container);
    when(container.toString()).thenReturn("Empty Mock Container");
    return container;
  }

  /**
   * Returns a {@link Mockito#mock(Class)} {@link Data}(or subclass of Data} with {@link
   * Data#isClass(Class)} and {@link Data#asClass(Class)} directed the the default implementation.
   */
  public static <T extends Data> T mockWithClass(Class<T> clazz) {
    T mock = mock(clazz);
    when(mock.isClass(any())).thenAnswer(Answers.CALLS_REAL_METHODS);
    when(mock.asClass(any())).thenAnswer(Answers.CALLS_REAL_METHODS);
    when(mock.isContainer()).thenAnswer(Answers.CALLS_REAL_METHODS);
    when(mock.isArray()).thenAnswer(Answers.CALLS_REAL_METHODS);
    when(mock.isPrimitive()).thenAnswer(Answers.CALLS_REAL_METHODS);
    return mock;
  }

  public static Dataset datasetOf(Data... elements) {
    return new MockDataset(arrayOf(elements));
  }

  public static NullData nul() {
    return NullData.instance;
  }

  public static Data arbitrary() {
    return mock(Data.class, Mockito.withSettings().defaultAnswer(Answers.CALLS_REAL_METHODS));
  }

  /** Function to call to add items to the initial state of the container. */
  @FunctionalInterface
  public interface ContainerInitializer {
    void set(String key, Data value);
  }

  private MockData() {}
}
