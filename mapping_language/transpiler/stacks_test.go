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
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"google.golang.org/protobuf/testing/protocmp" /* copybara-comment: protocmp */

	mpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func TestStack(t *testing.T) {
	tests := []struct {
		name   string
		stack  *valueStack
		others []*mpb.ValueSource
		want   *mpb.ValueSource
	}{
		{
			name:  "empty stack",
			stack: &valueStack{},
			want:  nil,
		},
		{
			name: "one item stack",
			stack: &valueStack{
				&mpb.ValueSource{
					// Note that in practice, all items in a valueStack should evaluate to bool, but in these
					// tests we want a way to identify them.
					Source: &mpb.ValueSource_ConstString{
						ConstString: "A",
					},
				},
			},
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstString{
					ConstString: "A",
				},
			},
		},
		{
			name: "multiple item stack",
			stack: &valueStack{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "A",
					},
				},
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "B",
					},
				},
			},
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstString{
					ConstString: "A",
				},
				AdditionalArg: []*mpb.ValueSource{
					&mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "B",
						},
					},
				},
				Projector: "$And",
			},
		},
		{
			name:  "empty stack with one other",
			stack: &valueStack{},
			others: []*mpb.ValueSource{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "AOther",
					},
				},
			},
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstString{
					ConstString: "AOther",
				},
			},
		},
		{
			name: "one item stack with one other",
			stack: &valueStack{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "A",
					},
				},
			},
			others: []*mpb.ValueSource{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "AOther",
					},
				},
			},
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstString{
					ConstString: "A",
				},
				AdditionalArg: []*mpb.ValueSource{
					&mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "AOther",
						},
					},
				},
				Projector: "$And",
			},
		},
		{
			name: "multiple item stack with multiple others",
			stack: &valueStack{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "A",
					},
				},
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "B",
					},
				},
			},
			others: []*mpb.ValueSource{
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "AOther",
					},
				},
				&mpb.ValueSource{
					Source: &mpb.ValueSource_ConstString{
						ConstString: "BOther",
					},
				},
			},
			want: &mpb.ValueSource{
				Source: &mpb.ValueSource_ConstString{
					ConstString: "A",
				},
				AdditionalArg: []*mpb.ValueSource{
					&mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "B",
						},
					},
					&mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "AOther",
						},
					},
					&mpb.ValueSource{
						Source: &mpb.ValueSource_ConstString{
							ConstString: "BOther",
						},
					},
				},
				Projector: "$And",
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := test.stack.and(test.others...)
			if diff := cmp.Diff(test.want, got, protocmp.Transform()); diff != "" {
				t.Errorf("%v.and(%v) got unexpected value: -want +got: %s", test.stack, test.others, diff)
			}
		})
	}
}
