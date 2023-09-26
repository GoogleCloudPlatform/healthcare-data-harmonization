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
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/errors" /* copybara-comment: errors */
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */
)

// fail is a helper function to break out of parsing immediately with a Transpilation error.
func (t *transpiler) fail(ctx antlr.ParserRuleContext, err error) {
	panic(errors.NewTranspilationError(ctx.GetStart().GetLine(), ctx.GetStart().GetColumn(), err))
}
