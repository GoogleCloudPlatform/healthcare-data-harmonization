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
package com.google.cloud.verticals.foundations.dataharmonization.data.function.reference;

import java.util.Collections;
import java.util.List;

/** Generators proto for higher order function. */
public class HigherOrderFcnRefProtoGenerator extends DefaultFcnRefProtoGenerator {

  private final List<String> freeArgs;

  public HigherOrderFcnRefProtoGenerator(String functionName, List<String> freeArgs) {
    this("", functionName, freeArgs);
  }

  public HigherOrderFcnRefProtoGenerator(String functionName) {
    this("", functionName, Collections.emptyList());
  }

  protected HigherOrderFcnRefProtoGenerator(
      String packageName, String functionName, List<String> freeArgs) {
    super(packageName, functionName);
    this.freeArgs = freeArgs;
  }

  public List<String> getFreeArgs() {
    return Collections.unmodifiableList(freeArgs);
  }
}
