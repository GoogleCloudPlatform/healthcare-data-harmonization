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
	"fmt"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

// StackMap implements StackMapInterface, which acts like a layered stack of maps.
type StackMap struct {
	maps []map[string]*jsonutil.JSONToken
}

// NewStackMap instantiates a new stack map.
func NewStackMap() *StackMap {
	return &StackMap{
		maps: []map[string]*jsonutil.JSONToken{},
	}
}

// Push creates a new layer (empty map) in the stack.
func (s *StackMap) Push() {
	s.maps = append(s.maps, map[string]*jsonutil.JSONToken{})
}

// Pop removes the latest (last) map from the stack.
func (s *StackMap) Pop() (map[string]*jsonutil.JSONToken, error) {
	if err := s.allowedOp(); err != nil {
		return nil, err
	}

	l := len(s.maps)
	m := s.maps[l-1]
	s.maps = s.maps[:l-1]
	return m, nil
}

// Set stores the key-value pair specified as parameters to the latest map.
func (s *StackMap) Set(key string, value *jsonutil.JSONToken) error {
	if err := s.allowedOp(); err != nil {
		return err
	}

	m := s.maps[len(s.maps)-1]
	m[key] = value
	return nil
}

// Get reads the value specified by the key, from the latest map going forward.
func (s *StackMap) Get(key string) (*jsonutil.JSONToken, error) {
	if err := s.allowedOp(); err != nil {
		return nil, err
	}

	return s.maps[len(s.maps)-1][key], nil
}

// allowedOp checks that there is at least one map exists to operate on.
func (s *StackMap) allowedOp() error {
	if len(s.maps) == 0 {
		return fmt.Errorf("operation is not allowed on an empty stack map")
	}
	return nil
}

// Empty returns true if the StackMap contains no layers or its sole layer is empty.
func (s *StackMap) Empty() bool {
	return len(s.maps) == 0 || (len(s.maps) == 1 && len(s.maps[0]) == 0)
}

// String returns a pretty printed, human readable string representation of this StackMap.
func (s *StackMap) String() string {
	sb := strings.Builder{}

	sb.WriteString("-- Top of Stack --\n")

	for i := len(s.maps) - 1; i >= 0; i-- {
		sb.WriteString(prettyPrintTokenMap(s.maps[i]))
		sb.WriteString("-- -- -- -- --\n")
	}

	return sb.String()
}

func prettyPrintTokenMap(mp map[string]*jsonutil.JSONToken) string {
	sb := strings.Builder{}

	for k, v := range mp {
		sb.WriteString(fmt.Sprintf("%s: %v\n", k, *v))
	}

	return sb.String()
}
