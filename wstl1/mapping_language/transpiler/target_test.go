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

func TestVisitTargetVar(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "var abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "abc",
				},
			},
		},
		{
			name:  "complex",
			input: "var abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "abc.xyz[1][]",
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetVar"
	})
}

func TestVisitTargetVar_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "var abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "abc",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 7}),
					},
				},
			},
		},
		{
			name:  "complex",
			input: "var abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetLocalVar{
					TargetLocalVar: "abc.xyz[1][]",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 16}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetVar_IncludeSourcePositions"
	})
}

func TestVisitTargetObj(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "out abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetObject{
					TargetObject: "abc",
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetObj"
	})
}

func TestVisitTargetObj_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "out abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetObject{
					TargetObject: "abc",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 7}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetObj_IncludeSourcePositions"
	})
}

func TestVisitTargetRootField(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "root abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetRootField{
					TargetRootField: "abc",
				},
			},
		},
		{
			name:  "complex",
			input: "root abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetRootField{
					TargetRootField: "abc.xyz[1][]",
				},
			},
		},
	}

	tp := &transpiler{}
	tp.pushEnv(newEnv("not the root env" /* name */, []string{} /* args */, []string{} /* requiredArgs */))
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetRootField"
	})
}

func TestVisitTargetRootField_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "root abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetRootField{
					TargetRootField: "abc",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 8}),
					},
				},
			},
		},
		{
			name:  "complex",
			input: "root abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetRootField{
					TargetRootField: "abc.xyz[1][]",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 17}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	tp.pushEnv(newEnv("not the root env" /* name */, []string{} /* args */, []string{} /* requiredArgs */))
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetRootField_IncludeSourcePositions"
	})
}

func TestVisitTargetThis(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple test",
			input: "$this",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
			},
		},
	}

	tp := &transpiler{}
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetThis"
	})
}

func TestVisitTargetThis_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple test",
			input: "$this",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: ".",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 5}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetThis_IncludeSourcePositions"
	})
}

func TestVisitTargetField(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: "abc",
				},
			},
		},
		{
			name:  "complex",
			input: "abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: "abc.xyz[1][]",
				},
			},
		},
	}

	tp := &transpiler{}
	tp.pushEnv(newEnv("" /* name */, []string{} /* args */, []string{} /* requiredArgs */))
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetField"
	})
}

func TestVisitTargetField_IncludeSourcePositions(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "simple",
			input: "abc",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: "abc",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 3}),
					},
				},
			},
		},
		{
			name:  "complex",
			input: "abc.xyz[1][]",
			want: &mpb.FieldMapping{
				Target: &mpb.FieldMapping_TargetField{
					TargetField: "abc.xyz[1][]",
				},
				TargetMeta: &mpb.Meta{
					Entries: map[string]*anypb.Any{
						SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
						SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 12}),
					},
				},
			},
		},
	}

	tp := &transpiler{}
	tp.pushEnv(newEnv("" /* name */, []string{} /* args */, []string{} /* requiredArgs */))
	IncludeSourcePositions(tp)
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Target(), "TargetField_IncludeSourcePositions"
	})
}
