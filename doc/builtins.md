# Package builtins

[TOC]

## Import

The builtins package does not need to be imported, or prefixed in front of any
functions. These functions are available everywhere.

## Functions

### absPath

`absPath(first: String, rest: String...)` returns `Primitive` - Primitive
`string`

#### Arguments
**first**: `String` - the first path segment

**rest**: `String...` - the remaining path segments

#### Description

Joins the given path segments, then returns the normalized absolute path as a
`Primitive` `string`. Normalization removes relative path segments, such as `./`
and `../`. If the path segments are relative, the returned path is relative to
the file where the function is called.

```
// Suppose that the file calling the following functions is located at
// file:///foo/bar/baz/myfile.wstl. The `baz` directory contains the subdirectories
// `one`, `two`, and `three`.

// Returns "file:///foo/bar/baz/one/two/three"
absPath("./one/two", "three")

// Returns "file:///foo/bar/baz/one/two"
absPath("./one/two")

// Returns "file:///foo/bar/baz/one/three"
absPath("../one/two")

// Returns "prefix:///newdir/one/two"
absPath("prefix:///newdir/one/two")
```

### and
`and(args: Closure...)` returns `Primitive` - Primitive `boolean`

#### Arguments

**args**: `Closure...` - arguments to evaluate. A `Closure` contains a lazily
evaluated expression which will be evaluated by the function if and when
appropriate.

#### Description

Returns `true` if all arguments are truthy. Evaluation stops after encountering
a falsey argument.

```
// Returns true
var a: "string1"
var b: "string2"
(a == "string1" and b == "string2")

// Returns false
var c: "string3"
var d: "string3"
(c == "string3" and d == "string4")

// Returns false. Evaluation stops after encountering the falsey argument y == "string1".
// The actual value of y is "string2".
var x: "string1"
var y: "string2"
var z: "string3"
(x == "string1" and y == "string1" and z == "string3")
```

### arrayOf
`arrayOf(items: Data...)` returns `Array`

#### Arguments
**items**: `Data...` - one or more `Data` data elements

#### Description

Creates a new `Array` from the values provided to the `items` argument. Provides
a convenient way to create an `Array` without needing to be concerned with the
`Array` implementation. Creating an array using `[foo, bar, baz]` is syntactic
sugar for the equivalent `arrayOf(foo, bar, baz)`.

```
var container1: {
  entry: [
    {
      request: {
        method: "POST"
        url: "Type/abc123"
    }
      resource: {
        active: "true"
        id: "1234567890"
      }
    }
  ]
}
var array1: ["item1", 1, 2] // Syntactic sugar for arrayOf("item1", 1, 2)
var string1: "mystring"

// Pass the data to arrayOf().
arrayOf(container1, array1, string1)

// Returns the following output:
// [
//   {
//     "entry": [
//       {
//         "request": {
//           "method": "POST",
//           "url": "Type/abc123"
//         },
//         "resource": {
//           "active": "true",
//           "id": "1234567890"
//         }
//       }
//     ]
//   },
//   {
//     "code": "200",
//     "response": "out"
//   },
//   "mystring"
// ]

// Passing an array to arrayOf() returns the same array.
arrayOf(array1)
// Returns the following output:
["item1", 1, 2]
```

### base64decode

`base64decode(inputBase64: String)` returns `Primitive` - Primitive `string` a
base64-decoded string version of the input data

#### Arguments
**inputBase64**: `String` - base64 data to decode

#### Description

Decodes the given base64 data to a `string`. The base64 encoded data must be a
UTF-8 string.

```
// Decodes the input to a string. After calling `base64decode()`, the value of `string1`
// is "this is a string".
string1: base64decode("dGhpcyBpcyBhIHN0cmluZw==")

// Decodes the base 64-encoded input and stores it in the `data1` data structure.
data1: deserializeJson(
             base64decode("eyJhcnJheTEiOlsxLDIsM10sIm5lc3RlZCI6eyJudW1iZXIyIjo0NTZ9LCJudW1iZXIxIjoxMjN9"))

// After calling `base64decode()`, the value of `data1` is:

var data1: {
    array1: [1, 2, 3]
    number1: 123
    nested: {
        number2: 456
    }
}
```

### base64encode

`base64encode(data: String)` returns `Primitive` - Primitive `string` a
base64-encoded version of the `data` input

#### Arguments

**data**: `String` - The `string` to encode. If encoding structured data, such
as a `Container`, first pass the data to the `serializeJson()` function to
convert the structured data to a `string`.

#### Description
Encodes the given `String` to UTF-8 bytes and then to base64.

An empty string/null input will return an empty string.

```
// Encodes "this is a string" to base64.
// After calling `base64encode()`, the value of `string1` is "dGhpcyBpcyBhIHN0cmluZw==".
string1: base64encode("this is a string")

var data1: {
    array1: [1, 2, 3]
    number1: 123
    nested: {
        number2: 456
    }
}

// Encodes the structured `data1` Container, then serializes it to JSON.
// After calling `base64encode()`, the value of `structure_enc` is
// "eyJhcnJheSI6WzEsMiwzXSwibmVzdGVkIjp7Im51bSI6MzIxfSwibnVtIjoxMjN9".
structure_enc: base64encode(serializeJson(structure))

// Returns null.
base64encode("")
```

### calculateElapsedDuration

`calculateElapsedDuration(iso8601StartDateTime: String, iso8601EndDateTime:
String, timeScale: String)` returns `Primitive` - Primitive `number`

#### Arguments
**iso8601StartDateTime**: `String` - start timestamp

**iso8601EndDateTime**: `String` - end timestamp

**timeScale**: `String` - time scale to represent the difference between the two
points in time. Case-sensitive and must be uppercase. Supported time scales:

*   YEARS
*   MONTHS
*   WEEKS
*   DAYS
*   HOURS
*   MINUTES
*   SECONDS
*   MILLIS

#### Description
Calculates the duration between two points in time as follows:

1.  Converts the ISO 8601-formatted start and end timestamp values to
    millisecond values.
1.  Subtracts the start time from the end time.
1.  Divides the remainder by the number of milliseconds in the provided time
    scale.

ISO 8601 timestamps use the format `yyyy-MM-dd'T'HH:mm:ss.SSSZ`.

```
var start: "2020-01-01T01:01:01.111Z"
var end: "2020-01-01T05:05:05.555Z"

// Returns 14644
calculateElapsedDuration(start, end, "SECONDS")

// Returns 244
calculateElapsedDuration(start, end, "MINUTES")

// Returns 4
calculateElapsedDuration(start, end, "HOURS")

// Returns 0
calculateElapsedDuration(start, end, "DAYS")

// Throws an IllegalArgumentException because "seconds" isn't uppercase.
calculateElapsedDuration(start, end, "seconds")
```

#### Throws

*   **`IllegalArgumentException`** - if the start time or end time aren't ISO
    8601-formatted or if an unsupported time scale is used

### calculateNewDateTime

