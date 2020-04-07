# Data Harmonization Mapping Language (DHML) builtin functions

[TOC]

There are various built-in functions available. The parameters to these
functions are JSON types namely:

*   number: JSON number (both integer and decimal)
*   boolean: JSON boolean
*   string: JSON string
*   object: JSON object
*   array: JSON array
*   any: Any JSON object, array or primitive
*   `...` implies that a function takes a variable number of arguments

Function signatures are of the following format:

`FunctionName(argument1 argumentType, argument2 argumentType, ...) returnType`

## Arithmetic

### $Div

```go
$Div(left number, right number) number
```

Div divides the first argument by the second.

### $Mod

```go
$Mod(left number, right number) number
```

Mod returns the remainder of dividing the first argument by the second.

### $Mul

```go
$Mul(operands ...number) number
```

Mul multiplies together all given arguments. Returns 0 if nothing given.

### $Sub

```go
$Sub(left number, right number) number
```

Sub subtracts the second argument from the first.

### $Sum

```go
$Sum(operands ...number) number
```

Sum adds up all given values.

## Collections

### $Flatten

```go
$Flatten(arr array) array
```

Flatten turns a nested array of arrays (of any depth) into a single array. Item
ordering is preserved, depth first.

### $ListCat

```go
$ListCat(args ...array) array
```

ListCat concatenates all given arrays into one array.

### $ListLen

```go
$ListLen(in array) number
```

ListLen finds the length of the array.

### $ListOf

```go
$ListOf(args ...any) array
```

ListOf creates a list of the given tokens.

### $SortAndTakeTop

```go
$SortAndTakeTop(arr array, key string, desc boolean) object
```

SortAndTakeTop sorts the elements in the array by the key in the specified
direction and returns the top element.

### UnionBy

```go
$UnionBy(items array, keys ...string) array
```

UnionBy unions the items in the given array by the given keys, such that each
item in the resulting array has a unique combination of those keys. The first
unique element is picked when deduplicating. The items in the resulting array
are ordered deterministically (i.e unioning of array [x, y, z] and array [z, x,
x, y], both return [x, y, z]).

E.g: Arguments: items: `[{"id": 1}, {"id": 2}, {"id": 1, "foo": "hello"}]`,
keys: "id" Return: [{"id": 1}, {"id": 2}]

### Unique

```go
$Unique(arr array) array
```

Unique returns the unique elements in the array by comparing their hashes.

### UnnestArrays

```go
$UnnestArrays(c object) array
```

UnnestArrays takes a json object with nested arrays (e.g.: {"key1": [{}...],
"key2": {}}) and returns an unnested array that contains the top level key in
the "k" field and each array element, unnested, in the "v" field (e.g.: [{"k":
"key1", "v": {}} ...]). If the value of a key is an object, it simply returns
that object. The output is sorted by the keys, and the array ordering is
preserved. If the nested array is empty, the key is ignored.

E.g: c: `{"key1":[{"a": "z"}, {"b": "y"}], "key2": {"c": "x"}, "key3": []}
return: [{"k": "key1", "v":{"a": "z"}}`, {"k": "key1", "v":{"b": "y"}}, {"k":
"key2", "v":{"c": "x"}}]

## Date/Time

### $CurrentTime

```go
$CurrentTime(format string, tz string) string
```

