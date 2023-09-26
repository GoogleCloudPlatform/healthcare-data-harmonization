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

package builtins

import (
	"encoding/hex"
	"encoding/json"
	"fmt"
	"math"
	"regexp"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
)

func mustParseContainer(json json.RawMessage, t *testing.T) jsonutil.JSONContainer {
	t.Helper()
	c := make(jsonutil.JSONContainer)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func TestConvertTimeFormatGoToPython(t *testing.T) {
	tests := []struct {
		name, inFormat, want string
		wantErr              bool
	}{
		{
			name:     "regular date time",
			inFormat: "2006_01_02 15:04:05",
			want:     "%Y_%m_%d %X",
		},
		{
			name:     "irregular date time",
			inFormat: "06/01!2 3:04:5",
			want:     "%y/%m!%e %i:%M:%s",
		},
		{
			name:     "date time without padding (can only be used in formatting)",
			inFormat: "20060123045Z",
			want:     "%Y%m%e%i%M%sZ",
		},
		{
			name:     "UnixDate",
			inFormat: "Mon Jan _2 15:04:05 MST 2006",
			want:     "%a %b _%e %X %Z %Y",
		},
		{
			name:     "RFC3339",
			inFormat: "2006-01-02T15:04:05Z",
			want:     "%Y-%m-%dT%XZ",
		},
		{
			name:     "ISO8601",
			inFormat: "2006-01-02T15:04-07:00",
			want:     "%Y-%m-%dT%H:%M%z",
		},
		{
			name:     "ISO8601-UTC",
			inFormat: "2006-01-02T15:04Z",
			want:     "%Y-%m-%dT%H:%MZ",
		},
		{
			name:     "FHIR v4.0.1-dateTime",
			inFormat: "2006-01-02T15:04:05-07:00",
			want:     "%Y-%m-%dT%X%z",
		},
		{
			name:     "FHIR v4.0.1-date",
			inFormat: "2006-01-02",
			want:     "%Y-%m-%d",
		},
		{
			name:     "FHIR v4.0.1-time",
			inFormat: "15:04:05",
			want:     "%X",
		},
		{
			name:     "empty string",
			inFormat: "",
			wantErr:  true,
		},
		{
			name:     "invalid Go format",
			inFormat: "YYYY-MM-DD",
			wantErr:  true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := convertTimeFormatGoToPython(jsonutil.JSONStr(test.inFormat))
			if gotErr := (err != nil); gotErr != test.wantErr {
				t.Errorf("TestConvertTimeFormatGoToPython(%s) = %v with error %s, want %v with error %v", test.inFormat, got, err, test.want, test.wantErr)
			} else if string(got) != test.want {
				t.Errorf("TestConvertTimeFormatGoToPython(%s) = %v , want %s", test.inFormat, got, test.want)
			}
		})
	}
}

func TestConvertTimeFormatToGo(t *testing.T) {
	tests := []struct {
		name, inFormat, want string
		wantErr              bool
	}{

		{
			name:     "irregular date time",
			inFormat: "%e/%m/%y:%H-%M-%S",
			want:     "2/01/06:15-04-05",
		},
		{
			name:     "irregular date time without separator",
			inFormat: "%Y%m%e%i%M%s%z",
			want:     "20060123045-07:00",
		},
		{
			name:     "UnixDate",
			inFormat: "%a %b _%e %X %Z %Y",
			want:     "Mon Jan _2 15:04:05 MST 2006",
		},
		{
			name:     "RFC3339",
			inFormat: "%Y-%m-%dT%X%z",
			want:     "2006-01-02T15:04:05-07:00",
		},
		{
			name:     "ISO8601",
			inFormat: "%Y-%m-%dT%H:%M:%S%z",
			want:     "2006-01-02T15:04:05-07:00",
		},
		{
			name:     "ISO8601-UTC",
			inFormat: "%Y-%m-%dT%H:%M:%SZ",
			want:     "2006-01-02T15:04:05Z",
		},
		{
			name:     "FHIR v4.0.1",
			inFormat: "%Y-%m-%dT%X%z",
			want:     "2006-01-02T15:04:05-07:00",
		},
		{
			name:     "FHIR v4.0.1-date",
			want:     "2006-01-02",
			inFormat: "%Y-%m-%d",
		},
		{
			name:     "FHIR v4.0.1-time",
			inFormat: "%H:%M:%S",
			want:     "15:04:05",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got := convertTimeFormatToGo(jsonutil.JSONStr(test.inFormat))
			if string(got) != test.want {
				t.Errorf("TestConvertTimeFormatToGo(%s) = %s, want %s", test.inFormat, got, test.want)
			}
		})
	}
}

func mustParseArray(json json.RawMessage, t *testing.T) jsonutil.JSONArr {
	t.Helper()
	c := make(jsonutil.JSONArr, 0)

	err := c.UnmarshalJSON(json)

	if err != nil {
		t.Fatal(err)
	}

	return c
}

