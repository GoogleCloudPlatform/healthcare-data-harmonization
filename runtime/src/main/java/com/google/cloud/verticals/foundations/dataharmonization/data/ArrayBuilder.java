/*
 * Copyright 2023 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.common.collect.Streams.stream;
import static java.util.Arrays.stream;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/** Builds an {@link Array} */
public class ArrayBuilder {
  private final DataTypeImplementation dti;
  private final ImmutableList.Builder<Data> listBuilder;

  private ArrayBuilder(DataTypeImplementation dti) {
    this.dti = dti;
    this.listBuilder = ImmutableList.builder();
  }

  /** Creates a new ArrayBuilder from a {@link RuntimeContext}. */
  @CanIgnoreReturnValue
  public static ArrayBuilder fromContext(RuntimeContext ctx) {
    return new ArrayBuilder(ctx.getDataTypeImplementation());
  }

  /** Creates a new ArrayBuilder from a {@link DataTypeImplementation}. */
  @CanIgnoreReturnValue
  public static ArrayBuilder fromDataTypeImplementation(DataTypeImplementation dti) {
    return new ArrayBuilder(dti);
  }

  private Data nullToNullData(Data element) {
    return element == null ? NullData.instance : element;
  }

  private Data nullToNullData(String element) {
    return element == null ? NullData.instance : dti.primitiveOf(element);
  }

  private Data nullToNullData(Boolean element) {
    return element == null ? NullData.instance : dti.primitiveOf(element);
  }

  private Data nullToNullData(Double element) {
    return element == null ? NullData.instance : dti.primitiveOf(element);
  }

  /** Adds all vararg Data elements. */
  @CanIgnoreReturnValue
  public ArrayBuilder add(Data... elements) {
    if (elements == null) {
      return this;
    }
    stream(elements).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Uses the {@link DataTypeImplementation} to add one or more String elements. */
  @CanIgnoreReturnValue
  public ArrayBuilder add(String... elements) {
    if (elements == null) {
      return this;
    }
    stream(elements).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Uses the {@link DataTypeImplementation} to add one or more Boolean elements. */
  @CanIgnoreReturnValue
  public ArrayBuilder add(Boolean... elements) {
    if (elements == null) {
      return this;
    }
    stream(elements).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Uses the {@link DataTypeImplementation} to add a one or more Double elements. */
  @CanIgnoreReturnValue
  public ArrayBuilder add(Double... elements) {
    if (elements == null) {
      return this;
    }
    stream(elements).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Adds all {@link Data} elements from an Iterable. */
  @CanIgnoreReturnValue
  public ArrayBuilder addAll(@Nonnull Iterable<? extends Data> iterable) {
    stream(iterable).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Adds all {@link Data} elements from a Stream. This is a terminal stream operation. */
  @CanIgnoreReturnValue
  public ArrayBuilder addAll(@Nonnull Stream<? extends Data> stream) {
    stream.map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Adds all elements of an {@link Array}. */
  @CanIgnoreReturnValue
  public ArrayBuilder addAll(@Nonnull Array array) {
    array.stream().forEach(listBuilder::add);
    return this;
  }

  /**
   * Combines with another ArrayBuilder, building it and adding all elements to this one. If an
   * ArrayBuilder is passed to itself, the result is a No-operation.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAll(@Nonnull ArrayBuilder arrayBuilder) {
    if (arrayBuilder == this) {
      return this;
    }
    return addAll(arrayBuilder.build());
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a String Iterable, each
   * converted to {@link Primitive} elements.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllStrings(@Nonnull Iterable<String> iterable) {
    stream(iterable).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a String Stream, each converted
   * to {@link Primitive} elements. This is a terminal stream operation.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllStrings(@Nonnull Stream<String> stream) {
    stream.map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a Boolean Iterable, each
   * converted to {@link Primitive} elements.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllBooleans(@Nonnull Iterable<Boolean> iterable) {
    stream(iterable).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a Boolean Stream, each converted
   * to {@link Primitive} elements. This is a terminal stream operation.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllBooleans(@Nonnull Stream<Boolean> stream) {
    stream.map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a Double Iterable, each
   * converted to {@link Primitive} elements.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllNumbers(@Nonnull Iterable<Double> iterable) {
    stream(iterable).map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to add each element of a Double Stream, each converted
   * to {@link Primitive} elements. This is a terminal stream operation.
   */
  @CanIgnoreReturnValue
  public ArrayBuilder addAllNumbers(@Nonnull Stream<Double> stream) {
    stream.map(this::nullToNullData).forEach(listBuilder::add);
    return this;
  }

  /** Builds an Array and returns the result */
  public Array build() {
    return dti.arrayOf(listBuilder.build());
  }
}
