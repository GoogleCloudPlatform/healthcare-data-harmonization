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

// Package harmonizeunit harmonizes units.
package harmonizeunit

import (
	"context"
	"fmt"
	"io/ioutil"
	"math"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/auth" /* copybara-comment: auth */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/harmonization/harmonizecode" /* copybara-comment: harmonizecode */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/gcsutil" /* copybara-comment: gcsutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"google.golang.org/protobuf/encoding/prototext" /* copybara-comment: prototext */

	hpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: harmonization_go_proto */
	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
	ucpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: unit_config_go_proto */
	io "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/ioutil" /* copybara-comment: ioutil */
)

const (
	maxDecimalPlaces = 30
	projectorName    = "$HarmonizeUnit"

)

// UnitHarmonizer is the interface for harmonizing units.
type UnitHarmonizer interface {
	Harmonize(sourceQuantity float64, sourceUnit string, harmonizedCodes []harmonizecode.HarmonizedCode) (HarmonizedUnit, error)
}

// LocalUnitHarmonizer will harmonize units using files stored locally.
type LocalUnitHarmonizer struct {
	version    string
	system     string
	decimals   int32
	conversion map[codeSpecificUnit]*ucpb.UnitConversion
}

// RemoteUnitHarmonizer will harmonize units using a remote lookup service.
type RemoteUnitHarmonizer struct {
	client auth.Client
	// TODO: store cached results.
}

// HarmonizedUnit is the result of harmonization.
type HarmonizedUnit struct {
	Quantity         float64
	Unit             string
	System           string
	Version          string
	OriginalQuantity float64
	OriginalUnit     string
}

// codeSpecificUnit contains all the information needed to determine what the target unit should be.
type codeSpecificUnit struct {
	sourceUnit       string
	sourceCode       string
	sourceCodeSystem string
}

// ToJSONContainer converts the HarmonizedUnit to a JSONContainer.
func (h HarmonizedUnit) ToJSONContainer() jsonutil.JSONContainer {
	jc := make(jsonutil.JSONContainer)
	c := jsonutil.JSONToken(jsonutil.JSONNum(h.Quantity))
	jc["quantity"] = &c

	v := jsonutil.JSONToken(jsonutil.JSONStr(h.Unit))
	jc["unit"] = &v

	d := jsonutil.JSONToken(jsonutil.JSONStr(h.System))
	jc["system"] = &d

	s := jsonutil.JSONToken(jsonutil.JSONStr(h.Version))
	jc["version"] = &s

	oq := jsonutil.JSONToken(jsonutil.JSONNum(h.OriginalQuantity))
	jc["originalQuantity"] = &oq

	ou := jsonutil.JSONToken(jsonutil.JSONStr(h.OriginalUnit))
	jc["originalUnit"] = &ou

	return jc
}

// Harmonize implements UnitHarmonizer's Harmonize function.
func (h *LocalUnitHarmonizer) Harmonize(sourceQuantity float64, sourceUnit string, harmonizedCodes []harmonizecode.HarmonizedCode) (HarmonizedUnit, error) {
	harmonizedQuantity := sourceQuantity
	harmonizedUnit := strings.TrimSpace(sourceUnit)
	harmonizedSystem := "urn:unharmonized-unit"

	if len(harmonizedCodes) == 0 {
		harmonizedCodes = append(harmonizedCodes, harmonizecode.HarmonizedCode{})
	}

	// Handle code specific unit harmonizations.
	for _, v := range harmonizedCodes {
		sourceLookupKey := codeSpecificUnit{
			sourceUnit:       sourceUnit,
			sourceCode:       v.Code,
			sourceCodeSystem: v.System,
		}

		conversion, ok := h.conversion[sourceLookupKey]
		if ok {
			// unit needs to be converted, otherwise, unit is already harmonized.
			if conversion.ConstantFirst {
				harmonizedQuantity = (conversion.GetConstant() + sourceQuantity) * conversion.GetScalar()
			} else {
				harmonizedQuantity = conversion.GetScalar()*sourceQuantity + conversion.GetConstant()
			}

			harmonizedQuantity = roundFloat(harmonizedQuantity, h.decimals)
			harmonizedUnit = conversion.GetDestUnit()
			harmonizedSystem = h.system
			break
		}
	}

	return HarmonizedUnit{
		Unit:             harmonizedUnit,
		Quantity:         harmonizedQuantity,
		Version:          h.version,
		System:           harmonizedSystem,
		OriginalQuantity: sourceQuantity,
		OriginalUnit:     sourceUnit,
	}, nil
}

