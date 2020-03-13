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
	"fmt"
	"sync"
)

// JSONFuture represents the result of an asynchronous JSONToken, used in
// parallelized mapping engine.
// Methods are provided to get and set its value. The value can only be
// retrieved using method Value when the value has been set by method Set,
// blocking if necessary until it is ready.
// JSONFuture allows multiple Value calls to block the callers, and broadcasts
// the value to unblock all the callers at the same time once the value is ready.
type JSONFuture struct {
	channels []chan JSONToken
	value    JSONToken
	mux      sync.Mutex
	ready    bool
}

func (*JSONFuture) jsonObject() {}

// Value gets actual JSONToken value if it is ready, or is blocked and read from
// a channel.
func (f *JSONFuture) Value() JSONToken {
	f.mux.Lock()
	if f.ready {
		f.mux.Unlock()
		return f.value
	}

	ch := make(chan JSONToken, 1)
	f.channels = append(f.channels, ch)
	f.mux.Unlock()
	return <-ch
}

// Set sets a JSONToken value in JSONFuture to unblock all Value callers and
// close the channels.
// Caller who calls Set will not be blocked even if there is no Value calls.
// Note that once a JSONFuture is set, it cannot be set again.
func (f *JSONFuture) Set(j JSONToken) error {
	f.mux.Lock()
	if f.ready {
		f.mux.Unlock()
		return fmt.Errorf("double set a JSONFuture %p: previous %v and now %v", f, f.value, j)
	}
	f.value = j
	f.ready = true
	for _, ch := range f.channels {
		ch <- j
		close(ch)
	}
	f.mux.Unlock()
	return nil
}

// NewJSONFuture instantiates a new JSONFuture.
func NewJSONFuture() *JSONFuture {
	return &JSONFuture{
		channels: make([]chan JSONToken, 0),
		value:    nil,
		ready:    false,
	}
}

// JSONVersions represents a persistent data structure for preserving
// all versions of JSONToken on one field or var in a projector, used
// in parallelized mapping engine. It is used to resolve value dependencies
// in overwrite cases.
// TODO: Users must sort field mappings in dependency order
// to indicate each output value depends on which input.
// In a single projector, a JSONVersions will be created for every output of
// the field mappings, and in a JSONVersions, one version may depend on the
// other versions.
// For example, we have a projector
// projector: {
//   name: "overwrite"
//   mapping: {
//     value_source:{
//       from_source: "a"
//     }
//     target_field: "x"
//   }
//   mapping: {
//     value_source: {
//       from_destination: "x"
//     }
//     target_field: "y"
//   }
//   mapping: {
//     value_source: {
//       from_source: "b"
//     }
//     target_field: "x!"
//   }
// }
// Field "x" has two versions as "a" and "b", and field "y" depends on
// the first version of "x". In this case, we need to create a JSONVersions
// for "x" and add 2 versions as
//   m := NewJSONVersions()
//   m.AddVersion(JSONStr("a"), 1)
//   m.AddVersion(JSOnStr("b"), 3)
// Each version in a JSONVersions is identified by an integer ID, and we
// use the index (1-based) of mapping in projector as its verison ID and
// use 0 to represent the initial value before any mapping.
// It is not necessary to lock the map "versions" in JSONVersions, since
// we guarantee that the method AddVersion will be only called sequentially,
// but we still use an indicator "locked" to make sure the map is read-only
// after the method Lock is called.
type JSONVersions struct {
	versions map[int]JSONToken
	latest   int
	locked   bool
}

func (*JSONVersions) jsonObject() {}

// Value gets the latest version of a JSONVersions. If no version exists, return nil.
func (m *JSONVersions) Value() JSONToken {
	return m.Version(m.latest)
}

// Version gets an version by vid. If version does not exist, return nil.
func (m *JSONVersions) Version(vid int) JSONToken {
	if v, ok := m.versions[vid]; ok {
		return v
	}
	return nil
}

// Lock locks the JSONVersions to prevent the method AddVersion being called again.
// It makes the JSONVersions read-only.
func (m *JSONVersions) Lock() {
	m.locked = true
}

// PrevVersion gets a previous version than vid one.
// If no previous is found, return nil, and if vid is greater than the latest,
// return the latest one.
func (m *JSONVersions) PrevVersion(vid int) JSONToken {
	// TODO: Can use a data structure to optimize this function.
	if vid > m.latest {
		return m.Version(m.latest)
	}
	for prev := vid - 1; prev >= 0; prev-- {
		if u, ok := m.versions[prev]; ok {
			return u
		}
	}
	return nil
}

// ExistVersion checks if version with vid exists.
func (m *JSONVersions) ExistVersion(vid int) bool {
	_, ok := m.versions[vid]
	return ok
}

// AddVersion adds an version vid with value t.
// Returns an error if either vid is negative or version with vid exists already
// in the JSONVersions.
func (m *JSONVersions) AddVersion(t JSONToken, vid int) error {
	if m.locked {
		return fmt.Errorf("JSONVersions has been locked and AddVersion cannot be called")
	}
	if vid < 0 {
		return fmt.Errorf("given vid %d cannot be negative", vid)
	}
	if m.ExistVersion(vid) {
		return fmt.Errorf("add a version (vid %d) which already exists", vid)
	}
	m.versions[vid] = t
	if vid > m.latest {
		m.latest = vid
	}
	return nil
}

// SkipVersion skips a version by setting the value of version vid to the value of
// its previous version.
// The skipped version must be a JSONFuture, and caller will be blocked until it
// gets a value of its previous version. So it is possible to have a cascading
// skipped version chain.
// Returns an error if either version vid does not exist or it is not a JSONFuture.

// For example,
//   m := NewJSONVersions()
//   m.AddVersion(JSONNum(0), 0)
//   m.AddVersion(NewJSONFuture, 1)
//   m.SkipVersion(1)
//   m.Value().Get() => JSONNum(0) // not blocked

// SkipVersion is used for parallelizing the mapping engine.
// Suppose we have a projector
//   def p(o) {
//     var a: o.a;
//     var a (if false): o.b;
//     c: var a;
//   }
// Initially, we will create a JSONVersions "m" for variable "a", and add two
// versions to it:
//   m.AddVersion(NewJSONFuture, 1)
//   m.AddVersion(NewJSONFuture, 2)
// and the value of output field "c" depends on the latest value of "a".
// When we evaluate these mappings in parallel, input value "o.a" will fill-in the
// JSONFuture in version 1, and the field "c" would be blocked because it depends
// on version 2 of "o.a".
// However, the second mapping has false condition, i.e. the value does not exist
// in practice, so we need call m.SkipVersion(2) to copy value from version 1 and
// set into version 2, which unblocks the mapping for field "c".
func (m *JSONVersions) SkipVersion(vid int) error {
	if u, ok := m.versions[vid]; ok {
		if f, ok := u.(*JSONFuture); ok {
			f.Set(GetValue(m.PrevVersion(vid)))
			return nil
		}
		return fmt.Errorf("skip a version (vid %d), expected a JSONFuture but got %+v", vid, u)
	}
	return fmt.Errorf("skip a version (vid %d) which does not exist", vid)
}

// NewJSONVersions instantiates a new JSONVersions.
func NewJSONVersions() *JSONVersions {
	return &JSONVersions{
		versions: make(map[int]JSONToken),
		latest:   -1,
		locked:   false,
	}
}
