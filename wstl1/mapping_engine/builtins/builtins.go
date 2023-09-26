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

// Package builtins contains function definitions and implementation for built-in mapping functions.
package builtins

import (
	"encoding/hex"
	"errors"
	"fmt"
	"hash/fnv"
	"math"
	"regexp"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"bitbucket.org/creachadair/stringset" /* copybara-comment: stringset */
	"github.com/google/uuid" /* copybara-comment: uuid */
)

// When adding a built-in, remember to add it to the map below with its name as the key.
var BuiltinFunctions = map[string]interface{}{
	// Arithmetic
	"$Div": Div,
	"$Mod": Mod,
	"$Mul": Mul,
	"$Sub": Sub,
	"$Sum": Sum,

	// Collections
	"$Flatten":        Flatten,
	"$ListCat":        ListCat,
	"$ListLen":        ListLen,
	"$ListOf":         ListOf,
	"$SortAndTakeTop": SortAndTakeTop,
	"$Range":          Range,
	"$UnionBy":        UnionBy,
	"$Unique":         Unique,
	"$UnnestArrays":   UnnestArrays,

	// Date/Time
	"$CurrentTime":          CurrentTime,
	"$MultiFormatParseTime": MultiFormatParseTime,
	"$ParseTime":            ParseTime,
	"$ParseUnixTime":        ParseUnixTime,
	"$ReformatTime":         ReformatTime,
	"$SplitTime":            SplitTime,

	// Data operations
	"$Hash":      Hash,
	"$IntHash":   IntHash,
	"$IsNil":     IsNil,
	"$IsNotNil":  IsNotNil,
	"$MergeJSON": MergeJSON,
	"$UUID":      UUID,
	"$Type":      Type,

	// Debugging
	"$DebugString": DebugString,
	"$Void":        Void,

	// Logic
	"$And":  And,
	"$Eq":   Eq,
	"$Gt":   Gt,
	"$GtEq": GtEq,
	"$Lt":   Lt,
	"$LtEq": LtEq,
	"$NEq":  NEq,
	"$Not":  Not,
	"$Or":   Or,

	// Strings
	"$MatchesRegex": MatchesRegex,
	"$ParseFloat":   ParseFloat,
	"$ParseInt":     ParseInt,
	"$SubStr":       SubStr,
	"$StrCat":       StrCat,
	"$StrFmt":       StrFmt,
	"$StrJoin":      StrJoin,
	"$StrSplit":     StrSplit,
	"$ToLower":      ToLower,
	"$ToUpper":      ToUpper,
	"$Trim":         Trim,
}

// Now is exported to allow for mocking in tests.
var Now = time.Now

const (
	defaultTimeFormat   = "2006-01-02 03:04:05"
	pythonStyleDateTime = 0
	goStyleDateTime     = 1
)

// need to put more complicated formatting first and the substrings it contains after otherwise the longer string only gets partially translated
var (
	timeTokenMap = [...][2]string{
		{"%c", "Mon Jan 2 15:04:05 2006"}, // Python: Locale’s appropriate date and time representation.
		{"%x", "01/02/06"},                // Python: Locale’s appropriate date representation.
		{"%X", "15:04:05"},                // Python: Locale’s appropriate time representation.
		{"%A", "Monday"},                  // Python: Locale’s full weekday name.
		{"%a", "Mon"},                     // Python: Locale’s abbreviated weekday name.
		{"%B", "January"},                 // Python: Locale’s full month name.
		{"%b", "Jan"},                     // Python: Locale’s abbreviated month name.
		{"%Y", "2006"},                    // Python: Year with century as a decimal number.
		{"%d", "02"},                      // Python: Day of the month as a decimal number [01,31].
		{"%e", "2"},                       // Google Cloud SQL: The day of month as a decimal number (1-31).
		{"%H", "15"},                      // Python: Hour (24-hour clock) as a decimal number [00,23].
		{"%I", "03"},                      // Python: Hour (12-hour clock) as a decimal number [01,12].
		{"%i", "3"},                       // ADDED: 12H hour representation without padding
		{"%m", "01"},                      // Python: Month as a decimal number [01,12].
		{"%M", "04"},                      // Python: Minute as a decimal number [00,59].
		{"%p", "PM"},                      // Python: Locale’s equivalent of either AM or PM.
		{"%S", "05"},                      // Python: Second as a decimal number [00,60].
		{"%s", "5"},                       // ADDED: second as a decimal number without padding [0,60]
		{"%y", "06"},                      // Python: Year without century as a decimal number [00,99].
		{"%Z", "MST"},                     // Python: Time zone name (no characters if no time zone exists).
		{"%z", "-07:00"},                  // Python: Time zone offset indicating a positive or negative time difference from UTC/GMT
		{"%z", "-0700"},
		{"%z", "-07"},
	}
	pythonFormatRegex *regexp.Regexp
)

