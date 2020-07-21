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

// Package transpiler contains an implementation of a Whistle Tree Visitor that produces a Whistler Program.
package transpiler

import (
	"fmt"
	"strconv"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	// foreachElementVarName is the name of the input that can be used to access the current array item
	// in an array filter.
	foreachElementInputName = "$"
)

func (t *transpiler) VisitSourceConstNum(ctx *parser.SourceConstNumContext) interface{} {
	// Parse the number and return const ValueSource.
	f, err := strconv.ParseFloat(ctx.GetText(), 32)
	if err != nil {
		t.fail(ctx, err)
	}

	return &mpb.ValueSource{
		Source: &mpb.ValueSource_ConstFloat{
			ConstFloat: float32(f),
		},
	}
}

func (t *transpiler) VisitSourceInput(ctx *parser.SourceInputContext) interface{} {
	// Parse the path.
	p := ctx.SourcePath().Accept(t).(pathSpec)

	if p.arg == "" && t.environment.name == "" {
		t.fail(ctx, fmt.Errorf("root mapping can't access source %q. It can only use vars or the input %q", p.index, rootEnvInputName))
	}

	if ctx.InlineFilter() != nil || ctx.ARRAYMOD() != nil {
		// Force a foreach.
		p.field = strings.TrimSuffix(p.field, "[]") + "[]"
	}

	var vs *mpb.ValueSource

	if (p.arg == "" && p.index != "") || ctx.DEST() != nil {
		// Index targets may not be known in the environment since they are not declared.
		vs = &mpb.ValueSource{
			Source: &mpb.ValueSource_FromDestination{
				FromDestination: p.index + p.arg + p.field,
			},
		}
	} else if vs = t.environment.readVar(p.arg+p.index, p.field); ctx.VAR() != nil && vs == nil {
		t.fail(ctx, fmt.Errorf("unable to find variable %q", p.arg))
	} else if vs = t.environment.readInput(p.arg+p.index, p.field); vs == nil {
		t.fail(ctx, fmt.Errorf("unable to find input %q", p.arg))
	}

	if ctx.InlineFilter() != nil {
		lambdaEnv := t.environment.newChild(fmt.Sprintf("$filter_%d_%d", ctx.GetStart().GetLine(), ctx.GetStart().GetColumn()), []string{foreachElementInputName}, []string{})
		t.pushEnv(lambdaEnv)
		t.environment.addMapping(&mpb.FieldMapping{
			Condition: ctx.InlineFilter().Accept(t).(*mpb.ValueSource),
			Target: &mpb.FieldMapping_TargetField{
				TargetField: ".",
			},
			ValueSource: t.environment.readInput("$", ""),
		})
		t.projectors = append(t.projectors, t.environment.generateProjector())
		t.popEnv()

		cs, err := lambdaEnv.generateCallsite(vs)
		if err != nil {
			t.fail(ctx, fmt.Errorf("unable to generate filter callsite: %v", err))
		}
		if ctx.ARRAYMOD() != nil {
			cs.Projector += ctx.ARRAYMOD().GetText()
		}

		return cs
	}

	return vs
}

func (t *transpiler) VisitSourceConstStr(ctx *parser.SourceConstStrContext) interface{} {
	// Strip quotes from string.
	text := ctx.STRING().GetText()[1 : len(ctx.STRING().GetText())-1]
	// Replace escaped quotes.
	text = strings.ReplaceAll(text, `\"`, `"`)
	// Replace escaped backslashes
	text = strings.ReplaceAll(text, `\\`, `\`)
	return &mpb.ValueSource{
		Source: &mpb.ValueSource_ConstString{
			ConstString: text,
		},
	}
}

func (t *transpiler) VisitSourceConstBool(ctx *parser.SourceConstBoolContext) interface{} {
	return &mpb.ValueSource{
		Source: &mpb.ValueSource_ConstBool{
			ConstBool: ctx.BOOL().GetText() == "true",
		},
	}
}

func (t *transpiler) VisitSourceProjection(ctx *parser.SourceProjectionContext) interface{} {
	vs := &mpb.ValueSource{
		Source: &mpb.ValueSource_ProjectedValue{
			ProjectedValue: ctx.Expression().Accept(t).(*mpb.ValueSource),
		},
	}

	if ctx.ARRAYMOD() != nil {
		vs.Projector += ctx.ARRAYMOD().GetText()
	}
	return vs
}
