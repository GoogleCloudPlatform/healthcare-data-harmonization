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

package jsonutil

import (
	"encoding/json"
	"flag"
	"fmt"
	"reflect"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */
)

type JSONMetaTestNode struct{}

func (j JSONMetaTestNode) Key() string {
	return ""
}

func (j JSONMetaTestNode) Parent() JSONMetaNode {
	return nil
}

func (j JSONMetaTestNode) Path() string {
	return ""
}

func (j JSONMetaTestNode) ContentString(level int) string {
	return fmt.Sprintf("level %d", level)
}

type JSONTestToken struct{}

func (j JSONTestToken) String() string { return `"test"` }

func (j JSONTestToken) jsonObject() {}

func (j JSONTestToken) Value() JSONToken {
	return j
}

var (
	benchmarkParallelism = flag.Int("benchmark_parallelism", 8, "Number of goroutines to use for conversion.")

	// These are hacks employed to create instances of *JSONToken.
	st  = JSONToken(JSONStr("123"))
	nm  = JSONToken(JSONNum(123))
	bl  = JSONToken(JSONBool(true))
	arr = JSONToken(JSONArr([]JSONToken{st, nm, bl}))
	obj = JSONToken(JSONContainer(map[string]*JSONToken{
		"key1": &st,
		"key2": &nm,
		"key3": &bl,
	}))
)

func TestPath(t *testing.T) {
	tests := []struct {
		name string
		node JSONMetaNode
		want string
	}{
		{
			name: "root has empty path",
			node: JSONMetaContainerNode{JSONMeta: JSONMeta{}},
			want: "",
		},
		{
			name: "single child has valid path",
			node: JSONMetaPrimitiveNode{
				JSONMeta: JSONMeta{
					key: "foo",
					parent: JSONMetaContainerNode{
						JSONMeta: JSONMeta{},
					},
				},
			},
			want: "foo",
		},
		{
			name: "nested child has valid path",
			node: JSONMetaPrimitiveNode{
				JSONMeta: JSONMeta{
					key: "bar",
					parent: JSONMetaContainerNode{
						JSONMeta: JSONMeta{
							key: "foo",
							parent: JSONMetaContainerNode{
								JSONMeta: JSONMeta{},
							},
						},
					},
				},
			},
			want: "foo.bar",
		},
		{
			name: "nested array item has valid path",
			node: JSONMetaPrimitiveNode{
				JSONMeta: JSONMeta{
					key: "[1]",
					parent: JSONMetaArrayNode{
						JSONMeta: JSONMeta{
							key: "foo",
							parent: JSONMetaContainerNode{
								JSONMeta: JSONMeta{},
							},
						},
					},
				},
			},
			want: "foo[1]",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			if got := test.node.Path(); got != test.want {
				t.Errorf("%v.Path() => %q, want %q", test.node, got, test.want)
			}
		})
	}
}

func TestNodeToToken(t *testing.T) {
	tests := []struct {
		name string
		node JSONMetaNode
		want JSONToken
	}{
		{
			name: "container",
			node: JSONMetaContainerNode{
				JSONMeta: JSONMeta{key: "k", parent: nil},
				Children: map[string]JSONMetaNode{
					"key1": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key1", parent: nil}, Value: st.(JSONPrimitive)},
					"key2": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key2", parent: nil}, Value: nm.(JSONPrimitive)},
					"key3": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key3", parent: nil}, Value: bl.(JSONPrimitive)},
				},
			},
			want: JSONContainer(map[string]*JSONToken{
				"key1": &st,
				"key2": &nm,
				"key3": &bl,
			}),
		},
		{
			name: "array",
			node: JSONMetaArrayNode{
				JSONMeta: JSONMeta{key: "k", parent: nil},
				Items: []JSONMetaNode{
					JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[0]", parent: nil}, Value: st.(JSONPrimitive)},
					JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[1]", parent: nil}, Value: nm.(JSONPrimitive)},
					JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[2]", parent: nil}, Value: bl.(JSONPrimitive)},
				},
			},
			want: JSONArr([]JSONToken{st, nm, bl}),
		},
		{
			name: "str",
			node: JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[0]", parent: nil}, Value: st.(JSONPrimitive)},
			want: st,
		},
		{
			name: "num",
			node: JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[1]", parent: nil}, Value: nm.(JSONPrimitive)},
			want: nm,
		},
		{
			name: "bool",
			node: JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[2]", parent: nil}, Value: bl.(JSONPrimitive)},
			want: bl,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NodeToToken(test.node)
			if err != nil {
				t.Fatalf("NodeToToken(%v) returned error: %v", test.node, err)
			}
			if !reflect.DeepEqual(got, test.want) {
				t.Errorf("NodeToToken(%v) => %v, want %v", test.node, got, test.want)
			}
		})
	}
}

