// Copyright 2020 Google LLC.
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

package projector

import (
	"encoding/json"
	"fmt"
	"strings"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	parallel = false
)

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

func UDFFoo() (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr("foo"), nil
}

func TestFromFunction(t *testing.T) {
	tests := []struct {
		name string
		fn   interface{}
		args []jsonutil.JSONToken
		want jsonutil.JSONToken
	}{
		{
			name: "no args global",
			fn:   UDFFoo,
			args: []jsonutil.JSONToken{},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "no args",
			fn:   func() (jsonutil.JSONStr, error) { return jsonutil.JSONStr("foo"), nil },
			args: []jsonutil.JSONToken{},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "one arg",
			fn:   func(str jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo")},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "two args",
			fn:   func(a, b jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a + b, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")},
			want: jsonutil.JSONStr("foobar"),
		},
		{
			name: "slice args",
			fn:   func(a []jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")}},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "specific return type",
			fn:   func(a, b jsonutil.JSONNum) (jsonutil.JSONNum, error) { return a + b, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1), jsonutil.JSONNum(2)},
			want: jsonutil.JSONNum(3),
		},
		{
			name: "arg assignability",
			fn:   func(a jsonutil.JSONToken) (jsonutil.JSONToken, error) { return a, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo")},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "variadic args with array",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")}},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "variadic args with empty array",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return jsonutil.JSONStr("foo"), nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{}},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "variadic args with multiple args",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return jsonutil.JSONNum(len(a)), nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")},
			want: jsonutil.JSONNum(2),
		},
		{
			name: "variadic args with single arg",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo")},
			want: jsonutil.JSONStr("foo"),
		},
		{
			name: "variadic args with no arg",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return nil, nil },
			args: []jsonutil.JSONToken{},
			want: nil,
		},
		{
			name: "nil json array",
			fn:   func(a jsonutil.JSONArr) (jsonutil.JSONNum, error) { return jsonutil.JSONNum(len(a)), nil },
			args: []jsonutil.JSONToken{nil},
			want: jsonutil.JSONNum(0),
		},
		{
			name: "nil slice",
			fn:   func(a []jsonutil.JSONStr) (jsonutil.JSONNum, error) { return jsonutil.JSONNum(len(a)), nil },
			args: []jsonutil.JSONToken{nil},
			want: jsonutil.JSONNum(0),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			proj, err := FromFunction(test.fn, test.name)
			if err != nil {
				t.Fatalf("FromFunction(%v) returned unexpected error %v", test.fn, err)
			}

			got, err := proj(toNodes(t, test.args), types.NewContext(types.NewRegistry()))
			if err != nil {
				t.Fatalf("<generated projector>(%v) => unexpected error %v", test.args, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("<generated projector>(%v) = %v, want %v", test.args, got, test.want)
			}
		})
	}
}

func TestFromFunctionCompileErrors(t *testing.T) {
	tests := []struct {
		name string
		fn   interface{}
	}{
		{
			name: "no return value",
			fn:   func() {},
		},
		{
			name: "not enough returned values",
			fn:   func() jsonutil.JSONToken { return nil },
		},
		{
			name: "too many returned values",
			fn:   func() (jsonutil.JSONStr, error, jsonutil.JSONToken) { return "", nil, nil },
		},
		{
			name: "incorrect first return type",
			fn:   func() (int, error) { return 0, nil },
		},
		{
			name: "incorrect second return type",
			fn:   func() (jsonutil.JSONStr, bool) { return "", false },
		},
		{
			name: "invalid argument type",
			fn:   func(s string) (jsonutil.JSONStr, error) { return jsonutil.JSONStr(s), nil },
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := FromFunction(test.fn, test.name)
			if err == nil {
				t.Fatalf("FromFunction(%v) returned a projector but expected an error", test.fn)
			}
		})
	}
}

