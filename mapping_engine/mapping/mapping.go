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

// Package mapping contains methods and mechanisms for executing a mapping config.
package mapping

import (
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// ProcessMappings evaluates mappings in a projector or root mappings. If parallel is set to be
// true, all mappings will be evaluated and processed in parallel.
func ProcessMappings(maps []*mappb.FieldMapping, projName string, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context, parallel bool) error {
	// TODO: Remove flag "parallel" once parallelized mapping engine is stable.
	if parallel {
		// TODO: Add parallelized ProcessMapping.
		return fmt.Errorf("parallelization is not implemented yet")
	}
	for i, m := range maps {
		if err := ProcessMappingSequential(m, args, output, pctx); err != nil {
			return fmt.Errorf("error processing field mapping %d in projector %s: %v", i, projName, err)
		}
	}
	return nil
}

// ProcessMappingSequential evaluates and assigns a single field mapping sequentially. This method
// will check the condition as well, returning false if the condition check successfully evaluated
// to false.
// The JSONToken returned is the resulting value of this mapping (including a top level object if
// that was the target).
func ProcessMappingSequential(m *mappb.FieldMapping, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context) error {
	pctx.Trace.StartMapping(m)

	if m.Condition != nil {
		pctx.Trace.StartConditionCheck()

		cond, err := EvaluateValueSource(m.Condition, args, *output, pctx)
		if err != nil {
			return fmt.Errorf("error evaluating condition: %v", err)
		}

		cb, ok := cond.(jsonutil.JSONBool)
		if !ok {
			return fmt.Errorf("condition returned unexpected type %T (expected JSONBool)", cond)
		}

		pctx.Trace.EndConditionCheck(bool(cb))

		if !cb {
			pctx.Trace.EndMapping(m, nil)
			return nil
		}
	}

	src, err := EvaluateValueSource(m.ValueSource, args, *output, pctx)
	if err != nil {
		return fmt.Errorf("error evaluating value source: %v", err)
	}

	src = postProcessValue(src)
	// Skip nil-check if target is var, since we still want to define the var even assign nil to it.
	// Once the var is used, and written to something else that isn't a var, that's when nil-check
	// will happen on this value.
	if _, isVar := m.Target.(*mappb.FieldMapping_TargetLocalVar); !isVar && isNil(src) {
		pctx.Trace.EndMapping(m, nil)
		return nil
	}

	// No target field defaults to self.
	if m.Target == nil {
		m.Target = &mappb.FieldMapping_TargetField{TargetField: ""}
	}

	switch t := m.Target.(type) {
	case *mappb.FieldMapping_TargetField:
		if err := writeField(src, t.TargetField, output, false); err != nil {
			return fmt.Errorf("could not write field %q: %v", t.TargetField, err)
		}
		pctx.Trace.EndMapping(m, src)
		return nil
	case *mappb.FieldMapping_TargetLocalVar:
		cval, name, err := getVar(t.TargetLocalVar, *pctx)
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

		if err := writeField(src, field, &cval, forceOverwrite); err != nil {
			return err
		}

		err = pctx.Variables.Set(name, &cval)
		if err != nil {
			return fmt.Errorf("error setting var %q: %v", t.TargetLocalVar, err)
		}

		pctx.Trace.EndMapping(m, cval)
		return nil
	case *mappb.FieldMapping_TargetObject:
		addObject(src, t.TargetObject, pctx)
		pctx.Trace.EndMapping(m, src)
		return nil
	case *mappb.FieldMapping_TargetRootField:
		if err := writeField(src, t.TargetRootField, &pctx.Output, false); err != nil {
			return fmt.Errorf("could not write root field %q: %v", t.TargetRootField, err)
		}
		pctx.Trace.EndMapping(m, src)
		return nil
	default:
		return fmt.Errorf("unknown target %T", m.Target)
	}
}

func isNil(src jsonutil.JSONToken) bool {
	switch t := src.(type) {
	case jsonutil.JSONStr:
		return len(t) == 0
	case jsonutil.JSONArr:
		return len(t) == 0
	case jsonutil.JSONContainer:
		return len(t) == 0
	case nil:
		return true
	}

	return false
}

func postProcessValue(value jsonutil.JSONToken) jsonutil.JSONToken {
	switch t := value.(type) {
	case jsonutil.JSONStr:
		// Only trim if the string is more than just whitespace.
		if trimmed := strings.TrimSpace(string(t)); len(trimmed) > 0 {
			return jsonutil.JSONStr(trimmed)
		}
	}

	return value
}

func readField(src jsonutil.JSONToken, field string) (jsonutil.JSONToken, error) {
	return jsonutil.GetField(src, strings.TrimSuffix(field, "[]"))
}

func writeField(src jsonutil.JSONToken, field string, dest *jsonutil.JSONToken, forceOverwrite bool) error {
	return jsonutil.SetField(src, strings.TrimSuffix(field, "!"), dest, forceOverwrite || strings.HasSuffix(field, "!"))
}

func addObject(src jsonutil.JSONToken, targetObject string, pctx *types.Context) {
	// Prevent nested arrays showing up in top level objects. There is no real use
	// case where arrays should show up in the top level objects list. We
	// especially want to do this here (as opposed to later), to avoid having a
	// situation where the list is in a bad state and something tries to read from
	// it during mapping.
	var objs jsonutil.JSONArr
	if sa, ok := src.(jsonutil.JSONArr); ok {
		objs = sa
		for _, obj := range objs {
			addObject(obj, targetObject, pctx)
		}
	} else {
		pctx.TopLevelObjects[targetObject] = append(pctx.TopLevelObjects[targetObject], src)
	}
}

// EvaluateValueSource "interprets" a single value source. The source is converted into a JSONToken
// representation of its value (or an error).
func EvaluateValueSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context) (jsonutil.JSONToken, error) {
	if vs == nil {
		return nil, errors.New("nil value source pointer")
	}

	nextArgs := make([]jsonutil.JSONMetaNode, 0, 1)
	var enumerateArg []bool

	if vs.GetSource() != nil {
		arg, err := evaluateValueSourceSource(vs, args, output, pctx)
		if err != nil {
			return nil, fmt.Errorf("error evaluating value_source argument: %v", err)
		}

		nextArgs = append(nextArgs, arg)

		for _, s := range vs.AdditionalArg {
			arg, err := EvaluateValueSource(s, args, output, pctx)
			if err != nil {
				return nil, fmt.Errorf("error evaluating value_source argument: %v", err)
			}

			marg, err := jsonutil.TokenToNode(arg)
			if err != nil {
				return nil, fmt.Errorf("error wrapping value_source argument: %v", err)
			}

			nextArgs = append(nextArgs, marg)

			// Check if we need to enumerate each additional arg (based on whether it/it's projector is enumerated)
			enumerateArg = append(enumerateArg, (isArray(s) && s.GetProjector() == "") || isSelectorArray(s.GetProjector()))
		}
	}
	proj, err := pctx.Registry.FindProjector(strings.TrimSuffix(vs.Projector, "[]"))
	if err != nil {
		return nil, fmt.Errorf("error finding projector: %v", err)
	}

	if isArray(vs) {
		var arr jsonutil.JSONMetaArrayNode
		var ok bool
		if len(nextArgs) == 0 {
			return nil, errors.New("source was enumerated (ended with []) but source itself did not exist (?)")
		}
		if nextArgs[0] == nil {
			return nil, nil
		}
		if arr, ok = nextArgs[0].(jsonutil.JSONMetaArrayNode); !ok {
			// TODO: Make it an array of one item?
			// TODO: If container, return array of values?
			return nil, fmt.Errorf("source was enumerated (ended with []) but value was not an array (it was %T)", nextArgs[0])
		}

		// Zip the arguments together - enumerate any additional args that need it.
		zippedArgs, err := zip(arr, nextArgs[1:], enumerateArg)
		if err != nil {
			return nil, fmt.Errorf("error zipping args: %v", err)
		}

		projVals := make([]jsonutil.JSONToken, 0, len(arr.Items))
		for _, args := range zippedArgs {
			pv, err := proj(args, pctx)
			if err != nil {
				return nil, fmt.Errorf("error projecting array item: %v", err)
			}

			pv = postProcessValue(pv)
			if isNil(pv) {
				continue
			}
			projVals = append(projVals, pv)
		}

		return jsonutil.JSONArr(projVals), nil
	}

	return proj(nextArgs, pctx)
}

