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

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.Core;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Base class for exceptions caused trying to find overloads given some function candidates and
 * arguments.
 */
public abstract class OverloadException extends RuntimeException {
  private final ImmutableList<CallableFunction> candidates;

  protected OverloadException(List<CallableFunction> candidates, String message) {
    super(message);
    this.candidates = ImmutableList.copyOf(candidates);
  }

  public List<CallableFunction> getCandidates() {
    return candidates;
  }

  /**
   * Prints the types (Classes) of the given {@link Data}s as human readable String. Useful for
   * errors.
   */
  protected static String getDataTypesString(Data[] args) {
    return stream(args)
        .map(a -> a != null ? String.join("/", Core.types(a)) : "null")
        .collect(joining(", "));
  }
}
