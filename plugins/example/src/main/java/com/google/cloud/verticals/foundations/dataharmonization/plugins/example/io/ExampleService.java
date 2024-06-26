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

package com.google.cloud.verticals.foundations.dataharmonization.plugins.example.io;

import java.io.Serializable;

/**
 * A hypothetical interface for a class that performs IO.
 *
 * <p>TODO(): Better async support throughout the runtime.
 */
public interface ExampleService extends Serializable {
  /** Returns a greeting for the user with the given ID. */
  String getUserGreeting(String userId);
}
