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

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

// Registry stores projectors for a mapping config to use.
type Registry struct {
	registry map[string]Projector
}

// NewRegistry creates a new empty registry.
func NewRegistry() *Registry {
	return &Registry{
		registry: map[string]Projector{
			"": identity,
		},
	}
}

func identity(arguments []jsonutil.JSONMetaNode, _ *Context) (jsonutil.JSONToken, error) {
	if len(arguments) != 1 {
		return nil, fmt.Errorf("identity function called with multiple (%d) arguments, needs exactly 1", len(arguments))
	}

	return jsonutil.NodeToToken(arguments[0])
}

// RegisterProjector adds the given Projector to the registry.
func (r *Registry) RegisterProjector(name string, projector Projector) error {
	if err := r.validateProjectorName(name); err != nil {
		return err
	}

	r.registry[name] = projector

	return nil
}

func (r *Registry) validateProjectorName(name string) error {
	// TODO(b/120480646): Fail if UDF starts with _?
	if _, ok := r.registry[name]; ok {
		return fmt.Errorf("projector %s is already defined", name)
	}

	return nil
}

// FindProjector finds and returns a projector with the given name, or an error if no projector with
// that name exists.
func (r *Registry) FindProjector(name string) (Projector, error) {
	if proj, ok := r.registry[name]; ok {
		return proj, nil
	}
	return nil, fmt.Errorf("projector not found: %s", name)
}

// Count returns the number of projectors in the registry.
func (r *Registry) Count() int {
	return len(r.registry)
}
