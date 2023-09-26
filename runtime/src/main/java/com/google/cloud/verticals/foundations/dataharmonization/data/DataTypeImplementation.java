/*
 * Copyright 2021 Google LLC.
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

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

/**
 * A DataTypeImplementation contains factories of the Array, Container, and Primitive interfaces.
 */
public interface DataTypeImplementation extends Serializable {
  /** Returns an empty Array implementation. */
  Array emptyArray();

  /** Returns an Array implementation filled with the given items. */
  Array arrayOf(Collection<? extends Data> items);

  /** Returns an empty Container implementation. */
  Container emptyContainer();

  /** Returns a Container implementation filled with the given items. */
  Container containerOf(Map<String, ? extends Data> items);

  /**
   * Returns an {@link ArrayBuilder} from this DataTypeImplementation. The dti is used to convert
   * non-{@link Data} elements, so they can be built into an {@link Array}. Default method passes
   * this object to fromDataTypeImplementation of {@link ArrayBuilder}.
   */
  default ArrayBuilder arrayBuilder() {
    return ArrayBuilder.fromDataTypeImplementation(this);
  }

  /**
   * Returns a {@link ContainerBuilder} from this DataTypeImplementation. The dti is used to convert
   * non-{@link Data} values, so they can be built into a {@link Container}. Default method passes
   * this object to fromDataTypeImplementation of {@link ContainerBuilder}.
   */
  default ContainerBuilder containerBuilder() {
    return ContainerBuilder.fromDataTypeImplementation(this);
  }

  /** Returns a Primitive representation of the given number. */
  Primitive primitiveOf(Double num);

  /** Returns a Primitive representation of the given string. */
  Primitive primitiveOf(String str);

  /** Returns a Primitive representation of the given boolean. */
  Primitive primitiveOf(Boolean bool);
}
