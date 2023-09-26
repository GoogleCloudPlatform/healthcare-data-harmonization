/*
 * Copyright 2021 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.function.context;

import java.io.Serializable;
import java.util.Map;

/** Common interface for setting and accessing user defined data in RuntimeContext. */
public interface MetaData extends Serializable {

  /**
   * Retrieve a metadata object from this context (and the ones it was derived from). Be aware that
   * metadata is transient.
   *
   * @param name The name of the object.
   * @param <T> The type of the object.
   */
  @SuppressWarnings("TypeParameterUnusedInFormals")
  <T> T getMeta(String name);

  /**
   * Set a metadata object on this, all derived contexts, and all parent contexts (Meta is shared
   * across all contexts in a lineage). Be aware that metadata is transient.
   *
   * @param name The name of the object.
   * @param <T> The type of the object.
   */
  <T> void setMeta(String name, T item);

  /**
   * Return the map of serializable metadata objects from this context (and the ones it was derived
   * from).
   */
  Map<String, Serializable> getSerializableMetadata();

  /**
   * Retrieve a serializable metadata object from this context (and the ones it was derived from).
   *
   * @param name The name of the object.
   * @param <T> The type of the object.
   */
  @SuppressWarnings("TypeParameterUnusedInFormals")
  <T> T getSerializableMeta(String name);

  /**
   * Set a serializable metadata object on this, all derived contexts, and all parent contexts (Meta
   * is shared across all contexts in a lineage).
   *
   * @param name The name of the object.
   * @param item The object to set..
   */
  void setSerializableMeta(String name, Serializable item);

  /** Return a deep copy of this metadata. */
  MetaData deepCopy();

  /**
   * Shorthand method for checking if a boolean serializable meta is present and true. Returns true
   * iff the value is a non-null, true boolean.
   *
   * @throws IllegalArgumentException if the given serializable meta is present but not a boolean.
   */
  default boolean getFlag(String name) {
    Object meta = getSerializableMeta(name);

    if (meta == null) {
      return false;
    }

    if (!(meta instanceof Boolean)) {
      throw new IllegalArgumentException(
          String.format(
              "Serializable meta %s is not a boolean (it's a %s), and cannot be checked as a"
                  + " flag.",
              name, meta.getClass()));
    }

    return (Boolean) meta;
  }
}
