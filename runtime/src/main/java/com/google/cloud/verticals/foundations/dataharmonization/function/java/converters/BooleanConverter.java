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

package com.google.cloud.verticals.foundations.dataharmonization.function.java.converters;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;

/** Converts a {@link Data} to a boolean. */
public class BooleanConverter implements Converter<Boolean> {
  @Override
  public Boolean convert(Data primitive) {
    if (primitive == null) {
      return null;
    }
    if (!primitive.isPrimitive()) {
      throw new IllegalArgumentException(
          String.format(
              "Expected a %s but got %s",
              Boolean.class.getSimpleName(), primitive.getClass().getSimpleName()));
    }
    if (primitive.asPrimitive().num() != null || primitive.asPrimitive().string() != null) {
      throw new IllegalArgumentException(
          String.format(
              "Expected a %s but got %s", Boolean.class.getSimpleName(), primitive.toString()));
    }
    return primitive.asPrimitive().bool();
  }
}
