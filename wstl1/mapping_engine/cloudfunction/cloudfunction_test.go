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

package cloudfunction

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

// toNodes converts the given array of tokens into nodes with meta data.
func toNodes(t *testing.T, tokens []jsonutil.JSONToken) []jsonutil.JSONMetaNode {
	t.Helper()
	nodes := make([]jsonutil.JSONMetaNode, len(tokens))
	for i, arg := range tokens {
		tok, err := jsonutil.TokenToNode(arg)
		if err != nil {
			t.Fatalf("error convering tokens to nodes: %v", err)
		}
		nodes[i] = tok
	}
	return nodes
}

// strLen defines a cloud function, which computes length of input string.
func strLen(w http.ResponseWriter, r *http.Request) {
	var s string
	if err := json.NewDecoder(r.Body).Decode(&s); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "error: %v", err)
		return
	}
	res := len(s)
	fmt.Fprintf(w, "%d", res)
}

// minus defines a cloud function, which computes minus of two input integers.
func minus(w http.ResponseWriter, r *http.Request) {
	var d [2]int
	if err := json.NewDecoder(r.Body).Decode(&d); err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "error: %v", err)
		return
	}
	res := d[0] - d[1]
	fmt.Fprintf(w, "%d", res)
}

// identity defines a cloud function, which returns the same as input.
func identity(w http.ResponseWriter, r *http.Request) {
	m, err := ioutil.ReadAll(r.Body)
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		fmt.Fprintf(w, "error: %v", err)
		return
	}
	w.Header().Set("Content-Type", r.Header.Get("Content-Type"))
	w.Write(m)
}

// invalidResponse defines an invalid cloud function, which returns an invalid response
func invalidResponse(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte("[1, 2"))
}

// emptyResponse defines an invalid cloud function, which returns an empty response
func emptyResponse(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")
	w.Write([]byte{})
}

var cloudFunction = map[string]http.HandlerFunc{
	"/strLen":          strLen,
	"/minus":           minus,
	"/identity":        identity,
	"/invalidResponse": invalidResponse,
	"/emptyResponse":   emptyResponse,
}

func setUp() *httptest.Server {
	return httptest.NewServer(http.HandlerFunc(
		func(w http.ResponseWriter, r *http.Request) {
			if cf, ok := cloudFunction[r.URL.String()]; ok {
				cf(w, r)
				return
			}
			w.WriteHeader(http.StatusNotImplemented)
			fmt.Fprintf(w, "error: cannot find mock cloud function %s", r.URL)
			return
		},
	))
}

func TestFromCloudFunction(t *testing.T) {
	testCases := []struct {
		name   string
		method string
		cf     httppb.CloudFunction
		args   []jsonutil.JSONToken
		expect jsonutil.JSONToken
	}{
		{
			name:   "call cloud function: identity",
			method: "identity",
			cf: httppb.CloudFunction{
				Name: "@Identity",
			},
			args:   []jsonutil.JSONToken{jsonutil.JSONArr{jsonutil.JSONStr("abc"), jsonutil.JSONNum(0)}},
			expect: jsonutil.JSONArr{jsonutil.JSONStr("abc"), jsonutil.JSONNum(0)},
		},
		{
			name:   "call cloud function: strLen",
			method: "strLen",
			cf: httppb.CloudFunction{
				Name: "@StrLen",
			},
			args:   []jsonutil.JSONToken{jsonutil.JSONStr("Google")},
			expect: jsonutil.JSONNum(6),
		},
		{
			name:   "call cloud function: minus",
			method: "minus",
			cf: httppb.CloudFunction{
				Name: "@Minus",
			},
			args:   []jsonutil.JSONToken{jsonutil.JSONNum(100), jsonutil.JSONNum(99)},
			expect: jsonutil.JSONNum(1),
		},
	}

	s := setUp()
	defer s.Close()

	for _, test := range testCases {
		t.Run(test.name, func(t *testing.T) {
			test.cf.RequestUrl = s.URL + "/" + test.method

			proj, err := FromCloudFunction(&test.cf)
			if err != nil {
				t.Fatalf("FromCloudFunction(%v) returned unexpected error: %v", test.cf, err)
			}

			got, err := proj(toNodes(t, test.args), types.NewContext(types.NewRegistry()))
			if err != nil {
				t.Fatalf("<generated projector>(%v) => unexpected error: %v", test.args, err)
			}
			if !cmp.Equal(got, test.expect) {
				t.Errorf("<generated projector>(%v) = %v, expect %v", test.args, got, test.expect)
			}
		})
	}
}

func TestFromCloudFunctionCreateError(t *testing.T) {
	testCase := []struct {
		name string
		cf   httppb.CloudFunction
	}{
		{
			name: "invalid cloud function name",
			cf: httppb.CloudFunction{
				Name:       "Identity",
				RequestUrl: "https://google.cloud.function/identity",
			},
		},
		{
			name: "invalid cloud function request url",
			cf: httppb.CloudFunction{
				Name:       "@Identity",
				RequestUrl: "google.cloud.function/identity",
			},
		},
	}

	for _, test := range testCase {
		t.Run(test.name, func(t *testing.T) {
			_, err := FromCloudFunction(&test.cf)
			if err == nil {
				t.Fatalf("FromCloudFunction(%v) returned a projector but expected an error", test.cf)
			}
		})
	}
}

func TestFromCloudFunctionCallError(t *testing.T) {
	testCase := []struct {
		name   string
		method string
		cf     httppb.CloudFunction
		args   []jsonutil.JSONToken
	}{
		{
			name:   "incorrect request url",
			method: "minus",
			cf: httppb.CloudFunction{
				Name:       "@Minus",
				RequestUrl: "https://unknown",
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(10), jsonutil.JSONNum(1)},
		},
		{
			name:   "incorrect request method",
			method: "add",
			cf: httppb.CloudFunction{
				Name: "@Add",
			},
			args: []jsonutil.JSONToken{jsonutil.JSONNum(10), jsonutil.JSONNum(1)},
		},
		{
			name:   "invalid calling arguments",
			method: "minus",
			cf: httppb.CloudFunction{
				Name: "@Minus",
			},
			args: []jsonutil.JSONToken{jsonutil.JSONStr("10"), jsonutil.JSONStr("1")},
		},
		{
			name:   "invalid cloud function response",
			method: "invalidResponse",
			cf: httppb.CloudFunction{
				Name: "@InvalidResponse",
			},
			args: []jsonutil.JSONToken{},
		},
		{
			name:   "empty cloud function response",
			method: "emptyResponse",
			cf: httppb.CloudFunction{
				Name: "@EmptyResponse",
			},
			args: []jsonutil.JSONToken{},
		},
	}

	s := setUp()
	defer s.Close()

	for _, test := range testCase {
		t.Run(test.name, func(t *testing.T) {
			if test.cf.RequestUrl == "" {
				test.cf.RequestUrl = s.URL + "/" + test.method
			}
			proj, err := FromCloudFunction(&test.cf)
			if err != nil {
				t.Fatalf("FromCloudFunction(%v) returned unexpected error: %v", test.cf, err)
			}

			_, err = proj(toNodes(t, test.args), types.NewContext(types.NewRegistry()))

			if err == nil {
				t.Fatalf("call cloud function %v successfully but expected an error", test.cf)
			}
		})
	}
}
