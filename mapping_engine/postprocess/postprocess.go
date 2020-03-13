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

// Package postprocess handles post processing during the mapping process.
package postprocess

import (
	"fmt"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	bundleResourceType = "Bundle"
	transactionType    = "transaction"
)

// Process handles post processing logic for the mapping library.
func Process(pctx *types.Context, config *mappb.MappingConfig, skipBundling bool, parallel bool) (jsonutil.JSONToken, error) {
	var result jsonutil.JSONToken

	var p types.Projector
	switch proj := config.PostProcess.(type) {
	case *mappb.MappingConfig_PostProcessProjectorDefinition:
		p = projector.FromDef(*proj.PostProcessProjectorDefinition, parallel)
	case *mappb.MappingConfig_PostProcessProjectorName:
		fp, err := pctx.Registry.FindProjector(proj.PostProcessProjectorName)
		if err != nil {
			return nil, fmt.Errorf("post_process projector %v not found", proj.PostProcessProjectorName)
		}
		p = fp
	}

	result = pctx.Output
	if len(pctx.TopLevelObjects) > 0 {
		if err := jsonutil.Merge(convertTopLevelObjectsToContainer(pctx), &result, true, false); err != nil {
			return nil, fmt.Errorf("attempt to merge root mappings with target_object (Output Key) mappings failed: %v. target_object is deprecated, consider using target_root_field", err)
		}
	}

	if p != nil && !skipBundling {
		pctx.Trace.StartBundler(result)
		jmn, err := jsonutil.TokenToNode(result)
		if err != nil {
			return nil, err
		}
		res, err := p([]jsonutil.JSONMetaNode{jmn}, pctx)
		if err != nil {
			return nil, err
		}
		result = res
		pctx.Trace.EndBundler(result)
	}

	return result, nil
}

func convertTopLevelObjectsToContainer(ctx *types.Context) jsonutil.JSONContainer {
	cont := make(jsonutil.JSONContainer)

	for name, arr := range ctx.TopLevelObjects {
		jarr := jsonutil.JSONToken(jsonutil.JSONArr(arr))
		cont[name] = &jarr
	}

	return cont
}