func TestMod(t *testing.T) {
	tests := []struct {
		name      string
		l         jsonutil.JSONNum
		r         jsonutil.JSONNum
		want      jsonutil.JSONNum
		wantError bool
	}{
		{
			name: "baseline modulo operation",
			l:    jsonutil.JSONNum(0),
			r:    jsonutil.JSONNum(1),
			want: jsonutil.JSONNum(0),
		},
		{
			name: "basic modulo operation with integer inputs",
			l:    jsonutil.JSONNum(10000),
			r:    jsonutil.JSONNum(333),
			want: jsonutil.JSONNum(10),
		},
		{
			name: "basic modulo operation with non-integer inputs",
			l:    jsonutil.JSONNum(10000.34),
			r:    jsonutil.JSONNum(333.12),
			want: jsonutil.JSONNum(6.740000000000009),
		},
		{
			name:      "divide by zero",
			l:         jsonutil.JSONNum(1),
			r:         jsonutil.JSONNum(0),
			wantError: true,
		},
		{
			name:      "inf mod y",
			l:         jsonutil.JSONNum(math.Inf(1)),
			r:         jsonutil.JSONNum(10),
			wantError: true,
		},
		{
			name:      "NaN mod y",
			l:         jsonutil.JSONNum(math.NaN()),
			r:         jsonutil.JSONNum(10),
			wantError: true,
		},
		{
			name: "x mod inf",
			l:    jsonutil.JSONNum(10),
			r:    jsonutil.JSONNum(math.Inf(1)),
			want: jsonutil.JSONNum(10),
		},
		{
			name:      "x mod NaN",
			l:         jsonutil.JSONNum(10),
			r:         jsonutil.JSONNum(math.NaN()),
			wantError: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Mod(test.l, test.r)
			if err != nil && !test.wantError {
				t.Fatalf("Mod(%v, %v) returned unexpected error %v", test.l, test.r, err)
			}
			if err == nil && test.wantError {
				t.Fatalf("Mod(%v, %v) expected error, but did not receive one.", test.l, test.r)
			}
			if !cmp.Equal(got, test.want) && !test.wantError {
				t.Errorf("Mod(%v, %v) = %v, want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestIsNil(t *testing.T) {
	var v jsonutil.JSONToken = jsonutil.JSONNum(0)
	tests := []struct {
		name string
		arg  jsonutil.JSONToken
		want jsonutil.JSONBool
	}{
		{
			name: "nil",
			arg:  nil,
			want: true,
		},
		{
			name: "nil interface",
			arg:  jsonutil.JSONToken(nil),
			want: true,
		},
		{
			name: "empty string",
			arg:  jsonutil.JSONStr(""),
			want: true,
		},
		{
			name: "empty array",
			arg:  jsonutil.JSONArr{},
			want: true,
		},
		{
			name: "empty container",
			arg:  jsonutil.JSONContainer{},
			want: true,
		},
		{
			name: "non-empty string",
			arg:  jsonutil.JSONStr("foo"),
			want: false,
		},
		{
			name: "non-empty array",
			arg:  jsonutil.JSONArr{jsonutil.JSONNum(1)},
			want: false,
		},
		{
			name: "non-empty container",
			arg: jsonutil.JSONContainer{
				"foo": &v,
			},
			want: false,
		},
		{
			name: "number",
			arg:  jsonutil.JSONNum(0),
			want: false,
		},
		{
			name: "bool",
			arg:  jsonutil.JSONBool(false),
			want: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := IsNil(test.arg)
			if err != nil {
				t.Fatalf("IsNil(%v) returned unexpected error %v", test.arg, err)
			}

			gotNot, err := IsNotNil(test.arg)
			if err != nil {
				t.Fatalf("IsNil(%v) returned unexpected error %v", test.arg, err)
			}

			if got != test.want {
				t.Errorf("IsNil(%v) = %v, want %v", test.arg, got, test.want)
			}

			if gotNot != !test.want {
				t.Errorf("IsNotNil(%v) = %v, want %v", test.arg, gotNot, !test.want)
			}
		})
	}
}

func TestType(t *testing.T) {
	var v jsonutil.JSONToken = jsonutil.JSONNum(0)
	tests := []struct {
		name      string
		arg       jsonutil.JSONToken
		want      jsonutil.JSONStr
		wantError bool
	}{
		{
			name: "nil",
			arg:  nil,
			want: jsonutil.JSONStr("null"),
		},
		{
			name: "JSON token interface",
			arg:  jsonutil.JSONToken(nil),
			want: jsonutil.JSONStr("null"),
		},
		{
			name: "number",
			arg:  jsonutil.JSONNum(0),
			want: jsonutil.JSONStr("number"),
		},
		{
			name: "bool",
			arg:  jsonutil.JSONBool(false),
			want: jsonutil.JSONStr("bool"),
		},
		{
			name: "empty string",
			arg:  jsonutil.JSONStr(""),
			want: jsonutil.JSONStr("string"),
		},
		{
			name: "non-empty string",
			arg:  jsonutil.JSONStr("foo"),
			want: jsonutil.JSONStr("string"),
		},
		{
			name: "empty array",
			arg:  jsonutil.JSONArr{},
			want: jsonutil.JSONStr("array"),
		},
		{
			name: "non-empty array",
			arg:  jsonutil.JSONArr{jsonutil.JSONNum(1)},
			want: jsonutil.JSONStr("array"),
		},
		{
			name: "empty container",
			arg:  jsonutil.JSONContainer{},
			want: jsonutil.JSONStr("container"),
		},
		{
			name: "non-empty container",
			arg: jsonutil.JSONContainer{
				"foo": &v,
			},
			want: jsonutil.JSONStr("container"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Type(test.arg)
			if err != nil && !test.wantError {
				t.Fatalf("Type(%v) returned unexpected error %v.", test.arg, err)
			}
			if err == nil && test.wantError {
				t.Fatalf("Type(%v) expected error but did not receive one.", test.arg)
			}
			if !cmp.Equal(got, test.want) && !test.wantError {
				t.Errorf("Type(%v) = %s, got %s", test.arg, test.want, got)
			}
		})
	}
}

func TestSubStr(t *testing.T) {
	tests := []struct {
		name  string
		in    jsonutil.JSONStr
		start jsonutil.JSONNum
		end   jsonutil.JSONNum
		want  jsonutil.JSONStr
	}{
		{
			name:  "simple",
			in:    jsonutil.JSONStr("test"),
			start: jsonutil.JSONNum(1),
			end:   jsonutil.JSONNum(3),
			want:  jsonutil.JSONStr("es"),
		},
		{
			name:  "full string",
			in:    jsonutil.JSONStr("test"),
			start: jsonutil.JSONNum(0),
			end:   jsonutil.JSONNum(4),
			want:  jsonutil.JSONStr("test"),
		},
		{
			name:  "end index bigger than string size",
			in:    jsonutil.JSONStr("test"),
			start: jsonutil.JSONNum(0),
			end:   jsonutil.JSONNum(10),
			want:  jsonutil.JSONStr("test"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := SubStr(test.in, test.start, test.end)
			if err != nil {
				t.Fatalf("Test %s returned unexpected error %v", test.name, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("SubStr(%v, %v, %v) = %v, want %v", test.in, test.start, test.end, got, test.want)
			}
		})
	}
}

func TestSubStrErrs(t *testing.T) {
	tests := []struct {
		name  string
		in    jsonutil.JSONStr
		start jsonutil.JSONNum
		end   jsonutil.JSONNum
	}{
		{
			name:  "start and end index bigger than string size",
			in:    jsonutil.JSONStr("test"),
			start: jsonutil.JSONNum(10),
			end:   jsonutil.JSONNum(15),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := SubStr(test.in, test.start, test.end)
			if err == nil {
				t.Errorf("SubStr(%v, %v, %v) = %v, want error", test.in, test.start, test.end, got)
			}
		})
	}
}

func TestStrJoin(t *testing.T) {
	tests := []struct {
		name string
		sep  jsonutil.JSONStr
		arg  []jsonutil.JSONToken
		want jsonutil.JSONToken
	}{
		{
			name: "multiple arguments",
			sep:  jsonutil.JSONStr("."),
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("abc"), jsonutil.JSONStr("def"), jsonutil.JSONStr("ghi")},
			want: jsonutil.JSONStr("abc.def.ghi"),
		},
		{
			name: "single argument",
			sep:  jsonutil.JSONStr("."),
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("GET")},
			want: jsonutil.JSONStr("GET"),
		},
		{
			name: "empty argument",
			sep:  jsonutil.JSONStr("."),
			arg:  []jsonutil.JSONToken{},
			want: jsonutil.JSONStr(""),
		},
		{
			name: "multiple arguments including a nil",
			sep:  jsonutil.JSONStr("."),
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("abc"), jsonutil.JSONToken(nil)},
			want: jsonutil.JSONStr("abc"),
		},
		{
			name: "multiple arguments including a nil within list",
			sep:  jsonutil.JSONStr("."),
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("abc"), jsonutil.JSONToken(nil), jsonutil.JSONStr("def")},
			want: jsonutil.JSONStr("abc.def"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := StrJoin(test.sep, test.arg...)
			if err != nil {
				t.Fatalf("Test %s returned unexpected error %v", test.name, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("StrJoin(%v) = %v, want %v", test.arg, got, test.want)
			}
		})
	}
}

func TestStrCat(t *testing.T) {
	tests := []struct {
		name string
		arg  []jsonutil.JSONToken
		want jsonutil.JSONToken
	}{
		{
			name: "multiple arguments",
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("abc"), jsonutil.JSONStr("def"), jsonutil.JSONStr("ghi")},
			want: jsonutil.JSONStr("abcdefghi"),
		},
		{
			name: "single argument",
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("GET")},
			want: jsonutil.JSONStr("GET"),
		},
		{
			name: "empty argument",
			arg:  []jsonutil.JSONToken{},
			want: jsonutil.JSONStr(""),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := StrCat(test.arg...)
			if err != nil {
				t.Fatalf("Test %s returned unexpected error %v", test.name, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("StrCat(%v) = %v, want %v", test.arg, got, test.want)
			}
		})
	}
}