func isArray(vs *mappb.ValueSource) bool {
	var selector string
	switch s := vs.Source.(type) {
	case *mappb.ValueSource_FromSource:
		selector = s.FromSource
	case *mappb.ValueSource_FromDestination:
		selector = s.FromDestination
	case *mappb.ValueSource_FromLocalVar:
		selector = s.FromLocalVar
	case *mappb.ValueSource_FromInput:
		selector = s.FromInput.Field
	case *mappb.ValueSource_ProjectedValue:
		selector = s.ProjectedValue.Projector
	default:
		return false
	}

	return isSelectorArray(selector)
}

func isSelectorArray(selector string) bool {
	return strings.HasSuffix(selector, "[]")
}

// zip combines the given base array with the additionals to return a new array where each item
// is an array of the base item + all additionals. For example,
// zip([a, b, c], ["foo", "bar", "baz", [1, 2, 3, 4]], nil) returns
// [
// 		[a, "foo", "bar", "baz", [1, 2, 3, 4]],
// 		[b, "foo", "bar", "baz", [1, 2, 3, 4]],
// 		[c, "foo", "bar", "baz", [1, 2, 3, 4]],
// ]
// Also, zip can expand and enumerate the additionals if they are arrays, and
// the enumerateAdditional flag at that index is set to true. For example,
// zip([a, b, c], ["foo", "bar", "baz", [1, 2, 3]], [false, false, false,
//     true]) returns
// [
// 		[a, "foo", "bar", "baz", 1],
// 		[b, "foo", "bar", "baz", 2],
// 		[c, "foo", "bar", "baz", 3],
// ]
func zip(base jsonutil.JSONMetaArrayNode, additionals []jsonutil.JSONMetaNode, enumerateAdditional []bool) ([][]jsonutil.JSONMetaNode, error) {
	var res [][]jsonutil.JSONMetaNode

	if len(additionals) != len(enumerateAdditional) {
		return nil, fmt.Errorf("bug: number of arguments (%d) did not match number of enumeration flags (%d)", len(additionals), len(enumerateAdditional))
	}

	// Validate.
	if enumerateAdditional != nil {
		for i, a := range additionals {
			if !enumerateAdditional[i] {
				continue
			}

			arr, ok := a.(jsonutil.JSONMetaArrayNode)
			if !ok {
				return nil, fmt.Errorf("can't enumerate non-array (argument index %d)", i+1)
			}

			if len(arr.Items) != len(base.Items) {
				return nil, fmt.Errorf("can't zip/enumerate arrays of different sizes together (first array had %d items, arg index %d had %d)", len(base.Items), i+1, len(arr.Items))
			}
		}
	}

	// Zip.
	for i, v := range base.Items {
		z := []jsonutil.JSONMetaNode{v}
		for j, a := range additionals {
			if enumerateAdditional != nil && enumerateAdditional[j] {
				arr := additionals[j].(jsonutil.JSONMetaArrayNode)
				z = append(z, arr.Items[i])
			} else {
				z = append(z, a)
			}
		}
		res = append(res, z)
	}

	return res, nil
}

