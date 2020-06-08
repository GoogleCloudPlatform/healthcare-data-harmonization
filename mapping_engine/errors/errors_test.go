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
)

func TestStringLocation(t *testing.T) {
	tests := []struct {
		name, format string
		args         []interface{}
		want         string
	}{
		{
			name: "empty string",
		},
		{
			name:   "non-formatted string",
			format: "hello",
			want:   "hello",
		},
		{
			name:   "formatted string",
			format: "hello %s",
			args:   []interface{}{"world"},
			want:   "hello world",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			for _, fn := range []bool{true, false} {
				var gotLocation error
				var prefix string
				if fn {
					prefix = "Fn"
					gotLocation = FnLocationf(test.format, test.args...)
				} else {
					gotLocation = Locationf(test.format, test.args...)
				}

				if got := strings.TrimSpace(gotLocation.Error()); got != test.want {
					t.Errorf("%sLocationf(%q, %v) got %q want %q", prefix, test.format, test.args, got, test.want)
				}

				if !fn && !strings.HasPrefix(gotLocation.Error(), DefaultPrefix) {
					t.Errorf("%sLocationf(%q, %v) got %q, must start with \\t but did not", prefix, test.format, test.args, gotLocation.Error())
				}
				if fn && !strings.HasSuffix(gotLocation.Error(), FunctionStartSuffix) {
					t.Errorf("%sLocationf(%q, %v) got %q, must end with \\n but did not", prefix, test.format, test.args, gotLocation.Error())
				}
			}
		})
	}
}
