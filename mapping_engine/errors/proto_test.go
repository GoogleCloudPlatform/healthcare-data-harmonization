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

package errors

import (
	"strings"
	"testing"

	"github.com/golang/protobuf/proto" /* copybara-comment: proto */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestProtoLocationIsFunction(t *testing.T) {
	tests := []struct {
		name  string
		proto proto.Message
		want  bool
	}{
		{
			name:  "not ProjectorDefinition",
			proto: &mappb.ValueSource{},
			want:  false,
		},
		{
			name:  "ProjectorDefinition",
			proto: &mappb.ProjectorDefinition{},
			want:  true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			pl := NewProtoLocationf(test.proto, "")
			got := pl.isFunctionStart
			if got != test.want {
				t.Errorf("{%v}IsFunction() got %v want %v", test.proto, got, test.want)
			}

			gotErr := pl.Error()
			if pl.isFunctionStart && !strings.HasSuffix(gotErr, "\n") {
				t.Errorf("{%v}Error() got %q, but did not end with \\n", test.proto, gotErr)
			}
			if !pl.isFunctionStart && !strings.HasPrefix(gotErr, "\t") {
				t.Errorf("{%v}Error() got %q, but did not start with \\t", test.proto, gotErr)
			}
		})
	}
}

func TestProtoLocationReflectedString(t *testing.T) {
	tests := []struct {
		name      string
		parent    proto.Message
		msgGetter func(parent proto.Message) proto.Message
		want      string
	}{
		{
			name:   "contains a 'name' field",
			parent: nil,
			msgGetter: func(parent proto.Message) proto.Message {
				return &mappb.ProjectorDefinition{
					Name: "MyFunc",
				}
			},
			want: "MyFunc",
		},
		{
			name: "is a part of an array field",
			parent: &mappb.ValueSource{
				AdditionalArg: []*mappb.ValueSource{
					&mappb.ValueSource{
						Projector: "MyFunc1",
					},
					&mappb.ValueSource{
						Projector: "MyFunc2",
					},
				},
			},
			msgGetter: func(parent proto.Message) proto.Message {
				return parent.(*mappb.ValueSource).AdditionalArg[1]
			},
			want: "2nd additional_arg",
		},
		{
			name: "is a value of a field",
			parent: &mappb.ValueSource{
				Source: &mappb.ValueSource_FromInput{
					FromInput: &mappb.ValueSource_InputSource{
						Arg:   100,
						Field: "MyField",
					},
				},
			},
			msgGetter: func(parent proto.Message) proto.Message {
				return parent.(*mappb.ValueSource).GetFromInput()
			},
			want: "from_input",
		},
		{
			name: "is a projected value",
			parent: &mappb.ValueSource{
				Source: &mappb.ValueSource_ProjectedValue{
					ProjectedValue: &mappb.ValueSource{
						Source: &mappb.ValueSource_FromInput{
							FromInput: &mappb.ValueSource_InputSource{
								Arg:   100,
								Field: "MyField",
							},
						},
						Projector: "SomeProjector",
					},
				},
			},
			msgGetter: func(parent proto.Message) proto.Message {
				return parent.(*mappb.ValueSource).GetProjectedValue()
			},
			want: "projected_value",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			msg := test.msgGetter(test.parent)
			pl := NewProtoLocation(msg, test.parent)
			got := pl.String()
			if !strings.Contains(got, test.want) {
				t.Errorf("NewProtoLocation(%v, %v).String() got %q want it to contain %q", msg, test.parent, got, test.want)
			}
		})
	}
}