// evaluateValueSourceSource triages and extracts the specific value of a ValueSource.Source oneof
// (which can be one of various constant types, or a reference to an input field, variable, or
// output field).
func evaluateValueSourceSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context) (jsonutil.JSONMetaNode, error) {

	var token jsonutil.JSONToken
	var metaNode jsonutil.JSONMetaNode
	var err error
	var source string
	switch s := vs.Source.(type) {

	// Constants:
	case *mappb.ValueSource_ConstString:
		metaNode, source = jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONStr(s.ConstString)}, "const string"
	case *mappb.ValueSource_ConstInt:
		metaNode, source = jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONNum(s.ConstInt)}, "const int"
	case *mappb.ValueSource_ConstFloat:
		metaNode, source = jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONNum(s.ConstFloat)}, "const float"
	case *mappb.ValueSource_ConstBool:
		metaNode, source = jsonutil.JSONMetaPrimitiveNode{Value: jsonutil.JSONBool(s.ConstBool)}, "const bool"

	// More complicated things:
	case *mappb.ValueSource_FromSource:
		source = fmt.Sprintf("source %q", s.FromSource)
		as, asErr := fromSourceToArgSource(s, args)
		if asErr != nil {
			return nil, asErr
		}
		metaNode, err = EvaluateArgSource(as, args, pctx)
	case *mappb.ValueSource_FromDestination:
		source = fmt.Sprintf("destination %q", s.FromDestination)
		if token, err = EvaluateFromDestination(s, output); err != nil {
			return nil, err
		}
		metaNode, err = jsonutil.TokenToNode(token)
	case *mappb.ValueSource_FromLocalVar:
		source = fmt.Sprintf("var %q", s.FromLocalVar)
		if token, err = EvaluateFromVar(s, *pctx); err != nil {
			return nil, err
		}
		metaNode, err = jsonutil.TokenToNode(token)
	case *mappb.ValueSource_ProjectedValue:
		source = "projected value"
		if token, err = EvaluateValueSource(s.ProjectedValue, args, output, pctx); err != nil {
			return nil, err
		}
		metaNode, err = jsonutil.TokenToNode(token)
	// TODO: token Key = GUID(); Parent = common ancestor of all args
	// No need to mutate parent though.
	case *mappb.ValueSource_FromArg:
		if s.FromArg < 0 || int(s.FromArg) > len(args) {
			return nil, fmt.Errorf("from_arg is out of range. Requested arg %d but projector only got %d", s.FromArg, len(args))
		}

		source = fmt.Sprintf("from arg %d", s.FromArg)
		if s.FromArg == 0 {
			source += " (all args)"
			metaNode = jsonutil.JSONMetaArrayNode{Items: args}
		} else {
			metaNode = args[s.FromArg-1]
		}
	case *mappb.ValueSource_FromInput:
		metaNode, err = EvaluateArgSource(s.FromInput, args, pctx)
		source = fmt.Sprintf("input arg %d field %q", s.FromInput.Arg, s.FromInput.Field)
	default:
		return nil, fmt.Errorf("unknown value source %T", vs.Source)
	}

	if err != nil {
		return nil, err
	}

	pctx.Trace.ValueResolved(metaNode, source)

	return metaNode, nil
}

