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

package mapping_test

import (
	"encoding/json"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func mustParseContainer(json json.RawMessage, t *testing.T) jsonutil.JSONContainer {
	t.Helper()
	c := make(jsonutil.JSONContainer)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func mustParseArray(json json.RawMessage, t *testing.T) jsonutil.JSONArr {
	t.Helper()
	c := make(jsonutil.JSONArr, 0)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func mustSegmentPath(path string, t *testing.T) []string {
	segs, err := jsonutil.SegmentPath(path)
	if err != nil {
		t.Fatalf("failed to segment %q: %v", path, segs)
	}

	return segs
}

func udfGetBar(container jsonutil.JSONContainer) (jsonutil.JSONToken, error) {
	return jsonutil.GetField(container, "bar")
}

func udfMakeFooBar(foo jsonutil.JSONToken, bar jsonutil.JSONToken) (jsonutil.JSONToken, error) {
	return jsonutil.JSONContainer{
		"foo": &foo,
		"bar": &bar,
	}, nil
}

func buildStackMap(layers ...map[string]jsonutil.JSONToken) types.StackMapInterface {
	sm := types.StackMap{}
	if len(layers) == 0 {
		sm.Push()
	}
	for _, l := range layers {
		sm.Push()
		for k, v := range l {
			vc := v
			sm.Set(k, &vc)
		}
	}

	return &sm
}

func makeTreeFromLeaves(t *testing.T, leaves map[string]jsonutil.JSONToken) jsonutil.JSONToken {
	var root jsonutil.JSONToken = make(jsonutil.JSONContainer)
	for k, v := range leaves {
		if err := jsonutil.SetField(v, k, &root, true); err != nil {
			t.Fatalf("error making tree from leaves: can't set %q to %v: %v", k, v, err)
		}
	}
	return root
}

// toNodes converts the given array of tokens into nodes with meta data.
func toNodes(t *testing.T, tokens []jsonutil.JSONToken) []jsonutil.JSONMetaNode {
	t.Helper()
	nodes := make([]jsonutil.JSONMetaNode, len(tokens))
	for i, arg := range tokens {
		tok, err := jsonutil.TokenToNode(arg)
		if err != nil {
			t.Fatalf("error convering tokens to nodes: %v", err)
		}
		nodes[i] = tok
	}
	return nodes
}

func TestFindParentAndRemainderFromArgs(t *testing.T) {
	inputTree, err := jsonutil.TokenToNode(makeTreeFromLeaves(t, map[string]jsonutil.JSONToken{
		"group1.group2[0].array1[0].foo": jsonutil.JSONNum(1),
		"group1.group2[0].array1[0].bar": jsonutil.JSONNum(2),
		"group1.group2[1].array1[0].foo": jsonutil.JSONNum(3),
		"group1.group2[1].array1[0].bar": jsonutil.JSONNum(4),
		"group1.group2[1].array1[1].foo": jsonutil.JSONNum(5),
		"group1.group2[1].array2[0].foo": jsonutil.JSONNum(6),
		"group1.group2[2].sibling1":      jsonutil.JSONNum(7),
		"group1.group2[2].sibling2":      jsonutil.JSONNum(8),
		"group1.container.item1":         jsonutil.JSONNum(9),
		"group1.container.item2":         jsonutil.JSONNum(10),
		"group1.something.item3":         jsonutil.JSONNum(11),
		"groupX.other":                   jsonutil.JSONNum(12),
		"group1.item4":                   jsonutil.JSONNum(13),
	}))
	if err != nil {
		t.Fatalf("error creating test tree: %v", err)
	}

	tests := []struct {
		name         string
		argPaths     []string
		segs         []string
		wantNodePath string
		wantFinal    jsonutil.JSONToken
		wantSegs     []string
	}{
		{
			name:         "non-array sibling",
			argPaths:     []string{"group1.container.item1"},
			segs:         mustSegmentPath("group1.container.item2", t),
			wantNodePath: "group1.container",
			wantFinal:    jsonutil.JSONNum(10),
			wantSegs:     []string{"item2"},
		},
		{
			name:         "array sibling",
			argPaths:     []string{"group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1.bar", t),
			wantNodePath: "group1.group2[1].array1[0]",
			wantFinal:    jsonutil.JSONNum(4),
			wantSegs:     mustSegmentPath("bar", t),
		},
		{
			name:         "non-array ancestor",
			argPaths:     []string{"group1.container.item1"},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "group1",
			wantFinal:    jsonutil.JSONNum(11),
			wantSegs:     mustSegmentPath("something.item3", t),
		},
		{
			name:         "array ancestor",
			argPaths:     []string{"group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1[1].foo", t),
			wantNodePath: "group1.group2[1].array1",
			wantFinal:    jsonutil.JSONNum(5),
			wantSegs:     mustSegmentPath("[1].foo", t),
		},
		{
			name:         "deep array ancestor",
			argPaths:     []string{"group1.group2[1].array2[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1[1].foo", t),
			wantNodePath: "group1.group2[1]",
			wantFinal:    jsonutil.JSONNum(5),
			wantSegs:     mustSegmentPath("array1[1].foo", t),
		},
		{
			name:         "multiple args uses longest match",
			argPaths:     []string{"group1.container.item1", "group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1.bar", t),
			wantNodePath: "group1.group2[1].array1[0]",
			wantFinal:    jsonutil.JSONNum(4),
			wantSegs:     mustSegmentPath("bar", t),
		},
		{
			name:         "root is only common ancestor",
			argPaths:     []string{"groupX.other"},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "",
			wantFinal:    jsonutil.JSONNum(11),
			wantSegs:     mustSegmentPath("group1.something.item3", t),
		},
		{
			name:         "reference to ancestor",
			argPaths:     []string{"group1.group2[1].array2[0].foo"},
			segs:         mustSegmentPath("group1", t),
			wantNodePath: "group1",
			wantFinal:    toToken(t, mustGetNodeField(t, inputTree, "group1")),
			wantSegs:     []string{},
		},
		{
			name:         "no args",
			argPaths:     []string{},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "nil",
			wantFinal:    nil,
			wantSegs:     mustSegmentPath("group1.something.item3", t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			args := make([]jsonutil.JSONMetaNode, 0, len(test.argPaths))
			for _, p := range test.argPaths {
				n, err := jsonutil.GetNodeField(inputTree, p)
				if err != nil {
					t.Fatalf("test wants arg %q but this is not a valid path on the input: %v", p, err)
				}
				args = append(args, n)
			}

			gotNode, gotSegs, err := mapping.ParentInfoFromArgs(args, test.segs)
			if err != nil {
				t.Fatalf("ParentInfoFromArgs(%v, %v) => unexpected error: %v", args, test.segs, err)
			}

			finalNode, err := jsonutil.GetNodeFieldSegmented(gotNode, gotSegs)
			if err != nil {
				t.Fatalf("returned path %v is not valid: %v", gotSegs, err)
			}

			finalToken := toToken(t, finalNode)

			if diff := cmp.Diff(finalToken, test.wantFinal); diff != "" {
				t.Errorf("finalToken = %v want %v, diff:\n%s", finalToken, test.wantFinal, diff)
			}
			if !cmp.Equal(test.wantSegs, gotSegs) {
				t.Errorf("gotSegs = %v, want %v", gotSegs, test.wantSegs)
			}
		})
	}
}

func toToken(t *testing.T, node jsonutil.JSONMetaNode) jsonutil.JSONToken {
	t.Helper()
	tok, err := jsonutil.NodeToToken(node)
	if err != nil {
		t.Fatalf("could not convert %v to token: %v", node, err)
	}

	return tok
}

func TestEvaluateFromVar(t *testing.T) {
	tests := []struct {
		name    string
		argVs   mappb.ValueSource_FromLocalVar
		argPctx types.Context
		want    jsonutil.JSONToken
		wantErr bool
	}{
		{
			name: "existing var",
			argVs: mappb.ValueSource_FromLocalVar{
				FromLocalVar: "existing",
			},
			argPctx: types.Context{
				Variables: buildStackMap(map[string]jsonutil.JSONToken{
					"existing": jsonutil.JSONNum(33),
				}),
			},
			want: jsonutil.JSONNum(33),
		},
		{
			name: "non-existing var",
			argVs: mappb.ValueSource_FromLocalVar{
				FromLocalVar: "missing",
			},
			argPctx: types.Context{
				Variables: buildStackMap(map[string]jsonutil.JSONToken{
					"existing": jsonutil.JSONNum(33),
				}),
			},
			wantErr: true,
		},
		{
			name: "empty name",
			argVs: mappb.ValueSource_FromLocalVar{
				FromLocalVar: "",
			},
			argPctx: types.Context{
				Variables: buildStackMap(map[string]jsonutil.JSONToken{
					"existing": jsonutil.JSONNum(33),
				}),
			},
			wantErr: true,
		},
		{
			name: "field access",
			argVs: mappb.ValueSource_FromLocalVar{
				FromLocalVar: "var.foo",
			},
			argPctx: types.Context{
				Variables: buildStackMap(map[string]jsonutil.JSONToken{
					"var": mustParseContainer(json.RawMessage(`{"foo": 1337}`), t),
				}),
			},
			want: jsonutil.JSONNum(1337),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := mapping.EvaluateFromVar(&test.argVs, test.argPctx)

			if test.wantErr != (err != nil) || (!test.wantErr && got != test.want) {
				t.Errorf("FindLongestContextField(%v, %v) => %v, %v want %v, %v", test.argVs, test.argPctx, got, err, test.want, test.wantErr)
			}
		})
	}
}

func TestEvaluateArgSource(t *testing.T) {
	inputTree, err := jsonutil.TokenToNode(makeTreeFromLeaves(t, map[string]jsonutil.JSONToken{
		"group1.group2[0].array1[0].foo": jsonutil.JSONNum(1),
		"group1.group2[0].array1[0].bar": jsonutil.JSONNum(2),
		"group1.group2[1].array1[0].foo": jsonutil.JSONNum(3),
		"group1.group2[1].array1[0].bar": jsonutil.JSONNum(4),
		"group1.group2[1].array1[1].foo": jsonutil.JSONNum(5),
		"group1.group2[1].array2[0].foo": jsonutil.JSONNum(6),
		"group1.group2[2].sibling1":      jsonutil.JSONNum(7),
		"group1.group2[2].sibling2":      jsonutil.JSONNum(8),
		"group1.container.item1":         jsonutil.JSONNum(9),
		"group1.container.item2":         jsonutil.JSONNum(10),
		"group1.something.item3":         jsonutil.JSONNum(11),
		"groupX.other":                   jsonutil.JSONNum(12),
	}))
	if err != nil {
		t.Fatalf("error creating test tree: %v", err)
	}
	tests := []struct {
		name  string
		argVs mappb.ValueSource_FromInput
		args  []jsonutil.JSONMetaNode
		want  jsonutil.JSONMetaNode
	}{
		{
			name:  "single arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "groupX.other"}},
			args:  []jsonutil.JSONMetaNode{inputTree},
			want:  mustGetNodeField(t, inputTree, "groupX.other"),
		},
		{
			name:  "single arg with array",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "group1.group2"}},
			args:  []jsonutil.JSONMetaNode{inputTree},
			want:  mustGetNodeField(t, inputTree, "group1.group2"),
		},
		{
			name:  "single arg which is itself array",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "[0]"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0]"),
		},
		{
			name:  "single enumerated arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "group2[]"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1")},
			want:  mustGetNodeField(t, inputTree, "group1.group2"),
		},
		{
			name:  "empty ref to one arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: ""}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other")},
			want:  mustGetNodeField(t, inputTree, "groupX.other"),
		},
		{
			name:  "numeric ref to multiple args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 2, Field: ""}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  mustGetNodeField(t, inputTree, "group1.something.item3"),
		},
		{
			name:  "context ref (to nothing) with multiple args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "context"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  nil,
		},
		{
			name:  "(numeric) context ref (to nothing) with no args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "1"}},
			args:  []jsonutil.JSONMetaNode{},
			want:  nil,
		},
		{
			name:  "context ref with args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "group1.group2.array1.bar"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].foo"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].bar"),
		},
		{
			name:  "context ref with single arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "group1.group2.array1.bar"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].foo")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].bar"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := mapping.EvaluateArgSource(test.argVs.FromInput, test.args, types.NewContext(types.NewRegistry()))

			if err != nil {
				t.Fatalf("evaluateFromSource(%v, %v, <new context>) unexpected error %v", test.argVs, test.args, err)
			}

			if diff := cmp.Diff(got, test.want, cmpopts.IgnoreUnexported(jsonutil.JSONMeta{})); diff != "" {
				t.Errorf("evaluateFromSource(%v, %v, <new context>) => %v want %v diff %s", test.argVs, test.args, got, test.want, diff)
			}
		})
	}
}

