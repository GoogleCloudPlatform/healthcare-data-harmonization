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

// Package optrace_test contains tests for the optrace package.
package optrace_test

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"reflect"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */
	"google.golang.org/protobuf/testing/protocmp" /* copybara-comment: protocmp */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/cloudfunction" /* copybara-comment: cloudfunction */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/fetch" /* copybara-comment: fetch */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/optrace" /* copybara-comment: optrace */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/postprocess" /* copybara-comment: postprocess */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types/register_all" /* copybara-comment: registerall */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */

)

const (
	parallel = false
)

func emptyOutput() *jsonutil.JSONToken {
	var output jsonutil.JSONToken

	return &output
}

func TestTracesAdded(t *testing.T) {
	reg := types.NewRegistry()
	if err := registerall.RegisterAll(reg); err != nil {
		t.Fatalf("failed to register builtins: %v", err)
	}

	server := setupMockFetchServer(t)
	defer server.Close()

	tests := []struct {
		desc          string
		generateOpsFn func(pctx *types.Context, t *testing.T) []optrace.Op
	}{
		{
			desc: "config-based projectors",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				var args []jsonutil.JSONMetaNode

				ctxStart := pctx.String()

				proj := projector.FromDef(&mappb.ProjectorDefinition{Name: "test"}, parallel)
				res, err := proj(args, pctx)

				if err != nil {
					t.Fatalf("projector call got unexpected error: %v", err)
				}

				ctxEnd := pctx.String()

				return []optrace.Op{
					optrace.OpStartProjectorCall{
						ProjectorName: "test",
						Args:          args,
						CtxSnapshot:   ctxStart,
					},
					optrace.OpEndProjectorCall{
						ProjectorName: "test",
						CtxSnapshot:   ctxEnd,
						Result:        res,
					},
				}
			},
		},
		{
			desc: "function-based projectors",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				proj, err := projector.FromFunction(builtins.IsNil, "IsNil")
				if err != nil {
					t.Fatalf("failed to build projector from IsNil: %v", err)
				}

				ctxStart := pctx.String()

				args := []jsonutil.JSONMetaNode{jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONNum(1)}}

				res, err := proj(args, pctx)
				if err != nil {
					t.Fatalf("projector call got unexpected error: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartProjectorCall{
						ProjectorName: "IsNil",
						Args:          args,
						CtxSnapshot:   ctxStart,
					},
					optrace.OpEndProjectorCall{
						ProjectorName: "IsNil",
						CtxSnapshot:   "<same as start>",
						Result:        res,
					},
				}
			},
		},
		{
			desc: "cloud-function-based projectors",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				s := httptest.NewServer(http.HandlerFunc(
					func(w http.ResponseWriter, r *http.Request) {
						if r.URL.String() != "/zero" {
							w.WriteHeader(http.StatusNotImplemented)
							fmt.Fprintf(w, "error: cannot find mock cloud function %s", r.URL)
							return
						}
						fmt.Fprintf(w, "%d", 0)
					},
				))
				cf := httppb.CloudFunction{
					Name:       "@Zero",
					RequestUrl: s.URL + "/zero",
				}

				proj, err := cloudfunction.FromCloudFunction(&cf)
				if err != nil {
					t.Fatalf("failed to build project from cloud function %v: %v", cf.String(), err)
				}

				ctxStart := pctx.String()
				args := []jsonutil.JSONMetaNode{}

				_, err = proj(args, pctx)

				if err != nil {
					t.Fatalf("cloud function call got unexpected error: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartCloudFunctionCall{
						RequestURL:  cf.RequestUrl,
						Args:        args,
						CtxSnapshot: ctxStart,
					},
					optrace.OpEndCloudFunctionCall{
						RequestURL:  cf.RequestUrl,
						CtxSnapshot: "<same as start>",
						Status:      "200 OK",
					},
				}
			},
		},
		{
			desc: "context scan",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				_, err := mapping.EvaluateArgSource(&mappb.ValueSource_InputSource{Field: "context.foo.bar"},
					[]jsonutil.JSONMetaNode{jsonutil.JSONMetaPrimitiveNode{}, jsonutil.JSONMetaPrimitiveNode{}}, pctx)

				if err != nil {
					t.Fatalf("failed to call EvaluateArgSource: %v", err)
				}

				return []optrace.Op{
					optrace.OpContextScan{
						Value:   nil,
						RemSegs: nil,
						Segs:    []string{"context", "foo", "bar"},
					},
				}
			},
		},
		{
			desc: "value source resolution",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				_, err := mapping.EvaluateValueSource(&mappb.ValueSource{Source: &mappb.ValueSource_ConstString{ConstString: "Foo"}},
					[]jsonutil.JSONMetaNode{},
					&jsonutil.JSONContainer{}, pctx)

				if err != nil {
					t.Fatalf("failed to call EvaluateValueSource: %v", err)
				}

				return []optrace.Op{
					optrace.OpValueResolved{
						Value:  jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONStr("Foo")},
						Source: "const string",
					},
				}
			},
		},
		{
			desc: "field mapping",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				field := &mappb.FieldMapping{
					ValueSource: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{ConstString: "Foo"},
					},
					Target: &mappb.FieldMapping_TargetField{TargetField: "foo"},
				}
				err := mapping.ProcessMappingSequential(
					field,
					[]jsonutil.JSONMetaNode{},
					emptyOutput(), pctx)

				if err != nil {
					t.Fatalf("failed to call ProcessMappingSequential: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartMapping{
						Field: field,
					},
					optrace.OpEndMapping{
						Field:  field,
						Result: jsonutil.JSONStr("Foo"),
					},
				}
			},
		},
		{
			desc: "condition check",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				err := mapping.ProcessMappingSequential(
					&mappb.FieldMapping{
						ValueSource: &mappb.ValueSource{
							Source: &mappb.ValueSource_ConstString{ConstString: "Foo"},
						},
						Target: &mappb.FieldMapping_TargetField{TargetField: "foo"},
						Condition: &mappb.ValueSource{
							Source: &mappb.ValueSource_ConstBool{ConstBool: true},
						},
					},
					[]jsonutil.JSONMetaNode{},
					emptyOutput(), pctx)

				if err != nil {
					t.Fatalf("failed to call ProcessMappingSequential: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartConditionCheck{}, optrace.OpEndConditionCheck{Result: true},
				}
			},
		},
		{
			desc: "failing condition check",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				field := &mappb.FieldMapping{
					ValueSource: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{ConstString: "Foo"},
					},
					Target: &mappb.FieldMapping_TargetField{TargetField: "foo"},
					Condition: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstBool{ConstBool: false},
					},
				}

				err := mapping.ProcessMappingSequential(
					field,
					[]jsonutil.JSONMetaNode{},
					emptyOutput(), pctx)

				if err != nil {
					t.Fatalf("failed to call ProcessMappingSequential: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartConditionCheck{}, optrace.OpEndConditionCheck{Result: false}, optrace.OpEndMapping{Field: field},
				}
			},
		},
		{
			desc: "expect bundler trace to contain start and end bundle operations",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				if _, err := postprocess.Process(pctx, &mappb.MappingConfig{
					PostProcess: &mappb.MappingConfig_PostProcessProjectorName{
						PostProcessProjectorName: ""}}, false, parallel); err != nil {
					t.Fatalf("failed to call postprocess.Process: %v", err)
				}

				return []optrace.Op{
					optrace.OpStartBundler{
						Input: nil,
					}, optrace.OpEndBundler{
						Output: nil,
					},
				}
			},
		},
		{
			desc: "expect fetch trace to contain start and end fetch operations",
			generateOpsFn: func(pctx *types.Context, t *testing.T) []optrace.Op {
				fetchQueries := []*httppb.HttpFetchQuery{
					{
						Name: "fetch",
						RequestMethod: &mappb.ValueSource{
							Source: &mappb.ValueSource_ConstString{
								ConstString: "GET",
							},
						},
						RequestUrl: &mappb.ValueSource{
							Source: &mappb.ValueSource_ConstString{
								ConstString: server.URL,
							},
						},
					},
				}

				if err := fetch.LoadFetchProjectors(context.Background(), reg, fetchQueries); err != nil {
					t.Fatalf("loadFetchProjectors returned unexpected error %v", err)
				}

				proj, err := reg.FindProjector("fetch")
				if err != nil {
					t.Fatalf("could not find fetch projector, error is %v", err)
				}

				var args []jsonutil.JSONMetaNode
				if _, err := proj(args, pctx); err != nil {
					t.Fatalf("projector call got unexpected error: %v", err)
				}

				fetchResult := json.RawMessage(`{}`)
				return []optrace.Op{
					optrace.OpStartFetchCall{
						URL:    server.URL,
						Method: "GET",
					},
					optrace.OpEndFetchCall{
						Response: &fetchResult,
						Error:    nil,
					},
				}
			},
		},
	}
	for _, test := range tests {
		t.Run(test.desc, func(t *testing.T) {
			pctx := types.NewContext(reg)
			wantOps := test.generateOpsFn(pctx, t)

			wantOpFromName := make(map[string]optrace.Op)
			for _, op := range wantOps {
				wantOpFromName[reflect.TypeOf(op).Name()] = op
			}

			gotOps := pctx.Trace.Ops()

			ignore := cmpopts.IgnoreUnexported(
				jsonutil.JSONMeta{}, optrace.OpStartProjectorCall{}, optrace.OpEndProjectorCall{},
				optrace.OpStartCloudFunctionCall{}, optrace.OpEndCloudFunctionCall{},
				optrace.OpStartMapping{}, optrace.OpEndMapping{}, optrace.OpStartBundler{},
				optrace.OpEndBundler{}, optrace.OpStartConditionCheck{}, optrace.OpEndConditionCheck{},
				optrace.OpStartFetchCall{}, optrace.OpEndFetchCall{})

			// Got should be a superset of want (doesn't need to be equal).
			for _, gotOp := range gotOps {
				name := reflect.TypeOf(gotOp).Name()
				wantOp, ok := wantOpFromName[name]
				if diff := cmp.Diff(gotOp, wantOp, ignore, protocmp.Transform()); ok && diff != "" {
					t.Errorf("trace got op got %v, want %v, diff:\n %s", gotOp, wantOp, diff)
				}
				delete(wantOpFromName, name)
			}

			if len(wantOpFromName) > 0 {
				t.Errorf("gotOps was missing ops: %v", wantOpFromName)
			}
		})
	}
}

func TestString(t *testing.T) {
	trace := optrace.Trace{}

	// Test some predictable ops that exercise indentation
	trace.StartConditionCheck()
	trace.ValueResolved(nil, "test")
	trace.EndConditionCheck(true)

	got := trace.String()
	want := `checking condition...
	resolved test as <nil>
	condition is true
`
	if diff := cmp.Diff(got, want); diff != "" {
		t.Fatalf("trace.String() got\n%s\n want\n%s\n diff\n%s", got, want, diff)
	}
}

func setupMockFetchServer(t *testing.T) *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(
		func(w http.ResponseWriter, r *http.Request) {
			w.WriteHeader(http.StatusOK)
			w.Write(json.RawMessage(`{}`))
			return
		},
	))
}
