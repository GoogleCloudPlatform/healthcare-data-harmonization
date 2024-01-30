### mockGet
`plugin::mockGet(source: Data, path: String)` returns `Data`

#### Arguments
**source**: `Data`


**path**: `String`


#### Description
Core#get implementation for schema validation purposes. It verifies the validity of the `path` under `source` and records ViolationInfo to the ViolationHandler in case of schema or type violation. It also handles accessing descendant of GraduallyTypedData by "growing" the GraduallyTypedData using the appropriate types.

