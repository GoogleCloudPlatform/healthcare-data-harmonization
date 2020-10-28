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

func TestVisitPostProcessName(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple test",
			input: "post someName",
			want: &mpb.MappingConfig{
				PostProcess: &mpb.MappingConfig_PostProcessProjectorName{
					PostProcessProjectorName: "someName",
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.PostProcess(), "PostProcessName"
	})
}

func TestVisitPostProcessName_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple test",
			input: "post someName",
			want: &mpb.MappingConfig{
				PostProcess: &mpb.MappingConfig_PostProcessProjectorName{
					PostProcessProjectorName: "someName",
				},
				PostProcessMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 13}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.PostProcess(), "PostProcessName_IncludeSourcePositions"
	})
}

func TestVisitPostProcessInline(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "test",
			input: `post def projName() {}`,
			want: &mpb.MappingConfig{
				PostProcess: &mpb.MappingConfig_PostProcessProjectorDefinition{
					PostProcessProjectorDefinition: &mpb.ProjectorDefinition{
						Name: "projName",
					},
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.PostProcess(), "PostProcessInline"
	})
}

func TestVisitPostProcessInline_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "single line projector def",
			input: `post def projName() {}`,
			want: &mpb.MappingConfig{
				PostProcess: &mpb.MappingConfig_PostProcessProjectorDefinition{
					PostProcessProjectorDefinition: &mpb.ProjectorDefinition{
						Name: "projName",
					},
				},
				PostProcessMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 22}),
					},
				},
			},
		},
		{
			name: "multiline projector def",
			input: `post def projName() {

      }`,
			want: &mpb.MappingConfig{
				PostProcess: &mpb.MappingConfig_PostProcessProjectorDefinition{
					PostProcessProjectorDefinition: &mpb.ProjectorDefinition{
						Name: "projName",
					},
				},
				PostProcessMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 3, Column: 7}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.PostProcess(), "PostProcessInline"
	})
}