func TestFromFunctionInvocationErrors(t *testing.T) {
	tests := []struct {
		name string
		fn   interface{}
		args []jsonutil.JSONToken
	}{
		{
			name: "arg type mismatch",
			fn:   func(str jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1)},
		},
		{
			name: "too many args",
			fn:   func(str jsonutil.JSONNum) (jsonutil.JSONToken, error) { return str, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1), jsonutil.JSONNum(1)},
		},
		{
			name: "too few args",
			fn:   func(a, b jsonutil.JSONNum) (jsonutil.JSONToken, error) { return a + b, nil },
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1)},
		},
		{
			name: "slice arg complete type mismatch",
			fn:   func(str []jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1)},
		},
		{
			name: "slice arg type mismatch",
			fn:   func(str []jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo")},
		},
		{
			name: "slice arg element type mismatch",
			fn:   func(str []jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONNum(1)}},
		},
		{
			name: "slice arg non-homogeneous elements",
			fn:   func(str []jsonutil.JSONStr) (jsonutil.JSONToken, error) { return str[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONNum(1)}},
		},
		{
			name: "variadic args with mismatched array",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONNum(1)}},
		},
		{
			name: "variadic args with mismatched args",
			fn:   func(a ...jsonutil.JSONStr) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo"), jsonutil.JSONNum(1)},
		},
		{
			name: "variadic args with single mismatched arg",
			fn:   func(a ...jsonutil.JSONNum) (jsonutil.JSONToken, error) { return a[0], nil },
			args: []jsonutil.JSONToken{jsonutil.JSONStr("foo")},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			p, err := FromFunction(test.fn, test.name)
			if err != nil {
				t.Fatalf("FromFunction(%v) failed to create a projector: %v", test.fn, err)
			}

			r, err := p(toNodes(t, test.args), types.NewContext(types.NewRegistry()))
			if err == nil {
				t.Fatalf("<generated projector>(%v, ...) => %v but expected error", test.args, r)
			}
		})
	}
}

func TestFromDefinition_ManagesContext(t *testing.T) {
	reg := types.NewRegistry()

	const name = "myFoo"
	const origValue = "Hello World"
	const shadowedValue = "I pity the foo"

	// This projector will shadow myFoo, we want to make sure it retains
	// the original value once the projector is done. This ensures that
	// the projector pushes and pops the context.
	def := &mpb.ProjectorDefinition{
		Name: "Test",
		Mapping: []*mappb.FieldMapping{
			{
				ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: shadowedValue}},
				Target:      &mappb.FieldMapping_TargetLocalVar{TargetLocalVar: name},
			},
			{
				ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_FromLocalVar{FromLocalVar: name}},
				Target:      &mappb.FieldMapping_TargetObject{TargetObject: "Foo"},
			},
		},
	}

	proj := FromDef(def, parallel)
	ctx := types.NewContext(reg)
	ctx.Variables.Push()

	var origValueToken jsonutil.JSONToken = jsonutil.JSONStr(origValue)

	if err := ctx.Variables.Set(name, &origValueToken); err != nil {
		t.Fatalf("Set(%q, %v) returned unexpected error: %v", name, origValueToken, err)
	}

	if _, err := proj([]jsonutil.JSONMetaNode{}, ctx); err != nil {
		t.Fatalf("projector returned unexpected error: %v", err)
	}

	gotMyFoo, err := ctx.Variables.Get(name)
	if err != nil {
		t.Fatalf("Get(%q) returned unexpected error: %v", name, err)
	}

	if !cmp.Equal(string((*gotMyFoo).(jsonutil.JSONStr)), origValue) {
		t.Errorf("bad value for %s: got %v, want %v", name, *gotMyFoo, origValue)
	}

	if got := ctx.TopLevelObjects["Foo"][0]; !cmp.Equal(string(got.(jsonutil.JSONStr)), shadowedValue) {
		t.Errorf("bad value for top level object: got %v, want %v", got, shadowedValue)
	}
}

