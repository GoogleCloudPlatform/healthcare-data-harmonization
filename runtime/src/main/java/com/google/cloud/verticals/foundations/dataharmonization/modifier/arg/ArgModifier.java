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
package com.google.cloud.verticals.foundations.dataharmonization.modifier.arg;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;

/**
 * An ArgModifier is a modifier that can be attached to a WhistleFunction argument and can either
 * short circuit the function execution or modify the argument value when passed into the function.
 */
public interface ArgModifier extends Registrable {

  /** Return true iff the function execution can be skipped based on the argument value. */
  boolean canShortCircuit(Data args);

  /** Return value of the function when it's short circuited. */
  Data getShortCircuitValue(Data arg);

  /** Return the modified argument value that will be passed into the function. */
  default Data modifyArgValue(Data arg) {
    return arg;
  };

}
