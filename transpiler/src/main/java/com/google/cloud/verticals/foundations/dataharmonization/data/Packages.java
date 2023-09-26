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
package com.google.cloud.verticals.foundations.dataharmonization.data;

import com.google.common.collect.ImmutableMap;

/** Saves aliases for common packages. This will be dynamically registered in the future. */
public final class Packages {
  public static final ImmutableMap<String, String> ALIAS =
      ImmutableMap.<String, String>builder()
          .put(
              "test",
              "class://com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestPlugin")
          .buildOrThrow();

  private Packages() {}
  ;
}