func init() {
	precompilePythonTimeFormat()
}

// precompilePythonTimeFormat precompile the regex for python formatting string
func precompilePythonTimeFormat() {
	var tokens [len(timeTokenMap)]string
	for i := 0; i < len(timeTokenMap); i++ {
		tokens[i] = timeTokenMap[i][0][1:]
	}
	pythonFormatRegex, _ = regexp.Compile(fmt.Sprintf("^(.{0}|(.*%%[%s].*))+$", strings.Join(tokens[:], "")))
}

// Although arguments and types can vary, all projectors, including built-ins must return
// (jsonutil.JSONToken, error). The first return value can be any type assignable to
// jsonutil.JSONToken. For predicates that must return a boolean (jsonutil.JSONBool), the type
// will be checked/enforced at runtime.

// Div divides the first argument by the second.
func Div(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONNum, error) {
	return left / right, nil
}

// Mod returns the remainder of dividing the first argument by the second.
func Mod(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONNum, error) {
	res := math.Mod(float64(left), float64(right))
	if math.IsNaN(res) {
		return -1, errors.New("modulo operation returned NaN")
	}
	return jsonutil.JSONNum(res), nil
}

// Mul multiplies together all given arguments. Returns 0 if nothing given.
func Mul(operands ...jsonutil.JSONNum) (jsonutil.JSONNum, error) {
	if len(operands) == 0 {
		return 0, nil
	}

	var res jsonutil.JSONNum = 1
	for _, n := range operands {
		res *= n
	}
	return res, nil
}

// Sub subtracts the second argument from the first.
func Sub(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONNum, error) {
	return left - right, nil
}

// Sum adds up all given values.
func Sum(operands ...jsonutil.JSONNum) (jsonutil.JSONNum, error) {
	var res jsonutil.JSONNum
	for _, n := range operands {
		res += n
	}
	return res, nil
}

// Unique returns the unique elements in the array by comparing their hashes.
func Unique(array jsonutil.JSONArr) (jsonutil.JSONArr, error) {
	arr := make(jsonutil.JSONArr, 0)
	set := make(map[jsonutil.JSONStr]bool)

	for _, i := range array {
		hash, err := Hash(i)
		if err != nil {
			return nil, err
		}
		if !set[hash] {
			arr = append(arr, i)
			set[hash] = true
		}
	}

	return arr, nil
}

// Flatten turns a nested array of arrays (of any depth) into a single array.
// Item ordering is preserved, depth first.
func Flatten(array jsonutil.JSONArr) (jsonutil.JSONArr, error) {
	// This needs to always return an empty array, not a nil value. Nil values
	// may cause NPE down the line.
	res := make(jsonutil.JSONArr, 0)

	for _, item := range array {
		if subArr, ok := item.(jsonutil.JSONArr); ok {
			flat, err := Flatten(subArr)
			if err != nil {
				return nil, err
			}

			res = append(res, flat...)
		} else {
			res = append(res, item)
		}
	}

	return res, nil
}

// ListCat concatenates all given arrays into one array.
func ListCat(args ...jsonutil.JSONArr) (jsonutil.JSONArr, error) {
	if len(args) == 0 {
		return jsonutil.JSONArr{}, nil
	}
	if len(args) == 1 {
		return args[0], nil
	}

	var cat jsonutil.JSONArr
	for _, a := range args {
		cat = append(cat, a...)
	}

	return cat, nil
}

// ListLen finds the length of the array.
func ListLen(in jsonutil.JSONArr) (jsonutil.JSONNum, error) {
	return jsonutil.JSONNum(len(in)), nil
}

// ListOf creates a list of the given tokens.
func ListOf(args ...jsonutil.JSONToken) (jsonutil.JSONArr, error) {
	return jsonutil.JSONArr(args), nil
}

