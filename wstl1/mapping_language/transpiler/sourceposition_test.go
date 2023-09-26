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
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"google.golang.org/protobuf/testing/protocmp" /* copybara-comment: protocmp */

	anypb "google.golang.org/protobuf/types/known/anypb"
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

var tokenFactory = antlr.CommonTokenFactoryDEFAULT

func makeContext(start, stop antlr.Token) *antlr.BaseParserRuleContext {
	ctx := antlr.NewBaseParserRuleContext(nil, 0)
	ctx.SetStart(start)
	ctx.SetStop(stop)
	return ctx
}

func makeToken(line, col int, text string) antlr.Token {
	return tokenFactory.Create(&antlr.TokenSourceCharStreamPair{}, 0, text, 0, 0, 0, line, col)
}

func TestMakeSourcePositionMeta(t *testing.T) {
	type test struct {
		name        string
		start, stop antlr.Token
		meta        *mpb.Meta
		want        *mpb.Meta
	}

	tests := []test{
		{
			name:  "single word",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 0, "single"),
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 6}),
				},
			},
		},
		{
			name:  "single line, different word",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 7, "another token"),
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 20}),
				},
			},
		},
		{
			name:  "multi line",
			start: makeToken(2, 20, "doesn't matter"),
			stop:  makeToken(5, 5, "matters"),
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 2, Column: 20}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 5, Column: 12}),
				},
			},
		},
		{
			name:  "only start",
			start: makeToken(1, 1, "start"),
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 1}),
				},
			},
		},
		{
			name: "only stop",
			stop: makeToken(2, 2, "stop"),
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStop: makeAny(t, &mpb.SourcePosition{Line: 2, Column: 6}),
				},
			},
		},
		{
			name: "no start or stop",
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{},
			},
		},
		{
			name:  "existing meta, no entries",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 0, "single"),
			meta:  &mpb.Meta{},
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 6}),
				},
			},
		},
		{
			name:  "existing meta, empty entries",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 0, "single"),
			meta: &mpb.Meta{
				Entries: map[string]*anypb.Any{},
			},
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 6}),
				},
			},
		},
		{
			name:  "existing meta, existing entries get overwritten",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 0, "single"),
			meta: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 98, Column: 99}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 100, Column: 101}),
				},
			},
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 6}),
				},
			},
		},
		{
			name:  "existing meta, non-conflicting entries retained",
			start: makeToken(1, 0, "single"),
			stop:  makeToken(1, 0, "single"),
			meta: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					"foo": makeAny(t, &mpb.SourcePosition{Line: 98, Column: 99}),
					"bar": makeAny(t, &mpb.SourcePosition{Line: 100, Column: 101}),
				},
			},
			want: &mpb.Meta{
				Entries: map[string]*anypb.Any{
					SourcePosStart: makeAny(t, &mpb.SourcePosition{Line: 1, Column: 0}),
					SourcePosStop:  makeAny(t, &mpb.SourcePosition{Line: 1, Column: 6}),
					"foo":          makeAny(t, &mpb.SourcePosition{Line: 98, Column: 99}),
					"bar":          makeAny(t, &mpb.SourcePosition{Line: 100, Column: 101}),
				},
			},
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := makeSourcePositionMeta(makeContext(test.start, test.stop), test.meta)

			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Errorf("makeSourcePositionMeta(%v, %v) got %v\n-want +got %s", test.start, test.stop, got, diff)
			}
		})
	}
}
