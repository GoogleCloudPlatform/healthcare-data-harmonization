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

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types/register_all" /* copybara-comment: registerall */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestEvaluateFromVarWhistler(t *testing.T) {
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
			got, err := mapping.EvaluateFromVar(&test.argVs, &test.argPctx, jsonutil.DefaultAccessor{})

			if test.wantErr != (err != nil) || (!test.wantErr && got != test.want) {
				t.Errorf("FindLongestContextField(%v, %v) => %v, %v want %v, %v", test.argVs, test.argPctx, got, err, test.want, test.wantErr)
			}
		})
	}
}

func TestEvaluateValueSourceWhistler(t *testing.T) {
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
		want      jsonutil.JSONMetaNode
	}{
		{
			name: "const source",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_ConstString{
					ConstString: "foo",
				},
			},
			args: []jsonutil.JSONToken{},
			want: mustTokenToNode(t, jsonutil.JSONStr("foo")),
		},
		{
			name: "single source",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromSource{
					FromSource: "foo",
				},
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo":"bar"}`), t)},
			want: mustTokenToNode(t, jsonutil.JSONStr("bar")),
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
			want: mustTokenToNode(t, jsonutil.JSONStr("foo")),
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
			want: mustTokenToNode(t, mustParseContainer(json.RawMessage(`{"foo": {"bar":"foo"}, "bar": "foo"}`), t)),
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
			want: mustTokenToNode(t, mustParseContainer(json.RawMessage(`{"foo": {"bar":"foo"}, "bar": {"foo": "red", "bar": "blue"}}`), t)),
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
			want: mustTokenToNode(t, jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)}),
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
			want: mustTokenToNode(t, mustParseArray(json.RawMessage(`[{"foo":{"meep": 1}, "bar":"bar"}, {"foo":{"meep": 2}, "bar":"bar"}, {"foo":{"meep": 3}, "bar":"bar"}]`), t)),
		},
		{
			name: "enumerated non-first value with zipped args",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_ConstString{
					ConstString: "bar",
				},
				AdditionalArg: []*mappb.ValueSource{
					{
						Source: &mappb.ValueSource_FromInput{
							FromInput: &mappb.ValueSource_InputSource{
								Arg:   1,
								Field: "foo[]",
							},
						},
					},
				},
				Projector: "UDFMakeFooBar",
			},
			args: []jsonutil.JSONToken{mustParseContainer(json.RawMessage(`{"foo": [{"meep": 1}, {"meep": 2}, {"meep": 3}]}`), t)},
			want: mustTokenToNode(t, mustParseArray(json.RawMessage(`[{"bar":{"meep": 1}, "foo":"bar"}, {"bar":{"meep": 2}, "foo":"bar"}, {"bar":{"meep": 3}, "foo":"bar"}]`), t)),
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
			want: mustTokenToNode(t, mustParseArray(json.RawMessage(`[{"foo":{"meep": 1}, "bar":"red"}, {"foo":{"meep": 2}, "bar":"green"}, {"foo":{"meep": 3}, "bar":"blue"}]`), t)),
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
			want: mustTokenToNode(t, mustParseContainer(json.RawMessage(`{"foo": [1,2,3], "bar": "hello"}`), t)),
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
			want: mustTokenToNode(t, mustParseContainer(json.RawMessage(`{"foo": null, "bar": "hello"}`), t)),
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
			want: mustTokenToNode(t, mustParseContainer(json.RawMessage(`{"foo": null, "bar": "hello"}`), t)),
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
			want:      mustTokenToNode(t, jsonutil.JSONStr("baz")),
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
			want:    mustTokenToNode(t, jsonutil.JSONStr("baz")),
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
			want:      mustTokenToNode(t, jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)}),
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
			want: mustTokenToNode(t, jsonutil.JSONArr{jsonutil.JSONNum(1), jsonutil.JSONNum(2), jsonutil.JSONNum(3)}),
		},
		{
			name: "from arg - 0 returns all args",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 0,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: mustTokenToNode(t, jsonutil.JSONArr{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)}),
		},
		{
			name: "from arg - 1 returns first arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 1,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: mustTokenToNode(t, jsonutil.JSONNum(99)),
		},
		{
			name: "from arg - len(arg) returns last arg",
			argVs: mappb.ValueSource{
				Source: &mappb.ValueSource_FromArg{
					FromArg: 3,
				},
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(99), jsonutil.JSONNum(98), jsonutil.JSONNum(97)},
			want: mustTokenToNode(t, jsonutil.JSONNum(97)),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pctx := types.NewContext(reg)
			if test.argVars != nil {
				pctx.Variables = test.argVars
			}

			got, err := mapping.EvaluateValueSource(&test.argVs, toNodes(t, test.args), test.argOutput, pctx, jsonutil.DefaultAccessor{})

			if err != nil {
				t.Errorf("evaluateFromSource(%v, %v, %v) unexpected error %v", test.argVs, test.args, pctx, err)
			} else if diff := cmp.Diff(test.want, got, cmpopts.IgnoreUnexported(jsonutil.JSONMeta{})); diff != "" {
				t.Errorf("evaluateFromSource(%v, %v, %v) => %v want %v diff %s", test.argVs, test.args, pctx, got, test.want, diff)
			}
		})
	}
}

