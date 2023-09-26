// Copyright 2022 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.cloud.verticals.foundations.dataharmonization.data;

import static com.google.cloud.verticals.foundations.dataharmonization.builtins.Core.types;

/** Utility methods for verifying/enforcing Data to be of a certain disposition. */
public final class Preconditions {
  private Preconditions() {}

  /**
   * Requires the given data to be a primitive with a non-empty string value. If not, throws a
   * helpful exception.
   *
   * @param data the data to verify
   * @param name the name of the parameter being verified, used in the error.
   */
  public static String requireNonEmptyString(Data data, String name) {
    if (data.isNullOrEmpty()) {
      throw new IllegalArgumentException(
          String.format("Expected a non-empty string for %s but got null", name));
    }
    if (!data.isPrimitive()) {
      throw new IllegalArgumentException(
          String.format(
              "Expected a non-empty string for %s but got a %s",
              name, String.join("/", types(data))));
    }
    Primitive prim = data.asPrimitive();
    if (prim.string() == null) {
      throw new IllegalArgumentException(
          String.format(
              "Expected a non-empty string for %s but got a non-string primitive %s", name, data));
    }
    if (prim.string().length() == 0) {
      throw new IllegalArgumentException(
          String.format("Expected a non-empty string for %s but got an empty string", name));
    }

    return prim.string();
  }

  /**
   * Requires the given data to be a primitive with a number.
   *
   * @param data the data to verify
   * @param name the name of the parameter being verified, used in the error.
   */
  public static double requireNum(Data data, String name) {
    if (data == null || data.isNullOrEmpty()) {
      return 0;
    }
    if (!data.isPrimitive()) {
      throw new IllegalArgumentException(
          String.format(
              "Expected a number for %s but got a %s", name, String.join("/", types(data))));
    }
    Primitive prim = data.asPrimitive();
    if (prim.num() == null && (prim.string() != null || prim.bool() != null)) {
      throw new IllegalArgumentException(
          String.format("Expected a number for %s but got a non-numeric primitive %s", name, data));
    }
    return prim.num() != null ? prim.num() : 0;
  }
}