func TestParseTime(t *testing.T) {
	tests := []struct {
		name, format, date, want string
		wantErr                  bool
	}{
		{
			name:   "matching format and date",
			format: "2006_01_02 15:04:05.000",
			date:   "2019_04_10 10:20:49.123",
			want:   "2019-04-10T10:20:49.123Z",
		},
		{
			name:   "unpadded day and month",
			format: "1/2/06",
			date:   "1/8/17",
			want:   "2017-01-08T00:00:00Z",
		},
		{
			name:   "unpadded two digit day and month",
			format: "1/2/06",
			date:   "11/18/17",
			want:   "2017-11-18T00:00:00Z",
		},
		{
			name:   "padded one digit year",
			format: "1/2/06",
			date:   "11/8/07",
			want:   "2007-11-08T00:00:00Z",
		},
		{
			name:    "date format mismatch",
			format:  "2006_01_02",
			date:    "2019 04 10",
			wantErr: true,
		},
		{
			name:    "bad date",
			format:  "2006_01_02 15:04:05.000",
			date:    "2019_0",
			wantErr: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ParseTime(jsonutil.JSONStr(test.format), jsonutil.JSONStr(test.date))
			if err != nil != test.wantErr {
				var wantErrStr string
				if !test.wantErr {
					wantErrStr = "no "
				}
				t.Errorf("ParseTime(%s, %s) expected %serror, got %v %v", test.format, test.date, wantErrStr, got, err)
			}

			if string(got) != test.want {
				t.Errorf("ParseTime(%s, %s) = %s, want %s", test.format, test.date, got, test.want)
			}
		})
	}
}

func TestMultiFormatParseTime(t *testing.T) {
	tests := []struct {
		name, date, want string
		format           []string
		wantErr          bool
	}{
		{
			name:   "first match",
			format: []string{"2006_01_02 15:04:05.000", "1/2/06", "2006_01_02"},
			date:   "2019_04_10 10:20:49.123",
			want:   "2019-04-10T10:20:49.123Z",
		},
		{
			name:   "second match",
			format: []string{"2006_01_02 15:04:05.000", "1/2/06", "2006_01_02"},
			date:   "1/8/17",
			want:   "2017-01-08T00:00:00Z",
		},
		{
			name:    "date format mismatch",
			format:  []string{"2006_01_02 15:04:05.000", "1/2/06", "2006_01_02"},
			date:    "2019 04 10",
			wantErr: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			var arr jsonutil.JSONArr
			for _, s := range test.format {
				arr = append(arr, jsonutil.JSONStr(s))
			}
			got, err := MultiFormatParseTime(arr, jsonutil.JSONStr(test.date))
			if err != nil != test.wantErr {
				var wantErrStr string
				if !test.wantErr {
					wantErrStr = "no "
				}
				t.Errorf("MultiFormatParseTime(%s, %s) expected %serror, got %v %v", test.format, test.date, wantErrStr, got, err)
			}

			if string(got) != test.want {
				t.Errorf("MultiFormatParseTime(%s, %s) = %s, want %s", test.format, test.date, got, test.want)
			}
		})
	}
}

func TestSplitTime(t *testing.T) {
	tests := []struct {
		name, format, date string
		want               []string
	}{
		{
			name:   "only year",
			format: "2006",
			date:   "2019",
			want:   []string{"2019", "1", "1", "0", "0", "0", "0"},
		},
		{
			name:   "only month",
			format: "01",
			date:   "05",
			want:   []string{"0", "5", "1", "0", "0", "0", "0"},
		},
		{
			name:   "only day",
			format: "02",
			date:   "28",
			want:   []string{"0", "1", "28", "0", "0", "0", "0"},
		},
		{
			name:   "date",
			format: "2006-01-02",
			date:   "2019-05-28",
			want:   []string{"2019", "5", "28", "0", "0", "0", "0"},
		},
		{
			name:   "datetime",
			format: "2006-01-02 15:04:05.000",
			date:   "2019-05-28 13:48:25.123",
			want:   []string{"2019", "5", "28", "13", "48", "25", "123000000"},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			ja, err := SplitTime(jsonutil.JSONStr(test.format), jsonutil.JSONStr(test.date))
			if err != nil {
				t.Fatalf("SplitTime(%s, %s) returned error: %v", test.format, test.date, err)
			}
			got := []jsonutil.JSONToken(ja)
			if len(got) != len(test.want) {
				t.Fatalf("SplitTime(%s, %s) returned %d components, want %d", test.format, test.date, len(got), len(test.want))
			}
			for i := range got {
				c, ok := got[i].(jsonutil.JSONStr)
				if !ok {
					t.Errorf("SplitTime(%s, %s) = %dth component %v in wrong type, want JSONStr", test.format, test.date, i+1, got[i])
				}
				if string(c) != test.want[i] {
					t.Errorf("SplitTime(%s, %s) = %dth component %s, want %s", test.format, test.date, i+1, c, test.want[i])
				}
			}
		})
	}
}

func TestReformatTime(t *testing.T) {
	tests := []struct {
		name, inFormat, outFormat, date, want string
		wantErr                               bool
	}{
		{
			name:      "identity operation",
			inFormat:  "2006_01_02 15:04:05.000",
			date:      "2019_04_10 10:20:49.123",
			outFormat: "2006_01_02 15:04:05.000",
			want:      "2019_04_10 10:20:49.123",
		},
		{
			name:      "less precise output format",
			inFormat:  "2006_01_02 15:04:05.000",
			date:      "2019_04_10 10:20:49.123",
			outFormat: "2006_01_02",
			want:      "2019_04_10",
		},
		{
			name:      "date format mismatch",
			inFormat:  "2006_01_02",
			date:      "2019 04 10",
			outFormat: "2006_01_02",
			wantErr:   true,
		},
		{
			name:      "python format to python format",
			inFormat:  "%Y-%m-%dT%H:%M:%S%z",
			date:      "2019-04-05T05:06:07+10:00",
			outFormat: "%e/%m/%y:%H-%M-%S",
			want:      "5/04/19:05-06-07",
		},
		{
			name:      "python format to go format",
			inFormat:  "%Y-%m-%dT%H:%M:%S%z",
			date:      "2019-04-05T05:06:07+10:00",
			outFormat: "2/01/06:15-04-05",
			want:      "5/04/19:05-06-07",
		},
		{
			name:      "go format to python format",
			inFormat:  "2006_01_02 15:04:05.000",
			date:      "2019_04_10 10:20:49.123",
			outFormat: "%e/%m/%y:%H-%M-%S",
			want:      "10/04/19:10-20-49",
		},
		{
			name:     "implicitly illegal python format",
			inFormat: "%bu%H-%M-%s",
			date:     "Febu16:17:28",
			wantErr:  true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ReformatTime(jsonutil.JSONStr(test.inFormat), jsonutil.JSONStr(test.date), jsonutil.JSONStr(test.outFormat))
			if err != nil != test.wantErr {
				var wantErrStr string
				if !test.wantErr {
					wantErrStr = "no "
				}
				t.Errorf("ReformatTime(%s, %s, %s) expected %serror, got %v %v", test.inFormat, test.date, test.outFormat, wantErrStr, got, err)
			}

			if string(got) != test.want {
				t.Errorf("ReformatTime(%s, %s, %s) = %s, want %s", test.inFormat, test.date, test.outFormat, got, test.want)
			}
		})
	}
}

