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

// Package registerall contains registerAll function that registers builtin function into projectors
package registerall

import (
	"fmt"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
)

// RegisterAll registers all built-ins declared in the built-ins maps. This will wrap the functions
// into types.Projectors using projector.FromFunction.
func RegisterAll(r *types.Registry) error {
	for name, fn := range builtins.BuiltinFunctions {
		proj, err := projector.FromFunction(fn, name)
		if err != nil {
			return fmt.Errorf("failed to create projector from built-in %s: %v", name, err)
		}

		if err = r.RegisterProjector(name, proj); err != nil {
			return fmt.Errorf("failed to register built-in %s: %v", name, err)
		}
	}

	return nil
}
