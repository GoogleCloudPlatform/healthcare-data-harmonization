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

func TestUnmarshalR3Parameters(t *testing.T) {
	tests := []struct {
		name string
		in   json.RawMessage
		want *Parameters
	}{
		{
			name: "empty parameter list",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": []
			}`),
			want: &Parameters{
				ResourceType: "Parameters",
				Parameter:    []ParamParameter{},
			},
		},
		{
			name: "parameter with coding value",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
				  {
					  "name": "abc",
						"valueCoding": {
							"code": "c",
							"system": "s",
							"version": "v",
							"display": "d"
						}
					}
				]
			}`),
			want: &Parameters{
				ResourceType: "Parameters",
				Parameter: []ParamParameter{
					ParamParameter{
						Name: "abc",
						ValueCoding: ParamValueCoding{
							Code:    "c",
							System:  "s",
							Version: "v",
							Display: "d",
						},
					},
				},
			},
		},
		{
			name: "parameter with empty coding value",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
				  {
					  "name": "abc",
						"valueCoding": {}
					}
				]
			}`),
			want: &Parameters{
				ResourceType: "Parameters",
				Parameter: []ParamParameter{
					ParamParameter{
						Name: "abc",
					},
				},
			},
		},
		{
			// http://hl7.org/fhir/STU3/parameters-example.json.html
			name: "FHIR spec R3 example",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"id": "example",
				"parameter": [
					{
						"name": "start",
						"valueDate": "2010-01-01"
					},
					{
						"name": "end",
						"resource": {
							"resourceType": "Binary",
							"contentType": "text/plain",
							"content": "VGhpcyBpcyBhIHRlc3QgZXhhbXBsZQ=="
						}
					}
				]
			}`),
			want: &Parameters{
				ResourceType: "Parameters",
				Parameter: []ParamParameter{
					ParamParameter{
						Name: "start",
					},
					ParamParameter{
						Name: "end",
					},
				},
			},
		},
		{
			// http://hl7.org/fhir/parameters-example.json.html
			name: "FHIR spec R4 example",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
					{
						"name": "exact",
						"valueBoolean": true
					},
					{
						"name": "property",
						"part": [
							{
								"name": "code",
								"valueCode": "focus"
							},
							{
								"name": "value",
								"valueCode": "top"
							}
						]
					},
					{
						"name": "patient",
						"resource": {
							"resourceType": "Patient",
							"id": "example",
							"name": [
								{
									"use": "official",
									"family": "Chalmers",
									"given": [
										"Peter",
										"James"
									]
								}
							]
						}
					}
				]
			}`),
			want: &Parameters{
				ResourceType: "Parameters",
				Parameter: []ParamParameter{
					ParamParameter{
						Name: "exact",
					},
					ParamParameter{
						Name: "property",
						Part: []ParamParameter{
							ParamParameter{
								Name: "code",
							},
							ParamParameter{
								Name: "value",
							},
						},
					},
					ParamParameter{
						Name: "patient",
					},
				},
			},
		},
	}

	opts := cmp.AllowUnexported(Parameters{}, ParamParameter{}, ParamValueCoding{})

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := unmarshalR3Parameters(test.in)
			if err != nil {
				t.Errorf("unmarshalR3Parameters(_) returned unexpected error: %v", err)
			}

			if diff := cmp.Diff(test.want, got, opts); diff != "" {
				t.Errorf("unmarshalR3Parameters(_) => diff -%v +%v\n%s", test.want, got, diff)
			}
		})
	}
}

func TestUnmarshalR3Parameters_Errors(t *testing.T) {
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
			name: "two-level nested parameters",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
				  {
						"name": "abc",
						"part": [
							{
								"name": "def",
								"part": [
								  {
									  "name": "xyz"
									}
								]
							}
						]
					}
				]
			}`),
		},
		{
			name: "missing name field",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
				  {
						"valueCoding": {
							"code": "cc"
						}
					}
				]
			}`),
		},
		{
			name: "missing nested name field",
			in: json.RawMessage(`{
				"resourceType": "Parameters",
				"parameter": [
				  {
						"name": "abc",
						"part": [
							{
								"valueCoding": {
									"code": "cc"
								}
							}
						]
					}
				]
			}`),
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			_, err := unmarshalR3Parameters(test.in)
			if err == nil {
				t.Errorf("unmarshalR3Parameters(_) returned unexpected nil error")
			}
		})
	}
}
