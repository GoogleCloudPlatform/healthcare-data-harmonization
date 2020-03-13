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

package harmonizecode

import (
	"encoding/json"
	"io/ioutil"
	"net/http"
	"net/http/httptest"
	"strconv"
	"strings"
	"testing"
	"time"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
)

const (
	// Clear cache after 3 seconds.
	testCacheTTL = 3

	// TODO: Support multiple FHIR versions.
	parameter1 = `{
		"parameter": [
			{
				"name": "result",
				"valueBoolean": true
			},
			{
				"name": "match",
				"part": [
					{
						"name": "equivalence",
						"valueCode": "equivalent"
					},
					{
						"name": "concept",
						"valueCoding": {
							"code": "target-code",
							"display": "Target Code",
							"system": "target-system",
							"userSelected": false,
							"version": "target-version"
						}
					}
				]
			}
		],
		"resourceType": "Parameters"
	}`

	parameter2 = `{
		"parameter": [
			{
				"name": "result",
				"valueBoolean": false
			}
		],
		"resourceType": "Parameters"
	}`
)

func setupMockServer(t *testing.T) *httptest.Server {
	mocks := map[string]json.RawMessage{
		"result1": json.RawMessage(parameter1),
		"result2": json.RawMessage(parameter2),
	}

	count := 0

	return httptest.NewServer(http.HandlerFunc(
		func(w http.ResponseWriter, r *http.Request) {
			// Search for appropriate code result.
			parts := strings.Split(r.URL.String(), "/")
			k := parts[1]
			if k == "count" {
				w.Write(json.RawMessage(`{"count":` + strconv.Itoa(count) + `}`))
				w.Header().Set("Content-Type", "application/json")
				w.WriteHeader(http.StatusOK)
				return
			}
			if v, ok := mocks[k]; ok {
				w.Header().Set("Content-Type", "application/fhir+json")
				w.WriteHeader(http.StatusOK)
				w.Write(v)
				count++
				return
			}
			w.WriteHeader(http.StatusNotFound)
			return
		}))

}

func TestRemoteCodeHarmonizerBySearch_Error(t *testing.T) {
	tests := []struct {
		name           string
		urlPath        string
		sourceCode     string
		sourceSystem   string
		sourceValueset string
		targetValueset string
		version        string
	}{
		{
			name:           "empty source and target valuesets",
			urlPath:        "/result1",
			sourceCode:     "source-code",
			sourceSystem:   "source-system",
			sourceValueset: "",
			targetValueset: "",
			version:        "v1",
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			// Refresh server for each test.
			s := setupMockServer(t)
			harmonizer, err := makeRemoteCodeHarmonizer(s.URL+test.urlPath, testCacheTTL, 1)
			if err != nil {
				t.Fatalf("makeRemoteCodeHarmonizer in test %s returned unexpected error %v", test.name, err)
			}
			_, err = harmonizer.HarmonizeBySearch(test.sourceCode, test.sourceSystem, test.sourceValueset, test.targetValueset, test.version)
			if err == nil {
				t.Errorf("expected error but returned none")
			}
		})
	}
}

func TestRemoteCodeHarmonizerBySearch(t *testing.T) {
	tests := []struct {
		name           string
		urlPath        string
		sourceCode     string
		sourceSystem   string
		sourceValueset string
		targetValueset string
		version        string
		expectedOutput []HarmonizedCode
	}{
		{
			name:           "empty source and target valuesets",
			urlPath:        "/result1",
			sourceCode:     "source-code",
			sourceSystem:   "source-system",
			sourceValueset: "sourcevs",
			targetValueset: "targetvs",
			version:        "",
			expectedOutput: []HarmonizedCode{
				{
					Code:    "target-code",
					Display: "Target Code",
					System:  "target-system",
					Version: "target-version",
				},
			},
		},
		{
			name:           "empty lookup",
			urlPath:        "/result2",
			sourceCode:     "source-code",
			sourceSystem:   "source-system",
			sourceValueset: "sourcevs",
			targetValueset: "targetvs",
			version:        "v1",
			expectedOutput: []HarmonizedCode{
				{
					Code:    "source-code",
					System:  "unharmonized",
					Version: "v1",
				},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			// Refresh server for each test.
			s := setupMockServer(t)
			harmonizer, err := makeRemoteCodeHarmonizer(s.URL+test.urlPath, testCacheTTL, 1)
			if err != nil {
				t.Fatalf("makeRemoteCodeHarmonizer in test %s returned unexpected error %v", test.name, err)
			}
			actualOutput, err := harmonizer.HarmonizeBySearch(test.sourceCode, test.sourceSystem, test.sourceValueset, test.targetValueset, test.version)
			if err != nil {
				t.Fatalf("first harmonization attempt in test %s returned unexpected error %v", test.name, err)
			}
			if diff := cmp.Diff(test.expectedOutput, actualOutput); diff != "" {
				t.Errorf("first attempt Harmonize(%s, %s) => diff -%v +%v\n%s", test.sourceCode, test.sourceSystem, test.expectedOutput, actualOutput, diff)
			}
		})
	}
}

