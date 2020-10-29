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
	"errors"
	"fmt"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const jsonThis = "."

func (t *transpiler) VisitTargetVar(ctx *parser.TargetVarContext) interface{} {
	p := ctx.TargetPath().Accept(t).(pathSpec)

	if p.arg == "" {
		t.fail(ctx, fmt.Errorf("expected a valid variable name (optionally followed by a path), but got %s", p.index+p.field))
	}

	if t.environment != nil {
		if err := t.environment.declareVar(p.arg); err != nil {
			t.fail(ctx, err)
		}
	}

	fm := &mpb.FieldMapping{
		Target: &mpb.FieldMapping_TargetLocalVar{
			TargetLocalVar: jsonutil.JoinPath(p.arg, p.field),
		},
	}

	if t.includeSourcePositions {
		fm.TargetMeta = makeSourcePositionMeta(ctx, fm.TargetMeta)
	}

	return fm
}

func (t *transpiler) VisitTargetObj(ctx *parser.TargetObjContext) interface{} {
	fm := &mpb.FieldMapping{
		Target: &mpb.FieldMapping_TargetObject{
			TargetObject: getTokenText(ctx.TOKEN()),
		},
	}

	if t.includeSourcePositions {
		fm.TargetMeta = makeSourcePositionMeta(ctx, fm.TargetMeta)
	}

	return fm
}

func (t *transpiler) VisitTargetRootField(ctx *parser.TargetRootFieldContext) interface{} {
	p := ctx.TargetPath().Accept(t).(pathSpec)

	if t.environment.name == "" {
		t.fail(ctx, errors.New("using the root keyword in a root mapping is redundant"))
	}

	t.environment.declareTarget(p.arg + p.index)

	fm := &mpb.FieldMapping{
		Target: &mpb.FieldMapping_TargetRootField{
			TargetRootField: jsonutil.JoinPath(p.arg, p.index, p.field),
		},
	}

	if t.includeSourcePositions {
		fm.TargetMeta = makeSourcePositionMeta(ctx, fm.TargetMeta)
	}

	return fm
}

func (t *transpiler) VisitTargetThis(ctx *parser.TargetThisContext) interface{} {
	fm := &mpb.FieldMapping{
		Target: &mpb.FieldMapping_TargetField{
			TargetField: jsonThis,
		},
	}

	if t.includeSourcePositions {
		fm.TargetMeta = makeSourcePositionMeta(ctx, fm.TargetMeta)
	}

	return fm
}

func (t *transpiler) VisitTargetField(ctx *parser.TargetFieldContext) interface{} {
	p := ctx.TargetPath().Accept(t).(pathSpec)

	t.environment.declareTarget(p.arg + p.index)

	fm := &mpb.FieldMapping{
		Target: &mpb.FieldMapping_TargetField{
			TargetField: jsonutil.JoinPath(p.arg, p.index, p.field),
		},
	}

	if t.includeSourcePositions {
		fm.TargetMeta = makeSourcePositionMeta(ctx, fm.TargetMeta)
	}

	return fm
}
