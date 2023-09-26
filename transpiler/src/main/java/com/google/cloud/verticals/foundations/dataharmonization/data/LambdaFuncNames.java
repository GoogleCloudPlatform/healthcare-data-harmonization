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

package com.google.cloud.verticals.foundations.dataharmonization.data;

/** Lambda function name templates. */
public final class LambdaFuncNames {
  public static final String TERNARY_THEN = "ternary-then_";
  public static final String TERNARY_ELSE = "ternary-else_";
  public static final String INFIX_OPERATOR = "infix-operator_";
  public static final String BLOCK = "block_";

  private static final String SELECTOR = "selector-%s_";

  /** Produces a selector name prefix incorporating the given selector name. */
  public static String selector(String name) {
    return String.format(SELECTOR, name);
  }
}
