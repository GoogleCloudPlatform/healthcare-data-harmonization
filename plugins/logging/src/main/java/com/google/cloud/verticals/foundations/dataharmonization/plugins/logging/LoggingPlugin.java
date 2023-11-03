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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.logging;

import com.google.cloud.verticals.foundations.dataharmonization.plugin.Plugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.target.LogInfo;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.target.LogSevere;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.logging.target.LogWarning;
import com.google.cloud.verticals.foundations.dataharmonization.target.Target.Constructor;
import com.google.common.collect.Lists;
import java.util.List;

/** A plugin that adds functions to enable user defined logging. */
public class LoggingPlugin implements Plugin {

  public static final String PACKAGE_NAME = "logging";

  @Override
  public String getPackageName() {
    return PACKAGE_NAME;
  }

  @Override
  public List<Constructor> getTargets() {
    return Lists.newArrayList(
        new LogInfo.Constructor(),
        new LogWarning.Constructor(),
        new LogSevere.Constructor());
  }
}
