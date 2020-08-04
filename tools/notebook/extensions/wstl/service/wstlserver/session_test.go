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
package wstlserver

import (
	"testing"
)

func TestSession_NewSession(t *testing.T) {
	tests := []struct {
		name       string
		sessionID  string
		want       *Session
		wantErrors bool
	}{
		{
			name:      "client provided session ID",
			sessionID: "99",
			want: &Session{
				id:      "99",
				Context: &Context{},
			},
			wantErrors: false,
		},
		{
			name:      "client missing session ID",
			sessionID: "",
			want: &Session{
				id:      FallbackSessionID,
				Context: &Context{},
			},
			wantErrors: false,
		},
		{
			name:       "invalid session ID",
			sessionID:  "should be an integer",
			want:       &Session{},
			wantErrors: true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := NewSession(test.sessionID)
			if test.wantErrors && err == nil {
				t.Fatalf("expected error creating new session id %q", test.sessionID)
			} else if !test.wantErrors && err != nil {
				t.Fatalf("could not create session with id %q got error %v", test.want.ID(), err)
			}

			if !test.wantErrors && got.ID() != test.want.ID() {
				t.Fatalf("expected session %q got %q", test.want.ID(), got.ID())
			}
		})
	}
}