func mustGetNodeField(t *testing.T, root jsonutil.JSONMetaNode, path string) jsonutil.JSONMetaNode {
	n, err := jsonutil.GetNodeField(root, path)
	if err != nil {
		t.Fatalf("could not find node %q in %v: %v", path, root, err)
	}

	return n
}

func buildProjector(t *testing.T, fn interface{}) types.Projector {
	t.Helper()
	p, err := projector.FromFunction(fn, "test")
	if err != nil {
		t.Fatalf("failed to build projector: %v", err)
	}

	return p
}

func TestEvaluateValueSource(t *testing.T) {
	reg := types.NewRegistry()
	if err := reg.RegisterProjector("UDFGetBar", buildProjector(t, udfGetBar)); err != nil {
		t.Fatalf("failed to register test projector %v", err)
	}
	if err := reg.RegisterProjector("UDFMakeFooBar", buildProjector(t, udfMakeFooBar)); err != nil {
		t.Fatalf("failed to register test projector %v", err)
	}

	tests := []struct {
		name      string
		argVs     mappb.ValueSource
		args      []jsonutil.JSONToken
		argVars   types.StackMapInterface
		argOutput jsonutil.JSONToken
		want      jsonutil.JSONToken
	}{
		{
			name: "const source",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_ConstString{
					ConstString: "foo",
				},
			},
			args: []jsonutil.JSONToken{},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "single source",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo",
				},
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo":"bar"}`), t)},
			want: jsonutil.JSONStr("bar"),
		},
		{
			name: "single source with projector",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "baz",
				},
				Projector: "UDFGetBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"baz": {"bar":"foo"}}`), t)},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "additional arg with projector",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "baz",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_FromSource{
							FromSource: "baz.bar",
						},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"baz": {"bar":"foo"}}`), t)},
			want: mustParseContainer(json.RawMessage(`{"foo": {"bar":"foo"}, "bar": "foo"}`), t),
		},
		{
			name: "additional arg with projector in arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "baz",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "red",
						},
						AdditionalArg: []*mappb.ValueSource{
							{
								Source: &mappb.ValueSource_ConstString{
									ConstString: "blue",
								},
							},
						},
						Projector: "UDFMakeFooBar",
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"baz": {"bar":"foo"}}`), t)},
			want: mustParseContainer(json.RawMessage(`{"foo": {"bar":"foo"}, "bar": {"foo": "red", "bar": "blue"}}`), t),
		},
		{
			name: "enumerated value",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo[]",
				},

				Projector: "UDFGetBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"bar": 1}, {"bar": 2}, {"bar": 3}]}`), t)},
			want: jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)},
		},
		{
			name: "enumerated value with zipped args",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo[]",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "bar",
						},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"meep": 1}, {"meep": 2}, {"meep": 3}]}`), t)},
			want: mustParseArray(json.RawMessage(`[{"foo":{"meep": 1}, "bar":"bar"}, {"foo":{"meep": 2}, "bar":"bar"}, {"foo":{"meep": 3}, "bar":"bar"}]`), t),
		},
		{
			name: "enumerated multiple arrays as zipped args",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromInput{
					FromInput: &mappb.ValueSource_InputSource{
						Arg:   1,
						Field: "foo[]",
					},
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_FromInput{
							FromInput: &mappb.ValueSource_InputSource{
								Arg:   2,
								Field: "[]",
							},
						},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"meep": 1}, {"meep": 2}, {"meep": 3}]}`), t), mustParseArray(json.RawMessage(`["red", "green", "blue"]`), t)},
			want: mustParseArray(json.RawMessage(`[{"foo":{"meep": 1}, "bar":"red"}, {"foo":{"meep": 2}, "bar":"green"}, {"foo":{"meep": 3}, "bar":"blue"}]`), t),
		},
		{
			name: "projected enumerated value",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_ProjectedValue{
					ProjectedValue: &mappb.ValueSource{
						Source: &mappb.ValueSource_FromSource{
							FromSource: "foo[]",
						},

						Projector: "UDFGetBar",
					},
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"bar": 1}, {"bar": 2}, {"bar": 3}]}`), t)},
			want: mustParseContainer(json.RawMessage(`{"foo": [1,2,3], "bar": "hello"}`), t),
		},
		{
			name: "projected value with explicit nil arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "nullkey",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"nullkey": null}`), t)},
			want: mustParseContainer(json.RawMessage(`{"foo": null, "bar": "hello"}`), t),
		},
		{
			name: "projected value with implicit nil arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo.none.of.these[37].exist",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": {}}`), t)},
			want: mustParseContainer(json.RawMessage(`{"foo": null, "bar": "hello"}`), t),
		},
		{
			name: "non-existent key",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "zip.none.of.these[37].exist",
				},
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"zip": {}}`), t)},
			want: nil,
		},
		{
			name: "enumerated non-existent key",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "zip.none.of.these[37].exist[]",
				},
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"zip": {}}`), t)},
			want: nil,
		},
		{
			name: "from destination",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromDestination{
					FromDestination: "foo.bar",
				},
			},
			args:      []jsonutil.JSONToken{},
			argOutput: mustParseContainer(json.RawMessage(`{"foo": {"bar": "baz"}}`), t),
			want:      jsonutil.JSONStr("baz"),
		},
		{
			name: "from variable",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromLocalVar{
					FromLocalVar: "foo",
				},
			},
			args:    []jsonutil.JSONToken{},
			argVars: buildStackMap(map[string]jsonutil.JSONToken{"foo": jsonutil.JSONStr("baz")}),
			want:    jsonutil.JSONStr("baz"),
		},
		{
			name: "enumerated from destination",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromDestination{
					FromDestination: "foo[]",
				},
				Projector: "UDFGetBar",
			},
			args:      []jsonutil.JSONToken{},
			argOutput: mustParseContainer(json.RawMessage(`{"foo": [{"bar": 1}, {"bar": 2}, {"bar": 3}]}`), t),
			want:      jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)},
		},
		{
			name: "enumerated from variable",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromLocalVar{
					FromLocalVar: "foo[]",
				},
				Projector: "UDFGetBar",
			},
			args: []jsonutil.JSONToken{},
			argVars: buildStackMap(map[string]jsonutil.JSONToken{
				"foo": mustParseArray(json.RawMessage(`[{"bar": 1}, {"bar": 2}, {"bar": 3}]`), t),
			}),
			want: jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)},
		},
		{
			name: "from arg - 0 returns all args",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 0,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: jsonutil.JSONArr{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
		},
		{
			name: "from arg - 1 returns first arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 1,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: jsonutil.JSONNum(99),
		},
		{
			name: "from arg - len(arg) returns last arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 3,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: jsonutil.JSONNum(97),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pctx := types.NewContext(reg)
			if test.argVars != nil {
				pctx.Variables = test.argVars
			}

			got, err := mapping.EvaluateValueSource(&test.argVs, toNodes(t, test.args), test.argOutput, pctx)

			if err != nil {
				t.Errorf("evaluateFromSource(%v, %v, %v) unexpected error %v", test.argVs, test.args, pctx, err)
			} else if diff := cmp.Diff(got, test.want); diff != "" {
				t.Errorf("evaluateFromSource(%v, %v, %v) => %v want %v diff %s", test.argVs, test.args, pctx, got, test.want, diff)
			}
		})
	}
}

