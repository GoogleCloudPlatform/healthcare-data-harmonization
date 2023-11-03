# Mocking in Whistle 2.0

[TOC]

## Overview

This document covers the mocking functionality in Whistle Runtime Engine.

Mocking allows users to override any function or target using user-defined
functions. Optionally, users can also define selectors to specify when
a certain override should happen. This behaviour is supplied to the Whistle 2.0
Engine as a whistle file separate from the main config during the engine
initialization.

For clarity, we define the following terms:

*   _original function_/ _original target_: the function/target being
    overridden;

*   _mock function_/ _mock target_: the function/target users defined to
    override the original function/target;

*   _selector_: an optional user defined function to specify when a certain
    override should happen;

## Mock a function

### Syntax

```JavaScript
mocking::mock(<original function reference>, <mock function reference>, [<selector reference>]);
```

Function references (including the selector reference) are specified as
`"<package name>::<function name>"`. Both components are required currently.

### Semantics

The mock function and the selector are expected to accept the same number of
arguments as the original function. All invocation of the original function will
be redirected to the mock function using the same arguments. When a selector
function is provided, the overriding will only happen when the selector
evaluates to true (or truth equivalent, see
[Ternary#isTruthy](https://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/runtime/src/main/java/com/google/cloud/verticals/foundations/dataharmonization/builtins/Ternary.java;rcl=368043432;l=48))
using the arguments that the original function receives.

### Errors

1.  When mock function has an incompatible number of arguments from the original
    function, the runtime will treat it as if no compatible mock exists and
    execute the original function without warning.

2.  When mock function has correct number of arguments but the selector doesn't,
    the runtime will throw error indicating that the selector function signature
    does not match given argument types.

3.  When multiple mocks can run with the given arguments, the runtime will throw
    an error indicating such case.

### Examples

1.  **Mocking with a whistle function**

    `main_config.wstl`

    ```java
    package main

    def foo() "foo"

    side foo: foo();

    def same(x) {
      x
    }

    output[]: same(1)
    output[]: same(2)
    output[]: same(3)
    ```

    `mock_config.wstl`

    ```java
    package mock_config

    def mockFoo() "bar"

    def notTheSame(x) x + 1

    def selectorFunc(x) x > 2

    mocking::mock("main::foo", "mock_config::mockFoo")
    mocking::mock("main::same", "mock_config::notTheSame", "mock_config::selectorFunc")
    ```

    `output.json`

    ```json
    {
      "foo": "bar",
      "output": [
        1,
        2,
        4
      ]
    }
    ```

2.  **Mocking with an imported function**

    Functions defined from other packages or plugins can also be used in the
    mock config. Suppose `MyPlugin` defines a function `myMockFunc`. It can be
    used in mock config like this:

    `mock_config.wstl`

    ```JavaScript
    import "class://path.to.MyPlugin"

    mocking::mock("originalPackageName::originalFunc", "myPluginName::myMockFunc");
    ```

    Using functions defined in other whistle files is done is a similar manner.

## Mock a target

### Syntax

```JavaScript
mocking::mockTarget(<original target reference>, <mock target reference>, [<selector reference>]);
```

Target references (including the selector reference) are specified as `"<package
name>::<target name>"`. Both components are required currently.

### Semantics

`mocking::mockTarget` is responsible for registering the mock target to the
Whistle runtime. A mock target can be defined using either a whistle function or
through a plugin. When a mock target is defined by a Whistle function, the last
argument of the function will take the data written to the target. Therefore,
the Whistle function is expected to accept one argument more than the original
target does. i.e.

```JavaScript
originalPackage::originalTarget(arg1, arg2, arg3): data;

// will become a function call as follows

somePackage::myMockTarget(arg1, arg2, arg3, data);
```

Note that the output of the Whistle function that defines the mock target will
be discarded unless it's marked as `side` output. See examples below for detail.

When a mock target is defined in a plugin, it only needs to accept the same
amount of argument as the original target following the
[`Plugin` implementation interface](link needed).

Selector is expected to accept the same number of argument as the original
target. When a selector is registered for a mock target. The mock target will
only be used when the selector returns `true` or
[truth equivalent](link-to-isTruthy).

### Errors

1.  When either selector or the mock target has incompatible signature with the
    original target, the runtime will throw error indicating the function
    (either selector or mock target) does match the given argument type.

2.  When multiple mocks can run with the current arguments, an error will be
    thrown indicating such case.

### Examples

**Mocking a target using Whistle function**

`main.wstl`

```java
package main

import "class://path.to.my.OriginalPlugin"

originalPlugin::originalTarget("foo", "irrelavent"): "fooVal";
originalPlugin::originalTarget("bar", "pinkElephant"): "barVal";
```

`mock_config.wstl`

```java
package mock_config

def mockTarget(arg1, arg2, valToWrite) {
  side output[]: valToWrite
}

def selector(arg1, arg2) arg1 == "foo"

mocking::mockTarget("originalPlugin::originalTarget", "mock_config::mockTarget", "mock_config::selector");
```

Value written to the `originalTarget` will only be `"barVal"` and the root
output of the `main.wstl` will contain the following field

```json
{
  "output": ["fooVal"]
}
```

## Common errors & troubleshooting

1.  When the mock config shares imports with the main config, it will result in
    function duplication error. In this case, removing the import from the mock
    config will solve the issue.

## Caveat

Mocking is a extremely versitile as it allows users to override beaviour for any
function, including builtins. But this can also become a powerful way to exploit
existing config. Therefore, great caution must be taken when exposing the mock
option to the end user.