func fromSourceToArgSource(source *mappb.ValueSource_FromSource, args []jsonutil.JSONMetaNode) (*mappb.ValueSource_InputSource, error) {
	if len(args) == 0 {
		return &mappb.ValueSource_InputSource{
			Field: source.FromSource,
		}, nil
	}
	if len(args) == 1 {
		return &mappb.ValueSource_InputSource{
			// Default to first/only arg
			Arg:   1,
			Field: source.FromSource,
		}, nil
	}

	segs, err := jsonutil.SegmentPath(source.FromSource)
	if err != nil {
		return nil, fmt.Errorf("error parsing source %q: %v", source.FromSource, err)
	}

	if len(segs) == 0 {
		return nil, fmt.Errorf("error parsing source - was empty but projector had %d args (so at least an argument index is required)", len(args))
	}

	argIdx, err := strconv.Atoi(segs[0])
	if err != nil {
		return &mappb.ValueSource_InputSource{
			Field: source.FromSource,
		}, nil
	}

	return &mappb.ValueSource_InputSource{
		Arg:   int32(argIdx),
		Field: strings.ReplaceAll(strings.Join(segs[1:], "."), ".[", "["),
	}, nil
}

// EvaluateArgSource extracts a specified value from the arguments (or their subfields) or the
// context. Given a FromSource pb message, the function will check the args (which should be passed
// down from whatever projector this FromSource is used from), and/or the context, deciding on its
// own whether the former or latter is appropriate based on the content of FromSource.
func EvaluateArgSource(vs *mappb.ValueSource_InputSource, args []jsonutil.JSONMetaNode, pctx *types.Context) (jsonutil.JSONMetaNode, error) {
	// We need to find the argument (or subfield) or pctx referred to by this source.

	segs, err := jsonutil.SegmentPath(vs.Field)
	if err != nil {
		return nil, fmt.Errorf("error parsing source %s: %v", vs.Field, err)
	}

	if len(segs) == 0 && vs.Arg == 0 {
		return nil, errors.New("empty arg_source - needs to either have a valid argument index, a valid JSON path [without argument index refers to a input context value], or both")
	}

	var targetObj jsonutil.JSONMetaNode

	if vs.Arg < 0 || int(vs.Arg) > len(args) {
		return nil, fmt.Errorf("from_input.arg %d is out of range must be [0, %d]", vs.Arg, len(args))
	}

	// Remove array indicator ([]) suffix.
	if len(segs) > 0 && segs[len(segs)-1] == "[]" {
		segs = segs[0 : len(segs)-1]
	}

	if vs.Arg == 0 {
		targetObj, err = getValueFromContext(args, segs, pctx)
		if err != nil {
			return nil, fmt.Errorf("error getting value %q from input context: %v", vs.Field, err)
		}
	} else {
		targetObj, err = jsonutil.GetNodeFieldSegmented(args[vs.Arg-1], segs)
		if err != nil {
			return nil, fmt.Errorf("error getting value %q from argument %d (%v): %v", vs.Field, vs.Arg, args[vs.Arg-1], err)
		}
	}

	return targetObj, err
}

