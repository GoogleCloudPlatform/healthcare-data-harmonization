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
package com.google.cloud.verticals.foundations.dataharmonization.function;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import java.io.Serializable;
import java.util.List;

/**
 * OverloadSelector is a class used for selecting a function implementation, by comparing its
 * signature to some given arguments. 
 */
@FunctionalInterface
public interface OverloadSelector extends Serializable {
  /**
   * Selects the {@link CallableFunction} from the given list whose signature best matches the given
   * args. Implementation can vary.
   */
  CallableFunction select(List<CallableFunction> overloadGroup, Data[] args);
}
