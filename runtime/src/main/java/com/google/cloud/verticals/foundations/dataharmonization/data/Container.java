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

import com.google.common.collect.Sets;
import java.util.Set;
import javax.annotation.Nonnull;

/** A Container is a mapping between a set of fields to values. */
public interface Container extends Data {
  /**
   * Gets the value of the given field, or {@link NullData} if it does not exist.
   *
   * @param field the field to get. If field is null should return this instance itself.
   */
  @Nonnull
  Data getField(String field);

  /**
   * Sets (overwrites if existing) the value of the given field. Returns the modified container.
   * This may or may not operate in-place (implementation dependent).
   *
   * @param field the field to set.
   * @param value the value to assign.
   */
  Container setField(@Nonnull String field, Data value);

  /**
   * Removes the given field from the container. Returns the modified container. This may or may not
   * operate in-place (implementation dependent).
   *
   * @param field the field to clear.
   */
  Container removeField(@Nonnull String field);

  /**
   * Returns the set of fields that have been assigned in this Container.
   */
  @Nonnull
  Set<String> fields();

  @Override
  default boolean isContainer() {
    return true;
  }

  @Override
  default Container asContainer() {
    return this;
  }

  @Override
  default boolean isNullOrEmpty() {
    return nonNullFields().isEmpty();
  }

  /**
   * Gets the set of non null fields assigned to this container
   *
   * @return the list of fields
   */
  default Set<String> nonNullFields() {
    return Sets.filter(
        fields(),
        s -> {
          Data value = getField(s);
          return value != null && !value.isNullOrEmpty();
        });
  }
}
