# Whistle Data Transformation Language builtin functions

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

### $MultiFormatParseTime

```go
$MultiFormatParseTime(format array, date string) string
```

MultiFormatParseTime converts the time in the specified formats to
[RFC3339](https://www.ietf.org/rfc/rfc3339.txt) format. It tries the formats in
order and returns an error if none of the formats match. The function accepts a
[Go time-format](https://golang.org/pkg/time/#Time.Format) or
[Python time-format](#Python_tokens).

### $ParseTime

```go
$ParseTime(format string, date string) string
```

ParseTime converts the time in the specified format to
[RFC3339](https://www.ietf.org/rfc/rfc3339.txt) format. The function accepts a
[Go time format](https://golang.org/pkg/time/#Time.Format) or
[Python time format](#Python_tokens)

### $ParseUnixTime

```go
$ParseUnixTime(unit string, ts number, format string, tz string) string
```

ParseUnixTime parses a unit and a unix timestamp into the speficied format. The
function accepts a [Go time-format](https://golang.org/pkg/time/#Time.Format)

### $ReformatTime {#Python_tokens}

```go
$ReformatTime(inFormat string, date string, outFormat string) string
```

ReformatTime uses a Go time-format or a Python time-format to convert date into
another Go time-formatted date time or Python time-format. The Go time-formats
are defined in https://golang.org/pkg/time/#Time.Format and the Python
time-formats are based
https://docs.python.org/3/library/time.html#time.strftime. The details of
supported Python date-time formatting tokens are listed in the following table.
Some additional tokens (the ones marked ADDED or Google Cloud SQL) are added to
accomodate the unpadded alternative for those fields.

| Token | Meaning                               | Corresponding GO time-format |
| :---: | ------------------------------------- | ---------------------------- |
| %A    | Python: Locale’s full weekday name    | Monday                       |
| %a    | Python: Locale’s abbreviated weekday  | Mon                          |
:       : name                                  :                              :
| %B    | Python: Locale’s full month name      | January                      |
| %b    | Python: Locale’s abbreviated month    | Jan                          |
:       : name                                  :                              :
| %c    | Python: Locale’s appropriate date and | Mon Jan 2 15:04:05 2006      |
:       : time representation                   :                              :
| %d    | Python: Day of the month as a decimal | 02                           |
:       : number [01,31]                        :                              :
| %e    | Google Cloud SQL: The day of month as | 2                            |
:       : a decimal number (1-31)               :                              :
| %H    | Python: Hour (24-hour clock) as a     | 15                           |
:       : decimal number [00,23]                :                              :
| %I    | Python: Hour (12-hour clock) as a     | 03                           |
:       : decimal number [01,12]                :                              :
| %i    | ADDED: 12H hour representation        | 3                            |
:       : without padding                       :                              :
| %M    | Python: Minute as a decimal number    | 04                           |
:       : [00,59]                               :                              :
| %m    | Python: Month as a decimal number     | 01                           |
:       : [01,12]                               :                              :
| %p    | Python: Locale’s equivalent of either | PM                           |
:       : AM or PM                              :                              :
| %S    | Python: Second as a decimal number    | 05                           |
:       : [00,60]                               :                              :
| %s    | ADDED: second as a decimal number     | 5                            |
:       : without padding [0,60]                :                              :
| %X    | Python: Locale’s appropriate time     | 15:04:05                     |
:       : representation                        :                              :
| %x    | Python: Locale’s appropriate date     | 01/02/06                     |
:       : representation                        :                              :
| %Y    | Python: Year with century as a        | 2006                         |
:       : decimal number                        :                              :
| %y    | Python: Year without century as a     | 06                           |
:       : decimal number [00,99]                :                              :
| %Z    | Python: Time zone name (no characters | MST                          |
:       : if no time zone exists)               :                              :
| %z    | Python: Time zone offset indicating a | -07:00                       |
:       : positive or negative time difference  :                              :
:       : from UTC/GMT                          :                              :

### $SplitTime

```go
$SplitTime(format string, date string) array
```

SplitTime splits a time string into components based on the
[Go time-format](https://golang.org/pkg/time/#Time.Format) and
[Python time-format](#Python_tokens) provided. An array with all components
(year, month, day, hour, minute, second and nanosecond) will be returned.

## Data operations

### $Hash

```go
$Hash(object any) string
```

Hash converts the given item into a hash. Key order is not considered (array
item order is). This is not cryptographically secure, and is not to be used for
secure hashing.

### $IntHash

```go
$IntHash(object any) number
```

IntHash converts the given item into an integer hash. Key order is not considered (array
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

### $Type

```go
$Type(object any) string
```

Type returns the specific type the given object as a string. Possible values
are: `"bool"`, `"number"`, `"string"`, `"array"`, `"container"`, `"null"`.

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

### $MatchesRegex

```go
$MatchesRegex(str string, regex string) boolean
```

MatchesRegex returns true iff the string matches the regex pattern.

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

### $SubStr

```go
$SubStr(input string, start number, end number) string
```

SubStr returns a part of the string that is between the start index (inclusive) and the end index (exclusive). If the end index is greater than the length of the string, the end index is truncated to the length.

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
