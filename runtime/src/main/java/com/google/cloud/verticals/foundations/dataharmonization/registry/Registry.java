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

package com.google.cloud.verticals.foundations.dataharmonization.registry;

import java.io.Serializable;
import java.util.Set;

/** Simple mapping from Name (from {@link Registrable#getName()} to {@link Registrable}. */
public interface Registry<RegistrantT extends Registrable> extends Serializable {
  /**
   * Add a mapping from the given Registrant's name to its instance. Overwrites any existing
   * mappings with the same name.
   */
  void register(RegistrantT registrant);

  /** Retrieve the instance of the Registrant with the given name, or null if none is registered. */
  RegistrantT get(String name);

  /** Returns all unique registrants in the Registry. */
  Set<RegistrantT> getAll();
}