// Harmonize implements UnitHarmonizer's Harmonize function.
// TODO: Add support for this.
func (h *RemoteUnitHarmonizer) Harmonize(sourceQuantity float64, sourceUnit string, sourceCode string, sourceCodeSystem string) (HarmonizedUnit, error) {
	return HarmonizedUnit{}, fmt.Errorf("remote harmonization unimplemented")
}

// LoadUnitHarmonizationProjectors loads all unit harmonization projectors.
func LoadUnitHarmonizationProjectors(r *types.Registry, unitHarmonizationConfig *hpb.UnitHarmonizationConfig) error {
	if unitHarmonizationConfig.GetUnitConversion() == nil {
		return nil
	}
	uc, err := ParseUnitConfigFiles(unitHarmonizationConfig.GetUnitConversion())
	harmonizer, err := MakeLocalUnitHarmonizer(uc)
	proj, err := buildProjector(harmonizer, projectorName)
	if err != nil {
		return err
	}

	if err := r.RegisterProjector(projectorName, proj); err != nil {
		return err
	}

	return nil
}

// ParseUnitConfigFiles parses the unit config files.
func ParseUnitConfigFiles(unitConversion *httppb.Location) (*ucpb.UnitConfiguration, error) {
	uc := &ucpb.UnitConfiguration{}
	var raw []byte
	var err error
	switch t := unitConversion.Location.(type) {
	case *httppb.Location_LocalPath:
		if !io.Exists(t.LocalPath) {
			return nil, fmt.Errorf("unit conversion file %s does not exist", err)
		}
		raw, err = ioutil.ReadFile(t.LocalPath)
		if err != nil {
			return nil, fmt.Errorf("failed to read unit conversion file with error %v", err)
		}
	case *httppb.Location_GcsLocation:
		raw, err = gcsutil.ReadFromGcs(context.Background(), t.GcsLocation)
		if err != nil {
			return nil, fmt.Errorf("failed to read from GCS, %v", err)
		}
	default:
		return nil, fmt.Errorf("location type %T is not supported", t)
	}
	if err := prototext.Unmarshal(raw, uc); err != nil {
		return nil, fmt.Errorf("could not unmarshal unit conversion file: %v", err)
	}
	return uc, nil
}

// MakeLocalUnitHarmonizer creates a local unit harmonizer based on the provided config files.
func MakeLocalUnitHarmonizer(uc *ucpb.UnitConfiguration) (UnitHarmonizer, error) {
	conversionMap := make(map[codeSpecificUnit]*ucpb.UnitConversion)

	for _, c := range uc.GetConversion() {
		if c.GetDestUnit() == "" {
			return nil, fmt.Errorf("conversion destination unit cannot be empty")
		}

		if len(c.GetSourceUnit()) == 0 {
			return nil, fmt.Errorf("conversion source unit cannot be empty")
		}

		for _, su := range c.GetSourceUnit() {
			sourceUnit := codeSpecificUnit{
				sourceUnit:       su,
				sourceCode:       c.GetCode(),
				sourceCodeSystem: c.GetCodesystem(),
			}

			if _, exists := conversionMap[sourceUnit]; exists {
				return nil, fmt.Errorf("unit conversion for %v is defined twice", sourceUnit)
			}
			conversionMap[sourceUnit] = c
		}
	}

	return &LocalUnitHarmonizer{
		conversion: conversionMap,
		version:    uc.GetVersion(),
		system:     uc.GetSystem(),
		decimals:   uc.GetDecimals(),
	}, nil
}

func buildProjector(harmonizer UnitHarmonizer, name string) (types.Projector, error) {
	f := func(sourceQuantity jsonutil.JSONNum, sourceUnit jsonutil.JSONStr, rawCodes []jsonutil.JSONContainer) (jsonutil.JSONToken, error) {
		var codes []harmonizecode.HarmonizedCode
		for _, rawCode := range rawCodes {
			code, err := harmonizecode.FromJSONContainer(rawCode)
			if err != nil {
				return nil, err
			}
			codes = append(codes, code)
		}

		harmonizedUnit, err := harmonizer.Harmonize(float64(sourceQuantity), string(sourceUnit), codes)
		if err != nil {
			return nil, err
		}

		return harmonizedUnit.ToJSONContainer(), nil
	}

	return projector.FromFunction(f, name)
}

func roundFloat(value float64, decimals int32) float64 {
	d := decimals
	if decimals == 0 {
		d = maxDecimalPlaces
	}
	multiplier := math.Pow10(int(d))
	return math.Round(value*multiplier) / multiplier
}
