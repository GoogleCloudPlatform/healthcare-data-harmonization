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
	"regexp"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */
)

// identifierEscape is the quote escape character to use to indicate that an indentifier has special
// characters in it.
const identifierEscape = "'"

type pathSpec struct {
	arg, field, index string
}

// VisitTargetPath returns a pathSpec for the given TargetPathContext.
func (t *transpiler) VisitTargetPath(ctx *parser.TargetPathContext) interface{} {
	p := ctx.TargetPathHead().Accept(t).(pathSpec)
	for i := range ctx.AllTargetPathSegment() {
		p.field += ctx.TargetPathSegment(i).Accept(t).(string)
	}

	if ctx.OWMOD() != nil && ctx.OWMOD().GetText() != "" {
		p.field += ctx.OWMOD().GetText()
	}

	// Only one of p.arg and p.index can be filled.
	if (p.arg == "") == (p.index == "") {
		t.fail(ctx, fmt.Errorf("invalid target path - expected arg xor index but got both or neither (arg %s and index %s)", p.arg, p.index))
	}

	return p
}

// VisitTargetPathHead returns a partially filled pathSpec for the given TargetPathHeadContext.
// Either the arg or index field will be filled, as appropriate.
func (t *transpiler) VisitTargetPathHead(ctx *parser.TargetPathHeadContext) interface{} {
	if ctx.TOKEN() != nil && ctx.TOKEN().GetText() != "" {
		return pathSpec{
			arg: getTokenText(ctx.TOKEN()),
		}
	}

	// ROOT_INPUT is a special case path segment since normally they cannot contain $.
	if ctx.ROOT_INPUT() != nil && ctx.ROOT_INPUT().GetText() != "" {
		return pathSpec{
			arg: ctx.ROOT_INPUT().GetText(),
		}
	}

	// ROOT is a special case path segment since it is a keyword and does not get tokenized as a TOKEN
	// TODO(): Remove after sunset.
	if ctx.ROOT() != nil && ctx.ROOT().GetText() != "" {
		return pathSpec{
			arg: ctx.ROOT().GetText(),
		}
	}

	if ctx.Index() != nil && ctx.Index().GetText() != "" {
		return pathSpec{
			index: ctx.Index().GetText(),
		}
	}

	if ctx.WILDCARD() != nil && ctx.WILDCARD().GetText() != "" {
		return pathSpec{
			index: ctx.WILDCARD().GetText(),
		}
	}

	if ctx.ArrayMod() != nil && ctx.ArrayMod().GetText() != "" {
		return pathSpec{
			index: ctx.ArrayMod().GetText(),
		}
	}

	t.fail(ctx, fmt.Errorf("invalid target path head - no token, index, arraymod, or wildcard"))
	return nil
}

// VisitTargetPathSegment returns a string of the TargetPathSegmentContext contents.
func (t *transpiler) VisitTargetPathSegment(ctx *parser.TargetPathSegmentContext) interface{} {
	if ctx.TOKEN() != nil && ctx.TOKEN().GetText() != "" {
		delim := ""
		if ctx.DELIM() != nil {
			delim = ctx.DELIM().GetText()
		}
		return delim + getTokenText(ctx.TOKEN())
	}
	return ctx.GetText()
}

// VisitSourcePath returns a pathSpec for the given SourcePathContext.
func (t *transpiler) VisitSourcePath(ctx *parser.SourcePathContext) interface{} {
	p := ctx.SourcePathHead().Accept(t).(pathSpec)
	for i := range ctx.AllSourcePathSegment() {
		p.field += ctx.SourcePathSegment(i).Accept(t).(string)
	}

	// Only one of p.arg and p.index can be filled.
	if (p.arg == "") == (p.index == "") {
		t.fail(ctx, fmt.Errorf("invalid source path - expected arg xor index but got both or neither (arg %s and index %s)", p.arg, p.index))
	}

	return p
}

// VisitSourcePathHead returns a partially filled pathSpec for the given SourcePathHeadContext.
// Either the arg or index field will be filled, as appropriate.
func (t *transpiler) VisitSourcePathHead(ctx *parser.SourcePathHeadContext) interface{} {
	if ctx.TOKEN() != nil && ctx.TOKEN().GetText() != "" {
		return pathSpec{
			arg: getTokenText(ctx.TOKEN()),
		}
	}

	// ROOT_INPUT is a special case path segment since normally they cannot contain $.
	if ctx.ROOT_INPUT() != nil && ctx.ROOT_INPUT().GetText() != "" {
		return pathSpec{
			arg: ctx.ROOT_INPUT().GetText(),
		}
	}

	// ROOT is a special case path segment since it is a keyword and does not get tokenized as a TOKEN
	// TODO(): Remove after sunset.
	if ctx.ROOT() != nil && ctx.ROOT().GetText() != "" {
		return pathSpec{
			arg: ctx.ROOT().GetText(),
		}
	}

	t.fail(ctx, fmt.Errorf("invalid source path head - no token or %s", rootEnvInputName))
	return nil
}

// VisitSourcePathSegment returns a string of the SourcePathSegmentContext contents.
func (t *transpiler) VisitSourcePathSegment(ctx *parser.SourcePathSegmentContext) interface{} {
	if ctx.TOKEN() != nil && ctx.TOKEN().GetText() != "" {
		delim := ""
		if ctx.DELIM() != nil {
			delim = ctx.DELIM().GetText()
		}
		return delim + getTokenText(ctx.TOKEN())
	}
	return ctx.GetText()
}

var anyChar = regexp.MustCompile(".")

func getTokenText(node antlr.TerminalNode) string {
	nodeText := node.GetText()
	if strings.HasPrefix(nodeText, identifierEscape) && strings.HasSuffix(nodeText, identifierEscape) {
		return anyChar.ReplaceAllStringFunc(strings.Trim(nodeText, identifierEscape), func(s string) string {
			return `\` + s
		})
	}

	return nodeText
}
