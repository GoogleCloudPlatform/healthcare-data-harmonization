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
package com.google.cloud.verticals.foundations.dataharmonization.data.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Container;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.cloud.verticals.foundations.dataharmonization.data.Primitive;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/** Utility class for making Data for tests. For now just uses DefaultDTI. */
public class TestDataTypeImplementation implements DataTypeImplementation {

  @Override
  public Array emptyArray() {
    return DefaultDataTypeImplementation.instance.emptyArray();
  }

  @Override
  public Array arrayOf(Collection<? extends Data> items) {
    return DefaultDataTypeImplementation.instance.arrayOf(items);
  }

  @SafeVarargs
  public final <T extends Data> Array arrayOf(T... items) {
    return arrayOf(Arrays.asList(items));
  }

  @Override
  public Container emptyContainer() {
    return DefaultDataTypeImplementation.instance.emptyContainer();
  }

  @Override
  public Container containerOf(Map<String, ? extends Data> items) {
    return DefaultDataTypeImplementation.instance.containerOf(items);
  }

  @Override
  public Primitive primitiveOf(Double num) {
    return DefaultDataTypeImplementation.instance.primitiveOf(num);
  }

  @Override
  public Primitive primitiveOf(String str) {
    return DefaultDataTypeImplementation.instance.primitiveOf(str);
  }

  @Override
  public Primitive primitiveOf(Boolean bool) {
    return DefaultDataTypeImplementation.instance.primitiveOf(bool);
  }

  /** Utility method to be staticly imported. */
  public static TestDataTypeImplementation testDTI() {
    return new TestDataTypeImplementation();
  }
}
