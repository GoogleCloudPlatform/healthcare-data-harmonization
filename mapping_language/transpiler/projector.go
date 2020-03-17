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
)

func (t *transpiler) VisitProjectorDef(ctx *parser.ProjectorDefContext) interface{} {
	var aliases []string
	for i := range ctx.AllArgAlias() {
		aliases = append(aliases, ctx.ArgAlias(i).GetText())
	}

	// Create a new environment for each projector.
	t.pushEnv(t.environment.newChild(ctx.TOKEN().GetText(), aliases))

	ctx.Block().Accept(t)

	proj := t.environment.generateProjector()

	t.popEnv()

	return proj
}
