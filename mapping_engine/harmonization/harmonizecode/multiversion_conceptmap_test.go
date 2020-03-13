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
	"encoding/json"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

func TestUnmarshalR3ConceptMap(t *testing.T) {
	tests := []struct {
		name string
		in   json.RawMessage
		want *ConceptMap
	}{
		{
			name: "empty groups",
			in: json.RawMessage(`{
				"group":[],
				"id": "foo",
				"version": "bar",
				"resourceType": "ConceptMap"
			}`),
			want: &ConceptMap{
				ResourceType: "ConceptMap",
				ID:           "foo",
				Version:      "bar",
				Group:        []ConceptGroup{},
			},
		},
		{
			name: "no groups",
			in: json.RawMessage(`{
				"id": "foo",
				"version": "bar",
				"resourceType": "ConceptMap"
			}`),
			want: &ConceptMap{
				ResourceType: "ConceptMap",
				ID:           "foo",
				Version:      "bar",
			},
		},
		{
			name: "unmapped mode - provided",
			in: json.RawMessage(`{
				"group":[
					{
						"element":[
							{
								"code": "abc",
								"target":[
									{
										"code": "def",
										"equivalence": "EQUIVALENT"
									}
								]
							}
						],
						"unmapped": {
							"mode": "provided"
						},
						"target": "xyz"
					}
				],
				"id": "foo",
				"version": "bar",
				"resourceType": "ConceptMap"
			}`),
			want: &ConceptMap{
				ResourceType: "ConceptMap",
				ID:           "foo",
				Version:      "bar",
				Group: []ConceptGroup{
					ConceptGroup{
						Element: []ConceptElement{
							ConceptElement{
								Code: "abc",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code: "def",
									},
								},
							},
						},
						Unmapped: &ConceptUnmapped{
							Mode: "provided",
						},
						Target: "xyz",
					},
				},
			},
		},
		{
			name: "multiple groups",
			in: json.RawMessage(`{
				"group":[
					{
						"element":[
							{
								"code": "abc",
								"target":[
									{
										"code": "def",
										"equivalence": "EQUIVALENT"
									}
								]
							}
						],
						"unmapped": {
							"mode": "fixed",
							"code": "unknown",
							"display": "Unknown Code"
						},
						"target": "xyz1"
					},
					{
						"element":[
							{
								"code": "abc",
								"target":[
									{
										"code": "def",
										"equivalence": "EQUIVALENT"
									}
								]
							}
						],
						"unmapped": {
							"mode": "fixed",
							"code": "unknown",
							"display": "Unknown Code"
						},
						"target": "xyz2"
					},
					{
						"element":[
							{
								"code": "source-code",
								"target":[
									{
										"code": "def",
										"equivalence": "EQUIVALENT"
									}
								]
							}
						],
						"unmapped": {
							"mode": "fixed",
							"code": "unknown",
							"display": "Unknown Code"
						},
						"target": "xyz3"
					}
				],
				"id": "foo",
				"version": "bar",
				"resourceType": "ConceptMap"
			}`),
			want: &ConceptMap{
				ResourceType: "ConceptMap",
				ID:           "foo",
				Version:      "bar",
				Group: []ConceptGroup{
					ConceptGroup{
						Element: []ConceptElement{
							ConceptElement{
								Code: "abc",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code: "def",
									},
								},
							},
						},
						Unmapped: &ConceptUnmapped{
							Code:    "unknown",
							Display: "Unknown Code",
							Mode:    "fixed",
						},
						Target: "xyz1",
					},
					ConceptGroup{
						Element: []ConceptElement{
							ConceptElement{
								Code: "abc",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code: "def",
									},
								},
							},
						},
						Unmapped: &ConceptUnmapped{
							Code:    "unknown",
							Display: "Unknown Code",
							Mode:    "fixed",
						},
						Target: "xyz2",
					},
					ConceptGroup{
						Element: []ConceptElement{
							ConceptElement{
								Code: "source-code",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code: "def",
									},
								},
							},
						},
						Unmapped: &ConceptUnmapped{
							Code:    "unknown",
							Display: "Unknown Code",
							Mode:    "fixed",
						},
						Target: "xyz3",
					},
				},
			},
		},
		{
			// https://hl7.org/fhir/STU3/ConceptMap-example.json.html
			name: "FHIR spec example",
			in: json.RawMessage(`{
				"resourceType": "ConceptMap",
				"id": "101",
				"text": {
					"status": "generated",
					"div": "<div xmlns=\"http://www.w3.org/1999/xhtml\">\n      <h2>FHIR-v3-Address-Use (http://hl7.org/fhir/ConceptMap/101)</h2>\n      <p>Mapping from \n        <a href=\"valueset-address-use.html\">http://hl7.org/fhir/ValueSet/address-use</a> to \n        <a href=\"v3/AddressUse/vs.html\">http://hl7.org/fhir/ValueSet/v3-AddressUse</a>\n      </p>\n      <p>DRAFT (not intended for production usage). Published on 13/06/2012 by HL7, Inc (FHIR project team (example): \n        <a href=\"http://hl7.org/fhir\">http://hl7.org/fhir</a>). Creative Commons 0\n      </p>\n      <div>\n        <p>A mapping between the FHIR and HL7 v3 AddressUse Code systems</p>\n\n      </div>\n      <br/>\n      <table class=\"grid\">\n        <tr>\n          <td>\n            <b>Source Code</b>\n          </td>\n          <td>\n            <b>Equivalence</b>\n          </td>\n          <td>\n            <b>Destination Code</b>\n          </td>\n          <td>\n            <b>Comment</b>\n          </td>\n        </tr>\n        <tr>\n          <td>home (Home)</td>\n          <td/>\n          <td>H (home address)</td>\n          <td/>\n        </tr>\n        <tr>\n          <td>work (Work)</td>\n          <td/>\n          <td>WP (work place)</td>\n          <td/>\n        </tr>\n        <tr>\n          <td>temp (Temporary)</td>\n          <td/>\n          <td>TMP (temporary address)</td>\n          <td/>\n        </tr>\n        <tr>\n          <td>old (Old / Incorrect)</td>\n          <td>disjoint</td>\n          <td>BAD (bad address)</td>\n          <td>In the HL7 v3 AD, old is handled by the usablePeriod element, but you have to provide a time, there's no simple equivalent of flagging an address as old</td>\n        </tr>\n      </table>\n    </div>"
				},
				"url": "http://hl7.org/fhir/ConceptMap/101",
				"identifier": {
					"system": "urn:ietf:rfc:3986",
					"value": "urn:uuID:53cd62ee-033e-414c-9f58-3ca97b5ffc3b"
				},
				"version": "20120613",
				"name": "FHIR-v3-Address-Use",
				"title": "FHIR/v3 Address Use Mapping",
				"status": "draft",
				"experimental": true,
				"date": "2012-06-13",
				"publisher": "HL7, Inc",
				"contact": [
					{
						"name": "FHIR project team (example)",
						"telecom": [
							{
								"system": "url",
								"value": "http://hl7.org/fhir"
							}
						]
					}
				],
				"description": "A mapping between the FHIR and HL7 v3 AddressUse Code systems",
				"useContext": [
					{
						"code": {
							"system": "http://hl7.org/fhir/usage-context-type",
							"code": "venue"
						},
						"valueCodeableConcept": {
							"text": "for CDA Usage"
						}
					}
				],
				"purpose": "To help implementers map from HL7 v3/CDA to FHIR",
				"copyright": "Creative Commons 0",
				"sourceUri": "http://hl7.org/fhir/ValueSet/address-use",
				"targetUri": "http://hl7.org/fhir/ValueSet/v3-AddressUse",
				"group": [
					{
						"source": "http://hl7.org/fhir/address-use",
						"target": "http://hl7.org/fhir/v3/AddressUse",
						"element": [
							{
								"code": "home",
								"display": "home",
								"target": [
									{
										"code": "H",
										"display": "home"
									}
								]
							},
							{
								"code": "work",
								"display": "work",
								"target": [
									{
										"code": "WP",
										"display": "work place"
									}
								]
							},
							{
								"code": "temp",
								"display": "temp",
								"target": [
									{
										"code": "TMP",
										"display": "temporary address"
									}
								]
							},
							{
								"code": "old",
								"display": "old",
								"target": [
									{
										"code": "BAD",
										"display": "bad address",
										"equivalence": "disjoint",
										"comment": "In the HL7 v3 AD, old is handled by the usablePeriod element, but you have to provide a time, there's no simple equivalent of flagging an address as old"
									}
								]
							}
						],
						"unmapped": {
							"mode": "fixed",
							"code": "temp",
							"display": "temp"
						}
					}
				]
			}`),
			want: &ConceptMap{
				ResourceType: "ConceptMap",
				ID:           "101",
				Version:      "20120613",
				Group: []ConceptGroup{
					ConceptGroup{
						Element: []ConceptElement{
							ConceptElement{
								Code: "home",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code:    "H",
										Display: "home",
									},
								},
							},
							ConceptElement{
								Code: "work",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code:    "WP",
										Display: "work place",
									},
								},
							},
							ConceptElement{
								Code: "temp",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code:    "TMP",
										Display: "temporary address",
									},
								},
							},
							ConceptElement{
								Code: "old",
								Target: []ConceptElementTarget{
									ConceptElementTarget{
										Code:    "BAD",
										Display: "bad address",
									},
								},
							},
						},
						Unmapped: &ConceptUnmapped{
							Code:    "temp",
							Display: "temp",
							Mode:    "fixed",
						},
						Target: "http://hl7.org/fhir/v3/AddressUse",
					},
				},
			},
		},
	}

	opts := cmp.AllowUnexported(ConceptMap{}, ConceptGroup{}, ConceptElement{}, ConceptElementTarget{}, ConceptUnmapped{})

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			cm, err := unmarshalR3ConceptMap(test.in)
			if err != nil {
				t.Errorf("unmarshalR3ConceptMap(_) returned unexpected error: %v", err)
			}

			if diff := cmp.Diff(test.want, cm, opts); diff != "" {
				t.Errorf("unmarshalR3ConceptMap(_) => diff -%v +%v\n%s", test.want, cm, diff)
			}
		})
	}
}

