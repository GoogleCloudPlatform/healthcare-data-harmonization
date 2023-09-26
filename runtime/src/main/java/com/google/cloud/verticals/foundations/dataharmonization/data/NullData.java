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

import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * NullData is a value of Null. It can be treated as a valid value and used for null propagation.
 */
public final class NullData implements Primitive, Container, Array, Dataset {
  public static final NullData instance = new NullData();

  private NullData() {}

  /** Converts null to NullData. Leaves non-null Data as is. */
  public static Data wrapNull(Data data) {
    return data == null ? instance : data;
  }

  @Nonnull
  @Override
  public Data getElement(int index) {
    return instance;
  }

  @Override
  public Array setElement(int index, @Nonnull Data value) {
    throw new UnsupportedOperationException(
        "Cannot write to null. This method should not be called.");
  }

  @Override
  public int size() {
    return 0;
  }

  @Nonnull
  @Override
  public Data getField(String field) {
    return instance;
  }

  @Override
  public Container setField(@Nonnull String field, Data value) {
    throw new UnsupportedOperationException(
        "Cannot write to null. This method should not be called.");
  }

  @Override
  public Container removeField(@Nonnull String field) {
    // No op because the field is technically gone.
    return this;
  }

  @Nonnull
  @Override
  public Set<String> fields() {
    return Collections.emptySet();
  }

  @Override
  public Double num() {
    return null;
  }

  @Override
  public String string() {
    return null;
  }

  @Override
  public Boolean bool() {
    return null;
  }

  @Override
  public boolean isNullOrEmpty() {
    return true;
  }

  @Override
  public NullData deepCopy() {
    return instance;
  }

  @Override
  public Dataset map(RuntimeContext context, Closure closure, boolean flatten) {
    return instance;
  }

  @Override
  public Data getDataToSerialize() {
    return this;
  }

  @Override
  public boolean isWritable() {
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Data)) {
      return false;
    }
    final Data other = (Data) o;
    return other.isNullOrEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hash((Object) null);
  }

  @Override
  public String toString() {
    return "";
  }

  @Override
  public Array getThrough(Path remainingPath) {
    return NullData.instance;
  }

  @Override
  public Array flatten() {
    return NullData.instance;
  }
}
