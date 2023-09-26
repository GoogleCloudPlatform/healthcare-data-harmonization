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

import static com.google.common.collect.ImmutableMap.toImmutableMap;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * DefaultContainer is a simple implementation of the {@link Container} interface, backed by a
 * {@link Map}.
 */
public class DefaultContainer implements Container {
  private Map<String, Data> container;

  public DefaultContainer() {
    this(ImmutableMap.of());
  }

  /**
   * Create a Container by copying items from some existing map. The items are not cloned; the copy
   * is shallow.
   */
  public DefaultContainer(Map<String, ? extends Data> container) {
    this.container = new HashMap<>(container);
  }

  @Nonnull
  @Override
  public Data getField(String field) {
    return container.getOrDefault(field, NullData.instance);
  }

  @CanIgnoreReturnValue
  @Override
  public DefaultContainer setField(@Nonnull String field, Data value) {
    container.put(field, value);
    return this;
  }

  @CanIgnoreReturnValue
  @Override
  public Container removeField(@Nonnull String field) {
    container.remove(field);
    return this;
  }

  @Nonnull
  @Override
  public Set<String> fields() {
    return ImmutableSortedSet.copyOf(container.keySet());
  }

  @Override
  public Data deepCopy() {
    // Deep clone the values.
    return new DefaultContainer(
        container.entrySet().stream()
            .map(e -> new SimpleEntry<>(e.getKey(), e.getValue().deepCopy()))
            .collect(
                toImmutableMap(
                    SimpleEntry<String, Data>::getKey, SimpleEntry<String, Data>::getValue)));
  }

  @Override
  public boolean isWritable() {
    return true;
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof Data) || !((Data) o).isContainer()) {
      return false;
    }
    final Container other = ((Data) o).asContainer();

    Set<String> otherFieldsNonNull = other.nonNullFields();
    Set<String> nonNullFields = nonNullFields();
    if (otherFieldsNonNull.size() != nonNullFields.size()) {
      return false;
    }
    for (String field : fields()) {
      if (!other.getField(field).equals(getField(field))) {
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
    return Objects.hashCode(removeEmptyFields().container);
  }

  @Override
  public String toString() {
    Iterator<String> i = fields().iterator();
    if (!i.hasNext()) {
      return "{}";
    }
    StringBuilder sb = new StringBuilder();
    sb.append('{');
    for (; ; ) {
      String key = i.next();
      Data value = container.get(key);
      sb.append(key);
      sb.append('=');
      sb.append(value);
      if (!i.hasNext()) {
        return sb.append('}').toString();
      }
      sb.append(',').append(' ');
    }
  }

  private void writeObject(ObjectOutputStream oos) throws IOException {
    Map<String, Data> sorted =
        container.entrySet().stream()
            .sorted(comparingByKey())
            .collect(
                toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1 /* merge value old */, e2 /* merge value new */) -> e2 /* keep new  */,
                    LinkedHashMap::new));
    oos.writeObject(sorted);
  }

  /**
   * The writeObject method modifies the HashMap to a LinkedHashMap to provide deterministic
   * ordering of the elements in the map during serialization. This may need to be explicitly copied
   * back to a HashMap if unexpected behavior is noticed around serialization of Containers.
   */
  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    container = (HashMap<String, Data>) ois.readObject();
  }

  private DefaultContainer removeEmptyFields() {
    ImmutableMap<String, Data> container =
        nonNullFields().stream().collect(toImmutableMap(f -> f, this::getField));
    return new DefaultContainer(container);
  }
}
