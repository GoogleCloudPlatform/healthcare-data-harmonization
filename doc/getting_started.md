# Getting Started

[TOC]

This guide walks you through the basics of writing a mapping in the Whistle Data
Transformation Language. Basic understanding with reading and writing languages
like python or javascript is needed.

This tutorial demonstrates the basics of writing data mapping in Whistle to
transform data from one schema to another schema. The following codelab walks
you through different Whistle features in a toy sample of data mapping. There
are also a few exercises to help you get some hands-on practice with the
configuration language.

## Objectives

*   Get familiar with the Whistle Data Transformation Language syntax
*   Practice writing mapping configurations
*   Run the mapping engine and look at the produced output
*   Understand how this can be used to transform data from one format/schema to
    another

## Before you begin

*   Ensure
    [Gradle is installed](https://docs.gradle.org/current/userguide/installation.html)
    and added to PATH. Note your gradle version must be at least `7.0`.
*   Make a new directory, for example `$HOME/wstl_codelab`. You can use any
    other directory you like; If you do, substitute it instead of
    `$HOME/wstl_codelab` everywhere.
*   Place the mapping configurations from the exercises in a file called
    `codelab.wstl`.
*   Place the input in a file called `codelab.json` (for now the contents of the
    file should just be `{}`, we'll fill it later).
*   `cd` into the directory where you cloned this repository.
*   Run your mapping using this command: `gradle :runtime:run -q --args="-m
    $HOME/wstl_codelab/codelab.wstl -i $HOME/wstl_codelab/codelab.json"`
*   View the output in your terminal.

## Create a simple mapping

Start with a simple mapping example (put the config below in `codelab.wstl` from
the [Before you begin](#before-you-begin)).

```
package my_codelab

Planet: "Earth";
```

*   `Planet` is the path of the output field. Note it is a path, not just a name
    so `Planet.someSubfield.someArray.someOtherSubfield` is also valid
*   `:` is the mapping/assignment operator, which separates the target
    (`Planet`) and the data source (`"Earth"`)
*   `"Earth"` is a constant string data source

Run the above mapping (see [Before you begin](#before-you-begin) for
instructions). After build related output messages the last 3 lines are:

<details><summary>**Output**</summary>

<pre>
<code>{
  "Planet": "Earth"
}
</code>
</pre>

</details>

### Add array outputs

Output data to an array instead of an object by using following Whistle config:

```
package my_codelab

Planet[0]: "Earth";
Planet[1]: "Mars";
Planet[2]: "Jupiter";
Moon[0]: "Luna";
```

<details><summary>**Output**</summary>

<pre>
<code>
{
  "Moon": [
    "Luna"
  ],
  "Planet": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
</code>
</pre>

</details>

<details><summary>**Exercise**</summary>

Add another moon, "Io", but such that it appears before "Luna".

Your output should be:

<pre>
<code>
{
  "Moon": [
    "Io",
    "Luna"
  ],
  "Planet": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
</code>
</pre>

<details><summary>Hint</summary>

Copy, paste!
</details>
<details><summary>Solution</summary>

<pre>
<code>package my_codelab

Planet[0]: "Earth";
Planet[1]: "Mars";
Planet[2]: "Jupiter";
Moon[0]: "Io";
Moon[1]: "Luna";
</code>
</pre>

</details>
</details>

## Define and call functions

### Defining functions

*   A function takes in one or more input values and produces an output value.
*   The output of a function can be one of the 3 Whistle value types:
    *   An object like `{...}`
    *   An array like `[...]`
    *   A primitive like `"Earth"` or `3.14` or `true`

These types are modelled directly after JSON types. See
[The JSON RFC](https://tools.ietf.org/html/rfc7159#section-3) for information
about these types.

Let's add some object structures to planets using two functions:

```
package my_codelab

Planet[0]: PlanetName_PlanetInfo("Earth");
Planet[1]: PlanetName_PlanetInfo("Mars");
Planet[2]: PlanetName_PlanetInfo("Jupiter");
Moon[0]: MoonName_MoonInfo("Luna");

def PlanetName_PlanetInfo(planetName) {
  name: planetName;
  type: "Planet";
}

def MoonName_MoonInfo(moonName) {
  name: moonName;
  type: "Moon";
}
```

<details><summary>**Output**</summary>

<pre>
<code>{
  "Moon": [
    {
      "name": "Luna",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "type": "Planet"
    },
    {
      "name": "Mars",
      "type": "Planet"
    },
    {
      "name": "Jupiter",
      "type": "Planet"
    }
  ]
}
</code>
</pre>

</details>

Let's look at functions that return other types as well (note the function names
are arbitrary and not considered/enforced by the parser):

```
package my_codelab

Planet: ListPlanets()
Moon: ListMoons();

// These functions returns lists, note the `[...]` structure.
def ListPlanets() [
  PlanetName_PlanetInfo("Earth"),
  PlanetName_PlanetInfo("Mars"),
  PlanetName_PlanetInfo("Jupiter")
]

def ListMoons() [
  MoonName_MoonInfo("Luna")
]

// This function returns an object.
def PlanetName_PlanetInfo(planetName) {
  name: planetName;
  type: PLANET();
}

// This function returns a primitive (string).
def PLANET() "Planet"

def MoonName_MoonInfo(moonName) {
  name: moonName;
  type: MOON();
}

def MOON() "Moon"
```

The output is equivalent to the previous.

### Calling functions

*   Calling functions is similar to C/Python.
*   A simple function call looks like `FunctionName(a, b, c)`.
*   Function calls are chained by passing the result of one function to the next
    one like `SingleParameterFunctionName(FunctionName(a, b, c))`.
*   Similarly, multiple parameter function chaining is done like
    `MultipleParamFunctionName(FunctionName(a, b, c), d)`.

Generalize our functions by making the celstial body's `type` an input.

``` {highlight="content:\$ToUpper content:,\sbodyType"}
package my_codelab

Planet[0]: BodyName_BodyType_BodyInfo("Earth", "Planet");
Planet[1]: BodyName_BodyType_BodyInfo("Mars", "Planet");
Planet[2]: BodyName_BodyType_BodyInfo("Jupiter", "Planet");
Moon[0]: BodyName_BodyType_BodyInfo("Luna", "Moon");

def BodyName_BodyType_BodyInfo(bodyName, bodyType) {
  name: bodyName;
  type: bodyType;
}

```

The output is the same as the previous example's output.

<details><summary>**Exercise**</summary>

Create a new mapping file with a function to map the name of a star, along with
an array of planet names to an object that contains them in a field.

Your output should be:

<pre>
<code>{
  "Star": {
      "name": "Sol",
      "planets": [
        "Mercury",
        "Venus",
        "Earth"
      ]
    }
}
</code>
</pre>

Use the list syntax `[x, y, z]` which puts all given inputs into an array, to
make an array of the planets `"Mercury", "Venus", "Earth"` and pass it to your
function.

<details><summary>Hint 1</summary>

Call a function with multiple inputs like `Function(x, y)`. To build a list of
planets as one of the parameters, you will have something like `Function(?,
[????])`

</details>
<details><summary>Hint 2</summary>

Your Output mapping might look like

`Star: SunName_Planets_SunInfo("Sol", ["Mercury", "Venus", "Earth"])`

Given this, write the function `SunName_Planets_SunInfo`.
</details>
<details><summary>Solution</summary>

<pre>
<code>package my_codelab

Star: SunName_Planets_SunInfo("Sol", ["Mercury", "Venus", "Earth"]);

def SunName_Planets_SunInfo(sunName, planets) {
  name: sunName;
  planets: planets;
}
</code>
</pre>

</details>
</details>

#### Calling functions with closure parameter

Some plugin function or targets, such as
[array filtering](./builtins.md#where-2), [reduce](./builtins.md#reduce) or more
complicated ones like [error handling function](./builtins.md#witherror),
requires closure arguments.

On a high level,
[closure](https://en.wikipedia.org/wiki/Closure_\(computer_programming\)) is a
function together with an environment. If a (inner) function is declared within
another (enclosing) function, then the inner function can access the variables
of the enclosing function. The values of these variables (a.k.a. the
environment), along with the body/pointer/reference to the inner function
constitute a Closure. When the closure is created during the execution of the
enclosing function, the enclosing function's variables used by the inner
function are stored in the environment, and are then known as "bound" variables.
If the inner function has parameters, then these are known as "free" variables.

Closure's execution is determined at runtime, i.e. determined by the enclosing
function. For example, in logical [`and`](./builtins.md#and), if the first
closure executes to be `false` then other closures will not be even executed,
thereby supporting short-circuiting.

Sometimes closures can take one or more
[free parameters](https://en.wikipedia.org/wiki/Free_variables_and_bound_variables).
Free parameters can be regarded as arguments of the closure function whose value
is dynamically determined by the function that uses this closure. For example,
if in the previous example we accidentally wrote planets and moon into the same
array, in order to separate it into two arrays, we want to filter the array
using [`where`](./builtins.md#where-2).

```javascript
// Here's the set up (no closures yet)
package my_codelab

All[0]: BodyName_BodyType_BodyInfo("Earth", "Planet");
All[1]: BodyName_BodyType_BodyInfo("Mars", "Planet");
All[2]: BodyName_BodyType_BodyInfo("Jupiter", "Planet");
All[3]: BodyName_BodyType_BodyInfo("Luna", "Moon");

def BodyName_BodyType_BodyInfo(bodyName, bodyType) {
  name: bodyName;
  type: bodyType;
}

// TODO
Planet: ...
Moon: ...
```

We first consult [whistle reference](./builtins.md#description-61) that `where`
takes in two arguments, an array parameter and a closure parameter whose free
variable is denoted by `$` and it will be bound to each array element. So we can
do:

```javascript
Planet: where($this.All, BodyInfo_Predicate($, "Planet"));
Moon: where($this.All, BodyInfo_Predicate($, "Moon"));


def BodyInfo_Predicate(currentArrayElement, bodyType) {
  currentArrayElement.type == bodyType
}
```

> `$this` is a special [variable](#variables) representing the result of the
> current function (-- yes the root mapping is implicitly a whistle function).
> Because previously, we wrote both Planet and Moon arrays into the `All` field
> of the current result, i.e. `$this`, we reference it by `$this.All`.

In the aboved example, when `where` executes, it will replace all `$` in the
closure argument with each element of the array at runtime. Note that the
closure function can take in other arguments as well, but those arguments will
be evaluated before `where`.

Alternatively, if the closure parameter doesn't take in any extra parameter
other than the free parameters, it can be defined anonymously with just the
function body. As an example, we can rewrite the above filter operation to:

```javascript
Planet: where($this.All, {$.type == "Planet";});
// the bound variables can be defined externally
var MoonName: "Moon";
Moon: where($this.All, {$.type == MoonName;});

```

<details><summary>**Output**</summary>

<pre>
<code>
{
  "All": [
    {
      "name": "Earth",
      "type": "Planet"
    },
    {
      "name": "Mars",
      "type": "Planet"
    },
    {
      "name": "Jupiter",
      "type": "Planet"
    },
    {
      "name": "Luna",
      "type": "Moon"
    }
  ],
  "Moon": [
    {
      "name": "Luna",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "type": "Planet"
    },
    {
      "name": "Mars",
      "type": "Planet"
    },
    {
      "name": "Jupiter",
      "type": "Planet"
    }
  ]
}
</code>
</pre>

</details>

There are many other functions like `where` that takes in a collection as the
first parameter and closure as the second parameter, for example `reduce`,
`sortBy`, `uniqueBy` etc. So it can sometimes be more natural to chain them
using *selector syntax*. For example:

```javascript
PlanetNames: $this.All[where {$.type == "Planet";}][sortBy $.name][reduce if is($acc, "container") then "{$acc.name}, {$cur.name}" else "{$acc}, {$cur.name}"];
```

gives

```json
{
  "PlanetNames": "Earth, Jupiter, Mars"
}
```

> Fun fact: the selector syntax is a syntactic sugar that applies to any
> function with two arguments. For example, `BodyName_BodyType_BodyInfo("Earth",
> "Planet")` is equivalent to `"Earth"[BodyName_BodyType_BodyInfo, "Planet"]`

#### Calling functions as targets

Until this point, we have been using only variables and fields as targets:

```javascript
var myVar: ...
myField: ...
```

However, many plugins, along with a few builtins in Whistle provide custom
targets. For example, data can be printed to standard error with the logging
plugin:

```java
import "logging"

logging::logSevere(): "Oh no!"
```

Functions can also be called as targets:

```java
import "logging"

// Calling it like a regular function:
logEverywhereWithPrefix("HEY: ", "Listen!")

// Calling it as a target, making the intention clearer:
logEverywhereWithPrefix("HEY: "): "Listen!"

def logEverywhereWithPrefix(prefix, log): {
  logging::logSevere(): prefix + log
  logging::logWarning(): prefix + log
  logging::logInfo(): prefix + log
}
```

This applies to both user defined functions and native (plugin) functions. The
result/return value of a function called this way **is discarded**.

### Functions returning a primitive

*   We mentioned that functions can return Arrays, Objects, and Primitives.
*   Let's write a function that returns a primitive.

Set the `Primitive` field to the number `20` using a function.

```
package my_codelab

Primitive: Num_DoubleNum(10);

def Num_DoubleNum(num) 2 * num
```

> Note: Where should one put `;` (semicolons)? The rule is simple: semicolons
> only come at the end of field mappings. That is, if you have `some_field:
> ...;`. Another way of seeing it is that `;` only comes after `:`.

<details><summary>**Output**</summary>

<pre><code>{
  "Primitive": 20
}
</code></pre>

</details>

### Merge semantics

For more insights see [the spec](./spec.md#merge-behaviour).

Take special note of how fields are written to and merged in Whistle. Consider
the mapping below.

```
package my_codelab

Merged: MergeColors("red", "blue")

def MergeColors(color1, color2) {
  field1: "default color";
  SetColor1(color1);
  SetColor2(color2);
}

def SetColor1(color) {
  object.first: color;
  colors[]: "yellow";
  colors[1]: color;
}

def SetColor2(color) {
  object.second: color;
  colors[]: "green";
  colors[1]: color;
}
```

<details><summary>**Output**</summary>

<pre><code>{
  "Merged": {
      "field1": "default color",
      "colors": [
        "yellow",
        "blue",
        "green"
      ],
      "object": {
        "first": "red",
        "second": "blue"
      }
  }
}
</code></pre>

</details>

In the output:

*   Objects are merged.

*   Arrays are concatenated and hardcoded array indices are preserved.

    The `SetColor1(color)` and `SetColor2(color)` functions take a value and
    insert it into the `colors` array at index 1 (`colors[1]`), replacing the
    previous value at that index. For example, when calling `Merged:
    MergeColors("red", "blue")`, `"blue"` replaces `"red"`.

    These functions also insert a hard-coded value into the colors array. The
    lines `colors[]: "yellow";` and `colors[]: "green";` demonstrate this
    behavior. The value `"yellow"` is inserted at `colors[0]`, and `"green"` is
    inserted at `colors[2]`, because an element already exists at `colors[1]`.
    You can replace `colors[]: "green"` in the `SetColor2()` function with
    `colors[2]: "green"`, and the output in both cases is the same.

*   New fields in objects are simply added.

*   Previously existing fields in objects are merged recursively according to
    these rules.

*   Primitives are overwritten.

*   **null and empty values do not overwrite existing values**. null == {} ==
    [].

<details><summary>**Exercise**</summary>

Refactor the following functions so that the common fields in them are mapped by
a shared function. In your solution, none of your functions should have any
target fields in common (`tireProperties[0]`, `tireProperties[1]` and
`tireProperties[2]` can be considered distinct fields in this exercise).

<pre>
<code>def Sedan_Vehicle(sedan) {
  doors: sedan.doors;
  tireProperties[0].key: "Type";
  tireProperties[0].value: sedan.tireType;
  tireProperties[1].key: "Size";
  tireProperties[1].value: sedan.tireSize;
  digitalSpeedometer: sedan.speedometer.type == "Digital";
  type: "Sedan";
}

def Lorry_Vehicle(lorry) {
  doors: lorry.doors;
  tireProperties[0].key: "Type";
  tireProperties[0].value: lorry.tireType;
  tireProperties[1].key: "Size";
  tireProperties[1].value: lorry.tireSize;
  tireProperties[2].key: "Number";
  tireProperties[2].value: lorry.tireNum;
  towCapacity: lorry.towing.capacity;
  type: "Lorry";
}
</code>
</pre>

<details><summary>Hint</summary>

The common target fields are:

<pre>
<code>doors
tireProperties[0]...
tireProperties[1]...
type
</code>
</pre>

</details>
<details><summary>Solution</summary>

<pre>
<code>def Any_VehicleCommon(any, anyType) {
  doors: any.doors;
  tireProperties[0].key: "Type";
  tireProperties[0].value: any.tireType;
  tireProperties[1].key: "Size";
  tireProperties[1].value: any.tireSize;
  type: anyType;
}

def Sedan_Vehicle(sedan) {
  Any_VehicleCommon(sedan, "Sedan");

  digitalSpeedometer: sedan.speedometer.type == "Digital";
}

def Lorry_Vehicle(lorry) {
  Any_VehicleCommon(lorry, "Lorry");

  tireProperties[2].key: "Number";
  tireProperties[2].value: lorry.tireNum;
  towCapacity: lorry.towing.capacity;
}
</code>
</pre>

</details>
</details>

## Mapping from input data

Start by moving our planets and moons over to the input file `codelab.json`. See
[Setup](#before-you-begin) for more details. Set its contents to:

```json
{
    "Planets": [
        {
            "name": "Earth"
        },
        {
            "name": "Mars"
        },
        {
            "name": "Jupiter"
        }
    ],
    "Moons": [
        {
            "name": "Luna"
        }
    ]
}
```

*   This data will now be loaded into an input called `$root`.
*   Data loading into an input to the mapping engine will always be in this
    `$root` input.

``` {highlight="content:root\."}
package my_codelab

Planet[0]: BodyName_BodyType_BodyInfo($root.Planets[0], "Planet");
Planet[1]: BodyName_BodyType_BodyInfo($root.Planets[1], "Planet");
Planet[2]: BodyName_BodyType_BodyInfo($root.Planets[2], "Planet");
Moon[0]: BodyName_BodyType_BodyInfo($root.Moons[0], "Moon");

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  type: bodyType;
}
```

<details><summary>**Output**</summary>

<pre><code>{
    "Moon": [
        {
            "name": "Luna",
            "type": "Moon"
        }
    ],
    "Planet": [
        {
            "name": "Earth",
            "type": "Planet"
        },
        {
            "name": "Mars",
            "type": "Planet"
        },
        {
            "name": "Jupiter",
            "type": "Planet"
        }
    ]
}
</code></pre>

</details>

> NOTE: Since each element in the input `Planets` and `Moons` arrays is an
> object, we add '.name' inside our function to get its 'name' field.
> Alternatively, keep the function the same and add `.name` to the function's
> input: `root.Planets[0].name`.

## Arrays

### Iteration

The syntax for iterating an array is suffixing it with `[]`. More abstractly:

*   `Function(a[])` means "pass each element of `a` (one at a time) to
    `Function`".
*   `Function(a[], b)` means "pass each element of `a` (one at a time), along
    with `b` to `Function`".
*   `Function(a[], b[])` means "pass each element of `a` (one at a time), along
    with each element of `b` (at the same index) to `Function`". This mean `a`
    must be the same length as `b` so we can iterate them together.
*   `[]` is also allowed after function calls:
    *   `Function2(Function[](a))` means "pass each element from the result of
        `Function(a)` (one at a time) to `Function2`.
*   The result of an iterating function call is also an array.
*   An array can be passed to a target one at a time by iterating as well:
    `SomeTarget("x/y"): a[]` means pass the elements of `a`, one at a time, to
    `SomeTarget`. See
    [Calling functions as targets](#calling-functions-as-targets)

> NOTE: Iterating into a field target, such as `someVarOrField.x.y.z: array[]`
> means `for item in array; do someVarOrField.x.y.z: item'`. This means that the
> items will be merged/overwritten. If instead we use `someVarOrField.x[].y.z:
> array[]` this means that for each `item` in `array` a new object with `y.z:
> item` will be made in `someVarOrField.x`.

Adjust the mapping to iterate over the `Planets` and `Moons` arrays:

``` {highlight="content:\[\]"}
Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet");
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon");

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  type: bodyType;
}
```

The output is equivalent to the previous.

<details><summary>**Exercise**</summary>

**Without removing anything** from the existing mapping, adjust the mapping to
make the output look like:

<pre>
<code>{
  "Moon": [
    {
      "name": "Luna",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "extraInfo": {
        "fullName": "Planet Earth"
      },
      "name": "Earth",
      "type": "Planet"
    },
    {
      "extraInfo": {
        "fullName": "Planet Mars"
      },
      "name": "Mars",
      "type": "Planet"
    },
    {
      "extraInfo": {
        "fullName": "Planet Jupiter"
      },
      "name": "Jupiter",
      "type": "Planet"
    }
  ]
}
</code>
</pre>

> NOTE: `Moon` is unchanged.

<details><summary>Hint</summary>

We can't remove anything, and we can't change the existing function because the
`Moon` mapping does not have the new `extraInfo` field (and we haven't learned
conditions yet). So we must be mapping the BodyInfos produced by
`BodyName_BodyType_BodyInfo` in the first line for `Planet` to something new.
We'll need to add `[]` to the end of that function call and send it through a
new function that builds this new object.

We'll also need to use <code>$this</code> in our new function to merge the current data with the `extraInfo` field.
</details>
<details><summary>Solution</summary>

<pre>
<code>package my_codelab

Planet: BodyInfo_ExtendedBodyInfo(BodyName_BodyType_BodyInfo($root.Planets[], "Planet")[]);
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon");

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  type: bodyType;
}

def BodyInfo_ExtendedBodyInfo(info) {
  // Merge the normal info in first
  info;
  extraInfo.fullName: info.type + " " + info.name;
}
</code>
</pre>

</details>
</details>

### Appending

*   The mapping engine allows you to append to an array using `[]`.
*   `[]` in the middle of the path (e.g. types[].typeName: ...) is valid as well
    and creates `types: [{"typeName": ... }]`.
*   Hardcoded indexes can also be used (e.g.`types[0]: ...` and `types[1]:
    ...`).
*   "Out of bounds" indexes (e.g. `types[153]: ...` generates all the missing
    elements as `null`.

With index numbers:

```
Planet[0]: "Earth";
Planet[1]: "Mars";
Planet[2]: "Jupiter";
Moon[0]: "Luna";
```

With appending:

```
Planet[]: "Earth";
Planet[]: "Mars";
Planet[]: "Jupiter";
Moon[]: "Luna";
```

Notably:

*   The mapping engine allows you to omit the index in an array, try `Moon[5]:
    "Luna";` as an example.
*   Instead of writing `[0]` or `[3]`, write `[]`.
    *   If we remove the mapping for "Earth", we won't have to update the other
        indices to fill the gap when we use `[]`.
*   The empty index is a valid part of the JSON path in the target field.
    *   E.g: `SomeField.someArray[].someOtherField.someOtherArray[].finalField`
        is valid, and will append a new element to both `someArray` and
        `someOtherArray`

### Wildcards

*   The `[*]` syntax works like specifying an index, except that it returns an
    array of values.
*   Multiple arrays mapped through with `[*]`, for example `a[*].b.c[*].d`,
    results in one long, non-nested array of the values of `d` with the same
    item order.
*   Null values are included, through jagged traversal. E.g.: `a[*].b.c[*].d`,
    if some instance of `a` does not have `b.c`, then a single null value is
    returned for that instance.

Make a new Output Key that just contains our planet names:

``` {highlight="content:\[\*\] context:1,PlanetNames"}
package my_codelab

PlanetNames: $root.Planets[*].name;
```

<details><summary>**Output**</summary>

```json
{
  "PlanetNames": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
```

</details>

Prepend the words "Celestial Body" to the names in `PlanetNames` using what we
learned about iterating arrays:

``` {highlight="content:\[\]\s..\s_ToUpper"}
package my_codelab

PlanetNames: AddPrefix("Celestial Body ", $root.Planets[]);

def AddPrefix(prefix, planet) {
  prefix + planet.name
}
```

<details><summary>**Output**</summary>

```json
{
  "PlanetNames": [
    "Celestial Body Earth",
    "Celestial Body Mars",
    "Celestial Body Jupiter"
  ]
}
```

</details>

### Writing to array fields

Refactor the `types` field to an array by using the [append](#appending) syntax.

``` {highlight="content:types"}
package my_codelab

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet");
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon");

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  types[]: bodyType;
  types[]: "Body";
}
```

<details><summary>**Output**</summary>

```json
{
  "Moon": [
    {
      "name": "Luna",
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Mars",
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "types": [
        "Planet",
        "Body"
      ]
    }
  ]
}
```

</details>

<details><summary>**Exercise**</summary>

Update the mappings above to make types an array of objects. Each object should
have a single array field, which is an array with the type string in it. Define
no new functions.

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "Luna",
      "types": [
        {
          "array": [
            "Moon"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "types": [
        {
          "array": [
            "Planet"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
    },
    {
      "name": "Mars",
      "types": [
        {
          "array": [
            "Planet"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
    },
    {
      "name": "Jupiter",
      "types": [
        {
          "array": [
            "Planet"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
    },
    {
      "types": [
        {
          "array": [
            "Planet"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
    }
  ],
  "PlanetNames": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
</code>
</pre>

<details><summary>Hint</summary>

`types[]` adds a new item to the `types` array. What does `types[].array` do?
</details>
<details><summary>Solution</summary>

<pre>
<code>package my_codelab

PlanetNames: $root.Planets[*].name;

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet");
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon");

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  types[].array[]: bodyType;
  types[].array[]: "Body";
}
</code>
</pre>

> NOTE: make sure you understand the difference between the following lines:

*   `types[].array[]` results in:

```json
      "types": [
        {
          "array": [
            "Planet"
          ]
        },
        {
          "array": [
            "Body"
          ]
        }
      ]
```

*   `types[].array` results in:

```json
      "types": [
        {
          "array": "Planet"
        },
        {
          "array": "Body"
        }
      ]
```

*   `types.array[]` results in:

```json
      "types": {
        "array": [
          "Planet",
          "Body"
        ]
      }
```

</details>
</details>

## Variables

*   Variables allow reusing mapped data without re-excuting it.
*   The `var` keyword indicates the target field is a variable.
*   Variables have identical semantics to fields.
*   You can write to or iterate over them the same as any input, however
    variables don't show up in the mapping output.
*   Variables cannot have the same name as any of the inputs in its function.

The mapping below is equivalent to the
[previous exercise](#writing-to-array-fields), only instead of using `body.name`
directly, we assign its value to a variable named `tempName`.

``` {highlight="content:\bvar\b content:\bbigName\b"}
package my_codelab

PlanetNames: $root.Planets[*].name;

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var tempName: body.name;
  name: tempName;
  types[]: bodyType;
  types[]: "Body";
}
```

### Variables vs Fields

Any write not prefixed with `var` will write to a field, even if a variable with
that name exists. (b/186129826)

For example:

```
var hello: "one"
hello: "two" // Write to a field called "hello", not to the var above
helloX: hello // Reads "one" from the var above
var hello: "three" // Writes to the var above
helloY: hello // Reads "three" from the var above.
```

Will output

```json
{
  "hello": "two",
  "helloX": "one",
  "helloY": "three"
}
```

## Conditions

### Preparation

*   Update our data and mappings with some new fields and add the semi-major
    orbital axis, in millions of km, for our planets and moon, based on
    [these NASA factsheets](https://nssdc.gsfc.nasa.gov/planetary/factsheet/).
*   Update our input `codelab.json` file with:

    ```json
    {
        "Planets": [
            {
                "name": "Earth",
                "semiMajorAxis": 149.60
            },
            {
                "name": "Mars",
                "semiMajorAxis": 227.92
            },
            {
                "name": "Jupiter",
                "semiMajorAxis": 778.57
            }
        ],
        "Moons": [
            {
                "name": "Luna",
                "semiMajorAxis": 0.3844
            }
        ]
    }
    ```

*   Update our mapping to output the data in AU, or
    [Astronomical Units](https://en.wikipedia.org/wiki/Astronomical_unit)
    (converting from our input which is in millions of KM, assuming 149.598M
    KM = 1 AU):

    ``` {highlight="context:semiMajorAxisAU,1"}
    Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet");
    Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon");

    def BodyName_BodyType_BodyInfo(body, bodyType) {
      name: body.name;
      types[]: bodyType;
      types[]: "Body";

      semiMajorAxisAU: body.semiMajorAxis / 149.598;
    }
    ```

*   The `/` operator divides our Million KM distance by our conversion constant
    to get us the distance in AU.

### Conditions

*   Conditions are values that only evaluated if a condition is met.
*   Conditions in Whistle are expressed as
    [ternary expressions](https://en.wikipedia.org/wiki/%3F:).

Add a condition so that we only output the `semiMajorAxisAU` field on planets,
and not moons:

*   Use the `==` (equal) operator for comparison
*   Use the `if ... then ... else ...` statement for conditionally executing the
    mapping
    *   The expression after the `if` statement is evaluated and the value after
        `then` is evaluated and returned *if and only if* the conditions holds
        true. Otherwise the value after else is evaluated and returned.
    *   The `else ...` part is optional and defaults to `else {}` (i.e. returns
        a null value).

``` {highlight="content:\(if[^)]+\)"}
Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: bigname;
  types[]: bodyType;
  types[]: "Body";

  semiMajorAxisAU: if bodyType == "Planet" then body.semiMajorAxis / 149.598;
}
```

<details><summary>**Output**</summary>

```json
{
  "Moon": [
    {
      "name": "Luna",
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "semiMajorAxisAU": 1.0000142656266688,
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Mars",
      "semiMajorAxisAU": 1.5235511458665132,
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "semiMajorAxisAU": 5.2044191630277785,
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
}
```

</details>

### Condition Blocks

*   The expressions in the ternary can be Objects or Arrays just as well as
    other expressions.

Set the `semiMajorAxis.unit` to `AU` if the `bodyType` is a `Planet`.` Otherwise
convert it to Kilometers (rather than Millions of Kilometers).

``` {highlight="context:if,1 content:.\selse\s."}
Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  types[]: bodyType;
  types[]: "Body";
  if bodyType == "Planet" then {
    semiMajorAxis.value: body.semiMajorAxis / 149.598;
    semiMajorAxis.unit: "AU";
  } else {
    semiMajorAxis.value: body.semiMajorAxis * 1000000;
    semiMajorAxis.unit: "KM";
  }
}
```

<details><summary>**Output**</summary>

```json
{
  "Moon": [
    {
      "name": "Luna",
      "semiMajorAxis": {
        "unit": "KM",
        "value": 384400
      },
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.0000133691626891
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Mars",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.5235497800772735
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.204414497520021
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ]
}
```

</details>

*   Condition blocks can also expand to cover non-binary control flows through
    the use of \
    `else if ... then` statements.

For example:

```
if condition1 then {
  ...
} else if condition2 then {
  ...
} else {
  ...
}
```

### Operators

*   Similar to Python/C, there are operators available for common arithmetic and
    logical operations.
*   You've already seen some of these, there are some more:

<details><summary>All available operators:</summary>

where `num` is a number input, `bool` is a boolean input, `str` is a string
input, and `any` is any type of input:

```
num + num  // Addition
str + any  // Concatenation
any + str  // Concatenation
num - num  // Subtraction
num * num  // Multiplication
num / num  // Division

bool and bool  // Logical AND
bool or bool   // Logical OR
!bool          // Logical NOT

any == any      // Equal
any != any     // Not Equal

any?           // Value Exists
!any?          // Value Does Not Exist
```

> NOTE: Equality is qualified as a "deep equals". All elements in an array or
> values in an object must be the same to return true.

> NOTE: Existence is qualified as "is defined, is not literal `null` and is not
> empty."
>
> An empty array is one with 0 elements (`null`s count as elements). An empty
> object is one with 0 keys.

</details>

> WARNING: `x == y == z` is a valid expression and is equivalent to `(x == y) ==
> z`. If `x == y` is true this will then check `true == z`.

<details><summary>**Exercise**</summary>

Add this block to your input `codelab.json`:

<pre>
<code>"Stars": [
        {
            "name": "Sol"
        }
    ],
</code>
</pre>

<details><summary>Your full input file should now look like this:</summary>

<pre>
<code>{
    "Stars": [
        {
            "name": "Sol"
        }
    ],
    "Planets": [
        {
            "name": "Earth",
            "semiMajorAxis": 149.60
        },
        {
            "name": "Mars",
            "semiMajorAxis": 227.92
        },
        {
            "name": "Jupiter",
            "semiMajorAxis": 778.57
        }
    ],
    "Moons": [
        {
            "name": "Luna",
            "semiMajorAxis": 0.3844
        }
    ]
}
</code>
</pre>
</details>
<ul>
  <li>Add <code>Star: BodyName_BodyType_BodyInfo($root.Stars[], "Star")</code> to your
mapping just below <code>Moon: ...</code></li>
  <li>Update BodyName_BodyType_BodyInfo to output semiMajorAxis according to the
following specifications:</li>
  <ul>
    <li>Bodies with a semiMajorAxis greater than 1M KM should output a value converted to AU</li>
    <li>Bodies with a semiMajorAxis less than or equal to 1M KM should output a value converted to KM</li>
    <li>Bodies with no semiMajorAxis defined should have the field <code>orbitalRoot: true</code></li>
  </ul>
</ul>

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "Luna",
      "semiMajorAxis": {
        "unit": "KM",
        "value": 384400
      },
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "Earth",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.000013321018701
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Mars",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.5235497067284913
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.2044142469620995
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "Star": [
    {
      "name": "Sol",
      "orbitalRoot": true,
      "types": [
        "Star",
        "Body"
      ]
    }
  ]
}
</code>
</pre>

<details><summary>Hint</summary>

The `?` operator can be used to check if a field is defined.

Also remember that our input data contain semi-major axis in millions of KM, so
we are checking if it is greater than 1 to convert to AU.
</details>
<details><summary>Another Hint</summary>

`if` blocks can be nested.
</details>
<details><summary>Solution</summary>

<pre>
<code>Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")
Star: BodyName_BodyType_BodyInfo($root.Stars[], "Star")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name;
  types[]: bodyType
  types[]: "Body"
  if body.semiMajorAxis? then {
    if body.semiMajorAxis > 1 then {
      semiMajorAxis.value: body.semiMajorAxis / 149.598
      semiMajorAxis.unit: "AU"
    } else {
      semiMajorAxis.value: body.semiMajorAxis * 1000000
      semiMajorAxis.unit: "KM"
    }
  } else {
    orbitalRoot: true
  }
}
</code>
</pre>

</details>
</details>

## Filters

*   Filters allow narrowing an array to items that match a condition
*   The `where` keyword indicates a filter, similar to `if` indicating a
    condition
*   Each item from the array will be loaded one at a time into an input named
    `$` in the filter
*   The filter produces a new array. To iterate over the results, use the `[]`
    operator
*   Filters can only be the last element in a path, i.e. `a.b[where $.color =
    "red"].c` is invalid

Use a filter to only include planets with a semi-major axis greater than 200
million km. To do so in the previous mapping replace `Planet: ...` line with:

``` {highlight="content:\[where.+?\]"}
Planet: BodyName_BodyType_BodyInfo($root.Planets[where $.semiMajorAxis > 200][], "Planet");
```

<details><summary>**Output**</summary>

```json
{
  "Planet": [
    {
      "name": "Mars",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.5235511458665132
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.2044191630277785
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ]
}
```

</details>

<details><summary>  **Exercise**</summary>

1 astronomical unit is roughly the distance between the Earth and Sun. In this
exercise, derive a constant for conversion of million KM to AU based on the
semi-major axis of Earth **from the input data**, and use that to convert the
semi-major axis of the other planets to AU.

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "Luna",
      "semiMajorAxis": {
        "unit": "KM",
        "value": 384400
      },
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "Mars",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.5235294117647058
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "Jupiter",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.204344919786096
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "PlanetNames": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
</code>
</pre>

<details><summary>Hint</summary>

Try writing a function that maps the Earth object to a constant that converts
millions of kilometers to AU. How would you then *find* the Earth object in the
input to pass to this mapping?

</details>
<details><summary>Solution</summary>

<pre>
<code>package my_codelab

PlanetNames: $root.Planets[*].name;

var kmToAU: Earth_MKmToAUConst($root.Planets[where $.name=="Earth"][0]);
Planet: BodyName_BodyType_BodyInfo($root.Planets[where $.semiMajorAxis > 200][], "Planet", kmToAU);
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon", kmToAU);

def Earth_MKmToAUConst(Earth) 1 / Earth.semiMajorAxis

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  name: body.name;
  types[]: bodyType;
  types[]: "Body";
  if bodyType=="Planet" then {
    semiMajorAxis.value: body.semiMajorAxis * kmToAU;
    semiMajorAxis.unit: "AU";
  } else {
    semiMajorAxis.value: body.semiMajorAxis * 1000000;
    semiMajorAxis.unit: "KM";
  }
}
</code>
</pre>

</details>
</details>

## Multiple files and imports

Whistle code can be split up into multiple files. Each file must have a unique
package name. Let's make two new files:

`helpers.wstl`:

```
package my_helpers

def Earth_MKmToAUConst(Earth) 1 / Earth.semiMajorAxis
```

`main.wstl`:

```
package my_codelab

import "./helpers.wstl"

PlanetNames: $root.Planets[*].name;

// Note the package syntax var kmToAU:
var kmToAU : my_helpers::Earth_MKmToAUConst($root.Planets[where $.name=="Earth"][0]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[where $.semiMajorAxis > 200][], "Planet", kmToAU);
Moon:BodyName_BodyType_BodyInfo($root.Moons[], "Moon", kmToAU);

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  name: body.name;
  types[]: bodyType;
  types[]: "Body";
  if bodyType=="Planet" then {
    semiMajorAxis.value: body.semiMajorAxis * kmToAU;
    semiMajorAxis.unit: "AU";
  } else {
    semiMajorAxis.value: body.semiMajorAxis * 1000000;
    semiMajorAxis.unit: "KM";
  }
}
```

> NOTE: When running this don't forget to update the command line to run
> main.wstl instead of codelab.wstl

## Quirks

### Nulls and null propagation

*   The mapping engine handles null and missing values/fields by following these
    rules:
    *   If a non-null/non-empty field is written with a null or empty value, it
        will *not* be overwritten.
    *   If a non-existent field is accessed, it will return `null`
*   If a null value is passed to a function, the function is still executed.

For example:

Input:

```json
{
  "Red": {
    "Blue": 1
  }
}
```

Mapping:

```
package my_codelab

Example: Root_Example($root)

def Root_Example(rt) {
  // This field does not appear in the output
  excluded: rt.Abcdefghijklmnop

  // This array will only contain the existing items
  included[]: rt.Red.Blue
  included[]: rt.Abcd[123].efghi[*].jk[*].lmnop
  included[]: rt.Red.Blue

  // nested_1 will appear with just the constant, nested_2 will not appear
  nested_1: Nested_Example(rt.Abcdefghijklmnop, "Constant")
  nested_2: Nested_Example(rt.Abcdefghijklmnop, rt.Abcdefghijklmnop)
}

def Nested_Example(one, two) {
  one: one
  two: two
}
```

**Output**

```json
{
  "Example": {
      "included": [
        1,
        1
      ],
      "nested_1": {
        "two": "Constant"
      }
  }
}
```

### Using `side` In a Function

The `side` keyword may be used inside a function in order to send data to the up
the stack (sequence of functions leading to this point). For example:

```
package my_codelab

Red[]: "Blue";
Complex: Hello_World_HelloWorldObject("Hi", "Planet");

def Hello_World_HelloWorldObject(hello, world) {
    hello: hello;
    world: world;
    side Red[]: world;
    side Complex.boo: "boo!";
}
```

Run the above mapping (see [Before you begin](#before-you-begin) for
instructions).

See also [withSides](./builtins.md#withSides) and in the
[spec](./spec.md#side-outputs) for a description and examples of how to "catch"
these outputs.

<details><summary>**Output**</summary>

<pre>
<code>{
  "Complex": {
      "boo": "boo!",
      "hello": "Hi",
      "world": "Planet"
  },
  "Red": [
    "Blue",
    "Planet"
  ]
}
</code>
</pre>

</details>
