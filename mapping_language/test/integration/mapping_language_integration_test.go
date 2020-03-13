// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// package integration_test contains end-to-end integration tests.
package integration_test

import (
	"context"
	"encoding/json"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/transform" /* copybara-comment: transform */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language/transpiler" /* copybara-comment: transpiler */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	dhpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	mpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestTranspile(t *testing.T) {
	type valueTest struct {
		rootMappings, wantJSON, inputJSON string
	}
	tests := []struct {
		name      string
		whistle   string
		wantValue valueTest
	}{
		{
			name:    "object root mapping",
			whistle: `out ThisIsAnObject: "asd"`,
			wantValue: valueTest{
				wantJSON: `{
									   "ThisIsAnObject": [
									     "asd"
									   ]
									 }`,
			},
		},
		{
			name:    "legacy object root mapping",
			whistle: `obj ThisIsAnObject: "asd"`,
			wantValue: valueTest{
				wantJSON: `{
									   "ThisIsAnObject": [
									     "asd"
									   ]
									 }`,
			},
		},
		{
			name: "variable root mapping",
			whistle: `var myvar: "asd";
							 out MyObj: myvar;`,
			wantValue: valueTest{
				wantJSON: `{
									   "MyObj": [
									     "asd"
									   ]
									 }`,
			},
		},
		{
			name: "no args projector definition",
			whistle: `def MyProjector() {
									a: "test"
							  }`,
			wantValue: valueTest{
				rootMappings: "out myOut: => MyProjector",
				wantJSON: `{
											   "myOut": [
											     {
											       "a": "test"
											     }
											   ]
											 }`,
			},
		},
		{
			name: "one arg projector definition",
			whistle: `def MyProjector(thisIsAnArg) {
									a: thisIsAnArg.foo;
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: "hello world" => MakeFoo => MyProjector
											 def MakeFoo(value) { foo: value; }`,
				wantJSON: `{
									   "myOut": [
									     {
									       "a": "hello world"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "multiple arg projector definition",
			whistle: `def MyProjector(thisIsAnArg, thisIsAlsoAnArg) {
									a: thisIsAnArg // this is a comment
									b: thisIsAlsoAnArg
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: "one", "two" => MyProjector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "a": "one",
											       "b": "two"
											     }
											   ]
											 }`,
			},
		},
		{
			name: "constant sources",
			whistle: `def projector() {
									str: "str";
									int: 9238;
									float: 9.927840232849121;
									negfloat: -9.927840232849121;
									bool: false;
									alsoBool: true;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": "str",
											       "int": 9238,
											       "float": 9.927840232849121,
											       "negfloat": -9.927840232849121,
											       "bool": false,
											       "alsoBool": true
											     }
											   ]
											 }`,
			},
		},
		{
			name: "single projection",
			whistle: `def projector() {
									str: "str" => $StrCat;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": "str"
											     }
											   ]
											 }`,
			},
		},
		{
			name: "single projection with multiple args",
			whistle: `def projector() {
									str: "str", "foo" => $StrCat;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": "strfoo"
											     }
											   ]
											 }`,
			},
		},
		{
			name: "chained projection with multiple args",
			whistle: `def projector() {
									str: "str", "foo" => $ListOf => $ListLen;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": 2
											     }
											   ]
											 }`,
			},
		},
		{
			name: "chained projection with multiple args later",
			whistle: `def projector() {
									str: 1337, ("str", "foo" => $ListOf => $ListLen) => $Sum;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": 1339
											     }
											   ]
											 }`,
			},
		},
		{
			name: "modifiers",
			whistle: `def projector(a) {
									array[]: a[];
									array[]: a[0].b[] => $DebugString;
									value: "foo";
									value!: a[1].b;
									partial.arr[].key: "bar"
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: $root.a => projector`,
				wantJSON: `{
									   "myOut": [
									     {
									       "array": [
									         {
									           "b": [
									             1,
									             2,
									             3
									           ]
									         },
									         {
									           "b": [
									             4,
									             5,
									             6
									           ]
									         },
									         "1",
									         "2",
									         "3"
									       ],
									       "value": [
									         4,
									         5,
									         6
									       ],
									       "partial": {
									         "arr": [
									           {
									             "key": "bar"
									           }
									         ]
									       }
									     }
									   ]
									 }`,
				inputJSON: `{
											   "a": [
											     {
											       "b": [
											         1,
											         2,
											         3
											       ]
											     },
											     {
											       "b": [
											         4,
											         5,
											         6
											       ]
											     }
											   ]
											 }`,
			},
		},
		{
			name: "inline condition",
			whistle: `def projector(a) {
									value (if a => $IsNotNil): a.foo;
									value (iff a => $IsNil): "nothing";
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: $root.nothing => projector; out myOut: $root.something => projector;`,
				wantJSON: `{
											   "myOut": [
											     {
											       "value": "nothing"
											     },
											     {
											       "value": "something"
											     }
											   ]
											 }`,
				inputJSON: `{
											   "something": {
											     "foo": "something"
											   }
											 }`,
			},
		},
		{
			name: "prefix operators",
			whistle: `def projector(a) {
									value (if ~a): a;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: $root.nothing => projector; out myOut: $root.something => projector;`,
				wantJSON: `{
											   "myOut": [
											     {
											       "value": false
											     }
											   ]
										 	 }`,
				inputJSON: `{
											   "something": true,
											   "nothing": false
											 }`,
			},
		},
		{
			name: "postfix operators",
			whistle: `def projector(a) {
									value (if a?): a.foo;
									value (iff ~a?): "nothing";
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: $root.nothing => projector; out myOut: $root.something => projector;`,
				wantJSON: `{
											   "myOut": [
											     {
											       "value": "nothing"
											     },
											     {
											       "value": "something"
											     }
											   ]
											 }`,
				inputJSON: `{
											   "something": {
											     "foo": "something"
											   }
											 }`,
			},
		},
		{
			name: "binary operators",
			whistle: `def projector(a, b, c) {
									value[]: a + 2;
									value[]: a / 2;
									value[]: a - 2;
									value[]: a * 2;
									value[]: a > 2;
									value[]: a < 2;
									value[]: b and c;
									value[]: b or c;
									value[]: 3 ~= 4;
									value[]: 3 = 4;
									value[]: 3 >= 4;
									value[]: 3 >= 3;
									value[]: 4 >= 3;
									value[]: 3 <= 4;
									value[]: 3 <= 3;
									value[]: 4 <= 3;
									value[]: -3 <= -3;
									value[]: -3 <= 0;
									value[]: -3 <= -4;
									value[]: -3 <= 3;
									value[]: -3 >= 0;
									value[]: -3 >= -3;
									value[]: -4 >= -3;
									value[]: -3 >= 3;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: 100, true, false => projector`,
				wantJSON: `{
											   "myOut": [
											     {
											       "value": [
											         102,
											         50,
											         98,
											         200,
											         true,
											         false,
											         false,
											         true,
											         true,
											         false,
															 false,
															 true,
															 true,
															 true,
															 true,
															 false,
															 true,
															 true,
															 false,
															 true,
															 false,
															 true,
															 false,
															 false
											       ]
											     }
											   ]
											 }`,
			},
		},
		{
			name: "operator ordering",
			whistle: `def projector() {
									arithmetic[]: 1 + 2 * 5;
									arithmetic[]: 2 * 2 + 5;
									arithmetic[]: 2 * (2 + 5);
									arithmetic[]: 1 - -2 - 2;
									logic[]: ~(3 = 2 + 1);
									logic[]: ~(2 + 1 = 3);
									logic[]: 3 = 4 or 5 + 1 = 6;
									logic[]: 3 = 1 + 2 and 5 = 6 - 1;
									logic[]: 3 >= 2 + 2 or 1 - 5 >= 7;
								  logic[]: 1 - 3 <= -1 and 5 >= 6 - 1;
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: => projector",
				wantJSON: `{
						"result": [{
							"arithmetic": [11, 9, 14, 1],
							"logic": [false, false, true, true, false, true]
						}]
					}`,
			},
		},
		{
			name: "block condition",
			whistle: `def projector(a) {
									if a + 5 = 7 {
										aPlusFive: "seven"
									}
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: 2 => projector; out result: 100 => projector",
				wantJSON: `{
									   "result": [
									     {
									       "aPlusFive": "seven"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "block condition with else",
			whistle: `def projector(a) {
									if a + 5 = 7 {
										aPlusFive: "seven"
									} else {
										aPlusFive: "not seven"
									}
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: 99 => projector; out result: 2 => projector;",
				wantJSON: `{
									   "result": [
									     {
									       "aPlusFive": "not seven"
									     },
									     {
									       "aPlusFive": "seven"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "nested block condition with else",
			whistle: `def projector(a) {
									if a > 10 {
										if a > 20 {
											a: "more than 20"
										} else {
											a: "more than 10"
										}
									} else {
										if a = 10 {
											a: "equal to 10"
										} else {
											a: "less than 10"
										}
									}
							 }`,
			wantValue: valueTest{
				rootMappings: "var input: 21, 11, 10, 9 => $ListOf; out result: input[] => projector",
				wantJSON: `{
									   "result": [
									     {
									       "a": "more than 20"
									     },
									     {
									       "a": "more than 10"
									     },
									     {
									       "a": "equal to 10"
									     },
									     {
									       "a": "less than 10"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "nested block conditions share fields",
			whistle: `def projector(a) {
									foo: "outter"
									if a > 10 {
										if a > 20 {
											foo!: foo, " inner" => $StrCat;
											foo2: foo, " inner2" => $StrCat;
										}
									}
									bar: foo;
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: 999 => projector",
				wantJSON: `{
									   "result": [
									     {
									       "foo": "outter inner",
									       "foo2": "outter inner inner2",
									       "bar": "outter inner"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "nested block conditions share variables",
			whistle: `def projector(a) {
									var foo: "outter"
									if a > 10 {
										if a > 20 {
											var foo!: foo, " inner" => $StrCat;
										}
									}
									bar: foo;
									foo: foo;
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: 999 => projector",
				wantJSON: `{
									   "result": [
									     {
									       "foo": "outter inner",
									       "bar": "outter inner"
									     }
									   ]
									 }`,
			},
		},
		{
			name: "comments",
			whistle: `// Comment here
							 def projector(a) {
									// comment there
									value: a.foo.bar;

									// comment anywhere! With slashes: // that's fine
									value!: a.foo.bar;
							 }`,
		},
		{
			name:    "newlines optional",
			whistle: `def test(a, b) { if b? { var myvar (if a = "foo"): "asd" => $IsNotNil; if ~(b?) { var bvar (if b = "boo"): "bbb"?; } } else { out asd: 1.2 + 7 => $Str; some: a.field; } }`,
		},
		{
			name: "post process projector name",
			whistle: `
			def makeFoo(num) {
				num: num;
				foo: "bar"
			}

			def addFoo(result) {
				results: result.test
				extra: "extra key"
			}

			post => addFoo
			`,
			wantValue: valueTest{
				rootMappings: `var nums: 1, 2, 3, 4 => $ListOf
											 out test: nums[] => makeFoo;`,
				wantJSON: `{
								     "results": [
								       {
								         "num": 1,
								         "foo": "bar"
								       },
								       {
								         "num": 2,
								         "foo": "bar"
								       },
								       {
								         "num": 3,
								         "foo": "bar"
								       },
								       {
								         "num": 4,
								         "foo": "bar"
								       }
								     ],
								     "extra": "extra key"
								   }`,
			},
		},
		{
			name: "post process inline projector",
			whistle: `
			def makeFoo(num) {
				num: num;
				foo: "bar"
			}

			post def addFoo(result) {
				results: result.test
				extra: "extra key"
			}
			`,
			wantValue: valueTest{
				rootMappings: `var nums: 1, 2, 3, 4 => $ListOf
											 out test: nums[] => makeFoo;`,
				wantJSON: `{
									   "results": [
									     {
									       "num": 1,
									       "foo": "bar"
									     },
									     {
									       "num": 2,
									       "foo": "bar"
									     },
									     {
									       "num": 3,
									       "foo": "bar"
									     },
									     {
									       "num": 4,
									       "foo": "bar"
									     }
									   ],
									   "extra": "extra key"
									 }`,
			},
		},
		{
			name: "numeric/HL7v2 like paths",
			whistle: `
			def ProcessHL7v2(ABC) {
				MSH.1.2.3: ABC.4.5.6.7.A;
				ABC.1: ABC.4.5;
			}
			`,
			wantValue: valueTest{
				rootMappings: `out test: $root => ProcessHL7v2;`,
				inputJSON:    `{"4": {"5": {"6": {"7": {"A": "hello"}}}}}`,
				wantJSON: `{"test": [{
																	"MSH": {"1": {"2": {"3": "hello"}}},
																	"ABC": {"1": {"6": {"7": {"A": "hello"}}}}
																}]}`,
			},
		},
		{
			name: "arg zipping",
			whistle: `def add(a, b) {
									res: a + b
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: $root.nums[], 1 => add",
				inputJSON:    `{"nums": [0, 1, 2] }`,
				wantJSON: `{
									   "result": [
									     {
									       "res": 1
									     },
											 {
									       "res": 2
									     },
									     {
									       "res": 3
									     }
									   ]
									 }`,
			},
		},
		{
			name: "arg zipping with multiple arrays",
			whistle: `def add(a, b, c) {
									res: a + b + c
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: $root.nums[], $root.more_nums[], $root.even_more_nums[] => add",
				inputJSON:    `{"nums": [0, 1, 2], "more_nums": [100, -100, 47], "even_more_nums": [13, 17, 21] }`,
				wantJSON: `{
									   "result": [
									     {
									       "res": 113
									     },
											 {
									       "res": -82
									     },
									     {
									       "res": 70
									     }
									   ]
									 }`,
			},
		},
		{
			name: "arg zipping with wildcards",
			whistle: `def add(a, b, c) {
									res: a + b + c
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: $root.nums[*].a[], $root.more_nums[], $root.even_more_nums[] => add",
				inputJSON:    `{"nums": [{"a": 0}, {"a":1}, {"a":2}], "more_nums": [100, -100, 47], "even_more_nums": [13, 17, 21] }`,
				wantJSON: `{
									   "result": [
									     {
									       "res": 113
									     },
											 {
									       "res": -82
									     },
									     {
									       "res": 70
									     }
									   ]
									 }`,
			},
		},
		{
			name: "arg zipping not applied if there is a projector",
			whistle: `def add(a, b, c) {
									res: a + b[0] + c
							 }
							 def id(x) {
									$this: x
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: $root.nums[], ($root.more_nums[] => id), $root.even_more_nums[] => add",
				inputJSON:    `{"nums": [0, 1, 2], "more_nums": [100, -100, 47], "even_more_nums": [13, 17, 21] }`,
				wantJSON: `{
									   "result": [
									     {
									       "res": 113
									     },
											 {
									       "res": 118
									     },
									     {
									       "res": 123
									     }
									   ]
									 }`,
			},
		},
		{
			name: "filter on item",
			whistle: `def onlyBigNums(list) {
							nums: list[where $ > 100]
						}`,
			wantValue: valueTest{
				rootMappings: "out result: 1, 2, 101, 3, 102, 104 => $ListOf => onlyBigNums",
				wantJSON: `{
									   "result": [
									     {
											 "nums": [101, 102, 104]
										 }
									   ]
									 }`,
			},
		},
		{
			name: "filter with closure",
			whistle: `def onlyBigNums(list) {
							var limit: 100
							nums: list[where $ > limit]
						}`,
			wantValue: valueTest{
				rootMappings: "out result: 1, 2, 101, 3, 102, 104 => $ListOf => onlyBigNums",
				wantJSON: `{
									   "result": [
									     {
											 "nums": [101, 102, 104]
										 	 }
									   ]
									 }`,
			},
		},
		{
			name: "projector result enumeration",
			whistle: `def num_A(num) {
						a: num
						}`,
			wantValue: valueTest{
				rootMappings: "out one: 1, 2, 3 => $ListOf[] => num_A; out two: (1, 2, 3 => $ListOf)[] => num_A; out three: (1, 2, 3 => $ListOf[]) => num_A;",
				inputJSON:    `{"nums": [{"a": 0}, {"a":101}, {"a":201}], "more_nums": [100, -100, 47], "even_more_nums": [13, 17] }`,
				wantJSON: `{
										 "one":[
												{
													 "a":1
												},
												{
													 "a":2
												},
												{
													 "a":3
												}
										 ],
										 "two":[
												{
													 "a":1
												},
												{
													 "a":2
												},
												{
													 "a":3
												}
										 ],
										 "three":[
												{
													 "a":1
												},
												{
													 "a":2
												},
												{
													 "a":3
												}
										 ]
									}`,
			},
		},
		{
			name: "arg zipping with filters",
			whistle: `def GetA(x) {
				$this: x.a;
			}
			def add(a, b, c) {
									res: a + b + c
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: ($root.nums[where $.a > 100][] => GetA[]), $root.more_nums[where $ > 0][], $root.even_more_nums[] => add",
				inputJSON:    `{"nums": [{"a": 0}, {"a":101}, {"a":201}], "more_nums": [100, -100, 47], "even_more_nums": [13, 17] }`,
				wantJSON: `{
									   "result": [
									     {
									       "res": 214
									     },
											 {
									       "res": 265
									     }
									   ]
									 }`,
			},
		},
		{
			name:    "nested filters",
			whistle: ``,
			wantValue: valueTest{
				rootMappings: "out result: $root.nums[where $.a[where $ > 100] => $ListLen > 1]",
				inputJSON:    `{"nums": [{"a": [1, 101, 102]}, {"a": [1, 2, 300]}]}`,
				wantJSON: `{
									   "result": [{"a": [1, 101, 102]}]
									 }`,
			},
		},
		{
			name: "forced var/dest",
			whistle: `def bad_names(arg) {
									var bad: arg, "var" => $StrCat;
									bad: "dest";
									read_var: var bad;
									read_dest: dest bad;
								}`,
			wantValue: valueTest{
				rootMappings: "out result: \"bad\" => bad_names",
				wantJSON: `{
									   "result": [{"bad": "dest", "read_var": "badvar", "read_dest": "dest"}]
									 }`,
			},
		},
		{
			// TODO: Revert after sunset.
			name: "legacy <this> and root",
			whistle: `def this(arg) {
									<this>: arg
								}`,
			wantValue: valueTest{
				rootMappings: "out result: root => this",
				wantJSON: `{
									   "result": [{"red": "blue"}]
									 }`,
				inputJSON: `{"red": "blue"}`,
			},
		},
		{
			name: "root field mappings",
			whistle: `def root_fields(arg) {
									field: arg
									root one[]: "boo!"
									root two: "three"
								}`,
			wantValue: valueTest{
				rootMappings: `red: "red"
											blue: "blue"
											one: "one" => $ListOf
											one[]: "two" => root_fields`,
				wantJSON: `{
									   "red": "red",
										 "blue": "blue",
										 "one": [
										 		"one",
												"boo!",
												{
													"field": "two"
												}
										 ],
										 "two": "three"
									 }`,
			},
		},
		{
			name: "root field mappings does not affect non-root",
			whistle: `def root_fields(arg) {
									field: arg
									root one[]: "boo!"
									root two: "three"
								}`,
			wantValue: valueTest{
				rootMappings: `var _: ("shouldNotBeInOutput" => root_fields)`,
				wantJSON: `{
									   "one": ["boo!"],
										 "two": "three"
									 }`,
			},
		},
		{
			name: "array return types",
			whistle: `def array(one, two, three) {
										[0]: one
										[1]: two
										[5]: three
									}`,
			wantValue: valueTest{
				rootMappings: "result: 1, 2, 3 => array",
				wantJSON: `{
										 "result": [1, 2, null, null, null, 3]
									 }`,
			},
		},

		// TODO: Add more tests.
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			// Just test bare compilation
			_, err := transpiler.Transpile(test.whistle)
			if err != nil {
				t.Fatalf("Transpile(...) yielded unexpected error\nwhistle code:\n%s\nerror: %v", test.whistle, err)
			}

			if test.wantValue.wantJSON != "" {
				full := test.wantValue.rootMappings + "\n" + test.whistle
				compiled, err := transpiler.Transpile(full)
				if err != nil {
					t.Fatalf("transpiler.Transpile(...) yielded unexpected error\nwhistle code:\n%s\nerror: %v", full, err)
				}

				got, err := exec(t, compiled, test.wantValue.inputJSON)
				if err != nil {
					t.Fatalf("executing whistle code yielded unexpected error\nwhistle code:\n%s\nerror: %v\nwhistler: %s", full, err, proto.MarshalTextString(compiled))
				}

				want, err := jsonutil.UnmarshalJSON(json.RawMessage(test.wantValue.wantJSON))
				if err != nil {
					t.Fatalf("invalid test want JSON: %v", err)
				}

				if diff := cmp.Diff(want, got); diff != "" {
					t.Errorf("executing whistle code yielded unexpected results\nwhistle code:\n%s\ndiff -want +got: %v", full, diff)
				}
			}
		})
	}
}

func exec(t *testing.T, config *mpb.MappingConfig, inputJSON string) (jsonutil.JSONToken, error) {
	t.Helper()
	var tr *transform.Transformer
	var err error
	if tr, err = transform.NewTransformer(context.TODO(), &dhpb.DataHarmonizationConfig{StructureMappingConfig: &hpb.StructureMappingConfig{
		Mapping: &hpb.StructureMappingConfig_MappingConfig{MappingConfig: config}}}); err != nil {
		t.Fatalf("failed to initialize transformer: %v", err)
	}

	input, err := jsonutil.UnmarshalJSON(json.RawMessage(inputJSON))
	if err != nil {
		t.Fatalf("error unmarshaling test input: %v", err)
	}

	ic := jsonutil.JSONContainer{}
	if input != nil {
		ic = input.(jsonutil.JSONContainer)
	}

	return tr.Transform(&ic, transform.TransformationConfigs{
		SkipBundling: false,
		LogTrace:     true})
}
