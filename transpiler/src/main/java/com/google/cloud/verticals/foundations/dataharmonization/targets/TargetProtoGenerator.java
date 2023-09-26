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
package com.google.cloud.verticals.foundations.dataharmonization.targets;

import com.google.cloud.verticals.foundations.dataharmonization.Environment;
import com.google.cloud.verticals.foundations.dataharmonization.proto.Pipeline.FunctionCall;
import com.google.protobuf.AbstractMessage;

/**
 * Creates a target proto from a path head and path, as specified for a specific keyword (e.x.
 * "var")
 */
public interface TargetProtoGenerator {
  /**
   * Create a proto message representing a target for this keyword, using the given pathHead and
   * path. For example, a var target may use the pathHead as the name of the variable and the path
   * as the field to write on that variable.
   */
  AbstractMessage create(String pathHead, String path, Environment environment);

  /**
   * Create a proto message representing a (custom sink) target using the given pathHead, path, and
   * merge mode.
   */
  FunctionCall createWithMerge(
      String pathHead, String path, Environment environment, String mergeMode);
}
