// Copyright 2020 Google LLC.
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
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */

	anypb "google.golang.org/protobuf/types/known/anypb"
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestVisitMapping(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "this target, const float source",
			input: `$this: 1`,
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
				ValueSource: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstFloat{ConstFloat: 1},
				},
			},
		},
		{
			name:  "this target, inline condition, const float source",
			input: `$this (if true): 1`,
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
				Condition: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstBool{ConstBool: true},
				},
				ValueSource: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstFloat{ConstFloat: 1},
				},
			},
		},
	}

	tp := newTranspiler()
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Mapping(), "Mapping"
	})
}

func TestVisitMapping_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "this target, const float source",
			input: `$this: 1`,
			want: &mpb.FieldMapping{
				Meta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 8}),
					},
				},
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 5}),
					},
				},
				ValueSource: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstFloat{ConstFloat: 1},
				},
			},
		},
		{
			name:  "this target, inline condition, const float source",
			input: `$this (if true): 1`,
			want: &mpb.FieldMapping{
				Meta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 18}),
					},
				},
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 5}),
					},
				},
				Condition: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstBool{ConstBool: true},
				},
				ValueSource: &mpb.ValueSource{
					Source: &mpb.ValueSource_ConstFloat{ConstFloat: 1},
				},
			},
		},
	}

	tp := newTranspiler(IncludeSourcePositions)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Mapping(), "Mapping_IncludeSourcePositions"
	})
}