func TestCurrentTime(t *testing.T) {
	tests := []struct {
		name, inFormat, timeZone, wantRegex string
		wantError                           bool
	}{
		{
			name:      "date and empty timezone",
			inFormat:  "2006-01-02",
			timeZone:  "",
			wantRegex: "^[0-9]{4}-[0-9]{2}-[0-9]{2}$",
		},
		{
			name:      "date, time and empty timezone",
			inFormat:  "2006-01-02 03:04:05-0700 MST",
			timeZone:  "",
			wantRegex: "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}[+-][0-9]{4} UTC$",
		},
		{
			name:      "date and timezone",
			inFormat:  "2006-01-02 MST",
			timeZone:  "Europe/Berlin",
			wantRegex: "^[0-9]{4}-[0-9]{2}-[0-9]{2} CE[S]?T$",
		},
		{
			name:      "date, time and timezone",
			inFormat:  "2006-01-02 03:04:05-0700 MST",
			timeZone:  "America/Chicago",
			wantRegex: "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}[+-][0-9]{4} C[S|D]T$",
		},
		{
			name:      "invalid layout",
			inFormat:  "InvalidValue",
			wantRegex: "InvalidValue",
		},
		{
			name:      "empty layout",
			inFormat:  "",
			timeZone:  "Europe/Berlin",
			wantRegex: "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$",
		},
		{
			name:      "Invalid IANA Time Zone value",
			inFormat:  "",
			timeZone:  "Europe/Unknown",
			wantError: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			wantRegExp := regexp.MustCompile(test.wantRegex)
			got, err := CurrentTime(jsonutil.JSONStr(test.inFormat), jsonutil.JSONStr(test.timeZone))
			if err != nil && !test.wantError {
				t.Errorf("CurrentTime(%s) = Error - %v", test.inFormat, err)
			}
			if !wantRegExp.MatchString(string(got)) && !test.wantError {
				t.Errorf("CurrentTime(%s) = got %s, doesn't conform to %s", test.inFormat, got, wantRegExp)
			}
			if err == nil && test.wantError {
				t.Errorf("Received unexpected error (%v)", err)
			}
		})
	}
}

func TestListOf(t *testing.T) {
	tests := []struct {
		name string
		arg  []jsonutil.JSONToken
	}{
		{
			name: "multiple arguments",
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("abc"), jsonutil.JSONStr("def"), jsonutil.JSONStr("ghi")},
		},
		{
			name: "single argument",
			arg:  []jsonutil.JSONToken{jsonutil.JSONStr("GET")},
		},
		{
			name: "empty argument",
			arg:  []jsonutil.JSONToken{},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			want := jsonutil.JSONArr(test.arg)
			got, err := ListOf(test.arg...)
			if err != nil {
				t.Fatalf("Test %s returned unexpected error %v", test.name, err)
			}
			if !cmp.Equal(got, want) {
				t.Errorf("ListOf(%v) = %v, want %v", test.arg, got, want)
			}
		})
	}
}