func TestNodeToToken_Errors(t *testing.T) {
	node := JSONMetaTestNode{}

	tests := []struct {
		name string
		node JSONMetaNode
	}{
		{
			name: "top level",
			node: node,
		},
		{
			name: "in a container",
			node: JSONMetaContainerNode{Children: map[string]JSONMetaNode{"key": node}},
		},
		{
			name: "in an array",
			node: JSONMetaArrayNode{Items: []JSONMetaNode{node}},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := NodeToToken(test.node)
			if err == nil {
				t.Errorf("NodeToToken(%v) expects error", test.node)
			}
		})
	}
}

func TestTokenToNode(t *testing.T) {
	arr := JSONMetaArrayNode{JSONMeta: JSONMeta{key: "", parent: nil}}
	arr.Items = []JSONMetaNode{
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[0]", parent: &arr}, Value: st.(JSONPrimitive)},
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[1]", parent: &arr}, Value: nm.(JSONPrimitive)},
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[2]", parent: &arr}, Value: bl.(JSONPrimitive)},
	}
	obj := JSONMetaContainerNode{JSONMeta: JSONMeta{key: "", parent: nil}}
	obj.Children = map[string]JSONMetaNode{
		"key1": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key1", parent: &obj}, Value: st.(JSONPrimitive)},
		"key2": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key2", parent: &obj}, Value: nm.(JSONPrimitive)},
		"key3": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key3", parent: &obj}, Value: bl.(JSONPrimitive)},
	}

	tests := []struct {
		name  string
		token JSONToken
		want  JSONMetaNode
	}{
		{
			name: "container",
			want: obj,
			token: JSONContainer(map[string]*JSONToken{
				"key1": &st,
				"key2": &nm,
				"key3": &bl,
			}),
		},
		{
			name:  "array",
			want:  arr,
			token: JSONArr([]JSONToken{st, nm, bl}),
		},
		{
			name:  "str",
			want:  JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "", parent: nil}, Value: st.(JSONPrimitive)},
			token: st,
		},
		{
			name:  "num",
			want:  JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "", parent: nil}, Value: nm.(JSONPrimitive)},
			token: nm,
		},
		{
			name:  "bool",
			want:  JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "", parent: nil}, Value: bl.(JSONPrimitive)},
			token: bl,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := TokenToNode(test.token)
			if err != nil {
				t.Fatalf("TokenToNode(%v) returned error: %v", test.token, err)
			}
			if !cmp.Equal(got, test.want, cmpopts.IgnoreUnexported(JSONMeta{})) {
				t.Errorf("TokenToNode(%v) => %v, want %v", test.token, got, test.want)
			}
		})
	}
}

func TestTokenToNode_Errors(t *testing.T) {
	token := JSONToken(JSONTestToken{})

	tests := []struct {
		name  string
		token JSONToken
	}{
		{
			name:  "top level",
			token: token,
		},
		{
			name:  "in an array",
			token: JSONArr([]JSONToken{token}),
		},
		{
			name:  "in a container",
			token: JSONContainer(map[string]*JSONToken{"key1": &token}),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := TokenToNode(test.token)
			if err == nil {
				t.Errorf("TokenToNode(%v) expects error", test.token)
			}
		})
	}
}

