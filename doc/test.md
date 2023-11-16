# Package test

[TOC]

## Import

The test package can be imported by adding this code to the top of your Whistle
file:

```
import "class://com.google.cloud.verticals.foundations.dataharmonization.plugins.test.TestPlugin"
```

## Functions
### assertEquals
`test::assertEquals(want: Data, got: Data)` returns `NullData`

#### Arguments

**want**: `Data`

**got**: `Data`

#### Description

Throws an exception describing the difference between the two given data, if
there is any. If they are the same returns null.

An example diff between `{"a": {"b": [1, 2, 3]}, "c": 1}` and `{"a": {"b": [1,
5, 6, 7]}, "c": "one"}` might look like:

```
-want, +got
 a.b[1]: -2 +5
 a.b[2]: -3 +6
 a.b[3]: -past end of array +7
 c: -1 +"one"
```

### assertNull
`test::assertNull(data: Data)` returns `NullData`

#### Arguments

**data**: `Data`

#### Description

Throws an exception if the given value is not null/empty. Returns null
otherwise.

### assertTrue
`test::assertTrue(bool: Boolean)` returns `NullData`

#### Arguments
**bool**: `Boolean`

#### Description
Throws an exception if the given value is not true. Returns null otherwise.

### execPaths

`test::execPaths(whistlePath: String, mockPaths: Array, input: Data)` returns
`Data`

#### Arguments

**whistlePath**: `String` - The path to the Whistle file to run. Can use any
supported loader.

**mockPaths**: `Array` - An array of paths to mock configs. See
[mocking.md](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/doc/mocking.md)
for more information about mocking.

**input**: `Data` - The input data. Will be bound to `$root` in the whistlePath
config.

#### Description

Executes the given Whistle code at the given path, with the given input and
returns the result. For example:

```
var result: test::execPaths("./someOtherFile.wstl",
                            ["./someOtherFile.mockconfig.wstl"],
                            { some: "thing" });
```

Note: The executed Whistle code will run under its own context. This means data,
plugins, and other state will NOT be shared with the current runtime state (one
exception is loaders, which will be passed on from the current config to ensure
relative paths can work). Some plugins may store state globally in the JVM (e.x.
as static variables); This will be shared (i.e. a new JVM is not instantiated).

#### Throws

*   **`IOException`** - If there is an error loading any of the files.

### reportAll

`test::reportAll()` returns `TestReport` - A report of all run tests. For an
example, see
[this JSON file](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/plugins/test/src/test/resources/tests/codelab_test_report.json).

#### Arguments

#### Description
Runs all Test Functions in the current package.

A Test Function is a function whose name starts with test_. It must take no
parameters.

### reportAll

`test::reportAll(packageName: String)` returns `TestReport` - A report of all
run tests. For an example, see
[this JSON file](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/plugins/test/src/test/resources/tests/codelab_test_report.json).

#### Arguments

**packageName**: `String` - The package name to scan for test functions. This
can be set to * to run all test functions in all currently loaded packages.

#### Description
Runs all Test Functions in the specified package.

A Test Function is a function whose name starts with test_. It must take no
parameters.

### run

`test::run(body: Closure)` returns `TestReport` - The report of all tests
(`runSingle`s) invoked within body.

#### Arguments

**body**: `Closure` - A closure containing `runSingle` calls.

#### Description

Creates a report where each individual test is specified by a call to runSingle
within body. Upon failure, this method throws an exception (failing the
execution) with a pretty printed report of the result.

This method is only useful in conjunction with #runSingle. For example:

```
test::run({
    var input: {
      dog: "woof"
      cat: "Hello I am cat"
    }
    test::runSingle("doggo test", {
      test::assertEquals("woof", input.dog)
    })
    test::runSingle("catto test", {
      test::assertEquals("meow", input.cat)
    })
  })
```

An example report might look like:

```
VerifyException: Summary: 1 failed, 1 passed
Failed:
  TEST catto test FAIL
  com.google.common.base.VerifyException: -want, +got
    -meow +Hello I am cat
    ....AssertFns.assertEquals(Native)
    at example.<lambda on line 11>(file:///...example.wstl:12)
    ....RunnerFns.runSingle(Native)
    at example.<lambda on line 3>(file:///...example.wstl:11)
    ....RunnerFns.run(Native)
    at example.example_root_function(file:///...example.wstl:3)
Passed:
  TEST doggo test PASS
```

