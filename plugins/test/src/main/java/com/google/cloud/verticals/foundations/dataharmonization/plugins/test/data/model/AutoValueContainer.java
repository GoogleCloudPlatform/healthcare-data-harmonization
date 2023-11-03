/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model;

import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.errorprone.annotations.DoNotCall;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import javax.annotation.Nonnull;

/**
 * Base class to expose autovalue getters as container fields.
 *
 * <p>TODO(rpolyano): Make this an AutoValue extension?
 */
class AutoValueContainer<T extends AutoValueContainer<T>> implements Container {
  private final Map<String, Function<? super AutoValueContainer<T>, Data>> getters;

  protected AutoValueContainer() {
    getters = new HashMap<>();
  }

  protected void addFieldBinding(Class<T> clazz, String fieldName, Function<T, Data> fieldGetter) {
    getters.put(fieldName, fieldGetter.compose(clazz::cast));
  }

  protected <U> void addFieldBinding(
      Class<T> clazz, String fieldName, Function<T, U> fieldGetter, Function<U, Data> converter) {
    addFieldBinding(clazz, fieldName, fieldGetter.andThen(converter));
  }

  @Nonnull
  @Override
  public Data getField(String field) {
    return getters.containsKey(field) ? getters.get(field).apply(this) : NullData.instance;
  }

  @Override
  @DoNotCall
  public final Container setField(@Nonnull String field, Data value) {
    throw new UnsupportedOperationException();
  }

  @Override
  @DoNotCall
  public final Container removeField(@Nonnull String field) {
    throw new UnsupportedOperationException();
  }

  @Nonnull
  @Override
  public Set<String> fields() {
    return getters.keySet();
  }

  @Override
  @DoNotCall
  public final Data deepCopy() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isWritable() {
    return false;
  }
}
