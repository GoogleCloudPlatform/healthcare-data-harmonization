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

import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

/** An Array is a collection of values with indices. */
public interface Array extends Data, TransparentCollection<Array> {

  /**
   * Gets the value of the given field, or {@link NullData} if it does not exist.
   *
   * @param index the index to get.
   */
  @Nonnull
  Data getElement(int index);

  /**
   * Sets (overwrites if existing) the value of the given index. If the index is out of range, the
   * array is extended to the required length and all missing values will be filled in with {@link
   * NullData}. Returns the modified array. This may or may not operate in-place (implementation
   * dependent).
   *
   * @param index the index to set. Must be a positive integer.
   * @param value the value to assign.
   */
  Array setElement(int index, @Nonnull Data value);

  /**
   * Sets (overwrites if existing) the value of the given index. If the index is out of range, the
   * array is extended to the required length and all missing values will be filled in with {@link
   * NullData}. Returns the modified array. This may or may not operate in-place (implementation
   * dependent).
   *
   * <p>This method differs from {@link #setElement(int, Data)} by ensuring that rather than being
   * appended in a merge, the given index is merged with the index of the other merging array.
   *
   * @param index the index to set. Must be a positive integer.
   * @param value the value to assign.
   */
  default Array setFixedElement(int index, @Nonnull Data value) {
    return setElement(index, value);
  }

  /**
   * Returns true if the given index should not be appended when this array is merged into another.
   * See {@link #setFixedElement(int, Data)}.
   */
  default boolean isFixed(int index) {
    return false;
  }

  /** @return the number of elements in this Array. */
  int size();

  @Override
  default boolean isArray() {
    return true;
  }

  @Override
  default Array asArray() {
    return this;
  }

  @Override
  default boolean isNullOrEmpty() {
    return size() == 0;
  }

  /** Returns a sequential {@link Stream} of the elements in this array. */
  default Stream<Data> stream() {
    return IntStream.range(0, size()).mapToObj(this::getElement);
  }
}
