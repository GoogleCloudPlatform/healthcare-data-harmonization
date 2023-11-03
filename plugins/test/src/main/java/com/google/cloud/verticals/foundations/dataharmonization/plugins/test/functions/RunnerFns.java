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

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.joining;

import com.google.cloud.verticals.foundations.dataharmonization.data.Array;
import com.google.cloud.verticals.foundations.dataharmonization.data.Data;
import com.google.cloud.verticals.foundations.dataharmonization.data.NullData;
import com.google.cloud.verticals.foundations.dataharmonization.data.Preconditions;
import com.google.cloud.verticals.foundations.dataharmonization.exceptions.WhistleRuntimeException;
import com.google.cloud.verticals.foundations.dataharmonization.function.CallableFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.Closure;
import com.google.cloud.verticals.foundations.dataharmonization.function.DefaultClosure.FunctionReference;
import com.google.cloud.verticals.foundations.dataharmonization.function.context.RuntimeContext;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.JavaFunction;
import com.google.cloud.verticals.foundations.dataharmonization.function.java.PluginFunction;
import com.google.cloud.verticals.foundations.dataharmonization.imports.ImportPath;
import com.google.cloud.verticals.foundations.dataharmonization.init.Engine;
import com.google.cloud.verticals.foundations.dataharmonization.init.initializer.ExternalConfigExtractor;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestPlugin;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestSetupException;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model.TestReport;
import com.google.cloud.verticals.foundations.dataharmonization.plugins.test.data.model.TestRunReport;
import com.google.cloud.verticals.foundations.dataharmonization.registry.PackageRegistry;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;

/** Functions to run the tests. */
public final class RunnerFns {
  private static final String ASSERTION_MADE_KEY = RunnerFns.class.getName() + ".assertionMade";
  private static final String TEST_NAME_KEY = RunnerFns.class.getName() + ".testName";
  static final String DYNAMIC_RUNS_META_KEY = RunnerFns.class.getName() + ".dynamicRuns";
  static final String RUN_SINGLE_NESTED_META_KEY = RunnerFns.class.getName() + ".inRunSingle";

  private RunnerFns() {}

  /** Sets that an assertion (at least one) has been made in the current test. */
  public static void setAssertionMade(RuntimeContext context) {
    context.getMetaData().setMeta(ASSERTION_MADE_KEY, true);
  }

  /**
   * Runs all Test Functions in the current package.
   *
   * <p>A Test Function is a function whose name starts with test_. It must take no parameters.
   *
   * <p>Upon failure, this method throws an exception (failing the execution) with a pretty printed
   * report of the result. An example report might look like:
   *
   * <pre><code>
   * Summary: 2 failed, 1 passed
   * Failed:
   *   TEST test_my_erroring_test_func ERROR
   *   java.lang.UnsupportedOperationException: Attempted to key into non-container
   *   Array/DefaultArray with field woops
   *     at com.google.cloud.verticals.foundations.dataharmonization.builtins.Core.get(Native)
   *     at my_test.test_my_erroring_test_func(res:///tests/my_test.wstl:27)
   *     ....RunnerFns.runAll(Native)
   *     at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
   *
   *   TEST test_my_failing_test_func FAIL
   *   com.google.common.base.VerifyException: -want, +got
   *    .Moon[0].type -Moon +WOOPS
   *     ....AssertFns.assertEquals(Native)
   *     at my_test.test_my_failing_test_func(res:///tests/my_test.wstl:21)
   *     ....RunnerFns.runAll(Native)
   *     at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
   * Passed:
   *   TEST test_my_passing_test_func PASS
   * </code></pre>
   *
   * @param context RuntimeContext to run on.
   * @throws VerifyException an error with a detailed human-readable report of the failed tests.
   */
  @PluginFunction
  public static NullData runAll(RuntimeContext context) {
    TestReport report = reportAll(context);
    if (report.getNumFailed() > 0) {
      throw new VerifyException(report.prettyPrint());
    }

    return NullData.instance;
  }

