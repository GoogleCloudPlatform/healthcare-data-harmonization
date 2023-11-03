/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test;

import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.FunctionCollectionBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions.AssertFns;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions.RunnerFns;
import java.util.List;

/** The TestPlugin provides helpful functions for unit testing whistle code. */
public class TestPlugin implements Plugin {

  public static final String PACKAGE_NAME = "test";

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return new FunctionCollectionBuilder(PACKAGE_NAME)
        .addAllJavaPluginFunctionsInClass(AssertFns.class)
        .addAllJavaPluginFunctionsInClass(RunnerFns.class)
        .build();
  }
}
