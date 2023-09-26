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

import com.google.cloud.verticals.foundations.dataharmonization.function.context.StackFrame;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.impl.DefaultStackFrame.DefaultBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WhistleStackOverflowError replaces Java StackOverflowError to generate a human readable
 * statistics of Whistle 2 StackFrames.
 */
public class WhistleStackOverflowError extends StackOverflowError {

  public WhistleStackOverflowError(StackFrame stackFrame) {
    super(generateStackStats(stackFrame));
  }

  /** Overrides this method to disable original Java stack trace. */
  @Override
  public synchronized Throwable fillInStackTrace() {
    return this;
  }

  private static String generateStackStats(StackFrame stackFrame) {
    Map<String, Integer> stats = new HashMap<>();
    for (StackFrame ancestor = stackFrame.getParent();
        ancestor != null;
        ancestor = ancestor.getParent()) {
      String name = ancestor.getName();
      stats.merge(name, 1, Integer::sum);
    }
    List<Map.Entry<String, Integer>> list = new ArrayList<>(stats.entrySet());
    list.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
    StringBuilder stringBuilder =
        new StringBuilder("Number of stack frames exceed the max limit: ")
            .append(DefaultBuilder.STACK_FRAMES_LIMIT)
            .append("\n");
    stringBuilder.append("<The top of the stack> ").append(stackFrame.getName()).append("\n");
    stringBuilder.append("<Statistics of the stack>");
    for (Map.Entry<String, Integer> entry : list) {
      stringBuilder.append("\n").append(entry.getKey()).append(": ").append(entry.getValue());
    }
    return stringBuilder.toString();
  }
}
