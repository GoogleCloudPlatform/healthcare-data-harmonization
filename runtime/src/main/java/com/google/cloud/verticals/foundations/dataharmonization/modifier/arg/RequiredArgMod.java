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
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.function.whistle.WhistleFunction;

/**
 * {@link ArgModifier} that skips function execution if the argument provided is null or empty. In
 * other word, a {@link WhistleFunction} will only be executed if all arguments with required
 * modifier are not null or empty.
 */
public class RequiredArgMod implements ArgModifier {
  private static String NAME = "required";

  @Override
  public boolean canShortCircuit(Data args) {
   return args.isNullOrEmpty();
  }

  @Override
  public Data getShortCircuitValue(Data arg) {
    return NullData.instance;
  }

  @Override
  public String getName() {
    return NAME;
  }
}
