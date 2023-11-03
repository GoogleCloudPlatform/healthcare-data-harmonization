/*
 * Copyright 2021 Google LLC.
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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.target;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.LoggingFns;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target;
import java.io.Serializable;

/** Custom sink for logging. */
public class LogSevere implements Target, Serializable {

  @Override
  public void write(RuntimeContext ctx, Data value) {
    LoggingFns.logSevere(value.toString());
  }

  /** Constructor for creating an instance of LogInfo */
  public static final class Constructor implements Target.Constructor {
    private static final String TARGET_NAME = "logSevere";

    /**
     * Target which logs a String at the `SEVERE` level. Takes in no parameters.
     *
     * <p>Examples Usage: `logging::logSevere(): StringToLog;`
     */
    @Override
    public Target construct(RuntimeContext ctx, Data... args) {
      return new LogSevere();
    }

    @Override
    public String getName() {
      return TARGET_NAME;
    }
  }
}
