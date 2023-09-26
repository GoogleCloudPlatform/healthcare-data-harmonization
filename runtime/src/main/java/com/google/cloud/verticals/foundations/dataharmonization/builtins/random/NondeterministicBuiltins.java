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

package com.google.cloud.verticals.foundations.dataharmonization.builtins.random;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Builtins;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.TimeFns;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.FunctionCollectionBuilder;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import java.time.Clock;
import java.util.List;

/** Helper functions with non-deterministic behaviours. */
public class NondeterministicBuiltins implements Plugin {
  private final IdGenerator<String> uuidGenerator;
  private final Clock clock;

  public NondeterministicBuiltins(IdGenerator<String> uuidGenerator, Clock clock) {
    this.uuidGenerator = uuidGenerator;
    this.clock = clock;
  }

  @Override
  public String getPackageName() {
    return Builtins.PACKAGE_NAME;
  }

  @Override
  public List<CallableFunction> getFunctions() {
    return new FunctionCollectionBuilder(Builtins.PACKAGE_NAME)
        .addAllJavaPluginFunctionsInInstance(new Random(this.uuidGenerator))
        .addAllJavaPluginFunctionsInInstance(new TimeFns(clock))
        .addAllJavaPluginFunctionsInClass(TimeFns.class)
        .build();
  }
}