func TestEvaluateValueSourceErrors(t *testing.T) {
	reg := types.NewRegistry()
	if err := reg.RegisterProjector("UDFGetBar", buildProjector(t, udfGetBar)); err != nil {
		t.Fatalf("failed to register test projector %v", err)
	}
	tests := []struct {
		name      string
		argVs     mappb.ValueSource
		args      []jsonutil.JSONToken
		argOutput jsonutil.JSONToken
	}{
		{
			name: "no source",
			argVs: mappb.ValueSource{
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
			},
			args: []jsonutil.JSONToken{},
		},
		{
			name: "no source but has additional args",
			argVs: mappb.ValueSource{
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
			},
			args: []jsonutil.JSONToken{},
		},
		{
			name: "enumerated value with additional arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo[]",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_ConstString{ConstString: "hello"},
					},
				},
				Projector: "UDFGetBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"bar": 1}, {"bar": 2}, {"bar": 3}]}`), t)},
		},
		{
			name: "error parsing fromsource as segment",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo..bar",
				},
				Projector: "UDFGetBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"bar": 1}, {"bar": 2}, {"bar": 3}]}`), t)},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pctx := types.NewContext(reg)
			got, err := mapping.EvaluateValueSource(&test.argVs, toNodes(t, test.args), test.argOutput, pctx)

			if err == nil {
				t.Errorf("evaluateFromSource(%v, %v, %v) expected an error but got %v", test.argVs, test.args, pctx, got)
			}
		})
	}
}

