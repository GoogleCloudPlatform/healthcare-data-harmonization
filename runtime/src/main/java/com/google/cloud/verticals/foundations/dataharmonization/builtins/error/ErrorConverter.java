/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.builtins.error;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts an exception into Data. Implementations should take care to respect the {@link
 * Engine#isNoDataInExceptionsSet} flag.
 */
@FunctionalInterface
public interface ErrorConverter extends Serializable {
  Data convert(RuntimeContext context, Throwable ex);

  /** Returns an Error converter that merges the output of the two given ones. */
  static ErrorConverter merged(ErrorConverter a, ErrorConverter b) {
    return new MergedErrorConverter(a, b);
  }

  /**
   * Returns an Error converter that merges a container of {@code {field: value}} with the output of
   * the given base converted.
   */
  static ErrorConverter withField(ErrorConverter base, String field, Data value) {
    return merged(base, new ConstantFieldErrorConverter(field, value));
  }

  /**
   * Returns a ErrorConverter with the provided map of fields to Data merged with a base
   * ErrorConverter.
   *
   * @param base A base ErrorConverter.
   * @param fieldMap A map of fields to Data to be merged into the base converter.
   * @return A merged error converter with the fieldMap data present.
   */
  static ErrorConverter withField(ErrorConverter base, Map<String, Data> fieldMap) {
    ArrayList<ErrorConverter> errorConverters = new ArrayList<>();
    // Create ConstantFieldErrorConverters for the provided fieldMap.
    for (Entry<String, Data> e : fieldMap.entrySet()) {
      errorConverters.add(new ConstantFieldErrorConverter(e.getKey(), e.getValue()));
    }
    // Create a base ErrorConverter and then merge the remaining ErrorConverters.ß
    ErrorConverter baseMerge = merged(base, errorConverters.get(0));
    for (int i = 1; i < errorConverters.size(); i++) {
      baseMerge = merged(baseMerge, errorConverters.get(i));
    }
    return baseMerge;
  }

  /** Error converter that merges the result of two given error converters. */
  class MergedErrorConverter implements ErrorConverter {
    private final ErrorConverter mergee;
    private final ErrorConverter merger;

    public MergedErrorConverter(ErrorConverter mergee, ErrorConverter merger) {
      this.mergee = mergee;
      this.merger = merger;
    }

    @Override
    public Data convert(RuntimeContext context, Throwable ex) {
      return mergee
          .convert(context, ex)
          .merge(merger.convert(context, ex), context.getDataTypeImplementation());
    }
  }

  /**
   * Error converter that returns a container with a single preset field. The given exception is
   * ignored.
   */
  class ConstantFieldErrorConverter implements ErrorConverter {
    private final String field;
    private final Data value;

    public ConstantFieldErrorConverter(String field, Data value) {
      this.field = field;
      this.value = value;
    }

    @Override
    public Data convert(RuntimeContext context, Throwable ex) {
      return context.getDataTypeImplementation().containerOf(ImmutableMap.of(field, value));
    }
  }
}
