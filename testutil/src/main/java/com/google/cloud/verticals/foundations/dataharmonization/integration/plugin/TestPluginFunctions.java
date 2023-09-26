// Copyright 2021 Google LLC.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.cloud.verticals.foundations.dataharmonization.integration.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.errorprone.annotations.DoNotCall;

/**
 * Functions to use in integration tests.
 */
public final class TestPluginFunctions {
  @DoNotCall("Always throws java.lang.RuntimeException")
  @PluginFunction
  public static Data error(String message) {
    throw new RuntimeException(message);
  }

  @DoNotCall("Always throws a nested RuntimeException")
  @PluginFunction
  public static Data nestedError(String message1, String message2) {
    throw new IllegalArgumentException(message1, new UnsupportedOperationException(message2));
  }

  private TestPluginFunctions() {}
}
