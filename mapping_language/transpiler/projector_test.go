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

func TestVisitProjectorDef(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "empty proj",
			input: `def empty() {}`,
			want: &mpb.ProjectorDefinition{
				Name: "empty",
			},
		},
		{
			name: "single mapping proj",
			input: `def single() {
			  $this: 5
			}`,
			want: &mpb.ProjectorDefinition{
				Name: "single",
				Mapping: []*mpb.FieldMapping{
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "."},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_ConstFloat{ConstFloat: 5}},
					},
				},
			},
		},
		{
			name: "complex mapping proj",
			input: `def complex(x) {
			  $this: x
				var a: "b"
				c: a
			}`,
			want: &mpb.ProjectorDefinition{
				Name: "complex",
				Mapping: []*mpb.FieldMapping{
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "."},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_FromInput{FromInput: &mpb.ValueSource_InputSource{Arg: 1}}},
					},
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetLocalVar{TargetLocalVar: "a"},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_ConstString{ConstString: "b"}},
					},
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "c"},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_FromLocalVar{FromLocalVar: "a"}},
					},
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.ProjectorDef(), "ProjectorDef"
	})
}

func TestVisitProjectorDef_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "empty proj",
			input: `def empty() {}`,
			want: &mpb.ProjectorDefinition{
				Name: "empty",
				Meta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 14}),
					},
				},
			},
		},
		{
			name: "single mapping proj",
			input: `def single() {
			  $this: 5
			}`,
			want: &mpb.ProjectorDefinition{
				Name: "single",
				Mapping: []*mpb.FieldMapping{
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "."},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_ConstFloat{ConstFloat: 5}},
					},
				},
				Meta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 3, Column: 4}),
					},
				},
			},
		},
		{
			name: "complex mapping proj",
			input: `def complex(x) {
			  $this: x
				var a: "b"
				c: a
			}`,
			want: &mpb.ProjectorDefinition{
				Name: "complex",
				Mapping: []*mpb.FieldMapping{
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "."},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_FromInput{FromInput: &mpb.ValueSource_InputSource{Arg: 1}}},
					},
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetLocalVar{TargetLocalVar: "a"},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_ConstString{ConstString: "b"}},
					},
					&mpb.FieldMapping{
						Target:      &mpb.FieldMapping_TargetField{TargetField: "c"},
						ValueSource: &mpb.ValueSource{Source: &mpb.ValueSource_FromLocalVar{FromLocalVar: "a"}},
					},
				},
				Meta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 5, Column: 4}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.ProjectorDef(), "ProjectorDef_IncludeSourcePositions"
	})
}
