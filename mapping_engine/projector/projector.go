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

// Package projector contains methods and mechanisms for creating and calling projectors.
package projector

import (
	"fmt"
	"math"
	"reflect"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// FromDef creates a projector from a proto definition. This will not register it.
func FromDef(definition mappb.ProjectorDefinition, parallel bool) types.Projector {
	return func(arguments []jsonutil.JSONMetaNode, pctx *types.Context) (jsonutil.JSONToken, error) {
		pctx.Variables.Push()
		pctx.Trace.StartProjectorCall(definition.Name, arguments, pctx.String())
		if err := pctx.PushProjectorToStack(definition.Name); err != nil {
			return nil, err
		}

		var merged jsonutil.JSONToken

		// TODO: Sort in dependency order
		if err := mapping.ProcessMappings(definition.Mapping, definition.Name, arguments, &merged, pctx, parallel); err != nil {
			return nil, err
		}

		pctx.Trace.EndProjectorCall(definition.Name, pctx.String(), merged)
		pctx.PopProjectorFromStack(definition.Name)

		if _, err := pctx.Variables.Pop(); err != nil {
			return nil, err
		}
		return merged, nil
	}
}

// FromFunction creates a projector from a given function. The function must have a return type of
// (JSONObject, error) and all arguments must be assignable to JSONObject. This will not register
// the projector.
func FromFunction(fn interface{}, name string) (types.Projector, error) {
	tokenType := reflect.TypeOf((*jsonutil.JSONToken)(nil)).Elem()

	f := reflect.ValueOf(fn)
	if f.Kind() != reflect.Func {
		return nil, fmt.Errorf("projector must be a function")
	}

	// Check args are our JSON types.
	ft := reflect.TypeOf(fn)
	for i := 0; i < ft.NumIn(); i++ {
		isObj := ft.In(i).AssignableTo(tokenType)
		isSliceOfObj := ft.In(i).Kind() == reflect.Slice && ft.In(i).Elem().AssignableTo(tokenType)
		if !isObj && !isSliceOfObj {
			return nil, fmt.Errorf("parameter %d is of type %v which is not supported", i, ft.In(i))
		}
	}

	// Check return type is what we expect.
	if ft.NumOut() != 2 {
		return nil, fmt.Errorf("incorrect return type, expected (jsonutil.JSONToken, error) got %d return values", ft.NumOut())
	}
	if !ft.Out(0).AssignableTo(tokenType) || !ft.Out(1).AssignableTo(reflect.TypeOf((*error)(nil)).Elem()) {
		return nil, fmt.Errorf("incorrect return type, expected (jsonutil.JSONToken, error) got (%v, %v)", ft.Out(0), ft.Out(1))
	}

	// Build wrapper closure.
	return func(metaArgs []jsonutil.JSONMetaNode, pctx *types.Context) (jsonutil.JSONToken, error) {
		pctx.Trace.StartProjectorCall(name, metaArgs, pctx.String())
		if err := pctx.PushProjectorToStack(name); err != nil {
			return nil, err
		}

		// Lose the meta.
		args := make([]jsonutil.JSONToken, len(metaArgs))
		for i, metaArg := range metaArgs {
			node, err := jsonutil.NodeToToken(metaArg)
			if err != nil {
				return nil, fmt.Errorf("error converting args: %v", err)
			}
			args[i] = node
		}

		if ft.IsVariadic() && len(args) < ft.NumIn()-1 {
			return nil, fmt.Errorf("expected at least %d parameters (could be more, function is variadic), got %d", ft.NumIn()-1, len(args))
		}
		if !ft.IsVariadic() && len(args) != ft.NumIn() {
			return nil, fmt.Errorf("expected %d parameters, got %d", ft.NumIn(), len(args))
		}
		argvs := make([]reflect.Value, 0, len(args))
		for i, arg := range args {
			if ft.IsVariadic() && i == ft.NumIn()-1 {
				a, err := extractVariadic(ft.In(i).Elem(), args[i:])
				if err != nil {
					return nil, fmt.Errorf("error extracting variadic argument %d: %v", i, err)
				}

				argvs = append(argvs, a...)
				break
			}
			if ft.In(i).Kind() == reflect.Slice {
				a, err := extractSlice(ft.In(i).Elem(), arg)
				if err != nil {
					return nil, fmt.Errorf("error extracting slice argument %d: %v", i, err)
				}
				argvs = append(argvs, a)
				continue
			}

			a, err := extractSimple(ft.In(i), arg)
			if err != nil {
				return nil, fmt.Errorf("error extracting argument %d: %v", i, err)
			}
			argvs = append(argvs, a)
		}

		result := f.Call(argvs)

		var r jsonutil.JSONToken
		var err error

		if ri := result[0].Interface(); ri != nil {
			r = ri.(jsonutil.JSONToken)
		}
		if ri := result[1].Interface(); ri != nil {
			err = ri.(error)
		}

		pctx.Trace.EndProjectorCall(name, "<same as start>", r)
		pctx.PopProjectorFromStack(name)

		return r, err
	}, nil
}

func extractVariadic(elemType reflect.Type, args []jsonutil.JSONToken) ([]reflect.Value, error) {
	if arr, ok := args[0].(jsonutil.JSONArr); len(args) == 1 && ok {
		args = arr
	}

	vals := make([]reflect.Value, 0, len(args))
	for _, arg := range args {
		v, err := extractSimple(elemType, arg)
		if err != nil {
			return nil, fmt.Errorf("variadic argument error: %v", err)
		}
		vals = append(vals, v)
	}

	return vals, nil
}

func extractSimple(elemType reflect.Type, arg jsonutil.JSONToken) (reflect.Value, error) {
	if arg != nil && !reflect.TypeOf(arg).AssignableTo(elemType) {
		return reflect.ValueOf(nil), fmt.Errorf("got %T, expected %v", arg, elemType)
	}
	if arg == nil {
		return reflect.New(elemType).Elem(), nil
	}

	return reflect.ValueOf(arg), nil
}

func extractSlice(elemType reflect.Type, arg jsonutil.JSONToken) (reflect.Value, error) {
	if arg == nil {
		return reflect.MakeSlice(reflect.SliceOf(elemType), 0, 0), nil
	}

	arrArg, ok := arg.(jsonutil.JSONArr)
	if !ok {
		return reflect.ValueOf(nil), fmt.Errorf("got %T but expected a JSONArr (whose elements are all %v)", arg, elemType)
	}

	ret := reflect.MakeSlice(reflect.SliceOf(elemType), len(arrArg), len(arrArg))
	for i, t := range arrArg {
		tv := reflect.ValueOf(t)
		if t == nil {
			tv = reflect.Zero(elemType)
		}
		if !IsZero(tv) && !tv.Type().AssignableTo(elemType) {
			return reflect.ValueOf(nil), fmt.Errorf("array element %d is a %T but must be a %v", i, t, elemType)
		}

		ret.Index(i).Set(tv)
	}

	return ret, nil
}

// IsZero reports whether v is the zero value for its type.
// It panics if the argument is invalid. This is to backfill for the lack of this method in older
// versions of Go.
func IsZero(v reflect.Value) bool {
	switch v.Kind() {
	case reflect.Bool:
		return !v.Bool()
	case reflect.Int, reflect.Int8, reflect.Int16, reflect.Int32, reflect.Int64:
		return v.Int() == 0
	case reflect.Uint, reflect.Uint8, reflect.Uint16, reflect.Uint32, reflect.Uint64, reflect.Uintptr:
		return v.Uint() == 0
	case reflect.Float32, reflect.Float64:
		return math.Float64bits(v.Float()) == 0
	case reflect.Complex64, reflect.Complex128:
		c := v.Complex()
		return math.Float64bits(real(c)) == 0 && math.Float64bits(imag(c)) == 0
	case reflect.Array:
		for i := 0; i < v.Len(); i++ {
			if !IsZero(v.Index(i)) {
				return false
			}
		}
		return true
	case reflect.Chan, reflect.Func, reflect.Interface, reflect.Map, reflect.Ptr, reflect.Slice, reflect.UnsafePointer:
		return v.IsNil()
	case reflect.String:
		return v.Len() == 0
	case reflect.Struct:
		for i := 0; i < v.NumField(); i++ {
			if !IsZero(v.Field(i)) {
				return false
			}
		}
		return true
	default:
		// This should never happens, but will act as a safeguard for
		// later, as a default value doesn't makes sense here.
		panic(&reflect.ValueError{"reflect.Value.IsZero", v.Kind()})
	}
}
