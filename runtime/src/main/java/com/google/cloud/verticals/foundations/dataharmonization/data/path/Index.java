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

package com.google.cloud.verticals.foundations.dataharmonization.data.path;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Core;
import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Index is an implementation of PathSegment representing a single specific index in a {@link
 * Array}. It may also be used to append a value to the array. See {@link Index#Index(Integer)}.
 */
public final class Index implements PathSegment {

  private final Integer index;

  /**
   * Creates a new Index with the given array index.
   *
   * @param index the array index to hold. A null index will append values to the end of the array
   *     upon {@link #set(Data, Data)}, and throw an Exception upon {@link PathSegment#get(Data)}
   */
  public Index(Integer index) {
    this.index = index;
  }

  @Nonnull
  @Override
  public Data get(@Nonnull Data data) {
    if (!data.isArray()) {
      if (TYPE_MISMATCH_RETURNS_NULL) {
        return NullData.instance;
      }
      throw new UnsupportedOperationException(
          String.format(
              "Attempted to index into non-array %s with index [%d]",
              String.join("/", Core.types(data)), index));
    }
    if (index == null) {
      return NullData.instance;
    }
    return data.asArray().getElement(index);
  }

  @Override
  public Data set(@Nonnull Data data, Data value) {
    if (!data.isArray()) {
      throw new UnsupportedOperationException(
          String.format(
              "Attempted to index into non-array %s with index [%d]",
              String.join("/", Core.types(data)), index));
    }

    if (this.index != null) {
      data = data.asArray().setFixedElement(index, value);
    } else if (value.isArray()) {
      if (value.asArray() == data.asArray()) {
        // Uh oh.
        value = value.deepCopy();
      }
      // Concat arrays when added to with []
      for (int i = 0; i < value.asArray().size(); i++) {
        data = data.asArray().setElement(data.asArray().size(), value.asArray().getElement(i));
      }
    } else {
      // setting field
      data = data.asArray().setElement(data.asArray().size(), value);
    }

    return data;
  }

  /** @return a new, empty {@link Array}. */
  @Override
  public Data create(DataTypeImplementation dti) {
    return dti.emptyArray();
  }

  @Override
  public boolean isIndex() {
    return true;
  }

  @Override
  public boolean isField() {
    return false;
  }

  @Override
  public String toString() {
    if (index != null) {
      return "[" + index + "]";
    }
    return "[]";
  }

  // Auto-generated equality functions.
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Index index1 = (Index) o;
    return Objects.equals(index, index1.index);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index);
  }
}
