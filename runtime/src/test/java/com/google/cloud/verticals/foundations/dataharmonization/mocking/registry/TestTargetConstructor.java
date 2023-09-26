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

package com.google.cloud.verticals.foundations.dataharmonization.mocking.registry;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;

/** Fake skeleton implementation of {@link Target.Constructor} that only supplies target name. */
public class TestTargetConstructor implements Target.Constructor {
  private final String name;

  TestTargetConstructor(String name) {
    this.name = name;
  }

  @Override
  public Target construct(RuntimeContext ctx, Data... args) {
    throw new UnsupportedOperationException("This method is not implemented.");
  }

  @Override
  public String getName() {
    return name;
  }
}