#### Throws

*   **`VerifyException`** - if any test within body fails.
*   **`TestSetupException`** - if `run` calls are nested, or if a `runSingle` is
    incorrectly set up.

### runAll
`test::runAll()` returns `NullData`

#### Arguments

#### Description
Runs all Test Functions in the current package.

A Test Function is a function whose name starts with test_. It must take no
parameters.

Upon failure, this method throws an exception (failing the execution) with a
pretty printed report of the result. An example report might look like:

```
Summary: 2 failed, 1 passed
Failed:
  TEST test_my_erroring_test_func ERROR
  java.lang.UnsupportedOperationException: Attempted to key into non-container
  Array/DefaultArray with field woops
    at com.google.cloud.verticals.foundations.dataharmonization.builtins.Core.get(Native)
    at my_test.test_my_erroring_test_func(res:///tests/my_test.wstl:27)
    ....RunnerFns.runAll(Native)
    at my_test.my_test_root_function(res:///tests/my_test.wstl:4)

  TEST test_my_failing_test_func FAIL
  com.google.common.base.VerifyException: -want, +got
   .Moon[0].type -Moon +WOOPS
    ....AssertFns.assertEquals(Native)
    at my_test.test_my_failing_test_func(res:///tests/my_test.wstl:21)
    ....RunnerFns.runAll(Native)
    at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
Passed:
  TEST test_my_passing_test_func PASS
```

#### Throws

*   **`VerifyException`** - an error with a detailed human-readable report of
    the failed tests.

### runAll
`test::runAll(packageName: String)` returns `NullData`

#### Arguments

**packageName**: `String` - The package name to scan for test functions. This
can be set to * to run all test functions in all currently loaded packages.

#### Description
Runs all Test Functions in the specified package.

A Test Function is a function whose name starts with test_. It must take no
parameters.

Upon failure, this method throws an exception (failing the execution) with a
pretty printed report of the result. An example report might look like:

```
Summary: 2 failed, 1 passed
Failed:
  TEST test_my_erroring_test_func ERROR
  java.lang.UnsupportedOperationException: Attempted to key into non-container
  Array/DefaultArray with field woops
    at com.google.cloud.verticals.foundations.dataharmonization.builtins.Core.get(Native)
    at my_test.test_my_erroring_test_func(res:///tests/my_test.wstl:27)
    ....RunnerFns.runAll(Native)
    at my_test.my_test_root_function(res:///tests/my_test.wstl:4)

  TEST test_my_failing_test_func FAIL
  com.google.common.base.VerifyException: -want, +got
   .Moon[0].type -Moon +WOOPS
    ....AssertFns.assertEquals(Native)
    at my_test.test_my_failing_test_func(res:///tests/my_test.wstl:21)
    ....RunnerFns.runAll(Native)
    at my_test.my_test_root_function(res:///tests/my_test.wstl:4)
Passed:
  TEST test_my_passing_test_func PASS
```

#### Throws

*   **`VerifyException`** - an error with a detailed human-readable report of
    the failed tests.

### runSingle

`test::runSingle(name: String, body: Closure)` returns `Data` - The report for
this test execution (can be ignored in most cases, as it is registered
automatically with the enclosing `run` call's reports).

#### Arguments

**name**: `String` - The name of the test, referenced in the report.

**body**: `Closure` - A closure performing the test operations

#### Description

Executes body to create a test run entry of a single test with the given name.
body must invoke at least one assertion function, or the test will fail by
default with a TestSetupException indicating that no assertions were made. This
method can only be run inside the body of a #run call. The method returns a
single test run report (whether passing or failing), and registers it with the
enclosing run call's report.

This method is only useful in combination with `run`. For example:

```
test::run({
    var input: {
      dog: "woof"
      cat: "meow"
    }
    test::runSingle("doggo test", {
      test::assertEquals("woof", input.dog)
    })
    test::runSingle("catto test", {
      test::assertEquals("purr", input.cat)
    })
  })
```

#### Throws

*   **`TestSetupException`** - if no assertions are made, or this method is
    executed outside a `run` call.