// SortAndTakeTop sorts the elements in the array by the key in the specified direction and returns the top element.
func SortAndTakeTop(arr jsonutil.JSONArr, key jsonutil.JSONStr, desc jsonutil.JSONBool) (jsonutil.JSONToken, error) {
	if len(arr) == 0 {
		return nil, nil
	}
	if len(arr) == 1 {
		return arr[0], nil
	}

	tm := map[string]jsonutil.JSONToken{}
	var keys []string
	for _, t := range arr {
		k, err := jsonutil.GetField(t, string(key))
		if err != nil {
			return nil, err
		}
		kstr := fmt.Sprintf("%v", k)
		tm[kstr] = t
		keys = append(keys, kstr)
	}

	sort.Strings(keys)
	if desc {
		return tm[keys[len(keys)-1]], nil
	}
	return tm[keys[0]], nil
}

// Range generates an array of sequentially ordered number from start (inclusive) to end (exclusive) by a step of 1.
// Example:
// $Range(2, 5) returns: [2, 3, 4]
// $Range(5, 2) returns: [5, 4, 3]
// $Range(-2, 1) returns: [-2, -1, 0]
func Range(start jsonutil.JSONNum, end jsonutil.JSONNum) (jsonutil.JSONArr, error) {
	result := make(jsonutil.JSONArr, 0)
	var increment = (start <= end)
	var i = start
	for (increment && i < end) || (!increment && i > end) {
		result = append(result, jsonutil.JSONNum(i))
		if increment {
			i++
		} else {
			i--
		}
	}
	return result, nil
}

// UnionBy unions the items in the given array by the given keys, such that each item
// in the resulting array has a unique combination of those keys. The first unique element
// is picked when deduplicating. The items in the resulting array are ordered
// deterministically (i.e unioning of array [x, y, z] and array [z, x, x, y], both return
// [x, y, z]).
//
// E.g:
// Arguments: items: `[{"id": 1}, {"id": 2}, {"id": 1, "foo": "hello"}]`, keys: "id"
// Return: [{"id": 1}, {"id": 2}]
func UnionBy(items jsonutil.JSONArr, keys ...jsonutil.JSONStr) (jsonutil.JSONArr, error) {
	set := make(map[jsonutil.JSONStr]jsonutil.JSONToken)
	var orderedKeys []jsonutil.JSONStr

	for _, i := range items {
		var key jsonutil.JSONStr

		for _, k := range keys {
			v, err := jsonutil.GetField(i, string(k))
			if err != nil {
				return nil, err
			}

			h, err := Hash(v)
			if err != nil {
				return nil, err
			}

			key += h
		}

		if _, ok := set[key]; !ok {
			orderedKeys = append(orderedKeys, key)
			set[key] = i
		}
	}

	sort.Slice(orderedKeys, func(i int, j int) bool {
		return orderedKeys[i] < orderedKeys[j]
	})

	var arr jsonutil.JSONArr

	for _, k := range orderedKeys {
		arr = append(arr, set[k])
	}

	return arr, nil
}

// UnnestArrays takes a json object with nested arrays (e.g.: {"key1": [{}...], "key2": {}})
// and returns an unnested array that contains the top level key in the "k" field and each
// array element, unnested, in the "v" field (e.g.: [{"k": "key1", "v": {}} ...]).
// If the value of a key is an object, it simply returns that object. The
// output is sorted by the keys, and the array ordering is preserved.
// If the nested array is empty, the key is ignored.
//
// E.g:
// c: `{"key1":[{"a": "z"}, {"b": "y"}], "key2": {"c": "x"}, "key3": []}
// return: [{"k": "key1", "v":{"a": "z"}}`, {"k": "key1", "v":{"b": "y"}}, {"k": "key2", "v":{"c": "x"}}]
func UnnestArrays(c jsonutil.JSONContainer) (jsonutil.JSONArr, error) {
	var out jsonutil.JSONArr

	var keys []string
	for k := range c {
		keys = append(keys, k)
	}
	sort.Strings(keys)
	for _, k := range keys {
		var kstr jsonutil.JSONToken = jsonutil.JSONStr(k)

		arr, ok := (*c[k]).(jsonutil.JSONArr)
		if !ok {
			kv := jsonutil.JSONContainer{
				"k": &kstr,
				"v": c[k],
			}
			out = append(out, kv)
			continue
		}

		for _, i := range arr {
			vTkn := i
			kv := jsonutil.JSONContainer{
				"k": &kstr,
				"v": &vTkn,
			}
			out = append(out, kv)
		}
	}

	return out, nil
}