  /**
   * Runs all Test Functions in the specified package.
   *
   * <p>A Test Function is a function whose name starts with test_. It must take no parameters.
   *
   * <p>Upon failure, this method throws an exception (failing the execution) with a pretty printed
   * report of the result. An example report might look like:
   *
   * <pre><code>
   * Summary: 2 failed, 1 passed
   * Failed:
   *   TEST test_my_erroring_test_func ERROR
   *   java.lang.UnsupportedOperationException: Attempted to key into non-container
   *   Array/DefaultArray with field woops
   *     at com.google.cloud.verticals.foundations.dataharmonization.builtins.Core.get(Native)
   *     at my_test.test_my_erroring_test_func(res:///tests/my_test.wstl:27)
   *     ....RunnerFns.runAll(Native)
   *     at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
   *
   *   TEST test_my_failing_test_func FAIL
   *   com.google.common.base.VerifyException: -want, +got
   *    .Moon[0].type -Moon +WOOPS
   *     ....AssertFns.assertEquals(Native)
   *     at my_test.test_my_failing_test_func(res:///tests/my_test.wstl:21)
   *     ....RunnerFns.runAll(Native)
   *     at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
   * Passed:
   *   TEST test_my_passing_test_func PASS
   * </code></pre>
   *
   * @param context RuntimeContext to run on.
   * @param packageName The package name to scan for test functions. This can be set to * to run all
   *     test functions in all currently loaded packages.
   * @throws VerifyException an error with a detailed human-readable report of the failed tests.
   */
  @PluginFunction
  public static NullData runAll(RuntimeContext context, String packageName) {
    TestReport report = reportAll(context, packageName);
    if (report.getNumFailed() > 0) {
      throw new VerifyException(report.prettyPrint());
    }

    return NullData.instance;
  }

  /**
   * Runs all Test Functions in the current package.
   *
   * <p>A Test Function is a function whose name starts with test_. It must take no parameters.
   *
   * @param context RuntimeContext to run on.
   * @return A report of all run tests. For an example, see [this JSON
   *     file](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/plugins/test/src/test/resources/tests/codelab_test_report.json).
   */
  @PluginFunction
  public static TestReport reportAll(RuntimeContext context) {
    String currentPackage = context.getCurrentPackageContext().getCurrentPackage();
    return reportAll(context, currentPackage);
  }

  /**
   * Runs all Test Functions in the specified package.
   *
   * <p>A Test Function is a function whose name starts with test_. It must take no parameters.
   *
   * @param context RuntimeContext to run on.
   * @param packageName The package name to scan for test functions. This can be set to * to run all
   *     test functions in all currently loaded packages.
   * @return A report of all run tests. For an example, see [this JSON
   *     file](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/plugins/test/src/test/resources/tests/codelab_test_report.json).
   */
  @PluginFunction
  public static TestReport reportAll(RuntimeContext context, String packageName) {
    PackageRegistry<CallableFunction> registry =
        context.getRegistries().getFunctionRegistry(packageName);
    Set<CallableFunction> testFunctions;
    if (FunctionReference.WILDCARD_PACKAGE_NAME.equals(packageName)) {
      testFunctions = registry.getAll();
    } else {
      testFunctions = registry.getAllInPackage(packageName);
    }

    if (testFunctions.isEmpty()) {
      throw new TestSetupException(
          String.format(
              "Could not find any functions at all in package %s. Are you missing an import?",
              packageName));
    }

    testFunctions =
        testFunctions.stream()
            .filter(fn -> fn.getName().startsWith("test_"))
            .collect(toImmutableSet());

    if (testFunctions.isEmpty()) {
      throw new TestSetupException(
          String.format(
              "Could not find test functions in package %s. Test function names must"
                  + " start with test_ and take no parameters.",
              packageName));
    }

    ImmutableList<TestRunReport> tests =
        testFunctions.stream()
            .sorted(Comparator.comparing(CallableFunction::getName))
            .map(c -> runTest(context, c))
            .collect(toImmutableList());

    TestReport.Builder report = TestReport.builder();
    report.setNumRun(tests.size());
    report.setNumFailed(
        (int)
            tests.stream()
                .filter(trr -> trr.getError().isPresent() || trr.getFailure().isPresent())
                .count());
    report.testsBuilder().addAll(tests);
    return report.build(context.getDataTypeImplementation());
  }

