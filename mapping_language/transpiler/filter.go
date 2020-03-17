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
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_language/parser" /* copybara-comment: parser */
)

func (t *transpiler) VisitInlineFilter(ctx *parser.InlineFilterContext) interface{} {
	return ctx.Filter().Accept(t).(*mpb.ValueSource)
}

func (t *transpiler) VisitFilter(ctx *parser.FilterContext) interface{} {
	return ctx.Expression().Accept(t).(*mpb.ValueSource)
}