// CurrentTime returns the current time based on the Go func time.Now
// (https://golang.org/pkg/time/#Now). The function accepts a time format layout
// (https://golang.org/pkg/time/#Time.Format) and an IANA formatted time zone
// string (https://www.iana.org/time-zones). A string representing the current
// time is returned. A default layout of '2006-01-02 03:04:05'and a default
// time zone of 'UTC' will be used if not provided.
func CurrentTime(format jsonutil.JSONStr, tz jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	if len(format) == 0 {
		format = defaultTimeFormat
	}
	tm := Now().UTC()
	loc, err := time.LoadLocation(string(tz))
	if err != nil {
		return jsonutil.JSONStr(""), err
	}
	outputTime := tm.In(loc).Format(string(format))
	return jsonutil.JSONStr(outputTime), nil
}

// MultiFormatParseTime converts the time in the specified formats to RFC3339 (https://www.ietf.org/rfc/rfc3339.txt) format. It tries the formats in order and returns an error if none of the formats match.
// The function accepts a go time format layout (https://golang.org/pkg/time/#Time.Format) or Python time format layout (defined in timeTokenMap)
func MultiFormatParseTime(format jsonutil.JSONArr, date jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	for _, f := range format {
		s, ok := f.(jsonutil.JSONStr)
		if !ok {
			return jsonutil.JSONStr(""), fmt.Errorf("expected array of strings instead of %v", format)
		}
		t, err := ParseTime(s, date)
		if err == nil {
			return t, nil
		}
	}
	return jsonutil.JSONStr(""), fmt.Errorf("no date formats(%v) matched %v", format, date)
}

// ParseTime converts the time in the specified format to RFC3339 (https://www.ietf.org/rfc/rfc3339.txt) format.
// The function accepts a go time format layout (https://golang.org/pkg/time/#Time.Format) or Python time format layout (defined in timeTokenMap)
func ParseTime(format jsonutil.JSONStr, date jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	return ReformatTime(format, date, time.RFC3339Nano)
}

func parseTime(format, date jsonutil.JSONStr) (time.Time, error) {
	if len(date) == 0 {
		return time.Time{}, nil
	}
	format = convertTimeFormatToGo(format)
	isoDate, err := time.Parse(string(format), string(date))
	if err != nil {
		return time.Time{}, err
	}
	return isoDate, nil
}

// isPythonTimeFormat returns true iff the format string contains python formatting string.
func isPythonTimeFormat(format jsonutil.JSONStr) bool {
	return pythonFormatRegex.MatchString(string(format))
}

// convertTimeFormatToGo converts input DateTime formatting string to GO DateTime formatting string if it's in Python format.
func convertTimeFormatToGo(inFormat jsonutil.JSONStr) jsonutil.JSONStr {
	if isPythonTimeFormat(inFormat) {
		return convertTimeFormat(inFormat, pythonStyleDateTime, goStyleDateTime)
	}
	return inFormat
}

// convertTimeFormatGoToPython translates GO DateTime formatting string to Python DateTime formatting string.
func convertTimeFormatGoToPython(goFormat jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	if len(string(goFormat)) == 0 {
		return jsonutil.JSONStr(""), fmt.Errorf("the input goFormat cannot be empty")
	}
	pyFormat := convertTimeFormat(goFormat, goStyleDateTime, pythonStyleDateTime)
	isValid := isPythonTimeFormat(pyFormat)
	if !isValid {
		return jsonutil.JSONStr(""), fmt.Errorf("fail to convert the GO formatting string to valid python formatting string")
	}

	return pyFormat, nil
}

// ConvertTimeFormat converts a fomatting string in inStyle to that in outStyle, where inStyle and outStyle are defined in const
func convertTimeFormat(inFormat jsonutil.JSONStr, inStyle, outStyle int) jsonutil.JSONStr {
	result := []byte(string(inFormat))
	for _, tokenPair := range timeTokenMap {
		inToken := tokenPair[inStyle]
		outToken := tokenPair[outStyle]
		re := regexp.MustCompile(inToken)
		result = re.ReplaceAll(result, []byte(outToken))
	}
	return jsonutil.JSONStr(string(result))
}

