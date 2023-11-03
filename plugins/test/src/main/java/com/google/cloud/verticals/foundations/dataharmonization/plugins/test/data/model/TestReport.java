/*
 * Copyright 2022 Google LLC.
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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toUnmodifiableList;

import com.google.auto.value.AutoValue;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.DataTypeImplementation;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.function.Predicate;

/** The results of running a suite of tests. */
@AutoValue
public abstract class TestReport extends AutoValueContainer<TestReport> {

  public static Data of(DataTypeImplementation dti, TestRunReport... runs) {
    Builder builder = builder();
    int failed = 0;
    for (TestRunReport run : runs) {
      builder.addTest(run);
      failed += run.isFailed() ? 1 : 0;
    }
    builder.setNumFailed(failed);
    builder.setNumRun(runs.length);
    return builder.build(dti);
  }

  public abstract int getNumFailed();

  public abstract int getNumRun();

  public final int getNumPassed() {
    return getNumRun() - getNumFailed();
  }

  public abstract ImmutableList<TestRunReport> getTests();

  public static TestReport.Builder builder() {
    return new AutoValue_TestReport.Builder();
  }

  public String prettyPrint() {
    return String.format(
        "Summary: %d failed, %d passed\nFailed:\n%s\nPassed:\n%s",
        getNumFailed(),
        getNumPassed(),
        prettyPrint(
            getTests().stream().filter(TestRunReport::isFailed).collect(toUnmodifiableList())),
        prettyPrint(
            getTests().stream()
                .filter(Predicate.not(TestRunReport::isFailed))
                .collect(toUnmodifiableList())));
  }

  private String prettyPrint(List<TestRunReport> runs) {
    return runs.stream()
        .map(t -> t.prettyPrint().lines().map(l -> "  " + l).collect(joining("\n")))
        .collect(joining("\n\n"));
  }

  /** Builder for TestReport. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setNumFailed(int value);

    public abstract Builder setNumRun(int value);

    public abstract ImmutableList.Builder<TestRunReport> testsBuilder();

    @CanIgnoreReturnValue
    public final Builder addTest(TestRunReport test) {
      testsBuilder().add(test);
      return this;
    }

    abstract TestReport autoBuild();

    public final TestReport build(DataTypeImplementation dti) {
      TestReport testReport = autoBuild();
      testReport.addFieldBinding(
          TestReport.class,
          "numFailed",
          TestReport::getNumFailed,
          d -> dti.primitiveOf(Double.valueOf(d)));
      testReport.addFieldBinding(
          TestReport.class,
          "numPassed",
          TestReport::getNumPassed,
          d -> dti.primitiveOf(Double.valueOf(d)));
      testReport.addFieldBinding(
          TestReport.class,
          "numRun",
          TestReport::getNumRun,
          d -> dti.primitiveOf(Double.valueOf(d)));
      testReport.addFieldBinding(TestReport.class, "tests", TestReport::getTests, dti::arrayOf);
      return testReport;
    }
  }
}
