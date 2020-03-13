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

package jsonutil

import (
	"encoding/json"
	"sync"
	"testing"
	"time"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

func TestGetValue(t *testing.T) {
	tests := []struct {
		name  string
		token JSONToken
		want  JSONToken
	}{
		{
			name:  "get nil",
			token: nil,
			want:  nil,
		},
		{
			name:  "get JSONNum",
			token: JSONNum(1),
			want:  JSONNum(1),
		},
		{
			name:  "get JSONStr",
			token: JSONStr("google"),
			want:  JSONStr("google"),
		},
		{
			name:  "get JSONBool",
			token: JSONBool(true),
			want:  JSONBool(true),
		},
		{
			name:  "get JSONArr",
			token: JSONArr{JSONStr("a"), JSONNum(0)},
			want:  JSONArr{JSONStr("a"), JSONNum(0)},
		},
		{
			name:  "get JSONContainer",
			token: mustParseJSON(t, json.RawMessage(`{"id": 0, "v": false}`)),
			want:  mustParseJSON(t, json.RawMessage(`{"id": 0, "v": false}`)),
		},
		{
			name: "get JSONFuture",
			token: &JSONFuture{
				channels: make([]chan JSONToken, 0),
				value:    JSONStr("value"),
				ready:    true,
			},
			want: JSONStr("value"),
		},
		{
			name: "get JSONVersions",
			token: &JSONVersions{
				versions: map[int]JSONToken{
					0: nil,
					1: JSONNum(0),
					2: JSONBool(false),
					4: JSONStr("value"),
				},
				latest: 4,
			},
			want: JSONStr("value"),
		},
		{
			name:  "get empty JSONVersions",
			token: NewJSONVersions(),
			want:  nil,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := GetValue(test.token)
			if !cmp.Equal(got, test.want) {
				t.Errorf("GetValue(%v) got %v want %v", test.token, got, test.token)
			}
		})
	}
}

func TestJSONFuture(t *testing.T) {
	tests := []struct {
		name        string
		value       JSONToken
		numReceiver int
		sender      bool
		want        int
	}{
		{
			name:        "one sender and one receiver",
			value:       JSONNum(0),
			numReceiver: 1,
			sender:      true,
			want:        1,
		},
		{
			name:        "one sender and multiple receivers",
			value:       JSONNum(0),
			numReceiver: 4,
			sender:      true,
			want:        4,
		},
		{
			name:        "one sender and no receiver",
			value:       JSONNum(0),
			numReceiver: 0,
			sender:      true,
			want:        0,
		},
		{
			name:        "no sender and one receiver",
			value:       JSONNum(0),
			numReceiver: 1,
			sender:      false,
			want:        0,
		},
		{
			name:        "no sender and multiple receivers",
			value:       JSONNum(0),
			numReceiver: 4,
			sender:      false,
			want:        0,
		},
		{
			name:        "no sender and no receiver",
			value:       JSONNum(0),
			numReceiver: 0,
			sender:      false,
			want:        0,
		},
	}
	for _, test := range tests {
		t.Run("receivers before sender "+test.name, func(t *testing.T) {
			f := NewJSONFuture()
			var lock sync.Mutex
			received := make([]JSONToken, 0)

			var wg sync.WaitGroup
			wg.Add(test.numReceiver)

			// Receivers
			for i := 0; i < test.numReceiver; i++ {
				go func() {
					j := f.Value()
					lock.Lock()
					received = append(received, j)
					lock.Unlock()
					wg.Done()
				}()
			}
			time.Sleep(100 * time.Millisecond)
			// Sender
			if test.sender {
				go func() {
					f.Set(test.value)
				}()
				wg.Wait()
			}

			if len(received) != test.want {
				t.Errorf("number of receiver got %d want %d", len(received), test.want)
			}
			for _, r := range received {
				if !cmp.Equal(r, test.value) {
					t.Errorf("received unexpected JSONToken %v want %v", r, test.value)
				}
			}
		})
	}

	for _, test := range tests {
		t.Run("receivers and sender interleaved"+test.name, func(t *testing.T) {
			f := NewJSONFuture()
			var lock sync.Mutex
			received := make([]JSONToken, 0)

			var wg sync.WaitGroup
			wg.Add(test.numReceiver)

			// Receivers
			go func() {
				for i := 0; i < test.numReceiver; i++ {
					go func() {
						j := f.Value()
						lock.Lock()
						received = append(received, j)
						lock.Unlock()
						wg.Done()
					}()
				}
			}()
			// Sender
			go func() {
				if test.sender {
					f.Set(test.value)
				}
			}()

			if test.sender {
				wg.Wait()
			}

			if len(received) != test.want {
				t.Errorf("number of receiver got %d want %d", len(received), test.want)
			}
			for _, r := range received {
				if !cmp.Equal(r, test.value) {
					t.Errorf("received unexpected JSONToken %v want %v", r, test.value)
				}
			}
		})
	}
}

