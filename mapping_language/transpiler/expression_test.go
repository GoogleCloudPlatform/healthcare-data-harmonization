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

	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestVisitExprProjection(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "constant source",
			input: "3.14",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstFloat{
					ConstFloat: 3.14,
				},
			},
		},
		{
			name:  "token source",
			input: "arg1",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_FromInput{
					FromInput: &mpb.ValueSource_InputSource{
						Arg: 1,
					},
				},
			},
		},
		{
			name:  "no arg call",
			input: "Function()",
			want: &mpb.ValueSource{
				Projector: "Function",
			},
		},
		{
			name:  "one arg call with brackets",
			input: "Function(arg1)",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_FromInput{
					FromInput: &mpb.ValueSource_InputSource{
						Arg: 1,
					},
				},
				Projector: "Function",
			},
		},
		{
			name:  "multi arg call with brackets",
			input: "Function(arg1, 3.14)",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_FromInput{
					FromInput: &mpb.ValueSource_InputSource{
						Arg: 1,
					},
				},
				AdditionalArg: []*mpb.ValueSource{
					{
						Source: &mpb.ValueSource_ConstFloat{
							ConstFloat: 3.14,
						},
					},
				},
				Projector: "Function",
			},
		},
		{
			name:  "chained call with brackets",
			input: "OtherFunc(Function(arg1, 3.14))",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_FromInput{
							FromInput: &mpb.ValueSource_InputSource{
								Arg: 1,
							},
						},
						AdditionalArg: []*mpb.ValueSource{
							{
								Source: &mpb.ValueSource_ConstFloat{
									ConstFloat: 3.14,
								},
							},
						},
						Projector: "Function",
					},
				},
				Projector: "OtherFunc",
			},
		},
	}

	tp := &transpiler{}
	tp.pushEnv(newEnv("", []string{"arg1"}, []string{}))
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Expression(), "Expression"
	})

}

func TestVisitListInitialization(t *testing.T) {
	tests := []transpilerTest{
		{
			name:  "list with arg",
			input: "[arg1]",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_FromInput{
							FromInput: &mpb.ValueSource_InputSource{
								Arg: 1,
							},
						},
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "list with const int",
			input: "[3]",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_ConstFloat{
							ConstFloat: 3,
						},
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "list with const float",
			input: "[3.14]",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_ConstFloat{
							ConstFloat: 3.14,
						},
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "list with arg and const",
			input: "[arg1, 3.14]",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_FromInput{
							FromInput: &mpb.ValueSource_InputSource{
								Arg: 1,
							},
						},
					},
				},
				AdditionalArg: []*mpb.ValueSource{
					{
						Source: &mpb.ValueSource_ConstFloat{
							ConstFloat: 3.14,
						},
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "empty list",
			input: "[]",
			want: &mpb.ValueSource{
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "list with multi arg call with brackets",
			input: "[Function(arg1, 3.14)]",
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_FromInput{
							FromInput: &mpb.ValueSource_InputSource{
								Arg: 1,
							},
						},
						AdditionalArg: []*mpb.ValueSource{
							{
								Source: &mpb.ValueSource_ConstFloat{
									ConstFloat: 3.14,
								},
							},
						},
						Projector: "Function",
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "nested lists",
			input: `["a1", ["a2b1", ["a2b2c1"], ["a2b3c1"]]]`,
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "a1",
						},
					},
				},
				AdditionalArg: []*mpb.ValueSource{
					{
						Source: &mpb.ValueSource_ProjectedValue{
							ProjectedValue: &mpb.ValueSource{
								Source: &mpb.ValueSource_ConstString{
									ConstString: "a2b1",
								},
							},
						},
						AdditionalArg: []*mpb.ValueSource{
							{
								Source: &mpb.ValueSource_ProjectedValue{
									ProjectedValue: &mpb.ValueSource{
										Source: &mpb.ValueSource_ConstString{
											ConstString: "a2b2c1",
										},
									},
								},
								Projector: listInitializationProjector,
							},
							{
								Source: &mpb.ValueSource_ProjectedValue{
									ProjectedValue: &mpb.ValueSource{
										Source: &mpb.ValueSource_ConstString{
											ConstString: "a2b3c1",
										},
									},
								},
								Projector: listInitializationProjector,
							},
						},
						Projector: listInitializationProjector,
					},
				},
				Projector: listInitializationProjector,
			},
		},
		{
			name:  "complex list initialization",
			input: `[1, "two", false, OtherFunc(Function(arg1, 3.14))]`,
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ProjectedValue{
					ProjectedValue: &mpb.ValueSource{
						Source: &mpb.ValueSource_ConstFloat{
							ConstFloat: 1,
						},
					},
				},
				AdditionalArg: []*mpb.ValueSource{
					{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "two",
						},
					},
					{
						Source: &mpb.ValueSource_ConstBool{
							ConstBool: false,
						},
					},
					{
						Source: &mpb.ValueSource_ProjectedValue{
							ProjectedValue: &mpb.ValueSource{
								Source: &mpb.ValueSource_FromInput{
									FromInput: &mpb.ValueSource_InputSource{
										Arg: 1,
									},
								},
								AdditionalArg: []*mpb.ValueSource{
									{
										Source: &mpb.ValueSource_ConstFloat{
											ConstFloat: 3.14,
										},
									},
								},
								Projector: "Function",
							},
						},
						Projector: "OtherFunc",
					},
				},
				Projector: listInitializationProjector,
			},
		},
	}

	tp := &transpiler{}
	tp.pushEnv(newEnv("", []string{"arg1"}, []string{}))
	testRule(t, tests, tp, func(p *parser.WhistleParser) (antlr.ParseTree, string) {
		return p.Expression(), "ListInitialization"
	})
}
