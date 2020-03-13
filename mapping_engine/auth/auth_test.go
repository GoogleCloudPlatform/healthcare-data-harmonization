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

package auth

import (
	"strconv"
	"testing"

	httppb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

func TestRegisterFetchProjector(t *testing.T) {
	tests := []struct {
		name    string
		servers []*httppb.ServerDefinition
	}{
		{
			name: "load multiple servers",
			servers: []*httppb.ServerDefinition{
				{
					Name: "fhir_store1",
					AuthenticationConfig: &httppb.ServerDefinition_CustomGcp{
						CustomGcp: &httppb.CustomGCPConfig{
							ClientSecretFile: &httppb.Location{
								Location: &httppb.Location_GcsLocation{
									GcsLocation: "",
								},
							},
							Scopes: []string{},
						},
					},
				},
				{
					Name: "fhir_store2",
					AuthenticationConfig: &httppb.ServerDefinition_CustomGcp{
						CustomGcp: &httppb.CustomGCPConfig{
							ClientSecretFile: &httppb.Location{
								Location: &httppb.Location_GcsLocation{
									GcsLocation: "",
								},
							},
							Scopes: []string{},
						},
					},
				},
				{
					Name: "fhir_store3",
					AuthenticationConfig: &httppb.ServerDefinition_CustomGcp{
						CustomGcp: &httppb.CustomGCPConfig{
							ClientSecretFile: &httppb.Location{
								Location: &httppb.Location_GcsLocation{
									GcsLocation: "",
								},
							},
							Scopes: []string{},
						},
					},
				},
			},
		},
		{
			name: "load single server",
			servers: []*httppb.ServerDefinition{
				{
					Name: "fhir_store1",
					AuthenticationConfig: &httppb.ServerDefinition_CustomGcp{
						CustomGcp: &httppb.CustomGCPConfig{
							ClientSecretFile: &httppb.Location{
								Location: &httppb.Location_GcsLocation{
									GcsLocation: "",
								},
							},
							Scopes: []string{},
						},
					},
				},
			},
		},
		{
			name:    "load empty config",
			servers: []*httppb.ServerDefinition{},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			Clear()
			if err := LoadServerConfigs(test.servers); err != nil {
				t.Fatalf("LoadServerConfigs in test %s returned unexpected error %v", test.name, err)
			}

			actualCount := len(servers)
			expectedCount := len(test.servers)
			if actualCount != expectedCount {
				t.Fatalf("server count mismatch in test %s, got %v, but expected %v",
					test.name, strconv.Itoa(actualCount), strconv.Itoa(expectedCount))
			}
		})
	}
}
