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

package harmonizecode

import (
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

func TestToJsonContainer(t *testing.T) {
	var cstr jsonutil.JSONToken = jsonutil.JSONStr("target-code")
	var sstr jsonutil.JSONToken = jsonutil.JSONStr("target-system")
	var vstr jsonutil.JSONToken = jsonutil.JSONStr("v1")
	var dstr jsonutil.JSONToken = jsonutil.JSONStr("Target Code")
	var estr jsonutil.JSONToken = jsonutil.JSONStr("")

	tests := []struct {
		name string
		hc   HarmonizedCode
		jc   jsonutil.JSONContainer
	}{
		{
			name: "incomplete code",
			hc: HarmonizedCode{
				Code:   "target-code",
				System: "target-system",
			},
			jc: jsonutil.JSONContainer{
				"code":    &cstr,
				"system":  &sstr,
				"version": &estr,
				"display": &estr,
			},
		},
		{
			name: "full code",
			hc: HarmonizedCode{
				Code:    "target-code",
				System:  "target-system",
				Display: "Target Code",
				Version: "v1",
			},
			jc: jsonutil.JSONContainer{
				"code":    &cstr,
				"system":  &sstr,
				"version": &vstr,
				"display": &dstr,
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := test.hc.ToJSONContainer()
			if diff := cmp.Diff(got, test.jc); diff != "" {
				t.Errorf("ToJSONContainer(%v) => diff -%v +%v\n%s", test.hc, test.jc, got, diff)
			}

			gothc, err := FromJSONContainer(test.jc)
			if err != nil {
				t.Errorf("FromJSONContainer(%v) resulted in an error %v", test.jc, err)
			}
			if diff := cmp.Diff(gothc, test.hc); diff != "" {
				t.Errorf("FromJSONContainer(%v) => diff -%v +%v\n%s", test.jc, test.hc, gothc, diff)
			}
		})
	}
}
