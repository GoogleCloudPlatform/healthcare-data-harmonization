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

package com.google.cloud.verticals.foundations.dataharmonization.target.impl;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * DebugTarget is an implementation of the {@link Target} interface. This class is used for testing
 * output values by writing a JSON representation of value passed to write() method to {@link
 * Logger}.
 */
public class DebugTarget implements Target {
  public static final String TARGET_NAME = "Debug";

  @Override
  public void write(RuntimeContext ctx, Data value) {
    Gson gson = new GsonBuilder().create();
    Logger.getGlobal().log(Level.INFO, gson.toJson(value));
  }

  /** A Target.Builder provides thread-safe creation and initialization of a {@link Target}. */
  public static class Constructor implements Target.Constructor {
    @Override
    public DebugTarget construct(RuntimeContext ctx, Data... args) {
      return new DebugTarget();
    }

    @Override
    public String getName() {
      return TARGET_NAME;
    }
  }
}
