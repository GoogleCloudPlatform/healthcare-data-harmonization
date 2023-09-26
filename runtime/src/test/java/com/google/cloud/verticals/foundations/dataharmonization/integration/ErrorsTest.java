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

package com.google.cloud.verticals.foundations.dataharmonization.integration;

import static com.google.cloud.verticals.foundations.dataharmonization.utils.AssertUtil.assertDCAPEquals;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.fail;

import com.google.cloud.verticals.foundations.dataharmonization.builtins.error.Errors;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.NoMatchingOverloadsException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleStackOverflowError;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.WrapperContextTest;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.integration.plugin.TestLoaderPlugin.ResourceLoader;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockClosure;
import com.google.cloud.verticals.foundations.dataharmonization.mock.MockData;
import com.google.cloud.verticals.foundations.dataharmonization.utils.RuntimeContextUtil;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/** Test for checking the stack trace and exception functionality. */
@RunWith(JUnit4.class)
public class ErrorsTest {
  private static final String SUBDIR = "error/";
  private static final IntegrationTest TESTER = new IntegrationTest(SUBDIR);
  private static final String TEST_FOLDER_URI =
      String.format("%s://%s%s", ResourceLoader.TEST_LOADER, IntegrationTest.TESTS_DIR, SUBDIR);

  private static StackTraceElement at(String pkg, String function, String file, Integer line) {
    return new StackTraceElement(pkg, function, file, line != null ? line : -1);
  }

  @Test
  public void simpleError_containsWhistleStack() {
    try {
      Engine engine = TESTER.initializeTestFile("errors_simple.wstl");
      engine.transform(NullData.instance);
      fail();
    } catch (Exception e) {
      assertEquals(WhistleRuntimeException.class, e.getClass());
      assertThat(e).hasMessageThat().contains("myfield");

      String file = TEST_FOLDER_URI + "errors_simple.wstl";
      StackTraceElement[] expected =
          new StackTraceElement[] {
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.Core",
                "get",
                "Native",
                null),
            at("errors", "three", file, 17),
            at("errors", "two", file, 11),
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.Iteration",
                "iterate",
                "Native",
                null),
            at("errors", "one", file, 7),
            at("errors", "errors_root_function", file, 3),
          };