func TestRemoteCodeHarmonizer(t *testing.T) {
	tests := []struct {
		name           string
		urlPath        string
		sourceCode     string
		sourceSystem   string
		sourceName     string
		expectedOutput []HarmonizedCode
	}{
		{
			name:         "single code lookup",
			urlPath:      "/result1",
			sourceCode:   "source-code",
			sourceSystem: "source-system",
			sourceName:   "conceptmap1",
			expectedOutput: []HarmonizedCode{
				{
					Code:    "target-code",
					Display: "Target Code",
					System:  "target-system",
					Version: "target-version",
				},
			},
		},
		{
			name:         "empty lookup",
			urlPath:      "/result2",
			sourceCode:   "source-code",
			sourceSystem: "source-system",
			sourceName:   "conceptmap2",
			expectedOutput: []HarmonizedCode{
				{
					Code:   "source-code",
					System: "conceptmap2-unharmonized",
				},
			},
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			// Refresh server for each test.
			s := setupMockServer(t)
			harmonizer, err := makeRemoteCodeHarmonizer(s.URL+test.urlPath, testCacheTTL, 1)
			if err != nil {
				t.Fatalf("makeRemoteCodeHarmonizer in test %s returned unexpected error %v", test.name, err)
			}
			actualOutput, err := harmonizer.Harmonize(test.sourceCode, test.sourceSystem, test.sourceName)

			if err != nil {
				t.Fatalf("first harmonization attempt in test %s returned unexpected error %v", test.name, err)
			}
			if diff := cmp.Diff(test.expectedOutput, actualOutput); diff != "" {
				t.Errorf("first attempt Harmonize(%s, %s) => diff -%v +%v\n%s", test.sourceCode, test.sourceSystem, test.expectedOutput, actualOutput, diff)
			}

			// Harmonize call above should have populated cache.
			if harmonizer.cache.Len() != 1 {
				t.Fatalf("failed to register cache")
			}

			// Check the mock server was only called once.
			verifyServerCalls(t, test.name, s.URL, 1)

			// This call should retrieve results from cache, not server.
			cachedOutput, err := harmonizer.Harmonize(test.sourceCode, test.sourceSystem, test.sourceName)
			if err != nil {
				t.Fatalf("second harmonization attempt in test %s returned unexpected error %v", test.name, err)
			}
			// Check the mock server was only called once.
			verifyServerCalls(t, test.name, s.URL, 1)

			if diff := cmp.Diff(test.expectedOutput, cachedOutput); diff != "" {
				t.Errorf("second attempt Harmonize(%s, %s) => diff -%v +%v\n%s", test.sourceCode, test.sourceSystem, test.expectedOutput, cachedOutput, diff)
			}

			time.Sleep(time.Duration(testCacheTTL+1) * time.Second)
			if harmonizer.cache.Len() != 0 {
				t.Fatalf("failed to clear cache")
			}

			secondOutput, err := harmonizer.Harmonize(test.sourceCode, test.sourceSystem, test.sourceName)
			if err != nil {
				t.Fatalf("first harmonization attempt in test %s returned unexpected error %v", test.name, err)
			}
			// Check the mock server was called twice.
			verifyServerCalls(t, test.name, s.URL, 2)
			if diff := cmp.Diff(test.expectedOutput, secondOutput); diff != "" {
				t.Errorf("third attempt Harmonize(%s, %s) => diff -%v +%v\n%s", test.sourceCode, test.sourceSystem, test.expectedOutput, secondOutput, diff)
			}

		})
	}
}

func verifyServerCalls(t *testing.T, name, url string, expectedCount int) {
	req, err := http.NewRequest("GET", url+"/count", nil)
	if err != nil {
		t.Fatalf("error generating request %v", err)
	}
	client := &http.Client{}
	resp, err := client.Do(req)
	if err != nil {
		t.Fatalf("error sending request %v", err)
	}

	defer resp.Body.Close()
	bytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		t.Fatalf("error reading count response %v", err)
	}
	var c map[string]int
	err = json.Unmarshal(bytes, &c)
	if err != nil {
		t.Fatalf("error verifying request count %v", err)
	}
	if c["count"] != expectedCount {
		t.Errorf("request count verification failed, got %v, expected %v", c["count"], expectedCount)
	}
}
