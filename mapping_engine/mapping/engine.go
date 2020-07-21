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

// Package mapping contains methods and mechanisms for executing a mapping config.
package mapping

import (
	"errors"
	"fmt"
	"strconv"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/builtins" /* copybara-comment: builtins */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	errs "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// Engine defines an interface for mapping processing engine.
type Engine interface {
	ProcessMappings(maps []*mappb.FieldMapping, projName string, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context) error
	EvaluateValueSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context) (jsonutil.JSONMetaNode, error)
}

func checkCondition(conditionVs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output *jsonutil.JSONToken, pctx *types.Context, a jsonutil.JSONTokenAccessor) (bool, error) {
	cond, err := EvaluateValueSource(conditionVs, args, *output, pctx, a)
	if err != nil {
		return false, err
	}

	cp, ok := cond.(jsonutil.JSONMetaPrimitiveNode)
	if ok {
		cb, ok := cp.Value.(jsonutil.JSONBool)
		if ok {
			return bool(cb), nil
		}
	}

	t, err := jsonutil.NodeToToken(cond)
	if err != nil {
		return false, err
	}

	notNil, err := builtins.IsNotNil(t)
	return bool(notNil), err
}
func isNil(src jsonutil.JSONToken) bool {
	result, _ := builtins.IsNil(src)
	return bool(result)
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
func EvaluateValueSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context, a jsonutil.JSONTokenAccessor) (jsonutil.JSONMetaNode, error) {
	if vs == nil {
		return nil, errors.New("nil value source pointer")
	}

	nextArgs := make([]jsonutil.JSONMetaNode, 0, 1)
	var iterableIndicies []bool
	var anyArgsToIterate bool

	if vs.GetSource() != nil {
		arg, err := evaluateValueSourceSource(vs, args, output, pctx, a)
		if err != nil {
			return nil, err
		}

		nextArgs = append(nextArgs, arg)
		iterableIndicies = append(iterableIndicies, isArray(vs))
		anyArgsToIterate = isArray(vs)

		for _, s := range vs.AdditionalArg {
			arg, err := EvaluateValueSource(s, args, output, pctx, a)
			if err != nil {
				return nil, errs.Wrap(errs.NewProtoLocation(s, vs), err)
			}

			nextArgs = append(nextArgs, arg)

			// Check if we need to enumerate each additional arg (based on whether it/it's projector is enumerated)
			shouldIterate := (isArray(s) && s.GetProjector() == "") || isSelectorArray(s.GetProjector())
			iterableIndicies = append(iterableIndicies, shouldIterate)
			anyArgsToIterate = anyArgsToIterate || shouldIterate
		}
	}

	projName := strings.TrimSuffix(vs.Projector, "[]")
	proj, err := pctx.Registry.FindProjector(projName)
	if err != nil {
		return nil, fmt.Errorf("error finding projector: %v", err)
	}

	if anyArgsToIterate {
		if len(nextArgs) == 0 {
			return nil, errors.New("source was enumerated (ended with []) but source itself did not exist (?)")
		}
		if nextArgs[0] == nil {
			return nil, nil
		}

		// Zip the arguments together - enumerate any additional args that need it.
		zippedArgs, err := zip(nextArgs, iterableIndicies)
		if err != nil {
			return nil, fmt.Errorf("error zipping args: %v", err)
		}

		projVals := make([]jsonutil.JSONMetaNode, 0)
		for _, args := range zippedArgs {
			sources := []string{}
			for _, s := range args {
				if s != nil {
					sources = append(sources, s.ProvenanceString())
				} else {
					sources = append(sources, "null")
				}
			}

			pv, err := proj(args, pctx)
			if err != nil {
				return nil, errs.Wrap(errs.Locationf("Iterated arguments %q", strings.Join(sources, ", ")), err)
			}

			pv = postProcessValue(pv)
			if isNil(pv) {
				continue
			}

			pvm, err := jsonutil.TokenToNodeWithProvenance(pv, "", jsonutil.Provenance{
				Sources:  args,
				Function: projName,
			})
			if err != nil {
				return nil, err
			}

			projVals = append(projVals, pvm)
		}

		return jsonutil.JSONMetaArrayNode{
			Items: projVals,
			JSONMeta: jsonutil.NewJSONMeta("", jsonutil.Provenance{
				Sources:  nextArgs,
				Function: projName,
			}),
		}, nil
	}

	pv, err := proj(nextArgs, pctx)
	if err != nil {
		return nil, err
	}

	if projName == "" && len(nextArgs) == 1 && nextArgs[0] != nil {
		return jsonutil.TokenToNodeWithProvenance(pv, nextArgs[0].Key(), nextArgs[0].Provenance())
	}

	return jsonutil.TokenToNodeWithProvenance(pv, "", jsonutil.Provenance{
		Sources:  nextArgs,
		Function: projName,
	})
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

// zip allows synchronized iteration of some arrays along with non-arrays.
// Given some values, and an equal number of iterable flags; For any index where an iterable flag
// is true and the value is an array - the array is expanded. For example
// zip(["foo", "bar", "baz", [1, 2, 3, 4]], nil or [false, false, false, false]) returns
// [
// 		["foo", "bar", "baz", [1, 2, 3, 4]],
// ]
// zip(["foo", "bar", "baz", [1, 2, 3]], [false, false, false, true]) returns
// [
// 		["foo", "bar", "baz", 1],
// 		["foo", "bar", "baz", 2],
// 		["foo", "bar", "baz", 3],
// ]
// zip([["one", "two", "three"], [a, b], [1, 2, 3]], [true, false, true]) returns
// [
// 		["one",   [a, b], 1],
// 		["two",   [a, b], 2],
// 		["three", [a, b], 3],
// ]
func zip(values []jsonutil.JSONMetaNode, iterables []bool) ([][]jsonutil.JSONMetaNode, error) {
	if len(values) != len(iterables) {
		return nil, fmt.Errorf("bug: number of values (%d) did not match number of iterable flags (%d)", len(values), len(iterables))
	}

	// Validate that things flagged to be iterated are actually iterated.
	baseLen := -1
	basePath := ""
	if iterables != nil {
		for i, a := range values {
			if !iterables[i] {
				continue
			}

			arr, ok := a.(jsonutil.JSONMetaArrayNode)
			if !ok {
				return nil, fmt.Errorf("can't iterate non-array %q (it was the %s argument in the function call)", a.ProvenanceString(), errs.SuffixNumber(i+1))
			}

			if baseLen < 0 {
				baseLen = len(arr.Items)
				basePath = arr.ProvenanceString()
			}

			if len(arr.Items) != baseLen {
				return nil, fmt.Errorf("can't zip/iterate arrays of different sizes together (%q had %d items, but %q had %d)", basePath, baseLen, arr.ProvenanceString(), len(arr.Items))
			}
		}
	}

	if baseLen < 0 {
		return [][]jsonutil.JSONMetaNode{values}, nil
	}

	// Zip together iterables and non-iterables.
	var res [][]jsonutil.JSONMetaNode
	for i := 0; i < baseLen; i++ {
		z := []jsonutil.JSONMetaNode{}
		for j, a := range values {
			if iterables != nil && iterables[j] {
				arr := values[j].(jsonutil.JSONMetaArrayNode)
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
func evaluateValueSourceSource(vs *mappb.ValueSource, args []jsonutil.JSONMetaNode, output jsonutil.JSONToken, pctx *types.Context, a jsonutil.JSONTokenAccessor) (jsonutil.JSONMetaNode, error) {
	var metaNode jsonutil.JSONMetaNode
	var err error
	var location string
	switch s := vs.Source.(type) {

	// Constants:
	case *mappb.ValueSource_ConstString:
		location = "const string"
		metaNode, err = jsonutil.TokenToNodeWithProvenance(jsonutil.JSONStr(s.ConstString), fmt.Sprintf("%q", s.ConstString), jsonutil.Provenance{})
	case *mappb.ValueSource_ConstInt:
		location = "const int"
		metaNode, err = jsonutil.TokenToNodeWithProvenance(jsonutil.JSONNum(s.ConstInt), fmt.Sprintf("%d", s.ConstInt), jsonutil.Provenance{})
	case *mappb.ValueSource_ConstFloat:
		location = "const float"
		metaNode, err = jsonutil.TokenToNodeWithProvenance(jsonutil.JSONNum(s.ConstFloat), fmt.Sprintf("%f", s.ConstFloat), jsonutil.Provenance{})
	case *mappb.ValueSource_ConstBool:
		location = "const bool"
		metaNode, err = jsonutil.TokenToNodeWithProvenance(jsonutil.JSONBool(s.ConstBool), fmt.Sprintf("%v", s.ConstBool), jsonutil.Provenance{})

	// More complicated things:
	case *mappb.ValueSource_FromSource:
		location = fmt.Sprintf("From Source %q", s.FromSource)
		as, asErr := fromSourceToArgSource(s, args)
		if asErr != nil {
			return nil, asErr
		}
		metaNode, err = EvaluateArgSource(as, args, pctx)
	case *mappb.ValueSource_FromDestination:
		location = fmt.Sprintf("From Destination %q", s.FromDestination)
		token, lerr := EvaluateFromDestination(s, output, a)
		if lerr != nil {
			return nil, lerr
		}
		metaNode, err = jsonutil.TokenToNodeWithProvenance(token, fmt.Sprintf("%s's output field %s", pctx.Projector(), s.FromDestination), jsonutil.Provenance{})
	case *mappb.ValueSource_FromLocalVar:
		location = fmt.Sprintf("From Var %q", s.FromLocalVar)
		// TODO: Provenance support for vars.
		token, lerr := EvaluateFromVar(s, pctx, a)
		if lerr != nil {
			return nil, lerr
		}
		metaNode, err = jsonutil.TokenToNodeWithProvenance(token, fmt.Sprintf("%s's var %s", pctx.Projector(), s.FromLocalVar), jsonutil.Provenance{})
	case *mappb.ValueSource_ProjectedValue:
		if s.ProjectedValue.Projector != "" {
			location = "Argument for " + s.ProjectedValue.Projector
		} else {
			location = "Nested expression"
		}
		metaNode, err = EvaluateValueSource(s.ProjectedValue, args, output, pctx, a)
	// TODO: token Key = Gvid(); Parent = common ancestor of all args
	// No need to mutate parent though.
	case *mappb.ValueSource_FromArg:
		if s.FromArg < 0 || int(s.FromArg) > len(args) {
			return nil, fmt.Errorf("from_arg is out of range. Requested arg %d but projector only got %d", s.FromArg, len(args))
		}

		location = fmt.Sprintf("from arg %d", s.FromArg)
		if s.FromArg == 0 {
			location += " (all args)"
			metaNode = jsonutil.JSONMetaArrayNode{Items: args}
		} else {
			metaNode = args[s.FromArg-1]
		}
	case *mappb.ValueSource_FromInput:
		metaNode, err = EvaluateArgSource(s.FromInput, args, pctx)
		location = fmt.Sprintf("input arg %d field %q", s.FromInput.Arg, s.FromInput.Field)
	default:
		return nil, fmt.Errorf("unknown value source %T", vs.Source)
	}

	if err != nil {
		return nil, errs.Wrap(errs.Locationf(location), err)
	}

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
			return nil, fmt.Errorf("error getting field %q from %q: %v", vs.Field, args[vs.Arg-1].ProvenanceString(), err)
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
			return nil, segs, fmt.Errorf("argument %q does not have a valid path %s: %v", arg.ProvenanceString(), p, err)
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
func EvaluateFromDestination(vs *mappb.ValueSource_FromDestination, output jsonutil.JSONToken, a jsonutil.JSONTokenAccessor) (jsonutil.JSONToken, error) {
	return readField(output, vs.FromDestination, a)
}

// EvaluateFromVar returns the context variable with the given name, or an error if it was
// not found.
func EvaluateFromVar(vs *mappb.ValueSource_FromLocalVar, pctx *types.Context, a jsonutil.JSONTokenAccessor) (jsonutil.JSONToken, error) {
	v, name, err := getVar(vs.FromLocalVar, pctx)
	if err != nil {
		return nil, err
	}

	return readField(v, strings.TrimPrefix(strings.TrimPrefix(vs.FromLocalVar, name), "."), a)
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
func getVar(source string, pctx *types.Context) (jsonutil.JSONToken, string, error) {
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

func readField(src jsonutil.JSONToken, field string, a jsonutil.JSONTokenAccessor) (jsonutil.JSONToken, error) {
	return a.GetField(src, strings.TrimSuffix(field, "[]"))
}

func writeField(src jsonutil.JSONToken, field string, dest *jsonutil.JSONToken, forceOverwrite bool, a jsonutil.JSONTokenAccessor) error {
	return a.SetField(src, strings.TrimSuffix(field, "!"), dest, forceOverwrite || strings.HasSuffix(field, "!"))
}