func TestProcessMappingSequential(t *testing.T) {
	proj, err := projector.FromFunction(func(foo jsonutil.JSONStr) (jsonutil.JSONArr, error) {
		return jsonutil.JSONArr{jsonutil.JSONArr{foo, jsonutil.JSONStr("bar")}, jsonutil.JSONStr("baz")}, nil
	}, "MakeFooBar")

	if err != nil {
		t.Fatalf("failed to create test projector: %v", err)
	}

	reg := types.NewRegistry()
	if err := reg.RegisterProjector("MakeFooBar", proj); err != nil {
		t.Fatalf("failed to register test projector: %v", err)
	}

	tests := []struct {
		name          string
		mapping       *mappb.FieldMapping
		args          []jsonutil.JSONToken
		pctxGen       func() *types.Context
		output        jsonutil.JSONToken
		want          jsonutil.JSONToken
		wantOk        bool
		wantTLO       string
		wantRootField string
		wantVar       string
	}{
		{
			name: "true condition",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstBool{
						ConstBool: true,
					},
				},
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "",
				},
			},
			want:   jsonutil.JSONStr("foo"),
			wantOk: true,
		},
		{
			name: "false condition",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstBool{
						ConstBool: false,
					},
				},
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "",
				},
			},
			wantOk: false,
		},
		{
			name: "field target",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar.baz",
				},
			},
			want:   mustParseContainer(json.RawMessage(`{"bar": {"baz": "foo"}}`), t),
			wantOk: true,
		},
		{
			name: "var target",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "xyz",
				},
			},

			want:    jsonutil.JSONStr("foo"),
			wantOk:  true,
			wantVar: "xyz",
		},
		{
			name: "nested var target overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "two",
					},
				},
				Target: &mappb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "xyz.foo",
				},
			},
			pctxGen: func() *types.Context {
				pctx := types.NewContext(reg)
				pctx.Variables.Push()

				var val jsonutil.JSONToken = mustParseContainer(json.RawMessage(`{"foo": "one"}`), t)
				pctx.Variables.Set("xyz", &val)

				return pctx
			},
			want:    mustParseContainer(json.RawMessage(`{"foo": "two"}`), t),
			wantOk:  true,
			wantVar: "xyz",
		},
		{
			name: "var target append",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "three",
					},
				},
				Target: &mappb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "xyz[]",
				},
			},
			pctxGen: func() *types.Context {
				pctx := types.NewContext(reg)
				pctx.Variables.Push()

				var val jsonutil.JSONToken = mustParseArray(json.RawMessage(`["one", "two"]`), t)
				pctx.Variables.Set("xyz", &val)

				return pctx
			},
			want:    mustParseArray(json.RawMessage(`["one", "two", "three"]`), t),
			wantOk:  true,
			wantVar: "xyz",
		},
		{
			name: "top level target",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetObject{
					TargetObject: "Foo",
				},
			},
			want:    jsonutil.JSONArr{jsonutil.JSONStr("foo")},
			wantOk:  true,
			wantTLO: "Foo",
		},
		{
			name: "top level target array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "MakeFooBar",
				},
				Target: &mappb.FieldMapping_TargetObject{
					TargetObject: "Foo",
				},
			},
			want:    jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar"), jsonutil.JSONStr("baz")},
			wantOk:  true,
			wantTLO: "Foo",
		},
		{
			name: "root field target",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "Foo",
				},
			},
			want:          jsonutil.JSONStr("foo"),
			wantOk:        true,
			wantRootField: "Foo",
		},
		{
			name: "root field target array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "MakeFooBar",
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "Foo",
				},
			},
			want:          jsonutil.JSONArr{jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")}, jsonutil.JSONStr("baz")},
			wantOk:        true,
			wantRootField: "Foo",
		},
		{
			name: "root output is itself an array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "[]",
				},
			},
			want:          jsonutil.JSONStr("foo"),
			wantOk:        true,
			wantRootField: "[0]",
		},
		{
			name: "field overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar!",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": "foo"}`), t),
			wantOk: true,
		},
		{
			name: "root field overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar!",
				},
			},
			output:        mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
			want:          jsonutil.JSONStr("foo"),
			wantRootField: "bar",
			wantOk:        true,
		},
		{
			name: "array overwrite",
			args: jsonutil.JSONArr{jsonutil.JSONArr{jsonutil.JSONStr("one"), jsonutil.JSONStr("two")}},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: ".",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar!",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": ["a", "b"]}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": ["one", "two"]}`), t),
			wantOk: true,
		},
		{
			name: "overwrite with type change",
			args: jsonutil.JSONArr{jsonutil.JSONArr{jsonutil.JSONStr("one"), jsonutil.JSONStr("two")}},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: ".",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar!",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": "foo"}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": ["one", "two"]}`), t),
			wantOk: true,
		},
		{
			name: "array concat",
			args: jsonutil.JSONArr{jsonutil.JSONArr{jsonutil.JSONStr("one"), jsonutil.JSONStr("two")}},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: ".",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": ["a", "b"]}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": ["a", "b", "one", "two"]}`), t),
			wantOk: true,
		},
		{
			name: "array element overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar[0]!",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": ["a", "b"]}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": ["foo", "b"]}`), t),
			wantOk: true,
		},
		{
			name: "array element append",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar[]",
				},
			},
			output: mustParseContainer(json.RawMessage(`{"bar": ["a"]}`), t),
			want:   mustParseContainer(json.RawMessage(`{"bar": ["a", "foo"]}`), t),
			wantOk: true,
		},
		{
			name: "array element target (non existing array)",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar.baz[]",
				},
			},
			want:   mustParseContainer(json.RawMessage(`{"bar": {"baz": ["foo"]}}`), t),
			wantOk: true,
		},
		{
			name: "nulls ignored for field target",
			args: jsonutil.JSONArr{mustParseContainer(json.RawMessage(`{"foo": null}`), t)},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			wantOk: false,
		},
		{
			name: "nulls ignored for root field target",
			args: jsonutil.JSONArr{mustParseContainer(json.RawMessage(`{"foo": null}`), t)},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar",
				},
			},
			wantOk: false,
		},
		{
			name: "empty strings ignored for field target",
			args: jsonutil.JSONArr{mustParseContainer(json.RawMessage(`{"foo": ""}`), t)},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			wantOk: false,
		},
		{
			name: "nulls included for var target",
			args: jsonutil.JSONArr{mustParseContainer(json.RawMessage(`{"foo": null}`), t)},
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "xyz",
				},
			},

			want:    jsonutil.JSONToken(nil),
			wantOk:  true,
			wantVar: "xyz",
		},
		{
			name: "string spaces are trimmed",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "\t\r   \n\t\n\tfoo\t\t\r   \n\t\t",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar.baz",
				},
			},
			want:   mustParseContainer(json.RawMessage(`{"bar": {"baz": "foo"}}`), t),
			wantOk: true,
		},
		{
			name: "string spaces are trimmed for root fields",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "\t\r   \n\t\n\tfoo\t\t\r   \n\t\t",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar.baz",
				},
			},
			want:          mustParseContainer(json.RawMessage(`{"baz": "foo"}`), t),
			wantRootField: "bar",
			wantOk:        true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			output := test.output

			var pctx *types.Context
			if test.pctxGen == nil {
				pctx = types.NewContext(reg)
				pctx.Variables.Push()
			} else {
				pctx = test.pctxGen()
			}

			err := mapping.ProcessMappingSequential(test.mapping, toNodes(t, test.args), &output, pctx)
			if err != nil {
				t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => unexpected error %v", test.mapping, test.args, output, pctx, err)
			}

			if !test.wantOk {
				if test.wantTLO != "" && len(pctx.TopLevelObjects[test.wantTLO]) > 0 {
					t.Fatalf("wanted noop TLO mapping but found TLO in %s", test.wantTLO)
				}
				if v, err := pctx.Variables.Get(test.wantVar); test.wantVar != "" && v != nil && err == nil {
					t.Fatalf("wanted noop var mapping but found var %s: %v", test.wantVar, v)
				}
				if test.wantTLO == "" && test.wantVar == "" && output != nil {
					t.Fatalf("wanted noop field mapping but got %v", output)
				}
				return
			}

			if test.wantTLO != "" {
				tlo, ok := pctx.TopLevelObjects[test.wantTLO]
				if !ok || len(tlo) == 0 {
					t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => expected top level object %s, but did not find it.", test.mapping, test.args, output, pctx, test.wantTLO)
				}

				wantArr, ok := test.want.(jsonutil.JSONArr)
				if !ok {
					t.Fatalf("test.want must be an JSONArr for Top Level Objects (but was %T)", test.want)
				}

				if diff := cmp.Diff(tlo, []jsonutil.JSONToken(wantArr)); diff != "" {
					t.Errorf("ProcessMappingSequential(%v, %v, %v, %v) => expected top level object %v, want %v\ndiff %s", test.mapping, test.args, output, pctx, tlo, test.want, diff)
				}
			} else if test.wantVar != "" {
				v, err := pctx.Variables.Get(test.wantVar)
				if err != nil {
					t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => expected var %s, but did not find it.", test.mapping, test.args, output, pctx, test.wantVar)
				}

				if diff := cmp.Diff(*v, test.want); diff != "" {
					t.Errorf("ProcessMappingSequential(%v, %v, %v, %v) => var %s=%v, want %v\ndiff %s", test.mapping, test.args, output, pctx, test.wantVar, v, test.want, diff)
				}
			} else if test.wantRootField != "" {
				v, err := jsonutil.GetField(pctx.Output, test.wantRootField)
				if err != nil {
					t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => expected root field %s, but did not find it.", test.mapping, test.args, output, pctx, test.wantVar)
				}

				if diff := cmp.Diff(v, test.want); diff != "" {
					t.Errorf("ProcessMappingSequential(%v, %v, %v, %v) => root field %s=%v, want %v\ndiff %s", test.mapping, test.args, output, pctx, test.wantVar, v, test.want, diff)
				}
			} else if diff := cmp.Diff(output, test.want); diff != "" {
				t.Errorf("ProcessMappingSequential(%v, %v, %v, %v) => %v, want %v\ndiff %s", test.mapping, test.args, output, pctx, output, test.want, diff)
			}
		})
	}
}