func TestJSONFuture_Error(t *testing.T) {
	f := NewJSONFuture()
	if err := f.Set(JSONNum(0)); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := f.Set(JSONBool(false)); err == nil {
		t.Error("expected an error but got nil")
	}
}

func TestJSONVersions_AddAndValue(t *testing.T) {
	t.Run("get without add", func(t *testing.T) {
		m := NewJSONVersions()
		got := m.Value()
		if got != nil {
			t.Errorf("expected nil but got %v", got)
		}
	})
	t.Run("get latest", func(t *testing.T) {
		m := NewJSONVersions()
		if err := m.AddVersion(JSONNum(0), 2); err != nil {
			t.Errorf("got unexpected error: %v", err)
		}
		want := JSONStr("value")
		if err := m.AddVersion(want, 8); err != nil {
			t.Errorf("got unexpected error: %v", err)
		}

		got := m.Value()
		if got != want {
			t.Errorf("expected %v but got %v", want, got)
		}
	})
}

func TestJSONVersions_AddAndVersion(t *testing.T) {
	want := JSONBool(false)

	m := NewJSONVersions()
	if err := m.AddVersion(JSONNum(0), 1); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(want, 2); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(JSONStr("value"), 3); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}

	got := m.Version(2)
	if got != want {
		t.Errorf("expected %v but got %v", want, got)
	}
}

func TestJSONVersions_PrevVersion(t *testing.T) {
	want := JSONNum(0)

	m := NewJSONVersions()
	if err := m.AddVersion(want, 1); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(JSONBool(false), 2); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(JSONStr("value"), 3); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}

	got := m.PrevVersion(2)
	if got != want {
		t.Errorf("expected %v but got %v", want, got)
	}
}

func TestJSONVersions_ExistVersion(t *testing.T) {
	m := NewJSONVersions()
	if err := m.AddVersion(JSONNum(0), 1); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}

	if m.ExistVersion(0) {
		t.Errorf("not expected existence of a version")
	}
	if !m.ExistVersion(1) {
		t.Errorf("expected existence of a vesion")
	}
}

func TestJSONVersions_SkipVersion(t *testing.T) {
	want := JSONNum(0)

	m := NewJSONVersions()

	if err := m.AddVersion(want, 1); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(NewJSONFuture(), 2); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(NewJSONFuture(), 3); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.SkipVersion(2); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.SkipVersion(3); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}

	got := GetValue(m.Version(3))
	if got != want {
		t.Errorf("expected %v but got %v", want, got)
	}
}

func TestJSONVersions_MultipleOps(t *testing.T) {
	m := NewJSONVersions()
	versions := map[int]JSONToken{
		1:  JSONNum(0),
		7:  NewJSONFuture(),
		3:  JSONBool(true),
		2:  NewJSONFuture(),
		10: JSONStr("value"),
		5:  NewJSONFuture(),
	}
	latest := 10
	prev := map[int]JSONToken{
		1:    nil,
		2:    versions[1],
		3:    versions[2],
		4:    versions[3],
		5:    versions[3],
		6:    versions[5],
		7:    versions[5],
		8:    versions[7],
		9:    versions[7],
		10:   versions[7],
		11:   versions[latest],
		1024: versions[latest],
	}

	for k, v := range versions {
		if err := m.AddVersion(v, k); err != nil {
			t.Errorf("AddVersion(): got unexpected error: %v", err)
		}
	}

	for i := 0; i <= latest; i++ {
		_, want := versions[i]
		got := m.ExistVersion(i)
		if got != want {
			t.Errorf("ExistVersion(%d): expected %v but got %v", i, want, got)
		}
	}

	if m.Value() != versions[latest] {
		t.Errorf("Value(): expected %v got %v", versions[latest], m.Value())
	}

	for k, v := range versions {
		got := m.Version(k)
		if got != v {
			t.Errorf("Version(%d): expected %v got %v", k, v, got)
		}
	}

	for k, v := range prev {
		got := m.PrevVersion(k)
		if got != v {
			t.Errorf("PrevVersion(%d): expected %v got %v", k, v, got)
		}
	}

	if err := m.SkipVersion(5); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.SkipVersion(7); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	got := GetValue(m.Version(7))
	want := versions[3]
	if got != want {
		t.Errorf("skip twice: expected %v got %v", want, got)
	}
}

func TestJSONVersions_Error(t *testing.T) {
	m := NewJSONVersions()
	if err := m.SkipVersion(0); err == nil {
		t.Error("skip a not exist version: expected an error but got nil")
	}
	if err := m.AddVersion(JSONNum(0), 0); err != nil {
		t.Errorf("got unexpected error: %v", err)
	}
	if err := m.AddVersion(NewJSONFuture(), 0); err == nil {
		t.Error("double add versions with the same vid: expected an error but got nil")
	}
	if err := m.AddVersion(JSONNum(0), -1); err == nil {
		t.Error("negative vid: expected an error but got nil")
	}

	m.Lock()
	if err := m.AddVersion(JSONNum(1), 1); err == nil {
		t.Errorf("call AddVersion after locking: expected an error but got nil")
	}
}