// ParseUnixTime parses a unit and a unix timestamp into the speficied format.
// The function accepts a go time format layout (https://golang.org/pkg/time/#Time.Format) and Python time format layout (defined in timeTokenMap)
func ParseUnixTime(unit jsonutil.JSONStr, ts jsonutil.JSONNum, format jsonutil.JSONStr, tz jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	sec := int64(ts)
	ns := int64(0)
	switch strings.ToLower(string(unit)) {
	case "s":
		// Do nothing.
	case "ms":
		ns = sec * int64(time.Millisecond)
		sec = 0
	case "us":
		ns = sec * int64(time.Microsecond)
		sec = 0
	case "ns":
		ns = sec
		sec = 0
	default:
		return jsonutil.JSONStr(""), fmt.Errorf("unsupported unit %v, supported units are s, ms, us, ns", unit)
	}
	tm := time.Unix(sec, ns)
	loc, err := time.LoadLocation(string(tz))
	if err != nil {
		return jsonutil.JSONStr(""), fmt.Errorf("unsupported timezone %v", tz)
	}
	tm = tm.In(loc)
	format = convertTimeFormatToGo(format)
	return jsonutil.JSONStr(tm.Format(string(format))), nil
}

// ReformatTime uses a Go or Python time-format to convert date into another Go or Python time-formatted date time.
func ReformatTime(inFormat, date, outFormat jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	if len(string(inFormat)) == 0 {
		return jsonutil.JSONStr(""), fmt.Errorf("inFormat string cannot be empty")
	}
	if len(string(outFormat)) == 0 {
		return jsonutil.JSONStr(""), fmt.Errorf("outFormat string cannot be empty")
	}

	inFormat = convertTimeFormatToGo(inFormat)

	outFormat = convertTimeFormatToGo(outFormat)

	isoDate, err := parseTime(inFormat, date)
	if err != nil {
		return jsonutil.JSONStr(""), err
	}
	if isoDate.IsZero() {
		return jsonutil.JSONStr(""), nil
	}
	return jsonutil.JSONStr(isoDate.Format(string(outFormat))), nil
}

// SplitTime splits a time string into components based on the Go
// (https://golang.org/pkg/time/#Time.Format) and Python time-format provided.
// An array with all components (year, month, day, hour, minute, second and
// nanosecond) will be returned.
func SplitTime(format jsonutil.JSONStr, date jsonutil.JSONStr) (jsonutil.JSONArr, error) {
	d, err := parseTime(format, date)
	if err != nil {
		return jsonutil.JSONArr([]jsonutil.JSONToken{}), err
	}
	c := []jsonutil.JSONToken{
		jsonutil.JSONStr(strconv.Itoa(d.Year())),
		jsonutil.JSONStr(strconv.Itoa(int(d.Month()))),
		jsonutil.JSONStr(strconv.Itoa(d.Day())),
		jsonutil.JSONStr(strconv.Itoa(d.Hour())),
		jsonutil.JSONStr(strconv.Itoa(d.Minute())),
		jsonutil.JSONStr(strconv.Itoa(d.Second())),
		jsonutil.JSONStr(strconv.Itoa(d.Nanosecond())),
	}
	return jsonutil.JSONArr(c), nil
}

// Hash converts the given item into a hash. Key order is not considered (array item order is).
// This is not cryptographically secure, and is not to be used for secure hashing.
func Hash(obj jsonutil.JSONToken) (jsonutil.JSONStr, error) {
	h, err := jsonutil.Hash(obj, false)
	if err != nil {
		return "", err
	}
	return jsonutil.JSONStr(hex.EncodeToString(h)), nil
}

// IntHash converts the given item into a integer hash. Key order is not considered (array item order is).
// This is not cryptographically secure, and is not to be used for secure hashing.
func IntHash(obj jsonutil.JSONToken) (jsonutil.JSONNum, error) {
	h, err := jsonutil.Hash(obj, false)
	if err != nil {
		return -1, err
	}
	h32 := fnv.New32a()
	if _, err := h32.Write(h); err != nil {
		return -1, err
	}
	return jsonutil.JSONNum(h32.Sum32()), nil
}

