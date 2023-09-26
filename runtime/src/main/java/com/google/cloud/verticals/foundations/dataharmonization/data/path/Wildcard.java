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

package com.google.cloud.verticals.foundations.dataharmonization.data.path;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.TransparentCollection;
import javax.annotation.Nonnull;

/**
 * A Wildcard path segment projects through all elements in an array. That is, the remainder of the
 * path is applied to each element in the array, and the results are composed back into an array.
 */
public final class Wildcard implements PathSegment {
  private final boolean flatten;

  /**
   * Creates a new Wildcard PathSegment.
   *
   * @param flatten true to flatten (nested) arrays by one level before returning.
   */
  public Wildcard(boolean flatten) {
    this.flatten = flatten;
  }

  @Nonnull
  @Override
  public Data get(@Nonnull Data data) {
    return get(data, Path.empty());
  }

  /**
   * Applies the remainingPath to each element in the given data, using {@link
   * TransparentCollection#getThrough(Path)}. Accordingly, the given data must be a {@link
   * TransparentCollection}.
   */
  @Nonnull
  @Override
  public Data get(@Nonnull Data data, Path remainingPath) {
    if (data.isNullOrEmpty()) {
      return NullData.instance;
    }

    if (!data.isTransparentCollection()) {
      throw new UnsupportedOperationException(
          String.format(
              "Wildcard can only be used on transparent collections (but was applied to %s)",
              data.getClass().getSimpleName()));
    }

    TransparentCollection<?> got = data.asTransparentCollection().getThrough(remainingPath);

    // If this is not the last wildcard in the path, then we need to flatten the output of the
    // next wildcard, to match behaviour of JSON Path and not return nested lists.
    if (flatten) {
      got = got.flatten();
    }

    return got;
  }

  @Override
  public Data set(@Nonnull Data data, Data value) {
    throw new UnsupportedOperationException("Setting through wildcards is not yet supported");
    // This is totally doable though. Assert value is an array of the same dimensions as data (or
    // don't, and only fill as far as you can) and then call setElement on each data element with
    // each element of value.
  }

  @Override
  public Data create(DataTypeImplementation dti) {
    return dti.emptyArray();
  }

  @Override
  public boolean isIndex() {
    return true;
  }

  @Override
  public boolean isField() {
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Wildcard && ((Wildcard) obj).flatten == flatten;
  }

  @Override
  public String toString() {
    return "[*]";
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(flatten);
  }
}
