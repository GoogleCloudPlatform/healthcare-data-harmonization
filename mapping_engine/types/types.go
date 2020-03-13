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

// Package types contains shared type definitions.
package types

import (
	"fmt"
	"sort"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/optrace" /* copybara-comment: optrace */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

const (
	// MaxStackDepth contains the maximum number of nested projector calls that can be made without
	// returning.
	MaxStackDepth = 1000
)

// StackMapInterface defines a layered map, that allows a new map layer to be pushed, have values
// added to it, then be popped and have only those values disappear from the overall map. Get()
// calls will scan all maps starting at the top of the stack and return the first found values.
type StackMapInterface interface {
	Push()
	Pop() (map[string]*jsonutil.JSONToken, error)
	Set(key string, value *jsonutil.JSONToken) error
	Get(key string) (*jsonutil.JSONToken, error)
	String() string
	Empty() bool
}

// A Context stores data about variables and ancestors for a projector invocation.
type Context struct {
	Variables StackMapInterface

	Output          jsonutil.JSONToken
	TopLevelObjects map[string][]jsonutil.JSONToken
	Trace           *optrace.Trace
	Registry        *Registry

	// The depth of the projector stack
	stackDepth int

	// The number of times a projector is present in the current stack (useful for debugging).
	stackProjectorCounts map[string]int
}

func (c *Context) String() string {
	tlos := "<empty>"
	if len(c.TopLevelObjects) > 0 {
		tlos = "\n" + prettyPrintArrayMap(c.TopLevelObjects)
	}

	vars := "<empty>"
	if c.Variables != nil && !c.Variables.Empty() {
		vars = "\n" + c.Variables.String()
	}

	return fmt.Sprintf("{\n\t\tTop Level Objects: %s\n\t\tVariables: %s\n\t}", tlos, vars)
}

// PushProjectorToStack adds one count of the given projector name to the stack trace.
func (c *Context) PushProjectorToStack(name string) error {
	c.stackDepth++
	c.stackProjectorCounts[name]++

	if c.stackDepth > MaxStackDepth {
		return c.generateStackOverflowError()
	}

	return nil
}

// PopProjectorFromStack removes one count of the given projector name from the stack trace.
func (c *Context) PopProjectorFromStack(name string) {
	c.stackDepth--
	c.stackProjectorCounts[name]--
}

func (c *Context) generateStackOverflowError() error {
	type stackCount struct {
		projector string
		count     int
	}

	var stackCounts []stackCount

	for p, sc := range c.stackProjectorCounts {
		stackCounts = append(stackCounts, stackCount{p, sc})
	}

	sort.SliceStable(stackCounts, func(i, j int) bool {
		// Sort descending
		return !(stackCounts[i].count < stackCounts[j].count)
	})

	sb := strings.Builder{}
	for _, sc := range stackCounts {
		sb.WriteString(fmt.Sprintf("%s: %d\n", sc.projector, sc.count))
	}

	return fmt.Errorf("stack depth exceeded %d: too many recursive projector calls. Most frequently recurring projectors and how many times they appeared in the stack:\n%s", MaxStackDepth, sb.String())
}

// NewContext creates a new context with empty components initialized and ready to go.
func NewContext(registry *Registry) *Context {
	return &Context{
		TopLevelObjects:      map[string][]jsonutil.JSONToken{},
		Variables:            NewStackMap(),
		Trace:                &optrace.Trace{},
		Registry:             registry,
		stackProjectorCounts: map[string]int{},
	}
}

func prettyPrintArrayMap(mp map[string][]jsonutil.JSONToken) string {
	sb := strings.Builder{}

	for k, v := range mp {
		sb.WriteString(fmt.Sprintf("%s: %v\n", k, v))
	}

	return sb.String()
}

// Projector is a type alias for the function signature of a projector.
type Projector func(arguments []jsonutil.JSONMetaNode, pctx *Context) (jsonutil.JSONToken, error)
