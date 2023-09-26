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

package com.google.cloud.verticals.foundations.dataharmonization.data.serialization;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import java.io.Serializable;
import java.util.Base64;

/**
 * The Serializer interface provides a mechanism for serializing a {@link Data} object to a byte
 * array.
 */
public interface Serializer<T> extends Serializable {

  /**
   * Serializes a {@link Data} object into a byte array.
   *
   * @param data {@link Data} object to convert to a byte array.
   * @return A new byte array created from the passed in {@link Data} object.
   */
  byte[] serialize(T data);

  /**
   * Serializes a {@link Data} object into a base64 string.
   *
   * @param data {@link Data} object to convert to a base64 string.
   * @return A base64 encoding of {@link #serialize(Object)}.
   */
  default String serializeString(T data) {
    return Base64.getEncoder().encodeToString(serialize(data));
  }
}
