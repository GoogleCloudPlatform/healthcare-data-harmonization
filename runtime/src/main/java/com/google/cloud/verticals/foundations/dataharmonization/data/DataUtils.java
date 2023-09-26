/*
 * Copyright 2023 Google LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.verticals.foundations.dataharmonization.data;

/** Utilities for working with {@link Data}. */
public final class DataUtils {

  /**
   * Returns the boolean value if the element is a {@link Primitive} Boolean. Otherwise, returns
   * true if the element is not null or empty.
   */
  public static boolean isTruthy(Data element) {
    if (element == null) {
      return false;
    }
    return isTruePrimitive(element) || (isNonEmpty(element) && !isBoolean(element));
  }

  /** Returns true if the element is a {@link Primitive} Boolean, and its value is true. */
  public static boolean isTruePrimitive(Data element) {
    if (isBoolean(element)) {
      return element.asPrimitive().bool();
    }
    return false;
  }

  /** Returns true if the element is a {@link Primitive} String. */
  public static boolean isString(Data element) {
    return element.isPrimitive() && element.asPrimitive().string() != null;
  }

  /** Returns true if the element is a {@link Primitive} Double. */
  public static boolean isNumber(Data element) {
    return element.isPrimitive() && element.asPrimitive().num() != null;
  }

  /** Returns true if the element is a {@link Primitive} Boolean. */
  public static boolean isBoolean(Data element) {
    return element.isPrimitive() && element.asPrimitive().bool() != null;
  }

  /** Returns true if the element is a {@link Primitive} String, and matches the given String. */
  public static boolean equals(Data element, String string) {
    return isString(element) && element.asPrimitive().string().equals(string);
  }

  /** Returns true if the element is a {@link Primitive} Double, and matches the given Double. */
  public static boolean equals(Data element, Double number) {
    return isNumber(element) && element.asPrimitive().num().equals(number);
  }

  /** Returns true if the element is not null or empty */
  public static boolean isNonEmpty(Data element) {
    return element != null && !element.isNullOrEmpty();
  }

  private DataUtils() {}
}
