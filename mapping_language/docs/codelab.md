# Data Harmonization Mapping Language (DHML)

This guide will walk you through the basics of writing a mapping in the DHML.
Basic understanding with reading and writing languages like python or javascript
is needed.

## Background

The DHML expresses data mappings from one schema to another. It lets users
transform complex, nested data formats into other complex and nested data
formats. This codelab will walk you through its features by building upon a toy
example of data mapping.

## Setup {setup}

If you wish to execute the code in this guide:

-   Make a new directory, for example `$HOME/dhml_codelab`.

-   Place the *code* in a file called `codelab.dhml`,

-   Place the *input* in a file called `codelab.json` (for now the contents of
    the file should just be `{}`, we'll fill it later)

-   View the *output* in `$HOME/dhml_codelab/codelab.output.json`

-   Run your mapping using the mapping_engine binary, in mapping_engine/main.
    An example command might look like (run from mapping_engine/main):
    `go run . -- -input_file_spec=$HOME/dhml_codelab/codelab.json
    -output_dir=$HOME/dhml_codelab/
    -mapping_file_spec=$HOME/dhml_codelab/codelab.dhml`

## Hello Mapping World

Let's start with a simple mapping example (put this in `codelab.dhml` from the
[Setup](#setup)).

```
Planet: "Earth"
```

Let's break it down.

-   `Planet` is the path of the output field. Note it is a path, not just a name
    so `Planet.someSubfield.someArray.someOtherSubfield` is also valid. Try it
    to see what happens.
-   `:` is the mapping/assignment operator, which separates the target
    (`Planet`) and the data source (`"Earth"`)
-   `"Earth"` is a constant string data source

(see [Setup](#setup) for how to run this mapping)

<section class="zippy">
Running this mapping yields:

<pre>
<code>{
  "Planet": "Earth"
}
</code>
</pre>

</section>

**Let's add some more outputs**

```
Planet[0]: "Earth"
Planet[1]: "Mars"
Planet[2]: "Jupiter"
Moon[0]: "Luna"
```

We've now introduced a new part of the path, `[i]`, where i is an index. This is
standard JSON notation for indexing an array. The mapping engine will
automatically create elements that do not yet exist. If you are curious, try to
set one of the indices to 15, and see what happens.

> WARNING: Mapping to specific array indices is rarely useful, since if you
> remove or insert a mapping, you have to cascade your changes on to the
> mappings after. We will later see how how to omit the indices in favour of
> letting the engine keep track of them for us.

<section class="zippy">
Running this mapping yields:

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

## Functions

A function is a set of mappings that produce a JSON object. It maps a set of
inputs to a set of fields in its result object.

**The usual naming convention for functions in DHML is
Input1_Input2_InputN_Output**, though this is not formally enforced by tooling.

Let's add some structure to our planets.

```
Planet[0]: "Earth" => PlanetName_PlanetInfo
Planet[1]: "Mars" => PlanetName_PlanetInfo
Planet[2]: "Jupiter" => PlanetName_PlanetInfo
Moon[0]: "Luna" => MoonName_MoonInfo

def PlanetName_PlanetInfo(planetName) {
  name: planetName
  type: "Planet"
}

def MoonName_MoonInfo(moonName) {
  name: moonName
  type: "Moon"
}
```

<section class="zippy">
Running this mapping yields:

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

Breaking down the syntax, a simple function call looks like:

```
a, b, c => FunctionName
```

This is read as "take inputs `a`, `b`, and `c` and pipe them to function
`FunctionName`.

We can chain function calls by piping the result of one function to the next
one:

```
a, b, c => FunctionName => AnotherFunctionName
```

<section class="zippy">
The equivalent in a C/Python style language would be

<pre>
<code>AnotherFunctionName(FunctionName(a, b, c))
</code>
</pre>

</section>

This assumes `AnotherFunctionName` takes only a single input. We can add inputs
if `AnotherFunctionName` takes multiple inputs.

```
(a, b, c => FunctionName), d => AnotherFunctionName
```

<section class="zippy">
The equivalent in a C/Python style language would be

<pre>
<code>AnotherFunctionName(FunctionName(a, b, c), d)
</code>
</pre>

</section>

We must now add brackets to make it clear which inputs go to which function.
Without the brackets in this case, the code will not compile.

Let's generalize our functions by making the celstial body's `type` an input. We
will also make use of a builtin function: `$ToUpper`:

``` {highlight="content:..\s\$ToUpper content:,\sbodyType"}
Planet[0]: "Earth", "Planet" => BodyName_BodyType_BodyInfo
Planet[1]: "Mars", "Planet" => BodyName_BodyType_BodyInfo
Planet[2]: "Jupiter", "Planet" => BodyName_BodyType_BodyInfo
Moon[0]: "Luna", "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(bodyName, bodyType) {
  name: bodyName => $ToUpper
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
Hint

Remember that you can call a function with multiple inputs like `x, y =>
Function`. To build a list of planets as one of the parameters, you will have
something like `?, (???? => $ListOf) => Function`

</section>
<section class="zippy">
Another Hint

Your Output mapping might look like

`Star: "Sol", ("Mercury", "Venus", "Earth" => $ListOf) =>
SunName_Planets_SunInfo`

Given this, write the function `SunName_Planets_SunInfo`.
</section>
<section class="zippy">
Solution

<pre>
<code>Star: "Sol", ("Mercury", "Venus", "Earth" => $ListOf) => SunName_Planets_SunInfo

def SunName_Planets_SunInfo(sunName, planets) {
  name: sunName => $ToUpper
  planets: planets
}
</code>
</pre>

</section>
</section>

### Mapping using $this

All of the above functions map to fields in the return value of the function.
What if we want a function to return a number? We can do this by mapping to
`$this`:

```
Primitive: 10 => Num_DoubleNum
Merged: "red", "blue" => Colour_Colour_MergedColours

def Num_DoubleNum(num) {
  $this: 2, num => $Mul
}

def Colour_Colour_MergedColours(col1, col2) {
  $this: col1 => Colour_Col1

  // Merge the result of Colour_Col2 with $this.
  $this: col2 => Colour_Col2
}

def Colour_Col1(col) {
  colour.first: col
  colours[0]: col
}

def Colour_Col2(col) {
  colour.second: col
  colours[0]: col
}
```

<section class="zippy">
Running this mapping yields

<pre><code>{
  "Merged": {
      "colour": {
        "first": "red",
        "second": "blue"
      },
      "colours": [
        "red",
        "blue"
      ]
  },
  "Primitive": 20
}
</code></pre>

</section>

> NOTE: Pay attention to the merge semantics: The fields were preserved as is
> but the array was concatenated; Even though both functions mapped to
> `colours[0]`, `"blue"` ended up in `colours[1]`. If two fields have the same
> name, both fields must be arrays or both fields must be objects. Otherwise,
> then a merge conflict will arise and the mapping will fail. An overwrite can
> be forced (see [Overwriting](#overwriting-fields))

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
  digitalSpeedometer: sedan.speedometer.type, "Digital" => $Eq
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

Our solution will have those fields in a function the result of which will be
merged using $this. It also seems `type` differs but is hardcoded, so it
it will likely be an input to our extracted function.
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
  $this: sedan, "Sedan" => Any_VehicleCommon
  digitalSpeedometer: sedan.speedometer.type, "Digital" => $Eq
}

def Lorry_Vehicle(lorry) {
  $this: lorry, "Lorry" => Any_VehicleCommon
  tireProperties[2].key: "Number"
  tireProperties[2].value: lorry.tireNum
  towCapacity: lorry.towing.capacity
}
</code>
</pre>

</section>
</section>

## Mapping from data

So far, all our input data has been hardcoded into our mappings. Now let's use
an input file and move our planets and moon over there. In the [Setup](#setup)
we made a file called `codelab.json`. Let's set its contents to:

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

This data will now be loaded into an input called `$root`. Data loading into an
input to the mapping engine will always be in this `$root` input.

<section class="zippy">
$root should only be used outside of functions.

You should avoid accessing `$root` inside a function because it is a strong sign
of messy, non-modular mappings.
</section>

``` {highlight="content:root\."}
Planet[0]: $root.Planets[0], "Planet" => BodyName_BodyType_BodyInfo
Planet[1]: $root.Planets[1], "Planet" => BodyName_BodyType_BodyInfo
Planet[2]: $root.Planets[2], "Planet" => BodyName_BodyType_BodyInfo
Moon[0]: $root.Moons[0], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  type: bodyType
}
```

> NOTE: Since each element in the input `Planets` and `Moons` arrays is an
> object, we added '.name' inside our function to get its 'name' field. We could
> have alternatively kept the function the same and added `.name` to the
> function's input: `root.Planets[0].name`.

## Arrays

### Iteration

Our mappings currently have some hardcoded array indices - this will break if
there are not exactly 3 planets and 1 moon. Let's adjust our mapping to iterate
over the `Planets` and `Moons` arrays:

``` {highlight="content:\[\]"}
Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  type: bodyType
}
```

This yields the same output as before, but now we can add more elements to our
input arrays in `codelab.json` and they will get mapped.

The syntax for iterating an array is suffixing it with `[]`. More abstractly,

`a[] => Function` means "pipe each element of `a` (one at a time) to
`Function`". In our above mapping, we also pipe the constant "Planet";

`a[], b => Function` means "pipe each element of `a` (one at a time), along with
`b` to `Function`". If `b` is an array of the same length as `a` we can iterate
them together:

`a[], b[] => Function` means "pipe each element of `a` (one at a time), along
with each element of `b` (at the same index) to `Function`"

> NOTE: The result of an iterating mapping is itself an array. In our planets
> example, The result of `root.Planets[], "Planet" =>
> BodyName_BodyType_BodyInfo` is an array of BodyInfos. We then write this array
> to `Planets`. If we want to iterate over this array, and pipe it along one
> item at a time to something else, we can do
>
> `root.Planets[], "Planet" => BodyName_BodyType_BodyInfo[] =>
> BodyInfo_SomethingElse`.

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

Make use of the `$StrCat` builtin, where `"one", " ", "two", " ", "three" =>
$StrCat` makes `"one two three"`.

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
<code>Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo[] => BodyInfo_ExtendedBodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  type: bodyType
}

def BodyInfo_ExtendedBodyInfo(info) {
  $this: info
  extraInfo.fullName: info.type, " ", info.name => $StrCat
}
</code>
</pre>

</section>
</section>

### Appending

At the beginning of the codelab, we mentioned that you would rarely map to a
specific array index, as this is generally fragile. The mapping engine allows
you to omit the index in a target path (instead of writing `[0]` or `[3]` just
write `[]`), indicating that a new element should be appended. For example,

```
Planet[0]: "Earth"
Planet[1]: "Mars"
Planet[2]: "Jupiter"
Moon[0]: "Luna"
```

May be equivalently, but more robustly expressed as

```
Planet[]: "Earth"
Planet[]: "Mars"
Planet[]: "Jupiter"
Moon[]: "Luna"
```

Now, if we remove the mapping for "Earth", we won't have to update the other
indices to fill the gap.

> NOTE: The empty index is a valid part of the JSON path in the target field.
> That is, `SomeField.someArray[].someOtherField.someOtherArray[].finalField` is
> valid, and will append a new element to both `someArray` and `someOtherArray`.

### Wildcards

Let's make a new Output Key that just contains our planet names:

``` {highlight="content:\[\*\] context:1,PlanetNames"}
PlanetNames: $root.Planets[*].name;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  type: bodyType
}
```

The `[*]` syntax works like specifying an index, except that it returns an array
of values, in this case `["Earth", "Mars", "Jupiter"]`.

<section class="zippy">
Thus running this mapping yields:

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

> WARNING: `null` values and missing fields are filtered out. Try adding a
> planet with no `name` field to `codelab.json`. See that it is omitted from
> `PlanetNames` but an entry with no name appears in `Planet`.

> NOTE: If there are multiple arrays mapped through with `[*]`, for example
> `a[*].b.c[*].d`, the result will be one long, non-nested array of the values
> of `d`. Item order will be maintained.

Let's capitalize the names in `PlanetNames` using what we learned about
iterating arrays:

``` {highlight="content:\[\]\s..\s_ToUpper"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  type: bodyType
}
```

<section class="zippy">
Running the mapping now gives us

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

### Writing to array fields

What if we want to write to an array field? Let's make `type` an array:

``` {highlight="content:types"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  types[]: bodyType
  types[]: "Body"
}
```

Aside from renaming the field to `types` to signify that it's a collection,
we've added the `[]` syntax, which means "append an item".

<section class="zippy">
Running this mapping outputs:

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

> NOTE: We can also hardcode the index: `types[0]: ...` and `types[1]: ...`
> gives us the same result. If an index is written to that is "out of bounds",
> e.x. `types[153]: ...`, this will create all the missing elements as `null`.

> NOTE: The `[]` may appear in the middle of the path as well:
> `types[].typeName: ...` is a valid target that will create `types: [{
> "typeName": ... }]`

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
<code>PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  name: body.name => $ToUpper
  types[].array[]: bodyType
  types[].array[]: "Body"
}
</code>
</pre>

</section>
</section>

## Variables

Some mapping scenarios require some temporary storage for information. For
example, if we want to reuse some mapped data without executing the mapping
again:

``` {highlight="content:\bvar\b content:\bbigName\b"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: body.name => $ToUpper
  name: bigName
  types[]: bodyType
  types[]: "Body"
}
```

The output of this mapping is the same as before. The `var` keyword indicates
the target field is a variable.

**Variables have identical semantics to fields.**

You can write to `var a[]: ...` or `var a.b: ...` or iterate over them the same
as any input. The only difference is that they don't show up in the mapping
output.

> NOTE: Variables cannot have the same name as any of the inputs in its
> function.

## Conditions

### Preparation

Before we get into conditions, let's update our data and mappings with some new
fields. Let's add the semi-major orbital axis, in millions of km, for our
planets and moon, based on
[these NASA factsheets](https://nssdc.gsfc.nasa.gov/planetary/factsheet/).
Update our input `codelab.json` file with:

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

Let's update our mapping to output the data in AU, or
[Astronomical Units](https://en.wikipedia.org/wiki/Astronomical_unit)
(converting from our input which is in millions of KM, assuming 149.598M KM = 1
AU):

``` {highlight="context:semiMajorAxisAU,1"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: body.name => $ToUpper
  name: bigName
  types[]: bodyType
  types[]: "Body"

  semiMajorAxisAU: body.semiMajorAxis, 149.598 => $Div
}
```

The `$Div` builtin divides our Million KM distance by our conversion constant to
get us the distance in AU.

### Conditional Mappings

Mappings can be made to execute only if a condition on the data is met. Let's
add a condition so that we only output the `semiMajorAxisAU` field on planets,
and not moons:

``` {highlight="content:\(if[^)]+\)"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: body.name => $ToUpper
  name: bigName
  types[]: bodyType
  types[]: "Body"

  semiMajorAxisAU (if bodyType, "Planet" => $Eq): body.semiMajorAxis, 149.598 => $Div
}
```

Aside from the new `$Eq` (equal) builtin, the new thing we see in this mapping
is the `(if ...)` statement. This is inserted before the colon, after the target
field. The expression in the `if` statement is evaluated and the mapping is
executed _if and only if_ it holds true. Otherwise the entire mapping is
ignored.

<section class="zippy">
Running the mapping yields:

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

For the moon, we will leave the orbit in KM. We will also move the unit to its
own field to avoid confusion. Although we can express the condition by checking
if `bodyType` is equal to `"Moon"` on the unit and value mappings individually,
we can make our mapping more robust and easier to maintain/update by using
`if/else` and not repeating the condition multiple times:

``` {highlight="context:if,1 content:.\selse\s."}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: body.name => $ToUpper
  name: bigName
  types[]: bodyType
  types[]: "Body"
  if bodyType, "Planet" => $Eq {
    semiMajorAxis.value: body.semiMajorAxis, 149.598 => $Div
    semiMajorAxis.unit: "AU"
  } else {
    semiMajorAxis.value: body.semiMajorAxis, 1000000 => $Mul
    semiMajorAxis.unit: "KM"
  }
}
```

<section class="zippy">
Running this mapping yields:

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

Writing `x, y => $Mul`, `$Div`, and `$Eq` is clunky. The DHML contains operators
for basic logical and arithmetic operations. Let's simplify our conditions to

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

> WARNING: `x = y = z` is a valid expression but will not do what you expect. It
> Will be executed as `(x = y) = z`. So if `x = y` is true this will then check
> `true = z`.

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

Add `Star: $root.Stars[], "Star" => BodyName_BodyType_BodyInfo` to your
mapping just below `Moon: ...`.

Now, update BodyName_BodyType_BodyInfo to output semiMajorAxis according to the
following specifications:

1) Bodies with a semiMajorAxis greater than 1M KM should output a value converted to AU
1) Bodies with a semiMajorAxis less than or equal to 1M KM should output a value converted to KM
1) Bodies with no semiMajorAxis defined should have the field `orbitalRoot: true`

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
<code>PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo
Star: $root.Stars[], "Star" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType) {
  var bigName: body.name => $ToUpper
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

Filters allow narrowing an array down to just items that pass a condition. Let's
use a filter to only include planets with a semi-major axis greater than 200
million km:

``` {highlight="content:\[where.+?\]"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[where $.semiMajorAxis > 200][], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo
```

The `where` keyword indicates a filter, similar to `if` indicating a condition.
Each item from the array will be loaded into an input named `$` in the filter.

> WARNING: The filter produces a new array, so we retain the iteration operator
> `[]` afterwards, as we had before.

> NOTE: Filters can only be the last element in a path, i.e. `a.b[where
> $.color = "red"].c` is invalid.

<section class="zippy">
Running the mapping yields:

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
input to pipe to this mapping?

</section>
<section class="zippy">
Solution

<pre>
<code>PlanetNames: $root.Planets[*].name[] => $ToUpper;

var kmToAU: $root.Planets[where $.name = "Earth"] => Earths_MKmToAUConst
Planet: $root.Planets[where $.semiMajorAxis > 200][], "Planet", kmToAU => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon", kmToAU => BodyName_BodyType_BodyInfo

def Earths_MKmToAUConst(earths) {
  $this: 1 / earths[0].semiMajorAxis
}

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  var bigName: body.name => $ToUpper
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

Currently, the output of our mappings is limited to the form:

```json
{
  "Key1": [...],
  "Key2": [...],
  ...
}
```

What if we want to output a different form without all the fields being arrays?
To this effect, we can define a *post process* function. This function is like
any function we've seen so far - it maps input to output. However, it will be
executed at the very end of the mapping; Its input will be the output of the
mapping, and its result will become the new output.

Let's reformat our output by defining a post process function:

``` {highlight="content:post"}
PlanetNames: $root.Planets[*].name[] => $ToUpper;

Planet: $root.Planets[], "Planet" => BodyName_BodyType_BodyInfo
Moon: $root.Moons[], "Moon" => BodyName_BodyType_BodyInfo

def BodyName_BodyType_BodyInfo(body, bodyType, kmToAU) {
  var bigName: body.name => $ToUpper
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

  planetNames: output.PlanetNames[] => $ToLower
}
```

> NOTE: A `post` function must be the last thing in the file.

<section class="zippy">
Running this mapping yields:

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

The mapping engine will attempt to handle and ignore null and missing
values/fields for you:

1) If a field is written with a null or empty value, it will be ignored (thus
`null`, `{}`, and `[]` can never show up in the mapping output).

2) If a non-existent field is accessed, it will return `null`

However, a null value will still be piped to a function. Thus functions writing
fields with constants will still operate on null values.

The following example demonstrates some of these properties:

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
Example: $root => Root_Example

def Root_Example(rt) {
  // This field does not appear in the output
  excluded: rt.Abcdefghijklmnop

  // This array will only contain the existing items
  included[]: rt.Red.Blue
  included[]: rt.Abcd[123].efghi[*].jk[*].lmnop
  included[]: rt.Red.Blue

  // nested_1 will appear with just the constant, nested_2 will not appear
  nested_1: rt.Abcdefghijklmnop, "Constant" => Nested_Example
  nested_2: rt.Abcdefghijklmnop, rt.Abcdefghijklmnop => Nested_Example
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
Complex: "Hi", "Planet" => Hello_World_HelloWorldObject

def Hello_World_HelloWorldObject(hello, world) {
    hello: hello
    world: world
    root Red[]: world
    root Complex.boo: "boo!"
}
```

<section class="zippy">
Running this mapping yields:

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

In order to prevent data loss and reduce mapping errors, the DHML allows a
primitive (string, numeric, or boolean) field to only be written once. For
example, the following mapping will fail:

```
X: "Yep!" => String_X

def String_X(str) {
  x: str
  x: "Nope!"
}
```

The error will say `attempt to overwrite primitive destination with primitive
source`.

In order for it to succeed, the field can be suffixed with the `!` operator:

```
X: "Yep!" => String_X

def String_X(str) {
  x: str
  x!: "Nope!"
}
```

Which results in

```json
{
  "X": {
      "x": "Nope!"
    }
}
```

> NOTE: Overwriting restrictions do not apply to variables.
