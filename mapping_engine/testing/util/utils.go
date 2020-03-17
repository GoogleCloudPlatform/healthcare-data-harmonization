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

// Package util contains helper functions for mapping unit tests.
package util

import (
	"context"
	"encoding/json"
	"fmt"
	"net/url"
	"regexp"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"bitbucket.org/creachadair/stringset" /* copybara-comment: stringset */
)

var urlParse = regexp.MustCompile(`fhir/([^/]+)(?:/([^?#/]+))?`)

// SearchResources implements auth.Client's SearchResources function.
func SearchResources(ctx context.Context, dataset jsonutil.JSONToken, address string, method string) (*json.RawMessage, error) {
	arr, ok := dataset.(jsonutil.JSONArr)
	if !ok {
		j, err := json.Marshal(dataset)
		msg := json.RawMessage(j)
		return &msg, err
	}

	pa, err := url.Parse(address)
	if err != nil {
		return nil, fmt.Errorf("failed to parse URL %s: %v", address, err)
	}

	var matchingResources jsonutil.JSONArr

	searchParams := pa.Query()

	urlPathParams := urlParse.FindStringSubmatch(pa.Path)
	if urlPathParams[1] != "" {
		searchParams["resourceType"] = []string{urlPathParams[1]}
	}
	if urlPathParams[2] != "" {
		searchParams["id"] = []string{urlPathParams[2]}
	}

	for _, res := range arr {
		match := true
		for k, vs := range searchParams {
			valueSet := stringset.New(vs...)

			v, err := jsonutil.GetField(res, k)
			if v, ok := v.(jsonutil.JSONStr); err != nil || !ok || !valueSet.Contains(string(v)) {
				match = false
				break
			}
		}

		if match {
			matchingResources = append(matchingResources, res)
		}
	}

	var res jsonutil.JSONToken = matchingResources
	if len(matchingResources) == 0 {
		msg := json.RawMessage(`{}`)
		return &msg, nil
	}
	if len(matchingResources) == 1 {
		res = matchingResources[0]
	}

	resj, err := json.Marshal(res)
	msg := json.RawMessage(resj)
	return &msg, err
}
