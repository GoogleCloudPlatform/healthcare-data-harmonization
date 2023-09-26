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

package mapping

import (
	"fmt"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	errs "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// Whistler is an implementation of Engine which processes mappings sequentially.
type Whistler struct {
	accessor jsonutil.DefaultAccessor
}

// NewWhistler creates a new Whistler instance as Engine.
func NewWhistler() Engine {
	return Whistler{accessor: jsonutil.DefaultAccessor{}}
}

// ProcessMappings evaluates mappings in a projector or root mappings.
func (w Whistler) ProcessMappings(maps []*mappb.FieldMapping, projName string, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context) error {
	mapType := "field"
	if projName == "" {
		mapType = "root"
	}

	for i, m := range maps {
		if err := w.EvaluateMapping(m, args, output, pctx); err != nil {
			return errs.Wrap(errs.NewProtoLocationf(m, "%s %s_mapping", errs.SuffixNumber(i+1), mapType), err)
		}
	}

	return nil
}

// EvaluateMapping evaluates and assigns a single field mapping sequentially. This method
// will check the condition as well, returning false if the condition check successfully evaluated
// to false.
// The JSONToken returned is the resulting value of this mapping (including a top level object if
// that was the target).
func (w Whistler) EvaluateMapping(m *mappb.FieldMapping, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context) error {
	if m.Condition != nil {
		var cb bool
		var err error
		if cb, err = checkCondition(m.Condition, args, output, pctx, w.accessor); err != nil {
			return errs.Wrap(errs.NewProtoLocation(m.Condition, m), err)
		}
		if !cb {
			return nil
		}
	}

	var src jsonutil.JSONMetaNode
	var err error
	if src, err = EvaluateValueSource(m.ValueSource, args, *output, pctx, w.accessor); err != nil {
		return errs.Wrap(errs.NewProtoLocation(m.ValueSource, m), err)
	}

	srcToken, err := jsonutil.NodeToToken(src)
	if err != nil {
		return err
	}
	srcToken = postProcessValue(srcToken)

	// Skip nil-check if target is var, since we still want to define the var even assign nil to it.
	// Once the var is used, and written to something else that isn't a var, that's when nil-check
	// will happen on this value.
	if _, isVar := m.Target.(*mappb.FieldMapping_TargetLocalVar); !isVar && isNil(srcToken) {
		return nil
	}

	// No target field defaults to self.
	if m.Target == nil {
		m.Target = &mappb.FieldMapping_TargetField{TargetField: ""}
	}

	iterateSrc := isSrcIteratable(m.ValueSource)

	switch t := m.Target.(type) {
	case *mappb.FieldMapping_TargetField:
		if err := writeField(srcToken, t.TargetField, output, false, iterateSrc, w.accessor); err != nil {
			return fmt.Errorf("could not write field %q: %v", t.TargetField, err)
		}
		return nil
	case *mappb.FieldMapping_TargetLocalVar:
		cval, name, err := getVar(t.TargetLocalVar, pctx)
		// Undefined var errors are safe to ignore here.
		if _, ok := err.(undefinedVarError); !ok && err != nil {
			return err
		}

		if cval != nil {
			cval = jsonutil.Deepcopy(cval)
		}

		field := strings.TrimPrefix(strings.TrimPrefix(t.TargetLocalVar, name), ".")
		// For variables, we allow to overwrite them without "!" except for array appending.
		forceOverwrite := !isSelectorArray(field)

		if err := writeField(srcToken, field, &cval, forceOverwrite, iterateSrc, w.accessor); err != nil {
			return err
		}

		err = pctx.Variables.Set(name, &cval)
		if err != nil {
			return fmt.Errorf("error setting var %q: %v", t.TargetLocalVar, err)
		}

		return nil
	case *mappb.FieldMapping_TargetObject:
		addObject(srcToken, t.TargetObject, pctx)
		return nil
	case *mappb.FieldMapping_TargetRootField:
		if err := writeField(srcToken, t.TargetRootField, pctx.Output, false, iterateSrc, w.accessor); err != nil {
			return fmt.Errorf("could not write root field %q: %v", t.TargetRootField, err)
		}
		return nil
	default:
		return fmt.Errorf("unknown target %T", m.Target)
	}
}

// EvaluateValueSource evaluates a single value source with a DefaultAccessor.
func (w Whistler) EvaluateValueSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context) (jsonutil.JSONMetaNode, error) {
	return EvaluateValueSource(vs, args, output, pctx, w.accessor)
}
