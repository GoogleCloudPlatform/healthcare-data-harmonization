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
	"fmt"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	anonymousBlockNameFormat = "$anonblock_%d_%d"
)

// Consts for builtins.
var (
	preOpMapping = map[string]string{
		"~": "$Not",
	}
	biOpMapping = map[string]string{
		"-":   "$Sub",
		"+":   "$Sum",
		"*":   "$Mul",
		"/":   "$Div",
		"=":   "$Eq",
		"~=":  "$NEq",
		">":   "$Gt",
		">=":  "$GtEq",
		"<":   "$Lt",
		"<=":  "$LtEq",
		"or":  "$Or",
		"and": "$And",
	}
	postOpMapping = map[string]string{
		"?": "$IsNotNil",
	}
)

func (t *transpiler) VisitExprPreOp(ctx *parser.ExprPreOpContext) interface{} {
	// Map the prefix operator.
	proj, ok := preOpMapping[ctx.Preunoperator().GetText()]
	if !ok {
		t.fail(ctx, fmt.Errorf("unknown pre-operator %v", ctx.Preunoperator().GetText()))
	}

	// Transpile the operand.
	source := ctx.Expression().Accept(t).(*mpb.ValueSource)

	// Simplify if possible, and return the ValueSource.
	return projectAndSimplify(proj, source)
}

func (t *transpiler) VisitExprPostOp(ctx *parser.ExprPostOpContext) interface{} {
	// Map the postfix operator.
	proj, ok := postOpMapping[ctx.Postunoperator().GetText()]
	if !ok {
		t.fail(ctx, fmt.Errorf("unknown post-operator %v", ctx.Postunoperator().GetText()))
	}

	// Transpile the operand.
	source := ctx.Expression().Accept(t).(*mpb.ValueSource)

	// Simplify if possible, and return the ValueSource.
	return projectAndSimplify(proj, source)
}

func (t *transpiler) VisitExprBiOp(ctx *parser.ExprBiOpContext) interface{} {
	// Map the binary operator, trying each alternative in order of operator precedence.
	var op antlr.ParseTree = ctx.Bioperator1()

	if op == nil {
		op = ctx.Bioperator2()
	}

	if op == nil {
		op = ctx.Bioperator3()
	}

	if op == nil {
		op = ctx.Bioperator4()
	}

	proj, ok := biOpMapping[op.GetText()]
	if !ok {
		t.fail(ctx, fmt.Errorf("unknown binary operator %v", op.GetText()))
	}

	// Transpile the operands.
	lhs := ctx.Expression(0).Accept(t).(*mpb.ValueSource)
	rhs := ctx.Expression(1).Accept(t).(*mpb.ValueSource)

	// Simplify if possible, and return the ValueSource.
	return projectAndSimplify(proj, lhs, rhs)
}

func (t *transpiler) VisitExprProjection(ctx *parser.ExprProjectionContext) interface{} {
	arrMod := ""
	if ctx.ARRAYMOD() != nil {
		arrMod = ctx.ARRAYMOD().GetText()
	}

	vs := &mpb.ValueSource{
		Projector: ctx.TOKEN().GetText() + arrMod,
	}

	for i := range ctx.AllSourceContainer() {
		source := ctx.SourceContainer(i).Accept(t).(*mpb.ValueSource)

		if i == 0 {
			if source.Projector == "" {
				vs.Source = source.Source
			} else {
				vs.Source = &mpb.ValueSource_ProjectedValue{
					ProjectedValue: source,
				}
			}
			continue
		}

		vs.AdditionalArg = append(vs.AdditionalArg, source)
	}

	return vs
}

func (t *transpiler) VisitExprSource(ctx *parser.ExprSourceContext) interface{} {
	// No-op here, just visit the Source child.
	return ctx.Source().Accept(t)
}

// VisitExprAnonBlock handles anonymous block expressions by creating a new projector and calling it
// with no arguments.
func (t *transpiler) VisitExprAnonBlock(ctx *parser.ExprAnonBlockContext) interface{} {
	// Create a new environment/projector for the block with no args.
	anonBlockEnv := t.environment.newChild(fmt.Sprintf(anonymousBlockNameFormat, ctx.GetStart().GetLine(), ctx.GetStart().GetColumn()), []string{}, []string{})

	// Transpile the mappings for the block.
	t.pushEnv(anonBlockEnv)
	ctx.Block().Accept(t)

	// Add the projector to the mapping program.
	t.projectors = append(t.projectors, t.environment.generateProjector())
	t.popEnv()

	// Create a no-args call site for it.
	cs, err := anonBlockEnv.generateCallsite()
	if err != nil {
		t.fail(ctx, fmt.Errorf("unable to generate anonymous block callsite: %v", err))
	}
	return cs
}

func (t *transpiler) VisitSourceContainer(ctx *parser.SourceContainerContext) interface{} {
	// No-op here, just visit the appropriate child.
	if ctx.Source() != nil {
		return ctx.Source().Accept(t)
	}
	return ctx.Expression().Accept(t)
}

func (t *transpiler) VisitExprNoArg(ctx *parser.ExprNoArgContext) interface{} {
	// Simple projector call with no args, e.x. $UUID()
	return &mpb.ValueSource{
		Projector: ctx.TOKEN().GetText(),
	}
}
