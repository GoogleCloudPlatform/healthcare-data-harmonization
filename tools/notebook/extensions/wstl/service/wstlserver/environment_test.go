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

func TestEnvironment_Session(t *testing.T) {
	env := NewEnvironment()
	testSessionID := "22"
	testSession, _ := NewSession(testSessionID)
	env.Sessions.Store(testSessionID, testSession)

	tests := []struct {
		name       string
		sessionID  string
		want       *Session
		wantErrors bool
	}{
		{
			name:       "session exists",
			sessionID:  testSession.ID(),
			want:       testSession,
			wantErrors: false,
		},
		{
			name:       "session does not exist",
			sessionID:  "99",
			want:       &Session{},
			wantErrors: true,
		},
		{
			name:       "session id does not contain integer",
			sessionID:  "should be integer",
			want:       &Session{},
			wantErrors: true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := env.Session(test.sessionID)
			if test.wantErrors && err == nil {
				t.Fatalf("expected error accessing session in environment id %q", test.sessionID)
			} else if !test.wantErrors && err != nil {
				t.Fatalf("could not access session with id %q got error %v", test.sessionID, err)
			}

			if !test.wantErrors && got.ID() != test.want.ID() {
				t.Fatalf("expected session %q got %q", test.want.ID(), got.ID())
			}
		})
	}
}

func TestEnvironment_CreateSession(t *testing.T) {
	env := NewEnvironment()
	testSessionID := "22"
	testSession, _ := NewSession(testSessionID)
	env.Sessions.Store(testSessionID, testSession)

	tests := []struct {
		name          string
		wantSessionID string
		wantErrors    bool
	}{
		{
			name:          "create a session that does not exist",
			wantSessionID: "99",
			wantErrors:    false,
		},
		{
			name:          "session already exists",
			wantSessionID: testSessionID,
			wantErrors:    true,
		},
		{
			name:          "invalid session identifier",
			wantSessionID: "should be an integer",
			wantErrors:    true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := env.CreateSession(test.wantSessionID)
			if test.wantErrors && err == nil {
				t.Fatalf("expected error creating session id %q in environment", test.wantSessionID)
			} else if !test.wantErrors && err != nil {
				t.Fatalf("could not create session with id %q got error %v", test.wantSessionID, err)
			}

			if !test.wantErrors && got.ID() != test.wantSessionID {
				t.Fatalf("expected session %q got %q", test.wantSessionID, got.ID())
			}
		})
	}
}

func TestEnvironment_DestroySession(t *testing.T) {
	env := NewEnvironment()
	testSessionID := "22"
	testSession, _ := NewSession(testSessionID)
	env.Sessions.Store(testSessionID, testSession)

	tests := []struct {
		name       string
		sessionID  string
		wantErrors bool
	}{
		{
			name:       "destroy a session that exists",
			sessionID:  testSessionID,
			wantErrors: false,
		},
		{
			name:       "destroy session that does not exist",
			sessionID:  "99",
			wantErrors: true,
		},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			err := env.DestroySession(test.sessionID)
			if test.wantErrors && err == nil {
				t.Fatalf("expected error destroying session id %q in environment", test.sessionID)
			} else if !test.wantErrors && err != nil {
				t.Fatalf("could not destroy session with id %q got error %v", test.sessionID, err)
			}

			if !test.wantErrors && env.SessionExists(test.sessionID) {
				t.Fatalf("session %q still exists in environment %v", test.sessionID, env)
			}
		})
	}
}
