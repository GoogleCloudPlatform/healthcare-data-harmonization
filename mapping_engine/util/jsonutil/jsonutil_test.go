// Copyright 2020 Google LLC
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
	"encoding/hex"
	"encoding/json"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

func mustParseJSON(t *testing.T, message json.RawMessage) JSONToken {
	t.Helper()

	jt, err := UnmarshalJSON(message)
	if err != nil {
		t.Fatalf("JSON unmarshal error for %s: %v", message, err)
	}

	return jt
}

func TestUnmarshalJSON(t *testing.T) {
	// JSONContainer needs a reference, and it is not possible to create
	// a reference of a primitive type on the fly.
	st := JSONToken(JSONStr("val"))

	tests := []struct {
		name string
		in   json.RawMessage
		want JSONToken
	}{
		{
			"empty",
			json.RawMessage(``),
			nil,
		},
		{
			"empty - whitespace",
			json.RawMessage(`        `),
			nil,
		},
		{
			"nil",
			json.RawMessage(`nil`),
			nil,
		},
		{
			"string",
			json.RawMessage(`"test"`),
			JSONStr("test"),
		},
		{
			"string - whitespace",
			json.RawMessage(`    "test"    `),
			JSONStr("test"),
		},
		{
			"int - positive",
			json.RawMessage(`10`),
			JSONNum(10),
		},
		{
			"int - negative",
			json.RawMessage(`-10`),
			JSONNum(-10),
		},
		{
			"float",
			json.RawMessage(`10.15`),
			JSONNum(10.15),
		},
		{
			"bool - true",
			json.RawMessage(`true`),
			JSONBool(true),
		},
		{
			"bool - false",
			json.RawMessage(`false`),
			JSONBool(false),
		},
		{
			"array",
			json.RawMessage(`[1, 2, 3]`),
			JSONArr{JSONNum(1), JSONNum(2), JSONNum(3)},
		},
		{
			"container",
			json.RawMessage(`{"key":"val"}`),
			JSONContainer(map[string]*JSONToken{"key": &st}),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := UnmarshalJSON(test.in)
			if err != nil {
				t.Fatalf("UnmarshalJSON(%v) returned unexpected err %v", test.in, err)
			}
			if diff := cmp.Diff(got, test.want); diff != "" {
				t.Errorf("UnmarshalJSON(%v) = %v, want: %v \ndiff:%v", test.in, got, test.want, diff)
			}
		})
	}
}

