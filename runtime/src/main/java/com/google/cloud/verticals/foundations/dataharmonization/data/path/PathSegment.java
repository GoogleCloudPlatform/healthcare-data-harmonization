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

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * PathSegment is a base interface for path elements (notably an index or a field). An instance of a
 * path segment represents a specific field or index.
 */
public interface PathSegment extends Serializable {
  // TODO(rpolyano): Move this to a better/configurable location.
  boolean TYPE_MISMATCH_RETURNS_NULL = false;

  /**
   * Retrieves the value of this segment from the given data. The general flow of path traversal in
   * this method should be:
   *
   * <ol>
   *   <li>Call {@link #get(Data)} on the given data to retrieve the value for this specific segment
   *       in the given data.
   *   <li>Call {@link Path#get(Data)} on the result of the above step to get the value specified by
   *       the remaining path (this step would end up recursive).
   *   <li>Post-process and return the result, if needed.
   * </ol>
   *
   * For example, a[*].b[*].d applied to some data D will:
   *
   * <ol>
   *   <li>Field("a").get(D, [*].b[*].d) -> Applies [*].b[*].d to D.a
   *   <li>Wildcard().get(D.a, b[*].d) -> Applies b.c[*].d to every element [0..n] of D.a:
   *       <ol>
   *         <li>Field("b").get(D.a[0], [*].d) -> Applies [*].d to D.a[0].b
   *         <li>Wildcard().get(D.a[0].b, [*].d) -> Applies .d to every element [0..m] of D.a[0].b:
   *             <ol>
   *               <li>Field("d").get(D.a[0].b[0], empty) -> D.a[0].b[0].d
   *               <li>Field("d").get(D.a[0].b[1], empty) -> D.a[0].b[1].d
   *               <li>...
   *               <li>Field("d").get(D.a[0].b[m], empty) -> D.a[0].b[m].d
   *             </ol>
   *         <li>Field("b").get(D.a[1], [*].d) -> Applies [*].d to D.a[1].b
   *         <li>Wildcard().get(D.a[1].b, [*].d) -> Applies .d to every element [0..m] of D.a[1].b:
   *             <ol>
   *               <li>Field("d").get(D.a[1].b[0], empty) -> D.a[1].b[0].d
   *               <li>Field("d").get(D.a[1].b[1], empty) -> D.a[1].b[1].d
   *               <li>...
   *               <li>Field("d").get(D.a[1].b[m], empty) -> D.a[1].b[m].d
   *             </ol>
   *         <li>Field("b").get(D.a[n], [*].d) -> Applies [*].d to D.a[n].b
   *         <li>Wildcard().get(D.a[n].b, [*].d) -> Applies .d to every element [0..m] of D.a[n].b:
   *             <ol>
   *               <li>Field("d").get(D.a[n].b[0], empty) -> D.a[n].b[0].d
   *               <li>Field("d").get(D.a[n].b[1], empty) -> D.a[n].b[1].d
   *               <li>...
   *               <li>Field("d").get(D.a[n].b[m], empty) -> D.a[n].b[m].d
   *             </ol>
   *       </ol>
   *       The first wildcard now post-processes (flattens) the array produced by the subsequent
   *       one.
   * </ol>
   *
   * @param data the container/array/dataset to read from.
   * @param remainingPath the Path after this segment (excluding this segment).
   * @return the value of this segment, or {@link NullData#instance}.
   */
  @Nonnull
  default Data get(@Nonnull Data data, Path remainingPath) {
    return remainingPath.get(get(data));
  }

  @Nonnull
  Data get(@Nonnull Data data);

  /**
   * Sets the value of this segment on the given data to the given value, returning the modified
   * data. This may or may not modify the data in-place, depending on the data's implementation.
   *
   * @param data the container/array/dataset to write to.
   * @param value the value to set.
   */
  Data set(@Nonnull Data data, Data value);

  /**
   * Creates a new instance of the appropriate type (e.g. an {@link Array} or {@link Container})
   * that can be passed as the first parameter to this instance's {@link #set(Data, Data)} method.
   *
   * @return a new, empty {@link Array} or {@link Container}.
   */
  Data create(DataTypeImplementation dti);

  /** @return true if the implementation of this PathSegment is an {@link Index}. */
  boolean isIndex();

  /** @return true if the implementation of this PathSegment is a {@link Field}. */
  boolean isField();
}
