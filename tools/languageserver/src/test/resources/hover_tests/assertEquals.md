### assertEquals
`functions::assertEquals(want: Data, got: Data)` returns `NullData`

#### Arguments
**want**: `Data`


**got**: `Data`


#### Description
Throws an exception describing the difference between the two given data, if there is any. If they are the same returns null.

An example diff between `{"a": {"b": [1, 2, 3]}, "c": 1}` and `{"a": {"b": [1, 5, 6, 7]}, "c": "one"}` might look like:

```
-want, +got
a.b[1]: -2 +5
a.b[2]: -3 +6
a.b[3]: -past end of array +7
c: -1 +"one"
```

