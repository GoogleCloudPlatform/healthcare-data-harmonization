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

package com.google.cloud.verticals.foundations.dataharmonization.registry.util;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/** A common interface to define string similarities used in Whistle. */
public interface StringSimilarity extends Serializable {

  /**
   * Picks the best matches to the target string from candidate strings in terms of the metric.
   *
   * @param candidates the candidate strings
   * @param target the target string
   * @return a list of the best matches
   */
  List<String> pick(Set<String> candidates, String target);

  /** Returns true if the text and target are similar under the specified criteria. */
  boolean accept(String text, String target);
}
