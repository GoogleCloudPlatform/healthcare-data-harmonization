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
	"github.com/antlr/antlr4/runtime/Go/antlr" /* copybara-comment: antlr */

	anypb "google.golang.org/protobuf/types/known/anypb"
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	// SourcePosStart is the metadata key for start position in Whistle source.
	SourcePosStart = "Source Position Start"

	// SourcePosStop is the metadata key for stop position in Whistle source.
	// The Column is exclusive, i.e., the last character is the one before.
	SourcePosStop = "Source Position Stop"
)

func startToSourcePosition(tok antlr.Token) *mpb.SourcePosition {
	return &mpb.SourcePosition{
		Line:   int32(tok.GetLine()),
		Column: int32(tok.GetColumn()),
	}
}

func stopToSourcePosition(tok antlr.Token) *mpb.SourcePosition {
	return &mpb.SourcePosition{
		Line:   int32(tok.GetLine()),
		Column: int32(tok.GetColumn() + len(tok.GetText())),
	}
}

// makeSourcePositionMeta adds entries for start and stop source positions for
// the provided Antlr ParserRuleContext to the provided Meta.
// If the provided Meta is nil, a new one is created and returned.
// If an error occurs while marhsalling to the Any, the corresponding start or
// stop entry does not get set.
func makeSourcePositionMeta(ctx antlr.ParserRuleContext, meta *mpb.Meta) *mpb.Meta {
	if meta == nil {
		meta = &mpb.Meta{}
	}
	if meta.Entries == nil {
		meta.Entries = make(map[string]*anypb.Any)
	}

	start := ctx.GetStart()
	stop := ctx.GetStop()

	if start != nil {
		// TODO(b/171877982): Create and set some error type when start/end are nil.
		if anyStart, err := anypb.New(startToSourcePosition(start)); err == nil {
			meta.Entries[SourcePosStart] = anyStart
		}
	}

	if stop != nil {
		if anyStop, err := anypb.New(stopToSourcePosition(stop)); err == nil {
			meta.Entries[SourcePosStop] = anyStop
		}
	}

	return meta
}
