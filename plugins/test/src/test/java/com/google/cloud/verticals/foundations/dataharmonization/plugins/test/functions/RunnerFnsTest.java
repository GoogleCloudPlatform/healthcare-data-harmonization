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
package com.google.cloud.verticals.foundations.dataharmonization.plugins.test.functions;

import static com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil.testContext;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockClosure;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestSetupException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model.TestReport;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model.TestRunReport;
import com.google.common.base.VerifyException;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Tests for runner fns not covered by integration tests. */
@RunWith(JUnit4.class)
public class RunnerFnsTest {

  @Test
  public void runSingle_notInRun_throws() {
    TestSetupException ex =
        assertThrows(
            TestSetupException.class,
            () -> RunnerFns.runSingle(testContext(), "hello", MockClosure.noop()));
    assertThat(ex).hasMessageThat().contains("runSingle must be called within a run call");
  }

  @Test
  public void runSingle_nested_throws() {
    RuntimeContext context = testContext();
    context.getMetaData().setMeta(RunnerFns.DYNAMIC_RUNS_META_KEY, new ArrayList<TestRunReport>());

    TestSetupException ex =
        assertThrows(
            TestSetupException.class,
            () ->
                RunnerFns.runSingle(
                    context,
                    "hello",
                    new MockClosure(
                        0, (args, ctx) -> RunnerFns.runSingle(ctx, "world", MockClosure.noop()))));
    assertThat(ex)
        .hasMessageThat()
        .contains("runSingle cannot be called within another runSingle call.");
  }

  @Test
  public void runSingle_noAssertions_throws() {
    RuntimeContext context = testContext();
    context.getMetaData().setMeta(RunnerFns.DYNAMIC_RUNS_META_KEY, new ArrayList<TestRunReport>());

    TestSetupException ex =
        assertThrows(
            TestSetupException.class,
            () -> RunnerFns.runSingle(context, "hello", MockClosure.noop()));
    assertThat(ex).hasMessageThat().contains("hello did not make any assertions");
  }

  @Test
  public void runSingle_savesPassingReport() {
    RuntimeContext context = testContext();
    ArrayList<TestRunReport> reports = new ArrayList<>();
    context.getMetaData().setMeta(RunnerFns.DYNAMIC_RUNS_META_KEY, reports);

    RunnerFns.runSingle(
        context, "hello", new MockClosure(0, (data, ctx) -> AssertFns.assertTrue(ctx, true)));
    assertThat(reports).hasSize(1);
    assertThat(reports.get(0).isFailed()).isFalse();
  }

  @Test
  public void runSingle_savesFailingReport() {
    RuntimeContext context = testContext();
    ArrayList<TestRunReport> reports = new ArrayList<>();
    context.getMetaData().setMeta(RunnerFns.DYNAMIC_RUNS_META_KEY, reports);

    RunnerFns.runSingle(
        context, "hello", new MockClosure(0, (data, ctx) -> AssertFns.assertTrue(ctx, false)));
    assertThat(reports).hasSize(1);
    assertThat(reports.get(0).isFailed()).isTrue();
  }

  @Test
  public void run_nested_throws() {
    TestSetupException ex =
        assertThrows(
            TestSetupException.class,
            () ->
                RunnerFns.run(
                    testContext(),
                    new MockClosure(0, (data, ctx) -> RunnerFns.run(ctx, MockClosure.noop()))));
    assertThat(ex).hasMessageThat().contains("run cannot be called within another run");
  }

  @Test
  public void run_noTests_throws() {
    TestSetupException ex =
        assertThrows(
            TestSetupException.class, () -> RunnerFns.run(testContext(), MockClosure.noop()));
    assertThat(ex)
        .hasMessageThat()
        .contains("At least one runSingle call must be made within a run call");
  }

  @Test
  public void run_passingTests_returnsReport() {
    TestReport actual =
        RunnerFns.run(
            testContext(),
            new MockClosure(
                0,
                (args, ctx) -> {
                  RunnerFns.runSingle(
                      ctx,
                      "I pass",
                      new MockClosure(0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                  RunnerFns.runSingle(
                      ctx,
                      "I pass 2",
                      new MockClosure(0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                  RunnerFns.runSingle(
                      ctx,
                      "I pass 3",
                      new MockClosure(0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                  return NullData.instance;
                }));
    assertThat(actual.getNumRun()).isEqualTo(3);
    assertThat(actual.getNumPassed()).isEqualTo(3);

    assertThat(actual.getTests().get(0).isFailed()).isFalse();
    assertThat(actual.getTests().get(0).getName()).isEqualTo("I pass");
    assertThat(actual.getTests().get(1).isFailed()).isFalse();
    assertThat(actual.getTests().get(1).getName()).isEqualTo("I pass 2");
    assertThat(actual.getTests().get(2).isFailed()).isFalse();
    assertThat(actual.getTests().get(2).getName()).isEqualTo("I pass 3");
  }

  @Test
  public void run_multiple_produceIndependentReports() {
    RuntimeContext context = testContext();
    TestReport one =
        RunnerFns.run(
            context,
            new MockClosure(
                0,
                (args, ctx) -> {
                  RunnerFns.runSingle(
                      ctx,
                      "I pass",
                      new MockClosure(0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                  return NullData.instance;
                }));
    TestReport two =
        RunnerFns.run(
            context,
            new MockClosure(
                0,
                (args, ctx) -> {
                  RunnerFns.runSingle(
                      ctx,
                      "I pass 2",
                      new MockClosure(0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                  return NullData.instance;
                }));
    assertThat(one.getNumRun()).isEqualTo(1);
    assertThat(one.getNumPassed()).isEqualTo(1);

    assertThat(one.getTests().get(0).isFailed()).isFalse();
    assertThat(one.getTests().get(0).getName()).isEqualTo("I pass");

    assertThat(two.getNumRun()).isEqualTo(1);
    assertThat(two.getNumPassed()).isEqualTo(1);

    assertThat(two.getTests().get(0).isFailed()).isFalse();
    assertThat(two.getTests().get(0).getName()).isEqualTo("I pass 2");
  }

  @Test
  public void run_mixedTests_returnsReport() {
    VerifyException ex =
        assertThrows(
            VerifyException.class,
            () ->
                RunnerFns.run(
                    testContext(),
                    new MockClosure(
                        0,
                        (args, ctx) -> {
                          RunnerFns.runSingle(
                              ctx,
                              "I pass",
                              new MockClosure(
                                  0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, true)));
                          RunnerFns.runSingle(
                              ctx,
                              "I fail",
                              new MockClosure(
                                  0, (args2, ctx2) -> AssertFns.assertTrue(ctx2, false)));

                          RunnerFns.runSingle(
                              ctx,
                              "I error",
                              new MockClosure(
                                  0,
                                  (args2, ctx2) -> {
                                    throw WhistleRuntimeException.fromCurrentContext(
                                        ctx2, new IllegalStateException("oops"));
                                  }));
                          return NullData.instance;
                        })));
    assertThat(ex).hasMessageThat().contains("2 failed, 1 passed");
    assertThat(ex).hasMessageThat().contains("TEST I error ERROR");
    assertThat(ex).hasMessageThat().contains("TEST I pass PASS");
    assertThat(ex).hasMessageThat().contains("TEST I fail FAIL");
  }
}