  /**
   * Executes the given Whistle code at the given path, with the given input and returns the result.
   * For example:
   *
   * <pre><code>
   * var result: test::execPaths("./someOtherFile.wstl",
   *                             ["./someOtherFile.mockconfig.wstl"],
   *                             { some: "thing" });
   * </code></pre>
   *
   * Note: The executed Whistle code will run under its own context. This means data, plugins, and
   * other state will NOT be shared with the current runtime state (one exception is loaders, which
   * will be passed on from the current config to ensure relative paths can work). Some plugins may
   * store state globally in the JVM (e.x. as static variables); This will be shared (i.e. a new JVM
   * is not instantiated).
   *
   * @param context the RuntimeContext to run under. The executed Whistle code will run under its
   *     own context.
   * @param whistlePath The path to the Whistle file to run. Can use any supported loader.
   * @param mockPaths An array of paths to mock configs. See
   *     [mocking.md](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/doc/mocking.md)
   *     for more information about mocking.
   * @param input The input data. Will be bound to <code>$root</code> in the whistlePath config.
   * @throws IOException If there is an error loading any of the files.
   */
  @PluginFunction
  public static Data execPaths(
      RuntimeContext context, String whistlePath, Array mockPaths, Data input) throws IOException {
    ImportPath whistle = relativeToCurrent(context, whistlePath);

    Engine.Builder builder =
        new Engine.Builder(ExternalConfigExtractor.of(whistle))
            .withDefaultLoaders(context.getRegistries().getLoaderRegistry().getAll());

    mockPaths.stream()
        .forEach(
            mp ->
                builder.addMock(
                    ExternalConfigExtractor.of(
                        relativeToCurrent(
                            context, Preconditions.requireNonEmptyString(mp, "mockPath")))));

    Engine engine = builder.initialize().build();
    return engine.transform(input);
  }

  /**
   * Creates a report where each individual test is specified by a call to runSingle within body.
   * Upon failure, this method throws an exception (failing the execution) with a pretty printed
   * report of the result.
   *
   * <p>This method is only useful in conjunction with {@link #runSingle}. For example:
   *
   * <pre><code>
   *   test::run({
   *     var input: {
   *       dog: "woof"
   *       cat: "Hello I am cat"
   *     }
   *     test::runSingle("doggo test", {
   *       test::assertEquals("woof", input.dog)
   *     })
   *     test::runSingle("catto test", {
   *       test::assertEquals("meow", input.cat)
   *     })
   *   })
   * </code></pre>
   *
   * <p>An example report might look like:
   *
   * <pre><code>
   * VerifyException: Summary: 1 failed, 1 passed
   * Failed:
   *   TEST catto test FAIL
   *   com.google.common.base.VerifyException: -want, +got
   *     -meow +Hello I am cat
   *     ....AssertFns.assertEquals(Native)
   *     at example.&lt;lambda on line 11>(file:///...example.wstl:12)
   *     ....RunnerFns.runSingle(Native)
   *     at example.&lt;lambda on line 3>(file:///...example.wstl:11)
   *     ....RunnerFns.run(Native)
   *     at example.example_root_function(file:///...example.wstl:3)
   * Passed:
   *   TEST doggo test PASS
   * </code></pre>
   *
   * @param body A closure containing {@code runSingle} calls.
   * @return The report of all tests ({@code runSingle}s) invoked within body.
   * @throws VerifyException if any test within body fails.
   * @throws TestSetupException if {@code run} calls are nested, or if a {@code runSingle} is
   *     incorrectly set up.
   */
  @PluginFunction
  public static TestReport run(RuntimeContext context, Closure body) {
    if (context.getMetaData().getMeta(DYNAMIC_RUNS_META_KEY) != null) {
      throw new TestSetupException("run cannot be called within another run call.");
    }
    context.getMetaData().setMeta(DYNAMIC_RUNS_META_KEY, new ArrayList<TestRunReport>());
    body.execute(context);

    ArrayList<TestRunReport> tests = context.getMetaData().getMeta(DYNAMIC_RUNS_META_KEY);
    TestReport.Builder reportBuilder = TestReport.builder();
    reportBuilder.setNumRun(tests.size());
    reportBuilder.setNumFailed(
        (int)
            tests.stream()
                .filter(trr -> trr.getError().isPresent() || trr.getFailure().isPresent())
                .count());
    reportBuilder.testsBuilder().addAll(tests);
    TestReport report = reportBuilder.build(context.getDataTypeImplementation());

    context.getMetaData().setMeta(DYNAMIC_RUNS_META_KEY, null);

    if (report.getNumFailed() > 0) {
      throw new VerifyException(report.prettyPrint());
    }

    if (report.getNumRun() == 0) {
      throw new TestSetupException("At least one runSingle call must be made within a run call.");
    }

    return report;
  }

