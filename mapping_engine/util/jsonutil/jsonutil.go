// Copyright 2020 Google LLC
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

// Package jsonutil provides utilities for interacting with a JSON objects.
package jsonutil

import (
	"encoding/binary"
	"encoding/json"
	"fmt"
	"hash"
	"hash/fnv"
	"math"
	"sort"
	"strconv"
	"strings"
	"unicode"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

// MarshalJSON serializes the given token into JSON string.
func MarshalJSON(tkn JSONToken) string {
	switch t := tkn.(type) {
	case JSONNum:
		// Format is based on RFC 7159.
		return strconv.FormatFloat(float64(t), 'G', -1, 64)
	case JSONStr:
		return fmt.Sprintf("%q", t)
	case JSONBool:
		return fmt.Sprintf("%t", t)
	case nil:
		return "null"
	case JSONContainer:
		return t.String()
	case JSONArr:
		return t.String()
	}
	return fmt.Sprintf("%v", tkn)
}

// UnmarshalJSON determines the type of the RawMessage and unmarshals it into a JSONToken.
func UnmarshalJSON(in json.RawMessage) (JSONToken, error) {
	ins := strings.TrimSpace(string(in))

	if len(ins) == 0 {
		return nil, nil
	}

	switch {
	case ins[0] == '{':
		var jc JSONContainer
		err := jc.UnmarshalJSON(in)
		return jc, err
	case ins[0] == '[':
		var jc JSONArr
		err := jc.UnmarshalJSON(in)
		return jc, err
	case ins[0] == '"':
		return JSONStr(strings.Trim(string(ins), `"`)), nil
	case ins == "true":
		return JSONBool(true), nil
	case ins == "false":
		return JSONBool(false), nil
	case ins == "null":
		return JSONToken(nil), nil
	default:
		// The only valid choice left is a number.
		num, err := strconv.ParseFloat(ins, 64)
		if err != nil {
			return nil, err
		}

		return JSONNum(num), nil
	}
}

// IsIndex returns true iff the given field looks like [x] where x is any string of any length.
func IsIndex(field string) bool {
	return len(field) >= 2 && field[0] == '[' && field[len(field)-1] == ']'
}

// SegmentPath splits the given JSON path into segments/components. Static path components (like
// foo.bar.baz are returned verbatim ["foo", "bar", "baz"], and boxed array indices are returned
// boxed like foo[123].baz => ["foo", "[123]", "baz"]. A numeric static component like foo.3.bar
// will be returned as a string like ["foo", "3", "bar"].
func SegmentPath(path string) ([]string, error) {
	segs := make([]string, 0, 1)
	sb := strings.Builder{}

	var escaped, prevEscaped bool
	for i, c := range path {
		escaped = i > 0 && path[i-1] == '\\' && !prevEscaped
		delim := c == '.' && !escaped

		// Validation
		if !unicode.IsLetter(c) && !unicode.IsDigit(c) && !strings.Contains(`-*[]._\' `, string(c)) && !escaped {
			return nil, fmt.Errorf("invalid character %q", string(c))
		}
		if i > 0 && c == '.' && path[i-1] == '.' && !prevEscaped {
			return nil, fmt.Errorf("consecutive dots in path %s", path)
		}

		if (!delim && c != '\\') || escaped {
			sb.WriteRune(c)
		}

		if (delim || (i < len(path)-1 && path[i+1] == '[')) && sb.Len() > 0 {
			segs = append(segs, sb.String())
			sb.Reset()
		}

		prevEscaped = escaped
	}

	if sb.Len() > 0 {
		segs = append(segs, sb.String())
	}

	return segs, nil
}

// JoinPath rejoins the given path segments into a JSON Path.
func JoinPath(segs ...string) string {
	sb := strings.Builder{}
	for _, s := range segs {
		st := strings.Trim(s, " .")
		if st == "" {
			continue
		}

		if sb.Len() > 0 && !strings.HasSuffix(sb.String(), ".") && !strings.HasPrefix(st, "[") {
			sb.WriteString(".")
		}
		sb.WriteString(st)
	}

	return sb.String()
}

func hasArrayStar(segments []string) bool {
	for _, seg := range segments {
		if seg == "[*]" {
			return true
		}
	}
	return false
}

// JSONTokenAccessor defines an interface for accessing JSONToken with different engines.
type JSONTokenAccessor interface {
	GetField(src JSONToken, field string) (JSONToken, error)
	SetField(src JSONToken, field string, dest *JSONToken, forceOverwrite bool, matchNesting bool) error
	setFieldSegmented(src JSONToken, segments []string, dest *JSONToken, overwrite bool, matchNesting bool) error
}

// DefaultAccessor is a default JSONTokenAccessor to read/write JSONToken and used by the engine Whistler.
type DefaultAccessor struct{}

// GetField gets the specified field value for the provided JSON object.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first
// If the field is not found, nil is returned.
// Please note that field string should not have "[]" suffix.
// For example,
// GetField({"foo": "bar"}, "") => {"foo": "bar"}
// GetField(123, "") => 123
// GetField({"foo": "bar"}, "foo") => "bar"
// GetField({"foo": ["bar", 1]}, "foo") => ["bar", 1]
// GetField({"foo": ["bar", 1]}, "foo[0]") => "bar"
// GetField({"foo": [{"bar": 1}, {"bar": 2}], "foo[*].bar") => [1, 2]
// GetField is a wrapper for DefaultAccessor.GetField().
func GetField(src JSONToken, field string) (JSONToken, error) {
	a := DefaultAccessor{}
	return a.GetField(src, field)
}

// GetFieldSegmented is a wrapper for DefaultAccessor.GetFieldSegmented().
func GetFieldSegmented(src JSONToken, segments []string) (JSONToken, error) {
	a := DefaultAccessor{}
	return a.getFieldSegmented(src, segments)
}

// GetField gets the specified field value for the provided JSON object.
func (w DefaultAccessor) GetField(src JSONToken, field string) (JSONToken, error) {
	segs, err := SegmentPath(field)
	if err != nil {
		return nil, fmt.Errorf("failed to segment path: %v", err)
	}
	return w.getFieldSegmented(src, segs)
}

// getFieldSegmented gets the specified field value for the provided JSON object.
// segments are path segments like ["foo", "bar", "array", "[2]", "value"].
func (w DefaultAccessor) getFieldSegmented(src JSONToken, segments []string) (JSONToken, error) {
	if len(segments) == 0 {
		return src, nil
	}
	if src == nil {
		return nil, nil
	}

	seg := segments[0]

	switch o := src.(type) {
	case JSONArr:
		if seg == "[]" {
			return nil, fmt.Errorf("expected an array index with brackets like [123] but got []")
		}
		if !IsIndex(seg) {
			return nil, fmt.Errorf("expected an array index with brackets like [123] but got %s", seg)
		}

		idxSubstr := seg[1 : len(seg)-1]

		if idxSubstr == "*" {
			flatten := JSONArr{}

			for i := range o {
				f, err := w.getFieldSegmented(o[i], segments[1:])
				if err != nil {
					return nil, fmt.Errorf("error expanding [*] on item index %d: %v", i, err)
				}

				// If an array expansion occurs down the line, we need to unnest the resulting array here.
				if f != nil && hasArrayStar(segments[1:]) {
					fArr, ok := f.(JSONArr)
					if !ok {
						return nil, fmt.Errorf("this is an internal bug: found nested [*] but value was not an array (was %T)", f)
					}
					flatten = append(flatten, fArr...)
				} else {
					flatten = append(flatten, f)
				}
			}

			return flatten, nil
		}

		idx, err := strconv.Atoi(idxSubstr)
		if err != nil {
			return nil, fmt.Errorf("could not parse array index %s: %v", seg, err)
		}
		if idx < 0 {
			return nil, fmt.Errorf("negative array indices are not supported but got %d", idx)
		}
		if idx >= len(o) {
			// TODO: Consider returning a different value for fields that don't exist vs
			// fields that are actually set to null.
			return nil, nil
		}
		return w.getFieldSegmented(o[idx], segments[1:])
	case JSONContainer:
		if seg == "" || seg == "." {
			return w.getFieldSegmented(o, segments[1:])
		}
		if item, ok := o[seg]; ok {
			return w.getFieldSegmented(*item, segments[1:])
		}
		// TODO: Consider returning a different value for fields that don't exist vs
		// fields that are actually set to null.
		return nil, nil
	case JSONNum, JSONStr, JSONBool:
		return nil, fmt.Errorf("attempt to key into primitive with key %s", seg)
	}
	return nil, fmt.Errorf("this is an internal bug: JSON contained unknown data type %T at %s", src, seg)
}

// GetArray gets the specified array value for the provided JSON object.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first
func GetArray(j JSONToken, path string) ([]JSONToken, error) {
	tok, err := GetField(j, path)
	if err != nil {
		return nil, err
	}

	arr, ok := tok.(JSONArr)
	if !ok {
		return nil, fmt.Errorf("expected array but found %T at path: %s", tok, path)
	}

	return []JSONToken(arr), nil
}

// GetString gets the specified field value for the provided JSON object as a string.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first
func GetString(j JSONToken, path string) (string, error) {
	tok, err := GetField(j, path)
	if err != nil {
		return "", err
	}

	str, ok := tok.(JSONStr)
	if !ok {
		return "", fmt.Errorf("expected string but found %T at path: %s", tok, path)
	}

	return string(str), nil
}

// GetStringOrDefault gets the specified field value for the provided JSON object as a string.
// If no object is found at the specified path, the given default string is returned.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first
func GetStringOrDefault(j JSONToken, path, def string) (string, error) {
	tok, err := GetField(j, path)
	if err != nil {
		return "", err
	}
	if tok == nil {
		return def, nil
	}

	str, ok := tok.(JSONStr)
	if !ok {
		return "", fmt.Errorf("expected string but found %T at path: %s", tok, path)
	}

	return string(str), nil
}

// HasField determines if the specified field exists and is non-nil for the provided JSON object.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first
func HasField(j JSONToken, path string) (bool, error) {
	obj, err := GetField(j, path)
	if err != nil {
		return false, err
	}

	return obj != nil, err
}

// SetField sets the specified field of dest JSONToken into the src provided.
// Nested fields can be accessed using the "." notation and repeated fields can be accessed using
// the "[i]" notation. E.g. name[0].first.
// dest can be of primitive, array or object.
// if matchNesting is set to True, each element in src will be unpacked at each level and pass to the corresponding field.
// For example,
// SetField(0, "foo.baz", &{"foo": {"bar": 1}}, false, false) => {"foo": {"bar": 1, "baz": 0}}
// SetField(0, "foo.bar", &{"foo": {"bar": 1}}, true, false) => {"foo": {"bar": 0}}
// SetField(1, "foo[]", &{"foo": [0]}, false, false) => {"foo": [0, 1]}
// SetField(1, "foo[]", &{"foo": [0]}, false, false) => {"foo": [0, 1]}
// SetField{[1, 2], "foo[].bar", &{"foo": [0]}, false, false) => {"foo": [0, {"bar": [1, 2]}]}
// SetField{[1, 2], "foo[].bar", &{"foo": [0]}, false, true) => {"foo": [0, {"bar": 1}, {"bar": 2}]}
func SetField(src JSONToken, field string, dest *JSONToken, overwrite bool, matchNesting bool) error {
	w := DefaultAccessor{}
	return w.SetField(src, field, dest, overwrite, matchNesting)
}

// SetField sets the specified field value for the provided JSON object.
func (w DefaultAccessor) SetField(src JSONToken, field string, dest *JSONToken, overwrite bool, matchNesting bool) error {
	segments, err := SegmentPath(field)
	if err != nil {
		return fmt.Errorf("failed to segment path: %v", err)
	}
	return w.setFieldSegmented(src, segments, dest, overwrite, matchNesting)
}

// setFieldSegmented sets the specified field value for the provided JSON object.
// segments are path segments like ["foo", "bar", "array", "[2]", "value"].
func (w DefaultAccessor) setFieldSegmented(src JSONToken, segments []string, dest *JSONToken, overwrite bool, matchNesting bool) error {
	if len(segments) == 0 {
		if overwrite {
			*dest = src
			return nil
		}
		return Merge(src, dest, true /* failOnOverwrite */, false /* overwriteArrays */)
	}

	seg := segments[0]

	if *dest == nil {
		if IsIndex(seg) {
			*dest = make(JSONArr, 0, 1)
		} else {
			*dest = make(JSONContainer)
		}
	}

	switch o := (*dest).(type) {
	case JSONArr:
		return setArrField(w, src, segments, dest, overwrite, matchNesting)
	case JSONContainer:
		if seg == "" || seg == "." {
			var obj JSONToken = o
			return w.setFieldSegmented(src, segments[1:], &obj, overwrite, matchNesting)
		}
		item, ok := o[seg]
		if !ok {
			n := JSONToken(nil)
			item = &n
			o[seg] = item
		}
		return w.setFieldSegmented(src, segments[1:], item, overwrite, matchNesting)
	case JSONNum, JSONStr, JSONBool:
		return fmt.Errorf("attempt to key into primitive with key %s", seg)
	}
	return fmt.Errorf("this is an internal bug: JSON contained unknown data type %T at %s", *dest, seg)
}

// setArrField sets the specified field(s) in *dest to data in src using the
// Accessor acc (either in parallel or sequentially) when *dest is a JSONArr.
// the field(s) to set values are determined by path segments like like ["foo",
// "bar", "array", "[2]", "value", "[]"]. when overwrite is set to true, the
// field will be overwritten by the new data when matchNesting is set to true,
// everytime the segment is '[]', each element in src array will be unpacked and
// appended to the dest array as a separate container i.e. dest[].field: src[]
// is equivalant to for s in src {dest[].field : s} When there are more
// appending operation in dest than the src, the src will be expanded until the
// deepest layer of the target
// E.g.
// setField([1,2], ['A', '[]', 'B', '[]', 'C'], &{"A": [0]}, false, true} => {"A": [0, {"B": [{"C": 1}, {"C": 2}]}]}
// setField([[1, 2],[[3]], 4], ['A', '[]', 'B', '[]', 'C'], &{"A": [0]}, false, true} => {"A": [0, {"B": [{"C": 1}, {"C": 2}]}, {"B": [{"C": [3]}]}, {"B": [{"C": 4}]}]}
func setArrField(acc JSONTokenAccessor, src JSONToken, segments []string, dest *JSONToken, overwrite bool, matchNesting bool) error {
	o := (*dest).(JSONArr)
	seg := segments[0]
	if seg == "[]" {
		// If both of src and dest are arrays, src will be appended into dest.
		if a, isArr := src.(JSONArr); isArr {
			if len(segments) == 1 {
				*dest = append(o, a...)
				return nil
			} else if matchNesting {
				for _, element := range a {
					n := JSONToken(nil)
					item := &n
					if err := acc.setFieldSegmented(element, segments[1:], item, overwrite, matchNesting); err != nil {
						return err
					}
					*dest = append((*dest).(JSONArr), n)
				}
				return nil
			}
		}
		seg = fmt.Sprintf("[%d]", len(o))
	}
	if !IsIndex(seg) {
		return fmt.Errorf("expected an array index with brackets like [123] but got %s", seg)
	}

	idxSubstr := seg[1 : len(seg)-1]

	if idxSubstr == "*" {
		return fmt.Errorf("cannot use [*] when writing to a field (can only use it when reading)")
	}

	idx, err := strconv.Atoi(idxSubstr)
	if err != nil {
		return fmt.Errorf("could not parse array index %s: %v", seg, err)
	}

	if idx < 0 {
		return fmt.Errorf("negative array indices are not supported but got %d", idx)
	}
	if idx >= len(o) {
		o = append(o, make(JSONArr, idx-len(o)+1)...)
		*dest = o
	}
	return acc.setFieldSegmented(src, segments[1:], &o[idx], overwrite, matchNesting)
}

// Merge merges two JSONTokens together. If failOnOverwrite is true, this method guarantees that no
// existing data anywhere in the destination will be lost (unless overwriteArrays is true).
func Merge(src JSONToken, dest *JSONToken, failOnOverwrite, overwriteArrays bool) error {
	if dest == nil {
		return errors.New("destination is nil pointer")
	} else if *dest == nil {
		*dest = src
		return nil
	}

	// Overwrite or fail.
	if isPrim(src) {
		if err := getOverwriteError(dest); failOnOverwrite && err != nil {
			return err
		}
		*dest = src
		return nil
	}

	switch d := (*dest).(type) {
	case JSONContainer:
		if srcCon, ok := src.(JSONContainer); ok {
			for k, v := range srcCon {
				if d[k] == nil {
					d[k] = v
				} else if err := Merge(*v, d[k], failOnOverwrite, overwriteArrays); err != nil {
					return err
				}
			}
		} else if len(d) == 0 {
			// If the destination is empty, allow type change.
			*dest = src
		} else {
			return fmt.Errorf("can't merge source %T with destination %T", src, d)
		}
	case JSONArr:
		if srcArr, ok := src.(JSONArr); ok {
			if !overwriteArrays {
				*dest = append(d, srcArr...)
			} else {
				*dest = srcArr
			}
		} else if len(d) == 0 {
			// If the destination is empty, allow type change.
			*dest = src
		} else {
			// TODO: Append src as is to dest?
			return fmt.Errorf("can't merge source %T with destination %T", src, d)
		}
	default:
		return fmt.Errorf("this is an internal bug: destination is of unknown type %T", d)
	}

	return nil
}

// getOverwriteError returns the problem with attempting to overwrite the given destination token.
// If the destination can be safely overwritten (no data lost), returns nil.
func getOverwriteError(dest *JSONToken) error {
	if *dest != nil && isPrim(*dest) {
		return fmt.Errorf("attempt to overwrite primitive destination with primitive source")
	}
	if jc, ok := (*dest).(JSONContainer); ok && len(jc) > 0 {
		return fmt.Errorf("attempt to overwrite non-empty container destination with primitive source")
	}
	if ja, ok := (*dest).(JSONArr); ok && len(ja) > 0 {
		return fmt.Errorf("attempt to overwrite non-empty array destination with primitive source")
	}

	return nil
}

func isPrim(object JSONToken) bool {
	switch (object).(type) {
	case JSONNum:
		return true
	case JSONStr:
		return true
	case JSONBool:
		return true
	case nil:
		return true
	}
	return false
}

// JSONToken represents a base JSON token. It can be a primitive, an array,
// or a container object.
type JSONToken interface {
	jsonObject()
	Value() JSONToken
	Equal(JSONToken) bool
}

// GetValue gets actual JSON value in a JSONToken. If it is nil, return nil.
func GetValue(j JSONToken) JSONToken {
	if j == nil {
		return nil
	}
	return j.Value()
}

// JSONContainer is a JSON object that contains some child fields. For example,
// { "foo": "bar", "baz": [1, 2, 3] }
type JSONContainer map[string]*JSONToken

// JSONArr is a JSON array that contains 0 or more JSONObjects.
type JSONArr []JSONToken

// JSONStr represents a primitive JSON string.
type JSONStr string

// JSONNum represents a primitive JSON float.
type JSONNum float64

// JSONBool represents a primitive JSON bool.
type JSONBool bool

func (JSONContainer) jsonObject() {}
func (JSONArr) jsonObject()       {}
func (JSONStr) jsonObject()       {}
func (JSONNum) jsonObject()       {}
func (JSONBool) jsonObject()      {}

// Value gets a JSON object.
func (c JSONContainer) Value() JSONToken {
	return c
}

// Value gets a JSON array.
func (a JSONArr) Value() JSONToken {
	return a
}

// Value gets a primitive JSON string.
func (s JSONStr) Value() JSONToken {
	return s
}

// Value gets a primitive JSON float.
func (n JSONNum) Value() JSONToken {
	return n
}

// Value gets a primitive JSON bool.
func (b JSONBool) Value() JSONToken {
	return b
}

// Equal reports whether j and itself are equal by recursively checking each element in the map.
func (c JSONContainer) Equal(j JSONToken) bool {
	jc, ok := j.(JSONContainer)
	if !ok || len(c) != len(jc) {
		return false
	}
	for k, x := range c {
		y, ok := jc[k]
		if !ok {
			return false
		}
		if *x == nil {
			if *y != nil {
				return false
			}
		} else if !(*x).Equal(*y) {
			return false
		}
	}
	return true
}

// Equal reports whether j and itself are equal by recursively checking each element in the array.
func (a JSONArr) Equal(j JSONToken) bool {
	ja, ok := j.(JSONArr)
	if !ok || len(a) != len(ja) {
		return false
	}
	for i, x := range a {
		if x == nil {
			if ja[i] != nil {
				return false
			}
		} else if !x.Equal(ja[i]) {
			return false
		}
	}
	return true
}

// Equal reports whether j and itself are equal.
func (s JSONStr) Equal(j JSONToken) bool {
	js, ok := j.(JSONStr)
	return ok && s == js
}

// Equal reports whether j and itself are equal.
func (n JSONNum) Equal(j JSONToken) bool {
	jn, ok := j.(JSONNum)
	return ok && n == jn
}

// Equal reports whether j and itself are equal.
func (b JSONBool) Equal(j JSONToken) bool {
	jb, ok := j.(JSONBool)
	return ok && b == jb
}

func (c JSONContainer) String() string {
	o := make([]string, 0, len(c))
	for k, v := range c {
		o = append(o, fmt.Sprintf("%q:%s", k, MarshalJSON(*v)))
	}
	return fmt.Sprintf("{%s}", strings.Join(o, ","))
}

func (a JSONArr) String() string {
	o := make([]string, 0, len(a))
	for _, v := range a {
		o = append(o, MarshalJSON(v))
	}
	return fmt.Sprintf("[%s]", strings.Join(o, ","))
}

// JSONPrimitive represents one of the JSON primitive types (Str, Bool, Num). This is useful for
// defining a variable/parameter that is guaranteed to be a leaf node in the JSON tree.
type JSONPrimitive interface {
	jsonPrimitive()
}

func (JSONStr) jsonPrimitive()  {}
func (JSONBool) jsonPrimitive() {}
func (JSONNum) jsonPrimitive()  {}

// unmarshaledToJSONToken wraps the output of json.Unmarshal() into one of the above JSON wrapper
// types. For arrays and containers will recursively convert their contents. unmarshaledToJSONToken
// exclusively supports int, float64, bool, string,  []interface{}, and map[string]interface{}
// (where interface{} is a type supported by unmarshaledToJSONToken).
func unmarshaledToJSONToken(object interface{}) (JSONToken, error) {
	if object == nil {
		return nil, nil
	}

	var token JSONToken
	switch o := object.(type) {
	case int:
		n := JSONNum(o)
		token = n
	case float64:
		n := JSONNum(o)
		token = n
	case bool:
		b := JSONBool(o)
		token = b
	case string:
		s := JSONStr(o)
		token = s
	case []interface{}:
		ja := make(JSONArr, 0, len(o))
		for _, obj := range o {
			co, err := unmarshaledToJSONToken(obj)
			if err != nil {
				return nil, err
			}
			ja = append(ja, co)
		}
		token = ja
	case map[string]interface{}:
		jc := make(JSONContainer)
		for k, v := range o {
			cv, err := unmarshaledToJSONToken(v)
			if err != nil {
				return nil, err
			}
			jc[k] = &cv
		}
		token = jc
	default:
		return nil, fmt.Errorf("unable to wrap a %T in JSON wrapper types", o)
	}

	return token, nil
}

// UnmarshalJSON sets the contents of the receiver to be the unmarshaled and converted contents of
// the given JSON (hence given JSON must be of an object, rather than primitive or array).
func (c *JSONContainer) UnmarshalJSON(j json.RawMessage) error {
	var m map[string]interface{}
	err := json.Unmarshal(j, &m)
	if err != nil {
		return err
	}

	cm, err := unmarshaledToJSONToken(m)
	if err != nil {
		return err
	}

	*c = cm.(JSONContainer)

	return nil
}

// UnmarshalJSON sets the contents of the receiver to be the unmarshaled and converted contents of
// the given JSON (hence given JSON must be of an array, rather than primitive or object).
func (a *JSONArr) UnmarshalJSON(j json.RawMessage) error {
	var m []interface{}
	err := json.Unmarshal(j, &m)
	if err != nil {
		return err
	}

	cm, err := unmarshaledToJSONToken(m)
	if err != nil {
		return err
	}

	*a = cm.(JSONArr)

	return nil
}

// UnmarshalRawMessages unmarshals an array of raw json messages into an array of JSON containers.
func UnmarshalRawMessages(resources []*json.RawMessage) (JSONArr, error) {
	array := make(JSONArr, 0, len(resources))
	for _, raw := range resources {
		jc := &JSONContainer{}
		if err := jc.UnmarshalJSON(*raw); err != nil {
			return nil, fmt.Errorf("error parsing retrieved resources %s", err)
		}
		array = append(array, *jc)
	}
	return array, nil
}

// Deepcopy will make a deep copy of the elements inside a JSONToken
func Deepcopy(t JSONToken) JSONToken {
	switch c := t.(type) {
	case JSONContainer:
		a := make(JSONContainer)
		for k, v := range c {
			copied := Deepcopy(*v)
			a[k] = &copied
		}
		return a
	case JSONArr:
		a := make(JSONArr, 0, len(c))
		for _, v := range c {
			a = append(a, Deepcopy(v))
		}
		return a
	default:
		return c
	}
}

// UnorderedEqual recursively compares two given tokens.
// Both of key order and array item order are not considered.
func UnorderedEqual(x, y JSONToken) bool {
	hx, err := Hash(x, true)
	if err != nil {
		return false
	}
	hy, err := Hash(y, true)
	if err != nil {
		return false
	}
	return cmp.Equal(hx, hy)
}

// Hash converts the given token into a hash. Key order is not considered.
// This is not cryptographically secure, and is not to be used for secure hashing.
// If arrayWithoutOrder is true, array item order will be not considered.
func Hash(obj JSONToken, arrayWithoutOrder bool) ([]byte, error) {
	h := fnv.New128()
	err := hashObj(obj, h, arrayWithoutOrder)
	return h.Sum([]byte{}), err
}

func hashObj(obj JSONToken, h hash.Hash, arrayWithoutOrder bool) error {
	switch t := obj.(type) {
	case JSONStr:
		return hashBytes("str", []byte(t), h)
	case JSONNum:
		var b [8]byte
		binary.BigEndian.PutUint64(b[:], math.Float64bits(float64(t)))
		return hashBytes("num", b[:], h)
	case JSONBool:
		b := []byte{1}
		if t {
			b = []byte{2}
		}
		return hashBytes("bool", b, h)
	case JSONArr:
		if arrayWithoutOrder {
			hc := make([]byte, 16)
			for _, a := range t {
				ah := fnv.New128()
				if err := hashObj(a, ah, arrayWithoutOrder); err != nil {
					return err
				}
				if err := xor128(hc, ah.Sum(nil)); err != nil {
					return err
				}
			}
			return hashBytes("arr", hc, h)
		}
		for _, a := range t {
			if err := hashObj(a, h, arrayWithoutOrder); err != nil {
				return err
			}
		}
	case JSONContainer:
		var kv []struct {
			k string
			v JSONToken
		}

		for k, v := range t {
			kv = append(kv, struct {
				k string
				v JSONToken
			}{k, *v})
		}

		// Stable sort not needed because container can't have multiple occurrences of the same key.
		sort.Slice(kv, func(i, j int) bool {
			return kv[i].k < kv[j].k
		})

		for _, a := range kv {
			if err := hashBytes("key", []byte(a.k), h); err != nil {
				return err
			}
			if err := hashObj(a.v, h, arrayWithoutOrder); err != nil {
				return err
			}
		}
	case nil:
		return hashBytes("nil", []byte{}, h)
	default:
		return fmt.Errorf("this is an internal bug: unknown JSON type %T", obj)
	}
	return nil
}

func xor128(x []byte, y []byte) error {
	if len(x) != 16 || len(y) != 16 {
		return fmt.Errorf("this is an internal bug: xor128() got data that was not 128 bits long: %v, %v", x, y)
	}
	for i := 0; i < 16; i++ {
		x[i] ^= y[i]
	}
	return nil
}

// StringToToken returns a pointer to a JSONToken created using the input string.
func StringToToken(text string) *JSONToken {
	t := JSONToken(JSONStr(text))
	return &t
}

func hashBytes(salt string, data []byte, h hash.Hash) error {
	sb := append([]byte(salt), data...)
	n, err := h.Write(sb)
	if err != nil {
		return err
	}
	if n != len(sb) {
		return fmt.Errorf("this is an internal bug: could not write %d bytes to %T (only wrote %d): %v", len(sb), h, n, err)
	}
	return nil
}
