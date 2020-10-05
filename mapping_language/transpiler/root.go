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

package transpiler

import (
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	rootEnvInputName = "$root"

	// TODO(b/148939976): Revert after sunset.
	legacyRootEnvInputName = "root"
)

func (t *transpiler) VisitRoot(ctx *parser.RootContext) interface{} {
	program := &mpb.MappingConfig{}

	// Parse each root item with its corresponding rule and add them to the MappingConfig.

	if ctx.PostProcess() != nil {
		program = ctx.PostProcess().Accept(t).(*mpb.MappingConfig)
	}

	t.environment = newEnv("", []string{rootEnvInputName}, []string{})

	// TODO(b/148939976): Remove this env and the callsite after sunset.
	t.environment.args[legacyRootEnvInputName] = t.environment.args[rootEnvInputName]

	for i := range ctx.AllMapping() {
		ctx.Mapping(i).Accept(t)
	}

	for i := range ctx.AllProjectorDef() {
		p := ctx.ProjectorDef(i).Accept(t).(*mpb.ProjectorDefinition)
		program.Projector = append(program.Projector, p)
	}

	program.Projector = append(program.Projector, t.projectors...)
	program.RootMapping = t.environment.mapping

	return program
}

func (t *transpiler) VisitPostProcessName(ctx *parser.PostProcessNameContext) interface{} {
	return &mpb.MappingConfig{
		PostProcess: &mpb.MappingConfig_PostProcessProjectorName{
			PostProcessProjectorName: getTokenText(ctx.TOKEN()),
		},
	}
}

func (t *transpiler) VisitPostProcessInline(ctx *parser.PostProcessInlineContext) interface{} {
	return &mpb.MappingConfig{
		PostProcess: &mpb.MappingConfig_PostProcessProjectorDefinition{
			PostProcessProjectorDefinition: ctx.ProjectorDef().Accept(t).(*mpb.ProjectorDefinition),
		},
	}
}