func TestListCat(t *testing.T) {
	tests := []struct {
		name string
		arg  []jsonutil.JSONArr
		want jsonutil.JSONArr
	}{
		{
			name: "no arguments",
			arg:  []jsonutil.JSONArr{},
			want: jsonutil.JSONArr{},
		},
		{
			name: "one empty argument",
			arg:  []jsonutil.JSONArr{{}},
			want: jsonutil.JSONArr{},
		},
		{
			name: "one argument",
			arg:  []jsonutil.JSONArr{{jsonutil.JSONStr("foo")}},
			want: jsonutil.JSONArr{jsonutil.JSONStr("foo")},
		},
		{
			name: "multiple arguments",
			arg:  []jsonutil.JSONArr{{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")}, {jsonutil.JSONStr("baz"), jsonutil.JSONStr("quip")}},
			want: jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar"), jsonutil.JSONStr("baz"), jsonutil.JSONStr("quip")},
		},
		{
			name: "multiple arguments with empties",
			arg:  []jsonutil.JSONArr{{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar")}, {}, {jsonutil.JSONStr("baz"), jsonutil.JSONStr("quip")}},
			want: jsonutil.JSONArr{jsonutil.JSONStr("foo"), jsonutil.JSONStr("bar"), jsonutil.JSONStr("baz"), jsonutil.JSONStr("quip")},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ListCat(test.arg...)
			if err != nil {
				t.Fatalf("ListCat(%v) returned unexpected error %v", test.arg, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ListCat(%v) = %v, want %v", test.arg, got, test.want)
			}
		})
	}
}

func TestUnionBy(t *testing.T) {
	tests := []struct {
		name  string
		items jsonutil.JSONArr
		keys  []jsonutil.JSONStr
		want  jsonutil.JSONArr
	}{
		{
			name:  "one item no keys",
			items: mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
			keys:  []jsonutil.JSONStr{},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "multiple items no keys",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}]`), t),
			keys:  []jsonutil.JSONStr{},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "one item one key",
			items: mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
			keys:  []jsonutil.JSONStr{"id"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "one item one non-existent key",
			items: mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
			keys:  []jsonutil.JSONStr{"nonexistentkey"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "multiple items one occasionally non-existent key",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2, "foo": "hello"}]`), t),
			keys:  []jsonutil.JSONStr{"foo"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2, "foo": "hello"}]`), t),
		},
		{
			name:  "multiple items with one often non-existent key",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}, {"id": 3, "foo": "hello"}]`), t),
			keys:  []jsonutil.JSONStr{"foo"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 3, "foo": "hello"}]`), t),
		},
		{
			name:  "multiple items with shared key",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}, {"id": 3}, {"id": 3}]`), t),
			keys:  []jsonutil.JSONStr{"id"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}, {"id": 3}]`), t),
		},
		{
			name:  "multiple items with multiple keys",
			items: mustParseArray(json.RawMessage(`[{"id": 1, "foo": "hello"}, {"id": 2, "foo": "hello"}, {"id": 3, "foo": "world"}, {"id": 3, "foo": "world"}, {"id": 4, "foo": "world"}]`), t),
			keys:  []jsonutil.JSONStr{"id", "foo"},
			want:  mustParseArray(json.RawMessage(`[{"id": 1, "foo": "hello"}, {"id": 2, "foo": "hello"}, {"id": 3, "foo": "world"}, {"id": 4, "foo": "world"}]`), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := UnionBy(test.items, test.keys...)
			if err != nil {
				t.Fatalf("UnionBy(%v, %v) returned unexpected error %v", test.items, test.keys, err)
			}

			sliceLessFn := func(a, b jsonutil.JSONToken) bool {
				t.Helper()
				ah, err := jsonutil.Hash(a, false)
				if err != nil {
					t.Fatalf("error hashing %v: %v", a, err)
				}
				bh, err := jsonutil.Hash(b, false)
				if err != nil {
					t.Fatalf("error hashing %v: %v", b, err)
				}
				return hex.EncodeToString(ah) < hex.EncodeToString(bh)
			}

			mapLessFn := func(a, b string) bool {
				return a < b
			}

			if !cmp.Equal(got, test.want, cmpopts.SortSlices(sliceLessFn), cmpopts.SortMaps(mapLessFn)) {
				t.Errorf("UnionBy(%v, %v) = %v, want %v", test.items, test.keys, got, test.want)
			}
		})
	}
}

func TestUnique(t *testing.T) {
	tests := []struct {
		name  string
		items jsonutil.JSONArr
		want  jsonutil.JSONArr
	}{
		{
			name:  "nil",
			items: nil,
			want:  mustParseArray(json.RawMessage(`[]`), t),
		},
		{
			name:  "no items",
			items: mustParseArray(json.RawMessage(`[]`), t),
			want:  mustParseArray(json.RawMessage(`[]`), t),
		},
		{
			name:  "one item",
			items: mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "multiple items all unique",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}]`), t),
			want:  mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 2}]`), t),
		},
		{
			name:  "multiple items some unique - same key order",
			items: mustParseArray(json.RawMessage(`[{"id": 1}, {"id": 1}]`), t),
			want:  mustParseArray(json.RawMessage(`[{"id": 1}]`), t),
		},
		{
			name:  "multiple items some unique - different key order",
			items: mustParseArray(json.RawMessage(`[{"key": "val", "id": 1}, {"id": 1, "key": "val"}]`), t),
			want:  mustParseArray(json.RawMessage(`[{"id": 1, "key": "val"}]`), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Unique(test.items)
			if err != nil {
				t.Fatalf("Unique(%v) returned unexpected error %v", test.items, err)
			}

			sliceLessFn := func(a, b jsonutil.JSONToken) bool {
				t.Helper()
				ah, err := jsonutil.Hash(a, false)
				if err != nil {
					t.Fatalf("error hashing %v: %v", a, err)
				}
				bh, err := jsonutil.Hash(b, false)
				if err != nil {
					t.Fatalf("error hashing %v: %v", b, err)
				}
				return hex.EncodeToString(ah) < hex.EncodeToString(bh)
			}

			mapLessFn := func(a, b string) bool {
				return a < b
			}

			if !cmp.Equal(got, test.want, cmpopts.SortSlices(sliceLessFn), cmpopts.SortMaps(mapLessFn)) {
				t.Errorf("Unique(%v) = %v, want %v", test.items, got, test.want)
			}
		})
	}
}

func TestMatchesRegex(t *testing.T) {
	tests := []struct {
		name  string
		str   jsonutil.JSONStr
		regex jsonutil.JSONStr
		want  jsonutil.JSONBool
	}{
		{
			name:  "matches",
			str:   jsonutil.JSONStr("123"),
			regex: jsonutil.JSONStr("\\d+"),
			want:  jsonutil.JSONBool(true),
		},
		{
			name:  "not matches",
			str:   jsonutil.JSONStr("abc"),
			regex: jsonutil.JSONStr("\\d+"),
			want:  jsonutil.JSONBool(false),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := MatchesRegex(test.str, test.regex)
			if err != nil {
				t.Fatalf("MatchesRegex(%v, %v) returned unexpected error %v", test.str, test.regex, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("MatchesRegex(%v, %v) = %v, want %v", test.str, test.regex, got, test.want)
			}
		})
	}
}

func TestParseInt(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		want jsonutil.JSONNum
	}{
		{
			name: "single test",
			in:   jsonutil.JSONStr("123"),
			want: jsonutil.JSONNum(123),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ParseInt(test.in)
			if err != nil {
				t.Fatalf("ParseInt(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ParseInt(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestParseFloat(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		want jsonutil.JSONNum
	}{
		{
			name: "with floating point",
			in:   jsonutil.JSONStr("123.123"),
			want: jsonutil.JSONNum(123.123),
		},
		{
			name: "without floating point",
			in:   jsonutil.JSONStr("123"),
			want: jsonutil.JSONNum(123),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ParseFloat(test.in)
			if err != nil {
				t.Fatalf("ParseFloat(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ParseFloat(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestStrFmt(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONToken
		fmt  jsonutil.JSONStr
		want jsonutil.JSONStr
	}{
		{
			name: "boolean",
			in:   jsonutil.JSONBool(true),
			fmt:  jsonutil.JSONStr("%v"),
			want: jsonutil.JSONStr("true"),
		},
		{
			name: "string",
			in:   jsonutil.JSONStr("this is a string"),
			fmt:  jsonutil.JSONStr("%s"),
			want: jsonutil.JSONStr("this is a string"),
		},
		{
			name: "quoted string",
			in:   jsonutil.JSONStr("this is a string"),
			fmt:  jsonutil.JSONStr("%q"),
			want: jsonutil.JSONStr(`"this is a string"`),
		},
		{
			name: "integer",
			in:   jsonutil.JSONNum(123),
			fmt:  jsonutil.JSONStr("%d"),
			want: jsonutil.JSONStr("123"),
		},
		{
			name: "binary integer",
			in:   jsonutil.JSONNum(3),
			fmt:  jsonutil.JSONStr("%b"),
			want: jsonutil.JSONStr("11"),
		},
		{
			name: "float",
			in:   jsonutil.JSONNum(123.456),
			fmt:  jsonutil.JSONStr("%f"),
			want: jsonutil.JSONStr("123.456000"),
		},
		{
			name: "array",
			in:   jsonutil.JSONArr{jsonutil.JSONStr("a"), jsonutil.JSONStr("b")},
			fmt:  jsonutil.JSONStr("%v"),
			want: jsonutil.JSONStr(`["a","b"]`),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := StrFmt(test.fmt, test.in)
			if err != nil {
				t.Fatalf("StrFmt(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("StrFmt(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestToLower(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		want jsonutil.JSONStr
	}{
		{
			name: "english",
			in:   jsonutil.JSONStr("ABCdef"),
			want: jsonutil.JSONStr("abcdef"),
		},
		{
			name: "not english",
			in:   jsonutil.JSONStr("АБСдеф"),
			want: jsonutil.JSONStr("абсдеф"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ToLower(test.in)
			if err != nil {
				t.Fatalf("ToLower(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ToLower(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestToUpper(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		want jsonutil.JSONStr
	}{
		{
			name: "english",
			in:   jsonutil.JSONStr("abcDEF"),
			want: jsonutil.JSONStr("ABCDEF"),
		},
		{
			name: "not english",
			in:   jsonutil.JSONStr("АБСдеф"),
			want: jsonutil.JSONStr("АБСДЕФ"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ToUpper(test.in)
			if err != nil {
				t.Fatalf("ToUpper(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ToUpper(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestDebugString(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONToken
		want jsonutil.JSONStr
	}{
		{
			name: "boolean",
			in:   jsonutil.JSONBool(true),
			want: jsonutil.JSONStr("true"),
		},
		{
			name: "string",
			in:   jsonutil.JSONStr("this is a string"),
			want: jsonutil.JSONStr("this is a string"),
		},
		{
			name: "integer",
			in:   jsonutil.JSONNum(123),
			want: jsonutil.JSONStr("123"),
		},
		{
			name: "float",
			in:   jsonutil.JSONNum(123.456),
			want: jsonutil.JSONStr("123.456"),
		},
		{
			name: "array",
			in:   jsonutil.JSONArr{jsonutil.JSONStr("a"), jsonutil.JSONStr("b")},
			want: jsonutil.JSONStr(`["a","b"]`),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := DebugString(test.in)
			if err != nil {
				t.Fatalf("DebugString(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("DebugString(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestMergeJSON(t *testing.T) {
	tests := []struct {
		name            string
		in              jsonutil.JSONArr
		want            jsonutil.JSONToken
		overwriteArrays jsonutil.JSONBool
	}{
		{
			name: "empty array",
			in:   mustParseArray(json.RawMessage(`[]`), t),
			want: nil,
		},
		{
			name: "empty objects",
			in:   mustParseArray(json.RawMessage(`[{}]`), t),
			want: mustParseContainer(json.RawMessage(`{}`), t),
		},
		{
			name: "no matching keys",
			in:   mustParseArray(json.RawMessage(`[{"key1":"val1"}, {"key2":"val2"}]`), t),
			want: mustParseContainer(json.RawMessage(`{"key1":"val1", "key2":"val2"}`), t),
		},
		{
			name: "primitives only",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1"}, {"key":"val2"}]`), t),
			want: mustParseContainer(json.RawMessage(`{"key":"val2"}`), t),
		},
		{
			name: "primitives only",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1"}, {"key":"val2"}, {"key":"val3"}]`), t),
			want: mustParseContainer(json.RawMessage(`{"key":"val3"}`), t),
		},
		{
			name: "arrays only",
			in:   mustParseArray(json.RawMessage(`[{"key":["val1"]}, {"key":["val2"]}]`), t),
			want: mustParseContainer(json.RawMessage(`{"key":["val1","val2"]}`), t),
		},
		{
			name: "primitives and arrays",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1", "arr": ["aval1"]}, {"key":"val2", "arr": ["aval2"]}]`), t),
			want: mustParseContainer(json.RawMessage(`{"key":"val2", "arr": ["aval1", "aval2"]}`), t),
		},
		{
			name: "complex type in array",
			in:   mustParseArray(json.RawMessage(`[{"arr": [{"key":"aval1"}]}, {"arr": [{"key":"aval2"}]}]`), t),
			want: mustParseContainer(json.RawMessage(`{"arr": [{"key":"aval1"}, {"key":"aval2"}]}`), t),
		},
		{
			name:            "array overwrite",
			in:              mustParseArray(json.RawMessage(`[{"arr": [{"key":"aval1"}]}, {"arr": [{"key":"aval2"}]}]`), t),
			want:            mustParseContainer(json.RawMessage(`{"arr": [{"key":"aval2"}]}`), t),
			overwriteArrays: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := MergeJSON(test.in, test.overwriteArrays)
			if err != nil {
				t.Fatalf("MergeJSON(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("MergeJSON(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}

func TestRange(t *testing.T) {
	tests := []struct {
		name  string
		start jsonutil.JSONNum
		end   jsonutil.JSONNum
		want  jsonutil.JSONArr
	}{
		{
			name:  "normal case",
			start: jsonutil.JSONNum(2),
			end:   jsonutil.JSONNum(5),
			want:  mustParseArray(json.RawMessage("[2, 3, 4]"), t),
		},
		{
			name:  "start non-integer",
			start: jsonutil.JSONNum(1.5),
			end:   jsonutil.JSONNum(5),
			want:  mustParseArray(json.RawMessage("[1.5, 2.5, 3.5, 4.5]"), t),
		},
		{
			name:  "start bigger than end",
			start: jsonutil.JSONNum(5),
			end:   jsonutil.JSONNum(2),
			want:  mustParseArray(json.RawMessage("[5, 4, 3]"), t),
		},
		{
			name:  "negative range",
			start: jsonutil.JSONNum(-2),
			end:   jsonutil.JSONNum(2),
			want:  mustParseArray(json.RawMessage("[-2, -1, 0, 1]"), t),
		},
		{
			name:  "start bigger than end",
			start: jsonutil.JSONNum(5),
			end:   jsonutil.JSONNum(2),
			want:  mustParseArray(json.RawMessage("[5, 4, 3]"), t),
		},
		{
			name:  "zero start",
			start: jsonutil.JSONNum(0),
			end:   jsonutil.JSONNum(5),
			want:  mustParseArray(json.RawMessage("[0, 1, 2, 3, 4]"), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Range(test.start, test.end)
			if err != nil {
				t.Fatalf("Range(%v, %v) returned unexpected error %v", test.start, test.end, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("Range(%v, %v) = %v, want %v", test.start, test.end, got, test.want)
			}
		})
	}
}

func TestSortAndTakeTop(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONArr
		key  jsonutil.JSONStr
		desc jsonutil.JSONBool
		want jsonutil.JSONToken
	}{
		{
			name: "no elements",
			in:   mustParseArray(json.RawMessage(`[]`), t),
			key:  jsonutil.JSONStr("key"),
			desc: jsonutil.JSONBool(false),
			want: nil,
		},
		{
			name: "one only",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1"}]`), t),
			key:  jsonutil.JSONStr("key"),
			desc: jsonutil.JSONBool(false),
			want: mustParseContainer(json.RawMessage(`{"key":"val1"}`), t),
		},
		{
			name: "ascending",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1"}, {"key":"val2"}]`), t),
			key:  jsonutil.JSONStr("key"),
			desc: jsonutil.JSONBool(false),
			want: mustParseContainer(json.RawMessage(`{"key":"val1"}`), t),
		},
		{
			name: "descending",
			in:   mustParseArray(json.RawMessage(`[{"key":"val1"}, {"key":"val2"}]`), t),
			key:  jsonutil.JSONStr("key"),
			desc: jsonutil.JSONBool(true),
			want: mustParseContainer(json.RawMessage(`{"key":"val2"}`), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := SortAndTakeTop(test.in, test.key, test.desc)
			if err != nil {
				t.Fatalf("SortAndTakeTop(%v, %v, %v) returned unexpected error %v", test.in, test.key, test.desc, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("SortAndTakeTop(%v, %v, %v) = %v, want %v", test.in, test.key, test.desc, got, test.want)
			}
		})
	}
}

func TestSplit(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		sep  jsonutil.JSONStr
		want jsonutil.JSONArr
	}{
		{
			name: "simple",
			in:   jsonutil.JSONStr("a-b"),
			sep:  jsonutil.JSONStr("-"),
			want: jsonutil.JSONArr{jsonutil.JSONStr("a"), jsonutil.JSONStr("b")},
		},
		{
			name: "spaces",
			in:   jsonutil.JSONStr("a - b"),
			sep:  jsonutil.JSONStr("-"),
			want: jsonutil.JSONArr{jsonutil.JSONStr("a"), jsonutil.JSONStr("b")},
		},
		{
			name: "empty items",
			in:   jsonutil.JSONStr("a - - b"),
			sep:  jsonutil.JSONStr("-"),
			want: jsonutil.JSONArr{jsonutil.JSONStr("a"), jsonutil.JSONStr("b")},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := StrSplit(test.in, test.sep)
			if err != nil {
				t.Fatalf("StrSplit(%v, %v) returned unexpected error %v", test.in, test.sep, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("StrSplit(%v, %v) = %v, want %v", test.in, test.sep, got, test.want)
			}
		})
	}
}

func TestParseUnixTime(t *testing.T) {
	tests := []struct {
		name   string
		in     jsonutil.JSONNum
		unit   jsonutil.JSONStr
		format jsonutil.JSONStr
		tz     jsonutil.JSONStr
		want   jsonutil.JSONStr
	}{
		{
			name:   "s",
			in:     jsonutil.JSONNum(86400000),
			unit:   jsonutil.JSONStr("s"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "ms",
			in:     jsonutil.JSONNum(86400000000),
			unit:   jsonutil.JSONStr("ms"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "us",
			in:     jsonutil.JSONNum(86400000000000),
			unit:   jsonutil.JSONStr("us"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "ns",
			in:     jsonutil.JSONNum(86400000000000000),
			unit:   jsonutil.JSONStr("ns"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "S",
			in:     jsonutil.JSONNum(86400000),
			unit:   jsonutil.JSONStr("S"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "Ms",
			in:     jsonutil.JSONNum(86400000000),
			unit:   jsonutil.JSONStr("Ms"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "uS",
			in:     jsonutil.JSONNum(86400000000000),
			unit:   jsonutil.JSONStr("uS"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "NS",
			in:     jsonutil.JSONNum(86400000000000000),
			unit:   jsonutil.JSONStr("NS"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T00:00:00Z"),
		},
		{
			name:   "s - date only",
			in:     jsonutil.JSONNum(86400000),
			unit:   jsonutil.JSONStr("s"),
			tz:     jsonutil.JSONStr("UTC"),
			format: jsonutil.JSONStr("2006-01-02"),
			want:   jsonutil.JSONStr("1972-09-27"),
		},
		{
			name:   "different timezone",
			in:     jsonutil.JSONNum(86400000),
			unit:   jsonutil.JSONStr("s"),
			tz:     jsonutil.JSONStr("Europe/Paris"),
			format: jsonutil.JSONStr("2006-01-02T15:04:05.999999999Z07:00"),
			want:   jsonutil.JSONStr("1972-09-27T01:00:00+01:00"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := ParseUnixTime(test.unit, test.in, test.format, test.tz)
			if err != nil {
				t.Fatalf("ParseUnixTime(%v, %v, %v, %v) returned unexpected error %v", test.unit, test.in, test.format, test.tz, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("ParseUnixTime(%v, %v, %v, %v) = %v, want %v", test.unit, test.in, test.format, test.tz, got, test.want)
			}
		})
	}
}

func TestAnd(t *testing.T) {
	tests := []struct {
		name string
		args []jsonutil.JSONToken
		want jsonutil.JSONBool
	}{
		{
			name: "no args",
			args: []jsonutil.JSONToken{},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one true arg",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "one false arg",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(false)},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "multiple true args",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true), jsonutil.JSONBool(true), jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "multiple partly true args",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true), jsonutil.JSONBool(false), jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one nil",
			args: []jsonutil.JSONToken{nil},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one non-null non-boolean",
			args: []jsonutil.JSONToken{jsonutil.JSONStr("a random string")},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "multiple non-boolean",
			args: []jsonutil.JSONToken{nil, jsonutil.JSONStr("a random string")},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one true with null",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true), nil},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "non-boolean and true",
			args: []jsonutil.JSONToken{jsonutil.JSONNum(1), jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "multiple partly true values with null",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true), jsonutil.JSONBool(true), jsonutil.JSONBool(true), nil},
			want: jsonutil.JSONBool(false),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := And(test.args...)
			if err != nil {
				t.Fatalf("And(%v) returned unexpected error %v", test.args, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("And(%v) = %v, want %v", test.args, got, test.want)
			}
		})
	}
}

func TestOr(t *testing.T) {
	tests := []struct {
		name string
		args []jsonutil.JSONToken
		want jsonutil.JSONBool
	}{
		{
			name: "no args",
			args: []jsonutil.JSONToken{},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one true arg",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "one false arg",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(false)},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "multiple true args",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(true), jsonutil.JSONBool(true), jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "multiple false args",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(false), jsonutil.JSONBool(false), jsonutil.JSONBool(false)},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "multiple partly true args",
			args: []jsonutil.JSONToken{jsonutil.JSONBool(false), jsonutil.JSONBool(false), jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "one nil",
			args: []jsonutil.JSONToken{nil},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "one non-null non-boolean",
			args: []jsonutil.JSONToken{jsonutil.JSONStr("a random string")},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "multiple non-boolean",
			args: []jsonutil.JSONToken{nil, jsonutil.JSONStr("a random string")},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "one true with null",
			args: []jsonutil.JSONToken{nil, jsonutil.JSONBool(true)},
			want: jsonutil.JSONBool(true),
		},
		{
			name: "one false with null",
			args: []jsonutil.JSONToken{nil, jsonutil.JSONBool(false)},
			want: jsonutil.JSONBool(false),
		},
		{
			name: "multiple partly true values with null",
			args: []jsonutil.JSONToken{nil, jsonutil.JSONBool(false), jsonutil.JSONBool(false), jsonutil.JSONBool(true), jsonutil.JSONBool(false)},
			want: jsonutil.JSONBool(true),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Or(test.args...)
			if err != nil {
				t.Fatalf("Or(%v) returned unexpected error %v", test.args, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("Or(%v) = %v, want %v", test.args, got, test.want)
			}
		})
	}
}

func TestUnnestArrays(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONContainer
		want jsonutil.JSONArr
	}{
		{
			name: "arrays",
			in:   mustParseContainer(json.RawMessage(`{"key1":[{"a": "z"}, {"b": "y"}], "key2": [{"c": "x"}], "key3": []}`), t),
			want: jsonutil.JSONArr{
				mustParseContainer(json.RawMessage(`{"k": "key1", "v":{"a": "z"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key1", "v":{"b": "y"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key2", "v":{"c": "x"}}`), t),
			},
		},
		{
			name: "non array",
			in:   mustParseContainer(json.RawMessage(`{"key1":{"a": "z"}, "key2": {"c": "x"}, "key3": {}}`), t),
			want: jsonutil.JSONArr{
				mustParseContainer(json.RawMessage(`{"k": "key1", "v":{"a": "z"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key2", "v":{"c": "x"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key3", "v":{}}`), t),
			},
		},
		{
			name: "mixed",
			in:   mustParseContainer(json.RawMessage(`{"key1":[{"a": "z"}, {"b": "y"}], "key2": {"c": "x"}, "key3": []}`), t),
			want: jsonutil.JSONArr{
				mustParseContainer(json.RawMessage(`{"k": "key1", "v":{"a": "z"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key1", "v":{"b": "y"}}`), t),
				mustParseContainer(json.RawMessage(`{"k": "key2", "v":{"c": "x"}}`), t),
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := UnnestArrays(test.in)
			if err != nil {
				t.Fatalf("UnnestArrays(%v) returned unexpected error %v", test.in, err)
			}
			if diff := cmp.Diff(test.want, got); diff != "" {
				t.Errorf("UnnestArrays(%v) -want/+got\n%s", test.in, diff)
			}
		})
	}
}

func TestSum(t *testing.T) {
	tests := []struct {
		name     string
		operands []jsonutil.JSONNum
		want     jsonutil.JSONNum
	}{
		{
			name: "no operands",
		},
		{
			name:     "one operand",
			operands: []jsonutil.JSONNum{-1337},
			want:     -1337,
		},
		{
			name:     "many operands",
			operands: []jsonutil.JSONNum{1337, -7.2111, 155.829, 0},
			want:     1485.6179,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Sum(test.operands...)
			if err != nil {
				t.Fatalf("Sum(%v) = error %v", test.operands, err)
			}
			if got != test.want {
				t.Errorf("Sum(%v) = %v want %v", test.operands, got, test.want)
			}
		})
	}
}

func TestSub(t *testing.T) {
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONNum
	}{
		{
			name: "positive operands",
			l:    10,
			r:    11,
			want: -1,
		},
		{
			name: "negative operands",
			l:    -10,
			r:    -11,
			want: 1,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Sub(test.l, test.r)
			if err != nil {
				t.Fatalf("Sub(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("Sub(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestMul(t *testing.T) {
	tests := []struct {
		name     string
		operands []jsonutil.JSONNum
		want     jsonutil.JSONNum
	}{
		{
			name: "no operands",
		},
		{
			name:     "one operand",
			operands: []jsonutil.JSONNum{-1337},
			want:     -1337,
		},
		{
			name:     "many operands with 0",
			operands: []jsonutil.JSONNum{1337, -7.2111, 155.829, 0, 9123.1112},
			want:     0,
		},
		{
			name:     "many operands",
			operands: []jsonutil.JSONNum{1337, -7.2111, 155.829},
			want:     1337 * -7.2111 * 155.829,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Mul(test.operands...)
			if err != nil {
				t.Fatalf("Mul(%v) = error %v", test.operands, err)
			}
			if got != test.want {
				t.Errorf("Mul(%v) = %v want %v", test.operands, got, test.want)
			}
		})
	}
}

func TestDiv(t *testing.T) {
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONNum
	}{
		{
			name: "positive operands",
			l:    10.0,
			r:    11.0,
			want: 10.0 / 11.0,
		},
		{
			name: "negative operands",
			l:    -20.0,
			r:    -11.0,
			want: -20.0 / -11.0,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Div(test.l, test.r)
			if err != nil {
				t.Fatalf("Div(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("Div(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestGt(t *testing.T) {
	epsilon := jsonutil.JSONNum(math.Nextafter(1.0, 2.0) - 1.0)
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONBool
	}{
		{
			name: "greater",
			l:    epsilon,
			r:    0,
			want: true,
		},
		{
			name: "equals",
			l:    0,
			r:    0,
			want: false,
		},
		{
			name: "lesser",
			l:    0,
			r:    epsilon,
			want: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Gt(test.l, test.r)
			if err != nil {
				t.Fatalf("Gt(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("Gt(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestGtEq(t *testing.T) {
	epsilon := jsonutil.JSONNum(math.Nextafter(1.0, 2.0) - 1.0)
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONBool
	}{
		{
			name: "greater",
			l:    epsilon,
			r:    0,
			want: true,
		},
		{
			name: "equals",
			l:    0,
			r:    0,
			want: true,
		},
		{
			name: "lesser",
			l:    0,
			r:    epsilon,
			want: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := GtEq(test.l, test.r)
			if err != nil {
				t.Fatalf("GtEq(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("GtEq(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestLt(t *testing.T) {
	epsilon := jsonutil.JSONNum(math.Nextafter(1.0, 2.0) - 1.0)
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONBool
	}{
		{
			name: "greater",
			l:    epsilon,
			r:    0,
			want: false,
		},
		{
			name: "equal",
			l:    0,
			r:    0,
			want: false,
		},
		{
			name: "lesser",
			l:    0,
			r:    epsilon,
			want: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Lt(test.l, test.r)
			if err != nil {
				t.Fatalf("Lt(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("Lt(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestLtEq(t *testing.T) {
	epsilon := jsonutil.JSONNum(math.Nextafter(1.0, 2.0) - 1.0)
	tests := []struct {
		name string
		l, r jsonutil.JSONNum
		want jsonutil.JSONBool
	}{
		{
			name: "greater",
			l:    epsilon,
			r:    0,
			want: false,
		},
		{
			name: "equals",
			l:    0,
			r:    0,
			want: true,
		},
		{
			name: "lesser",
			l:    0,
			r:    epsilon,
			want: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := LtEq(test.l, test.r)
			if err != nil {
				t.Fatalf("LtEq(%v, %v) = error %v", test.l, test.r, err)
			}
			if got != test.want {
				t.Errorf("LtEq(%v, %v) = %v want %v", test.l, test.r, got, test.want)
			}
		})
	}
}

func TestNot(t *testing.T) {
	tests := []struct {
		name string
		arg  jsonutil.JSONBool
		want jsonutil.JSONBool
	}{
		{
			name: "true",
			arg:  true,
			want: false,
		},
		{
			name: "false",
			arg:  false,
			want: true,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Not(test.arg)
			if err != nil {
				t.Fatalf("Not(%v) = error %v", test.arg, err)
			}
			if got != test.want {
				t.Errorf("Not(%v) = %v want %v", test.arg, got, test.want)
			}
		})
	}
}

func TestNotNonBoolean(t *testing.T) {
	var v jsonutil.JSONToken = jsonutil.JSONNum(0)
	tests := []struct {
		name string
		arg  jsonutil.JSONToken
		want jsonutil.JSONBool
	}{
		{
			name: "nil",
			arg:  jsonutil.JSONToken(nil),
			want: true,
		},
		{
			name: "not nil",
			arg:  jsonutil.JSONToken(jsonutil.JSONArr{jsonutil.JSONNum(1)}),
			want: false,
		},
		{
			name: "non-empty container",
			arg: jsonutil.JSONContainer{
				"foo": &v,
			},
			want: false,
		},
		{
			name: "number",
			arg:  jsonutil.JSONNum(0),
			want: false,
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Not(test.arg)
			if err != nil {
				t.Fatalf("Not(%v) = error %v", test.arg, err)
			}
			if got != test.want {
				t.Errorf("Not(%v) = %v want %v", test.arg, got, test.want)
			}
		})
	}
}
func TestVoid(t *testing.T) {
	tests := []struct {
		name     string
		operands []jsonutil.JSONToken
	}{
		{
			name: "no arguments",
		},
		{
			name:     "one argument",
			operands: []jsonutil.JSONToken{jsonutil.JSONNum(-1337)},
		},
		{
			name:     "many arguments",
			operands: []jsonutil.JSONToken{jsonutil.JSONStr("hail the V0ID"), nil, mustParseContainer(json.RawMessage(`{"a":"b"}`), t)},
		},
		{
			name:     "nil",
			operands: []jsonutil.JSONToken{nil},
		},
	}
	for _, test := range tests {
		t.Run(fmt.Sprintf("You send %s into the Void, the Void nils back", test.name), func(t *testing.T) {
			got, err := Void(test.operands...)
			if err != nil {
				t.Fatalf("Void(%v) = error %v", test.operands, err)
			}
			if got != nil {
				t.Errorf("Void(%v) = %v want nil", test.operands, got)
			}
		})
	}
}

func TestFlatten(t *testing.T) {
	tests := []struct {
		name  string
		input jsonutil.JSONArr
		want  jsonutil.JSONArr
	}{
		{
			name:  "empty array",
			input: jsonutil.JSONArr{},
			want:  jsonutil.JSONArr{},
		},
		{
			name:  "not nested array",
			input: mustParseArray(json.RawMessage(`[1, 2, 3]`), t),
			want:  mustParseArray(json.RawMessage(`[1, 2, 3]`), t),
		},
		{
			name:  "deeply nested array",
			input: mustParseArray(json.RawMessage(`[1, [2, 3, [4, 5], [6, [7, 8]]]]`), t),
			want:  mustParseArray(json.RawMessage(`[1, 2, 3, 4, 5, 6, 7, 8]`), t),
		},
		{
			name:  "deeply nested array with object",
			input: mustParseArray(json.RawMessage(`[1, [2, 3, [4, 5], [], [{"k": [7, 8]}]]]`), t),
			want:  mustParseArray(json.RawMessage(`[1, 2, 3, 4, 5, {"k": [7, 8]}]`), t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Flatten(test.input)
			if err != nil {
				t.Fatalf("Flatten(%v) = error %v", test.input, err)
			}
			if diff := cmp.Diff(test.want, got); diff != "" {
				t.Errorf("Flatten(%v) -want/+got:\n%s", test.input, diff)
			}
		})
	}
}

func TestIntHash(t *testing.T) {
	tests := []struct {
		name  string
		input jsonutil.JSONToken
		want  jsonutil.JSONNum
	}{
		{
			name:  "string",
			input: jsonutil.JSONStr("test"),
			want:  jsonutil.JSONNum(1.776318759e+09),
		},
		{
			name:  "num",
			input: jsonutil.JSONNum(123),
			want:  jsonutil.JSONNum(6.12871997e+08),
		},
		{
			name:  "array",
			input: mustParseArray(json.RawMessage(`[1, 2, 3]`), t),
			want:  jsonutil.JSONNum(2.706119551e+09),
		},
		{
			name:  "object",
			input: mustParseContainer(json.RawMessage(`{"a":"b"}`), t),
			want:  jsonutil.JSONNum(4.026688896e+09),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := IntHash(test.input)
			if err != nil {
				t.Fatalf("IntHash(%v) = error %v", test.input, err)
			}
			if got != test.want {
				t.Errorf("IntHash(%v) = %v want %v", test.input, got, test.want)
			}
		})
	}
}

func TestTrim(t *testing.T) {
	tests := []struct {
		name string
		in   jsonutil.JSONStr
		want jsonutil.JSONStr
	}{
		{
			name: "leading and trailing",
			in:   jsonutil.JSONStr(" hello "),
			want: jsonutil.JSONStr("hello"),
		},
		{
			name: "just trailing",
			in:   jsonutil.JSONStr("world.  "),
			want: jsonutil.JSONStr("world."),
		},
		{
			name: "no change",
			in:   jsonutil.JSONStr("hello world"),
			want: jsonutil.JSONStr("hello world"),
		},
		{
			name: "new line",
			in:   jsonutil.JSONStr(" hello world\n\n"),
			want: jsonutil.JSONStr("hello world"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := Trim(test.in)
			if err != nil {
				t.Fatalf("Trim(%v) returned unexpected error %v", test.in, err)
			}
			if !cmp.Equal(got, test.want) {
				t.Errorf("Trim(%v) = %v, want %v", test.in, got, test.want)
			}
		})
	}
}
