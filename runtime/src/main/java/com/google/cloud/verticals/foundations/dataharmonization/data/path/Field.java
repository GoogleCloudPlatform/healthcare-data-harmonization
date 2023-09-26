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
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Field is an implementation of PathSegment representing a single specific field in a {@link
 * Container}.
 */
public final class Field implements PathSegment {
  private final String field;

  public Field(@Nonnull String field) {
    this.field = field;
  }

  @Nonnull
  @Override
  public Data get(@Nonnull Data data) {
    if (!data.isContainer()) {
      if (TYPE_MISMATCH_RETURNS_NULL) {
        return NullData.instance;
      }
      throw new UnsupportedOperationException(
          String.format(
              "Attempted to key into non-container %s with field %s",
              String.join("/", Core.types(data)), field));
    }
    return data.asContainer().getField(field);
  }

  @Override
  public Data set(@Nonnull Data data, Data value) {
    if (!data.isContainer()) {
      throw new UnsupportedOperationException(
          String.format(
              "Attempted to key into non-container %s with field %s",
              String.join("/", Core.types(data)), field));
    }
    data = data.asContainer().setField(field, value);
    return data;
  }

  @Override
  public String toString() {
    return field;
  }

  /** @return a new, empty {@link Container}. */
  @Override
  public Data create(DataTypeImplementation dti) {
    return dti.emptyContainer();
  }

  @Override
  public boolean isIndex() {
    return false;
  }

  @Override
  public boolean isField() {
    return true;
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
    Field field1 = (Field) o;
    return Objects.equals(field, field1.field);
  }

  @Override
  public int hashCode() {
    return Objects.hash(field);
  }
}
