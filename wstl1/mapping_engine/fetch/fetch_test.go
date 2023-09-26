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

package fetch

import (
	"context"
	"strconv"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestRegisterFetchProjector(t *testing.T) {
	tests := []struct {
		name           string
		httpProjectors []*httppb.HttpFetchQuery
	}{
		{
			name: "single fetch",
			httpProjectors: []*httppb.HttpFetchQuery{
				{
					Name: "id_fetch",
					RequestMethod: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "GET",
						},
					},
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_url",
						},
						AdditionalArg: []*mappb.ValueSource{
							{
								Source: &mappb.ValueSource_ConstString{
									ConstString: "sample_url",
								},
							},
						},
					},
				},
			},
		},
		{
			name: "multiple fetch",
			httpProjectors: []*httppb.HttpFetchQuery{
				{
					Name: "id_fetch",
					RequestMethod: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "GET",
						},
					},
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_id",
						},
					},
				},
				{
					Name: "name_fetch",
					RequestMethod: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "GET",
						},
					},
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_name",
						},
					},
				},
				{
					Name: "location_fetch",
					RequestMethod: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "GET",
						},
					},
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_location",
						},
					},
				},
			},
		},
		{
			name:           "empty fetch",
			httpProjectors: []*httppb.HttpFetchQuery{},
		},
	}
	for _, test := range tests {
		reg := types.NewRegistry()
		t.Run(test.name, func(t *testing.T) {
			if err := LoadFetchProjectors(context.Background(), reg, test.httpProjectors); err != nil {
				t.Fatalf("LoadFetchProjectors in test %s returned unexpected error %v.", test.name, err)
			}

			actualCount := reg.Count()

			// + 1 for identity
			expectedCount := len(test.httpProjectors) + 1
			if actualCount != expectedCount {
				t.Fatalf("Projector count mismatch in test %s, got %v projectors, but expected %v.",
					test.name, strconv.Itoa(actualCount), strconv.Itoa(expectedCount))
			}

			for _, query := range test.httpProjectors {
				if _, err := reg.FindProjector(query.GetName()); err != nil {
					t.Fatalf("LoadFetchProjectors in test %s succeeded but registry had no key %s.", test.name, query.GetName())
				}
			}
		})
	}
}

func TestRegisterFetchProjector_Error(t *testing.T) {
	tests := []struct {
		name           string
		httpProjectors []*httppb.HttpFetchQuery
	}{
		{
			name: "duplicate names",
			httpProjectors: []*httppb.HttpFetchQuery{
				{
					Name: "id_fetch",
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_url",
						},
						AdditionalArg: []*mappb.ValueSource{
							{
								Source: &mappb.ValueSource_ConstString{
									ConstString: "sample_url",
								},
							},
						},
					},
				},
				{
					Name: "id_fetch",
					RequestUrl: &mappb.ValueSource{
						Source: &mappb.ValueSource_ConstString{
							ConstString: "sample_url",
						},
						AdditionalArg: []*mappb.ValueSource{
							{
								Source: &mappb.ValueSource_ConstString{
									ConstString: "sample_url",
								},
							},
						},
					},
				},
			},
		},
	}
	for _, test := range tests {
		reg := types.NewRegistry()
		t.Run(test.name, func(t *testing.T) {
			if err := LoadFetchProjectors(context.Background(), reg, test.httpProjectors); err == nil {
				t.Fatalf("LoadFetchProjectors in test %s should have returned an error but did not.", test.name)
			}
		})
	}
}
