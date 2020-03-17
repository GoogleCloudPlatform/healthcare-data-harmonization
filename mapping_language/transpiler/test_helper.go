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
	"fmt"
	"runtime/debug"
	"testing"

	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/golang/protobuf/proto" /* copybara-comment: proto */
	"google.golang.org/protobuf/testing/protocmp" /* copybara-comment: protocmp */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/errors" /* copybara-comment: errors */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
)

type transpilerTest struct {
	name, input string
	want        proto.Message
}

func createParser(t *testing.T, whistle string) *parser.WhistleParser {
	t.Helper()
	is := antlr.NewInputStream(whistle)

	// Create the Lexer.
	lexer := parser.NewWhistleLexer(is)
	lexer.RemoveErrorListeners()
	lexer.AddErrorListener(&errors.LexerListener{Code: whistle})

	stream := antlr.NewCommonTokenStream(lexer, antlr.TokenDefaultChannel)

	// Create the Parser.
	p := parser.NewWhistleParser(stream)
	p.RemoveErrorListeners()
	p.AddErrorListener(&errors.ParserListener{Code: whistle})

	return p
}

func transpileSingleRule(t *testing.T, transpiler parser.WhistleVisitor, rule antlr.ParseTree) (mp proto.Message, err error) {
	t.Helper()
	defer func() {
		if rec := recover(); rec != nil {
			err = fmt.Errorf("%v\n\n%s", rec, debug.Stack())
		}
	}()

	mp = rule.Accept(transpiler).(proto.Message)
	return
}

func testRule(t *testing.T, tests []transpilerTest, transpiler parser.WhistleVisitor, rule func(p *parser.WhistleParser) (antlr.ParseTree, string)) {
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			p := createParser(t, test.input)
			r, name := rule(p)
			got, err := transpileSingleRule(t, transpiler, r)
			if err != nil {
				t.Fatalf("%s.Accept(%#v) got unexpected error %v", name, transpiler, err)
			}

			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Errorf("%s.Accept(%#v) got %v\n-want +got %s", name, transpiler, got, diff)
			}
		})
	}
}
