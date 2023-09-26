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
package com.google.cloud.verticals.foundations.dataharmonization.error;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Contains one or more Transpilation Issues that occurred.
 *
 * <p>TODO(rpolyano): Make this a checked exception.
 */
public class TranspilationException extends RuntimeException {
  private final List<TranspilationIssue> issues;

  public TranspilationException(List<TranspilationIssue> issues) {
    super(generateExceptionMessage(issues));
    this.issues = issues;
  }

  private static String generateExceptionMessage(List<TranspilationIssue> issues) {
    // General format is something like (between the --------):
    // ----------------------------------
    // Errors occurred during transpilation:
    // file://x/y/z.wstl
    //   1:2-1:5 - something went wrong here
    //   2:17-2:19 - something else went wrong here
    // file://x/y/abc.wstl
    //   1:2-1:5 - something went wrong here too
    //   2:17-2:19 - something else went wrong here too
    // ----------------------------------
    // Note that currently transpiler only works on one file at a time, but this format will work
    // with multiple files as shown above just fine.
    return String.format(
        "Errors occurred during transpilation:\n%s",
        issues.stream().collect(groupingBy(TranspilationIssue::getFile)).entrySet().stream()
            .map(
                e ->
                    e.getKey().getUrl()
                        + "\n"
                        + e.getValue().stream()
                            .map(TranspilationException::getIssueString)
                            .collect(joining("\n\t")))
            .collect(joining("\n\n")));
  }

  private static String getIssueString(TranspilationIssue issue) {
    return String.format(
        "%d:%d-%d:%d - %s",
        issue.getLine(), issue.getCol(), issue.getEndLine(), issue.getEndCol(), issue.getMessage());
  }

  public List<TranspilationIssue> getIssues() {
    return ImmutableList.copyOf(issues);
  }
}
