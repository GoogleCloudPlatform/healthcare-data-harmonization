/*
 * Copyright 2023 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import java.util.Objects;

/**
 * ExplicitEmptyString is an implementation of the {@link Primitive} interface, to allow for
 * explicit empty Strings as values in Whistle. Note, this class returns false on a call to
 * `isNullOrEmpty()`.
 */
public class ExplicitEmptyString implements Primitive {

  @Override
  public boolean isNullOrEmpty() {
    return false;
  }

  @Override
  public Data deepCopy() {
    return new ExplicitEmptyString();
  }

  @Override
  public Double num() {
    return null;
  }

  @Override
  public String string() {
    return "";
  }

  @Override
  public Boolean bool() {
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ExplicitEmptyString) {
      return true;
    }
    if (!(o instanceof Data) || !((Data) o).isPrimitive()) {
      return false;
    }
    Primitive other = ((Data) o).asPrimitive();
    // Check if the primitive is a string type, and that it is an empty string.
    return other.string() != null && other.string().isEmpty();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode("");
  }
}
