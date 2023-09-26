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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.plugin;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import java.util.ArrayList;
import java.util.List;

/** Plugin providing functionality for registering function mocks. */
public class MockingPlugin implements Plugin {
  public static final String MOCKING_PLUGIN_NAME = "mocking";
  public static final String MOCK_META_KEY = "MOCK_META";
  public static final String MOCK_TARGET_META_KEY = "MOCK_TARGET_META";

  @Override
  public String getPackageName() {
    return MOCKING_PLUGIN_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return new ArrayList<>(
        JavaFunction.ofPluginFunctionsInClass(MockingFns.class, MOCKING_PLUGIN_NAME));
  }
}