  /**
   * Executes body to create a test run entry of a single test with the given name. body must invoke
   * at least one assertion function, or the test will fail by default with a TestSetupException
   * indicating that no assertions were made. This method can only be run inside the body of a
   * {@link #run} call. The method returns a single test run report (whether passing or failing),
   * and registers it with the enclosing run call's report.
   *
   * <p>This method is only useful in combination with {@code run}. For example:
   *
   * <pre><code>
   *   test::run({
   *     var input: {
   *       dog: "woof"
   *       cat: "meow"
   *     }
   *     test::runSingle("doggo test", {
   *       test::assertEquals("woof", input.dog)
   *     })
   *     test::runSingle("catto test", {
   *       test::assertEquals("purr", input.cat)
   *     })
   *   })
   * </code></pre>
   *
   * @param name The name of the test, referenced in the report.
   * @param body A closure performing the test operations
   * @return The report for this test execution (can be ignored in most cases, as it is registered
   *     automatically with the enclosing {@code run} call's reports).
   * @throws TestSetupException if no assertions are made, or this method is executed outside a
   *     {@code run} call.
   */
  @PluginFunction
  public static Data runSingle(RuntimeContext context, String name, Closure body) {
    ArrayList<TestRunReport> tests = context.getMetaData().getMeta(DYNAMIC_RUNS_META_KEY);
    if (tests == null) {
      throw new TestSetupException("runSingle must be called within a run call.");
    }
    if (context.getMetaData().getFlag(RUN_SINGLE_NESTED_META_KEY)) {
      throw new TestSetupException("runSingle cannot be called within another runSingle call.");
    }
    context.getMetaData().setSerializableMeta(RUN_SINGLE_NESTED_META_KEY, true);

    TestRunReport report = runTest(context, name, body);
    tests.add(report);
    context.getMetaData().setSerializableMeta(RUN_SINGLE_NESTED_META_KEY, null);

    return report;
  }

  private static ImportPath relativeToCurrent(RuntimeContext context, String path) {
    ImportPath current = context.getCurrentPackageContext().getCurrentImportPath();
    ImportPath next = ImportPath.resolve(current, path);
    return ImportPath.of(next.getLoader(), next.getAbsPath(), next.getAbsPath().getParent());
  }

  private static TestRunReport runTest(RuntimeContext context, CallableFunction function) {
    // TODO(rpolyano): Support parameterized tests
    if (!function.getSignature().getArgs().isEmpty()) {
      throw new TestSetupException(
          String.format("Test function %s cannot have parameters.", function.getSignature()));
    }

    return runTest(context, function.getName(), new CallableFunctionClosure(function, new Data[0]));
  }

  private static TestRunReport runTest(RuntimeContext context, String name, Closure function) {
    TestRunReport.Builder builder = TestRunReport.builder();
    builder = builder.setName(name);
    try {
      context.getMetaData().setMeta(TEST_NAME_KEY, function.getName());
      context.getMetaData().setMeta(ASSERTION_MADE_KEY, false);

      function.execute(context);

      Boolean assertionMade = context.getMetaData().getMeta(ASSERTION_MADE_KEY);
      if (assertionMade == null || !assertionMade) {
        throw new TestSetupException(
            String.format(
                "Test function %s did not make any assertions. Make sure you call one of the"
                    + " assertion functions at least once. Assertion functions are: %s.",
                name,
                JavaFunction.ofPluginFunctionsInClass(AssertFns.class, TestPlugin.PACKAGE_NAME)
                    .stream()
                    .map(CallableFunction::getName)
                    .map(n -> TestPlugin.PACKAGE_NAME + "::" + n)
                    .sorted()
                    .collect(joining(", "))));
      }
    } catch (WhistleRuntimeException ex) {
      if (ex.getCause() instanceof VerifyException) {
        builder.setFailure(ex);
      } else {
        builder.setError(ex);
      }
    } catch (VerifyException ex) {
      builder.setFailure(WhistleRuntimeException.fromCurrentContext(context, ex));
    }

    return builder.build(context);
  }
}
