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

package transpiler

import (
	"fmt"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"bitbucket.org/creachadair/stringset" /* copybara-comment: stringset */

	mpb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// env represents a lexical variable/input/target binding environment. This is a component of a
// closure, though not the whole closure itself (closures do not currently exist in Whistle).
type env struct {
	name   string
	parent *env

	vars             stringset.Set
	targets          stringset.Set
	args             map[string]int
	inputsFromParent map[string]int

	mapping []*mpb.FieldMapping
}

// newEnv creates a new environment with the given name and registers/binds the given arguments.
func newEnv(name string, args ...string) *env {
	am := make(map[string]int)
	for i, a := range args {
		am[a] = i
	}

	return &env{
		name:             name,
		args:             am,
		inputsFromParent: make(map[string]int),
	}
}

// declareVar checks if the given var is already declared as an arg, and if not binds it in the
// environment.
func (n *env) declareVar(v string) error {
	if _, ok := n.args[v]; ok {
		return fmt.Errorf("variable %s has the same name as a function argument, it must be unique", v)
	}
	n.vars.Add(v)
	return nil
}

// declareTarget binds the given target in the environment.
func (n *env) declareTarget(target string) {
	n.targets.Add(target)
}

// readVar
func (n *env) readVar(input, field string) *mpb.ValueSource {
	if n.vars.Contains(input) {
		return &mpb.ValueSource{
			Source: &mpb.ValueSource_FromLocalVar{
				FromLocalVar: jsonutil.JoinPath(input, field),
			},
		}
	}

	return nil
}

// readInput checks the environment (vars, args, targets, parent environment in that order) for the
// given input, and produces a ValueSource to read it. Inputs read from the parent environment will
// be accounted for when the callsite is generated.
func (n *env) readInput(input, field string) *mpb.ValueSource {
	if v := n.readVar(input, field); v != nil {
		return v
	}

	if i, ok := n.args[input]; ok {
		return &mpb.ValueSource{
			Source: &mpb.ValueSource_FromInput{
				FromInput: &mpb.ValueSource_InputSource{
					Arg:   int32(i + 1),
					Field: field,
				},
			},
		}
	}

	if n.targets.Contains(input) {
		return &mpb.ValueSource{
			Source: &mpb.ValueSource_FromDestination{
				FromDestination: jsonutil.JoinPath(input, field),
			},
		}
	}

	if n.parent == nil {
		return nil
	}

	// Check to see if we've already read this input from the parent environment. If not, attempt to
	// read it.
	if _, ok := n.inputsFromParent[input]; !ok {
		if n.parent.readInput(input, field) != nil {
			n.inputsFromParent[input] = len(n.inputsFromParent)
		}
	}

	// At this point, if the parent environment had the input, we will have read it.
	if i, ok := n.inputsFromParent[input]; ok {
		return &mpb.ValueSource{
			Source: &mpb.ValueSource_FromInput{
				FromInput: &mpb.ValueSource_InputSource{
					Arg:   int32(len(n.args) + i + 1),
					Field: field,
				},
			},
		}
	}

	return nil
}

// generateProjector creates a ProjectorDefinition from the current environment.
func (n *env) generateProjector() *mpb.ProjectorDefinition {
	return &mpb.ProjectorDefinition{
		Name:    n.name,
		Mapping: n.mapping,
	}
}

// addMapping adds the given mapping to the environment and subsequently the projector definition
// registered by it.
func (n *env) addMapping(mapping *mpb.FieldMapping) {
	n.mapping = append(n.mapping, mapping)
}

func (n *env) newChild(name string, args []string) *env {
	child := &env{
		name:             name,
		parent:           n,
		args:             make(map[string]int),
		inputsFromParent: make(map[string]int),
	}

	for i, a := range args {
		child.args[a] = i
	}

	return child
}

// generateCallsite generates a callsite, intended to be placed in the parent environment's projector,
// for calling this environment's projector, passing along any inputs pulled from the parent env in
// addition to any arguments required by this environment's projector itself (i.e. those declared in
// newChild)
func (n *env) generateCallsite(args ...*mpb.ValueSource) (*mpb.ValueSource, error) {
	vs := &mpb.ValueSource{
		Projector: n.name,
	}

	if len(args) != len(n.args) {
		return nil, fmt.Errorf("wrong number of arguments - %s expects %d %v but got %d", n.name, len(n.args), n.args, len(args))
	}

	addArgs(vs, args...)

	inputsFromParents := make([]string, len(n.inputsFromParent))
	for a, i := range n.inputsFromParent {
		inputsFromParents[i] = a
	}

	for _, a := range inputsFromParents {
		addArgs(vs, n.parent.readInput(a, ""))
	}

	return vs, nil
}
