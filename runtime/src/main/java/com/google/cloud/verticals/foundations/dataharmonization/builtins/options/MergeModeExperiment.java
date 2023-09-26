/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.builtins.options;

import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.plugin.Option;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.PipelineConfig;

/** Experiment option to enable explicit merge modes (in the runtime). */
public class MergeModeExperiment implements Option {

  @Override
  public RuntimeContext enable(RuntimeContext context, PipelineConfig.Option config) {
    return Option.withOption(context, this);
  }

  @Override
  public RuntimeContext disable(RuntimeContext context) {
    return Option.withoutOption(context, this);
  }

  @Override
  public String getName() {
    return Option.experiment("merge_modes");
  }

  public static boolean isEnabled(RuntimeContext context) {
    return context.enabledOptions().stream().anyMatch(MergeModeExperiment.class::isInstance);
  }
}