func TestGetField(t *testing.T) {
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
		],
		"dot.ted": { "field": "escaped" },
		"dash-ed": { "field": "dashing!" },
		"last.": { "field": 1 },
		"last..": { "field": 2, ".field": 3 },
		"slash\\": { "field": 4 },
		"dot": { "ted": {"field": "unescaped" } }
	}`)
	j := mustParseJSON(t, msg)

	tests := []struct {
		name, field string
		want        JSONToken
	}{
		{
			"non nested field",
			"id",
			JSONStr("an_id"),
		},
		{
			"nested field",
			"code.value",
			JSONStr("code_value"),
		},
		{
			"repeated field",
			"name[0]",
			JSONStr("first_name"),
		},
		{
			"nested repeated field",
			"address[0].city",
			JSONStr("waterloo"),
		},
		{
			"repeated repeated field",
			"nested[0][1]",
			JSONNum(99),
		},
		{
			"nested repeated repeated field",
			"nested[0][0].foo",
			JSONStr("bar"),
		},
		{
			"int val",
			"val",
			JSONNum(123),
		},
		{
			"bool val",
			"b",
			JSONBool(true),
		},
		{
			"array",
			"name",
			JSONArr{JSONStr("first_name"), JSONStr("second_name"), JSONStr("third_name"), JSONStr("fourth_name")},
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
			"non existent field",
			"code.text",
			nil,
		},
		{
			"non existent array item",
			"name[100]",
			nil,
		},
		{
			"field on non existent array item",
			"name[100].field",
			nil,
		},
		{
			"non existent field and non existent parent",
			"code.text.value",
			nil,
		},
		{
			"non existent field - repeated",
			"code.repeated[0]",
			nil,
		},
		{
			"non existent field - appending repeated",
			"code.repeated[]",
			nil,
		},
		{
			"non existent field and non existent parent - repeated",
			"code.parent.repeated[0]",
			nil,
		},
		{
			"empty field is root",
			"",
			j,
		},
		{
			"path with escaped dot",
			`dot\.ted.field`,
			JSONStr("escaped"),
		},
		{
			"path with escaped trailing dot",
			`last\..field`,
			JSONNum(1),
		},
		{
			"path with two escaped trailing dots",
			`last\.\..field`,
			JSONNum(2),
		},
		{
			"path with escaped trailing and leading dots",
			`last\.\..\.field`,
			JSONNum(3),
		},
		{
			"path with unescaped dot",
			"dot.ted.field",
			JSONStr("unescaped"),
		},
		{
			"path with escaped slash",
			`slash\\.field`,
			JSONNum(4),
		},
		{
			"path with escaped everything",
			`\d\o\t\.\t\e\d.\f\i\e\l\d`,
			JSONStr("escaped"),
		},
		{
			"path with dash",
			"dash-ed.field",
			JSONStr("dashing!"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := GetField(j, test.field)
			if err != nil {
				t.Fatalf("GetField(%v, %v) returned unexpected error %v", string(msg), test.field, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("GetField(%v, %v) = %v, want %v", string(msg), test.field, got, test.want)
			}
		})
	}
}

func TestGetField_Errors(t *testing.T) {
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
	j := mustParseJSON(t, msg)

	tests := []struct {
		name, field string
	}{
		{
			"invalid brackets",
			"name[0]]",
		},
		{
			"incomplete brackets",
			"name[0",
		},
		{
			"indexing primitive",
			"id[0]",
		},
		{
			"keying into primitive",
			"id.foo.bar",
		},
		{
			"non-numeric index",
			"name[asd]",
		},
		{
			"sequence of dots",
			"code..system",
		},
		{
			"invalid characters",
			"code/system",
		},
		{
			"negative index",
			"name[-1]",
		},
		{
			"array without index",
			"name[]",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := GetField(j, test.field)
			if err == nil {
				t.Fatalf("GetField(%v, %v) did not return expected error", string(msg), test.field)
			}
		})
	}
}

func TestHasField(t *testing.T) {
	testMsg := json.RawMessage(`{
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
		],
		"dot.ted": { "field": "escaped" },
		"dash-ed": { "field": "dashing!" },
		"last.": { "field": 1 },
		"last..": { "field": 2, ".field": 3 },
		"slash\\": { "field": 4 },
		"dot": { "ted": {"field": "unescaped" } }
	}`)
	tests := []struct {
		name, field string
		want        bool
		wantErr     bool
	}{
		{
			name:  "non nested field",
			field: "id",
			want:  true,
		},
		{
			name:  "nested field",
			field: "code.value",
			want:  true,
		},
		{
			name:  "repeated field",
			field: "name",
			want:  true,
		},
		{
			name:    "repeated field with brackets",
			field:   "name[]",
			wantErr: true,
		},
		{
			name:  "repeated field with index",
			field: "name[1]",
			want:  true,
		},
		{
			name:    "nested repeated field",
			field:   "address.city",
			wantErr: true,
		},
		{
			name:    "nested repeated field with brackets",
			field:   "address[].city",
			wantErr: true,
		},
		{
			name:  "nested repeated field with index",
			field: "address[0].city",
			want:  true,
		},
		{
			name:  "non existent field",
			field: "first_name",
			want:  false,
		},
		{
			name:  "non existent nested field",
			field: "code.text",
			want:  false,
		},
		{
			name:    "non repeating nested field",
			field:   "code.system[0]",
			wantErr: true,
		},
		{
			name:  "non existent field and non existent parent",
			field: "code.new_field.value",
			want:  false,
		},
		{
			name:  "non existent field - repeated",
			field: "code.repeated[0]",
			want:  false,
		},
		{
			name:  "non existent field and non existent parent - repeated",
			field: "code.parent.repeated[0]",
			want:  false,
		},
		{
			name:  "non existent field and non existent repeated parent - repeated",
			field: "code.repeated_parent[0].repeated[0]",
			want:  false,
		},
		{
			name:  "non existent field and non existent repeated parent",
			field: "code.repeated_parent[0].field",
			want:  false,
		},
		{
			name:  "list out of bounds",
			field: "name[5]",
			want:  false,
		},
		{
			name:  "field on out of bounds element",
			field: "name[5].field",
			want:  false,
		},
		{
			name:  "root as empty",
			field: "",
			want:  true, // should be error?
		},
		{
			name:  "root as dot",
			field: ".",
			want:  true, // should be error?
		},
		{
			name:  "repeated repeated field",
			field: "nested[0][1]",
			want:  true,
		},
		{
			name:  "nested repeated repeated field",
			field: "nested[0][0].foo",
			want:  true,
		},
		{
			name:  "single [*] expansion",
			field: "expansion[*].red",
			want:  true,
		},
		{
			name:  "multiple [*] expansions",
			field: "expansion[*].green[*].cyan",
			want:  true,
		},
		{
			name:  "single [*] expansion on nested arrays",
			field: "nested[*]",
			want:  true,
		},
		{
			name:  "double [*] expansion on nested arrays",
			field: "nested[*][*]",
			want:  true,
		},
		{
			name:  "single [*] expansions on jagged array",
			field: "expansion[*].purple",
			want:  true,
		},
		{
			name:  "multiple [*] expansions on jagged array",
			field: "expansion[*].purple[*].sometimesmissing",
			want:  true,
		},
		{
			name:  "multiple [*] expansions on jagged array with explicit nulls",
			field: "expansion[*].purple[*].explicitnull",
			want:  true,
		},
		{
			name:  "path with escaped dot",
			field: `dot\.ted.field`,
			want:  true,
		},
		{
			name:  "path with escaped trailing dot",
			field: `last\..field`,
			want:  true,
		},
		{
			name:  "path with two escaped trailing dots",
			field: `last\.\..field`,
			want:  true,
		},
		{
			name:  "path with escaped trailing and leading dots",
			field: `last\.\..\.field`,
			want:  true,
		},
		{
			name:  "path with unescaped dot",
			field: "dot.ted.field",
			want:  true,
		},
		{
			name:  "path with escaped slash",
			field: `slash\\.field`,
			want:  true,
		},
		{
			name:  "path with escaped everything",
			field: `\d\o\t\.\t\e\d.\f\i\e\l\d`,
			want:  true,
		},
		{
			name:  "path with dash",
			field: "dash-ed.field",
			want:  true,
		},
	}
	jOrig := mustParseJSON(t, testMsg)
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := HasField(jOrig, test.field)
			if !test.wantErr && err != nil {
				t.Errorf("HasField(%v, %v) returned unexpected error %v", jOrig, test.field, err)
			}
			if test.wantErr && err == nil {
				t.Errorf("HasField(%v, %v) returned unexpected nil error", jOrig, test.field)
			}
			if !test.wantErr && got != test.want {
				t.Errorf("HasField(%v, %v) => %v, wanted %v", jOrig, test.field, got, test.want)
			}
		})
	}
}

func TestSetField(t *testing.T) {
	testMsg := json.RawMessage(`{
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
	tests := []struct {
		name, field string
		val         JSONToken
		overwrite   bool
		want        json.RawMessage
	}{
		{
			"non nested field",
			"id",
			JSONStr("new_id"),
			true,
			json.RawMessage(`{"id":"new_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"nested field",
			"code.value",
			JSONStr("new_code_value"),
			true,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"new_code_value"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"repeated field",
			"name[0]",
			JSONStr("new_first_name"),
			true,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["new_first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"nested repeated field",
			"address[0].city",
			JSONStr("kitchener"),
			true,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name"],"address":[{"city":"kitchener","country":"canada"}]}`),
		},
		{
			"append to repeated field",
			"name[]",
			JSONStr("third_name"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name","third_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"append to nested repeated field",
			"address[].city",
			JSONStr("kitchener"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"},{"city":"kitchener"}]}`),
		},
		{
			"int val",
			"val",
			JSONNum(321),
			true,
			json.RawMessage(`{"id":"an_id","val":321,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field",
			"code.text",
			JSONStr("test_text"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","text":"test_text"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field and non existent parent",
			"code.new_field.value",
			JSONStr("new_field_value"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","new_field":{"value":"new_field_value"}},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field - repeated",
			"code.repeated[0]",
			JSONStr("new_repeated"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","repeated":["new_repeated"]},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field and non existent parent - repeated",
			"code.parent.repeated[0]",
			JSONStr("new_parent_repeated"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","parent":{"repeated":["new_parent_repeated"]}},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field and non existent repeated parent - repeated",
			"code.repeated_parent[0].repeated[0]",
			JSONStr("new_repeated_parent_repeated"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","repeated_parent":[{"repeated":["new_repeated_parent_repeated"]}]},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"non existent field and non existent repeated parent",
			"code.repeated_parent[0].field",
			JSONStr("new_repeated_parent_repeated"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value","repeated_parent":[{"field":"new_repeated_parent_repeated"}]},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"extending repeated",
			"name[3]",
			JSONStr("new_name"),
			false,
			json.RawMessage(`{"id":"an_id","val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name", null, "new_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
		{
			"root as empty",
			"",
			mustParseJSON(t, json.RawMessage(`{"foo": "bar"}`)),
			true,
			json.RawMessage(`{"foo": "bar"}`),
		},
		{
			"root as dot",
			".",
			mustParseJSON(t, json.RawMessage(`{"foo": "bar"}`)),
			true,
			json.RawMessage(`{"foo": "bar"}`),
		},
		{
			"complex member",
			"id",
			mustParseJSON(t, json.RawMessage(`{"foo": "bar"}`)),
			true,
			json.RawMessage(`{"id":{"foo": "bar"},"val":123,"code":{"system":"code_system","value":"code_value"},"name":["first_name","second_name"],"address":[{"city":"waterloo","country":"canada"}]}`),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {

			jOrig := mustParseJSON(t, testMsg)
			err := SetField(test.val, test.field, &jOrig, test.overwrite)
			if err != nil {
				t.Errorf("SetField(%v, %v, %v, %v) returned unexpected error %v", test.val, test.field, jOrig, test.overwrite, err)
			}

			jWant := mustParseJSON(t, test.want)

			if diff := cmp.Diff(jWant, jOrig); diff != "" {
				t.Errorf("SetField(%v, %v, %v, %v) => diff -%v +%v\n%s", test.val, test.field, string(testMsg), test.overwrite, jWant, jOrig, diff)
			}
		})
	}
}

func TestSetField_Arrays(t *testing.T) {
	tests := []struct {
		name      string
		msg       json.RawMessage
		values    map[string]interface{}
		overwrite bool
		want      json.RawMessage
	}{
		{
			name: "new array",
			msg:  json.RawMessage(`{"existing":[0,false]}`),
			values: map[string]interface{}{
				"nametwo[0]": 123,
				"nametwo[1]": true,
			},
			overwrite: true,
			want:      json.RawMessage(`{"existing":[0,false],"nametwo":[123,true]}`),
		},
		{
			name: "existing array",
			msg:  json.RawMessage(`{"existing":[0,false]}`),
			values: map[string]interface{}{
				"existing[0]": 123,
				"existing[1]": "foo",
				"existing[3]": "bar",
			},
			overwrite: true,
			want:      json.RawMessage(`{"existing":[123,"foo",null,"bar"]}`),
		},
		{
			name: "nested array",
			msg:  json.RawMessage(`{"existing":[0,false]}`),
			values: map[string]interface{}{
				"existing[0]": []interface{}{"foo", 1},
			},
			overwrite: true,
			want:      json.RawMessage(`{"existing":[["foo",1],false]}`),
		},
		{
			name: "array append to array",
			msg:  json.RawMessage(`{"existing":[0, false]}`),
			values: map[string]interface{}{
				"existing[]": []interface{}{1, true},
			},
			overwrite: false,
			want:      json.RawMessage(`{"existing":[0,false,1,true]}`),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			jOrig := mustParseJSON(t, test.msg)

			for k, v := range test.values {
				jv, err := unmarshaledToJSONToken(v)
				if err != nil {
					t.Fatalf("can't convert value %v to json: %v", v, err)
				}

				if err := SetField(jv, k, &jOrig, test.overwrite); err != nil {
					t.Errorf("SetField(%v, %v, %v, %v) returned unexpected error %v", k, v, jOrig, test.overwrite, err)
				}
			}

			jWant := mustParseJSON(t, test.want)

			if diff := cmp.Diff(jWant, jOrig); diff != "" {
				t.Errorf("After multiple sets, message was differed from want %s", diff)
			}
		})
	}
}

func TestMerge(t *testing.T) {
	tests := []struct {
		name                            string
		src                             json.RawMessage
		dest                            json.RawMessage
		want                            json.RawMessage
		wantFailIfFoOW, overwriteArrays bool
	}{
		{
			name: "containers of primitives",
			src:  json.RawMessage(`{"foo":"bar"}`),
			dest: json.RawMessage(`{"baz":"hi"}`),
			want: json.RawMessage(`{"foo":"bar","baz":"hi"}`),
		},
		{
			name: "nested containers",
			src:  json.RawMessage(`{"foo":{"bar":"baz"}}`),
			dest: json.RawMessage(`{"baz":"hi","foo":{"qux":1}}`),
			want: json.RawMessage(`{"foo":{"bar":"baz", "qux":1},"baz":"hi"}`),
		},
		{
			name: "arrays of primitives",
			src:  json.RawMessage(`[1, 2, 3]`),
			dest: json.RawMessage(`["foo", "bar"]`),
			want: json.RawMessage(`["foo", "bar", 1, 2, 3]`),
		},
		{
			name: "arrays of containers",
			src:  json.RawMessage(`[{"foo": true}, {"foo": false}]`),
			dest: json.RawMessage(`[{"foo": 1}, {"foo": 2}]`),
			want: json.RawMessage(`[{"foo": 1}, {"foo": 2}, {"foo": true}, {"foo": false}]`),
		},
		{
			name: "containers with arrays",
			src:  json.RawMessage(`{"foo": ["red_foo", "blue_foo"]}`),
			dest: json.RawMessage(`{"foo": ["one_foo", "two_foo"]}`),
			want: json.RawMessage(`{"foo": ["one_foo", "two_foo", "red_foo", "blue_foo"]}`),
		},
		{
			name:           "overlapping containers of primitives",
			src:            json.RawMessage(`{"foo":"bar"}`),
			dest:           json.RawMessage(`{"foo":"hi"}`),
			want:           json.RawMessage(`{"foo":"bar"}`),
			wantFailIfFoOW: true,
		},
		{
			name:           "overlapping containers of primitive and array",
			src:            json.RawMessage(`{"foo":"bar"}`),
			dest:           json.RawMessage(`{"foo":[1, 2]}`),
			want:           json.RawMessage(`{"foo":"bar"}`),
			wantFailIfFoOW: true,
		},
		{
			name:           "overlapping containers of primitive and container",
			src:            json.RawMessage(`{"foo":"bar"}`),
			dest:           json.RawMessage(`{"foo":{"bar":"baz", "qux":1}}`),
			want:           json.RawMessage(`{"foo":"bar"}`),
			wantFailIfFoOW: true,
		},
		{
			name:           "nil src",
			dest:           json.RawMessage(`{"foo":{"bar":"baz", "qux":1}}`),
			want:           json.RawMessage(``),
			wantFailIfFoOW: true,
		},
		{
			name:           "nil value",
			src:            json.RawMessage(`{"foo":null}`),
			dest:           json.RawMessage(`{"foo":{"bar":"baz", "qux":1}, "quz":"blurb"}`),
			want:           json.RawMessage(`{"foo":null, "quz":"blurb"}`),
			wantFailIfFoOW: true,
		},
		{
			name: "overwrite empty container with primitive",
			src:  json.RawMessage(`"foo"`),
			dest: json.RawMessage(`{}`),
			want: json.RawMessage(`"foo"`),
		},
		{
			name: "overwrite empty array with primitive",
			src:  json.RawMessage(`"foo"`),
			dest: json.RawMessage(`[]`),
			want: json.RawMessage(`"foo"`),
		},
		{
			name: "overwrite nil with primitive",
			src:  json.RawMessage(`"foo"`),
			dest: json.RawMessage(`nil`),
			want: json.RawMessage(`"foo"`),
		},
		{
			name: "overwrite empty JSONArr with JSONContainer",
			src:  json.RawMessage(`{"foo":{"key": "bar"}}`),
			dest: json.RawMessage(`[]`),
			want: json.RawMessage(`{"foo":{"key": "bar"}}`),
		},
		{
			name: "overwrite empty JSONContainer with JSONArr",
			src:  json.RawMessage(`[{"foo":{"key": "bar"}}]`),
			dest: json.RawMessage(`{}`),
			want: json.RawMessage(`[{"foo":{"key": "bar"}}]`),
		},
		{
			name:            "array overwriting",
			src:             json.RawMessage(`{"foo":["bar"]}`),
			dest:            json.RawMessage(`{"foo":[1, 2]}`),
			want:            json.RawMessage(`{"foo":["bar"]}`),
			overwriteArrays: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			for _, foow := range []bool{true, false} {
				dest := mustParseJSON(t, test.dest)
				src := mustParseJSON(t, test.src)

				err := Merge(src, &dest, foow, test.overwriteArrays)

				wantErr := test.wantFailIfFoOW && foow

				if err != nil && !wantErr {
					t.Errorf("Merge(%v, %v, %v) => unexpected error %v", src, string(test.dest), foow, err)
				}
				if err == nil && wantErr {
					t.Errorf("Merge(%v, %v, %v) => %v but wanted error", src, string(test.dest), foow, dest)
				}

				if wantErr {
					continue
				}

				jWant := mustParseJSON(t, test.want)

				if diff := cmp.Diff(jWant, dest); diff != "" {
					t.Errorf("Merge(%v, %v, %v) => diff -%v +%v\n%s", src, string(test.dest), foow, jWant, dest, diff)
				}
			}

		})
	}
}

func TestMerge_Errors(t *testing.T) {
	tests := []struct {
		name      string
		src, dest JSONToken
	}{
		{
			"two primitives",
			JSONStr("foo"),
			JSONStr("bar"),
		},
		{
			"primitive and non-primitive",
			JSONStr("foo"),
			mustParseJSON(t, json.RawMessage(`{"foo":"bar"}`)),
		},
		{
			"nested primitives",
			mustParseJSON(t, json.RawMessage(`{"foo":1}`)),
			mustParseJSON(t, json.RawMessage(`{"foo":"bar"}`)),
		},
		{
			"primitive and array",
			mustParseJSON(t, json.RawMessage(`{"foo": 4}`)),
			mustParseJSON(t, json.RawMessage(`{"foo": [1 ,2, 3]}`)),
		},
		{
			"container and array",
			mustParseJSON(t, json.RawMessage(`{"foo": {"bar": "baz"}}`)),
			mustParseJSON(t, json.RawMessage(`{"foo": [1 ,2, 3]}`)),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			dest := test.dest
			err := Merge(test.src, &dest, true, false)
			if err == nil {
				t.Fatalf("Merge(%v, %v) => %v, expected error", test.src, test.dest, dest)
			}
		})
	}
}

func TestUnmarshalRawMessages(t *testing.T) {
	testMsg1 := json.RawMessage(`{"a": {"b": "c"}}`)
	testMsg2 := json.RawMessage(`{"d": {"e": ["f", "i"]}}`)
	testMsg3 := json.RawMessage(`{"g": {"h": 123}}`)

	var jc1, jc2, jc3 JSONContainer
	jc1.UnmarshalJSON(testMsg1)
	jc2.UnmarshalJSON(testMsg2)
	jc3.UnmarshalJSON(testMsg3)

	tests := []struct {
		name  string
		input []*json.RawMessage
		want  JSONArr
	}{
		{
			"unmarshal array",
			[]*json.RawMessage{&testMsg1, &testMsg2, &testMsg3},
			JSONArr{jc1, jc2, jc3},
		},
		{
			"unmarshal empty array",
			[]*json.RawMessage{},
			JSONArr{},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			output, err := UnmarshalRawMessages(test.input)
			if err != nil {
				t.Errorf("UnmarshalRawMessages(%v) returned error %v", test.input, err)
			}

			if diff := cmp.Diff(output, test.want); diff != "" {
				t.Errorf("UnmarshalRawMessages(%v) returned %v, but expected %v, diff: \n%s", test.input, output, test.want, diff)
			}
		})
	}
}

func TestDeepcopy(t *testing.T) {
	var jc1, jc2, jc1c, jc2c JSONContainer
	var ja1, ja1c JSONArr

	testMsg1 := json.RawMessage(`{"a": "c"}`)
	testMsg2 := json.RawMessage(`{"a": {"b": {"c": "d", "e": {"f": "g"}}}}`)
	testMsg3 := json.RawMessage(`["a", "b", {"c": {"d": "e"}}]`)

	testMsg1Clone := json.RawMessage(`{"a": "c"}`)
	testMsg2Clone := json.RawMessage(`{"a": {"b": {"c": "d", "e": {"f": "g"}}}}`)
	testMsg3Clone := json.RawMessage(`["a", "b", {"c": {"d": "e"}}]`)

	jc1.UnmarshalJSON(testMsg1)
	jc2.UnmarshalJSON(testMsg2)
	ja1.UnmarshalJSON(testMsg3)

	jc1c.UnmarshalJSON(testMsg1Clone)
	jc2c.UnmarshalJSON(testMsg2Clone)
	ja1c.UnmarshalJSON(testMsg3Clone)

	var newValue JSONToken = JSONStr("abc")

	tests := []struct {
		name       string
		input      JSONToken
		inputClone JSONToken
		newValue   JSONToken
		// The value of each key must be a JSONContainer
		keysToModify  []string
		indexToModify int
	}{
		{
			name:         "simple container deepcopy",
			input:        jc1,
			inputClone:   jc1c,
			newValue:     JSONStr("abc"),
			keysToModify: []string{"a"},
		},
		{
			name:         "nested container deepcopy",
			input:        jc2,
			inputClone:   jc2c,
			newValue:     JSONContainer(map[string]*JSONToken{"abc": &newValue}),
			keysToModify: []string{"a", "b", "e"},
		},
		{
			name:          "array deepcopy",
			input:         ja1,
			inputClone:    ja1c,
			newValue:      JSONContainer(map[string]*JSONToken{"c": &newValue}),
			indexToModify: 2,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			a := Deepcopy(test.input)
			switch c := test.input.(type) {
			case JSONContainer:
				node := c
				for i := 0; i < len(test.keysToModify)-1; i++ {
					node, _ = (*node[test.keysToModify[i]]).(JSONContainer)
				}
				node[test.keysToModify[len(test.keysToModify)-1]] = &test.newValue
			case JSONArr:
				c[test.indexToModify] = test.newValue
			default:
				test.input = test.newValue
			}

			if diff := cmp.Diff(a, test.inputClone); diff != "" {
				t.Errorf("deep copied JSONToken is no longer equal to the original")
			}
		})
	}
}

