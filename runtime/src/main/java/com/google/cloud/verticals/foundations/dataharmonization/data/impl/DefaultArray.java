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

package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/**
 * DefaultArray is a simple implementation of the {@link Array} interface, backed by a {@link
 * ArrayList}.
 */
public class DefaultArray implements Array {
  private List<Data> array;
  private Set<Integer> fixedElements = new HashSet<>();

  public DefaultArray() {
    this(ImmutableList.of());
  }

  /**
   * Create an array by copying items from some existing list. The items are not cloned; the copy is
   * shallow.
   */
  public DefaultArray(Collection<? extends Data> array) {
    this.array = new ArrayList<>(array);
  }

  @Nonnull
  @Override
  public Data getElement(int index) {
    return index < size() ? array.get(index) : NullData.instance;
  }

  @CanIgnoreReturnValue
  @Override
  public DefaultArray setElement(int index, @Nonnull Data value) {
    if (index >= size()) {
      array.addAll(Collections.nCopies(index - size() + 1, NullData.instance));
    }
    array.set(index, value);
    return this;
  }

  @Override
  public Array setFixedElement(int index, @Nonnull Data value) {
    fixedElements.add(index);
    return setElement(index, value);
  }

  @Override
  public boolean isFixed(int index) {
    return fixedElements.contains(index);
  }

  @Override
  public int size() {
    return array.size();
  }

  @Override
  public Data deepCopy() {
    // Deep clone the values.
    return new DefaultArray(array.stream().map(Data::deepCopy).collect(Collectors.toList()));
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public Stream<Data> stream() {
    return array.stream();
  }

  @Override
  public Array getThrough(Path remainingPath) {
    return new DefaultArray(
        stream()
            .map(remainingPath::get)
            .filter(d -> !d.isNullOrEmpty())
            .collect(Collectors.toList()));
  }

  @Override
  public Array flatten() {
    return new DefaultArray(
        stream().flatMap(d -> d.asArray().stream()).collect(Collectors.toList()));
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Data) || !((Data) o).isArray()) {
      return false;
    }
    final Array other = ((Data) o).asArray();
    if (other.size() != size()) {
      return false;
    }
    for (int i = 0; i < size(); ++i) {
      if (!other.getElement(i).equals(getElement(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    if (isNullOrEmpty()) {
      return NullData.instance.hashCode();
    }
    return Objects.hashCode(array);
  }

  @Override
  public String toString() {
    return array.toString();
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.writeObject(array);
    oos.writeObject(fixedElements);
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    array = (List<Data>) ois.readObject();
    fixedElements = (Set<Integer>) ois.readObject();
  }
}
