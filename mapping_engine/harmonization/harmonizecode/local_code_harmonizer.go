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
	"fmt"
)

const (
	unmappedModeFixed    = "fixed"
	unmappedModeProvided = "provided"
)

// cachedMap stores FHIR concept map data.
type cachedMap struct {
	version string
	groups  []cachedGroup
}

type cachedGroup struct {
	sourceSystem string
	targetSystem string
	lookups      map[string][]ConceptElementTarget
	unmapped     *ConceptUnmapped
}

// LocalCodeHarmonizer will harmonize codes using files stored locally.
type LocalCodeHarmonizer struct {
	// cachedMaps are cachedMaps (FHIR concept map data) cached by resource IDs.
	cachedMaps map[string]cachedMap
}

// NewLocalCodeHarmonizer instantiates a new LocalCodeHarmonizer.
func NewLocalCodeHarmonizer() *LocalCodeHarmonizer {
	return &LocalCodeHarmonizer{cachedMaps: make(map[string]cachedMap)}
}

// HarmonizeBySearch implements CodeHarmonizer's HarmonizeBySearch function.
func (h *LocalCodeHarmonizer) HarmonizeBySearch(sourceCode, sourceSystem, sourceValueset, targetValueset, version string) ([]HarmonizedCode, error) {
	return nil, fmt.Errorf("HarmonizeBySearch is not supported in local harmonizer")
}

func groupMatch(sourceSystem, targetSystem string, group cachedGroup) bool {
	// If group.sourceSystem or group.targetSystem is empty, match it to all codes.
	// For backward compatibility, if targetSystem is not provided, match it to all groups.
	return (group.sourceSystem == "" || group.sourceSystem == sourceSystem) &&
		(group.targetSystem == "" || targetSystem == group.targetSystem || targetSystem == "")
}

// HarmonizeWithTarget implements CodeHarmonizer's HarmonizeWithTarget function.
func (h *LocalCodeHarmonizer) HarmonizeWithTarget(sourceCode, sourceSystem, targetSystem, sourceName string) ([]HarmonizedCode, error) {
	conceptMap, ok := h.cachedMaps[sourceName]
	if !ok {
		return nil, fmt.Errorf("the harmonization source %q does not exist", sourceName)
	}
	mapGroups := conceptMap.groups

	if len(mapGroups) == 0 {
		return nil, fmt.Errorf("concept map %q must have at least one group", sourceName)
	}

	var output []HarmonizedCode
	for _, group := range mapGroups {
		if !groupMatch(sourceSystem, targetSystem, group) {
			continue
		}
		targets, ok := group.lookups[sourceCode]
		if !ok {
			if group.unmapped == nil {
				continue
			}
			switch mode := group.unmapped.Mode; mode {
			case unmappedModeFixed:
				output = append(output, HarmonizedCode{
					Version: conceptMap.version,
					System:  group.targetSystem,
					Code:    group.unmapped.Code,
					Display: group.unmapped.Display,
				})
			case unmappedModeProvided:
				output = append(output, HarmonizedCode{
					Version: conceptMap.version,
					System:  group.targetSystem,
					Code:    sourceCode,
					Display: sourceCode,
				})
			}
			continue
		}
		for _, target := range targets {
			output = append(output, HarmonizedCode{
				Version: conceptMap.version,
				System:  group.targetSystem,
				Code:    target.Code,
				Display: target.Display,
			})
		}
	}

	if len(output) == 0 {
		output = append(output, HarmonizedCode{
			Code:    sourceCode,
			System:  fmt.Sprintf("%s-%s", sourceName, "unharmonized"),
			Version: conceptMap.version,
		})
	}
	return output, nil
}

// Harmonize implements CodeHarmonizer's Harmonize function.
func (h *LocalCodeHarmonizer) Harmonize(sourceCode, sourceSystem, sourceName string) ([]HarmonizedCode, error) {
	return h.HarmonizeWithTarget(sourceCode, sourceSystem, "", sourceName)
}

// Cache takes a conceptMap and caches it internally for lookups.
func (h *LocalCodeHarmonizer) Cache(cm *ConceptMap) error {
	cachedMap, id, err := buildCachedMap(cm)
	if err != nil {
		return err
	}

	h.cachedMaps[id] = cachedMap
	return nil
}

// TODO: Validate duplicate mappings.
// TODO: Validation should be decoupled from caching.
func buildCachedMap(cm *ConceptMap) (cachedMap, string, error) {
	if cm.ID == "" {
		return cachedMap{}, "", fmt.Errorf("concept map must have an id field")
	}

	cache := cachedMap{
		version: cm.Version,
		groups:  make([]cachedGroup, 0, len(cm.Group)),
	}

	if len(cm.Group) == 0 {
		return cachedMap{}, "", fmt.Errorf("concept map must have at least one group")
	}

	for _, group := range cm.Group {
		lookup := make(map[string][]ConceptElementTarget)
		for _, element := range group.Element {
			lookup[element.Code] = element.Target
		}

		if unmapped := group.Unmapped; unmapped != nil &&
			unmapped.Mode != unmappedModeFixed &&
			unmapped.Mode != unmappedModeProvided {
			return cachedMap{}, "", fmt.Errorf("only fixed and provided modes are supported in concept map %q", cm.ID)
		}
		cachedGroup := cachedGroup{
			lookups:      lookup,
			targetSystem: group.Target,
			sourceSystem: group.Source,
			unmapped:     group.Unmapped,
		}
		cache.groups = append(cache.groups, cachedGroup)
	}

	return cache, cm.ID, nil
}
