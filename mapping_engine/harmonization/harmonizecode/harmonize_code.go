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

// Package harmonizecode handles harmonization of codes.
package harmonizecode

import (
	"context"
	"fmt"
	"io/ioutil"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/gcsutil" /* copybara-comment: gcsutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

const (

	projectorName       = "$HarmonizeCode"
	withTargetProjector = "$HarmonizeCodeWithTarget"
	searchProjector     = "$HarmonizeCodeBySearch"
	localHarmonizerName = "$Local"
)

// CodeHarmonizer is the interface for harmonizing codes.
type CodeHarmonizer interface {
	// sourceCode is the code being translated.
	// sourceSystem is the system that this code belongs to.
	// sourceName is the name of the concept map to use.
	Harmonize(sourceCode, sourceSystem, sourceName string) ([]HarmonizedCode, error)
	// sourceCode is the code being translated.
	// sourceSystem is the system that this code belongs to.
	// targetSystem is the system in which a translated code is sought.
	// sourceName is the name of the concept map to use.
	HarmonizeWithTarget(sourceCode, sourceSystem, targetSystem, sourceName string) ([]HarmonizedCode, error)
	// sourceCode is the code being translated.
	// sourceSystem is the system that this code belongs to.
	// sourceValueset is the name of the source value set to search by.
	// targetValueset is the name of the target value set to search by.
	// version is the version of the concept map to use.
	HarmonizeBySearch(sourceCode, sourceSystem, sourceValueset, targetValueset, version string) ([]HarmonizedCode, error)
}

// HarmonizedCode is the result of harmonization.
// TODO(b/149298236): Add original code and equivalence here.
type HarmonizedCode struct {
	Code    string
	System  string
	Display string
	Version string
}

// ToJSONContainer converts the HarmonizedCode to a JSONContainer.
func (h HarmonizedCode) ToJSONContainer() jsonutil.JSONContainer {
	jc := make(jsonutil.JSONContainer)
	c := jsonutil.JSONToken(jsonutil.JSONStr(h.Code))
	jc["code"] = &c

	v := jsonutil.JSONToken(jsonutil.JSONStr(h.Version))
	jc["version"] = &v

	d := jsonutil.JSONToken(jsonutil.JSONStr(h.Display))
	jc["display"] = &d

	s := jsonutil.JSONToken(jsonutil.JSONStr(h.System))
	jc["system"] = &s

	return jc
}

// FromJSONContainer converts a JSONContainer to a HarmonizedCode.
func FromJSONContainer(jc jsonutil.JSONContainer) (HarmonizedCode, error) {
	result := HarmonizedCode{}
	if c, ok := jc["code"]; ok {
		if s, ok := (*c).(jsonutil.JSONStr); ok {
			result.Code = string(s)
		} else {
			return result, fmt.Errorf("code field is invalid")
		}
	}
	if v, ok := jc["version"]; ok {
		if s, ok := (*v).(jsonutil.JSONStr); ok {
			result.Version = string(s)
		} else {
			return result, fmt.Errorf("version field is invalid")
		}
	}

	if s, ok := jc["system"]; ok {
		if system, ok := (*s).(jsonutil.JSONStr); ok {
			result.System = string(system)
		} else {
			return result, fmt.Errorf("system field is invalid")
		}
	}

	if d, ok := jc["display"]; ok {
		if s, ok := (*d).(jsonutil.JSONStr); ok {
			result.Display = string(s)
		} else {
			return result, fmt.Errorf("display field is invalid")
		}
	}

	return result, nil
}

// LoadCodeHarmonizationProjectors loads all harmonization projectors.
func LoadCodeHarmonizationProjectors(r *types.Registry, hc *hpb.CodeHarmonizationConfig) error {
	if hc == nil {
		return nil
	}

	harmonizers, err := makeCodeHarmonizers(hc)
	if err != nil {
		return err
	}

	proj, err := buildHarmonizeCodeProjector(harmonizers, projectorName)
	if err != nil {
		return err
	}

	if err = r.RegisterProjector(projectorName, proj); err != nil {
		return fmt.Errorf("error registering projector %q: %v", projectorName, err)
	}

	sproj, err := buildHarmonizeBySearchProjector(harmonizers, searchProjector)
	if err != nil {
		return err
	}

	if err = r.RegisterProjector(searchProjector, sproj); err != nil {
		return fmt.Errorf("error registering projector %q: %v", searchProjector, err)
	}

	tproj, err := buildHarmonizeWithTargetProjector(harmonizers, withTargetProjector)
	if err != nil {
		return err
	}

	if err = r.RegisterProjector(withTargetProjector, tproj); err != nil {
		return fmt.Errorf("error registering projector %q: %v", withTargetProjector, err)
	}

	return nil
}

func makeCodeHarmonizers(lookups *hpb.CodeHarmonizationConfig) (map[string]CodeHarmonizer, error) {
	harmonizers := make(map[string]CodeHarmonizer)

	local := NewLocalCodeHarmonizer()

	for _, l := range lookups.CodeLookup {
		var raw []byte
		var err error
		switch t := l.Location.(type) {
		case *httppb.Location_LocalPath:
			if !strings.HasSuffix(t.LocalPath, ".json") {
				continue
			}
			raw, err = ioutil.ReadFile(t.LocalPath)
			if err != nil {
				return nil, fmt.Errorf("failed to read concept map file with error %v", err)
			}
		case *httppb.Location_GcsLocation:
			raw, err = gcsutil.ReadFromGcs(context.Background(), t.GcsLocation)
			if err != nil {
				return nil, fmt.Errorf("failed to read from GCS, %v", err)
			}
		case *httppb.Location_UrlPath:
			h, err := makeRemoteCodeHarmonizer(t.UrlPath, int(lookups.CacheTtlSeconds), int(lookups.CleanupIntervalSeconds))
			if err != nil {
				return nil, fmt.Errorf("unable to create remote code harmonizer from url %s: %v", t.UrlPath, err)
			}
			harmonizers[l.GetName()] = h
			continue
		default:
			return nil, fmt.Errorf("location type %T is not supported", t)
		}

		// TODO(b/132161794): Add support for multiple FHIR versions.
		cm, err := unmarshalR3ConceptMap(raw)
		if err != nil {
			return nil, fmt.Errorf("unmarshal failed with error %v", err)
		}
		if err := local.Cache(cm); err != nil {
			return nil, err
		}
	}

	harmonizers[localHarmonizerName] = local
	return harmonizers, nil
}

func codesToJSONArray(hcs []HarmonizedCode) jsonutil.JSONArr {
	results := make(jsonutil.JSONArr, 0, len(hcs))
	for _, v := range hcs {
		results = append(results, v.ToJSONContainer())
	}
	return results
}

func buildHarmonizeWithTargetProjector(harmonizers map[string]CodeHarmonizer, name string) (types.Projector, error) {
	f := func(sourceType, sourceCode, sourceSystem, targetSystem, sourceName jsonutil.JSONStr) (jsonutil.JSONToken, error) {
		st := string(sourceType)
		if st == "" {
			return nil, fmt.Errorf("the harmonization source type cannot be empty")
		}
		harmonizer, ok := harmonizers[st]
		if !ok {
			return nil, fmt.Errorf("the harmonization source %s does not exist", st)
		}

		harmonizedCodes, err := harmonizer.HarmonizeWithTarget(string(sourceCode), string(sourceSystem), string(targetSystem), string(sourceName))
		if err != nil {
			return nil, err
		}

		return codesToJSONArray(harmonizedCodes), nil
	}

	return projector.FromFunction(f, name)
}

func buildHarmonizeBySearchProjector(harmonizers map[string]CodeHarmonizer, name string) (types.Projector, error) {
	f := func(sourceType, sourceCode, sourceSystem, sourceValueset, targetValueset, version jsonutil.JSONStr) (jsonutil.JSONToken, error) {
		st := string(sourceType)
		if st == "" {
			return nil, fmt.Errorf("the harmonization source type cannot be empty")
		}
		harmonizer, ok := harmonizers[st]
		if !ok {
			return nil, fmt.Errorf("the harmonization source %s does not exist", st)
		}

		harmonizedCodes, err := harmonizer.HarmonizeBySearch(string(sourceCode), string(sourceSystem), string(sourceValueset), string(targetValueset), string(version))
		if err != nil {
			return nil, err
		}

		return codesToJSONArray(harmonizedCodes), nil
	}

	return projector.FromFunction(f, name)
}

func buildHarmonizeCodeProjector(harmonizers map[string]CodeHarmonizer, name string) (types.Projector, error) {
	f := func(sourceType, sourceCode, sourceSystem, sourceName jsonutil.JSONStr) (jsonutil.JSONToken, error) {
		st := string(sourceType)
		if st == "" {
			return nil, fmt.Errorf("the harmonization source type cannot be empty")
		}
		harmonizer, ok := harmonizers[st]
		if !ok {
			return nil, fmt.Errorf("the harmonization source %s does not exist", st)
		}

		harmonizedCodes, err := harmonizer.Harmonize(string(sourceCode), string(sourceSystem), string(sourceName))
		if err != nil {
			return nil, err
		}

		return codesToJSONArray(harmonizedCodes), nil
	}

	return projector.FromFunction(f, name)
}