func getValueFromContext(args []jsonutil.JSONMetaNode, segs []string, pctx *types.Context) (jsonutil.JSONMetaNode, error) {
	var node jsonutil.JSONMetaNode
	var remSegs []string
	var err error

	if node, remSegs, err = ParentInfoFromArgs(args, segs); err != nil {
		return nil, err
	}

	pctx.Trace.ContextScan(node, segs, remSegs)

	return jsonutil.GetNodeFieldSegmented(node, remSegs)
}

// ParentInfoFromArgs finds the longest (prefix) subset of the given path segments that are
// present in the argument ancestors. Returns this ancestor and the remaining path relative to it.
func ParentInfoFromArgs(args []jsonutil.JSONMetaNode, segs []string) (jsonutil.JSONMetaNode, []string, error) {
	if len(args) == 0 {
		return nil, segs, nil
	}

	var bestCommonAnc jsonutil.JSONMetaNode
	var remSegs []string

	for _, arg := range args {
		if arg == nil {
			continue
		}
		root := arg
		for root.Parent() != nil {
			root = root.Parent()
		}

		// Make sure this isn't just some const string arg.
		if _, ok := root.(jsonutil.JSONMetaPrimitiveNode); ok {
			continue
		}

		p := arg.Path()
		argSegs, err := jsonutil.SegmentPath(p)
		if err != nil {
			return nil, segs, fmt.Errorf("argument %v does not have a valid path %s: %v", arg, p, err)
		}

		commonAnc, rem, err := getCommonAncestor(root, argSegs, segs)

		// The best common node is the one closest to the leaf we want.
		if bestCommonAnc == nil || len(rem) < len(remSegs) {
			bestCommonAnc = commonAnc
			remSegs = rem
		}
	}

	return bestCommonAnc, remSegs, nil
}

