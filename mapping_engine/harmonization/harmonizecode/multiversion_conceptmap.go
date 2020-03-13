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

// ConceptMap (and the nested structs) represents a slimmed-down, multiversion
// representation of a FHIR ConceptMap.
type ConceptMap struct {
	ID, Version, ResourceType string
	Group                     []ConceptGroup
}

// ConceptGroup (and the nested structs) represents a slimmed-down, multiversion
// representation of a FHIR Group within a ConceptMap resource.
type ConceptGroup struct {
	Element  []ConceptElement
	Unmapped *ConceptUnmapped
	Target   string
}

// ConceptElement (and the nested structs) represents a slimmed-down,
// multiversion representation of a FHIR Element within a ConceptMap > Group.
type ConceptElement struct {
	Code   string
	Target []ConceptElementTarget
}

// ConceptElementTarget represents a slimmed-down, multiversion representation
// of a FHIR Target within a ConceptMap > Group > Element.
type ConceptElementTarget struct {
	Code, Display string
}

// ConceptUnmapped represents a slimmed-down, multiversion representation of a
// FHIR Unmapped within a ConceptMap > Group.
type ConceptUnmapped struct {
	Code, Display, Mode string
}

// unmarshalR3ConceptMap takes a serialized JSON representation of a R3 FHIR
// ConceptMap resource, unmarshals it into a ConceptMap struct, and performs
// some FHIR validation.
func unmarshalR3ConceptMap(raw []byte) (*ConceptMap, error) {
	cm := &ConceptMap{}

	err := json.Unmarshal(raw, cm)
	if err != nil {
		return nil, fmt.Errorf("failed to unmarshal json ConceptMap: %v", err)
	}

	if cm.ResourceType != "ConceptMap" {
		return nil, fmt.Errorf("expected resourceType of ConceptMap, got: %s", cm.ResourceType)
	}

	for i, group := range cm.Group {
		errSuffix := fmt.Sprintf(" Target: %s", group.Target)
		if group.Target == "" {
			errSuffix = fmt.Sprintf("[%d]", i)
		}

		if len(group.Element) == 0 {
			return nil, fmt.Errorf("at least one Element required in Group%s", errSuffix)
		}

		if group.Unmapped != nil && group.Unmapped.Mode == "" {
			return nil, fmt.Errorf("Unmapped > Mode field is required in Group%s", errSuffix)
		}
	}

	return cm, nil
}