func TestFromDefinition_OutputsAnArrayWhenMapping(t *testing.T) {
	reg := types.NewRegistry()

	def := &mpb.ProjectorDefinition{
		Name: "Test",
		Mapping: []*mappb.FieldMapping{
			{
				ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "hello"}},
				Target:      &mappb.FieldMapping_TargetLocalVar{TargetLocalVar: "myvar"},
			},
			{
				ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_FromLocalVar{FromLocalVar: "myvar"}},
				Target:      &mappb.FieldMapping_TargetField{TargetField: "[2].hi"},
			},
		},
	}

	proj := FromDef(def, parallel)
	ctx := types.NewContext(reg)
	ctx.Variables.Push()

	got, err := proj([]jsonutil.JSONMetaNode{}, ctx)
	if err != nil {
		t.Fatalf("projector returned unexpected error: %v", err)
	}

	want, err := jsonutil.UnmarshalJSON(json.RawMessage(`[null, null, {"hi": "hello"}]`))
	if err != nil {
		t.Fatalf("could not unmarshal want JSON: %v", err)
	}

	if diff := cmp.Diff(want, got); diff != "" {
		t.Errorf("projector result was incorrect: -want +got: %s", diff)
	}
}

func TestProjectorsCannotExceedMaxStackDepth(t *testing.T) {
	tests := []struct {
		name       string
		projectors []*mpb.ProjectorDefinition
		wants      []string
	}{
		{
			name: "single-projector cycle",
			projectors: []*mpb.ProjectorDefinition{
				&mpb.ProjectorDefinition{
					Name: "Root",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "hello"}, Projector: "Root"},
						},
					},
				},
			},
			wants: []string{
				fmt.Sprintf("Root: %d", types.MaxStackDepth+1),
			},
		},
		{
			name: "multi-projector cycle",
			projectors: []*mpb.ProjectorDefinition{
				&mpb.ProjectorDefinition{
					Name: "Root",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "hello"}, Projector: "A"},
						},
					},
				},
				&mpb.ProjectorDefinition{
					Name: "A",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "hello"}, Projector: "B"},
						},
					},
				},
				&mpb.ProjectorDefinition{
					Name: "B",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "hello"}, Projector: "A"},
						},
					},
				},
			},
			wants: []string{
				fmt.Sprintf("A: %d", types.MaxStackDepth/2),
				fmt.Sprintf("B: %d", types.MaxStackDepth/2),
				"Root: 1",
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			reg := types.NewRegistry()

			for _, p := range test.projectors {
				if err := reg.RegisterProjector(p.Name, FromDef(p, parallel)); err != nil {
					t.Fatalf("failed to register test projector %s: %v", p.Name, err)
				}
			}

			ctx := types.NewContext(reg)
			ctx.Variables.Push()

			root, err := reg.FindProjector("Root")
			if err != nil {
				t.Fatalf("registry could not find Root test projector: %v", err)
			}

			_, err = root([]jsonutil.JSONMetaNode{}, ctx)
			if err == nil {
				t.Fatalf("expected stack overflow error, got nil")
			}
			errStr := err.Error()

			for _, w := range test.wants {
				if !strings.Contains(errStr, w) {
					t.Errorf("expected stack overflow error to contain %s, but it was %s", w, errStr)
				}
			}
		})
	}
}