// IsNil returns true iff the given object is nil or empty.
func IsNil(object jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	switch t := object.(type) {
	case jsonutil.JSONStr:
		return len(t) == 0, nil
	case jsonutil.JSONArr:
		return len(t) == 0, nil
	case jsonutil.JSONContainer:
		return len(t) == 0, nil
	case nil:
		return true, nil
	}

	return false, nil
}

// IsNotNil returns true iff the given object is not nil or empty.
func IsNotNil(object jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	isNil, err := IsNil(object)
	return !isNil, err
}

// MergeJSON merges the elements in the JSONArr into one JSON object by repeatedly calling the merge
// function. The merge function overwrites single fields and concatenates array fields (unless
// overwriteArrays is true, in which case arrays are overwritten).
func MergeJSON(arr jsonutil.JSONArr, overwriteArrays jsonutil.JSONBool) (jsonutil.JSONToken, error) {
	var out jsonutil.JSONToken
	for _, t := range arr {
		if out == nil {
			out = jsonutil.Deepcopy(t)
			continue
		}
		err := jsonutil.Merge(t, &out, false, bool(overwriteArrays))
		if err != nil {
			return nil, err
		}
	}
	return out, nil
}

// UUID generates a RFC4122 (https://tools.ietf.org/html/rfc4122) UUID.
func UUID() (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr(uuid.New().String()), nil
}

// Type returns the type of the given JSON Token as a string.
func Type(object jsonutil.JSONToken) (jsonutil.JSONStr, error) {

	switch object.(type) {
	case jsonutil.JSONNum:
		return jsonutil.JSONStr("number"), nil
	case jsonutil.JSONBool:
		return jsonutil.JSONStr("bool"), nil
	case jsonutil.JSONStr:
		return jsonutil.JSONStr("string"), nil
	case jsonutil.JSONArr:
		return jsonutil.JSONStr("array"), nil
	case jsonutil.JSONContainer:
		return jsonutil.JSONStr("container"), nil
	case nil:
		return jsonutil.JSONStr("null"), nil
	}

	return jsonutil.JSONStr(""), fmt.Errorf("Unrecognized JSON token type: %T", object)
}

// DebugString converts the JSON element to a string representation by
// recursively converting objects to strings.
func DebugString(t jsonutil.JSONToken) (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr(fmt.Sprintf("%v", t)), nil
}

// Void returns nil given any inputs. You non-nil into the Void, the Void nils back.
func Void(unused ...jsonutil.JSONToken) (jsonutil.JSONToken, error) {
	return nil, nil
}

// And is a logical AND of all given arguments.
func And(args ...jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	if len(args) == 0 {
		return false, nil
	}

	for _, a := range args {
		notA, _ := Not(a)
		if notA {
			return false, nil
		}
	}

	return true, nil
}

// Eq returns true iff all given arguments are equal.
func Eq(args ...jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	if len(args) < 2 {
		return true, nil
	}

	for _, arg := range args[1:] {
		if !cmp.Equal(arg, args[0]) {
			return false, nil
		}
	}

	return true, nil
}

// Gt returns true iff the first argument is greater than the second.
func Gt(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONBool, error) {
	return left > right, nil
}

// GtEq returns true iff the first argument is greater than or equal to the second.
func GtEq(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONBool, error) {
	return left >= right, nil
}

// Lt returns true iff the first argument is less than the second.
func Lt(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONBool, error) {
	return left < right, nil
}

// LtEq returns true iff the first argument is less than or equal to the second.
func LtEq(left jsonutil.JSONNum, right jsonutil.JSONNum) (jsonutil.JSONBool, error) {
	return left <= right, nil
}

// NEq returns true iff all given arguments are different.
func NEq(args ...jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	if len(args) < 2 {
		return true, nil
	}

	hashSet := stringset.NewSize(len(args))
	for _, a := range args {
		h, err := Hash(a)
		if err != nil {
			return false, err
		}

		if !hashSet.Add(string(h)) {
			return false, nil
		}
	}

	return true, nil
}

// Not returns true iff the given value is false.
func Not(object jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	result, ok := object.(jsonutil.JSONBool)
	if !ok {
		return IsNil(object)
	}
	return !result, nil
}

