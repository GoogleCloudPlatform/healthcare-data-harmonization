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
	"fmt"
	"testing"
)

func TestSuffixNumber(t *testing.T) {
	tests := []struct {
		input int
		want  string
	}{
		{
			input: 0,
			want:  "0th",
		},
		{
			input: 1,
			want:  "1st",
		},
		{
			input: 2,
			want:  "2nd",
		},
		{
			input: 3,
			want:  "3rd",
		},
		{
			input: 4,
			want:  "4th",
		},
		{
			input: 10,
			want:  "10th",
		},
		{
			input: 13,
			want:  "13th",
		},
		{
			input: 21,
			want:  "21st",
		},
		{
			input: 908082,
			want:  "908082nd",
		},
	}
	for _, test := range tests {
		t.Run(fmt.Sprintf("%d", test.input), func(t *testing.T) {
			got := SuffixNumber(test.input)
			if test.want != got {
				t.Errorf("SuffixNumber(%d) got %q want %q", test.input, got, test.want)
			}
		})
	}
}
