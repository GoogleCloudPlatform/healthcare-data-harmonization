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

import static com.google.common.math.DoubleMath.fuzzyEquals;
import static java.lang.StrictMath.round;

/** A Primitive represents a single value - a String, Double, or Boolean. */
public interface Primitive extends Data {

  /** Returns the numeric value of this primitive or null if it is not numeric. */
  Double num();

  /** Returns the string value of this primitive or null if it is not a string. */
  String string();

  /** Returns the boolean value of this primitive or null if it is not a boolean. */
  Boolean bool();

  /** Returns the rounded numeric value of this primitive or null if it is not numeric. */
  default Long rounded() {
    return num() != null ? round(num()) : null;
  }

  @Override
  default boolean isWritable() {
    return false;
  }

  @Override
  default boolean isPrimitive() {
    return true;
  }

  @Override
  default Primitive asPrimitive() {
    return this;
  }

  /**
   * Returns true if this is a numeric value with a tiny fraction (less than the {@link Math#ulp} of
   * the integer value).
   */
  default boolean isFractionNegligible() {
    Long rounded = rounded();
    return rounded != null && fuzzyEquals(num(), rounded, Math.ulp(Double.valueOf(rounded)));
  }
}