func TestTokenToNodeThenBack(t *testing.T) {
	token := JSONContainer(map[string]*JSONToken{"key1": &arr, "key2": &obj})
	got, err := TokenToNode(token)
	if err != nil {
		t.Fatalf("TokenToNode(%v) returned error: %v", token, err)
	}
	gotToken, err := NodeToToken(got)
	if err != nil {
		t.Fatalf("NodeToToken(%v) returned error: %v", gotToken, err)
	}
	if !reflect.DeepEqual(gotToken, token) {
		t.Errorf("NodeToToken(%v) => %v, want %v", got, gotToken, token)
	}
}

func TestNodeToTokenThenBack(t *testing.T) {
	node := complexNode()
	got, err := NodeToToken(node)
	if err != nil {
		t.Fatalf("NodeToToken(%v) returned error: %v", got, err)
	}
	gotNode, err := TokenToNode(got)
	if err != nil {
		t.Fatalf("TokenToNode(%v) returned error: %v", got, err)
	}
	if !cmp.Equal(gotNode, node, cmpopts.IgnoreUnexported(JSONMeta{})) {
		t.Errorf("TokenToNode(%v) => %v, want %v", got, gotNode, node)
	}
}

func TestGetNodeField(t *testing.T) {
	msg := json.RawMessage(`{
	  "id":"an_id",
	  "b":true,
	  "val":123,
	  "code":{
	    "system":"code_system",
	    "value":"code_value"
	  },
	  "name":[
	    "first_name",
	    "second_name",
	    "third_name",
	    "fourth_name"
	  ],
	  "address":[
	    {
	      "city":"waterloo",
	      "country":"canada"
	    }
	  ],
	  "nested":[
	    [
	      {
	        "foo":"bar"
	      },
	      99
	    ]
	  ],
		"expansion": [
			{
					"red": 1,
					"green": [{ "cyan": "leaf1" }, { "cyan": "leaf2" }],
					"blue": [1, 2, 3]
			},
			{
					"red": 2,
					"green": [{ "cyan": "leaf3" }, { "cyan": "leaf4" }],
					"blue": [101, 102, 103],
					"purple": [{ "sometimesmissing": "leaf1", "explicitnull": null }, { "explicitnull": "leaf2" }]
			}
		]
	}`)
	jn, err := TokenToNode(mustParseJSON(t, msg))
	if err != nil {
		t.Fatalf("error creating test node: %v", err)
	}

	tests := []struct {
		name, field string
		want        JSONToken
	}{
		{
			name:  "non nested field",
			field: "id",
			want:  JSONStr("an_id"),
		},
		{
			name:  "nested field",
			field: "code.value",
			want:  JSONStr("code_value"),
		},
		{
			name:  "repeated field",
			field: "name[0]",
			want:  JSONStr("first_name"),
		},
		{

			name:  "nested repeated field",
			field: "address[0].city",
			want:  JSONStr("waterloo"),
		},
		{
			name:  "repeated repeated field",
			field: "nested[0][1]",
			want:  JSONNum(99),
		},
		{
			name:  "nested repeated repeated field",
			field: "nested[0][0].foo",
			want:  JSONStr("bar"),
		},
		{
			name:  "int val",
			field: "val",
			want:  JSONNum(123),
		},
		{
			name:  "bool val",
			field: "b",
			want:  JSONBool(true),
		},
		{
			name:  "single [*] expansion",
			field: "expansion[*].red",
			want:  mustParseJSON(t, json.RawMessage(`[1, 2]`)),
		},
		{
			name:  "multiple [*] expansions",
			field: "expansion[*].green[*].cyan",
			want:  mustParseJSON(t, json.RawMessage(`["leaf1", "leaf2", "leaf3", "leaf4"]`)),
		},
		{
			name:  "multiple [*] expansions to primitives",
			field: "expansion[*].blue[*]",
			want:  mustParseJSON(t, json.RawMessage(`[1, 2, 3, 101, 102, 103]`)),
		},
		{
			name:  "single [*] expansion on nested arrays does not unnest",
			field: "nested[*]",
			want: mustParseJSON(t, json.RawMessage(`[
	    [
	      {
	        "foo":"bar"
	      },
	      99
	    ]
	  ]`)),
		},
		{
			name:  "double [*] expansion on nested arrays unnests",
			field: "nested[*][*]",
			want: mustParseJSON(t, json.RawMessage(`[
	      {
	        "foo":"bar"
	      },
	      99
	    ]`)),
		},
		{
			name:  "single [*] expansions on jagged array",
			field: "expansion[*].purple",
			want:  mustParseJSON(t, json.RawMessage(`[null, [{ "sometimesmissing": "leaf1", "explicitnull": null }, { "explicitnull": "leaf2" }]]`)),
		},
		{
			name:  "multiple [*] expansions on jagged array",
			field: "expansion[*].purple[*].sometimesmissing",
			want:  mustParseJSON(t, json.RawMessage(`[null, "leaf1", null]`)),
		},
		{
			name:  "multiple [*] expansions on jagged array with explicit nulls",
			field: "expansion[*].purple[*].explicitnull",
			want:  mustParseJSON(t, json.RawMessage(`[null, null, "leaf2"]`)),
		},
		{
			name:  "non existent field",
			field: "code.text",
		},
		{
			name:  "non existent field and non existent parent",
			field: "code.text.value",
		},
		{
			name:  "non existent field - repeated",
			field: "code.repeated[0]",
		},
		{
			name:  "non existent field and non existent parent - repeated",
			field: "code.parent.repeated[0]",
		},
		{
			name:  "out of bounds array item",
			field: "name[100]",
		},
		{
			name:  "field on out of bounds array item",
			field: "name[100].foo",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := GetNodeField(jn, test.field)
			if err != nil {
				t.Fatalf("GetNodeField(%v, %v) returned unexpected error %v", string(msg), test.field, err)
			}

			switch gotT := got.(type) {
			case nil:
				if test.want != nil {
					t.Errorf("GetNodeField(%v, %v) = %v, want nil", string(msg), test.field, gotT)
				}
			case JSONMetaPrimitiveNode:
				gp := gotT.Value

				if !cmp.Equal(gp, test.want) {
					t.Errorf("GetNodeField(%v, %v) = %v, want %v", string(msg), test.field, got, test.want)
				}
			default:
				gotTkn, err := NodeToToken(got)
				if err != nil {
					t.Fatalf("failed to convert got value %v to token: %v", got, err)
				}

				if diff := cmp.Diff(test.want, gotTkn); diff != "" {
					t.Errorf("GetNodeField(%v, %v) unexpected result (-want/+got):\n%s", string(msg), test.field, diff)
				}
			}
		})
	}
}

