### where
`where(nullData: NullData, predicate: Closure)` returns `NullData`

#### Arguments
**nullData**: `NullData`


**predicate**: `Closure`


#### Description
Catch-all for where applied to null, to disambiguate null from Array and Container.



### where
`where(array: Array, predicate: Closure)` returns `Array`

#### Arguments
**array**: `Array`


**predicate**: `Closure`


#### Description
Filters the given Array, returning a new one containing only items that match the given predicate. Each array element is represented in the predicate by `$`. Example:

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
Filters the given Container, returning a new one containing only items that match the given predicate. For each field-value pair in the Container, they are available in the predicate as `$.field` and `$.value`. If the predicate returns false, the resulting container will not have this field-value pair present. Example:

```
var container: {
   f1: 1; f2: 2; f3: 3; f4: 4;
 }
 var newContainer: container[where $.value < 2 || $.field == "f4"]
 // newContainer : {f1: 1; f4: 4;}
```