// Or is a logical OR of all given arguments.
func Or(args ...jsonutil.JSONToken) (jsonutil.JSONBool, error) {
	for _, a := range args {
		boolVal, ok := a.(jsonutil.JSONBool)
		if !ok {
			boolVal, _ = IsNotNil(a)
		}
		if boolVal {
			return true, nil
		}
	}

	return false, nil
}

// MatchesRegex returns true iff the string matches the regex pattern.
func MatchesRegex(str jsonutil.JSONStr, regex jsonutil.JSONStr) (jsonutil.JSONBool, error) {
	// TODO(): Consider compiling and caching these regexes.
	m, err := regexp.MatchString(string(regex), string(str))
	return jsonutil.JSONBool(m), err
}

// ParseFloat parses a string into a float.
func ParseFloat(str jsonutil.JSONStr) (jsonutil.JSONNum, error) {
	f, err := strconv.ParseFloat(string(str), 64)
	if err != nil {
		return 0, err
	}
	return jsonutil.JSONNum(f), nil
}

// ParseInt parses a string into an int.
func ParseInt(str jsonutil.JSONStr) (jsonutil.JSONNum, error) {
	i, err := strconv.Atoi(string(str))
	if err != nil {
		return -1, err
	}
	return jsonutil.JSONNum(i), nil
}

// SubStr returns a part of the string that is between the start index (inclusive) and the
// end index (exclusive). If the end index is greater than the length of the string, the end
// index is truncated to the length.
func SubStr(str jsonutil.JSONStr, start, end jsonutil.JSONNum) (jsonutil.JSONStr, error) {
	e := int(end)
	l := len(str)
	if e > l {
		e = l
	}
	if int(start) > l {
		return jsonutil.JSONStr(""), fmt.Errorf("start index %v is greater string length %v", start, l)
	}
	return jsonutil.JSONStr(string(str)[int(start):e]), nil
}

// StrCat joins the input strings with the separator.
func StrCat(args ...jsonutil.JSONToken) (jsonutil.JSONStr, error) {
	return StrJoin(jsonutil.JSONStr(""), args...)
}

// StrFmt formats the given item using the given Go format specifier (https://golang.org/pkg/fmt/).
func StrFmt(format jsonutil.JSONStr, item jsonutil.JSONToken) (jsonutil.JSONStr, error) {
	// This cast avoids formatting issues with numbers (since JSONNum is not detected as a number by the formatter)
	if numItem, ok := item.(jsonutil.JSONNum); ok {
		fmtSpec := format[strings.Index(string(format), "%")+1]
		if strings.Contains("bcdoqxXU", string(fmtSpec)) {
			return jsonutil.JSONStr(fmt.Sprintf(string(format), int(numItem))), nil
		}
		return jsonutil.JSONStr(fmt.Sprintf(string(format), float64(numItem))), nil
	}
	return jsonutil.JSONStr(fmt.Sprintf(string(format), item)), nil
}

// StrJoin joins the inputs together and adds the separator between them. Non-string arguments
// are converted to strings before joining.
func StrJoin(sep jsonutil.JSONStr, args ...jsonutil.JSONToken) (jsonutil.JSONStr, error) {
	var o []string
	for _, token := range args {
		if token != nil {
			o = append(o, fmt.Sprintf("%v", token))
		}
	}
	return jsonutil.JSONStr(strings.Join(o, string(sep))), nil
}

// StrSplit splits a string by the separator and ignores empty entries.
func StrSplit(str jsonutil.JSONStr, sep jsonutil.JSONStr) (jsonutil.JSONArr, error) {
	outs := strings.Split(string(str), string(sep))
	var res jsonutil.JSONArr
	for _, out := range outs {
		val := strings.TrimSpace(out)
		if len(val) == 0 {
			continue
		}
		res = append(res, jsonutil.JSONStr(val))
	}
	return res, nil
}

// ToLower converts the given string with all unicode characters mapped to their lowercase.
func ToLower(str jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr(strings.ToLower(string(str))), nil
}

// ToUpper converts the given string with all unicode characters mapped to their uppercase.
func ToUpper(str jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr(strings.ToUpper(string(str))), nil
}

// Trim strips the leading and trailing whitespace of the input string.
func Trim(str jsonutil.JSONStr) (jsonutil.JSONStr, error) {
	return jsonutil.JSONStr(strings.TrimSpace(string(str))), nil
}