func TestGetNodeField_EmptyFieldReturnsRoot(t *testing.T) {
	msg := json.RawMessage(`{
	  "id":"an_id",
	  "val":123,
	  "code":{
	    "system":"code_system",
	    "value":"code_value"
	  },
	  "name":[
	    "first_name",
	    "second_name"
	  ],
	  "address":[
	    {
	      "city":"waterloo",
	      "country":"canada"
	    }
	  ]
	}`)
	jn, err := TokenToNode(mustParseJSON(t, msg))
	if err != nil {
		t.Fatalf("error creating test node: %v", err)
	}
	t.Run("", func(t *testing.T) {
		got, err := GetNodeField(jn, "")
		if err != nil {
			t.Fatalf("GetNodeField(%v, '') returned unexpected error %v", string(msg), err)
		}

		if !cmp.Equal(got, jn, cmpopts.IgnoreUnexported(JSONMeta{})) {
			t.Errorf("GetNodeField(%v, '') = %v, want %v", string(msg), got, jn)
		}
	})
}

func TestGetNodeField_Errors(t *testing.T) {
	msg := json.RawMessage(`{
	  "id":"an_id",
	  "val":123,
	  "code":{
	    "system":"code_system",
	    "value":"code_value"
	  },
	  "name":[
	    "first_name",
	    "second_name"
	  ],
	  "address":[
	    {
	      "city":"waterloo",
	      "country":"canada"
	    }
	  ]
	}`)
	jn, err := TokenToNode(mustParseJSON(t, msg))
	if err != nil {
		t.Fatalf("error creating test node: %v", err)
	}
	tests := []struct {
		name, field string
	}{
		{
			name:  "invalid brackets",
			field: "name[0]]",
		},
		{
			name:  "incomplete brackets",
			field: "name[0",
		},
		{
			name:  "indexing primitive",
			field: "id[0]",
		},
		{
			name:  "indexing object",
			field: "code[0]",
		},
		{
			name:  "array projection through primitive",
			field: "id[*]",
		},
		{
			name:  "keying into primitive",
			field: "id.foo.bar",
		},
		{
			name:  "non-numeric index",
			field: "name[asd]",
		},
		{
			name:  "sequence of dots",
			field: "code..system",
		},
		{
			name:  "invalid characters",
			field: "code/system",
		},
		{
			name:  "negative index",
			field: "name[-1]",
		},
		{
			name:  "array projection through object",
			field: "code[*]",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := GetNodeField(jn, test.field)
			if err == nil {
				t.Fatalf("GetNodeField(%v, %v) did not return expected error", string(msg), test.field)
			}
		})
	}
}

