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

package com.google.cloud.verticals.foundations.dataharmonization.data;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.common.collect.ImmutableMap;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

/** Builds a {@link Container} */
public class ContainerBuilder {

  private final DataTypeImplementation dti;
  private final ImmutableMap.Builder<String, Data> mapBuilder;

  private ContainerBuilder(DataTypeImplementation dti) {
    this.dti = dti;
    this.mapBuilder = ImmutableMap.builder();
  }

  /** Creates a new ContainerBuilder from a {@link RuntimeContext}. */
  @CanIgnoreReturnValue
  public static ContainerBuilder fromContext(RuntimeContext ctx) {
    return new ContainerBuilder(ctx.getDataTypeImplementation());
  }

  /** Creates a new ContainerBuilder from a {@link DataTypeImplementation}. */
  @CanIgnoreReturnValue
  public static ContainerBuilder fromDataTypeImplementation(DataTypeImplementation dti) {
    return new ContainerBuilder(dti);
  }

  /** Copies the value of fromField in fromContainer, and puts it in toField. */
  @CanIgnoreReturnValue
  public ContainerBuilder copy(Container fromContainer, String fromField, String toField) {
    mapBuilder.put(toField, fromContainer.getField(fromField));
    return this;
  }

  /** Copies the value for the given field in the fromContainer, and puts it in the same field. */
  @CanIgnoreReturnValue
  public ContainerBuilder copy(Container fromContainer, String field) {
    return copy(fromContainer, field, field);
  }

  /** Puts the given {@link Data} value in the given field. */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, Data value) {
    mapBuilder.put(field, value == null ? NullData.instance : value);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put the given Collection of {@link Data} in the
   * given field, converted to an {@link Array} value.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, Collection<? extends Data> arrayElements) {
    mapBuilder.put(field, arrayElements == null ? NullData.instance : dti.arrayOf(arrayElements));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put the given Map of {@link Data} in the given
   * field, converted to a {@link Container} value. If the Map is null, sets the field to {@link
   * NullData}.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, Map<String, ? extends Data> map) {
    mapBuilder.put(field, map == null ? NullData.instance : dti.containerOf(map));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put the given String in the given field, converted
   * to a {@link Primitive} value. If the String is null, sets the field to {@link NullData}.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, String primitive) {
    mapBuilder.put(field, primitive == null ? NullData.instance : dti.primitiveOf(primitive));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put the given Double in the given field, converted
   * to a {@link Primitive} value. If the Double is null, sets the field to {@link NullData}.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, Double primitive) {
    mapBuilder.put(field, primitive == null ? NullData.instance : dti.primitiveOf(primitive));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put the given Boolean in the given field, converted
   * to a {@link Primitive} value. If the Boolean is null, sets the field to {@link NullData}.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder put(String field, Boolean primitive) {
    mapBuilder.put(field, primitive == null ? NullData.instance : dti.primitiveOf(primitive));
    return this;
  }

  /** Puts an Entry of String to Data. */
  @CanIgnoreReturnValue
  public ContainerBuilder putEntry(Entry<String, Data> entry) {
    if (entry == null) {
      return this;
    }
    mapBuilder.put(entry);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put an Entry of String to String, with the value
   * converted to a {@link Primitive}. If the Entry is null, does nothing.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putStringEntry(Entry<String, String> entry) {
    if (entry == null) {
      return this;
    }
    mapBuilder.put(entry.getKey(), dti.primitiveOf(entry.getValue()));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put an Entry of String to Boolean, with the value
   * converted to a {@link Primitive}. If the Entry is null, does nothing.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putBooleanEntry(Entry<String, Boolean> entry) {
    if (entry == null) {
      return this;
    }
    mapBuilder.put(entry.getKey(), dti.primitiveOf(entry.getValue()));
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to put an Entry of String to Double, with the value
   * converted to a {@link Primitive}. If the Entry is null, does nothing.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putNumberEntry(Entry<String, Double> entry) {
    if (entry == null) {
      return this;
    }
    mapBuilder.put(entry.getKey(), dti.primitiveOf(entry.getValue()));
    return this;
  }

  /** Copies all entries of a given Map of Data values. If the map is null, does nothing. */
  @CanIgnoreReturnValue
  public ContainerBuilder putAll(Map<String, Data> map) {
    if (map == null) {
      return this;
    }
    mapBuilder.putAll(map);
    return this;
  }

  /**
   * Copies all entries of an existing {@link Container}. If the Container is null, does nothing.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putAll(Container container) {
    if (container == null) {
      return this;
    }
    container.fields().forEach(f -> mapBuilder.put(f, container.getField(f)));
    return this;
  }

  /** Copies all entries of an existing {@link ContainerBuilder}. */
  @CanIgnoreReturnValue
  public ContainerBuilder putAll(ContainerBuilder containerBuilder) {
    return putAll(containerBuilder.build());
  }

  /**
   * Uses the {@link DataTypeImplementation} to copy all entries of a given Map of String values,
   * converted to a {@link Container} of String {@link Primitive} values.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putAllStrings(Map<String, String> map) {
    if (map == null) {
      return this;
    }
    map.entrySet().forEach(this::putStringEntry);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to copy all entries of a given Map of Boolean values,
   * converted to a {@link Container} of Boolean {@link Primitive} values.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putAllBooleans(Map<String, Boolean> map) {
    if (map == null) {
      return this;
    }
    map.entrySet().forEach(this::putBooleanEntry);
    return this;
  }

  /**
   * Uses the {@link DataTypeImplementation} to copy all entries of a given Map of Double values,
   * converted to a {@link Container} of Double {@link Primitive} values.
   */
  @CanIgnoreReturnValue
  public ContainerBuilder putAllNumbers(Map<String, Double> map) {
    if (map == null) {
      return this;
    }
    map.entrySet().forEach(this::putNumberEntry);
    return this;
  }

  /** Builds a Container returns the result. */
  public Container build() {
    return dti.containerOf(mapBuilder.buildOrThrow());
  }
}
