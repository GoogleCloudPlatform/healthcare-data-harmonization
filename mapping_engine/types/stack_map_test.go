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
	"reflect"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

var (
	key   = "patient.1.address[]"
	value = jsonutil.JSONToken(jsonutil.JSONArr([]jsonutil.JSONToken{jsonutil.JSONNum(1.1)}))
)

func TestPush(t *testing.T) {
	s := NewStackMap()
	s.Push()
	if len(s.maps) != 1 {
		t.Fatalf("maps: %v, want: %v", len(s.maps), 1)
	}
	want := map[string]jsonutil.JSONToken{}
	if reflect.DeepEqual(s.maps[0], want) {
		t.Errorf("map: %v, want: %v", s.maps[0], want)
	}
}

func TestPop(t *testing.T) {
	s := NewStackMap()
	s.Push()
	s.Push()
	s.Set(key, &value)
	m, err := s.Pop()
	if err != nil {
		t.Fatalf("unable to pop, err: %v", err)
	}
	want := map[string]*jsonutil.JSONToken{key: &value}
	if !reflect.DeepEqual(m, want) {
		t.Fatalf("map: %v, want: %v", m, want)
	}
	if len(s.maps) != 1 {
		t.Errorf("number of maps is %v, want: %v", len(s.maps), 0)
	}
}

func TestPop_Error(t *testing.T) {
	s := NewStackMap()
	_, err := s.Pop()
	if err == nil {
		t.Fatalf("pop got nil, but expected error")
	}
}

func TestSet(t *testing.T) {
	s := NewStackMap()
	s.Push()
	err := s.Set(key, &value)
	if err != nil {
		t.Fatalf("unable to set (%v, %v), err: %v", key, value, err)
	}
	if len(s.maps) != 1 {
		t.Fatalf("maps: %v, want: %v", len(s.maps), 1)
	}
	want := map[string]*jsonutil.JSONToken{key: &value}
	if !reflect.DeepEqual(s.maps[0], want) {
		t.Fatalf("map: %v, want: %v", s.maps[0], want)
	}
}

func TestSet_Error(t *testing.T) {
	s := NewStackMap()
	err := s.Set(key, &value)
	if err == nil {
		t.Fatalf("set (%v, %v) got nil, but expected error", key, &value)
	}
}

func TestGet(t *testing.T) {
	s := NewStackMap()
	s.Push()
	s.Set(key, &value)

	if len(s.maps) != 1 {
		t.Fatalf("maps: %v, want: %v", len(s.maps), 1)
	}

	m, err := s.Get(key)
	if err != nil {
		t.Fatalf("get (%v) caused error %v", key, err)
	}
	if !reflect.DeepEqual(m, &value) {
		t.Errorf("got: %v, want: %v", m, &value)
	}
}

func TestGet_DoesNotGetAboveCurrentFrame(t *testing.T) {
	s := NewStackMap()
	s.Push()
	s.Set(key, &value)
	s.Push()

	if len(s.maps) != 2 {
		t.Fatalf("maps: %v, want: %v", len(s.maps), 2)
	}

	m, err := s.Get(key)
	if err != nil {
		t.Fatalf("get (%v) caused error %v", key, err)
	}
	if m != nil {
		t.Errorf("got: %v, want: nil", m)
	}
}

func TestGet_Error(t *testing.T) {
	s := NewStackMap()
	_, err := s.Get(key)
	if err == nil {
		t.Fatalf("get (%v) got nil, but expected error", key)
	}
}

func TestPushSetGetPop(t *testing.T) {
	s := NewStackMap()
	s.Push()
	v := jsonutil.JSONToken(jsonutil.JSONNum(1))
	s.Set(key, &v)
	s.Push()
	s.Set(key, &value)
	if len(s.maps) != 2 {
		t.Fatalf("maps: %v, want: %v", len(s.maps), 2)
	}
	m, err := s.Get(key)
	if err != nil {
		t.Fatalf("get (%v) caused error %v", key, err)
	}
	if !reflect.DeepEqual(m, &value) {
		t.Errorf("got: %v, want: %v", *m, value)
	}
	want := map[string]*jsonutil.JSONToken{key: &value}
	got, err := s.Pop()
	if err != nil {
		t.Fatalf("pop failed with error: %v", err)
	}
	if reflect.DeepEqual(got, &want) {
		t.Errorf("map: %v, want: %v", got, &want)
	}
	if len(s.maps) != 1 {
		t.Errorf("number of maps is %v, want: %v", len(s.maps), 1)
	}
}
