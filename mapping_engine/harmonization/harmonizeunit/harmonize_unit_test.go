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

package harmonizeunit

import (
	"encoding/json"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizecode" /* copybara-comment: harmonizecode */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */

	ucpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: unit_config_go_proto */
)

func TestHarmonizeUnits(t *testing.T) {
	tests := []struct {
		name            string
		unitConversions *ucpb.UnitConfiguration
		sourceQuantity  float64
		sourceUnit      string
		expectedOutput  json.RawMessage
		harmonizedCodes []harmonizecode.HarmonizedCode
	}{
		{
			name: "convert with no conversion configs",
			unitConversions: &ucpb.UnitConfiguration{
				Version: "xyz",
				System:  "http://unitsofmeasure.org",
			},
			sourceQuantity: 123,
			sourceUnit:     "foo",
			expectedOutput: json.RawMessage(`{
				"quantity": 123,
				"unit": "foo",
				"system": "urn:unharmonized-unit",
				"version": "xyz",
				"originalQuantity": 123,
				"originalUnit": "foo"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "convert with empty conversion configs",
			unitConversions: &ucpb.UnitConfiguration{
				Version:    "xyz",
				System:     "http://unitsofmeasure.org",
				Decimals:   3,
				Conversion: []*ucpb.UnitConversion{},
			},
			sourceQuantity: 1.234,
			sourceUnit:     "liters",
			expectedOutput: json.RawMessage(`{
				"quantity": 1.234,
				"unit": "liters",
				"system": "urn:unharmonized-unit",
				"version": "xyz",
				"originalQuantity": 1.234,
				"originalUnit": "liters"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "map and convert unit",
			unitConversions: &ucpb.UnitConfiguration{
				Version:  "xyz",
				System:   "http://unitsofmeasure.org",
				Decimals: 6,
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit:    []string{"F"},
						DestUnit:      "CEL",
						Constant:      -32,
						Scalar:        0.55555555555,
						ConstantFirst: true,
					},
				},
			},
			sourceQuantity: 0,
			sourceUnit:     "F",
			expectedOutput: json.RawMessage(`{
				"quantity": -17.777778,
				"unit": "CEL",
				"system": "http://unitsofmeasure.org",
				"version": "xyz",
				"originalQuantity": 0,
				"originalUnit": "F"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "map and convert unit without system",
			unitConversions: &ucpb.UnitConfiguration{
				Version:  "xyz",
				Decimals: 6,
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit:    []string{"F"},
						DestUnit:      "CEL",
						Constant:      -32,
						Scalar:        0.55555555555,
						ConstantFirst: true,
					},
				},
			},
			sourceQuantity: 0,
			sourceUnit:     "F",
			expectedOutput: json.RawMessage(`{
				"quantity": -17.777778,
				"unit": "CEL",
				"version": "xyz",
				"system": "",
				"originalQuantity": 0,
				"originalUnit": "F"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "map and convert unit without specifying decimals",
			unitConversions: &ucpb.UnitConfiguration{
				Version: "xyz",
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit:    []string{"F"},
						DestUnit:      "CEL",
						Constant:      -32,
						Scalar:        0.55555555555,
						ConstantFirst: true,
					},
				},
			},
			sourceQuantity: 0,
			sourceUnit:     "F",
			expectedOutput: json.RawMessage(`{
				"quantity": -17.7777777776,
				"unit": "CEL",
				"version": "xyz",
				"system": "",
				"originalQuantity": 0,
				"originalUnit": "F"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "map and convert unit with codes",
			unitConversions: &ucpb.UnitConfiguration{
				Version: "xyz",
				System:  "http://unitsofmeasure.org",
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"L"},
						DestUnit:   "ML_PER_BREATH",
						Constant:   0,
						Scalar:     1000,
						Code:       "vt_obs",
						Codesystem: "urn:google-health-research:harmonized:valueset-observation-name",
					},
				},
			},
			sourceQuantity: 1.234,
			sourceUnit:     "L",
			expectedOutput: json.RawMessage(`{
				"quantity": 1234,
				"unit": "ML_PER_BREATH",
				"system": "http://unitsofmeasure.org",
				"version": "xyz",
				"originalQuantity": 1.234,
				"originalUnit": "L"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{
				harmonizecode.HarmonizedCode{
					Code:   "vt_obs",
					System: "urn:google-health-research:harmonized:valueset-observation-name",
				},
			},
		},
		{
			name: "map and convert unit with mismatched codes",
			unitConversions: &ucpb.UnitConfiguration{
				Version: "xyz",
				System:  "http://unitsofmeasure.org",
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"L"},
						DestUnit:   "ML_PER_BREATH",
						Constant:   0,
						Scalar:     1000,
						Code:       "vt_obs",
						Codesystem: "urn:google-health-research:harmonized:valueset-observation-name",
					},
				},
			},
			sourceQuantity: 1.234,
			sourceUnit:     "L",
			expectedOutput: json.RawMessage(`{
				"quantity": 1.234,
				"unit": "L",
				"system": "urn:unharmonized-unit",
				"version": "xyz",
				"originalQuantity": 1.234,
				"originalUnit": "L"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{
				harmonizecode.HarmonizedCode{
					Code:   "vt_spont",
					System: "urn:google-health-research:harmonized:valueset-observation-name",
				},
			},
		},
		{
			name: "convert with multiple possible source units",
			unitConversions: &ucpb.UnitConfiguration{
				Version: "xyz",
				System:  "http://unitsofmeasure.org",
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"L", "liters", "litres", "l"},
						DestUnit:   "ML_PER_BREATH",
						Constant:   0,
						Scalar:     1000,
					},
				},
			},
			sourceQuantity: 1.234,
			sourceUnit:     "liters",
			expectedOutput: json.RawMessage(`{
				"quantity": 1234,
				"unit": "ML_PER_BREATH",
				"system": "http://unitsofmeasure.org",
				"version": "xyz",
				"originalQuantity": 1.234,
				"originalUnit": "liters"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
		{
			name: "convert with extra decimals specified",
			unitConversions: &ucpb.UnitConfiguration{
				Version:  "xyz",
				System:   "http://unitsofmeasure.org",
				Decimals: 8,
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"liters"},
						DestUnit:   "ML_PER_BREATH",
						Constant:   0,
						Scalar:     1000,
					},
				},
			},
			sourceQuantity: 1.234,
			sourceUnit:     "liters",
			expectedOutput: json.RawMessage(`{
				"quantity": 1234,
				"unit": "ML_PER_BREATH",
				"system": "http://unitsofmeasure.org",
				"version": "xyz",
				"originalQuantity": 1.234,
				"originalUnit": "liters"
			}`),
			harmonizedCodes: []harmonizecode.HarmonizedCode{},
		},
	}
	for _, test := range tests {

		t.Run(test.name, func(t *testing.T) {
			harmonizer, err := MakeLocalUnitHarmonizer(test.unitConversions)
			if err != nil {
				t.Fatalf("MakeLocalUnitHarmonizer in test %s returned unexpected error %v.", test.name, err)
			}

			result, err := harmonizer.Harmonize(test.sourceQuantity, test.sourceUnit, test.harmonizedCodes)
			if err != nil {
				t.Fatalf("Harmonization in test %s returned unexpected error %v.", test.name, err)
			}

			actualOutput := result.ToJSONContainer()
			var expectedOutput jsonutil.JSONContainer
			expectedOutput.UnmarshalJSON(test.expectedOutput)

			if diff := cmp.Diff(expectedOutput, actualOutput); diff != "" {
				t.Errorf("Harmonize(%v, %s) => diff -%v +%v\n%s", test.sourceQuantity, test.sourceUnit, expectedOutput, actualOutput, diff)
			}
		})
	}
}