func TestUnorderedEqual(t *testing.T) {
	tests := []struct {
		name   string
		a, b   json.RawMessage
		wantEq bool
	}{
		{
			name:   "string and string",
			a:      json.RawMessage(`"google"`),
			b:      json.RawMessage(`"Google"`),
			wantEq: false,
		},
		{
			name:   "array and array in different item orders",
			a:      json.RawMessage(`[1, 2, 3]`),
			b:      json.RawMessage(`[2, 3, 1]`),
			wantEq: true,
		},
		{
			name:   "two array of arrays in different item orders",
			a:      json.RawMessage(`[1, [2, 4], [3, 6, 5]]`),
			b:      json.RawMessage(`[[4, 2], [3, 5, 6], 1]`),
			wantEq: true,
		},
		{
			name:   "different two array of arrays",
			a:      json.RawMessage(`["a", "b", ["c", "d"]]`),
			b:      json.RawMessage(`["a", "b", ["c", "d", ""]]`),
			wantEq: false,
		},
		{
			name:   "object and object",
			a:      json.RawMessage(`{"a": [0, 1], "b": [2, 1], "c": [1, 3, 2]}`),
			b:      json.RawMessage(`{"b": [1, 2], "a": [1, 0], "c": [2, 3, 1]}`),
			wantEq: true,
		},
		{
			name:   "different two objects",
			a:      json.RawMessage(`{"a": [0, 1], "b": [2, 1], "c": [1, 3, 2]}`),
			b:      json.RawMessage(`{"b": [1, 2], "a": [1, 0], "c": [2, 3, 1]}`),
			wantEq: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			aj, err := UnmarshalJSON(test.a)
			if err != nil {
				t.Fatalf("could not unmarshal %s: %v", test.a, err)
			}

			bj, err := UnmarshalJSON(test.b)
			if err != nil {
				t.Fatalf("could not unmarshal %s: %v", test.b, err)
			}

			if UnorderedEqual(aj, bj) != test.wantEq {
				t.Errorf("DeepEqual(%s, %s): expected equivalence: %v", test.a, test.b, test.wantEq)
			}
		})
	}
}

