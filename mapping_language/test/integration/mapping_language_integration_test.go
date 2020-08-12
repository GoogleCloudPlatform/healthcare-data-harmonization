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

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/transform" /* copybara-comment: transform */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/transpiler" /* copybara-comment: transpiler */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"google.golang.org/protobuf/encoding/prototext" /* copybara-comment: prototext */

	dhpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: data_harmonization_go_proto */
	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
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
			name: "no args function definition",
			whistle: `def MyProjector() {
									a: "test"
							  }`,
			wantValue: valueTest{
				rootMappings: "out myOut: MyProjector()",
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
			name: "one arg function definition",
			whistle: `def MyProjector(thisIsAnArg) {
									a: thisIsAnArg.foo;
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector(MakeFoo("hello world"))
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
			name: "one arg function definition with required - Nil",
			whistle: `def MyProjector(required thisIsAnArg) {
									a: thisIsAnArg.foo;
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector($root.nothing)
											 `,
				wantJSON: ``,
				inputJSON: `{
											   "something": {
											     "foo": "something"
											   }
											 }`,
			},
		},
		{
			name: "one arg function definition with required - notNil",
			whistle: `def MyProjector(required thisIsAnArg) {
									a: thisIsAnArg.foo;
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector($root.something)
											 `,
				wantJSON: `{
									   "myOut": [
									     {
											 		"a": "something"
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
			name: "required keyword multiple argument function nil with nested",
			whistle: `def MyProjector(rt) {
									nested_1: both_required(rt.Abcdefghijklmnop, "Constant")
        					nested_2: both_required(rt.Abcdefghijklmnop, rt.Abcdefghijklmnop)
        					nested_3: both_required(rt.Red.Blue, rt.Red)
									nested_4: none_required(rt.kalfdjlakdf, "Constant")
									nested_5: first_required(rt.Red, rt.dlajfldklkjlakdf)
									nested_6: second_required(rt.kalfdjlakdf, "Constant")
									nested_7: first_required(rt.kadjfk, "Constant")
									nested_8: second_required(rt.Red, rt.akdfjafkl)
									nested_9: required_condition_compose(rt.akldfjkaldf, rt.Red.Blue)
									if 1 > 0 {
										nested_10: first_required("true & required satisfied", rt.ajdfakdfd)
									}
									if 1 < 0 {
										nested_11: first_required("false & required satisfied", rt.lkdjaklfj)
									}
								}

								def none_required(one, two) {
									one: one
									two: two
								}
								def both_required(required one,required two) {
									one: one
									two: two
								}
								def first_required(required one, two) {
									one: one
									two: two
								}
								def second_required(one, required two) {
									one: one
									two: two
								}
								def required_condition_compose(one, required two) {
									if two > 0 {
										one: one
										two: two
										conclusion: "bigger than 0"
									} else {
										two: "smaller than 0"
									}
								}`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector($root)
											 `,
				wantJSON: `{"myOut": [{
											"nested_3":{
													"one":1,
													"two": {"Blue":1}
													},
											"nested_4": { "two":"Constant"},
											"nested_5": {"one":{"Blue":1}},
											"nested_6": { "two":"Constant"},
											"nested_9": { "two":1, "conclusion":"bigger than 0"},
											"nested_10": {"one":"true & required satisfied"}
									}
								]
							}`,
				inputJSON: `{
										"Red": {
											"Blue": 1
										}
									}`,
			},
		},
		{
			name: "one arg function definition with required - notNIL",
			whistle: `def MyProjector(required thisIsAnArg) {
									a: thisIsAnArg.foo;
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector(MakeFoo("hello world"))
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
			name: "multiple arg function definition",
			whistle: `def MyProjector(thisIsAnArg, thisIsAlsoAnArg) {
									a: thisIsAnArg // this is a comment
									b: thisIsAlsoAnArg
							  }`,
			wantValue: valueTest{
				rootMappings: `out myOut: MyProjector("one", "two")`,
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
			whistle: `def function() {
									str: "str\\back\"slash\\\\";
									int: 9238;
									float: 9.927840232849121;
									negfloat: -9.927840232849121;
									bool: false;
									alsoBool: true;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
				wantJSON: `{
											   "myOut": [
											     {
											       "str": "str\\back\"slash\\\\",
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
			whistle: `def function() {
									str: $StrCat("str");
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
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
			whistle: `def function() {
									str: $StrCat("str", "foo");
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
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
			whistle: `def function() {
									str: $ListLen($ListOf("str", "foo"));
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
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
			name: "chained projection with multiple args with brackets",
			whistle: `def function() {
									str: $ListLen($ListOf("str", "foo"));
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
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
			whistle: `def function() {
									str: $Sum(1337, $ListLen($ListOf("str", "foo")));
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function()`,
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
			whistle: `def function(a) {
									array[]: a[];
									array[]: $DebugString(a[0].b[]);
									value: "foo";
									value!: a[1].b;
									partial.arr[].key: "bar"
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function($root.a)`,
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
			whistle: `def function(a) {
									value (if $IsNotNil(a)): a.foo;
									value (iff $IsNil(a)): "nothing";
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function($root.nothing); out myOut: function($root.something);`,
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
			whistle: `def function(a) {
									value (if ~a): a;
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function($root.nothing); out myOut: function($root.something);`,
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
			whistle: `def function(a) {
									value (if a): a.foo;
									value (iff ~a): "nothing";
							 }`,
			wantValue: valueTest{
				rootMappings: `out myOut: function($root.nothing); out myOut: function($root.something);`,
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
			whistle: `def function(a, b, c, d) {
									value[]: a + 2;
									value[]: a / 2;
									value[]: a - 2;
									value[]: a * 2;
									value[]: a > 2;
									value[]: a < 2;
									value[]: ~a;
									value[]: a and c;
									value[]: a or c;
									value[]: ~d;
									value[]: d or b;
									value[]: d and b;
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
				rootMappings: `out myOut: function(100, true, false, $root.nothing)`,
				inputJSON: `{
											   "something": {
											     "foo": "something"
											   }
											 }`,
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
															 false,
															 true,
															 true,
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
			whistle: `def function() {
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
				rootMappings: "out result: function()",
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
			whistle: `def function(a) {
									if a + 5 = 7 {
										aPlusFive: "seven"
									}
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: function(2); out result: function(100)",
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
			whistle: `def function(a) {
									if a + 5 = 7 {
										aPlusFive: "seven"
									} else {
										aPlusFive: "not seven"
									}
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: function(99); out result: function(2);",
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
			whistle: `def function(a) {
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
				rootMappings: "var input: $ListOf(21, 11, 10, 9); out result: function(input[])",
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
			whistle: `def function(a) {
									foo: "outter"
									if a > 10 {
										if a > 20 {
											foo!: $StrCat(foo, " inner");
											foo2: $StrCat(foo, " inner2");
										}
									}
									bar: foo;
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: function(999)",
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
			whistle: `def function(a) {
									var foo: "outter"
									if a > 10 {
										if a > 20 {
											var foo!: $StrCat(foo, " inner");
										}
									}
									bar: foo;
									foo: foo;
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: function(999)",
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
							 def function(a) {
									// comment there
									value: a.foo.bar; // Even here

									// comment anywhere! With slashes: // that's fine
									value!: a.foo.bar;
							 }`,
		},
		{
			name:    "newlines optional",
			whistle: `def test(a, b) { if b { var myvar (if a = "foo"): $IsNotNil("asd"); if ~(b) { var bvar (if b = "boo"): "bbb"; } } else { out asd: $Str(1.2 + 7); some: a.field; } }`,
		},
		{
			name: "post process function name",
			whistle: `
			def makeFoo(num) {
				num: num;
				foo: "bar"
			}

			def addFoo(result) {
				results: result.test
				extra: "extra key"
			}

			post addFoo
			`,
			wantValue: valueTest{
				rootMappings: `var nums: $ListOf(1, 2, 3, 4)
											 out test: makeFoo(nums[]);`,
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
			name: "post process inline function",
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
				rootMappings: `var nums: $ListOf(1, 2, 3, 4)
											 out test: makeFoo(nums[]);`,
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
				rootMappings: `out test: ProcessHL7v2($root);`,
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
				rootMappings: "out result: add($root.nums[], 1)",
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
				rootMappings: "out result: add($root.nums[], $root.more_nums[], $root.even_more_nums[])",
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
				rootMappings: "out result: add($root.nums[*].a[], $root.more_nums[], $root.even_more_nums[])",
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
			name: "arg zipping not applied if there is a function",
			whistle: `def add(a, b, c) {
									res: a + b[0] + c
							 }
							 def id(x) {
									$this: x
							 }`,
			wantValue: valueTest{
				rootMappings: "out result: add($root.nums[], id($root.more_nums[]), $root.even_more_nums[])",
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
				rootMappings: "out result: onlyBigNums($ListOf(1, 2, 101, 3, 102, 104))",
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
				rootMappings: "out result: onlyBigNums($ListOf(1, 2, 101, 3, 102, 104))",
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
			name: "function result enumeration",
			whistle: `def num_A(num) {
						a: num
						}`,
			wantValue: valueTest{
				rootMappings: "out one: num_A($ListOf[](1, 2, 3)); out two: num_A($ListOf[](1, 2, 3)); out three: num_A($ListOf[](1, 2, 3));",
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
				rootMappings: "out result: add(GetA[]($root.nums[where $.a > 100][]), $root.more_nums[where $ > 0][], $root.even_more_nums[])",
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
				rootMappings: "out result: $root.nums[where $ListLen($.a[where $ > 100]) > 1]",
				inputJSON:    `{"nums": [{"a": [1, 101, 102]}, {"a": [1, 2, 300]}]}`,
				wantJSON: `{
									   "result": [{"a": [1, 101, 102]}]
									 }`,
			},
		},
		{
			name: "forced var/dest",
			whistle: `def bad_names(arg) {
									var bad: $StrCat(arg, "var");
									bad: "dest";
									read_var: var bad;
									read_dest: dest bad;
								}`,
			wantValue: valueTest{
				rootMappings: `out result: bad_names("bad")`,
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
				rootMappings: "out result: this(root)",
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
											one: $ListOf("one")
											one[]: root_fields("two")`,
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
			name: "root field mappings using inline list initialization",
			whistle: `def root_fields(arg) {
									field: arg
									root one[]: "boo!"
									root two: "three"
									root empty[1]: "skipped"
								}`,
			wantValue: valueTest{
				rootMappings: `red: "red"
											blue: "blue"
											one: ["one"]
											one[]: root_fields("two")
											int: [1]
											empty: []`,
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
										 "two": "three",
										 "int": [1],
										 "empty": [
										   null,
										   "skipped"
									   ]
									 }`,
			},
		},
		{
			name: "complex list initialization",
			whistle: `def add_field(arg) {
									field: arg
								}`,
			wantValue: valueTest{
				rootMappings: `nested: [1, ["n2"], [["nn3"], "n3"]]
				               functions: [add_field("first"), add_field(add_field("second"))]
											 empty: []
											 empty[]: add_field("not_empty")`,
				wantJSON: `{
										 "nested": [
										   1,
										   [
											   "n2"
											 ],
											 [
											   [
												   "nn3"
												 ],
												 "n3"
											 ]
									   ],
										 "functions": [
										   {
											   "field": "first"
											 },
											 {
											   "field": {
												   "field": "second"
												 }
											 }
										 ],
										 "empty": [
										   {
										     "field": "not_empty"
											 }
										 ]
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
				rootMappings: `var _: root_fields("shouldNotBeInOutput")`,
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
				rootMappings: "result: array(1, 2, 3)",
				wantJSON: `{
										 "result": [1, 2, null, null, null, 3]
									 }`,
			},
		},
		{
			name: "block expression",
			wantValue: valueTest{
				rootMappings: `result: {
					one: "two"
					two: 2
				}`,
				wantJSON: `{
										 "result": {"one": "two", "two": 2}
									 }`,
			},
		},
		{
			name: "block expression pulls from parent env",
			whistle: `def projector(arg) {
			            var v: arg + 1
									block: {
										key: arg
										key2: v
									}
									other_key: arg
								}`,
			wantValue: valueTest{
				rootMappings: `$this: projector(1)`,
				wantJSON: `{
									   "block": {
										 	"key": 1,
											"key2": 2
										 },
										 "other_key": 1
									 }`,
			},
		},
		{
			name: "block expression as an argument",
			whistle: `def projector(arg, other) {
			            key: arg
									other_key: other
								}`,
			wantValue: valueTest{
				rootMappings: `$this: projector({
					blocks: "are fun!"
				}, {thisone: "is inline";})`,
				wantJSON: `{
									   "key": {
										 		"blocks": "are fun!"
										 },
										 "other_key": {
										 		"thisone": "is inline"
										 }
									 }`,
			},
		},
		{
			name: "block expression var semantics",
			wantValue: valueTest{
				rootMappings: `var one: "one"
					nested: {
						read: one
						var one: "two"
						inner: one
					}
					outter: one`,
				wantJSON: `{
									   "nested": {
										 		"read": "one",
												"inner": "two"
										 },
										 "outter": "one"
									 }`,
			},
		},
		{
			name: "field names with spaces",
			whistle: `Hello\ World: "hello"
			          var some\ var: $root.input\ with[0].some\ spaces
			          Test\.field: Test\ Projector(some\ var)
								def Test\ Projector(test\ arg){
									more\ spaces\ \ \ \ everywhere: test\ arg
								}`,
			wantValue: valueTest{
				inputJSON: `{"input with": [{"some spaces" : "hi"}]}`,
				wantJSON: `{
									   "Hello World": "hello",
										 "Test.field": {
										 		"more spaces    everywhere": "hi"
										 }
									 }`,
			},
		},
		{
			name: "field names with fancy chars",
			whistle: `\0123.\-000: "hello"
								var 'моя переменная': $root.123[0].\123X
								'=+2': '"proj\\ector\'"'('моя переменная')
								def '"proj\\ector\'"'(\1ar\ g1){
									\123\\_: \1ar\ g1
									'поле': "field"
									'😊'.status: "whoa"
									var 'def': {
									    'required': "keyword"
									}
									'required': 'def'.'required'
								}`,
			wantValue: valueTest{
				inputJSON: `{"123": [{"123X" : "hi"}]}`,
				wantJSON: `{
									   "0123": {"-000": "hello"},
										 "=+2": {
										 		"123\\_": "hi",
												"поле": "field",
												"😊": {
													"status": "whoa"
												},
												"required": "keyword"
										 }
									 }`,
			},
		},
		{
			name: "arraymod for target",
			whistle: `def arr_mod(a, b, c) {
									[]: a
									[]: b
									[]: c
								}`,
			wantValue: valueTest{
				rootMappings: `arr: arr_mod(1, 2, 3)
											 arr: arr_mod(4, 5, 6)
											 arr2: arr_mod(7, 8, 9)`,
				wantJSON: `{
										 "arr": [1, 2, 3, 4, 5, 6],
										 "arr2": [7, 8, 9]
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
					m, _ := prototext.Marshal(compiled)
					t.Fatalf("executing whistle code yielded unexpected error\nwhistle code:\n%s\nerror: %v\nwhistler: %s", full, err, string(m))
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