func TestUnmarshalR3ConceptMap_Errors(t *testing.T) {
	tests := []struct {
		name string
		in   json.RawMessage
	}{
		{
			name: "empty",
			in:   json.RawMessage(``),
		},
		{
			name: "no resource type",
			in:   json.RawMessage(`{}`),
		},
		{
			name: "wrong resource type",
			in: json.RawMessage(`{
				"resourceType": "Patient"
			}`),
		},
		{
			name: "no element field in group",
			in: json.RawMessage(`{
				"group":[
					{
						"unmapped": {
							"mode": "fixed"
						},
						"target": "xyz"
					}
				],
				"id": "foo",
				"resourceType": "ConceptMap"
			}`),
		},
		{
			name: "no elements in group",
			in: json.RawMessage(`{
				"group":[
					{
					  "element": [],
						"unmapped": {
							"mode": "fixed"
						},
						"target": "xyz"
					}
				],
				"id": "foo",
				"resourceType": "ConceptMap"
			}`),
		},
		{
			name: "no mode in unmapped",
			in: json.RawMessage(`{
				"group":[
					{
						"element":[
							{
								"code": "abc",
								"target":[
									{
										"code": "def",
										"equivalence": "EQUIVALENT"
									}
								]
							}
						],
						"unmapped": {},
						"target": "xyz"
					}
				],
				"id": "foo",
				"resourceType": "ConceptMap"
			}`),
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := unmarshalR3ConceptMap(test.in)
			if err == nil {
				t.Errorf("unmarshalR3ConceptMap(_) returned unexpected nil error")
			}
		})
	}
}
