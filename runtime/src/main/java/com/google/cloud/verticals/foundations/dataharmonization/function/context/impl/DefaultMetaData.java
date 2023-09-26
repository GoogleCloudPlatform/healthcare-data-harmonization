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

package com.google.cloud.verticals.foundations.dataharmonization.function.context.impl;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.MetaData;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/** Default implementation of {@link MetaData}. */
public class DefaultMetaData implements MetaData {

  private transient Map<String, Object> metadata;
  private Map<String, Serializable> serializableMetadata;

  public DefaultMetaData() {
    this(new HashMap<>(), new HashMap<>());
  }

  public DefaultMetaData(
      Map<String, Object> metadata, Map<String, Serializable> serializableMetadata) {
    this.metadata = metadata;
    this.serializableMetadata =
        (serializableMetadata != null) ? serializableMetadata : new HashMap<>();
  }

  @SuppressWarnings("TypeParameterUnusedInFormals")
  @Override
  public <T> T getMeta(String name) {
    // code accessing metadata will be aware of the type that was put in metadata entry and is
    // responsible for accepting the correct return type when calling getMeta().
    @SuppressWarnings("unchecked")
    T value = (T) metadata.get(name);
    return value;
  }

  @Override
  public <T> void setMeta(String name, T item) {
    metadata.put(name, item);
  }

  @Override
  public Map<String, Serializable> getSerializableMetadata() {
    return Collections.unmodifiableMap(this.serializableMetadata);
  }

  // As with transient metadata, code accessing serializableMetadata will be aware of the type in
  // the entry and is responsible for accepting the correct return type when calling
  // getSerializableMeta().

  @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
  @Override
  public <T> T getSerializableMeta(String name) {
    return (T) serializableMetadata.get(name);
  }

  @Override
  public void setSerializableMeta(String name, Serializable item) {
    serializableMetadata.put(name, item);
  }

  @Override
  public MetaData deepCopy() {
    // TODO(): This is only a shallow copy for now.
    return new DefaultMetaData(new HashMap<>(metadata), new HashMap<>(serializableMetadata));
  }

  /**
   * Non-generated override of equals method to provide a logical equivalence check for
   * DefaultRuntimeContext.
   *
   * @param that The object on which to execute the comparison
   * @return true if equal, otherwise false
   */
  @Override
  public boolean equals(@Nullable Object that) {
    if (!(that instanceof DefaultMetaData)) {
      return false;
    }
    return Objects.equals(
        this.serializableMetadata, ((DefaultMetaData) that).getSerializableMetadata());
  }

  /**
   * Non-generated Override of hashCode on the class to provide the basis for testing for logical
   * equivalence.
   *
   * @return int hash value of the class.
   */
  @Override
  public int hashCode() {
    return Objects.hashCode(this.serializableMetadata);
  }

  // helper functions for serialization.
  private void writeObject(ObjectOutputStream oos) throws IOException {
    oos.writeObject(serializableMetadata);
  }

  @SuppressWarnings("unchecked")
  private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
    serializableMetadata = new HashMap<>((Map<String, Serializable>) ois.readObject());
    metadata = new HashMap<>();
  }
}