`calculateNewDateTime(iso8601DateTime: String, timeOffset: Long, timeScale:
String)` returns `Primitive` - Primitive `string` a new ISO 8601-formatted
timestamp

#### Arguments
**iso8601DateTime**: `String` - timestamp to modify

**timeOffset**: `Long` - timeOffset to add or subtract to the `iso8601DateTime`
timestamp. Cannot be `null`.

**timeScale**: `String` - the time scale to add or subtract `timeOffset` to or
from. Case-sensitive and must be uppercase. Supported time scales:

*   YEARS
*   MONTHS
*   WEEKS
*   DAYS
*   HOURS
*   MINUTES
*   SECONDS
*   MILLIS

#### Description

If `timeOffset` is positive, adds it to `iso8601DateTime`. If `timeOffset` is
negative, subtracts it from `iso8601DateTime`. Uses the provided time scale.

ISO 8601 timestamps use the format `yyyy-MM-dd'T'HH:mm:ss.SSSZ`.

```
var start: "2020-01-01T01:01:01.111Z"
var timeOffset: 5
// Adds 5 hours to the initial timestamp and returns "2020-01-01T06:01:01.111Z"
calculateNewDateTime(start, timeOffset, "HOURS")

var start: "2020-01-01T01:01:01.111Z"
var timeOffset: 10
// Adds 10 days to the initial timestamp "2020-01-11T01:01:01.111Z"
calculateNewDateTime(start, timeOffset, "DAYS")

var start: "2020-01-01T01:01:01.111Z"
var timeOffset: -50
// Subtracts 50 days from the initial timestamp and returns "2019-11-12T01:01:01.111Z"
calculateNewDateTime(start, timeOffset, "DAYS")

// Throws an IllegalArgumentException because "seconds" isn't uppercase.
calculateNewDateTime((start, timeOffset, "seconds")
```

#### Throws

*   **`IllegalArgumentException`** - if the start time or end time aren't ISO
    8601-formatted, an unsupported time scale is used, or the value of
    `timeOffset` is `null`

### callFn

`callFn(functionName: String, args: Data...)` returns `Data` - the result of the
function call

#### Arguments
**functionName**: `String` - the name of the function to call

**args**: `Data...` - the arguments to pass to the function specified in
`functionName`

#### Description

Invokes the function provided in the `functionName` argument and passes the
`Data` arguments to the specified function. The function provided in the
`functionName` argument must be in the same package as the code that calls
`callFn`. This function uses reflection, which lets a program inspect, analyze,
and modify its own structure and behavior at runtime.

```
// A simple function that takes no arguments.
def packageFunction() {
  10
}
// Pass the simple function to the callFn function.
output: callFn("packageFunction")
// The result of the function call:
// {
//   output: 10
// }
```

### callPackageFn

`callPackageFn(packageName: String, functionName: String, args: Data...)`
returns `Data` - the result of the function call

#### Arguments

**packageName**: `String` - the package where the function specified in
`functionName` resides

**functionName**: `String` - the name of the function to call

**args**: `Data...` - the arguments to pass to the function specified in
`functionName`

#### Description

Invokes the function provided in the `functionName` argument from the package
provided in the `packageName` argument. Passes the `Data` arguments to the
specified function.

**The following examples must be run from separate Whistle files. You can't
define multiple package names in a single file.**

`my_file_1.wstl`:

```
// Declare the "foo" package.
package "foo"
// A simple function with no arguments.
def otherPackageFunction() {
  10
}
```
`my_file_2.wstl`:

```
// Declare the "bar" package.
package "bar"
// Import the file containing the "foo" package. In this example, the files are in the same
// directory.
import "./my_file_1.wstl"

// Pass the `otherPackageFunction()` function from the "foo" package to a variable in this
// package, and call `callPackageFn()` on `otherPackageFunction()`.
output: callPackageFn("foo", "otherPackageFunction")
// The result of the function call:
// {
//   output: 10
// }
```

### currentTime

`currentTime(format: String)` returns `Primitive` - `string` representing the
current local date-time in the provided format

#### Arguments
**format**: `String` - the format for representing the current local time

#### Description