CurrentTime returns the current time based on the Go func time.Now
(https://golang.org/pkg/time/#Now). The function accepts a time format layout
(https://golang.org/pkg/time/#Time.Format) and an IANA formatted time zone
string (https://www.iana.org/time-zones). A string representing the current time
is returned. A default layout of '2006-01-02 03:04:05'and a default time zone of
'UTC' will be used if not provided.

### $ParseTime

```go
$ParseTime(format string, date string) string
```

ParseTime uses a Go time-format to convert date into the RFC3339
(https://www.ietf.org/rfc/rfc3339.txt) format.

### $ParseUnixTime

```go
$ParseUnixTime(unit string, ts number, format string, tz string) string
```

ParseUnixTime parses a unit and a unix timestamp into the speficied format. The
function accepts a go time format layout
(https://golang.org/pkg/time/#Time.Format)

### $ReformatTime

```go
$ReformatTime(inFormat string, date string, outFormat string) string
```

ReformatTime uses a Go time-format (https://golang.org/pkg/time/#Time.Format) to
convert date into another Go time-formatted date time
(https://golang.org/pkg/time/#Time.Format).

### $SplitTime

```go
$SplitTime(format string, date string) array
```

SplitTime splits a time string into components based on the Go time-format
(https://golang.org/pkg/time/#Time.Format) provided. An array with all
components (year, month, day, hour, minute, second and nanosecond) will be
returned.

## Data operations

### $Hash

```go
$Hash(object any) string
```

Hash converts the given item into a hash. Key order is not considered (array
item order is). This is not cryptographically secure, and is not to be used for
secure hashing.

### $IsNil

```go
$IsNil(object any) boolean
```

IsNil returns true iff the given object is nil or empty.

### $IsNotNil

```go
$IsNotNil(object any) boolean
```

IsNotNil returns true iff the given object is not nil or empty.

### $MergeJSON

```go
$MergeJSON(arr array, overwriteArrays boolean) object
```

MergeJSON merges the elements in the array into one JSON object by repeatedly
calling the merge function. The merge function overwrites single fields and
concatenates array fields (unless overwriteArrays is true, in which case arrays
are overwritten).

### $UUID

```go
$UUID() string
```

UUID generates a RFC4122 (https://tools.ietf.org/html/rfc4122) UUID.

## Debugging

### $DebugString

```go
$DebugString(t any) string
```

DebugString converts the JSON element to a string representation by recursively
converting objects to strings.

### $Void

```go
$Void(unused ...any) object
```

Void returns nil given any inputs. You non-nil into the Void, the Void nils
back.

## Logic

### $And

```go
$And(args ...boolean) boolean
```

And is a logical AND of all given arguments.

### $Eq

```go
$Eq(args ...any) boolean
```

Eq returns true iff all given arguments are equal.

### $Gt

```go
$Gt(left number, right number) boolean
```

Gt returns true iff the first argument is greater than the second.

### $GtEq

```go
$GtEq(left number, right number) boolean
```

GtEq returns true iff the first argument is greater than or equal to the second.

### $Lt

```go
$Lt(left number, right number) boolean
```

Lt returns true iff the first argument is less than the second.

### $LtEq

```go
$LtEq(left number, right number) boolean
```

LtEq returns true iff the first argument is less than or equal to the second.

### $NEq

```go
$NEq(args ...any) boolean
```

NEq returns true iff all given arguments are different.

### $Not

```go
$Not(v boolean) boolean
```

Not returns true iff the given value is false.

### $Or

```go
$Or(args ...boolean) boolean
```

Or is a logical OR of all given arguments.

## Strings

### $ParseFloat

```go
$ParseFloat(str string) number
```

ParseFloat parses a string into a float.

### $ParseInt

```go
$ParseInt(str string) number
```

ParseInt parses a string into an int.

### $StrCat

```go
$StrCat(args ...any) string
```

StrCat joins the input strings with the separator.

### $StrFmt

```go
$StrFmt(format string, item any) string
```

StrFmt formats the given item using the given Go format specifier
(https://golang.org/pkg/fmt/).

### $StrJoin

```go
$StrJoin(sep string, args ...any) string
```

StrJoin joins the inputs together and adds the separator between them.
Non-string arguments are converted to strings before joining.

### $StrSplit

```go
$StrSplit(str string, sep string) array
```

StrSplit splits a string by the separator and ignores empty entries.

### $ToLower

```go
$ToLower(str string) string
```

ToLower converts the given string with all unicode characters mapped to their
lowercase.

### $ToUpper

```go
$ToUpper(str string) string
```

ToUpper converts the given string with all unicode characters mapped to their
uppercase.
