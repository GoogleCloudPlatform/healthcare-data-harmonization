# Data Harmonization Mapping Language (DHML)

This guide walks you through the basics of writing a mapping in the DHML. Basic
understanding with reading and writing languages like python or javascript is
needed.

This tutorial demonstrates the basics of writing data mapping in the Data
Harmonization Mapping Language (DHML) to transform data from one schema to
another schema. The following codelab walks you through different DHML features
in a toy sample of data mapping. There are also a few exercises to help you get
some hands-on practice with the configuration language.

## Objectives

*   Get familiar with the Data Harmonization Mapping Language (DHML) syntax
*   Practice writing mapping configurations
*   Run the mapping engine and look at the produced output
*   Understand how this can be used to transform from one healthcare standard to
    another

## Before you begin {setup}

*   Make a new directory, for example `$HOME/dhml_codelab`
*   Place the mapping configurations from the exercises in a file called
    `codelab.dhml`
*   Place the input in a file called `codelab.json` (for now the contents of the
    file should just be `{}`, we'll fill it later)

*   View the *output* in `$HOME/dhml_codelab/codelab.output.json`
*   Run your mapping using the mapping_engine binary, in mapping_engine/main. An
    example command might look like (run from mapping_engine/main): `go run . --
    -input_file_spec=$HOME/dhml_codelab/codelab.json
    -output_dir=$HOME/dhml_codelab/
    -harmonize_code_dir_spec=$HOME/dhml_codelab/code_harmonization
    -harmonize_unit_spec=$HOME/dhml_codelab/codelab-units.textproto
    -mapping_file_spec=$HOME/dhml_codelab/codelab.dhml`
*   See [running mappings](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_language/doc/reference.md#running-your-mappings) for all available options

## Hello mapping world

Start with a simple mapping example (put the config below in `codelab.dhml` from
the [Before you begin](#setup)).

```
Planet: "Earth"
```

*   `Planet` is the path of the output field. Note it is a path, not just a name
    so `Planet.someSubfield.someArray.someOtherSubfield` is also valid
*   `:` is the mapping/assignment operator, which separates the target
    (`Planet`) and the data source (`"Earth"`)
*   `"Earth"` is a constant string data source

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre>
<code>{
  "Planet": "Earth"
}
</code>
</pre>

</section>

### Add array outputs

Output data to an array instead of an object.

```
Planet[0]: "Earth"
Planet[1]: "Mars"
Planet[2]: "Jupiter"
Moon[0]: "Luna"
```

<section class="zippy">
Output:

<pre>
<code>{
  "Planet": ["Earth", "Mars", "Jupiter"],
  "Moon": ["Luna"]
}
</code>
</pre>

</section>

<section class="zippy">
**Exercise**

Add another moon, "Io", but such that it appears before "Luna".

Your output should be:

<pre>
<code>{
  "Planet": ["Earth", "Mars", "Jupiter"],
  "Moon": ["Io", "Luna"]
}
</code>
</pre>

<section class="zippy">
Hint

Copy, paste!
</section>
<section class="zippy">
Solution

<pre>
<code>Planet[0]: "Earth"
Planet[1]: "Mars"
Planet[2]: "Jupiter"
Moon[0]: "Io"
Moon[1]: "Luna"
</code>
</pre>

</section>
</section>

## Define and call functions

### Defining functions

*   A function is a set of mappings that produce a JSON object
*   It maps a set of inputs to a set of fields in its result object

Add some structure to planets.

```
Planet[0]: PlanetName_PlanetInfo("Earth")
Planet[1]: PlanetName_PlanetInfo("Mars")
Planet[2]: PlanetName_PlanetInfo("Jupiter")
Moon[0]: MoonName_MoonInfo("Luna")

def PlanetName_PlanetInfo(planetName) {
  name: planetName
  type: "Planet"
}

def MoonName_MoonInfo(moonName) {
  name: moonName
  type: "Moon"
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

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

</section>

### Calling functions

*   Calling functions is similar to C/Python
*   A simple function call looks like `FunctionName(a, b, c)`
*   Function calls are chained by passing the result of one function to the next
    one like `SingleParameterFunctionName(FunctionName(a, b, c))`
*   Similarly, multiple parameter function chaining is done like
    `MultipleParamFunctionName(FunctionName(a, b, c), d)`

Generalize our functions by making the celstial body's `type` an input. We will
also make use of a builtin function: `$ToUpper`:

``` {highlight="content:\$ToUpper content:,\sbodyType"}
Planet[0]: BodyName_BodyType_BodyInfo("Earth", "Planet")
Planet[1]: BodyName_BodyType_BodyInfo("Mars", "Planet")
Planet[2]: BodyName_BodyType_BodyInfo("Jupiter", "Planet")
Moon[0]: BodyName_BodyType_BodyInfo("Luna", "Moon")

def BodyName_BodyType_BodyInfo(bodyName, bodyType) {
  name: $ToUpper(bodyName)
  type: bodyType
}

```

<section class="zippy">
Running the mapping outputs

<pre>
<code>{
  "Moon": [
    {
      "name": "LUNA",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "name": "EARTH",
      "type": "Planet"
    },
    {
      "name": "MARS",
      "type": "Planet"
    },
    {
      "name": "JUPITER",
      "type": "Planet"
    }
  ]
}
</code>
</pre>

</section>

<section class="zippy">
**Exercise**

Create a new mapping file with a function to map the name of a star, along with
an array of planet names to an object that contains them in a field.

Your output should be:

<pre>
<code>{
  "Star": {
      "name": "SOL",
      "planets": [
        "Mercury",
        "Venus",
        "Earth"
      ]
    }
}
</code>
</pre>

Use the `$ListOf` builtin, which puts all given inputs into an array, to make an
array of the planets `"Mercury", "Venus", "Earth"`.

<section class="zippy">
Hint 1

Call a function with multiple inputs like `Function(x, y)`. To build a list of
planets as one of the parameters, you will have something like `Function(?,
$ListOf(????))`

</section>
<section class="zippy">
Hint 2

Your Output mapping might look like

`Star: SunName_Planets_SunInfo("Sol", $ListOf("Mercury", "Venus", "Earth"))`

Given this, write the function `SunName_Planets_SunInfo`.
</section>
<section class="zippy">
Solution

<pre>
<code>Star: SunName_Planets_SunInfo("Sol", $ListOf("Mercury", "Venus", "Earth"))

def SunName_Planets_SunInfo(sunName, planets) {
  name: $ToUpper(sunName)
  planets: planets
}
</code>
</pre>

</section>
</section>

### Mapping using $this

*   Functions by default map to fields in the return value of the function
*   The `$this` keyword allows functions to return a value instead

Set the `Primitive` field to the number `20` using a function.

```
Primitive: Num_DoubleNum(10)

def Num_DoubleNum(num) {
  $this: $Mul(2, num)
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre><code>{
  "Primitive": 20
}
</code></pre>

</section>

### Merge semantics

*   Arrays are concatenated
    *   Even though both functions mapped to `colours[0]`, `"blue"` ended up in
        `colours[1]`
*   New fields are added
*   Similar fields produce a merge conflict. An overwrite can be forced (see
    [Overwriting](#overwriting-fields))

<section class="zippy">
**Exercise**

Refactor the following functions so that the common fields in them are mapped by
a shared function. In your solution, none of your functions should have any
target fields in common (`tireProperties[0]`, `tireProperties[1]` and
`tireProperties[2]` can be considered distinct fields in this exercise).

<pre>
<code>def Sedan_Vehicle(sedan) {
  doors: sedan.doors
  tireProperties[0].key: "Type"
  tireProperties[0].value: sedan.tireType
  tireProperties[1].key: "Size"
  tireProperties[1].value: sedan.tireSize
  digitalSpeedometer: $Eq(sedan.speedometer.type, "Digital")
  type: "Sedan"
}

def Lorry_Vehicle(lorry) {
  doors: lorry.doors
  tireProperties[0].key: "Type"
  tireProperties[0].value: lorry.tireType
  tireProperties[1].key: "Size"
  tireProperties[1].value: lorry.tireSize
  tireProperties[2].key: "Number"
  tireProperties[2].value: lorry.tireNum
  towCapacity: lorry.towing.capacity
  type: "Lorry"
}
</code>
</pre>

<section class="zippy">
Hint

The common target fields are:

<pre>
<code>doors
tireProperties[0]...
tireProperties[1]...
type
</code>
</pre>

</section>
<section class="zippy">
Solution

<pre>
<code>def Any_VehicleCommon(any, anyType) {
  doors: any.doors
  tireProperties[0].key: "Type"
  tireProperties[0].value: any.tireType
  tireProperties[1].key: "Size"
  tireProperties[1].value: any.tireSize
  type: anyType
}

def Sedan_Vehicle(sedan) {
  $this: Any_VehicleCommon(sedan, "Sedan")
  digitalSpeedometer: $Eq(sedan.speedometer.type, "Digital")
}

def Lorry_Vehicle(lorry) {
  $this: Any_VehicleCommon(lorry, "Lorry")
  tireProperties[2].key: "Number"
  tireProperties[2].value: lorry.tireNum
  towCapacity: lorry.towing.capacity
}
</code>
</pre>

</section>
</section>

## Mapping from data

Start by moving our planets and moons over to the input file `codelab.json`. See
[Setup](#setup) for more details. Set its contents to:

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

*   This data will now be loaded into an input called `$root`
*   Data loading into an input to the mapping engine will always be in this
    `$root` input
*   $root can be used inside functions as well
    *   You should avoid accessing `$root` inside a function because it is a strong sign of messy, non-modular mappings.
        </section>

``` {highlight="content:root\."}
Planet[0]: BodyName_BodyType_BodyInfo($root.Planets[0], "Planet")
Planet[1]: BodyName_BodyType_BodyInfo($root.Planets[1], "Planet")
Planet[2]: BodyName_BodyType_BodyInfo($root.Planets[2], "Planet")
Moon[0]: BodyName_BodyType_BodyInfo($root.Moons[0], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  type: bodyType
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre><code>{
    "Planet": [
        {
            "name": "EARTH",
            "type": "Planet"
        },
        {
            "name": "MARS",
            "type": "Planet"
        },
        {
            "name": "JUPITER",
            "type": "Planet"
        }
    ],
    "Moon": [
        {
            "name": "LUNA",
            "type": "Moon"
        }
    ]
}
</code></pre>

</section>

> NOTE: Since each element in the input `Planets` and `Moons` arrays is an
> object, we add '.name' inside our function to get its 'name' field.
> Alternatively, keep the function the same and add `.name` to the function's
> input: `root.Planets[0].name`.

## Arrays

### Iteration

The syntax for iterating an array is suffixing it with `[]`. More abstractly:

*   `Function(a[])` means "pass each element of `a` (one at a time) to
    `Function`"
*   `Function(a[], b)` means "pass each element of `a` (one at a time), along
    with `b` to `Function`". If `b` is an array of the same length as `a` we can
    iterate them together
*   `Function(a[], b[])` means "pass each element of `a` (one at a time), along
    with each element of `b` (at the same index) to `Function`"
*   `[]` is also allowed after function calls
    *   `Function2(Function(a)[])` means "pass each element from the result of
        `Function(a)` (one at a time) to `Function2`
*   The result of an iterating function call is also an array

Adjust the mapping to iterate over the `Planets` and `Moons` arrays:

``` {highlight="content:\[\]"}
Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  type: bodyType
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre><code>{
    "Planet": [
        {
            "name": "EARTH",
            "type": "Planet"
        },
        {
            "name": "MARS",
            "type": "Planet"
        },
        {
            "name": "JUPITER",
            "type": "Planet"
        }
    ],
    "Moons": [
        {
            "name": "LUNA",
            "type": "Moon"
        }
    ]
}
</code></pre>

</section>

<section class="zippy">
**Exercise**

**Without removing anything** from the existing mapping, adjust the mapping to
make the output look like:

<pre>
<code>{
  "Moon": [
    {
      "name": "LUNA",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "extraInfo": {
        "fullName": "Planet EARTH"
      },
      "name": "EARTH",
      "type": "Planet"
    },
    {
      "extraInfo": {
        "fullName": "Planet MARS"
      },
      "name": "MARS",
      "type": "Planet"
    },
    {
      "extraInfo": {
        "fullName": "Planet JUPITER"
      },
      "name": "JUPITER",
      "type": "Planet"
    }
  ]
}
</code>
</pre>

> NOTE: `Moon` is unchanged.

Make use of the `$StrCat` builtin, where `$StrCat("one", " ", "two", " ",
"three")` makes `"one two three"`.

<section class="zippy">
Hint

We can't remove anything, and we can't change the existing function because the
`Moon` mapping does not have the new `extraInfo` field (and we haven't learned
conditions yet). So we must be mapping the BodyInfos produced by
`BodyName_BodyType_BodyInfo` in the first line for `Planet` to something new.
We'll need to add `[]` to the end of that function call and send it through a
new function that builds this new object.

We'll also need to use <code>$this</code> in our new function to merge the current data with the `extraInfo` field.
</section>
<section class="zippy">
Solution

<pre>
<code>Planet: BodyInfo_ExtendedBodyInfo(BodyName_BodyType_BodyInfo[]($root.Planets[], "Planet"))
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  type: bodyType
}

def BodyInfo_ExtendedBodyInfo(info) {
  $this: info
  extraInfo.fullName: $StrCat(info.type, " ", info.name)
}
</code>
</pre>

</section>
</section>

### Appending {appending}

*   The mapping engine allows you to append to an array using `[]`
*   `[]` in the middle of the path (e.g. types[].typeName: ...) is valid as well
    and creates `types: [{"typeName": ... }]`
*   Hardcoded indexes can also be used (e.g.`types[0]: ...` and `types[1]: ...`)
*   "Out of bounds" indexes (e.g. `types[153]: ...` generates all the missing
    elements as `null`

With index numbers:

```
Planet[0]: "Earth"
Planet[1]: "Mars"
Planet[2]: "Jupiter"
Moon[0]: "Luna"
```

With appending:

```
Planet[]: "Earth"
Planet[]: "Mars"
Planet[]: "Jupiter"
Moon[]: "Luna"
```

Noteably:

*   The mapping engine allows you to omit the index in an array
*   Instead of writing `[0]` or `[3]`, write `[]`
*   If we remove the mapping for "Earth", we won't have to update the other
    indices to fill the gap
*   The empty index is a valid part of the JSON path in the target field.
    *   E.g: `SomeField.someArray[].someOtherField.someOtherArray[].finalField`
        is valid, and will append a new element to both `someArray` and
        `someOtherArray`

### Wildcards

*   The `[*]` syntax works like specifying an index, except that it returns an
    array of values
*   Multiple arrays mapped through with `[*]`, for example `a[*].b.c[*].d`, in
    one long, non-nested array of the values of `d` with the same item order
*   Null values are included, through jagged traversal. E.g.: `a[*].b.c[*].d`,
    if some instance of a does not have `b.c`, then a single null value is
    returned for that instance

Make a new Output Key that just contains our planet names:

``` {highlight="content:\[\*\] context:1,PlanetNames"}
PlanetNames: $root.Planets[*].name;

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  type: bodyType
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "name": "EARTH",
      "type": "Planet"
    },
    {
      "name": "MARS",
      "type": "Planet"
    },
    {
      "name": "JUPITER",
      "type": "Planet"
    }
  ],
  "PlanetNames": [
    "Earth",
    "Mars",
    "Jupiter"
  ]
}
```

</section>

Capitalize the names in `PlanetNames` using what we learned about iterating
arrays:

``` {highlight="content:\[\]\s..\s_ToUpper"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  type: bodyType
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)
<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
      "type": "Moon"
    }
  ],
  "Planet": [
    {
      "name": "EARTH",
      "type": "Planet"
    },
    {
      "name": "MARS",
      "type": "Planet"
    },
    {
      "name": "JUPITER",
      "type": "Planet"
    }
  ],
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
```

</section>

### Writing to array fields {writing_to_array}

Refactor the `type` field to an array by using the [append](#appending) syntax.

``` {highlight="content:types"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  types[]: bodyType
  types[]: "Body"
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "EARTH",
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "MARS",
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "JUPITER",
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
```

</section>

<section class="zippy">
**Exercise**

Update the mappings above to make types an array of objects. Each object should
have a single array field, which is an array with the type string in it. Define
no new functions.

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "LUNA",
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
      "name": "EARTH",
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
      "name": "MARS",
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
      "name": "JUPITER",
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
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
</code>
</pre>

<section class="zippy">
Hint

`types[]` adds a new item to the `types` array. What does `types[].array` do?
</section>
<section class="zippy">
Solution

<pre>
<code>PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: $ToUpper(body.name)
  types[].array[]: bodyType
  types[].array[]: "Body"
}
</code>
</pre>

</section>
</section>

## Variables

*   Variables allow reusing mapped data without re-excuting it
*   The `var` keyword indicates the target field is a variable
*   Variables have identical semantics to fields
*   You can write to or iterate over them the same as any input, however
    variables don't show up in the mapping output
*   Variables cannot have the same name as any of the inputs in its function

The mapping below is equivalent to the [exercise above](#writing_to_array).

``` {highlight="content:\bvar\b content:\bbigName\b"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"
}
```

## Conditions

### Preparation

*   Update our data and mappings with some new fields and add the semi-major
    orbital axis, in millions of km, for our planets and moon, based on
    [these NASA factsheets](https://nssdc.gsfc.nasa.gov/planetary/factsheet/)
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
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"

  semiMajorAxisAU: $Div(body.semiMajorAxis, 149.598)
}
```

*   The `$Div` builtin divides our Million KM distance by our conversion
    constant to get us the distance in AU

### Conditional Mappings

*   Conditional mappings are mappings that only evaluated if a condition is met.

Add a condition so that we only output the `semiMajorAxisAU` field on planets,
and not moons:

*   Use the `$Eq` (equal) builtin for comparison
*   Use the `(if ...)` statement for conditionally executing the mapping
    *   The expression in the `if` statement is evaluated and the mapping is
        executed _if and only if_ it holds true. Otherwise the entire mapping is
        ignored

``` {highlight="content:\(if[^)]+\)"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"

  semiMajorAxisAU (if $Eq(bodyType, "Planet")): $Div(body.semiMajorAxis, 149.598)
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
      "types": [
        "Moon",
        "Body"
      ]
    }
  ],
  "Planet": [
    {
      "name": "EARTH",
      "semiMajorAxisAU": 1.0000142656266688,
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "MARS",
      "semiMajorAxisAU": 1.5235511458665132,
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "JUPITER",
      "semiMajorAxisAU": 5.2044191630277785,
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
```

</section>

### Condition Blocks

*   Similar to conditional mappings, conditional blocks allow wrapping a set of
    mappings within a condition

Set the `semiMajorAxis.unit` to `AU` if the `bodyType` is a `Planet`.`

``` {highlight="context:if,1 content:.\selse\s."}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"
  if $Eq(bodyType, "Planet") {
    semiMajorAxis.value: $Div(body.semiMajorAxis, 149.598)
    semiMajorAxis.unit: "AU"
  } else {
    semiMajorAxis.value: $Mul(body.semiMajorAxis, 1000000)
    semiMajorAxis.unit: "KM"
  }
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
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
      "name": "EARTH",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 1.0000142656266688
      },
      "types": [
        "Planet",
        "Body"
      ]
    },
    {
      "name": "MARS",
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
      "name": "JUPITER",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.2044191630277785
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
```

</section>

### Operators

*   Similar to Python/C, there are operators available for common arithmetic and
    logical operations

<section class="zippy">
All available operators:

where `num` is a number input, `bool` is a boolean input, and `any` is any type
of input:

```
num + num  // Addition
num - num  // Subtraction
num * num  // Multiplication
num / num  // Division

bool and bool  // Logical AND
bool or bool   // Logical OR
~bool          // Logical NOT

any = any      // Equal*
any ~= any     // Not Equal

any?           // Value Exists**
~any?          // Value Does Not Exist
```

> NOTE: \*Equality is qualified as a "deep equals". All elements in an array or
> values in an object must be the same to return true.

> NOTE: \*\*Existence is qualified as "is defined, is not literal `null` and is
> not empty."
>
> An empty array is one with 0 elements (`null`s count as elements). An empty
> object is one with 0 keys.

</section>

Replace $Mul(x,y), $Div(x,y), and $Eq(x,y) with basic logical and arithmetic
operations.

``` {highlight="context:*"}
if bodyType = "Planet" {
  semiMajorAxis.value: body.semiMajorAxis / 149.598
  semiMajorAxis.unit: "AU"
} else {
  semiMajorAxis.value: body.semiMajorAxis * 1000000
  semiMajorAxis.unit: "KM"
}
```

Running the mapping yields the same result.

> WARNING: `x = y = z` is a valid expression and is equivalent to `(x = y) = z`.
> If `x = y` is true this will then check `true = z`.

<section class="zippy">
**Exercise**

Add this block to your input `codelab.json`:

<pre>
<code>"Stars": [
        {
            "name": "Sol"
        }
    ],
</code>
</pre>

<section class="zippy">
Your full input file should now look like this:

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
</section>

* Add `Star: BodyName_BodyType_BodyInfo($root.Stars[], "Star")` to your
mapping just below `Moon: ...`
* Update BodyName_BodyType_BodyInfo to output semiMajorAxis according to the
following specifications:
  * Bodies with a semiMajorAxis greater than 1M KM should output a value converted to AU
  * Bodies with a semiMajorAxis less than or equal to 1M KM should output a value converted to KM
  * Bodies with no semiMajorAxis defined should have the field `orbitalRoot: true`

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "LUNA",
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
      "name": "EARTH",
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
      "name": "MARS",
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
      "name": "JUPITER",
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
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ],
  "Star": [
    {
      "name": "SOL",
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

<section class="zippy">
Hint

The `?` operator can be used to check if a field is defined.

Also remember that our input data contain semi-major axis in millions of KM, so
we are checking if it is greater than 1 to convert to AU.
</section>
<section class="zippy">
Another Hint

`if` blocks can be nested.
</section>
<section class="zippy">
Solution

<pre>
<code>PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")
Star: BodyName_BodyType_BodyInfo($root.Stars[], "Star")

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"
  if body.semiMajorAxis? {
    if body.semiMajorAxis > 1 {
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

</section>
</section>

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
million km:

``` {highlight="content:\[where.+?\]"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[where $.semiMajorAxis > 200][], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

```json
{
  "Moon": [
    {
      "name": "LUNA",
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
      "name": "MARS",
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
      "name": "JUPITER",
      "semiMajorAxis": {
        "unit": "AU",
        "value": 5.2044191630277785
      },
      "types": [
        "Planet",
        "Body"
      ]
    }
  ],
  "PlanetNames": [
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
```

</section>

<section class="zippy">
  **Exercise**

1 astronomical unit is roughly the distance between the Earth and Sun. In this
exercise, derive a constant for conversion of million KM to AU based on the
semi-major axis of Earth **from the input data**, and use that to convert the
semi-major axis of the other planets to AU.

Your output should be:

<pre>
<code>{
  "Moon": [
    {
      "name": "LUNA",
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
      "name": "MARS",
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
      "name": "JUPITER",
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
    "EARTH",
    "MARS",
    "JUPITER"
  ]
}
</code>
</pre>

<section class="zippy">
Hint

Try writing a function that maps the Earth object to a constant that converts
millions of kilometers to AU. How would you then *find* the earth object in the
input to pass to this mapping?

</section>
<section class="zippy">
Solution

<pre>
<code>PlanetNames: $ToUpper($root.Planets[*].name[]);

var kmToAU: Earths_MKmToAUConst($root.Planets[where $.name = "Earth"])
Planet: BodyName_BodyType_BodyInfo($root.Planets[where $.semiMajorAxis > 200][], "Planet", kmToAU)
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon", kmToAU)

def Earths_MKmToAUConst(earths) {
  $this: 1 / earths[0].semiMajorAxis
}

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"
  if bodyType = "Planet" {
    semiMajorAxis.value: body.semiMajorAxis * kmToAU
    semiMajorAxis.unit: "AU"
  } else {
    semiMajorAxis.value: body.semiMajorAxis * 1000000
    semiMajorAxis.unit: "KM"
  }
}
</code>
</pre>

</section>
</section>

## Post Processing

*   Post processing allows running a function after the mapping is complete
*   The input to the post processing function is the output from the mapping and
    the result will become the new output
*   A `post` function must be the last thing in the file

Reformat the output and moving the top level "Moons" array into the "Earth"
planet by defining a post process function:

``` {highlight="content:post"}
PlanetNames: $ToUpper($root.Planets[*].name[]);

Planet: BodyName_BodyType_BodyInfo($root.Planets[], "Planet")
Moon: BodyName_BodyType_BodyInfo($root.Moons[], "Moon")

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  var bigName: $ToUpper(body.name)
  name: bigName
  types[]: bodyType
  types[]: "Body"
  if bodyType = "Planet" {
    semiMajorAxis.value: body.semiMajorAxis / 149.598
    semiMajorAxis.unit: "AU"
  } else {
    semiMajorAxis.value: body.semiMajorAxis * 1000000
    semiMajorAxis.unit: "KM"
  }
}

post def RestructureExample(output) {
  solarSystem.planets: output.Planet[where $.name ~= "EARTH"]

  var earths: output.Planet[where $.name = "EARTH"]
  if earths? {
    var earths[0].moons  : output.Moon[where $.name = "LUNA"]
    solarSystem.planets[]: earths[0]
  }

  planetNames: $ToLower(output.PlanetNames[])
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre>
<code>{
  "planetNames": [
    "earth",
    "mars",
    "jupiter"
  ],
  "solarSystem": {
    "planets": [
      {
        "name": "MARS",
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
        "name": "JUPITER",
        "semiMajorAxis": {
          "unit": "AU",
          "value": 5.2044142469620995
        },
        "types": [
          "Planet",
          "Body"
        ]
      },
      {
        "moons": [
          {
            "name": "LUNA",
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
        "name": "EARTH",
        "semiMajorAxis": {
          "unit": "AU",
          "value": 1.000013321018701
        },
        "types": [
          "Planet",
          "Body"
        ]
      }
    ]
  }
}
</code>
</pre>

</section>

## Quirks

### Nulls and null propagation

*   The mapping engine handles and ignore null and missing values/fields by
    default, by following these rules:
    *   If a field is written with a null or empty value, it will be ignored
        (thus `null`, `{}`, and `[]` can never show up in the mapping output)
    *   If a non-existent field is accessed, it will return `null`
*   However, a null value is passed to a function and the function is still
    executed

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

Output:

```json
{
  "Example": [
      "included": [
        1,
        1
      ],
      "nested_1": {
        "two": "Constant"
      }
  ]
}
```

### Using `root` In a Function

The `root` keyword may be used inside a function in order to send data to the
root of the output. For example:

```
Red[]: "Blue"
Complex: Hello_World_HelloWorldObject("Hi", "Planet")

def Hello_World_HelloWorldObject(hello, world) {
    hello: hello
    world: world
    root Red[]: world
    root Complex.boo: "boo!"
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre>
<code>{
  "Complex": [
    {
      "boo": "boo!",
      "hello": "Hi",
      "world": "Planet"
    }
  ],
  "Red": [
    "Blue",
    "Planet"
  ]
}
</code>
</pre>

</section>

### Overwriting fields {overwriting}

*   In order to prevent data loss and reduce mapping errors, the DHML allows a
    primitive (string, numeric, or boolean) field to only be written once
*   Use the `!` operator to overwrite
*   Overwriting restrictions do not apply to variables

For example, the following mapping will fail with the error `attempt to
overwrite primitive destination with primitive source`.

```
X: String_X("Yep!")

def String_X(str) {
  x: str
  x: "Nope!"
}
```

Now suffix the field with the `!` operator:

```
X: String_X("Yep!")

def String_X(str) {
  x: str
  x!: "Nope!"
}
```

Output:

```json
{
  "X": {
      "x": "Nope!"
    }
}
```

## Code harmonization

*   Code Harmonization is the mechanism for mapping a code in one terminology to
    another
*   The mapping engine uses
    [FHIR ConceptMaps](https://www.hl7.org/fhir/conceptmap.html) to store lookup
    tables, and uses a subset of
    [FHIR Translate](https://www.hl7.org/fhir/conceptmap-operation-translate.html)
    mechanics to do code lookups
    *   Lookups return an array of
        [FHIR Codings](https://www.hl7.org/fhir/datatypes.html#Coding)
    *   Remote lookup is also possible
    *   Multiple concept maps and multiple remote lookup servers are supported
*   Lookup syntax
    *   $HarmonizeCode(LookupSourceName, SourceCode, SourceSystem, ConceptMapID)
    *   $HarmonizeCodeBySearch(LookupSourceName, SourceCode, SourceSystem)

### Preparation

#### ConceptMap

Start by creating a folder `code_harmonization` and adding a ConceptMap `codelab.harmonization.json` in it. Set its contents
to:

```json
{
  "group":[
    {
      "element":[
        {
          "code": "red",
          "target":[
            {
              "code": "target-red",
              "equivalence": "EQUIVALENT"
            }
          ]
        },
        {
          "code": "blue",
          "target":[
            {
              "code": "target-blue",
              "equivalence": "EQUIVALENT"
            }
          ]
        }
      ],
      "source": "codelab-source",
      "target": "codelab-target"
    }
  ],
  "id": "codelab-conceptmap-id",
  "version": "v1",
  "resourceType":"ConceptMap"
}
```

### Example

Translate the code `red` to the value specified by the ConceptMap above.

```
Codes: TranslateCode("red")

def TranslateCode(code) {
  original_code: code
  translated_code: $HarmonizeCode("$Local", code, "codelab-source", "codelab-conceptmap-id")
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre>
<code>{
  "Codes": {
    "original_code": "red",
    "translated_code": [
      {
        "code": "target-red",
        "display": "A display for code red",
        "system": "codelab-target",
        "version": "v1"
      }
    ]
  }
}
</code>
</pre>

</section>

## Unit harmonization

*   Unit harmonization is the mechanism for coverting values in one unit to
    another
*   The mapping engine uses conversion tables defined in the
    [UnitConfiguration](http://github.com/GoogleCloudPlatform/healthcare-data-harmonization/blob/master/mapping_engine/proto/unit_config.proto)
    syntax
*   Conversions return a object that contains:
    *   `originalQuantity`: the original quantity
    *   `originalUnit`: the original unit
    *   `quantity`: the converted quantity
    *   `unit`: the converted unit
    *   `system`: the unit config system
    *   `version`: the unit config version
*   Lookup syntax
    *   $HarmonizeUnit(sourceValue, sourceUnit, sourceCodingSystemArray)

### Preparation

Start by creating a text
[protobuf](https://developers.google.com/protocol-buffers)
`codelab-units.textproto`. Set its contents to:

```textproto
version: "v1"
system: "http://unitsofmeasure.org"
conversion {
  source_unit: "LB"
  dest_unit: "KG"
  code: "weight"
  codesystem: "metric"
  constant: 0.0
  scalar: 0.453592
}
conversion {
  source_unit: "G"
  dest_unit: "KG"
  code: "weight"
  codesystem: "metric"
  constant: 0.0
  scalar: 0.001
}
```

### Example

Translate the quantity `50 LB` to the `KG` using the conversion factor in the
config above.

```
Units: ConvertUnit(50, "LB")

def ConvertUnit(value, unit) {
  var codesystem.system: "metric"
  var codesystem.code: "weight"
  var codesystems: $ListOf(codesystem)
  converted_unit: $HarmonizeUnit(value, unit, codesystems)
}
```

Run the above mapping (see [Before you begin](#setup) for instructions)

<section class="zippy">
Output:

<pre>
<code>{
  "Units": {
    "converted_unit": {
      "originalQuantity": 50,
      "originalUnit": "LB",
      "quantity": 22.6796,
      "system": "http://unitsofmeasure.org",
      "unit": "KG",
      "version": "v1"
    }
  }
}
</code>
</pre>

</section>

## [FHIR](https://hl7.org/fhir/) -> [OMOP](https://www.ohdsi.org/data-standardization/the-common-data-model/) example

This exercise will map a [FHIR](https://hl7.org/fhir/)
resource to its corresponding
[OMOP](https://www.ohdsi.org/data-standardization/the-common-data-model/) table
row, specifically the [FHIR Encounter](https://www.hl7.org/fhir/STU3/encounter.html) to an
[OMOP VisitOccurrence](https://github.com/OHDSI/CommonDataModel/wiki/VISIT_OCCURRENCE).

Using what you have learnt above, create a DHML file to map the following input and iterate on it until it produces the expected output.

Input:

```json
{
  "resourceType": "Encounter",
  "id": "9eb1ee4b-12f0-4a31-8168-acd7af54023e",
  "status": "finished",
  "class": {
    "system": "http://terminology.hl7.org/CodeSystem/v3-ActCode",
    "code": "AMB"
  },
  "type": [
    {
      "coding": [
        {
          "system": "http://snomed.info/sct",
          "code": "185349003",
          "display": "Encounter for check up (procedure)"
        }
      ],
      "text": "Encounter for check up (procedure)"
    }
  ],
  "subject": {
    "reference": "Patient/e6274e20-da06-4dd7-b9be-634cc3e31927"
  },
  "participant": [
    {
      "individual": {
        "reference": "Practitioner/d9894296-41ab-4c50-a17a-8ec502b05ae7"
      }
    }
  ],
  "period": {
    "start": "2009-07-04T17:20:00-04:00",
    "end": "2009-07-04T17:50:00-04:00"
  },
  "serviceProvider": {
    "reference": "Organization/b0e04623-b02c-3f8b-92ea-943fc4db60da"
  }
}
```

Expected output:

```json
{
  "VisitOccurrence":{
    "visit_occurrence_id": "9eb1ee4b-12f0-4a31-8168-acd7af54023e",
    "person_id": "e6274e20-da06-4dd7-b9be-634cc3e31927",
    "visit_concept_id": "AMB",
    "visit_start_date": "2009-07-04",
    "visit_start_time": "17:20:00-04:00",
    "visit_end_date": "2009-07-04",
    "visit_end_time": "17:50:00-04:00",
    "visit_type_concept_id": "44818518",
    "care_site_id": "b0e04623-b02c-3f8b-92ea-943fc4db60da",
    "provider_id": "d9894296-41ab-4c50-a17a-8ec502b05ae7"
  }
}
```

### Mappings

> NOTE: The mappings below are based off the
> [OMOP on FHIR Project](http://omoponfhir.org/)

Input field                         | Mapping operations                                                        | Output field
----------------------------------- | ------------------------------------------------------------------------- | ------------
id                                  | assign as is to output field                                              | visit_occurrence_id
subject.reference                   | extract the id from the FHIR Reference                                    | person_id
class.code                          | assign as is to output field                                              | visit_concept_id
period.start                        | reformat time from "2006-01-02T15:04:05Z07:00" format to "2006-01-02"     | visit_start_date
period.start                        | reformat time from "2006-01-02T15:04:05Z07:00" format to "15:04:05Z07:00" | visit_start_time
period.end                          | reformat time from "2006-01-02T15:04:05Z07:00" format to "2006-01-02"     | visit_end_date
period.end                          | reformat time from "2006-01-02T15:04:05Z07:00" format to "15:04:05Z07:00" | visit_end_time
"44818518"                          | constant string                                                           | visit_type_concept_id
serviceProvider.reference           | extract the id from the FHIR Reference                                    | care_site_id
participant[0].individual.reference | extract the id from the FHIR Reference                                    | provider_id

<section class="zippy">
Solution:

<pre><code>
VisitOccurrence(if $root.resourceType = "Encounter"): Encounter_VisitOccurrence($root)

def ExtractReferenceID(str) {
    var temp: $StrSplit(str, "/");
    $this: temp[1];
}

def ExtractDate(str) {
    $this:  $ReformatTime("2006-01-02T15:04:05Z07:00", str, "2006-01-02");
}

def ExtractTime(str) {
    $this: $ReformatTime("2006-01-02T15:04:05Z07:00", str, "15:04:05Z07:00");
}

def Encounter_VisitOccurrence(encounter) {
    visit_occurrence_id: encounter.id
    person_id: ExtractReferenceID(encounter.subject.reference)
    visit_concept_id: encounter.class.code
    visit_start_date: ExtractDate(encounter.period.start)
    visit_start_time: ExtractTime(encounter.period.start)
    visit_end_date: ExtractDate(encounter.period.end)
    visit_end_time: ExtractTime(encounter.period.end)
    // This constant comes from the suggested mapping: 44818518 (Visit derived from EHR)
    visit_type_concept_id: "44818518"
    care_site_id: ExtractReferenceID(encounter.serviceProvider.reference)
    provider_id: ExtractReferenceID(encounter.participant[0].individual.reference)
}
</code></pre>

</section>