Returns the current local date-time in the provided Joda-Time formatted string
(https://www.joda.org/joda-time/key_format.html).

```
// The result of calling the function is similar to `"2020-01-01T01:02:03.123Z"`.
timestamp: currentTime("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")

// The result of calling the function is similar to `"2020-01-01"`.
timestampYearMonthDate: currentTime("yyyy-MM-dd")
```

### deepCopy
`deepCopy(data: Data)` returns `Data` - a deep copy of the original `Data`

#### Arguments
**data**: `Data` - the `Data` to make a deep copy of

#### Description

Returns a deep copy of the input `Data`. Use this function to protect a variable
from being modified. See the following examples.

```
// A simple `billingAccount` container.
var billingAccount: {
   description: "Hospital charges"
   id: "example"
   resourceType: "Account"
}

// Compare the following `modifierFunction()` and `modifierFunctionDeepCopy()` functions.
// `modifierFunction()` modifies the container passed to it and returns the modified value.
def modifierFunction(container) {
  var container.description: "MODIFIED"
}
// `modifierFunctionDeepCopy()` makes a deep copy of the container passed to it and doesn't
// modify the original container.
def modifierFunctionDeepCopy(container) {
  // Replaces `container` with a deep copy.
  var billingAccount: deepCopy(container)
  // Only modifies the copy in this function scope.
  var billingAccount.description: "MODIFIED"
}

// Call `modifierFunction()` and pass the `billingAccount` container.
// `modifierFunction()` doesn't write any fields, so the returned value is null, but after
// calling this function, the value of `description` in `billingAccount` becomes `"MODIFIED"`.
var value: modifierFunction(billingAccount)

// Remove the previous line `var value: modifierFunction(billingAccount)` from your Whistle
// configuration.
// Call `modifierFunctionDeepCopy()` instead and pass the `billingAccount` container.
// `modifierFunctionDeepCopy()` doesn't write any fields, so the returned value is null, but
// after calling this function using a deep copy, the value of `description` in
// `billingAccount` is unchanged.
var value: modifierFunctionDeepCopy(billingAccount)
```

### deserializeJson
`deserializeJson(json: String)` returns `Data` - the deserialized Whistle `Data`

#### Arguments
**json**: `String` - the JSON `string` to deserialize

#### Description
Deserializes a JSON string to the Whistle `Data` format.

```
var inputJson: "\{\"a\":\"a-value\",\"b\":\"b-value\"\}"
deserializeJson(inputJson)

// Returns the following:
// {
//   a: "a-value"
//   b: "b-value"
// }
```

### div

`div(dividend: Primitive, divisor: Primitive)` returns `Primitive` - Primitive
`number`

#### Arguments
**dividend**: `Primitive` - the number to divide

**divisor**: `Primitive` - the number by which to divide the `dividend`

#### Description

Returns a `Primitive` `number` representing the quotient of two arguments.
`null` arguments are treated like the `number` `0`.

```
// Returns 5
div(10, 2)

// Returns 10. The / operator is shorthand for the div function.
20/2

// Returns 0.5
div(1, 2)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more arguments isn't a `number`

### eq

`eq(first: Data, second: Data, rest: Data...)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**first**: `Data` - the first value to compare

**second**: `Data` - the second value to compare

**rest**: `Data...` - the remaining values to compare

#### Description
Returns `Primitive` `boolean` with the value `true` if all arguments are equal.

```
// Returns true
eq(10, 10)

// Returns true. The == operator is shorthand for the eq function.
"text" == "text"

// Returns true
eq(5, 5, 5, 5, 5, 5)

// Returns false
eq(true, 7)

// Returns true (both are equivalent null data)
eq([], {})
```

### explicitEmptyString
`explicitEmptyString()` returns `Primitive`

#### Arguments

#### Description
Allows a user to explicitly output an empty string as a value for fields.

For example:

```
thisWillOutput: explicitEmptyString()
thisWillNotOutput: ""
```

### extractRegex

`extractRegex(input: String, pattern: String)` returns `Primitive` - the first
match of `pattern` in `input`. NullData if either input or pattern is null or
there is no match.

#### Arguments
**input**: `String` - string input to match.

**pattern**: `String` - string regex pattern.

#### Description

Returns the substring in `input` that first matches `pattern`, or null if
there's no match for `pattern` in `input`.

### extractRegex

`extractRegex(input: String, pattern: String, formatter: Closure)` returns
`Array` - An array where each element is the result of calling formatter on each
match.

#### Arguments
**input**: `String` - The input string to match against.

**pattern**: `String` - The regex pattern to match.

**formatter**: `Closure` - A closure with free variable $, where $ is a
container with a field for each group, as in $.0 containing the whole match
(group 0), $.1 containing group 1 (if it exists in the pattern), etc.

#### Description

Finds all matches of the given regex pattern in the given input, and passes each
match to the given formatter. Returns a result of the formatters in an array.

The parameter passed to formatter - $ - contains each group from the regex as a
field. That is, group 0 (the whole match) is $.0, group 1 is $.1, etc. Groups
are not accessible by name.

Note: If the pattern is an empty string, this method will always return null
(i.e. an empty array).

Note: Only the last match of a repeated group will be returned.

Example:

```
var pattern: "a(b+)(c+)d"
var input: "abbcccd______abbbbccccccd"

extractRegex(input, pattern, $.1) == ["bb", "bbbb"]
extractRegex(input, pattern, $.2) == ["ccc", "cccccc"]
extractRegex(input, pattern, {
  whole: $.0
  bees: $.1
  cees: $.2
}) == [
  {
    bees: "bb",
    cees: "ccc",
    whole: "abbcccd"
  },
  {
    bees: "bbbb",
    cees: "cccccc",
    whole: "abbbbccccccd"
  }
]
```

### fail
`fail(message: String)` returns `NullData`

#### Arguments
**message**: `String`

#### Description

Throws an exception with the given message. It will be caught by
Errors#withError if present.

### fields
`fields(container: Container)` returns `Array`

#### Arguments
**container**: `Container`

#### Description

Returns an array of the fields in the given container. The fields are sorted
alphabetically.

### fileExists
`fileExists(path: String)` returns `Primitive`

#### Arguments

**path**: `String` - The path to the resource. Relative paths are resolved
relative to the current file. This path can use the same loaders/schemes as
imports (e.x. file:///hello/world or gs://my-bucket/hello/world if a GCP plugin
is imported).

#### Description

Returns true iff the file at the given path exists (regardless of whether it is
empty).

### fileName
`fileName(path: String)` returns `Primitive`

#### Arguments
**path**: `String`

#### Description

Returns the file/dir name specified by the given path (including extension).
This simply returns the last path segment after the last /. If there are query
parameters or other suffixes after this segment, they are included.

For example:

```
fileName("hello") == "hello"
fileName("/hello") == "hello"
fileName("/hello/") == fileName("") == ""
fileName("/hello/world/this/is/a/path.json.zip.tar.gz") == "path.json.zip.tar.gz"
```

### floor
`floor(data: Data)` returns `Primitive`

#### Arguments
**data**: `Data` - a `Data` data type

#### Description

Returns the largest `Primitive` `number` integer value that is less than or
equal to the argument.

#### Throws

*   **`IllegalArgumentException`** - if the argument isn't a `number`

### formatDateTime

`formatDateTime(format: String, iso8601DateTime: String)` returns `Primitive` -
Primitive string holding a representation of the provided timestamp formatted
according to the given format; NullData.instance if input datetime is not valid
as ISO 8601.

#### Arguments

**format**: `String` - String indicating the destination format of the given
timestamp.

**iso8601DateTime**: `String` - a timestamp in ISO 8601 format.

#### Description

Parses the given timestamp (which must be in ISO 8601 -
https://www.w3.org/TR/NOTE-datetime, yyyy-MM-dd'T'HH:mm:ss.SSSZ format), and
reformats it according to the given format (forcing a UTC timezone). The given
format must be specified according to Java formatting rules:
https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html

### formatDateTimeZ

`formatDateTimeZ(format: String, timezone: String, iso8601DateTime: String)`
returns `Primitive` - Primitive string holding a representation of the provided
timestamp formatted according to the given format; NullData.instance if input
datetime is not valid as ISO 8601.

#### Arguments

**format**: `String` - String indicating the destination format of the given
timestamp.

**timezone**: `String` - String describing a timezone either by id like
"America/Toronto" or by offset like "+08:00".

**iso8601DateTime**: `String` - a timestamp in ISO 8601 format.

#### Description

Parses the given timestamp (which must be in ISO 8601 -
https://www.w3.org/TR/NOTE-datetime, yyyy-MM-dd'T'HH:mm:ss.SSSZ format), and
reformats it according to the given format with the given timezone. The given
format must be specified according to Java formatting rules:
https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html

### get
`get(source: Data, path: String)` returns `Data`

#### Arguments
**source**: `Data`

**path**: `String`

#### Description
Gets the value at the given path applied to the given value.

### getEpochMillis

`getEpochMillis(iso8601DateTime: String)` returns `Primitive` - Primitive double
of the milliseconds since the Unix epoch for the provided datetime or NullData
if parse of timestamp using the ISO 8601 format fails.

#### Arguments

**iso8601DateTime**: `String` - a timestamp in ISO 8601 format
(yyyy-MM-dd'T'HH:mm:ss.SSSZ).

#### Description

Gets the milliseconds from the Java epoch of 1970-01-01T00:00:00Z for the
provided datetime, which must be in ISO 8601
(https://www.w3.org/TR/NOTE-datetime) format.

### groupBy
`groupBy(array: Array, keyExtractor: Closure)` returns `Array`

#### Arguments
**array**: `Array`

**keyExtractor**: `Closure`

#### Description

Groups the elements of an array by their keys extracted by the `keyExtractor`
closure. The output is an array of objects/containers, each with two fields
"key" and "value", analogous to key:value pairs. The "key" field of each
container in the return value is a unique join key in the original collection
computed by the `keyExtractor` closure. The "value" field will be a collection
of elements from the original collection that maps to the join key in the "key"
field. Example:

```
var array: [{num: 1; word: "one";}, {num: 2; word: "two";},
             {num: 3; word: "three";}, {num: 4; word: "four";}]
 var groupByResult: array[groupBy if $.num > 2 then "biggerThan2" else $.num + 2]
 // groupByResult == [{key: "biggerThan2"; elements: [{num: 3; word: "three";},
 //                                                   {num: 4; word: "four";}];},
 //                   {key: 3; elements: [{num: 1; word: "one";}];},
 //                   {key: 4; elements: [{num: 2; word: "two";}];}
 //                   ]
```

### gt

`gt(left: Primitive, right: Primitive)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**left**: `Primitive` - number to compare

**right**: `Primitive` - the second number to compare

#### Description

Returns `true` if the value of `left` is greater than the value of `right`.
`null` values are treated like the `number` `0`.

```
// Returns true
 gt(10, 1)

 // Returns true. The > operator is shorthand for the gt function.
 5 > 1

 // Returns false
 gt(1, 10)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more values isn't a `number`

### gtEq

`gtEq(left: Primitive, right: Primitive)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**left**: `Primitive` - the first number to compare

**right**: `Primitive` - the second number to compare

#### Description

Returns `true` if the value of `left` is greater than or equal to the value of
`right`. `null` values are treated like the `number` 0.

```
// Returns true
gtEq(10, 1)

// Returns true. The >= operator is shorthand for the gtEq function.
5 >= 5

Returns false
gtEq(1, 10)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more values isn't a `number`

### hash

`hash(obj: Data)` returns `Primitive` - Primitive String hash code representing
the input Data object.

#### Arguments
**obj**: `Data` - Data object to generate hash code for.

#### Description

Generates a Primitive String hash from the given Data object. Key order is not
considered for Containers, (Array item order is). This is not cryptographically
secure and is not to be used for secure hashing. Uses murmur3 hashing for speed
and stability.

### intHash

`intHash(obj: Data)` returns `Primitive` - Primitive holding Integer hash code
representing the input Data object.

#### Arguments
**obj**: `Data` - Data object to generate hash code for.

#### Description

Generates an Integer hash from the given Data object. Key order is not
considered for Containers, (Array item order is). This is not cryptographically
secure and is not to be used for secure hashing. Uses murmur3 hashing for speed
and stability.

### is
`is(data: Data, type: String)` returns `Primitive`

#### Arguments
**data**: `Data`

**type**: `String`

#### Description
Returns true if the given data is of the given type (according to #types).

Possible basic types are: Array, Container, Primitive, Dataset, and null.
Implementations, like DefaultArray can vary depending on the execution
environment.

This check is not sensitive to letter casing.

### isDateTimeBetween

`isDateTimeBetween(iso8601StartDateTime: String, iso8601EndDateTime: String,
iso8601CompareDateTime: String)` returns `Primitive` - Primitive boolean value
true if inside window, false otherwise.

#### Arguments

**iso8601StartDateTime**: `String` - timestamp in ISO 8601
format(yyyy-MM-dd'T'HH:mm:ss.SSSZ)

**iso8601EndDateTime**: `String` - timestamp in ISO 8601 format
(yyyy-MM-dd'T'HH:mm:ss.SSSZ)

**iso8601CompareDateTime**: `String` - timestamp in ISO 8601 format
(yyyy-MM-dd'T'HH:mm:ss.SSSZ)

#### Description

Calculates whether the value specified by iso8601CompareDateTime exists within
the window defined by iso8601StartDateTime and iso8601EndDateTime. All ISO 8601
timestamps are converted to millisecond values and a comparison is done
inclusive of the start and end points.

#### Throws

*   **`IllegalArgumentException`** - if any of ISO 8601 formatted input values
    are improperly formatted.

### isNil
`isNil(data: Data)` returns `Primitive` - Primitive `boolean`

#### Arguments
**data**: `Data` - a `Data` data type

#### Description

Returns `true` if the value of `Data` is null or empty, depending on the `Data`
implementation.

```
// Returns true because dir is empty.
var dir: ""
isNil(dir)

// Returns false because x isn't empty. The !? operator is shorthand for the isNil function.
var x: "abc"
!x?

// Returns true because emptyContainer is empty.
var emptyContainer: {}
isNil(emptyContainer)
```

### isNotNil
`isNotNil(data: Data)` returns `Primitive` - Primitive `boolean`

#### Arguments
**data**: `Data` - a `Data` data type

#### Description

Returns `true` if the value of `Data` isn't null or empty, depending on the
`Data` implementation.

```
// Returns true because x isn't empty.
var x: "abc"
isNotNil(x)

// Returns true because dir isn't empty. dir becomes "path/".
// The ? operator is shorthand for the isNotNil function.
var dir: "path"
if dir? then {
  var dir: dir + "/";
}

// Returns false because dataContainer is empty.
var dataContainer: {}
isNotNil(dataContainer)
```

### iterate
`iterate(closure: Closure, iterables: NullData...)` returns `NullData`

#### Arguments
**closure**: `Closure`

**iterables**: `NullData...`

#### Description

Iteration stub for iterating over NullData, to disambiguate it from a dataset or
an array. This method always returns NullData and never calls the given closure.

### iterate
`iterate(closure: Closure, iterables: Array...)` returns `Array`

#### Arguments
**closure**: `Closure` - The closure to use for iteration.

**iterables**: `Array...` - The arrays to iterate.

#### Description

Iterate the given Array(s) together passing them one element at a time to the
given closure, and composing the results into a new array. The closure must
therefore have the same number of free arguments as there are arrays to iterate
together.

If multiple arrays are iterated together, then zipped iteration is performed.
This means that for every index, an element is selected at that index from each
iterated array (except empty arrays, where null is selected for every index).
These elements are passed in as arguments to the function (in place of the
arrays), and the results of the function are composed to the returned new Array.
All non-empty arrays must have the same sizes (empty Arrays just yield as many
nulls as necessary).

For example, if (a, b, c, x, y) are args, a and b are arrays of size 3, c is an
array of size 0 (empty) and x and y are non-iterated args (that is, they are not
free args), then the given function will be called like:

*   fn(a[0], b[0], null, x, y)
*   fn(a[1], b[1], null, x, y)
*   fn(a[2], b[2], null, x, y)

The results of these calls will be collected into an array of length 3.

This method may also be called directly, with an anonymous expression. For
example: `iterate($1 + $2, ["a", "b", "c"], ["A", "B", "C"])` will return
`["aA", "bB", "cC"]`. Alternatively, `iterate($ + 100, [1, 2, 3])` will return
`[101, 102, 103]`. As many arguments as desired may be passed in, and will be
bound to $1, $2, $3...$n in order. If there is only one array argument, it is
bound to just $ (with no number).

### iterate
`iterate(closure: Closure, iterables: Container...)` returns `Container`

#### Arguments
**closure**: `Closure` - The closure to use for iteration.

**iterables**: `Container...` - The containers to iterate.

#### Description

Containers are iterated by passing in the values of each key. The resulting
value is then assigned to the corresponding key in the output container.

The main notable difference between arrays and containers is that there is no
size requirement for containers - since the iteration is done upon key-values,
containers that are missing the key just get a Null value.

For example, if (a, b, c, x, y) are args, a, b, and c are containers such that
a = {k1: ..., k3: ...} and b = {k1: ..., k2: ...} and c = {}, then the given
function will be called like:

*   fn(a.k1, b.k1, c.k1, x, y)
*   fn(a.k2, b.k2, c.k2, x, y)
*   fn(a.k3, b.k3, c.k3, x, y)

where a.k2 == b.k3 == c.k1 == c.k2 == c.k3 == null

The results of these calls will be collected into a container as in:

```
{
   k1: fn(a.k1, b.k1, c.k1, x, y)
   k2: fn(a.k2, b.k2, c.k2, x, y)
   k3: fn(a.k3, b.k3, c.k3, x, y)
}
```
Example:

```
var c1: {
  k1: "c1k1"
  k2: "c1k2"
}

def modify(value) value + "-modified"

modify(c1[]) == {
  "k1": "c1k1-modified",
  "k2": "c1k2-modified"
}

var c2: {
  k1: "c2k1"
  k3: "c2k3"
}

var c3: {
  k1: "c3k1"
  k3: "c3k3"
}

sum(c1[], c2[], c3[]) == {
  "k1": "c1k1c2k1c3k1",
  "k2": "c1k2",
  "k3": "c2k3c3k3"
}
```

### iterate
`iterate(closure: Closure, dataset: Dataset)` returns `Dataset`

#### Arguments
**closure**: `Closure` - The closure to use for iteration.

**dataset**: `Dataset` - The dataset to iterate/map.

#### Description

Iterates a dataset through the given closure. Each dataset element will be
passed through the given closure using the dataset's Dataset#map(RuntimeContext,
Closure, boolean) implementation.

### join

`join(left: Array, right: Array, joinOp: Closure)` returns `Array` - Array of
joined elements. Joined pairs are themselves arrays, with matching elements from
left and right, or nulls in place of no matches.

#### Arguments
**left**: `Array` - The left array

**right**: `Array` - The right array

**joinOp**: `Closure` - a predicate operating on $left and $right, which returns
true if the two elements shall be joined together

#### Description
Performs a full outer join on the two given arrays.

Example:

```
var array1: [{
  id: 1
  val: "1aaa"
}, {
  id: 2
  val: "1bbb"
}]
var array2: [{
  id: 1
  val: "2aaa"
}, {
  id: 3
  val: "2ccc"
}]

join(array1, array2, $left.id == $right.id) == [
 [{ id: 1; val: "1aaa"; }, { id: 1; val: "2aaa"; }],
 [{ id: 2; val: "1bbb"; }, {}],
 [{}, { id: 3; val: "2ccc"; }]
]
```
**Ordering:** Order is preserved such that:

*   Items from left (and corresponding matches from right, if any) appear first
*   Unmatched remaining items from right appear last

**Duplicate items:** Duplicate items are only matched once. That is, if left is
`Al Al Bl`, and right is `Ar Br Br Cr`, the result is

```
Al - Ar
Al - null
Bl - Br
null - Br
null - Cr
```

### joinPath
`joinPath(first: String, rest: String...)` returns `Primitive`

#### Arguments
**first**: `String` - The first element of the path.

**rest**: `String...` - The remaining elements of the path.

#### Description

Joins the given path segments, then returns the normalized path. Normalized
means all redundant relative segments such as non-leading `./` and `../` are
resolved away.

Example:

```
joinPath("./one/../two", "three") == "./two/three"
joinPath("../one/../two", "three", "four") == "../two/three/four"
joinPath("/one/two", "../three") == "/one/three"
joinPath("one/two", "three") == "one/two/three"
joinPath("hello:///bucket/one/two", "../three", "four") == "hello:///bucket/one/three/four"
```

### last
`last(array: Array)` returns `Data`

#### Arguments
**array**: `Array`

#### Description
Returns the lastIndex data Data in a given Array or Null data for an empty array

### listFiles
`listFiles(pattern: String)` returns `Array`

#### Arguments

**pattern**: `String` - A file pattern to match against, in glob form. Currently
(b/230104706), only local files are supported. For more info on glob syntax, see
[getPathMatcher("glob")](https://docs.oracle.com/javase/9/docs/api/java/nio/file/FileSystem.html#getPathMatcher-java.lang.String-)

#### Description

Lists all files matching the given pattern. Relative paths are resolved against
the current file.

Example:

```
Given directories:
/
├── current.wstl <------ we are in this file.
├── aaa.json
├── one
│     ├── bar
│     │     ├── fff.json
│     │     └── ggg.json
│     ├── bbb.zzz.json
│     └── foo
│         ├── ccc.json
│         ├── ddd.json
│         └── eee.zzz.json
└── two
    ├── hhh.zzz.json
    └── iii.json

listFiles("./*.json") == ["aaa.json"]
listFiles("./**/*.json") == ["/one/bar/ggg.json", "/one/foo/ccc.json",
          "/one/foo/eee.zzz.json", "/two/iii.json", "/one/bar/fff.json", "/one/bbb.zzz.json",
          "/one/foo/ddd.json", "/two/hhh.zzz.json"]
listFiles("/**/*.zz?.json") == ["./one/bbb.zzz.json", "./one/foo/eee.zzz.json",
                                 "./two/hhh.zzz.json"]
```

### listLen
`listLen(array: Array)` returns `Primitive`

#### Arguments
**array**: `Array`

#### Description
Returns the length of the given Array.

### loadJson
`loadJson(path: String)` returns `Data`

#### Arguments

**path**: `String` - The path to the resource. Relative paths are resolved
relative to the current file. This path can use the same loaders/schemes as
imports (e.x. file:///hello/world or gs://my-bucket/hello/world if a GCP plugin
is imported).

#### Description
Loads the json data at the given path, and returns it as a Data.

### loadText
`loadText(path: String)` returns `Primitive`

#### Arguments

**path**: `String` - The path to the resource. Relative paths are resolved
relative to the current file. This path can use the same loaders/schemes as
imports (e.x. file:///hello/world or gs://my-bucket/hello/world if a GCP plugin
is imported).

#### Description

Loads the UTF-8 text data at the given path, and returns it as a string
primitive.

### lt

`lt(left: Primitive, right: Primitive)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**left**: `Primitive` - the first number to compare

**right**: `Primitive` - the second number to compare

#### Description

Returns `true` if the value of `left` is less than the value of `right`. `null`
values are treated like the `number` 0.

```
// Returns true
 lt(1, 10)

 // Returns true. The < operator is shorthand for the lt function.
 1 < 2

 // Returns false
 lt(2, 1)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more values isn't a `number`

### ltEq

`ltEq(left: Primitive, right: Primitive)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**left**: `Primitive` - the first number to compare

**right**: `Primitive` - the second number to compare

#### Description

Returns `true` if the value of `left` is less than or equal to the value of
`right`. `null` values are treated like the `number` 0.

```
// Returns true
ltEq(5, 5)

// Returns true. The <= operator is shorthand for the ltEq function.
1 <= 10

// Returns false
ltEq(10, 1)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more values isn't a `number`

### matchesRegex

`matchesRegex(str: String, regex: String)` returns `Primitive` - Primitive
boolean, true iff input string matches input regex pattern

#### Arguments
**str**: `String` - Primitive string input to match.

**regex**: `String` - Primitive string regex pattern.

#### Description

Returns true iff the Primitive string matches the Primitive string regex
pattern.

### mul

`mul(first: Primitive, rest: Primitive...)` returns `Primitive` - Primitive
`number`

#### Arguments
**first**: `Primitive` - the first number to multiply

**rest**: `Primitive...` - the remaining numbers to multiply

#### Description

Returns a `Primitive` `number` representing the product of the arguments. `null`
arguments are treated like the `number` 0.

```
// Returns 100
mul(5, 10, 2)

// Returns 50. The * operator is shorthand for the mul function.
5 * 10
```

#### Throws

*   **`IllegalArgumentException`** - if one or more arguments isn't a `number`

### neq

`neq(first: Data, second: Data, rest: Data...)` returns `Primitive` - Primitive
`boolean`

#### Arguments
**first**: `Data` - the first value to compare

**second**: `Data` - the second value to compare

**rest**: `Data...` - the remaining values to compare

#### Description

Returns a `Primitive` `boolean` with the value `true` if any two arguments are
unequal.

```
// Returns true
neq("text1", "text2")

// Returns true. The != operator is shorthand for the neq function.
false != 7

// Returns false
neq("text1", "text1")
```

### not
`not(data: Data)` returns `Primitive` - Primitive `boolean`

#### Arguments
**data**: `Data` - a `Data` data type

#### Description

Returns `true` if `data` represents a `Primitive` `boolean` with a falsey value.
Returns `true` if `data` is null or empty.

```
// Returns true
var a: false
not(a)

// Returns false. The ! operator is shorthand for the not function.
var a: true
!a

// Returns true
var b: ""
not(b)
```

### or
`or(args: Closure...)` returns `Primitive` - Primitive `boolean`

#### Arguments

**args**: `Closure...` - arguments to evaluate. A `Closure` contains a lazily
evaluated expression which will be evaluated by the function if and when
appropriate.

#### Description

Returns `true` if any arguments are truthy. Evaluation stops after encountering
a truthy argument.

```
// Returns true
var a: "mystring"
var b: "yourstring"
(a == "mystring" or b == "yourstring")

// Returns false
var c: "astring"
var d: "astring"
(b == "string" or c == "string")

// Returns true. Evaluation continues after the first conditional even though it's false, then
// evaluation stops after encountering y == 2, which is true, and doesn't evaluate z == 3.
var x: 1
var y: 2
var z: 3
(a == 2 or y == 2 or z == 3)
```

### parseDateTime

`parseDateTime(format: String, datetime: String)` returns `Primitive` -
Primitive string holding an ISO 8601 representation of the provided timestamp;
NullData.instance if parse of timestamp using provided format fails.

#### Arguments
**format**: `String` - format String for parsing the provided timestamp.

**datetime**: `String` - timestamp String to be parsed.

#### Description

Parses String timestamp into the ISO 8601 "Complete date plus hours, minutes,
seconds and a decimal fraction of a second" format, in the UTC timezone
(https://www.w3.org/TR/NOTE-datetime, yyyy-MM-dd'T'HH:mm:ss.SSSZ) from the
format specified according to Java formatting rules:
https://www.joda.org/joda-time/apidocs/org/joda/time/format/DateTimeFormat.html

Missing components are defaulted, except literals which are non-optional.

### parseEpochMillis

`parseEpochMillis(input: Long)` returns `Primitive` - Primitive String of a
timestamp in ISO 8601 format (yyyy-MM-dd'T'HH:mm:ss.SSSZ) or NullData if parse
fails.

#### Arguments
**input**: `Long` - the number of milliseconds from 1970-01-01T00:00:00Z.

#### Description

Gets the Java epoch of 1970-01-01T00:00:00Z from the milliseconds for the
provided datetime.

### parseNum
`parseNum(str: String)` returns `Primitive`

#### Arguments
**str**: `String`

#### Description
Parses String and returns Primitive double.

### range
`range(start: Primitive, end: Primitive)` returns `Array`

#### Arguments
**start**: `Primitive`

**end**: `Primitive`

#### Description

Returns an array of sequentially ordered integers from start (inclusive) to end
(exclusive) by an increment step of 1. Example:

```
var x : range(5, 10) // x: [5, 6, 7, 8, 9]
```

### range
`range(size: Primitive)` returns `Array`

#### Arguments
**size**: `Primitive`

#### Description

Returns an array of sequentially ordered integers with provided size starting
from 0 and incremented in steps of 1. Example:

```
var x : range(5) // x: [0, 1, 2, 3, 4]
```

### reduce

`reduce(array: Array, seed: Data, accumulator: Closure)` returns `Data` - Data
representing the result of the reduction.

#### Arguments
**array**: `Array` - Array to reduce.

**seed**: `Data` - Initial value of $acc.

**accumulator**: `Closure` - Closure to use as the accumulation function. `$acc`
represents the cumulative reduced value so far, and `$cur` represents the
current value to accumulate. The result of this closure is the next `$acc`.

#### Description

Performs a reduction on the elements of an Array using an associative
accumulation Closure. If the array is empty, the given seed is returned.

Example:

```
reduce([1, 2, 3], 10, $acc + $cur) == 16
reduce([1], 10, $acc + $cur) == 11
reduce([1], 10, "some const") == "some const"
```

### reduce

`reduce(array: Array, accumulator: Closure)` returns `Data` - Data representing
the result of the reduction.

#### Arguments
**array**: `Array` - Array to reduce.

**accumulator**: `Closure` - Closure to use as the accumulation function. `$acc`
represents the cumulative reduced value so far, and `$cur` represents the
current value to accumulate. The result of this closure is the next `$acc`.

#### Description

Performs a reduction on the elements of an Array using an associative
accumulation Closure. If the array is empty, null data is returned. If the array
has only one item, that item is returned.

Example:

```
reduce([1, 2, 3], $acc + $cur) == 6
reduce([1], $acc + $cur) == 1
reduce([1], "some const") == 1
```

### rethrowError

`rethrowError(body: Closure, errorHandler: Closure)` returns `Data` - the result
value of either the body, or the error handler if it was called.

#### Arguments
**body**: `Closure` - The code to handle errors from.

**errorHandler**: `Closure` - The code to handle errors with.

#### Description

rethrowError calls #withError to execute the code given in `body`. The behavior
is the same as withError, but if there is an exception, it is rethrown after
handling. An error rethrown by this method will not be handled again when caught
by any other withError or rethrowError handlers.

### serializeJson

`serializeJson(data: Data)` returns `Primitive` - A string representing the JSON
serialization of the input data.

#### Arguments
**data**: `Data` - The data to serialize.

#### Description
Serializes the Data input to JSON string.

### sleep

`sleep(millis: Long)` returns `Data` link `NullData`

#### Arguments

**millis**: `Long` - number of milliseconds to sleep

#### Description

Sleep for the specified number of milliseconds.

### sortBy
`sortBy(array: Array, keySelector: Closure)` returns `Array` - sorted Array.

#### Arguments
**array**: `Array` - Array to sort.

**keySelector**: `Closure` - Closure to use for extracting sortBy key.

#### Description
Sorts an Array using the key specified by the provided Closure.

### sortByDescending

`sortByDescending(array: Array, keySelector: Closure)` returns `Array` - Array
sorted in descending order.

#### Arguments
**array**: `Array` - Array to sort.

**keySelector**: `Closure` - Closure to use for extracting sort-by key.

#### Description

Sorts an Array in descending order using the key specified by the provided
Closure.

### split

`split(str: String, delimiter: String)` returns `Array` - Array of substrings of
the input String which are partitioned by the provided delimiter.

#### Arguments
**str**: `String` - String to split.

**delimiter**: `String` - String to use to split the input string.

#### Description

Splits a string using the provided delimiter. Does not trim empty strings or
trailing whitespace characters. When the delimiter is null or empty, returns an
array of individual characters.

### split

`split(str: String)` returns `Array` - Array of individual characters in the
input string.

#### Arguments
**str**: `String` - string to split.

#### Description
Splits the String `str` into individual characters.

### strFmt
`strFmt(format: String, args: Data...)` returns `Primitive`

#### Arguments

**format**: `String` - A format string, using String#format(String, Object...)
conventions.

**args**: `Data...` - Arguments to fill into the placeholders.

#### Description
Formats a string using the given format and arguments.

### strJoin

`strJoin(delimiter: String, components: Array)` returns `Primitive` - the join
result as a string Primitive.

#### Arguments
**delimiter**: `String` - String to use to join the given array

**components**: `Array` - Array of data to join.

#### Description

Joins the given `Array` using the delimiter. The array is cast to its string
expression.

### sub

`sub(first: Primitive, rest: Primitive...)` returns `Primitive` - Primitive
`number`

#### Arguments
**first**: `Primitive` - the first number

**rest**: `Primitive...` - the remaining numbers to subtract from `first`

#### Description

Returns a `Primitive` `number` representing the difference of the arguments.
`null` arguments are treated like the `number` `0`.

```
// Returns 1
sub(10, 5, 4)

// Returns 2. The - operator is shorthand for the sub function.
3 - 1

// Returns 6. The null value is treated like the number 0
sub(10, "", 4)
```

#### Throws

*   **`IllegalArgumentException`** - if one or more arguments isn't a `number`

### sum

`sum(first: Primitive, rest: Primitive...)` returns `Primitive` - Primitive
representation of the sum

#### Arguments
**first**: `Primitive` - the first value to sum

**rest**: `Primitive...` - the remaining values to sum

#### Description

Returns a `Primitive` representing the sum of the arguments. `number` arguments
are added. `string` arguments are concatenated. If any argument is a `string`,
every argument is treated like a `string`. If arguments are only `number` and
`boolean` or only `boolean`, an IllegalArgumentException is thrown.

```
// Returns 10
sum(5, 5)

// Returns hello world
sum("hello ", "world")

// Returns 1 hello 2 world
// The + operator is shorthand for the sum function.
1 + " hello " + 2 + " world"

// If any argument is a string, every argument is treated like a string.
// Returns true1hello
sum(true, 1, "hello")
```

#### Throws

*   **`IllegalArgumentException`** - if the arguments are only `number` and
    `boolean` or only `boolean`

### timed

`timed(body: Closure, timeHandler: Closure)` returns `Data` - result of
executing `body`

#### Arguments
**body**: `Closure` - code to execute and time

**timeHandler**: `Closure` - code to handle the elapsed time result, in
milliseconds

#### Description

Executes the code given in `body`, while timing the execution. The return value
of this function is the return value of `body`, and the elapsed time is passed
as a `$time` parameter of type `Primitive` `number` to the code provided in
`timeHandler`. Note that the `timeHandler` won't be called if there's an
exception in `body`.

Example usage:

```
result: timed(functionToTime(1, 2), functionToHandleMetrics($time))
def functionToTime(arg1, arg2) {
  "Passed {arg1} and {arg2}."
}
def functionToHandleMetrics($time) {
  logging::logInfo(): "Timed function executed in {$time} milliseconds."
}

// Output:
result: "Passed 1 and 2."
// Log output:
INFO: Timed function executed in 9.0 milliseconds.
```

### toLower
`toLower(str: String)` returns `Primitive`

#### Arguments
**str**: `String`

#### Description
Converts all letters in the provided string to lower case.

### toUpper
`toUpper(str: String)` returns `Primitive`

#### Arguments
**str**: `String`

#### Description
Converts all letters in the provided string to upper case.

### tryParseNum
`tryParseNum(primitive: Primitive)` returns `Primitive`

#### Arguments
**primitive**: `Primitive`

#### Description

Tries to parse Primitive and returns Primitive double if primitive is String.
Returns itself otherwise.

### types
`types(data: Data)` returns `Array`

#### Arguments
**data**: `Data`

#### Description

Returns the type(s) of the given data. This will be an Array of strings. For
example, a Container may return `["Container", "DefaultContainer"]` and an empty
Array may return `["Array", "null", "DefaultArray"]`.

Possible basic types are: Array, Container, Primitive, Dataset, and null.
Implementations, like DefaultArray can vary depending on the execution
environment.

### unique
`unique(array: Array)` returns `Array` - a Data object with duplicates removed

#### Arguments
**array**: `Array` - an array of elements

#### Description

Remove duplicate data from the input Data. Elements are compared with
the #equals method. There is no guarantee as to which specific duplicates will
be removed. The order of the elements in the returned value will be preserved.

### uniqueBy

`uniqueBy(array: Array, keySelector: Closure)` returns `Array` - a Array with
duplicate elements removed.

#### Arguments
**array**: `Array` - Array to deduplicate.

**keySelector**: `Closure` - Closure for extracting key from array entries to
deduplicate over.

#### Description

Remove duplicates from the input Array. Elements are compared using the provided
'keySelector' Closure function. Existing values take precedence over later
values, such that repeated elements are resolved by removing the later-occurring
element in the Array. The order of the elements in the returned value will be
preserved.

### unset
`unset(container: Container, field: String)` returns `Container`

#### Arguments
**container**: `Container`

**field**: `String`

#### Description

Returns a `container` who is the same as the provided `container` but with the
`field` removed. Depending on the container implementation used in
RuntimeContext#getDataTypeImplementation() the returned container may or may not
be the same object as the input.

### uuid
`uuid()` returns `Primitive`

#### Arguments

#### Description
Returns a random uuid String.

### values
`values(container: Container)` returns `Array`

#### Arguments
**container**: `Container`

#### Description

Returns an array of the values in the given container. The order of values
corresponds to the fields being sorted alphabetically.

### where
`where(nullData: NullData, predicate: Closure)` returns `NullData`

#### Arguments
**nullData**: `NullData`

**predicate**: `Closure`

#### Description

Catch-all for where applied to null, to disambiguate null from Array and
Container.

### where
`where(array: Array, predicate: Closure)` returns `Array`

#### Arguments
**array**: `Array`

**predicate**: `Closure`

#### Description

Filters the given Array, returning a new one containing only items that match
the given predicate. Each array element is represented in the predicate by `$`.
Example:

```
var array: [-1, 2, -3, -4, 5, -6]
 var positiveArray: array[where $ > 0] // positiveArray: [2, 5]
```

### where
`where(container: Container, predicate: Closure)` returns `Container`

#### Arguments
**container**: `Container`

**predicate**: `Closure`

#### Description

Filters the given Container, returning a new one containing only items that
match the given predicate. For each field-value pair in the Container, they are
available in the predicate as `$.field` and `$.value`. If the predicate returns
false, the resulting container will not have this field-value pair present.
Example:

```
var container: {
   f1: 1; f2: 2; f3: 3; f4: 4;
 }
 var newContainer: container[where $.value < 2 || $.field == "f4"]
 // newContainer : {f1: 1; f4: 4;}
```

### withError

`withError(body: Closure, errorHandler: Closure)` returns `Data` - the result
value of either the body, or the error handler if it was called.

#### Arguments
**body**: `Closure` - The code to handle errors from.

**errorHandler**: `Closure` - The code to handle errors with.

#### Description

withError executes the code given in `body`. If any errors/exceptions occur, the
code in `errorHandler` is called, with `$error` representing information about
the error (structure below).

`$error` looks like:

```
{
    "cause": "Some string explaining what went wrong",
    "stack": [{ // Stack trace of where the error occurred. The top most stack frame is where
                // it occurred.
        "package": "Whistle or Java package name",
        "file": "file://the/original/file/path.wstl",
        "function": "myFunctionName",
        "line": 99 // Line in the file where the error occurs
       },
       {
        "package": "Whistle or Java package name",
        "file": "file://the/original/file/path.wstl",
        "function": "myOtherFunctionName",
        "line": 33 // The line where myFunctionName in the stack frame above is called.
       }, ...],
    "vars": {
      "x": ..., // value of x
      "y": ... // value of y
    }
  }
```

### withSides

`withSides(body: Closure)` returns `Data` - merged side and main outputs from
the given expression.

#### Arguments

**body**: `Closure` - the expression from which to capture and merge side
outputs.

#### Description

Executes the given expression, merging its output with any `side` outputs that
are written within (including by other functions/expressions called by this
one).

### withTimeout

`withTimeout(body: Closure, millis: Long, timeoutHandler: Closure)` returns
`Data` - result of executing `body`

#### Arguments

**body**: `Closure` - code to execute

**millis**: `Long` - number of milliseconds to wait

**timeoutHandler**: `Closure` - code to execute when timing out

#### Description

Executes the code given in `body`, timing out after the specified number of
milliseconds. If the execution finishes before the time limit is reached, the
return value of this function is the return value of `body`. If, on the other
hand, the time limit is reached, the execution will be aborted and the
`timeoutHandler` will be called.

## Targets
### Debug
`Debug(...TODO: Args need to be added to javadoc...): ...`

#### Arguments
**...TODO**: `Args need to be added to javadoc...` - ...

#### Description
TODO: This is missing documentation. It will be added soon.

### set
`set(var: optional String, field: String, mergeMode: optional String): ...`

#### Arguments

**var**: `optional String` - The variable to write to. Defaults to $this.

**field**: `String` - The path on the var to write. An empty string will write
directly to the var.

**mergeMode**: `optional String` - The mode to use for merging. Possible options
are:

*   replace - replace the given var's given field with the data.
*   merge - recursively merge the given var's given field with the data.
*   append - append the data as the last element in given var's given (array)
    field.
*   extend - extend the given var's given field with the data. For arrays this
    concatenates, for containers this adds only missing fields.

Note: Primitives will always be replaced, regardless of merge mode.

#### Description

Writes to the specified path on either the specified variable or to the output
of the enclosing block (if var is omitted).

For example:

```
result: setSomeVars(100)

def setSomeVars(num) {
  var my_var: 0 // Although the below statements will be able to set the vars without them
                // being declared here, we need these declarations to read them later,
                // or the transpiler will complain.
  var my_var2: 0

  set("my_var", ""): num // Sets directly to my_var, overwriting the 0 with num (100)
  set("hundred"): my_var // Reads the prior value of 100, and sets it on field "hundred"
                         // on the output of this block. Same as doing:
                         // hundred: my_var

  set("my_var2", "field"): num + 1 // my_var2 becomes a container, and sets field "field"
                                   // on it to 101
  set("my_var2", "field2.value"): num + 10 // Add a new field to my_var2 (alongside the
                                           // prior) and set the nested value to 110.
  set("$this", "nested"): my_var2 // Sets field "nested" on the output of this block.
}

// result is now {
//    hundred: 100
//    nested: {
//      field: 101
//      field2: {
//        value: 110
//      }
//    }
//}
```

### side
`side(path: String, mergeMode: optional String): ...`

#### Arguments

**path**: `String` - Path on the side output to write to. These are then merged
with the main output of withSides.

**mergeMode**: `optional String` - The mode to use for merging. Possible options
are:

*   replace - replace the given field with the data.
*   merge - recursively merge the given field with the data (default).
*   append - append the data as the last element in the given (array) field.
*   extend - extend the given field with the data. For arrays this concatenates,
    for containers this adds only missing fields.

Note: Primitives will always be replaced, regardless of merge mode.

#### Description
Writes to a side output (for using with Core#withSides.

For example:

```
// or equivalently side my_field:...
side("my_field"):... // will write to the my_field field of the side output which is then
                       // merged in withSides.

// A complete usecase:

result: withSides({
  one: 1
  addSomeSides(333)
  two: 2
})

// ... somewhere later on

def addSomeSides(num) {
  side sideNum.value: num
  // or equivalently side("sideNum.value"): num
}


// result above will be {
//  one: 1
//  two: 2
//  sideNum: {
//    value: 333
//  }
// }
// Do note that the field ordering here is for illustration, no ordering is guaranteed
// by the side target.
```
For more information see Core#withSides.