func TestProcessMappingSequentialErrors(t *testing.T) {
	proj, err := projector.FromFunction(func(kv jsonutil.JSONStr) (jsonutil.JSONContainer, error) {
		var kvt jsonutil.JSONToken = kv
		return jsonutil.JSONContainer{string(kv): &kvt}, nil
	}, "MakeObject")
	if err != nil {
		t.Fatalf("failed to create test projector: %v", err)
	}

	reg := types.NewRegistry()
	builtins.RegisterAll(reg)
	if err := reg.RegisterProjector("MakeObject", proj); err != nil {
		t.Fatalf("failed to register test projector: %v", err)
	}
	tests := []struct {
		name          string
		mapping       *mappb.FieldMapping
		args          []jsonutil.JSONToken
		argOutput     jsonutil.JSONToken
		argPctxOutput jsonutil.JSONToken
	}{
		{
			name: "non-bool condition",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
		},
		{
			name: "nil condition",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromSource{
						FromSource: "foo",
					},
				},
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "irrelevant",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "irrelevant",
				},
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": null}`), t)},
		},
		{
			name: "field overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
		},
		{
			name: "field cardinality change - array to prim",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": ["hi"]}`), t),
		},
		{
			name: "field cardinality change - prim to array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "$ListOf",
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
		},
		{
			name: "field cardinality change - object to array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "$ListOf",
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
		},
		{
			name: "field cardinality change - array to object",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "MakeObject",
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": ["hi"]}`), t),
		},
		{
			name: "root field cardinality change - array to prim",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar",
				},
			},
			argPctxOutput: mustParseContainer(json.RawMessage(`{"bar": ["hi"]}`), t),
		},
		{
			name: "root field cardinality change - prim to array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "$ListOf",
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar",
				},
			},
			argPctxOutput: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
		},
		{
			name: "root field cardinality change - object to array",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "$ListOf",
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar",
				},
			},
			argPctxOutput: mustParseContainer(json.RawMessage(`{"bar": "hi"}`), t),
		},
		{
			name: "root field cardinality change - array to object",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
					Projector: "MakeObject",
				},
				Target: &mappb.FieldMapping_TargetRootField{
					TargetRootField: "bar",
				},
			},
			argPctxOutput: mustParseContainer(json.RawMessage(`{"bar": ["hi"]}`), t),
		},
		{
			name: "array overwrite",
			mapping: &mappb.FieldMapping{
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "foo",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "bar[0]",
				},
			},
			argOutput: mustParseContainer(json.RawMessage(`{"bar": ["hi"]}`), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pctx := types.NewContext(reg)
			pctx.Output = test.argPctxOutput
			err := mapping.ProcessMappingSequential(test.mapping, toNodes(t, test.args), &test.argOutput, pctx)
			if err == nil {
				t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => expected error, got nil", test.mapping, test.args, test.argOutput, pctx)
			}
		})
	}
}
