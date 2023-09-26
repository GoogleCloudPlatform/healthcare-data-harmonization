/*
 * Copyright 2022 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.data.wrappers;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import java.util.Collection;
import java.util.Map;

/** {@link DataTypeImplementation} that provides a certain wrapping of data. */
public abstract class WrapperDataFactory implements DataTypeImplementation {
  protected final DataTypeImplementation backing;

  public WrapperDataFactory(DataTypeImplementation backing) {
    this.backing = backing;
  }

  protected abstract Data wrap(Data data);

  @Override
  public Array emptyArray() {
    return wrap(backing.emptyArray()).asArray();
  }

  @Override
  public Array arrayOf(Collection<? extends Data> items) {
    return wrap(backing.arrayOf(items)).asArray();
  }

  @Override
  public Container emptyContainer() {
    return wrap(backing.emptyContainer()).asContainer();
  }

  @Override
  public Container containerOf(Map<String, ? extends Data> items) {
    return wrap(backing.containerOf(items)).asContainer();
  }

  @Override
  public Primitive primitiveOf(Double num) {
    return wrap(backing.primitiveOf(num)).asPrimitive();
  }

  @Override
  public Primitive primitiveOf(String str) {
    return wrap(backing.primitiveOf(str)).asPrimitive();
  }

  @Override
  public Primitive primitiveOf(Boolean bool) {
    return wrap(backing.primitiveOf(bool)).asPrimitive();
  }
}