func TestHash(t *testing.T) {
	tests := []struct {
		name              string
		a, b              json.RawMessage
		arrayWithoutOrder bool
		wantEq            bool
	}{
		{
			name:              "string and num",
			a:                 json.RawMessage(`"1"`),
			b:                 json.RawMessage(`1`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "string and char code",
			a:                 json.RawMessage(`"1"`),
			b:                 json.RawMessage(`49`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "num and bool",
			a:                 json.RawMessage(`1`),
			b:                 json.RawMessage(`true`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "equal num",
			a:                 json.RawMessage(`102`),
			b:                 json.RawMessage(`102`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "equal num with differing decimal precision",
			a:                 json.RawMessage(`102.01000`),
			b:                 json.RawMessage(`102.01`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "equal string",
			a:                 json.RawMessage(`"asd"`),
			b:                 json.RawMessage(`"asd"`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "equal bool",
			a:                 json.RawMessage(`false`),
			b:                 json.RawMessage(`false`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "string and bool",
			a:                 json.RawMessage(`false`),
			b:                 json.RawMessage(`""`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "num and bool falsey",
			a:                 json.RawMessage(`false`),
			b:                 json.RawMessage(`0`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "object key order ignored",
			a:                 json.RawMessage(`{"a": 1, "b": 2, "c": 3}`),
			b:                 json.RawMessage(`{"b": 2, "a": 1, "c": 3}`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "nested objects",
			a:                 json.RawMessage(`{"a": {"x": "foo", "y": {"z": 77, "j": 99}}, "b": 2, "c": 3}`),
			b:                 json.RawMessage(`{"b": 2, "a": {"y": {"j": 99, "z": 77}, "x": "foo"}, "c": 3}`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "equal arrays",
			a:                 json.RawMessage(`[1, 2, 3, 4]`),
			b:                 json.RawMessage(`[1, 2, 3, 4]`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "non-equal arrays",
			a:                 json.RawMessage(`[1, 2, 3, 9]`),
			b:                 json.RawMessage(`[1, 2, 3, 4]`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "varying array item order",
			a:                 json.RawMessage(`[1, 2, 4, 3]`),
			b:                 json.RawMessage(`[1, 2, 3, 4]`),
			arrayWithoutOrder: false,
			wantEq:            false,
		},
		{
			name:              "arrays of objects",
			a:                 json.RawMessage(`[{"x": 1, "y": 9.99}, {"y": 2}, 3, 9]`),
			b:                 json.RawMessage(`[{"y": 9.99, "x": 1}, {"y": 2}, 3, 9]`),
			arrayWithoutOrder: false,
			wantEq:            true,
		},
		{
			name:              "varying number array item order without order checking",
			a:                 json.RawMessage(`[1, 2, 4, 3]`),
			b:                 json.RawMessage(`[1, 2, 3, 4]`),
			arrayWithoutOrder: true,
			wantEq:            true,
		},
		{
			name:              "varying string array item order without order checking",
			a:                 json.RawMessage(`["first", "second", "third"]`),
			b:                 json.RawMessage(`["third", "second", "first"]`),
			arrayWithoutOrder: true,
			wantEq:            true,
		},
		{
			name:              "non-equal arrays without order checking",
			a:                 json.RawMessage(`[1, 2, 3]`),
			b:                 json.RawMessage(`[1, 3]`),
			arrayWithoutOrder: true,
			wantEq:            false,
		},
		{
			name:              "array of arrays without order checking",
			a:                 json.RawMessage(`[[1, 2], [3, 4, 5], 6]`),
			b:                 json.RawMessage(`[6, [2, 1], [4, 5, 3]]`),
			arrayWithoutOrder: true,
			wantEq:            true,
		},
		{
			name:              "slightly different array of arrays without order checking",
			a:                 json.RawMessage(`[[1, 2], [3, 4, 5], 6]`),
			b:                 json.RawMessage(`[6, [2, 1], [4, 5, 3, 0]]`),
			arrayWithoutOrder: true,
			wantEq:            false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			aj, err := UnmarshalJSON(test.a)
			if err != nil {
				t.Fatalf("could not unmarshal %s: %v", test.a, err)
			}

			bj, err := UnmarshalJSON(test.b)
			if err != nil {
				t.Fatalf("could not unmarshal %s: %v", test.b, err)
			}

			ah, err := Hash(aj, test.arrayWithoutOrder)
			if err != nil {
				t.Fatalf("Hash(%s) unexpected error: %v", test.a, err)
			}

			bh, err := Hash(bj, test.arrayWithoutOrder)
			if err != nil {
				t.Fatalf("Hash(%s) unexpected error: %v", test.b, err)
			}

			if cmp.Equal(ah, bh) != test.wantEq {
				t.Errorf("Hash(%s) and Hash(%s): wanted equivalence: %v, got: %s and %s", test.a, test.b, test.wantEq, hex.EncodeToString(ah), hex.EncodeToString(bh))
			}
		})
	}
}

func TestJoinPath(t *testing.T) {
	tests := []struct {
		name  string
		input []string
		want  string
	}{
		{
			name: "no args",
		},
		{
			name:  "single arg",
			input: []string{"a"},
			want:  "a",
		},
		{
			name:  "two args",
			input: []string{"a", "b"},
			want:  "a.b",
		},
		{
			name:  "post index",
			input: []string{"a", "[100]"},
			want:  "a[100]",
		},
		{
			name:  "pre index",
			input: []string{"[100]", "b"},
			want:  "[100].b",
		},
		{
			name:  "two indices",
			input: []string{"[100]", "[200]"},
			want:  "[100][200]",
		},
		{
			name:  "extra dots",
			input: []string{".a.", ".b."},
			want:  "a.b",
		},
		{
			name:  "whitespace",
			input: []string{" .a. ", ". b. "},
			want:  "a.b",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			j := JoinPath(test.input...)
			if !cmp.Equal(j, test.want) {
				t.Errorf("JoinPath(%v) got %q want %q", test.input, j, test.want)
			}
		})
	}
}

func TestGetString(t *testing.T) {
	testMsg := json.RawMessage(`{
		"strField": "abc",
		"intField": 123,
		"boolField": true,
		"strArray": ["aa", "bb", "cc"],
		"intArray": [11, 22, 33],
		"boolArray": [true, false, true],
		"nested": {
			"strField": "def",
			"intField": 123,
			"boolField": true,
			"strArray": ["dd", "ee", "ff"],
			"intArray": [11, 22, 33],
			"boolArray": [true, false, true]
		}
	}`)

	tests := []struct {
		name    string
		field   string
		want    string
		wantErr bool
	}{
		{
			name:  "string",
			field: "strField",
			want:  "abc",
		},
		{
			name:    "int",
			field:   "intField",
			wantErr: true,
		},
		{
			name:    "bool",
			field:   "boolField",
			wantErr: true,
		},
		{
			name:    "does not exist",
			field:   "not_exist",
			wantErr: true,
		},
		{
			name:  "string array",
			field: "strArray[0]",
			want:  "aa",
		},
		{
			name:  "string array 2",
			field: "strArray[2]",
			want:  "cc",
		},
		{
			name:    "string array oob",
			field:   "strArray[4]",
			wantErr: true,
		},
		{
			name:    "string array non access",
			field:   "strArray",
			wantErr: true,
		},
		{
			name:    "int array",
			field:   "intArray[0]",
			wantErr: true,
		},
		{
			name:    "int array oob",
			field:   "intArray[4]",
			wantErr: true,
		},
		{
			name:    "bool array",
			field:   "boolArray[0]",
			wantErr: true,
		},
		{
			name:    "bool array oob",
			field:   "boolArray[4]",
			wantErr: true,
		},
		{
			name:    "obj",
			field:   "nested",
			wantErr: true,
		},
		{
			name:  "nested string",
			field: "nested.strField",
			want:  "def",
		},
		{
			name:  "nested string array",
			field: "nested.strArray[0]",
			want:  "dd",
		},
		{
			name:    "nested string array oob",
			field:   "nested.strArray[4]",
			wantErr: true,
		},
		{
			name:    "nested does not exist",
			field:   "nested.not_exist",
			wantErr: true,
		},
		{
			name:    "root",
			field:   "",
			wantErr: true,
		},
	}
	jOrig := mustParseJSON(t, testMsg)
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := GetString(jOrig, test.field)
			if err != nil && !test.wantErr {
				t.Errorf("GetString(_, %s) returned unexpected error: %v", test.field, err)
			}
			if err == nil && test.wantErr {
				t.Errorf("GetString(_, %s) returned unexpected nil error", test.field)
			}
			if !test.wantErr && !cmp.Equal(got, test.want) {
				t.Errorf("GetString(_, %s) got %s, want %s", test.field, got, test.want)
			}
		})
	}
}

func TestGetStringOrDefault(t *testing.T) {
	testMsg := json.RawMessage(`{
		"strField": "abc",
		"intField": 123,
		"boolField": true,
		"strArray": ["aa", "bb", "cc"],
		"intArray": [11, 22, 33],
		"boolArray": [true, false, true],
		"nested": {
			"strField": "def",
			"intField": 123,
			"boolField": true,
			"strArray": ["dd", "ee", "ff"],
			"intArray": [11, 22, 33],
			"boolArray": [true, false, true]
		}
	}`)

	tests := []struct {
		name    string
		field   string
		def     string
		want    string
		wantErr bool
	}{
		{
			name:  "string",
			field: "strField",
			want:  "abc",
		},
		{
			name:    "int",
			field:   "intField",
			wantErr: true,
		},
		{
			name:    "bool",
			field:   "boolField",
			wantErr: true,
		},
		{
			name:  "does not exist",
			field: "not_exist",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:  "string array",
			field: "strArray[0]",
			want:  "aa",
		},
		{
			name:  "string array 2",
			field: "strArray[2]",
			want:  "cc",
		},
		{
			name:  "string array oob",
			field: "strArray[4]",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:    "string array non access",
			field:   "strArray",
			wantErr: true,
		},
		{
			name:    "int array",
			field:   "intArray[0]",
			wantErr: true,
		},
		{
			name:  "int array oob",
			field: "intArray[4]",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:    "bool array",
			field:   "boolArray[0]",
			wantErr: true,
		},
		{
			name:  "bool array oob",
			field: "boolArray[4]",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:    "obj",
			field:   "nested",
			wantErr: true,
		},
		{
			name:  "nested string",
			field: "nested.strField",
			want:  "def",
		},
		{
			name:  "nested string array",
			field: "nested.strArray[0]",
			want:  "dd",
		},
		{
			name:  "nested string array oob",
			field: "nested.strArray[4]",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:  "nested does not exist",
			field: "nested.not_exist",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:  "nested does not exist - both levels",
			field: "not_exist.not_exist",
			def:   "DEFAULT",
			want:  "DEFAULT",
		},
		{
			name:    "root",
			field:   "",
			wantErr: true,
		},
	}
	jOrig := mustParseJSON(t, testMsg)
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := GetStringOrDefault(jOrig, test.field, test.def)
			if err != nil && !test.wantErr {
				t.Errorf("GetStringOrDefault(_, %s) returned unexpected error: %v", test.field, err)
			}
			if err == nil && test.wantErr {
				t.Errorf("GetStringOrDefault(_, %s) returned unexpected nil error", test.field)
			}
			if !test.wantErr && !cmp.Equal(got, test.want) {
				t.Errorf("GetStringOrDefault(_, %s) got %s, want %s", test.field, got, test.want)
			}
		})
	}
}
