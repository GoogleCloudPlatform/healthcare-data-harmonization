# Whistle 2 Test Plugin

This plugin provides methods to unit test Whistle functions.

## Getting started

This plugin provides a few specific approaches on how tests can be set up. There
are 3 approaches:

1.  Testing via one function per test. This setup is best for testing many
    unique functions with a few test for each.

1.  Testing via parameterized tests. This test is best for testing a few or one
    unique function with a large variety of inline-constructed inputs.

1.  Testing via input/output file pairs. This setup is best for testing a full
    end-to-end mapping package with a large variety of complex input/output
    pairs.

## Testing via functions

This is the simplest way to set up tests. In this setup, every function in the
test file constitutes a test.

Test functions must start with the prefix "test_" and contain no parameters.

```
package dog_test

import "test"
import "./dog.wstl"

// This function actually runs the test functions in this package. It returns
// nothing if the tests pass, or errors if one or more fail. See also reportAll.
test::runAll()

def test_give_treat() {
  // Set up desired output.
  var want: {
    animal: "dog"
    in_mouth: "treat"
    state: "happy"
  }

  // Set up sample input.
  var input: {
    type: "food"
    item: "treat"
  }

  // Execute the function.
  var got: dog::give(input)

  // Assert that the actual result equals the expected.
  test::assertEquals(want, got)
}

def test_give_toy() {
  // Set up desired output.
  var want: {
    animal: "dog"
    in_mouth: "ball"
    state: "playful"
  }

  // Set up sample input.
  var input: {
    type: "toy"
    item: "ball"
  }

  // Execute the function.
  var got: dog::give(input)

  // Assert that the actual result equals the expected.
  test::assertEquals(want, got)
}

def test_give_medicine() {
  // Set up desired output.
  var want: {
    animal: "dog"
    state: "fleeing"
  }

  // Set up sample input.
  var input: {
    type: "medicine"
    item: "ear drops"
  }

  // Execute the function.
  var got: dog::give(input)

  // Assert that the actual result equals the expected.
  test::assertEquals(want, got)
}

def test_give_unexpected() {
  // Set up desired output.
  var want: {
    animal: "dog"
    in_mouth: "laptop"
    state: "confused"
  }

  // Set up sample input.
  var input: {
    type: "technology"
    item: "laptop"
  }

  // Execute the function.
  var got: dog::give(input)
  test::assertEquals(want, got)
}
```

Accompanied by the file we are trying to test: `dog.wstl`:

```
def give(object) {
 animal: "dog"
 in_mouth: if object.type != "medicine" then object.item
 state: if object.type == "food" then "happy"
        else if object.type == "toy" then "playful"
        else if object.type == "medicine" then "fleeing"
        else "confused"
}
```

## Testing via parameterized tests

This setup is useful when repeating the same test/function upon different
inputs.

```
package dog_test

import "test"
import "./dog.wstl"

var tests: [{
    inputType: "treat"
    wantState: "happy"
},
{
    inputType: "toy"
    wantState: "playful"
},
{
    inputType: "medicine"
    wantState: "fleeing"
},
{
    inputType: "unexpected"
    wantState: "confused"
}]

// The run method allows a custom testing workflow.
test::run({
    // Iterate the test parameters.
    testState(tests[*].inputType[], tests[*].wantState[])
})

// test::runSingle will record each call as an individual test with the given
// name.
def testState(inputType, wantState) test::runSingle("dog::give - {inputType}",{
    var got: dog::give({
        type: inputType
    })

    test::assertEquals(wantState, got.state)
})
```

Accompanied by the (same as above) file we are trying to test: `dog.wstl`:

```
def give(object) {
 animal: "dog"
 in_mouth: if object.type != "medicine" then object.item
 state: if object.type == "food" then "happy"
        else if object.type == "toy" then "playful"
        else if object.type == "medicine" then "fleeing"
        else "confused"
}
```

## Testing external Whistle files

This setup is useful when the test wants to execute an entire Whistle (mapping)
file, rather than a specific function. It can be combined with either of the
above setups (e.x. loading inputs from an array, or executing different tests in
individual test functions).

```
package dog_test

import "test"

var tests: readTests("./testdata")

// The run method allows a custom testing workflow
test::run({
    // Iterate the test parameters
    doTest(tests[])
})

// Find input/output pairs and make them into test configs
def readTests(directory) {
    var inputs: listFiles("{directory}/*.input.json")

    // Pull out the base part (without input.json) of each file
    var prefixes: extractRegex(inputs[], ".+?(?=input.json$)")

    // Add output.json to each prefix
    var outputs: prefixes[] + "output.json"

    makeTests(inputs[], outputs[])
}

def makeTest(inputPath, outputPath) {
    name: input.item
    input: loadJson(inputPath)
    want: loadJson(outputPath)
}

// test::runSingle will record each call as an individual test with the given
// name.
def doTest(test) test::runSingle(test.name, {
    // Execute the file as an external file, running through the root mappings.
    var got: execPaths("./dog.wstl", [], test.input)
    test::assertEquals(test.output, got)
})
```

Accompanied by the (slightly altered from above) file we are trying to test:
`dog.wstl`:

```
dogState: give($root.itemToGiveDog)

def give(object) {
 animal: "dog"
 in_mouth: if object.type != "medicine" then object.item
 state: if object.type == "food" then "happy"
        else if object.type == "toy" then "playful"
        else if object.type == "medicine" then "fleeing"
        else "confused"
}
```

as well as a testdata directory for with files like:

`food.input.json`:

```json
{
  "itemToGiveDog": {
    "type": "food",
    "item": "treat"
  }
}
```

`food.output.json`:

```json
{
  "dogState": {
    "animal": "dog",
    "in_mouth": "treat",
    "state": "happy"
  }
}
```
