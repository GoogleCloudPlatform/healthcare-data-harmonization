# Language Spec and User Manual

## Ethos {#ethos}

The Whistle language is purpose-designed and built for transforming and
manipulating data. In order to achieve this, all decisions are made with respect
to the following principles:

1.  Whistle's primary purpose is to support data traversal and construction.
2.  Complexity of code is directly proportional to complexity of data.
3.  Every field should be written (mapped) as few times as possible, ideally
    exactly once.
4.  Feature decisions are based on use cases.

## Syntax and Semantics {#syntax-and-semantics}

### Lexical Elements {#lexical-elements}

#### Comments {#comments}

Comments can be placed on their own lines or at the end of any line. There are
no multi-line or inline comments. Examples:

```
// This is a comment
10 + 10 // This is a comment too
```

#### Identifiers {#identifiers}

Identifiers are names of functions, variables, fields, etc.

**Basic identifiers** are composed of alpha-numeric characters, dollar signs,
and underscores, and cannot start with a number. The exact pattern is
`[$a-zA-Z_][$a-zA-Z_0-9]*`. Examples:

```
var maryAndBob: 123
Hello
$this
$$
var _private123: 123
hax0rs_
```

**Quoted identifiers** allow the use of any UTF character in an identifier. They
are composed of any string surrounded by single quotes (internal single quotes
and backslashes must be escaped by backslash. The exact pattern is `' (\' |
~['])+ '` . Examples:

```
'this can be literally anything 🙂'
var 'this is a quote → \' ←': 123
var 'this is a backslash → \\ ←': 123
```

**Hybrid identifiers** allow inserting any non-alpha-numeric UTF characters into
basic identifiers, by escaping them with a backslash. Any character can be
inserted, including spaces (but not newlines).

```
var mary\ bob: 123
woopsie\+daisy\🙂
```

#### Constants and Literals {#constants-and-literals}

**Numbers** are represented by an optional minus sign followed by one or more
numbers, followed by an optional decimal point, followed by a decimal value if
the decimal point is present. The exact pattern is `-? [0-9]+ (. [0-9]+)?`.
Examples:

```
123
0.1
123.123
-7
-77.2123123434
```

Note that numbers must fit into a 64-bit floating point value.

Note that when converted into strings, numbers will lose their decimal if it is
considered "negligible". A negligible decimal is one less than or equal to the
[ULP](https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html#ulp-double-)
of the integral value.

**Basic strings** are quoted and consist of any UTF characters within the
quotes. Some characters must be escaped with backslashes, namely: double quotes,
curly braces, and backslashes. Examples:

```
"Hello World!"
"This is a 🙂 string"
"And then it printed \"Hello World!\", and we knew it was working"
"This is a backslash: \\, and this is a curly brace: \{, and this is a closing brace: \}"
```

**Interpolated strings** are the same as basic strings, but can contain
expressions in the middle that are evaluated and inserted. Expressions are
wrapped in curly braces. Examples:

```
"10 + 10 = {10 + 10}"

var result: 10 + 10
"The result was {result}"
"The function returned {someFunction()}"
```

Any expression can be placed within the braces. See below for more information
on expressions.

**Boolean literals** are expressed through the constants `true` and `false`.
Examples:

```
var truth: true
var lies: false
```

#### Keywords {#keywords}

This is the exhaustive list of all keywords in Whistle. Keywords are
case-sensitive. These keywords are not usable as Basic Identifiers (unless
specified), and must be [quoted](#identifiers) in single quotes if used as an
identifier anywhere:

```plaintext
if
then
else
true
false
def
var
side
root
global
import
option
as
required
package
merge // This keyword can be used in an identifier
append // This keyword can be used in an identifier
replace // This keyword can be used in an identifier
extend // This keyword can be used in an identifier
and
or
```

To ensure backwards compatibility any new keywords added henceforth shall be
either guarded by options, or be valid as identifiers.

## Data Types {#data-types}

Whistle contains 4 specific Data Types that can be used to represent data. It is
key to note that these types are specifications of behaviour (interfaces), and
thus a given implementation may exhibit (implement) more than one of type.

### Primitive {#primitive}

Primitive data types represent a number, string, or boolean. They are analogous
to
[JSON Numbers/Strings/Booleans](https://datatracker.ietf.org/doc/html/rfc8259#section-6).
Examples:

```
3.14
false
"Testing 123"
```

### Container {#container}

The container represents a mapping of UTF string fields to values. It is
analogous to
[JSON Objects](https://datatracker.ietf.org/doc/html/rfc8259#section-4). Each
unique field can contain exactly one mapping. Field ordering should not be
considered for purposes of equality or semantics, though some representations of
containers may choose to keep track of field order or sort fields somehow.

Accesses on non-existent fields return [Null](#null).

Examples:

```
{
   field1: 123
   field2: [1, 2, 3]
   field3: [{
       num: 321
   }]
}
```

### Array {#array}

An array is an ordered collection of data. It is analogous to
[JSON Arrays](https://datatracker.ietf.org/doc/html/rfc8259#section-5). Elements
within an array can be accessed through indices, wildcards, and selectors (see
[Path expressions](#path-expressions)). Arrays should be small enough to fit in
memory. Arrays can contain null values.

Accesses on non-existent indices (including beyond the size of the array, or
negative values) return [Null](#null).

Examples:

```
[1, 2, 3]

[{}, {}, 3, 4]

[] // Not a valid array expression but empty arrays are valid null values (see the section on Nulls below).

[[1, 2], [3, 4]]

[{
   num: 1
}, {
   num: 2
}]
```

### Datasets {#datasets}

A dataset is an unordered collection of data. It is similar to Arrays, except
that it does not support element access through a specific index (e.x. `[123]`).
Instead, datasets can only be accessed through Wildcards (see
[Path expressions](#path-expressions) below), or [iteration](#iteration) (See
Iteration below).

Datasets vary significantly by implementation, but their semantics enable them
to not have to fit in memory.

Datasets should not contain any Nulls.

There is no current implementation of Datasets in the default implementation of
Whistle.

### Null {#null}

Nulls are a special data structure in Whistle. Null is both a specific
implementation of Data and a possible state of other implementations.

Specifically, Whistle defines Null as a property on any Data that signifies that
it contains no meaningful value. Empty Containers and Empty Arrays are Null.

Whistle also contains an implementation of NullData. This implements all of
Container, Array, Primitive, and Dataset, and thus can be used as any of these
types. Any access or attempt to derive data from NullData will produce the same
NullData.

In theory, NullData should be interchangeable with empty containers and arrays.
In practice, there are specific situations where this doesn't happen correctly
(TODO: file bug).

## Expressions {#expressions}

Expressions in Whistle produce a value. There are various types of expressions.

### Literal value expressions {#literal-value-expressions}

Literal value expressions are just a literal value (see
[Constants and Literals](#constants-and-literals)). Examples:

```
10
true
"Hello!"
```

### Array expressions {#array-expressions}

Array expressions** **construct an array from the given elements. `[X, Y, …, Z]`
maps directly to `arrayOf(X, Y, …, Z)`.

Operands/elements in array expressions can themselves be any other expression,
including other array expressions.

Array expressions cannot be empty.

Examples:

```
[1, 2, 3]

["a", 1, true]

[[1, 2], [3, 4]

[{
   num: 1
}, {
   num: 2
}]
```

### Block expressions {#block-expressions}

Block expressions construct data from a series of Mappings. The resulting data
can be of any type, based on the [Mappings](#mappings) and
[merge behaviour](#field-merge-behaviour). Block expressions are most commonly
used to construct containers. Examples:

```
{} // null
{ 1; } // results in 1 (see Mapping behaviour)

{
   a: 1
   b: 2
}

{
   nested: {
      a: 1
   }
}
```

Every block expression defines an implicit local [Variable](#variables) called
<code>[$this](#exception-$this)</code>. Mappings in the block constitute
modifications to this variable. The result of the block expression is simply the
value of this variable. More details on this mechanism are described
[below](#exception-$this).

### Path expressions {#path-expressions}

Path expressions are paths upon a value (a variable). Assigning a value to a
variable is covered in more depth in Mappings below.

A path consists of a head, followed by 0 or more segments.

A path head is either the name of a variable, or a call to a function.

A path segment consists of one of:

A field name, like `.some_field`, where the field name follows identifier rules,
or `.123` where 123 is an integral number constant (this will access a field
called "123" and is useful for
[data structures where such fields are common](http://www.hl7.eu/HL7v2x/v251/std251/ch02a.html#Heading524)).

An array index, like `[123]` where the index is an integral number constant.

A wildcard, like `[*]`.

A selector, like `[someSelector &lt;expr>]`, which is covered in more detail in
the [Selector](#selectors) section below.

Paths are used to traverse deeply nested data structures (see above), where
fields navigate through Containers, and indices, wildcards, and selectors all
navigate through arrays. Examples:

```
var container: {
  array: [{
    num: 1
    nested: [1, 2, 3]
    nested2: [{x: 11;}, {x: 12;}, {x: 13;}]
  },
  {
    num: 2
    nested: [4, 5, 6]
    nested2: [{x: 14;}, {x: 15;}, {x: 16;}]
  }]
}

container // this is a valid expression that evaluates to the entire structure.
container.array[0] // { num: 1; nested: [...]; nested2: [...] }
container.array[1] // { num: 2; nested: [...]; nested2: [...] }
container.array[99] // nil
container.array[99].asd.woop[123] // nil
container.array[0].nested[2] // 3
container.array[1].nested2[0] // { x: 14 }
container.array[1].nested2[1].x // 15
```

Wildcards exhibit particular behaviour when applied to arrays - wildcards will
apply the remainder of the path after the wildcard to each element in the array
upon which the wildcard is applied, resulting in a new array. Multiple wildcards
in the same path will flatten the arrays N - 1 times, where N is the number of
wildcards. Examples:

```
var container: {
  array: [{
    num: 1
    nested: [1, 2, 3]
    nested2: [{x: 11;}, {x: 12;}, {x: 13;}]
  },
  {
    num: 2
    nested: [4, 5, 6]
    nested2: [{x: 14;}, {x: 15;}, {x: 16;}]
  }]
}

container.array[*].num // [1, 2]
container.array[*].nested // [[1, 2, 3], [4, 5, 6]]
container.array[*].nested[*] // [1, 2, 3, 4, 5, 6]
container.array[*].nested2[*].x // [11, 12, 13, 14, 15, 16]
container.array[1].nested2[*].x // [14, 15, 16]
container.array[*].nested2[1].x // [12, 15]
```

### Operator expressions {#operator-expressions}

Operator expressions apply an operator to another expression. Each operator maps
directly to a builtin function, and is syntactic sugar for calling that function
on its operands.

#### Infix operators {#infix-operators}

Infix operators are inserted between two operand expressions: `<expr> &lt;op>
&lt;expr>`. Examples:

```
10 + 10
10 + 10 + 10 + 10
1 - 3
true or false
"Hello " + "World!"
```

Operators are applied by precedence level (lowest first). This ensures that `1 +
1 * 10` evaluates as `(1 + (1 * 10))` rather than `(1 + 1) * 10`.

An exhaustive list of infix operators and the function they map to:

```
// Precedence 1

* → mul
/ → div

// Precedence 2

+ → sum
- → sub

// Precedence 3

== → eq
!= → neq
> → gt
>= → gtEq
< → lt
<= → ltEq

// Precedence 4

and → and
or → or
```

Note that some operators (namely `and` and `or`) will lazily evaluate their
operands and short-circuit if possible. This means that `true or 1/0` will
evaluate to true, without erroring due to `1/0`.

#### Prefix and Postfix operators {#prefix-and-postfix-operators}

Prefix and Postfix operators are unary operators prepended or appended to an
expression: `<prefix op>&lt;expr>` or `<expr>&lt;postfix op>`. Postfix operators
have lower precedence than prefix operators, which in turn have lower precedence
than infix operators. An exhaustive list of prefix and postfix operators, and
the function they map to:

```
// Prefix:
! → not

// Postfix:
? → isNotNil
```

Examples:

```
myVariable? // true iff myVariable is not nil
!true // false
!!true // true
!myVariable? // true iff myVariable is nil
```

### Conditional/Ternary expressions {#conditional-ternary-expressions}

The format for the ternary expression is `if &lt;condition expr> then &lt;result
if true expr> else &lt;result if false expr>`. `else` and the expression that
follows it are optional.

Ternary expressions return a value depending on a provided condition. If the
condition is truthy (see below), the `<result if true expr>` expression is
evaluated and the result returned. If the condition is not truthy, the value of
the `<result if false expr>` expression is evaluated and returned, or Null if
that expression was omitted.

Truthiness is defined as the value of a boolean (i.e. true or false), or
Null-ness for non-boolean values (i.e. Null == false, not Null == true).

The `<result if true expr>` and `<result if false expr>` are evaluated only if
the condition is true/false respectively. That is, if the condition is true
`<result if false expr>` will never be evaluated/executed.

If given a [Block expression](#block-expressions), ternary expressions can look
like `if` statements from some traditional languages:

```
if 10 + 10 == 20 then {
  …
} else {
  …
}
```

However, it is key to note that they are still just expressions, and thus
operate by evaluating to a specific value (the value of whichever body was
executed). This value is then treated according to the semantics of any other
value.

The ternary expression is transpiled to the `ternary` builtin:
`ternary(&lt;condition expr>, &lt;result if true expr>, &lt;result if false
expr>)`.

Examples:

```js
var math: if 1 + 1 == 2 then "ok" else "error"

var mode: if input.valid then 1 else fail() // fail() never called if input.valid is truthy

var box: if mode == 0 then {
    value: "zero"
} else if mode == 1 then { // See note below about else if
    value: "one"
} else if mode == 2 then {
    value: "two"
}

// box will look like { value: "..." }
```

Note that there is no `else if` construct per se, `if X then Y else if A then B
else M` in Whistle is just `if X then Y else (if A then B else M)`.

When assigning values to variables inside blocks, be aware of
[variable scoping rules](#variables).

### Function calls {#function-calls}

Function call expressions follow the format of `package::function(arg1, arg2,
argN)` or `package::function()` for a function with no arguments.

The `package::` can be omitted if the function is a
[builtin](#builtin-functions) or is in the same [package](#packages) as the
current file.

Function names are [identifiers](#identifiers).

Function definition, execution, and other behaviour are covered
[below](#functions).

## Mappings {#mappings}

Mappings allow construction of containers (fields), and intermediate states
(variables). Mappings are only valid within the context of
[Block expressions](#block-expressions). Without blocks (and therefore
implicitly mappings), Whistle is a purely functional paradigm (albeit a limited
one).

The general form of a mapping consists of a value source (an expression) and a
target (a destination for the value to be written to). The syntax is `target:
source` followed by either a semicolon or a new line (must be one or the other,
not both and not neither).

A mapping may omit the target entirely. In this case, the target is implied to
be the special variable `$this`, that is a mapping with no target implicitly
assumed to be a target of `var $this:`. This has some special behaviour defined
in the [Exception](#exception-$this) below.

The source of a mapping is any valid [expression](#expressions). There are 4
possible types of targets: Variables, Fields, Side/Root outputs, and Functions.

Side/Root outputs and Functions as targets are covered under the
[Functions](#functions) section.

### Variables {#variables}

Variables contain intermediate state and/or data. A variable is defined by
writing it for the first time, and is written with the syntax `var
&lt;identifier>: &lt;source>`. The identifier is the name of the variable, and
follows the [identifier](#identifiers) syntax.

Rather than just the identifier, the identifier followed by a
[path](#path-expressions) can be optionally specified when writing a variable:
`var &lt;identifier>&lt;path>: &lt;source>`. There are slight differences
between paths used in an expression and paths used in variable writes:

1.  Variable write paths cannot contain [wildcards](#bookmark=id.djn12tll5jvp)
2.  Variable write paths cannot contain [selectors](#selectors)
3.  Variable write paths can contain array appends `[]`(note that `[]`has a
    different meaning when not used in write path. See [iteration](#iteration).)

Variables can be read by just specifying the identifier.

#### Variable Merge Behaviour {#variable-merge-behaviour}

When writing to a variable without a path, the new value replaces the value in
the variable.

When writing to a variable with a path, the value is merged with any existing
value at that path, following [Merge Behaviour](#merge-behaviour).

The above behaviours can be precisely controlled/overridden using
[Merge Modes](#merge-modes).

##### Exception: $this {#exception-$this}

Every [block expression](#block-expressions) defines a local variable called
`$this`. This variable represents the return value of the block.

`$this` has one special behaviour in that it is not replaced, even by a write
without a path. A mapping without a path will still merge the value with the
existing value, according to the [Merge Behaviour](#merge-behaviour) (though the
merge behaviour does specify situations where a merge of A and B results in just
B - `$this` is not exempt and can be replaced this way).

Combined with the implicit target of `var $this:` this makes for more
predictable behaviours in block expressions that have mappings without targets.
It also shows how ternary expressions are still just expressions while
functioning as control flow. Examples:

```js
var number: { 1; } // This is actually { var $this: 1; }, so the value of $this is now 1,
                   // and the result of the block is 1 (rather than a container as would be
                   // expected).

var A: …
var B: …
var block: {
  if condition then {
    A
  } else {
    B
  }
}

// The above block uses the following rules:
// 1) if … is simply a ternary expression, and in this context has no target specified
// 2) A and B are variables, and inside their blocks have no target specified.
// This means the above block is really:

var block: {
  var $this: if condition then {
    var $this: A // $this of the 'then' block
  } else {
    var $this: B // $this of the 'else' block
  }
}
```

#### Scope {#scope}

Variables are scoped lexically. [Block expressions](#block-expressions) can
access (read/write) variables defined in outer blocks before them.

Variables are not
[hoisted](https://developer.mozilla.org/en-US/docs/Glossary/Hoisting), but since
writes and declarations are one and the same, and since all expressions evaluate
to a value (including [blocks](#block-expressions) and
[ternaries](#conditional-ternary-expressions)), hoisting is unproductive
anyways.

Examples:

```js
var simple: 123
var simple: simple + 321 // simple is now 444

var container: {
    value: 1
}

var container2.value: 1 // container2 == container
var container2.value2: 2 // container2 == { value1: 1; value2: 2; }
var container2: container // container2 == container (no path => replace)

var container3.one.two.three[10].four: 44 // All intermediate containers and arrays created.

var outer: 1234
var container4: { // All blocks can contain mappings
   var temp: "this is scoped to this inner block"
   var outer: outer + 4321 // Updates 'outer' from the outer block
   result: temp
}
// variable 'outer' is 5555 here
// variable 'temp' cannot be accessed here

var array: [1, 2, 3]
var array[]: 4 // appends 4 to array, so it is now [1, 2, 3, 4]
var array[].field.anotherArray[].num: 99
// array is now [1, 2, 3, 4, { field: { anotherArray: [{ num: 99; }]; }; }]
var array[4].field.anotherArray[].num: 999
// array is now [1, 2, 3, 4, { field: { anotherArray: [{ num: 99; }, { num: 999; }]; }; }]
```

### Fields {#fields}

Field mappings allow writing a value to the keys of a [Container](#container).
Fields are in most ways identical to variables, including the syntax which just
omits the `var` keyword: `<identifier>: &lt;source>` or `<identifier>&lt;path>:
&lt;source>`.

#### Field Merge Behaviour {#field-merge-behaviour}

The only difference between fields and variables is that fields merge values
(instead of replacing) even without a path.

When writing to a field without a path, the new value is merged with any
existing value at that field following [Merge Behaviour](#merge-behaviour).

When writing to a variable with a path, the value is merged with any existing
value at that path, following [Merge Behaviour](#merge-behaviour).

The above behaviours can be precisely controlled/overridden using
[Merge Modes](#merge-modes).

**Implementation detail: ** Fields are just paths on `$this` variable. That is,
`myField: myValue` is really `var $this.myField: myValue`. Hence fields do not
really have different merge rules from variables, they are just paths on a
single implicit variable.

##### Exception: $this {#exception-$this}

The field `$this` actually translates to the variable `$this`. That is, `var
$this: …` and `$this: …` are the same mapping. However, this only applies if
`$this` does not have a path: `$this.aaa.bbb: …` still translates to `var
$this.'$this'.aaa.bbb: …` as specified above. Note as specified
[above](#exception-$this) that `$this` cannot be replaced and will merge when
written (whether as a variable or a field).

### Merge Behaviour {#merge-behaviour}

By default, when a path/field is written with a value, it is merged with the
existing value at that field, rather than replacing it. This is done to promote
mapping fields as few times as possible and mapping one output field from many
input fields.

A merge operation is defined as `merge(existing Data, incoming Data) → Data`,
where each data is one or more of the [Data Types](#data-types). The merge rules
are as follows (where rules are applied in order, and the first applicable rule
is the result of the merge):

1.  `merge(existing Null, incoming Any) → incoming`
2.  `merge(existing Container, incoming Container) → deep union`

    This rule will union the fields of the two containers, and for any fields
    they have in common will recursively apply these merge rules.

3.  `merge(existing Array, incoming Array) → concatenation`

    The two arrays are concatenated, with the incoming array's elements being
    appended to the end of the existing array.

    A caveat exists here. Elements at explicitly specified indexes in these
    arrays will be preserved at those indexes, and merged if applicable. This
    lets you more precisely control the positions of elements in arrays through
    merges. For example:

    ```js
    var existing: [1, 2] // An array of two elements, 1 and 2, and without any explicitly specified indexes.
    var existing[2]: 999 // An array with a single item, 999, explicitly specified at the second index in the array.
    // existing is [1, 2, 999]

    var incoming: [3, 4] // An array of two elements, 3 and 4, and without any explicitly specified indexes.
    var incoming[2]: 123 // An array with a single item, 123, explicitly specified at the second index in the array.
    // incoming is [3, 4, 123]

    merged: existing
    merged: incoming
                     // merged is:
                     // [1, 2, 123, 3, 4]
                     // since the "fixed" elements at [2] were merged (the elements are primitives, and primitives replace one another, so 123 replaced 999), while the
                     // rest of the elements are appended.

    ```

    Be aware that the resulting array persists the fixed aspect of the element;
    That is, in the above result, `merged`'s `123` is still fixed.

4.  `merge(existing Any, incoming Any) → incoming`

#### Merge Modes {#merge-modes}

Merge modes are a feature that allow manually overriding merge behaviour for
more precise control. Merge modes are specified before the `var` keyword in
[Variable](#variables) mappings or the field name in [Field](#fields) mappings -
`<mode> var &lt;identifier>&lt;path>: &lt;value>` and `<mode> &lt;field
identifier>&lt;path>: &lt;value>`.

Merge modes are currently experimental and must be enabled with an
[Option](#options) - `option "experiment/merge_modes"`

There are currently 4 merge modes: merge, replace, append, extend.

##### Merge {#merge}

This is the default merge mode for fields when no explicit mode is specified. It
will recursively merge data according to the rules below

1.  `merge container1: container2` - recursively merge (using these same merge
    rules) all fields in `container1` with `container2`. Fields in `container1`
    not in `container2` left as is. Fields in `container2` not in `container1`
    are included as well.

    ```js
    option "experiment/merge_modes"

    var container1: {
      a: ["1"]
      b: {
        two: "gone"
        three: "three"
      }
      c: "3"
      y: "y"
    }

    var container2: {
      a: ["100"]
      b: {
        two: "200"
      }
      c: "300"
      z: "z"
    }
    merge var container1: container2 // container1 is now
                                     // {
                                     //   "a": [
                                     //     "1",
                                     //     "100"
                                     //   ],
                                     //   "b": {
                                     //     "two": "200"
                                     //     "three": "three"
                                     //   },
                                     //   "c": "300",
                                     //   "y": "y",
                                     //   "z": "z"
                                     // }
    ```

2.  `merge array1: array2` - (currently) concatenates the two arrays. *An open
    question exists of whether this should merge the array elements index by
    index. Feedback is welcome.*

3.  `merge primitive1: primitive2` - replaces primitive1 with primitive2.

4.  Catch-all `merge X: Y` where X and Y match none of the above rules -
    replaces X with Y.

##### Replace {#replace}

This is the default merge mode for variables when no explicit mode is specified
and no path on the variable is specified. This mode will replace any field,
variable, or path on fields or variables with the source (after the `:`) data.
Examples:

```js
option "experiment/merge_modes"

var variable1: {
   a: "a"
   ccc: {
       gone: "bye"
   }
}

var variable2: {
   b: "b"
}

field1: {
   also_gone: "bye"
}

replace field1: variable2
// field1 is now
// {
//   "b": "b"
// }

replace var variable1.ccc: variable2
// variable1 is now
// {
//     "a": "a",
//     "ccc": {
//         "b": "b"
//     }
// }
```

##### Append {#append}

This merge mode applies only to arrays. `append my_array: anything` or `append
var my_array: anything` will append `anything` to `my_array` as the last
element, regardless of what `anything` is (array, container, primitive, etc).

Append will error if:

1.  [b/243961894] The target does not exist - `append newfield: something`
2.  The target is not an array - `append my_container: something`

Examples:

```js
option "experiment/merge_modes"


var array1: [1, 2, 3]
var array2: [4, 5, 6]
var something: "hello!"

append var array2: something
// array2 is now
// [
//     4,
//     5,
//     6,
//     "hello!"
// ]

append var array1: array2
// array1 is now
// [
//     1,
//     2,
//     3,
//     [
//         4,
//         5,
//         6,
//         "hello!"
//     ]
// ]
```

##### Extend {#extend}

This merge mode applies only to arrays and containers. It works as follows:

1.  `extend array1: array2` - concatenate `array2` into `array1`. Examples:

    ```js
    option "experiment/merge_modes"

    var array1: [1, 2, 3]
    var array2: [4, 5, 6]

    extend var array1: array2 // array1 is now
                              // [
                              //   1,
                              //   2,
                              //   3,
                              //   4,
                              //   5,
                              //   6
                              // ]
    ```

1.  `extend container1: container2` - add any fields from `container2` to
    `container1` that are **not** already in `container1`. Examples:

    ```js
    option "experiment/merge_modes"

    var container1: {
      a: ["1"]
      b: {
        two: "2"
      }
      c: "3"
      y: "y"
    }
    var container2: {
      a: ["nope"]
      b: {
        two: "nope"
      }
      c: "nope"
      z: "yes"
    }

    extend var container1: container2 // container1 is now
                                      // {
                                      //   "a": [
                                      //     "1"
                                      //   ],
                                      //   "b": {
                                      //     "two": "2"
                                      //   },
                                      //   "c": "3",
                                      //   "y": "y",
                                      //   "z": "yes"
                                      // }
    ```

1.  Any other usage will error.

### Circular Mappings {#circular-mappings}

Field and variable assignments (when not merging, i.e. writing a non-null value
to a null field/path) will preserve the reference to the original data. That is,
assigning `var x.y: x` will create an "infinitely sized" object that can be
accessed through `x.y.y.y.y.y.y.y.y.y.y.y…`. This object is stored efficiently
in memory, but will throw an error if serialized (i.e. returned anywhere in the
output of the mapping).

Deeply nested objects are susceptible as well:

```js
var deep: {
  l1: {
    l2: [{
      num: 1
    }]
  }
}
var deep.l1.l2[0].circle: deep

result: deep.l1.l2[0].circle.l1.l2[0].circle.l1.l2[0].circle.l1.l2[0].circle.l1.l2[0].num
// result is 1
```

This effect does not apply to arrays being concatenated with themselves. That
is,

```js
var array: [1, 2, 3]
var array[]: array // Concatenate array with itself
// array is now [1, 2, 3, 1, 2, 3]
```

## Functions {#functions}

Functions allow duplication/reuse of functionality.

The syntax of a function is `def &lt;identifier>(&lt;arguments>) &lt;body>`
where the identifier follows the basic [identifier rules](#identifiers) (not
hybrid or quoted), arguments are an optional list of identifiers and modifiers
(see below), and body is any valid [Expression](#expressions).

Examples:

```js
def myFunction1() 3.14 // Constant valued function - useful for declaring global constants.

def myFunction2(a) a // This function has one argument and just returns it.

def add(a, b) a + b // Adds the two arguments.

def add(a, b, c) a + b + c // Adds the three arguments. Functions in the same
                           // package can have the same name if they have different
                           // numbers of arguments.

def blockFunction(a, b, c) { // This function uses a block expression as the body
   field1: a
   field2: b
   field3: c
}

def arrayFunction(a, b) [ // This function uses an array expression as the body
  a,
  b,
  a + b
]

def ternaryFunction(a, b, c) if a then { // A ternary expression is just as valid
   fieldB: b
} else {
   fieldC: c
}

def argumentValuedFunction(a) { // This function returns a container with field
                                // 'original' and all the fields from 'a' merged in
                                // according to Mapping rules (expression with no
                                // target)
   original: a
   a
}
```

### Java Functions {#java-functions}

In addition to functions written in Whistle, Whistle code can call functions
written in Java if they are loaded/[imported](#imports). Java functions must
have arguments of types derived from the `Data` interface (or RuntimeContext for
the first argument). The Java API is beyond the scope of this document, but more
details can be found in this
[other document](https://docs.google.com/document/u/0/d/11FGWu_6jbGO0tVbp9Dj5lI2rC50iDy9dxH9mm-FUJ2Q/edit).

### Arguments {#arguments}

Function arguments are a list of identifiers. They work exactly like
[Variables](#variables). Additionally, an argument can be preceded by a
modifier - a keyword that specifies some semantic for that argument.

The only currently supported modifier is `required`. This will cause the
function to skip execution (i.e. return Null immediately) if any `required`
argument is Null. Examples:

```js
def mustHaveA(required a, b) {
   aWasNull: !a? // Will always be false.
   bWasNull: !b? // May be true or false (true iff b was null).
   fieldA: a // Will always have a value.
   fieldB: b // May or may not be null.
}

mustHaveA(123, 123) // returns a container.
mustHaveA(123, {}) // returns a container.
mustHaveA({}, 123) // returns Null.
mustHaveA({}, {}) // returns Null.

def mustHaveBoth(required a, required b) {
   aWasNull: !a? // Will always be false.
   bWasNull: !b? // Will always be false.
   fieldA: a // Will always have a value.
   fieldB: b // Will always have a value.
}

mustHaveBoth(123, 123) // returns a container.
mustHaveBoth(123, {}) // returns Null.
mustHaveBoth({}, 123) // returns Null.
mustHaveBoth({}, {}) // returns Null.
```

### Overload selection {#overload-selection}

Multiple functions in Whistle can have the same name. When called, an overload
selection algorithm will select the specific function to call, or throw an error
if there is ambiguity.

Since only native Java functions have typed arguments, all arguments in Whistle
functions are treated as basic Data types.

Also note that only Java functions can be variadic, so any rules applying to
variadic would only apply to such Java functions.

The overload selector follows a series of rules to determine a score for each
candidate function (function candidates are just functions matching the given
name/identifier).

First, all candidates with the wrong arity are eliminated. That is, given X
arguments, non-variadic functions with `arity != X` are eliminated along with
variadic functions with `arity > X + 1`.

Next, each argument is matched up to the required argument types of each
candidate, and a score is computed. Given a provided argument of type `A` and a
required argument type `R`, the score for this pair is:

1.  `A == R` ⇒ 0
2.  `R` is an interface and `A` directly implements it ⇒ 1
3.  `R` is a superclass of `A` ⇒ 2
4.  `A` is a
    [Wrapper](https://docs.google.com/document/d/11FGWu_6jbGO0tVbp9Dj5lI2rC50iDy9dxH9mm-FUJ2Q/edit#heading=h.4gkwd21n1lv7)
    around some data `A'` ⇒ ε + `score(R, A')`
5.  None of the above ⇒ infinity

If R is the variadic argument, then:

1.  If R is an [Array](#array) (of arrays, i.e. `Array… R`) and A is an
    [Array](#array) ⇒ 0.1
2.  If A is an Array (R is not) ⇒ `sum(R, each element in A)`
3.  None of the above ⇒ `score(R, A)` as if R was not variadic

The scores of each given argument and required argument pairs are summed up and
attached to each candidate. The candidate with the lowest score is selected, or
an error is thrown if there is a tie for lowest (or if the lowest is infinity).

#### Variadic auto-unpacking

Note that the above rules (if R is the variadic argument, rule 2) will result in
arrays being unpacked into a variadic argument.

That is, given a function `func(A, B, C...)` (where `C` is variadic) - calling
it as `func(1, 2, [3, 4, 5])` or as `func(1, 2, 3, 4, 5)` will have the same
effect.

### Paths and Function calls {#paths-and-function-calls}

A function call can be used as the path head in a
[Path expression](#path-expressions). Example:

```js
def container() {
   field.nested: 123
   array[]: 456
}

var nested: container().field.nested // nested is 123.
var value: container().array[0] // value is 456.
```

### Consequences of pass by reference {#consequences-of-pass-by-reference}

Currently, function arguments are passed by reference. This means that
modifications to those arguments (given they are just variables) will be seen
when the function returns:

```js
var original: {
  field: 123
}

def naughtyFunction(container) {
   var container.field: "MODIFIED"
}

var value: naughtyFunction(original) // Function does not write any fields, return
                                     // is Null.
// original.field is now "MODIFIED"!

def naughtyFunction2(container) {
   var newContainer: container // This is still maintaining the reference -
                               // newContainer === container.
   var newContainer.field: "MODIFIED"
}

var original.field: 123 // Reset to original value
var value: naughtyFunction2(original) // Function does not write any fields, return
                                      // is Null.
// original.field is now "MODIFIED" again!
```

Relying on this behaviour is often dangerous, and can introduce unexpected
changes in values, especially with deep function call chains.

As general rules:

1.  Do not rely on modifying arguments for returning data from functions (use
    [side outputs](#side-outputs))
2.  Avoid modifying any variables used for input ([$root](#root-functions),
    arguments)
3.  When in doubt, protect a variable with `deepCopy`. Example:

    ```js
    var original: {
      field: 123
    }

    def notSoNaughtyFunction(container) {
      var container: deepCopy(container) // Protect container by replacing it with a
                                         // copy of the data.
      var container.field: "MODIFIED" // This only modifies the copy.
    }

    var value: notSoNaughtyFunction(original) // Function does not write any fields, return
                                              // is Null.
                                              // original.field is still 123!
    ```

### Root Functions {#root-functions}

The root function is the equivalent of `main` in Whistle. Root functions are
defined for every file, and are run if that file is specified as the "main"
config (how to do this will vary by how the file is run).

The root function is implicitly created from all the [Mappings](#mappings) in a
file that are written outside of any function body.

The root function has one argument - `$root`, to be provided by the runner,
which is the input to the file (in the context of a data transformation, this is
expected to be the input data to transform). \

The return value of the root function is the output of the file (in the context
of a data transformation, this is expected to be the output data of the
transformation).

Root functions are implicitly [block expressions](#block-expressions), but
follow all regular [Mapping](#mappings) behaviours (semantics of `$this`, var
scoping, etc).

Example:

```js
// All the below mappings constitute the root function.
var temp: 123
field: temp + $root // Assuming file input is a number.
array[]: 123

// Function calls can happen before the function definition.
funcField: someFunction()

def someFunction() 456 // Functions can be interspersed with root mappings.

// This mapping is still a part of the root function.
anotherField: someFunction()
```

### Side outputs {#side-outputs}

Side outputs allow returning multiple values from a function, or even a function
call chain. Side outputs use the same semantics as [Fields](#fields).

A side output can be written with `side &lt;identifier>&lt;path>: &lt;value>`.

Side outputs are written to a hidden container, which is:

*   Either created by `withSides`
*   Or the [root output](#root-functions) of a mapping if there is no withSides
    in the call chain

Examples:

```js
var result: withSides(multiOutput())
// withSides will merge all side outputs with the main output of the given
// expression using standard field merge behaviour.
// result is {
//   output1: 1
//   output2: 2
//   nested: {
//     output3: 3
//   }
// }

def multiOutput() { // This function on its own just returns { output: 1; }
   output1: 1
   side output2: 2
   side nested.output3: 3
}

def deeperChain1() deeperChain2()

def deeperChain2() deeperChain3()

def deeperChain3() multiOutput()

var result: withSides(deeperChain1()) // Side outputs are propagated up the
                                      // function call chain until either the
                                      // root function or a withSides. Thus,
                                      // result is the same as above here.
// result is {
//   output1: 1
//   output2: 2
//   nested: {
//     output3: 3
//   }
// }
```

### Functions as targets {#functions-as-targets}

Any function (with one or more parameters) can also be used as a target in a
[Mapping](#mappings).

The syntax for this follows the form of `<function identifier>(&lt;partial
arguments>): &lt;value>`.

This is syntactic sugar for `<function identifier>(&lt;partial arguments>,
&lt;value>)`. The output of the function is discarded. Side outputs are
maintained, and treated as they would be in any function call.

The function will be invoked as usual, but the `<value>` will be passed as the
last argument (hence why functions used this way must have at least one
argument).

This pattern is useful for functions that want to use many plugin targets or
side outputs on the same source.

Merge modes are not applicable to function targets.

Example:

```js
someFunc()

// result of the root function (since no withSides, side outputs propagate to root)
// {
//   paths: ["/one", "/two"]
//   values: [123, 456]
// }

def someFunc() {
   myTarget("/one"): 123
   myTarget("/two"): 456
}

def myTarget(path, value) {
  side paths[]: path
  side values[]: value

  thisWillNotShowUpAnywhere: "Can't see this"
}
```

### Closures {#closures}

A closure is a capture of any [expression](#expressions) (and all the variables
it relies upon), that can be used to evaluate that expression at any desired
point in time (rather than immediately as expressions usually are evaluated).

Currently, closures can only be used by native java functions that wish to defer
evaluation of one or more of their parameters.

For example, the `and` operator/function takes in closure arguments rather than
evaluating each argument and then calling the function with the values. That is,
`and` is implemented like (pseudo-code):

```js
boolean and(Closure arg…) {
  for each arg {
     value = arg.execute()
     if !truthy(value) {
         return false
     }
  }
  return true
}
```

This enables the short-circuiting behaviour of `and` - if a single Closure
returns false, the next Closures are not evaluated at all - `and(false, &lt;some
expensive operation>)` will never evaluate `<some expensive operation>`.

Closures can also have "free parameters" These are placeholder variables
available to the expression in the closure, but that do not have a value
assigned when the closure is created. The value is instead assigned when the
closure is executed. For example, [Iteration](#iteration) (see below for more
context) provides its closure with a free variable `$`, and binds it to each
element of the iterated array. `iterate` is implemented like (pseudo-code):

```js
Array iterate(Closure[FreeArg($)] body, Array iter) {
   Array result = []
   for each element in iter {
     mapped = body.executeWithFreeArgBindings($=element)
     result.add(element)
   }
   return result
}
```

Closures are used in various other implementations, such as the
[Ternary](#conditional-ternary-expressions) functions, [Iteration](#iteration),
etc.

### Selectors {#selectors}

Selectors are syntactic sugar for inserting function calls into a
[path expression](#path-expressions). The general syntax for selectors is
`<path>[&lt;selector identifier> &lt;optional argument>]&lt;optional path>`. Any
unary or binary function is viable as a selector. Selectors with closure
arguments can be used to perform some operation on the elements of `<path>` if
it returns a collection. Selector expression

An example of an existing selector is `where` which is used as:

```
var array: [1, 2, 3, 4, 5, 6]
big: where(array, $ >= 4) // Calling the regular function.
small: array[where $ < 4] // Calling where as a selector.
```

## Iteration {#iteration}

Iteration allows applying a mapping to elements of a collection. Iteration can
be applied to [Arrays](#array), [Datasets](#datasets), and
[Containers](#container).

### Arrays {#arrays}

Array iteration can be applied to one more arrays. If applied to multiple
arrays, each array must be the same size as the others, or empty. Nulls are not
skipped.

#### Inline iteration {#inline-iteration}

Inline iteration allows calling a function with one or more array arguments, but
rather than passing in the arrays as is, the function is called on each array
element instead and the results recomposed back into an array. The general
syntax pattern is `<function>(..., array[], …)` - that is, the iteration
operator `[]` can be applied to any array argument to any function to
immediately iterate the array and execute the function for each element.
Examples:

```js
var array: [1, 2, 3]
regular: operate("test", array) // No iteration, result is:
// {
//   const: "test"
//   element: [1, 2, 3]
// }

iterated: operate("test", array[]) // With iteration, result is:
// [{
//   const: "test"
//   element: 1 // The element argument is replaced with each iterated array element.
// }, {
//   const: "test" // The non-iterated arguments are passed in as-is to each
//                 // iteration. Beware of side effects (see Consequences of
//                 // pass-by-reference) if modifying these variables.
//   element: 2
// }, {
//   const: "test"
//   element: 3
// }]

def operate(c, element) {
   const: c
   element: element
}


var a1: [1, 2, 3]
var a2: [100, 200, 300]
var a3: [1000, 2000, 3000]

zipped: multipleElements(a1[], a2[], a3[])
// zipped is [1101, 2202, 3303]

oneEmpty: multipleElements(a1[], a2[], arrayOf()[]) // arrayOf produces an empty
                                                 // array, which we are then
                                                 // "iterating" - missing values
                                                 // are implicitly Null == 0.
// oneEmpty is [101, 202, 303]

def multipleElements(a, b, c) a + b + c
```

#### Explicit iteration {#explicit-iteration}

Explicit iteration uses the builtin `iterate` function, which does not require
declaring a function to iterate with like implicit iteration does. The general
syntax is `iterate(&lt;body>, &lt;array1>, &lt;array2>, etc)`. Note that in this
approach only arrays/iterated arguments are passed in. Constant arguments can
simply be used in `<body>`'s [closure](#closures).

If only one array is present, each element is bound to variable `$`. If multiple
arrays are present, each element in the first is bound to $1, the second to $2,
the third to $3, etc.

Examples:

```js
var array: [1, 2, 3]
var c: "hello"
iterated: iterate({
   const: c // c is just pulled in from outside
   element: $  // $ is bound to each element from array
}, array)
// result is:
// [{
//   const: "test"
//   element: 1
// }, {
//   const: "test"
//   element: 2
// }, {
//   const: "test"
//   element: 3
// }]

var a1: [1, 2, 3]
var a2: [100, 200, 300]
var a3: [1000, 2000, 3000]

zipped: iterate($1 + $2 + $3, a1, a2, a3) // $1 are elements from a1, $2 are elements
                                          // from a2, $3 are elements from a3.
// zipped is [1101, 2202, 3303]

oneEmpty: iterate($1 + $2 + $3, a1, a2, arrayOf())
// oneEmpty is [101, 202, 303]
```

### Containers {#containers}

[Containers](#container) can also be iterated. Containers can be viewed as
unordered "arrays" of key-value pairs, and are iterated by passing in the values
of each key. The resulting value is then assigned to the key in the output
container. The syntax for iteration is the same as arrays, for both inline and
explicit iteration.

The main notable difference between arrays and containers is that there is no
size requirement for containers - since the iteration is done upon key-values,
containers that are missing the key just get a Null value.

Examples:

```js
var c1: {
  k1: "c1k1"
  k2: "c1k2"
}

def modify(value) value + "-modified"

result: modify(c1[])
explicitResult: iterate($ + "-modified", c1)
// both of the above are the same, and are
// {
//    "k1": "c1k1-modified",
//    "k2": "c1k2-modified"
// }


var c2: {
  k1: "c2k1"
  k3: "c2k3"
}

var c3: {
  k1: "c3k1"
  k3: "c3k3"
}

zipped: add(c1[], c2[], c3[])
explicitZipped: iterate($1 + $2 + $3, c1, c2, c3)
// both of the above are the same, and are
// {
//    "k1": "c1k1c2k1c3k1",
//    "k2": "c1k2",
//    "k3": "c2k3c3k3"
//  }
```

### Datasets {#datasets}

Iterating datasets behaves identically to arrays, except that only one dataset
can be iterated at a time (i.e. datasets cannot be iterated together).

## Error Handling {#error-handling}

Error handling is done using the `withError` function. This function is
analogous to a `try/catch` paradigm - `withError` takes two
[closure/expression](#closures) arguments - the "body" which can produce an
error, and the "handler" which contains a [free parameter](#closures) -
`$error` - containing the error information.

`withError` is a regular function call and is used as such: `withError(<body>,
<body handler>)`

The result/return value of `withError` is the value of the body if no error is
encountered or the value of the handler if one is caught..

An example of the contents of `$error`:

```json
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

An example of usage of the entire setup:

```js
var noError: withError({
  // No problems here
  var array: [1, 2, 3]
  value: array[1]
}, {
  willNeverAppear: $error
})

// noError will be:
// {
//   value: 2
// }


var hasError: withError({
  // Trying to access a field on an array will result in an error
  var array: [1, 2, 3]
  willNeverAppear: array.hello
}, {
  theError: $error
})

// hasError will be:
// {
//   "theError": {
//     "cause": "UnsupportedOperationException: Attempted to key into
//               non-container Array/DefaultArray with field hello",
//     "stack": [
//       {
//         "file": "Native",
//         "function": "get",
//         "line": -1,
//         "package": "com.google.cloud.verticals...builtins.Core"
//       },
//       {
//         "file": "Native",
//         "function": "withError",
//         "line": 4,
//         "package": "com.google.cloud.verticals...builtins.error.Errors"
//       },
//       {
//         "file": "file:///usr/local/.../test.wstl",
//         "function": "test_root_function",
//         "line": 1,
//         "package": "test"
//       }
//     ],
//     "vars": {
//       "array": [
//         1,
//         2,
//         3
//       ]
//     }
//   }
// }
```

Some plugins can choose to integrate with
[withError](https://docs.google.com/document/d/11FGWu_6jbGO0tVbp9Dj5lI2rC50iDy9dxH9mm-FUJ2Q/edit#heading=h.eyzwlaspfphe)
to direct any internal errors into it.

## Packages {#packages}

Whistle code is organized into packages. Packages are specified with `package
&lt;name>` at the top of each file (must be the first statement in the file). A
file with no package specified defaults to a package named after the file name
itself. That is, `SomeFile.wstl` will have the package name `SomeFile`. If the
file name cannot be determined, the package name will be `$default`.

Packages are used to organize [functions](#functions). Packages must be prefixed
before function names that are not builtins and not in the same package as the
code [calling](#function-calls) the function.

Examples:

`file1.wstl`:

```js
package one
import "./file2.wstl"
import "./file3.wstl"

samePackageDifferentFile()

three::differentPackage() // Calling differentPackage() without a package name
                          // prefix will cause a function not found error.
```

`file3.wstl`:

```
package three

def differentPackage() "hello from file3"
```

`file2.wstl`:

```
package one

def samePackageDifferentFile() "hello from file2"
```

### Imports {#imports}

Imports allow Whistle files to import other Whistle files and Java plugins that
provide functions or other functionality.

Imports are implemented through a Loader which loads the content and a Parser
which processes the content. This implementation detail is out of scope of this
document but more information can be found
[here](https://docs.google.com/document/d/11FGWu_6jbGO0tVbp9Dj5lI2rC50iDy9dxH9mm-FUJ2Q/edit#heading=h.q4me9m9xrqzg).

Imports can be either constant strings, or a function call (of a function that
returns a string).

Constant string imports look like `import "&lt;loader>://&lt;path>"` where
`<path>` is of a format specific to the corresponding loader. There are some
builtin loaders (covered below), but other loaders can be provided by plugins
(which must be imported before imports that use their loaders). Imports of
strings may contain [interpolation](#constants-and-literals) (but not
concatenation/operators).

Function call imports look like `import someFunction()` where `someFunction`
must be defined in a file that was previously imported.

Loaders are optional - imports can be relative to the current file, for example
like `import "./&lt;relative path>"` - in this case the loader that was used to
load the current file will be used again and the path will be applied relatively
to the path used to load the current file (according to the `java.nio.Path`
implementation).

Examples:

`file1.wstl`:

```js
package one
import "./file1.1.wstl"
import importTwo()

samePackageDifferentFile()
```

`file1.1.wstl`:

```js
package one

def importTwo() "./file2.wstl"
```

`file2.wstl`:

```js
package two

def someFunction() "hello"
```

Note that import cycles are ignored (that is, a file or plugin that was
previously imported will not be imported again).

#### Builtin loaders {#builtin-loaders}

File loader - `file://` - the default loader if no loader is specified and an
absolute path is given. This loader takes a file system path to a file to load.

Plugin loader - `class://` - the loader for loading Java plugins. The path
argument is the fully qualified class name. Different environments will provide
different sets of available classes/plugins to load.

### Wildcards {#wildcards}

In lieu of a package name in a function call (such as
`<package>::&lt;function>(...)`), a wildcard `*` can be supplied. This will
search all matching functions in all imported packages/plugins. For example:

`file1.wstl`:

```js
package one

// This can be specifically called with one::func(...)
def func(a) "hello {a}, from package one"
```

`file2.wstl`:

```js
package two

// This can be specifically called with two::func(..., ...)
def func(a, b) "hello {a} and {b}, from package two"
```

`main.wstl`:

```js
import "./file1.wstl"
import "./file2.wstl"

one: *::func("main") // Resolves to one::func
two: *::func("main", "friend") // Resolves to two::func
```

Note that if two packages have the same function with the same number of
arguments, then an error will be thrown if it is called with a wildcard.

## Options {#options}

Options allow the enabling of specific behaviours for a single file. Options are
placed after packages but before imports. Note that the effects of options are
applied at a time that is specific to the option. This can be when a function is
called, a file is parsed, etc.

Options are specified as `option "&lt;name>"`. Options are provided by the
default runtime and plugins. The builtin options are:

`experiment/merge_modes` - enables the use of [Merge Modes](#merge-modes).

`experiment/unique_vars_and_fields` - prevents [fields](#fields) from having the
same name as [vars](#variables), for readability/linting. Will raise an error
during initialization/execution if conflicting names are found in a file.

## Builtin functions {#builtin-functions}

Builtin functions are functions built-in to the core runtime and are available
in all Whistle files. The list of functions and their documentation can be found
[here](./builtins.md).
