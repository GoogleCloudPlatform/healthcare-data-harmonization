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
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_language/parser" /* copybara-comment: parser */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */
)

func (t *transpiler) VisitBlock(ctx *parser.BlockContext) interface{} {
	// The block is just a list of children which should be handled by their own rules.
	for _, c := range ctx.GetChildren() {
		c.(antlr.ParseTree).Accept(t)
	}

	return nil
}
