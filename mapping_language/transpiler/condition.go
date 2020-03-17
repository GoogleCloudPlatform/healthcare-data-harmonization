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

func (t *transpiler) VisitConditionBlock(ctx *parser.ConditionBlockContext) interface{} {
	// Condition block is a composite of an If and optionally an else, with corresponding blocks.
	ctx.IfBlock().Accept(t)
	if ctx.ElseBlock() != nil {
		ctx.ElseBlock().Accept(t)
	}

	// IfBlock below pushes a condition (with its awareness of the condition subtree).
	t.conditionStack.pop()

	// Since this is a block of mappings that get added to the environment, there is nothing to
	// return.
	return nil
}

func (t *transpiler) VisitIfBlock(ctx *parser.IfBlockContext) interface{} {
	// Transpile and add the condition to the stack, and process the block as usual.
	t.conditionStack.push(ctx.Condition().Accept(t).(*mpb.ValueSource))
	ctx.Block().Accept(t)

	// Since this is a block of mappings that get added to the environment, there is nothing to
	// return.
	return nil
}

// not returns a new ValueSource, inverting the output of the given by passing it through _Not.
func not(vs *mpb.ValueSource) *mpb.ValueSource {
	return &mpb.ValueSource{
		Source: &mpb.ValueSource_ProjectedValue{
			ProjectedValue: vs,
		},
		Projector: "$Not",
	}
}

func (t *transpiler) VisitElseBlock(ctx *parser.ElseBlockContext) interface{} {
	// Else uses the condition in If, but inverted.
	t.conditionStack.push(not(t.conditionStack.pop()))
	ctx.Block().Accept(t)

	// Since this is a block of mappings that get added to the environment, there is nothing to
	// return.
	return nil
}

func (t *transpiler) VisitInlineCondition(ctx *parser.InlineConditionContext) interface{} {
	// No-op here, just visit the Condition child.
	return ctx.Condition().Accept(t)
}

func (t *transpiler) VisitCondition(ctx *parser.ConditionContext) interface{} {
	// No-op here, just visit the Expression child.
	return ctx.Expression().Accept(t)
}
