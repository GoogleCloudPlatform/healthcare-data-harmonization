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
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;

/** Exception thrown when no overloads match the given arguments and function name. */
public class NoMatchingOverloadsException extends OverloadException {
  public NoMatchingOverloadsException() {
    super(ImmutableList.of(), "No overloads to select from (function not found)");
  }

  public NoMatchingOverloadsException(FunctionReference ref, Set<String> suggestions) {
    // TODO(): Provide function signature in the suggestion message.
    super(
        ImmutableList.of(),
        String.format(
            "Unknown function %s%s()%s",
            ref.getPackageName().isEmpty() ? "" : String.format("%s::", ref.getPackageName()),
            ref.getFunctionName(),
            (suggestions.isEmpty()
                ? ""
                : String.format(", did you mean: %s?", String.join(", ", suggestions)))));
  }

  public NoMatchingOverloadsException(List<CallableFunction> candidates, Data[] args) {
    super(
        candidates,
        String.format(
            "Function does not have any overloads that match given argument types %s.\n"
                + "Known overloads:\n"
                + "%s",
            getDataTypesString(args),
            candidates.stream().map(c -> c.getSignature().toString()).collect(joining("\n"))));
  }

  public NoMatchingOverloadsException(CallableFunction soleCandidate, Data[] args) {
    super(
        ImmutableList.of(soleCandidate),
        String.format(
            "Function %s does not match given argument types %s",
            soleCandidate.getSignature(), getDataTypesString(args)));
  }
}
