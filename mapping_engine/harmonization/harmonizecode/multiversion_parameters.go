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
	"fmt"
)

// Parameters (and the nested structs) represents a slimmed-down, multiversion
// representation of a FHIR Parameters resource.
type Parameters struct {
	ResourceType string
	Parameter    []ParamParameter
}

// ParamParameter (and the nested structs) represents a slimmed-down,
// multiversion representation of a FHIR Parameter resource within a
// Parameters resource.
type ParamParameter struct {
	Name        string
	Part        []ParamParameter
	ValueCoding ParamValueCoding
}

// ParamValueCoding represents a multiversion representation of a FHIR Coding
// resource when used as the type for the Value field of a Parameter resource.
type ParamValueCoding struct {
	Code, System, Version, Display string
}

// unmarshalR3Parameters takes a serialized JSON representation of a R3 FHIR
// Parameters resource, unmarshals it into a Parameters struct, and performs
// some FHIR validation.
func unmarshalR3Parameters(raw []byte) (*Parameters, error) {
	p := &Parameters{}

	err := json.Unmarshal(raw, p)
	if err != nil {
		return nil, fmt.Errorf("failed to unmarshal json Parameters: %v", err)
	}

	if p.ResourceType != "Parameters" {
		return nil, fmt.Errorf("expected resourceType of Parameters, got: %s", p.ResourceType)
	}

	for _, param := range p.Parameter {
		if param.Name == "" {
			return nil, fmt.Errorf("Parameter > Name field is required")
		}
		for _, part := range param.Part {
			if part.Name == "" {
				return nil, fmt.Errorf("Parameter > Name field is required")
			}
			if len(part.Part) > 0 {
				return nil, fmt.Errorf("only one level of nested parameters is allowed")
			}
		}
	}

	return p, nil
}
