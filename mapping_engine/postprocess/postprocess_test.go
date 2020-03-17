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

package postprocess

import (
	"encoding/json"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	libpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: library_go_proto */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const postProcessProjectors = `
	projector {
		name: "BuildFHIRBundleEntryAndID"
		mapping: {
			value_source: {
				from_source: "1"
			}
			target_field: "resource"
		}
		mapping: {
			value_source: {
				from_source: "2"
			}
			target_field: "request.method"
		}
		mapping: {
			value_source: {
				from_destination: "resource.resourceType"
				additional_arg: {
					const_string: "/"
				}
				additional_arg: {
					from_destination: "resource.id"
				}
				projector: "$StrCat"
			}
			target_field: "request.url"
		}
	}

	projector: {
		name: "ExtractValuesFromUnnestedArrays"
		mapping: {
			value_source: {
				from_source: "v"
			}
			target_field: "."
		}
	}

	projector: {
		name: "CreateFHIRTransactionBundleFromResources"
		mapping: {
			value_source: {
				from_source: "."
				additional_arg: {
					const_string: "transaction"
				}
				projector: "CreateFHIRBundleFromResources"
			}
		}
	}

	projector: {
		name: "CreateFHIRBundleFromResources"
		mapping: {
			value_source: {
				from_source: "1"
				projector: "$UnnestArrays"
			}
			target_local_var: "kvs"
		}
		mapping: {
			value_source: {
				from_local_var: "kvs[]"
				projector: "ExtractValuesFromUnnestedArrays"
			}
			target_local_var: "res[]"
		}
		mapping: {
			value_source: {
				from_local_var: "res[]"
				projector: "BuildFHIRBundleEntryAndID"
				additional_arg: {
					const_string: "POST"
				}
			}
			target_local_var: "entries[]"
		}
		mapping: {
			value_source: {
				from_local_var: "entries"
			}
			target_field: "entry"
		}
		mapping: {
			value_source: {
				from_source: "2"
			}
			target_field: "type"
		}
		mapping: {
			value_source: {
				const_string: "Bundle"
			}
			target_field: "resourceType"
		}
	}`

// TODO: Remove these when we have libraries built into the main code.
func LoadLibraryProjectors(t *testing.T) *types.Registry {
	reg := types.NewRegistry()

	if err := builtins.RegisterAll(reg); err != nil {
		t.Fatalf("failed to load builtins %v", err)
	}
	lc := &libpb.LibraryConfig{}
	if err := proto.UnmarshalText(postProcessProjectors, lc); err != nil {
		t.Fatalf("failed to unmarshal post process projectors: %v", err)
	}

	for _, pd := range lc.Projector {
		p := projector.FromDef(pd, false /* parallel */)

		if err := reg.RegisterProjector(pd.Name, p); err != nil {
			t.Fatalf("failed to load library projector %q: %v", pd.Name, err)
		}
	}

	return reg
}

func TestPostProcess(t *testing.T) {
	dummyPatientEntry, err := jsonutil.UnmarshalJSON(json.RawMessage(`{
			"resourceType":"Patient",
			"id":"a"
    }`))
	if err != nil {
		t.Fatalf("test variable for patient entry is invalid, error: %v", err)
	}
	dummyEncounterEntry, err := jsonutil.UnmarshalJSON(json.RawMessage(`{
			"resourceType":"Encounter",
			"id":"a"
    }`))
	if err != nil {
		t.Fatalf("test variable for encounter entry is invalid, error: %v", err)
	}

	reg := LoadLibraryProjectors(t)

	tests := []struct {
		desc         string
		input        map[string][]jsonutil.JSONToken
		want         json.RawMessage
		config       *mappb.MappingConfig
		skipBundling bool
		parallel     bool
	}{
		{
			desc: "generate FHIR STU3 bundle",
			input: map[string][]jsonutil.JSONToken{
				"Patient":   {dummyPatientEntry},
				"Encounter": {dummyEncounterEntry},
			},
			config: &mappb.MappingConfig{
				PostProcess: &mappb.MappingConfig_PostProcessProjectorName{
					PostProcessProjectorName: "CreateFHIRTransactionBundleFromResources"}},
			want: json.RawMessage(`{
				"entry":[
					{
						"resource":{
							"resourceType":"Encounter",
							"id":"a"
						},
						"request":{
							"method":"POST",
							"url":"Encounter/a"
						}
					},
					{
						"resource":{
							"resourceType":"Patient",
							"id":"a"
						},
						"request":{
							"method":"POST",
							"url":"Patient/a"
						}
					}
				],
				"resourceType":"Bundle",
				"type":"transaction"
			}`),
			skipBundling: false,
			parallel:     false,
		},
		{
			desc: "skip bundling",
			input: map[string][]jsonutil.JSONToken{
				"Patient":   {dummyPatientEntry},
				"Encounter": {dummyEncounterEntry},
			},
			config: &mappb.MappingConfig{
				PostProcess: &mappb.MappingConfig_PostProcessProjectorName{
					PostProcessProjectorName: "CreateFHIRTransactionBundleFromResources"}},
			want: json.RawMessage(`{
				"Patient":[
					{
						"resourceType":"Patient",
						"id":"a"
					}
				],
				"Encounter":[
					{
						"resourceType":"Encounter",
						"id":"a"
					}
				]
			}`),
			skipBundling: true,
			parallel:     false,
		},
	}
	for _, test := range tests {
		t.Run(test.desc, func(t *testing.T) {
			pctx := types.NewContext(reg)
			pctx.TopLevelObjects = test.input
			got, err := Process(pctx, test.config, test.skipBundling, test.parallel)
			if err != nil {
				t.Errorf("Process(%v, %v, %v) failed with error: %v",
					pctx, test.config, test.skipBundling, err)
			}

			want, err := jsonutil.UnmarshalJSON(test.want)
			if err != nil {
				t.Errorf("expected result has bad format, jsonutil.UnmarshalJSON(%v) returned error: %v",
					test.want, err)
			}

			if diff := cmp.Diff(got, want); diff != "" {
				t.Errorf("Process(%v, %v, %v) = \n%v\nwant \n%v\ndiff:\n%v", pctx, test.config, test.skipBundling, got, want, diff)
			}
		})
	}
}
