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
import java.io.Serializable;

/**
 * Converts a parameter from {@link Data} to some specific type. Does not necessarily cast.
 *
 * @param <DestT> The output type to convert to.
 */
@FunctionalInterface
public interface Converter<DestT> extends Serializable {
  DestT convert(Data source);
}