func TestEvaluateValueSourceWhistlerErrors(t *testing.T) {
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
			got, err := mapping.EvaluateValueSource(&test.argVs, toNodes(t, test.args), test.argOutput, pctx, jsonutil.DefaultAccessor{})

			if err == nil {
				t.Errorf("evaluateFromSource(%v, %v, %v) expected an error but got %v", test.argVs, test.args, pctx, got)
			}
		})
	}
}

func TestWhistlerEvaluateMapping(t *testing.T) {
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
	registerall.RegisterAll(reg)
	if err := reg.RegisterProjector("MakeObject", proj); err != nil {
		t.Fatalf("failed to register test projector: %v", err)
	}
	st := jsonutil.JSONToken(jsonutil.JSONStr("bar"))

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
			name: "non-bool condition: non-empty string",
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
					TargetField: "",
				},
			},
			want:   jsonutil.JSONStr("foo"),
			wantOk: true,
		},
		{
			name: "non-bool condition: empty string",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "",
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
			name: "non-bool condition: int",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstInt{
						ConstInt: 0,
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
			name: "non-bool condition: nonempty array",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "item1",
					},
					Projector: "$ListOf",
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
			name: "non-bool condition: empty array",
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Projector: "$ListOf",
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
			name: "nonboolean condition: nil object",
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
					TargetField: "",
				},
			},
			wantOk: false,
		},
		{
			name: "nonboolean condition: nonnil object",
			args: jsonutil.JSONArr{jsonutil.JSONContainer(map[string]*jsonutil.JSONToken{
				"foo": &st,
			})},
			mapping: &mappb.FieldMapping{
				Condition: &mappb.ValueSource{
					Source: &mappb.ValueSource_FromInput{
						FromInput: &mappb.ValueSource_InputSource{
							Field: "foo",
						},
					},
				},
				ValueSource: &mappb.ValueSource{
					Source: &mappb.ValueSource_ConstString{
						ConstString: "irrelevant",
					},
				},
				Target: &mappb.FieldMapping_TargetField{
					TargetField: "",
				},
			},
			want:   jsonutil.JSONStr("irrelevant"),
			wantOk: true,
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

			w := mapping.Whistler{}
			err := w.EvaluateMapping(test.mapping, toNodes(t, test.args), &output, pctx)
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
				v, err := jsonutil.GetField(*pctx.Output, test.wantRootField)
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

func TestWhistlerEvaluateMappingErrors(t *testing.T) {
	proj, err := projector.FromFunction(func(kv jsonutil.JSONStr) (jsonutil.JSONContainer, error) {
		var kvt jsonutil.JSONToken = kv
		return jsonutil.JSONContainer{string(kv): &kvt}, nil
	}, "MakeObject")
	if err != nil {
		t.Fatalf("failed to create test projector: %v", err)
	}

	reg := types.NewRegistry()
	registerall.RegisterAll(reg)
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
			pctx.Output = &test.argPctxOutput
			w := mapping.Whistler{}
			err := w.EvaluateMapping(test.mapping, toNodes(t, test.args), &test.argOutput, pctx)
			if err == nil {
				t.Fatalf("ProcessMappingSequential(%v, %v, %v, %v) => expected error, got nil", test.mapping, test.args, test.argOutput, pctx)
			}
		})
	}
}
