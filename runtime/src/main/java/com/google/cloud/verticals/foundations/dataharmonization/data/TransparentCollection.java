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

/**
 * A transparent collection allows the application of a {@link Path} to all internal elements,
 * producing a new collection (of the same type) of the results.
 *
 * @param <T> The type of collection that will be returned.
 */
public interface TransparentCollection<T extends TransparentCollection<T>> extends Data {
  /**
   * Applies the given Path to all elements of the collection and returns a new collection of the
   * same type consisting of the results.
   */
  T getThrough(Path remainingPath);

  /** Flattens this collection one level. This assumes that this is a collection of collections. */
  T flatten();

  @Override
  default boolean isTransparentCollection() {
    return true;
  }

  @Override
  default TransparentCollection<T> asTransparentCollection() {
    return this;
  }
}
