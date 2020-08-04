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

package wstlserver

import (
	"context"
	"fmt"
	"testing"

	wspb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/tools/notebook/extensions/wstl/proto" /* copybara-comment: wstlservice_go_proto */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"google.golang.org/grpc/codes" /* copybara-comment: codes */
	"google.golang.org/grpc/status" /* copybara-comment: status */
)

type mockStorageClient struct {
	kv map[string]string
}

func (s *mockStorageClient) ReadBytes(ctx context.Context, bucket string, filename string) ([]byte, error) {
	path := "gs://" + bucket + "/" + filename
	if v, ok := s.kv[path]; ok {
		return []byte(v), nil
	}
	return nil, fmt.Errorf("missing file %q", path)
}

func TestContext_EvaluateTransformation(t *testing.T) {
	tests := []struct {
		name       string
		client     *mockStorageClient
		request    *wspb.IncrementalTransformRequest
		want       []*wspb.TransformedRecords
		wantErrors bool
	}{
		{
			name:   "execute stand alone whistle no input",
			client: &mockStorageClient{},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: \"dummy_value\"",
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"dummy_value"}`,
					},
				},
			},
			wantErrors: false,
		},
		{
			name:   "execute whistle with input",
			client: &mockStorageClient{},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: $ToUpper($root.input_key)",
				Input: []*wspb.Location{
					&wspb.Location{
						Location: &wspb.Location_InlineJson{
							InlineJson: `{"input_key":"dummy_value"}`,
						},
					},
				},
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"DUMMY_VALUE"}`,
					},
				},
			},
			wantErrors: false,
		},
		{
			name:   "execute whistle with input on GCS",
			client: &mockStorageClient{kv: map[string]string{"gs://dummy/config.wstl": `{"input_key":"dummy_value"}`}},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: $ToUpper($root.input_key)",
				Input: []*wspb.Location{
					&wspb.Location{
						Location: &wspb.Location_GcsLocation{
							GcsLocation: "gs://dummy/config.wstl",
						},
					},
				},
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"DUMMY_VALUE"}`,
					},
				},
			},
			wantErrors: false,
		},
		{
			name:   "execute whistle with repeated input",
			client: &mockStorageClient{},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: $ToUpper($root.input_key)",
				Input: []*wspb.Location{
					&wspb.Location{
						Location: &wspb.Location_InlineJson{
							InlineJson: `{"input_key":"dummy_value_1"}`,
						},
					},
					&wspb.Location{
						Location: &wspb.Location_InlineJson{
							InlineJson: `{"input_key":"dummy_value_2"}`,
						},
					},
				},
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"DUMMY_VALUE_1"}`,
					},
				},
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"DUMMY_VALUE_2"}`,
					},
				},
			},
			wantErrors: false,
		},
		{
			name:   "repeated input with one invalid input",
			client: &mockStorageClient{},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: $ToUpper($root.input_key)",
				Input: []*wspb.Location{
					&wspb.Location{
						Location: &wspb.Location_InlineJson{
							InlineJson: `{"input_key": 9}`,
						},
					},
					&wspb.Location{
						Location: &wspb.Location_InlineJson{
							InlineJson: `{"input_key":"dummy_value_2"}`,
						},
					},
				},
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Error{
						Error: status.New(codes.InvalidArgument, "Place holder").Proto(),
					},
				},
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Output{
						Output: `{"Output":"DUMMY_VALUE_2"}`,
					},
				},
			},
			wantErrors: false,
		},
		{
			name:       "nil request",
			client:     &mockStorageClient{},
			request:    nil,
			want:       []*wspb.TransformedRecords{},
			wantErrors: true,
		},
		{
			name:   "empty whistle config",
			client: &mockStorageClient{},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "",
			},
			want:       []*wspb.TransformedRecords{},
			wantErrors: true,
		},
		{
			name:   "missing input on GCS",
			client: &mockStorageClient{kv: map[string]string{}},
			request: &wspb.IncrementalTransformRequest{
				Wstl: "Output: $ToUpper($root.input_key)",
				Input: []*wspb.Location{
					&wspb.Location{
						Location: &wspb.Location_GcsLocation{
							GcsLocation: "gs://dummy/config.wstl",
						},
					},
				},
			},
			want: []*wspb.TransformedRecords{
				&wspb.TransformedRecords{
					Record: &wspb.TransformedRecords_Error{
						Error: status.New(codes.InvalidArgument, "Place holder").Proto(),
					},
				},
			},
			wantErrors: false,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			conn, err := NewContext(test.client)
			if err != nil {
				t.Fatalf("failed to instantiate context due to err: %v", err)
			}

			got, err := conn.EvaluateIncrementalTransformation(test.request)
			if err == nil && test.wantErrors {
				t.Fatalf("expected error executing EvaluateTransformation(%v)", test.request)
			} else if err != nil && !test.wantErrors {
				t.Fatalf("EvaluateTransformation failed with error: %v", err)
			}

			if len(got) != len(test.want) {
				t.Fatalf("EvaluateTransformation(%v) returned results length %d expected %d", test.request, len(got), len(test.want))
			}
			for i, want := range test.want {
				switch w := want.GetRecord().(type) {
				case *wspb.TransformedRecords_Output:
					if diff := cmp.Diff(string(w.Output), got[i].GetOutput()); diff != "" {
						t.Errorf("EvaluateTransformation(%v) returned diff (-want +got):\n%s", test.request, diff)
					}
				case *wspb.TransformedRecords_Error:
					if w.Error == nil && got[i].GetError() != nil {
						t.Errorf("EvaluateTransformation(%v) expected whistle error but got %v", test.request, got[i].GetOutput())
					} else if w.Error != nil && got[i].GetError() == nil {
						t.Errorf("EvaluateTransformation(%v) did not expect whistle error but got %v", test.request, got[i].GetError())
					}
				default:
					t.Fatalf("EvaluateTransformation(%v) returned unknown type: %v", test.request, w)
				}
			}
		})
	}
}