      assertArrayEquals(expected, e.getStackTrace());
    }
  }

  @Test
  public void ternaryError_containsOnlyRelevantWhistleStack() {
    try {
      Engine engine = TESTER.initializeTestFile("errors_ternary.wstl");
      engine.transform(NullData.instance);
      fail();
    } catch (Exception e) {
      assertEquals(WhistleRuntimeException.class, e.getClass());
      assertThat(e).hasMessageThat().contains("myfield");

      String file = TEST_FOLDER_URI + "errors_ternary.wstl";
      StackTraceElement[] expected =
          new StackTraceElement[] {
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.Core",
                "get",
                "Native",
                null),
            at("errors", "three", file, 12),
            at("errors", "two", file, 6),
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.Iteration",
                "iterate",
                "Native",
                null),
            at("errors", "one", file, 19),
            at("errors", "errors_root_function", file, 3),
          };

      assertArrayEquals(expected, e.getStackTrace());
    }
  }

  @Test
  public void lambdaError_containsOnlyRelevantWhistleStack() {
    try {
      Engine engine = TESTER.initializeTestFile("errors_lambda.wstl");
      engine.transform(NullData.instance);
      fail();
    } catch (Exception e) {
      assertEquals(WhistleRuntimeException.class, e.getClass());
      assertThat(e).hasMessageThat().contains("myfield");

      String file = TEST_FOLDER_URI + "errors_lambda.wstl";
      StackTraceElement[] expected =
          new StackTraceElement[] {
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.Core",
                "get",
                "Native",
                null),
            at("errors", "three", file, 12),
            at("errors", "<lambda on line 19>", file, 19),
            at(
                "com.google.cloud.verticals.foundations.dataharmonization.builtins.ArrayFns",
                "where",
                "Native",
                null),
            at("errors", "one", file, 19),
            at("errors", "errors_root_function", file, 3),
          };

      assertArrayEquals(expected, e.getStackTrace());
    }
  }

  @Test
  public void stackOverflowError_singleFunc_containsOnlyRelevantWhistleStack() throws Exception {
    final Engine engine = TESTER.initializeTestFile("stack_overflow_1.wstl");
    WhistleStackOverflowError error =
        assertThrows(WhistleStackOverflowError.class, () -> engine.transform(NullData.instance));
    assertThat(error).hasMessageThat().contains("Number of stack frames exceed the max limit");
    assertThat(error).hasMessageThat().contains("<The top of the stack>");
    assertThat(error).hasMessageThat().contains("<Statistics of the stack>");
    assertThat(error).hasMessageThat().contains("stack_overflow_root_function: ");
    assertThat(error).hasMessageThat().contains("f: ");
  }

  @Test
  public void stackOverflowError_multipleFunc_containsOnlyRelevantWhistleStack() throws Exception {
    final Engine engine = TESTER.initializeTestFile("stack_overflow_2.wstl");
    WhistleStackOverflowError error =
        assertThrows(WhistleStackOverflowError.class, () -> engine.transform(NullData.instance));
    assertThat(error).hasMessageThat().contains("Number of stack frames exceed the max limit");
    assertThat(error).hasMessageThat().contains("<The top of the stack>");
    assertThat(error).hasMessageThat().contains("<Statistics of the stack>");
    assertThat(error).hasMessageThat().contains("stack_overflow_root_function: ");
    assertThat(error).hasMessageThat().contains("loop: ");
    assertThat(error).hasMessageThat().contains("a: ");
    assertThat(error).hasMessageThat().contains("b: ");
    assertThat(error).hasMessageThat().contains("c: ");
    assertThat(error).hasMessageThat().contains("d: ");
    assertThat(error).hasMessageThat().matches(Pattern.compile(".*a:.*loop.*", Pattern.DOTALL));
  }

  @Test
  public void functionCallError_containsSuggestedFunctionName() throws Exception {
    final Engine engine = TESTER.initializeTestFile("errors_unknown_func.wstl");
    NoMatchingOverloadsException error =
        assertThrows(NoMatchingOverloadsException.class, () -> engine.transform(NullData.instance));
    assertThat(error).hasMessageThat().contains("Unknown function ones()");
    assertThat(error).hasMessageThat().contains("did you mean: ");
    assertThat(error).hasMessageThat().contains("errors::one()");
    assertThat(error).hasMessageThat().contains("builtins::neq()");
  }

  @Test
  public void functionCallError_containsSuggestedPackageName() throws Exception {
    final Engine engine = TESTER.initializeTestFile("errors_unknown_package.wstl");
    NoMatchingOverloadsException error =
        assertThrows(NoMatchingOverloadsException.class, () -> engine.transform(NullData.instance));
    assertThat(error).hasMessageThat().contains("Unknown function buildin::hash");
    assertThat(error).hasMessageThat().contains("did you mean: ");
    assertThat(error).hasMessageThat().contains("builtins::hash()");
  }

  @Test
  public void handlingIteration_containsCorrectOutput() throws Exception {
    Engine engine = TESTER.initializeTestFile("handling_iteration.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("handling_iteration.json");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void handlingNested_containsCorrectOutput() throws Exception {
    Engine engine = TESTER.initializeTestFile("handling_nested.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("handling_nested.json");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void handlingNonWhistleRuntime_containsCorrectOutput() throws Exception {
    Engine engine = TESTER.initializeTestFile("handling_nonrt.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("handling_nonrt.json");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void handlingRethrowError_containsCorrectOutput() throws Exception {
    Engine engine = TESTER.initializeTestFile("handling_rethrowError.wstl");
    Data actual = engine.transform(NullData.instance);
    Data expected = TESTER.loadJson("handling_rethrowError.json");
    assertDCAPEquals(expected, actual);
  }

  @Test
  public void wrappedRuntimeContext_callsHandler() {
    AtomicBoolean sentry = new AtomicBoolean();
    Data token = MockData.arbitrary();
    RuntimeContext ctx = RuntimeContextUtil.testContext();
    Data unused =
        Errors.withError(
            ctx,
            new MockClosure(
                0,
                (args, rtx) -> {
                  assertThat(rtx).isInstanceOf(WrapperContext.class);
                  assertThat(rtx).isNotSameInstanceAs(ctx);

                  rtx = WrapperContextTest.wrap("Shawarma", rtx);
                  Data handleResult = Errors.rethrowOrHandle(rtx, new RuntimeException("WOOPS"));

                  assertThat(handleResult).isSameInstanceAs(token);

                  sentry.set(true);
                  return NullData.instance;
                }),
            new MockClosure(0, (args, rtx) -> token));

    assertThat(sentry.get()).isTrue();
  }

  @Test
  public void rethrowError_callsHandlerAndRethrows() {
    AtomicBoolean bodyContinuationSentry = new AtomicBoolean();
    AtomicBoolean errorHandledSentry = new AtomicBoolean();
    RuntimeContext ctx = RuntimeContextUtil.testContext();
    WhistleRuntimeException rethrown =
        assertThrows(
            WhistleRuntimeException.class,
            () -> {
              Data unusedResult =
                  Errors.rethrowError(
                      ctx,
                      new MockClosure(
                          0,
                          (args, rtx) -> {
                            Data unusedInnerResult =
                                Errors.rethrowOrHandle(rtx, new RuntimeException("WOOPS"));

                            // Should not continue in closure
                            bodyContinuationSentry.set(true);
                            return NullData.instance;
                          }),
                      new MockClosure(
                          0,
                          (args, rtx) -> {
                            errorHandledSentry.set(true);
                            return NullData.instance;
                          }));
            });

    assertThat(bodyContinuationSentry.get()).isFalse();
    assertThat(errorHandledSentry.get()).isTrue();
    assertThat(rethrown).hasMessageThat().contains("WOOPS");
  }

  @Test
  public void withError_rethrowError_handlesAtBottomContinuesAtTop() {
    AtomicBoolean innerErrorHandledSentry = new AtomicBoolean();
    AtomicBoolean outerBodyContinuationSentry = new AtomicBoolean();
    AtomicBoolean outerErrorHandlingSentry = new AtomicBoolean();
    RuntimeContext ctx = RuntimeContextUtil.testContext();

    Data unused =
        Errors.withError(
            ctx,
            new MockClosure(
                0,
                (args, rtx) -> {
                  Data unusedResult =
                      Errors.rethrowError(
                          rtx,
                          new MockClosure(
                              0,
                              (innerArgs, innerRtx) -> {
                                Data innerResult =
                                    Errors.rethrowOrHandle(innerRtx, new RuntimeException("WOOPS"));

                                return NullData.instance;
                              }),
                          new MockClosure(
                              0,
                              (innerArgs, innerRtx) -> {
                                innerErrorHandledSentry.set(true);
                                return NullData.instance;
                              }));
                  outerBodyContinuationSentry.set(true);
                  return NullData.instance;
                }),
            new MockClosure(
                0,
                (args, rtx) -> {
                  outerErrorHandlingSentry.set(true);
                  return NullData.instance;
                }));

    assertThat(innerErrorHandledSentry.get()).isTrue();
    assertThat(outerBodyContinuationSentry.get()).isTrue();
    assertThat(outerErrorHandlingSentry.get()).isFalse();
  }
}
