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

package com.google.cloud.verticals.foundations.dataharmonization.data.wrappers;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Dataset;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.merge.MergeStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.data.serialization.WithCustomSerialization;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Base class that can be used to add additional properties to a backing Data without having to
 * implement a wrapper for each of the DACP types. Note that depending on the RuntimeContext and
 * function implementation, the properties in the data wrapper may or may not persist through native
 * java function execution.
 *
 * <p>When wrapper data is used as native java function argument, Java function resolve to the
 * shallowest layer of wrapper that is or is the subset of argument type.
 *
 * @param <T> The type of the implementing class.
 */
public abstract class WrapperData<T extends WrapperData<T>>
    implements Container, Array, Primitive, Dataset {
  private final Data backing;

  protected WrapperData(Data backing) {
    this.backing = backing;
  }

  // Data level methods forwarding
  @Override
  public boolean isNullOrEmpty() {
    return backing.isNullOrEmpty();
  }

  @Override
  public Data deepCopy() {
    return rewrap(backing.deepCopy());
  }

  @Override
  public boolean isWritable() {
    return backing.isWritable();
  }

  @Override
  public MergeStrategy getMergeStrategy() {
    return backing.getMergeStrategy();
  }

  // Primitive methods
  @Override
  public boolean isPrimitive() {
    return backing.isPrimitive();
  }

  // type narrowing
  @Override
  @Nullable
  public Primitive asPrimitive() {
    if (isPrimitive()) {
      // in-place operation
      return this;
    }
    return null;
  }

  @Override
  @Nullable
  public Double num() {
    if (isPrimitive()) {
      return backing.asPrimitive().num();
    }
    return null;
  }

  @Override
  @Nullable
  public String string() {
    if (isPrimitive()) {
      return backing.asPrimitive().string();
    }
    return null;
  }

  @Override
  @Nullable
  public Boolean bool() {
    if (isPrimitive()) {
      return backing.asPrimitive().bool();
    }
    return null;
  }

  // Container methods
  @Override
  public boolean isContainer() {
    return backing.isContainer();
  }

  @Override
  @Nullable
  public Container asContainer() {
    if (isContainer()) {
      return this;
    }
    return null;
  }

  @Nonnull
  @Override
  public Data getField(String field) {
    if (isContainer()) {
      return backing.asContainer().getField(field);
    }
    return incompatibleOperationResult(".getField(field)").asContainer().getField(field);
  }

  @Override
  public Container setField(@Nonnull String field, Data value) {
    if (isContainer()) {
      return rewrap(backing.asContainer().setField(field, value));
    }
    return incompatibleOperationResult(".setField(field, value)")
        .asContainer()
        .setField(field, value);
  }

  @Override
  public Container removeField(@Nonnull String field) {
    if (isContainer()) {
      return rewrap(backing.asContainer().removeField(field));
    }
    return incompatibleOperationResult(".removeField(field)").asContainer().removeField(field);
  }

  @Nonnull
  @Override
  public Set<String> fields() {
    if (isContainer()) {
      return backing.asContainer().fields();
    }
    return incompatibleOperationResult(".fields()").asContainer().fields();
  }

  // Array methods
  @Override
  public boolean isArray() {
    return backing.isArray();
  }

  @Override
  @Nullable
  public Array asArray() {
    if (isArray()) {
      return this;
    }
    return null;
  }

  @Nonnull
  @Override
  public Data getElement(int index) {
    if (isArray()) {
      return backing.asArray().getElement(index);
    }
    return incompatibleOperationResult(".getElement(index)").asArray().getElement(index);
  }

  @Override
  public Array setElement(int index, @Nonnull Data value) {
    if (isArray()) {
      return rewrap(backing.asArray().setElement(index, value));
    }
    return incompatibleOperationResult(".setElement(index, value)")
        .asArray()
        .setElement(index, value);
  }

  @Override
  public int size() {
    if (isArray()) {
      return backing.asArray().size();
    }
    return incompatibleOperationResult(".size()").asArray().size();
  }

  // Dataset methods
  @Override
  public boolean isDataset() {
    return backing.isDataset();
  }

  @Override
  @Nullable
  public Dataset asDataset() {
    if (isDataset()) {
      return this;
    }
    return null;
  }

  @Override
  public Dataset map(RuntimeContext context, Closure closure, boolean flatten) {
    if (isDataset()) {
      return rewrap(backing.asDataset().map(context, closure, flatten));
    }
    return incompatibleOperationResult("map(context, closure, flatten)")
        .asDataset()
        .map(context, closure, flatten);
  }

  @Override
  public Data getDataToSerialize() {
    if (getBacking() instanceof WithCustomSerialization) {
      return ((WithCustomSerialization) getBacking()).getDataToSerialize();
    }
    return getBacking();
  }

  // Transparent Collection methods.
  // Although the return type is Array as long as Whistle core interact with data through isArray()
  // and isDataset() etc., transparent collection methods actually stay closed for dataset vs.
  // arrays
  @Override
  public Array getThrough(Path remainingPath) {
    if (isTransparentCollection()) {
      return rewrap(backing.asTransparentCollection().getThrough(remainingPath));
    }
    return incompatibleOperationResult(".getThrough()").asArray();
  }

  @Override
  public Array flatten() {
    if (isTransparentCollection()) {
      return rewrap(backing.asTransparentCollection().flatten());
    }
    return incompatibleOperationResult(".flatten()").asArray();
  }

  @Override
  public boolean isTransparentCollection() {
    return backing.isTransparentCollection();
  }

  // Type cast and assertions -- wrapper specific implementation.
  @Override
  public boolean isClass(Class<? extends Data> clazz) {
    if (clazz == Primitive.class) {
      return isPrimitive();
    }
    if (clazz == Container.class) {
      return isContainer();
    }
    if (clazz == Array.class) {
      return isArray();
    }
    if (clazz == Dataset.class) {
      return isDataset();
    }
    return clazz.isAssignableFrom(getClass()) || backing.isClass(clazz);
  }

  @Override
  public <T2 extends Data> T2 asClass(Class<T2> clazz) {
    if (!isClass(clazz)) {
      throw new IllegalArgumentException(
          String.format(
              "Cannot cast %s into %s.", getClass().getSimpleName(), clazz.getSimpleName()));
    }
    if (clazz.isAssignableFrom(getClass())) {
      return clazz.cast(this);
    }
    return backing.asClass(clazz);
  }

  // Wrapper data specific methods
  protected abstract T rewrap(Data backing);

  /** Returns the backing {@link Data}. */
  public final Data getBacking() {
    return backing;
  }

  /**
   * Returns the result when incompatible operation in applied to this {@link WrapperData}. For
   * example when the backing data is a Container but array operation is directly applied to the
   * current wrapper, {@code incompatibleOperationResult} will be called instead.
   *
   * @param currentMethodName the incompatible method name. Mainly used for populating debugging
   *     info.
   */
  protected Data incompatibleOperationResult(String currentMethodName) {
    throw new IllegalStateException(
        String.format(
            "Attempt to call %s on a wrapper data with backing %s.", currentMethodName, backing));
  }
}
