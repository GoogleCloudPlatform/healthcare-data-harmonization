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
import java.util.Base64;

/**
 * The Deserializer interface provides a mechanism for deserializing a byte array into a DCAP
 * (Dataset, Container, Array, Primitive) ELP Data object.
 */
public interface Deserializer {

  /**
   * Uses some serialization to convert a byte array into an instance of a Data object.
   *
   * @param encoded the array of bytes to convert to a {@link Data} object
   * @return A new {@link Data} object created from the passed in byte array.
   */
  Data deserialize(byte[] encoded);

  /**
   * Uses base64 decoding and {@link #deserialize(byte[])} to convert a string into an instance of a
   * Data object.
   *
   * @param encoded the base64 string to convert to a {@link Data} object
   * @return A new {@link Data} object created from the passed in string.
   */
  default Data deserialize(String encoded) {
    return deserialize(Base64.getDecoder().decode(encoded));
  }
}
