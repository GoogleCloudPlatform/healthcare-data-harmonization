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
package com.google.cloud.verticals.foundations.dataharmonization.exceptions;

import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import java.util.List;

/** Exception thrown when more than one overloads match the given arguments and function name. */
public class MultipleMatchingOverloadsException extends OverloadException {

  public MultipleMatchingOverloadsException(List<CallableFunction> candidates, Data[] args) {
    super(
        candidates,
        String.format(
            "Function has multiple overloads that all match given argument types %s.\n"
                + "Matching overloads:\n"
                + "\t%s",
            getDataTypesString(args),
            candidates.stream().map(c -> c.getSignature().toString()).collect(joining("\n\t"))));
  }
}
