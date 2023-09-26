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

package registerall

import (
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
)

func TestRegisterAll(t *testing.T) {
	// This test also serves to assert that all builtins are actually valid projectors.
	reg := types.NewRegistry()
	err := RegisterAll(reg)
	if err != nil {
		t.Errorf("a builtin is invalid or failed to register: %v", err)
	}

	// +1 for identity function
	if r, b := reg.Count(), len(builtins.BuiltinFunctions); r != b+1 {
		t.Errorf("registry had a different number of functions (%d) than builtins map (%d)", r, b)
	}
}
