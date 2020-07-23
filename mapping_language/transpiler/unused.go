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
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */
)

func (t *transpiler) Visit(tree antlr.ParseTree) interface{} {
	panic("unused rule entered by visitor - this should never happen")
}

func (t *transpiler) VisitChildren(node antlr.RuleNode) interface{} {
	// NOTE: Hitting the below panic may indicate that the transpiler instance being
	// instantiated is not implementing the visitor interface. To verify, expliclity
	// declare the type of transpiler instance being intiatiated.
	panic("unused rule VisitChildren entered by visitor - this should never happen")
}

func (t *transpiler) VisitTerminal(node antlr.TerminalNode) interface{} {
	// No-op - this gets hit when enumerating children and some of them are tokens
	return nil
}

func (t *transpiler) VisitErrorNode(node antlr.ErrorNode) interface{} {
	panic("unused rule VisitErrorNode entered by visitor - this should never happen")
}

func (t *transpiler) VisitFloatingPoint(ctx *parser.FloatingPointContext) interface{} {
	panic("unused rule VisitFloatingPoint entered by visitor - this should never happen")
}

func (t *transpiler) VisitArgAlias(ctx *parser.ArgAliasContext) interface{} {
	panic("unused rule VisitArgAlias entered by visitor - this should never happen")
}

func (t *transpiler) VisitComment(ctx *parser.CommentContext) interface{} {
	// No-op
	return nil
}

func (t *transpiler) VisitBioperator1(ctx *parser.Bioperator1Context) interface{} {
	panic("unused rule VisitBioperator1 entered by visitor - this should never happen")
}

func (t *transpiler) VisitBioperator2(ctx *parser.Bioperator2Context) interface{} {
	panic("unused rule VisitBioperator2 entered by visitor - this should never happen")
}

func (t *transpiler) VisitBioperator3(ctx *parser.Bioperator3Context) interface{} {
	panic("unused rule VisitBioperator3 entered by visitor - this should never happen")
}

func (t *transpiler) VisitBioperator4(ctx *parser.Bioperator4Context) interface{} {
	panic("unused rule VisitBioperator4 entered by visitor - this should never happen")
}

func (t *transpiler) VisitPostunoperator(ctx *parser.PostunoperatorContext) interface{} {
	panic("unused rule VisitPostunoperator entered by visitor - this should never happen")
}

func (t *transpiler) VisitPreunoperator(ctx *parser.PreunoperatorContext) interface{} {
	panic("unused rule VisitPreunoperator entered by visitor - this should never happen")
}

func (t *transpiler) VisitIndex(ctx *parser.IndexContext) interface{} {
	panic("unused rule VisitIndex entered by visitor - this should never happen")
}

func (t *transpiler) VisitArrayMod(ctx *parser.ArrayModContext) interface{} {
	panic("unused rule VisitArrayMod entered by visitor - this should never happen")
}