func TestProjectorsCanNestUpToMaxStackDepth(t *testing.T) {
	tests := []struct {
		name       string
		projectors []*mpb.ProjectorDefinition
	}{
		{
			name: "single-projector cycle",
			projectors: []*mpb.ProjectorDefinition{
				&mpb.ProjectorDefinition{
					Name: "Root",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstInt{ConstInt: 1}, Projector: "Loop"},
						},
					},
				},
				&mpb.ProjectorDefinition{
					Name: "Loop",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
							},
						},
						{
							Target: &mappb.FieldMapping_TargetField{TargetField: ".!"},
							Condition: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
								AdditionalArg: []*mpb.ValueSource{
									{
										Source: &mappb.ValueSource_ConstInt{ConstInt: types.MaxStackDepth - 1},
									},
								},
								Projector: "$Lt",
							},
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_ProjectedValue{
									ProjectedValue: &mappb.ValueSource{
										Source: &mappb.ValueSource_FromArg{FromArg: 1},
										AdditionalArg: []*mpb.ValueSource{
											{
												Source: &mappb.ValueSource_ConstInt{ConstInt: 1},
											},
										},
										Projector: "$Sum",
									},
								},
								Projector: "Loop",
							},
						},
					},
				},
			},
		},
		{
			name: "multi-projector cycle",
			projectors: []*mpb.ProjectorDefinition{
				&mpb.ProjectorDefinition{
					Name: "Root",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{Source: &mappb.ValueSource_ConstInt{ConstInt: 1}, Projector: "A"},
						},
					},
				},
				&mpb.ProjectorDefinition{
					Name: "A",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
							},
						},
						{
							Target: &mappb.FieldMapping_TargetField{TargetField: ".!"},
							Condition: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
								AdditionalArg: []*mpb.ValueSource{
									{
										Source: &mappb.ValueSource_ConstInt{ConstInt: types.MaxStackDepth - 1},
									},
								},
								Projector: "$Lt",
							},
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_ProjectedValue{
									ProjectedValue: &mappb.ValueSource{
										Source: &mappb.ValueSource_FromArg{FromArg: 1},
										AdditionalArg: []*mpb.ValueSource{
											{
												Source: &mappb.ValueSource_ConstInt{ConstInt: 1},
											},
										},
										Projector: "$Sum",
									},
								},
								Projector: "B",
							},
						},
					},
				},
				&mpb.ProjectorDefinition{
					Name: "B",
					Mapping: []*mappb.FieldMapping{
						{
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
							},
						},
						{
							Target: &mappb.FieldMapping_TargetField{TargetField: ".!"},
							Condition: &mappb.ValueSource{
								Source: &mappb.ValueSource_FromArg{FromArg: 1},
								AdditionalArg: []*mpb.ValueSource{
									{
										Source: &mappb.ValueSource_ConstInt{ConstInt: types.MaxStackDepth - 1},
									},
								},
								Projector: "$Lt",
							},
							ValueSource: &mappb.ValueSource{
								Source: &mappb.ValueSource_ProjectedValue{
									ProjectedValue: &mappb.ValueSource{
										Source: &mappb.ValueSource_FromArg{FromArg: 1},
										AdditionalArg: []*mpb.ValueSource{
											{
												Source: &mappb.ValueSource_ConstInt{ConstInt: 1},
											},
										},
										Projector: "$Sum",
									},
								},
								Projector: "A",
							},
						},
					},
				},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			reg := types.NewRegistry()
			registerHelpers(t, reg)

			for _, p := range test.projectors {
				if err := reg.RegisterProjector(p.Name, FromDef(p, parallel)); err != nil {
					t.Fatalf("failed to register test projector %s: %v", p.Name, err)
				}
			}

			ctx := types.NewContext(reg)
			ctx.Variables.Push()

			root, err := reg.FindProjector("Root")
			if err != nil {
				t.Fatalf("registry could not find Root test projector: %v", err)
			}

			depth, err := root([]jsonutil.JSONMetaNode{}, ctx)
			if err != nil {
				t.Fatalf("projector Root([], {}, _) got unexpected error %v", err)
			}

			depthNum, ok := depth.(jsonutil.JSONNum)
			if !ok || depthNum != types.MaxStackDepth-1 {
				t.Errorf("projector Root([], {}, _) got %v, want %v", depth, types.MaxStackDepth-1)
			}
		})
	}
}

func registerHelpers(t *testing.T, registry *types.Registry) {
	t.Helper()

	helpers := map[string]types.Projector{
		"$Lt": func(arguments []jsonutil.JSONMetaNode, pctx *types.Context) (token jsonutil.JSONToken, e error) {
			a, err := jsonutil.NodeToToken(arguments[0])
			if err != nil {
				return nil, err
			}
			b, err := jsonutil.NodeToToken(arguments[1])
			if err != nil {
				return nil, err
			}

			return jsonutil.JSONBool(a.(jsonutil.JSONNum) < b.(jsonutil.JSONNum)), nil
		},
		"$Sum": func(arguments []jsonutil.JSONMetaNode, pctx *types.Context) (token jsonutil.JSONToken, e error) {
			a, err := jsonutil.NodeToToken(arguments[0])
			if err != nil {
				return nil, err
			}
			b, err := jsonutil.NodeToToken(arguments[1])
			if err != nil {
				return nil, err
			}

			return jsonutil.JSONNum(a.(jsonutil.JSONNum) + b.(jsonutil.JSONNum)), nil
		},
	}

	for name, proj := range helpers {
		if err := registry.RegisterProjector(name, proj); err != nil {
			t.Fatalf("failed to register helper projector %s: %v", name, err)
		}
	}
}