func complexNode() JSONMetaNode {
	node := JSONMetaContainerNode{JSONMeta: JSONMeta{}}
	arr := JSONMetaArrayNode{JSONMeta: JSONMeta{key: "key1", parent: &node}}
	arr.Items = []JSONMetaNode{
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[0]", parent: &arr}, Value: st.(JSONPrimitive)},
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[1]", parent: &arr}, Value: nm.(JSONPrimitive)},
		JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "[2]", parent: &arr}, Value: bl.(JSONPrimitive)},
	}
	obj := JSONMetaContainerNode{JSONMeta: JSONMeta{key: "key2", parent: &node}}
	obj.Children = map[string]JSONMetaNode{
		"key1": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key1", parent: &obj}, Value: st.(JSONPrimitive)},
		"key2": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key2", parent: &obj}, Value: nm.(JSONPrimitive)},
		"key3": JSONMetaPrimitiveNode{JSONMeta: JSONMeta{key: "key3", parent: &obj}, Value: bl.(JSONPrimitive)},
	}
	node.Children = map[string]JSONMetaNode{"key1": arr, "key2": obj}
	return node
}

func complexToken() JSONToken {
	return JSONContainer(map[string]*JSONToken{
		"key1": &arr,
		"key2": &obj,
		"key3": &st,
		"key4": &nm,
		"key5": &bl,
	})
}

func BenchmarkTokenToNode(b *testing.B) {
	token := complexToken()
	for i := 0; i < b.N; i++ {
		if _, err := TokenToNode(token); err != nil {
			b.Errorf("TokenToNode(%v) returned error: %v", token, err)
		}
	}
}

func BenchmarkNodeToToken(b *testing.B) {
	node := complexNode()
	for i := 0; i < b.N; i++ {
		if _, err := NodeToToken(node); err != nil {
			b.Errorf("NodeToToken(%v) returned error: %v", node, err)
		}
	}
}

func BenchmarkTokenToNode_Parallel(b *testing.B) {
	token := complexToken()
	count := b.N / *benchmarkParallelism
	c := make(chan struct{})
	for i := 0; i < *benchmarkParallelism; i++ {
		go func() {
			for i := 0; i < count; i++ {
				if _, err := TokenToNode(token); err != nil {
					b.Errorf("TokenToNode(%v) returned error: %v", token, err)
				}
			}
			c <- struct{}{}
		}()
	}
	for i := 0; i < *benchmarkParallelism; i++ {
		<-c
	}
}

func BenchmarkNodeToToken_Parallel(b *testing.B) {
	node := complexNode()
	count := b.N / *benchmarkParallelism
	c := make(chan struct{})
	for i := 0; i < *benchmarkParallelism; i++ {
		go func() {
			for i := 0; i < count; i++ {
				if _, err := NodeToToken(node); err != nil {
					b.Errorf("NodeToToken(%v) returned error: %v", node, err)
				}
			}
			c <- struct{}{}
		}()
	}
	for i := 0; i < *benchmarkParallelism; i++ {
		<-c
	}
}