// getCommonAncestor finds the last common node of two paths in a JSON tree. It takes one fully
// qualified path (i.e. a path with all array indices filled in) and one partly qualified path (i.e.
// a path missing array indices, up to the point where it diverges from the fully qualified path).
// It then traverses the tree until it finds a place where the two paths diverge, and returns that
// last common node both paths shared, along with the suffix of the second path that diverged from
// the fully qualified path.
//
// e.x. Given a JSON object, called obj:
// {
//   "commonA": {
//     "arrayA": [
//       {
//         "commonB": {
//           "foo": 1337,
//           "bar": 9999
//         }
//       }
//     ],
//     "baz": {
//       "num": 1234
//     }
//   }
// }
//
// getCommonAncestor(obj,
// 										["commonA", "arrayA", "[0]", "commonB", "foo"],
// 										["commonA", "arrayA", "commonB", "bar"])
// returns { "foo": 1337, "bar": 9999 }, ["bar"]
//
// getCommonAncestor(obj,
// 							["commonA", "arrayA", "[0]", "commonB", "foo"],
// 							["commonA", "baz", "num"]
//             )
// returns { "arrayA": [...], "baz": { "num": 1234 } }, ["baz", "num"]
func getCommonAncestor(baseNode jsonutil.JSONMetaNode, fullyQualifiedPath []string, partlyQualifiedPath []string) (jsonutil.JSONMetaNode, []string, error) {
	commonAnc := baseNode
	var fullIndex, partialIndex int
	var err error

	for ; partialIndex < len(partlyQualifiedPath); partialIndex++ {
		// Advance cursor for full path while it has array indices that the other path doesn't.
		for ; fullIndex < len(fullyQualifiedPath) && jsonutil.IsIndex(fullyQualifiedPath[fullIndex]) && !jsonutil.IsIndex(partlyQualifiedPath[partialIndex]); fullIndex++ {
			commonAnc, err = jsonutil.GetNodeField(commonAnc, fullyQualifiedPath[fullIndex])
			if err != nil {
				return nil, partlyQualifiedPath, fmt.Errorf("error traversing to ancestor %v: %v", fullyQualifiedPath[:fullIndex+1], err)
			}
		}

		// Check if the two paths diverge yet.
		if fullIndex >= len(fullyQualifiedPath) || partlyQualifiedPath[partialIndex] != fullyQualifiedPath[fullIndex] {
			break
		}

		// Advance cursor for both paths (they are still in sync).
		commonAnc, err = jsonutil.GetNodeField(commonAnc, fullyQualifiedPath[fullIndex])
		if err != nil {
			return nil, partlyQualifiedPath, fmt.Errorf("error traversing to ancestor %v: %v", fullyQualifiedPath[:fullIndex+1], err)
		}
		fullIndex++
	}

	return commonAnc, partlyQualifiedPath[partialIndex:], nil
}

// EvaluateFromDestination returns the field from the output specified by the given path.
func EvaluateFromDestination(vs *mappb.ValueSource_FromDestination, output jsonutil.JSONToken) (jsonutil.JSONToken, error) {
	return readField(output, vs.FromDestination)
}

// EvaluateFromVar returns the context variable with the given name, or an error if it was
// not found.
func EvaluateFromVar(vs *mappb.ValueSource_FromLocalVar, pctx types.Context) (jsonutil.JSONToken, error) {
	v, name, err := getVar(vs.FromLocalVar, pctx)
	if err != nil {
		return nil, err
	}

	return readField(v, strings.TrimPrefix(strings.TrimPrefix(vs.FromLocalVar, name), "."))
}

type undefinedVarError struct {
	name string
}

func (e undefinedVarError) Error() string {
	return fmt.Sprintf("attempted to access undefined var %s", e.name)
}

// getVar returns the context variable with the given path and the variable name
// extracted from the path, or returns an undefinedVarError if the variable is
// not defined in the context.
func getVar(source string, pctx types.Context) (jsonutil.JSONToken, string, error) {
	src := strings.TrimSuffix(strings.TrimSuffix(source, "!"), "[]")
	path, err := jsonutil.SegmentPath(src)
	if err != nil {
		return nil, "", fmt.Errorf("error parsing var accessor %q: %v", src, err)
	}

	if len(path) == 0 {
		return nil, "", fmt.Errorf("no valid var accessor specified (%q is not valid)", src)
	}

	name := path[0]

	var v *jsonutil.JSONToken
	if v, err = pctx.Variables.Get(name); err != nil {
		return nil, name, fmt.Errorf("error accessing var %s: %v", src, err)
	}

	if v == nil {
		return nil, name, undefinedVarError{name: src}
	}

	return *v, name, nil
}
