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

import com.google.auto.value.AutoValue;
import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.impl.DefaultErrorConverter;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.impl.DefaultPrimitive;
import com.google.cloud.verticals.foundations.dataharmonization.data.path.Path;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import java.util.Optional;

/** The results of running a single test method. */
@AutoValue
public abstract class TestRunReport extends AutoValueContainer<TestRunReport> {
  public abstract String getName();

  public abstract Optional<WhistleRuntimeException> getError();

  public abstract Optional<WhistleRuntimeException> getFailure();

  public static Builder builder() {
    return new AutoValue_TestRunReport.Builder();
  }

  public boolean isFailed() {
    return getError().isPresent() || getFailure().isPresent();
  }

  public String prettyPrint() {
    StringBuilder sb = new StringBuilder();
    sb.append("TEST ");
    sb.append(getName());
    sb.append(' ');

    WhistleRuntimeException ex = null;
    if (getError().isPresent()) {
      sb.append("ERROR");
      ex = getError().get();
    } else if (getFailure().isPresent()) {
      sb.append("FAIL");
      ex = getFailure().get();
    } else {
      sb.append("PASS");
    }
    sb.append("\n");
    if (ex != null) {
      sb.append(ex.getCause());
      sb.append("\n");
      for (StackTraceElement stackTraceElement : ex.getStackTrace()) {
        sb.append("  at ");
        sb.append(stackTraceElement);
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  @Override
  public Data merge(RuntimeContext ctx, Data other, Path path) {
    if (!path.isEmpty() || !other.isClass(TestRunReport.class)) {
      return super.merge(ctx, other, path);
    }

    return TestReport.of(ctx.getDataTypeImplementation(), this, other.asClass(TestRunReport.class));
  }

  /** Builder for TestRunReports. */
  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setName(String value);

    public abstract Builder setError(WhistleRuntimeException value);

    public abstract Builder setFailure(WhistleRuntimeException value);

    abstract TestRunReport autoBuild();

    public final TestRunReport build(RuntimeContext context) {
      TestRunReport testRunReport = autoBuild();
      testRunReport.addFieldBinding(
          TestRunReport.class, "name", TestRunReport::getName, DefaultPrimitive::new);
      if (testRunReport.getError().isPresent()) {
        testRunReport.addFieldBinding(
            TestRunReport.class,
            "error",
            TestRunReport::getError,
            ex ->
                ex.isPresent()
                    ? new DefaultErrorConverter().convert(context, ex.get())
                    : NullData.instance);
      }
      if (testRunReport.getFailure().isPresent()) {
        testRunReport.addFieldBinding(
            TestRunReport.class,
            "failure",
            TestRunReport::getFailure,
            ex ->
                ex.isPresent()
                    ? new DefaultErrorConverter().convert(context, ex.get())
                    : NullData.instance);
      }
      return testRunReport;
    }
  }
}
