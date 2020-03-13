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
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language/parser" /* copybara-comment: parser */

	mpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func (t *transpiler) VisitMapping(ctx *parser.MappingContext) interface{} {
	// Mapping rule has 3 components: target, condition, source. Parse each with their rules and
	// combine into a FieldMapping.
	target := ctx.Target().Accept(t).(*mpb.FieldMapping).Target

	// If there is an existing condition stack, we first have to combine them with _And, then add
	// the inline condition from this mapping if it exists.
	var condition *mpb.ValueSource
	if len(t.conditionStack) > 0 {
		if ctx.InlineCondition() != nil {
			condition = t.conditionStack.and(ctx.InlineCondition().Accept(t).(*mpb.ValueSource))
		} else {
			condition = t.conditionStack.and()
		}
	} else if ctx.InlineCondition() != nil {
		condition = ctx.InlineCondition().Accept(t).(*mpb.ValueSource)
	}

	source := ctx.Expression().Accept(t).(*mpb.ValueSource)

	f := &mpb.FieldMapping{
		Target:      target,
		Condition:   condition,
		ValueSource: source,
	}

	// Register the mapping in the environment if applicable.
	if t.environment != nil {
		t.environment.addMapping(f)
	}

	return f
}
