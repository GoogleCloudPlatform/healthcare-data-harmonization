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

package com.google.cloud.verticals.foundations.dataharmonization.target;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.registry.Registrable;

/** A Target is an Output plugin that handles output to specified destinations. */
public interface Target {

  /**
   * Writes information contained in value to a specified destination defined in value.
   *
   * @param ctx the {@link RuntimeContext} object with current stack for executing output to a
   *     specified destination.
   * @param value the {@link Data} object containing information to write to specified destination.
   *     The information in a Dataset of {@link DeferredWrite} objects.
   */
  void write(RuntimeContext ctx, Data value);

  /** A Target.Constructor provides thread-safe creation and initialization of a {@link Target}. */
  interface Constructor extends Registrable {
    /**
     * Returns an instance of Target.
     *
     * @param ctx the {@link RuntimeContext} with current stack for executing I/O to external
     *     destination.
     * @param args the arguments {@link Data} for initializing Target.
     */
    Target construct(RuntimeContext ctx, Data... args);
  }
}
