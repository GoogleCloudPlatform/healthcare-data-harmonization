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
	"regexp"
	"testing"
)

func TestTranspileErrors(t *testing.T) {
	type valueTest struct {
		rootMappings, wantJSON, inputJSON string
	}
	tests := []struct {
		name            string
		whistle         string
		wantErrKeywords []string
	}{
		{
			name: "variable with same name as arg",
			whistle: `def hello(world) {
									var world: "bad"
							 }`,
			wantErrKeywords: []string{"world", "function", "argument", "variable"},
		},
		{
			name:            "redundant root keyword",
			whistle:         `root hello: "world"`,
			wantErrKeywords: []string{"redundant", "root"},
		},
		{
			name:            "function without brackets",
			whistle:         `root hello: FooFunc "world"`,
			wantErrKeywords: []string{"parser error"},
		},
		// TODO: Add more tests.
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			// Just test bare compilation
			got, err := Transpile(test.whistle)
			if err == nil {
				t.Fatalf("Transpile(...) got %v\nwant error\nwhistle code:\n%s", got, test.whistle)
			}

			str := err.Error()
			for _, kw := range test.wantErrKeywords {
				if m, err := regexp.MatchString(fmt.Sprintf(`\b%s\b`, kw), str); !m {
					if err != nil {
						t.Fatalf("Failed to regexp search for keyword %q: %v", kw, err)
					}
					t.Errorf("Transpile(...) got error %q, want keyword %q", str, kw)
				}
			}

		})
	}
}