func TestLoadUnitHarmonizationProjectors_Error(t *testing.T) {
	tests := []struct {
		name            string
		unitConversions *ucpb.UnitConfiguration
	}{
		{
			name: "duplicate conversions",
			unitConversions: &ucpb.UnitConfiguration{
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"L"},
						DestUnit:   "ML_PER_BREATH",
						Constant:   0,
						Scalar:     1000,
					},
					{
						SourceUnit: []string{"L"},
						DestUnit:   "ML_PER_BEAT",
						Constant:   0,
						Scalar:     1000,
					},
				},
			},
		},
		{
			name: "missing sources",
			unitConversions: &ucpb.UnitConfiguration{
				Conversion: []*ucpb.UnitConversion{
					{
						DestUnit: "ML_PER_BREATH",
						Constant: 0,
						Scalar:   1000,
					},
				},
			},
		},
		{
			name: "missing destination",
			unitConversions: &ucpb.UnitConfiguration{
				Conversion: []*ucpb.UnitConversion{
					{
						SourceUnit: []string{"L"},
						Constant:   0,
						Scalar:     1000,
					},
				},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := MakeLocalUnitHarmonizer(test.unitConversions)
			if err == nil {
				t.Fatalf("MakeLocalUnitHarmonizer in test %s did not return an error as expected", test.name)
			}
		})
	}
}
