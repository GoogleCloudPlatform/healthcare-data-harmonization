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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import com.google.cloud.verticals.foundations.dataharmonization.data.merge.DefaultMergeStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.data.merge.MergeStrategy;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.io.Serializable;

/** Data is the base interface for all Types in Whistle. */
public interface Data extends Serializable {

  /** Returns true if this Data value is null or empty. */
  boolean isNullOrEmpty();

  /** Returns a deep clone of this Data. */
  Data deepCopy();

  /**
   * Returns true iff this instance can be written/modified. This refers to the mutation of this
   * specific instance, and not whether it can be overwritten/replaced enitrely as a value.
   */
  boolean isWritable();

  /** Returns true if this Data is a {@link Primitive}. */
  default boolean isPrimitive() {
    return false;
  }

  /**
   * Returns a {@link Primitive} instance if this Data is a {@link Primitive}, or null otherwise.
   * {@see #isPrimitive}.
   */
  default Primitive asPrimitive() {
    return null;
  }

  /** Returns true if this Data is a {@link Container}. */
  default boolean isContainer() {
    return false;
  }

  /**
   * Returns a {@link Container} instance if this Data is a {@link Container}, or null otherwise.
   * {@see #isContainer}.
   */
  default Container asContainer() {
    return null;
  }

  /** Returns true if this Data is an {@link Array}. */
  default boolean isArray() {
    return false;
  }

  /**
   * Returns a {@link Array} instance if this Data is a {@link Array}, or null otherwise. {@see
   * #isArray}.
   */
  default Array asArray() {
    return null;
  }

  /** Returns true if this Data is an {@link Dataset}. */
  default boolean isDataset() {
    return false;
  }

  /**
   * Returns a {@link Dataset} instance if this Data is a {@link Dataset}, or null otherwise. {@see
   * #isDataset}.
   */
  default Dataset asDataset() {
    return null;
  }

  /** Returns true if this Data is a {@link TransparentCollection}. */
  default boolean isTransparentCollection() {
    return false;
  }

  /**
   * Returns a {@link TransparentCollection} instance if this Data is a {@link
   * TransparentCollection}, or null otherwise. {@see #isTransparentCollection}.
   */
  default TransparentCollection<? extends TransparentCollection<?>> asTransparentCollection() {
    return null;
  }

  /**
   * Returns the resulting root data after merging the {@code other} data into the {@code path} of
   * this {@link Data}. This method makes sure that the {@code path} under the returning data is set
   * to the merge result.
   *
   * @param ctx the {@link RuntimeContext} used to supply auxilary information needed during the
   *     merge. For example {@link RuntimeContext#getDataTypeImplementation} that supplies missing
   *     intermediate value when it is missing, as specified in {@link
   *     Path#set(DataTypeImplementation, Data, Data)}.
   * @param other the inbound data to merge into the {@code path} of {@code this};
   * @param path the path to the current data to be merged into.
   * @return the resulting data root when merging the {@code other} data into the {@code path} of
   *     this {@link Data}. Depending on implementation, this root may or may not share the same
   *     reference with this {@link Data} instance.
   */
  default Data merge(RuntimeContext ctx, Data other, Path path) {
    return getMergeStrategy().merge(ctx.getDataTypeImplementation(), this, other, path);
  }

  /**
   * Returns the resulting data after merging the {@code other} data into the current data object.
   * Depending on implementation, this root may or may not share the same reference with this {@link
   * Data} instance.
   */
  default Data merge(Data other, DataTypeImplementation dti) {
    return getMergeStrategy().merge(dti, this, other, Path.empty());
  }

  default MergeStrategy getMergeStrategy() {
    return DefaultMergeStrategy.INSTANCE;
  }

  /** Returns true iff the current data can be used as data of Class clazz. */
  default boolean isClass(Class<? extends Data> clazz) {
    if (NullData.class.isAssignableFrom(clazz) && isNullOrEmpty()) {
      return true;
    }
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
    return clazz.isAssignableFrom(getClass());
  }

  /**
   * Convert this {@link Data} into Class clazz. Throws {@link IllegalArgumentException} if this
   * {@link Data} cannot be used as a data of Class clazz.
   *
   * @param clazz the Class to convert the current data into.
   * @param <T> the type of Data implementation.
   * @return the converted data. Note that depending on its implementation this may or may not be
   *     the same as original data object (i.e. may or may not share the same reference).
   */
  default <T extends Data> T asClass(Class<T> clazz) {
    if (!isClass(clazz)) {
      throw new ClassCastException(
          String.format(
              "Cannot cast %s into %s.", getClass().getSimpleName(), clazz.getSimpleName()));
    }
    if (NullData.class == clazz && isNullOrEmpty()) {
      return clazz.cast(NullData.instance);
    }
    return clazz.cast(this);
  }
}
