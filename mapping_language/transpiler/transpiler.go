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
	"runtime/debug"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/errors" /* copybara-comment: errors */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */

	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

type transpiler struct {
	environment    *env
	projectors     []*mpb.ProjectorDefinition
	conditionStack valueStack
}

func (t *transpiler) pushEnv(e *env) {
	t.environment = e
}

func (t *transpiler) popEnv() {
	t.environment = t.environment.parent
}

// Transpile converts the given Whistle into a Whistler mapping config.
func Transpile(whistle string) (mp *mpb.MappingConfig, err error) {
	defer func() {
		if rec := recover(); rec != nil {
			err = fmt.Errorf("%v\n\n%s", rec, debug.Stack())
		}
	}()

	is := antlr.NewInputStream(whistle)

	// Create the Lexer.
	lexer := parser.NewWhistleLexer(is)
	lexer.AddErrorListener(&errors.LexerListener{Code: whistle})

	stream := antlr.NewCommonTokenStream(lexer, antlr.TokenDefaultChannel)

	// Create the Parser.
	p := parser.NewWhistleParser(stream)
	p.AddErrorListener(&errors.ParserListener{Code: whistle})

	// NOTE: explicitly specifying the type of transpiler is necessary so that the methods of
	// the appropriate type, that implements the visitor interface, are invoked.
	var transpiler parser.WhistleVisitor = &transpiler{}

	mp = p.Root().Accept(transpiler).(*mpb.MappingConfig)
	return
}
