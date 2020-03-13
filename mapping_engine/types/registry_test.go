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

package types

import (
	"encoding/json"
	"reflect"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

func nilProjector(arguments []jsonutil.JSONMetaNode, pctx *Context) (jsonutil.JSONToken, error) {
	return nil, nil
}

func mustParseContainer(json json.RawMessage, t *testing.T) jsonutil.JSONContainer {
	t.Helper()
	c := make(jsonutil.JSONContainer)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func mustParseArray(json json.RawMessage, t *testing.T) jsonutil.JSONArr {
	t.Helper()
	c := make(jsonutil.JSONArr, 0)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func TestRegisterProjector(t *testing.T) {
	reg := NewRegistry()

	tests := []struct {
		name, pName string
	}{
		{
			name:  "valid name",
			pName: "foo",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			if err := reg.RegisterProjector(test.pName, nilProjector); err != nil {
				t.Fatalf("RegisterProjector(%v) returned unexpected error %v", test.pName, err)
			}
			if _, ok := reg.registry[test.pName]; !ok {
				t.Fatalf("RegisterProjector(%v) succeeded but registry had no key %s", test.pName, test.pName)
			}
		})
	}
}

func TestIdentity(t *testing.T) {
	tests := []struct {
		name     string
		arg      jsonutil.JSONToken
		wantType reflect.Type
	}{
		{
			name:     "returns same object as given",
			arg:      mustParseContainer(json.RawMessage(`{"foo":"bar","baz":[1,2,3],"bok":{"mok":"dok"}}`), t),
			wantType: reflect.TypeOf(jsonutil.JSONContainer{}),
		},
		{
			name:     "returns same primitive as given",
			arg:      jsonutil.JSONStr("foo"),
			wantType: reflect.TypeOf(jsonutil.JSONStr("")),
		},
		{
			name:     "returns same array as given",
			arg:      mustParseArray(json.RawMessage(`["foo","bar", {"mok":"dok"}]`), t),
			wantType: reflect.TypeOf(jsonutil.JSONArr{}),
		},
		{
			name:     "returns nil if given nil",
			arg:      nil,
			wantType: reflect.TypeOf(nil),
		},
		{
			name:     "returns same empty array as given",
			arg:      mustParseArray(json.RawMessage(`[]`), t),
			wantType: reflect.TypeOf(jsonutil.JSONArr{}),
		},
		{
			name:     "returns same empty object as given",
			arg:      mustParseContainer(json.RawMessage(`{}`), t),
			wantType: reflect.TypeOf(jsonutil.JSONContainer{}),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			arg, err := jsonutil.TokenToNode(test.arg)
			if err != nil {
				t.Fatalf("failed to convert arg %v: %v", test.arg, err)
			}
			got, err := identity([]jsonutil.JSONMetaNode{arg}, nil)
			if err != nil {
				t.Fatalf("Identity(%v) returned unexpected error %v", test.arg, err)
			}
			if !cmp.Equal(got, test.arg) {
				t.Errorf("Identity(%v) = %v, want %v", test.arg, got, test.arg)
			}
			if reflect.TypeOf(got) != test.wantType {
				t.Errorf("Identity(%v) returned type %T, want %v", test.arg, got, test.wantType)
			}
		})
	}
}

func TestRegisterProjectorErrors(t *testing.T) {
	reg := NewRegistry()

	if err := reg.RegisterProjector("foo", nilProjector); err != nil {
		t.Fatalf("RegisterProjector('foo', Identity) returned unexpected error %v", err)
	}

	tests := []struct {
		name, pName string
	}{
		{
			name:  "existing name",
			pName: "foo",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			if err := reg.RegisterProjector(test.pName, nilProjector); err == nil {
				t.Errorf("RegisterProjector(%v) expected to error but didn't", test.pName)
			}
		})
	}
}

func TestCount(t *testing.T) {
	reg := NewRegistry()

	tests := []struct {
		name     string
		registry map[string]Projector
		want     int
	}{
		{
			name:     "empty registry",
			registry: map[string]Projector{},
			want:     0,
		},
		{
			name: "two item registry",
			registry: map[string]Projector{
				"a": nilProjector,
				"b": nilProjector,
			},
			want: 2,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			reg.registry = test.registry
			if c := reg.Count(); c != test.want {
				t.Fatalf("Count() => %d, want %d", c, test.want)
			}
		})
	}
}
