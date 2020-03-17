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
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"path"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/auth" /* copybara-comment: auth */
)

// RemoteCodeHarmonizer will harmonize codes using a remote lookup service.
type RemoteCodeHarmonizer struct {
	client  auth.Client
	address string
	cache   *ExpiringCache
}

func makeRemoteCodeHarmonizer(address string, ttl int, cleanup int) (*RemoteCodeHarmonizer, error) {
	c := auth.NewClient(context.Background())

	return &RemoteCodeHarmonizer{
		address: address,
		client:  c,
		cache:   NewCache(ttl, cleanup),
	}, nil
}

// HarmonizeBySearch implements CodeHarmonizer's HarmonizeBySearch function.
func (h *RemoteCodeHarmonizer) HarmonizeBySearch(sourceCode, sourceSystem, sourceValueset, targetValueset, version string) ([]HarmonizedCode, error) {
	key := CodeLookupKey{
		Code:    sourceCode,
		System:  sourceSystem,
		Version: version,
	}

	if sourceValueset == "" && targetValueset == "" {
		return nil, fmt.Errorf("source and target value sets cannot both be empty")
	}

	// Check cache before making http request.
	if v, ok := h.cache.Get(key); ok {
		return v.value, nil
	}

	u, err := url.Parse(h.address)
	if err != nil {
		return nil, fmt.Errorf("url is invalid %v", err)
	}
	u.Path = path.Join(u.Path, "fhir/ConceptMap/$translate")
	addr := u.String()

	req, err := http.NewRequest(http.MethodGet, addr, nil)
	if err != nil {
		return nil, fmt.Errorf("error building new request %v", err)
	}
	q := req.URL.Query()
	q.Add("code", sourceCode)
	q.Add("system", sourceSystem)
	if sourceValueset != "" {
		q.Add("source", sourceValueset)
	}
	if targetValueset != "" {
		q.Add("target", targetValueset)
	}
	if version != "" {
		q.Add("version", version)
	}

	req.URL.RawQuery = q.Encode()
	raw, err := h.client.ExecuteRequest(context.Background(), req, "translate code", true)
	if err != nil {
		return nil, fmt.Errorf("error calling remote endpoint to harmonize code, %v", err)
	}

	res, err := rawToCodes(raw)
	if err != nil {
		return nil, fmt.Errorf("error unmarshalling translate result %v", err)
	}

	if len(res) == 0 {
		res = append(res, HarmonizedCode{
			Code:    sourceCode,
			System:  "unharmonized",
			Version: version,
		})
	}

	// Add result to cache.
	h.cache.Put(key, res)

	return res, nil
}

// Harmonize implements CodeHarmonizer's Harmonize function.
func (h *RemoteCodeHarmonizer) Harmonize(sourceCode, sourceSystem, sourceName string) ([]HarmonizedCode, error) {
	key := CodeLookupKey{
		Code:   sourceCode,
		System: sourceSystem,
	}

	// Check cache before making http request.
	if v, ok := h.cache.Get(key); ok {
		return v.value, nil
	}

	u, err := url.Parse(h.address)
	if err != nil {
		return nil, fmt.Errorf("url is invalid %v", err)
	}
	u.Path = path.Join(u.Path, fmt.Sprintf("fhir/ConceptMap/%s/$translate", sourceName))
	addr := u.String()

	req, err := http.NewRequest(http.MethodGet, addr, nil)
	if err != nil {
		return nil, fmt.Errorf("error building new request %v", err)
	}
	q := req.URL.Query()
	q.Add("code", sourceCode)
	q.Add("system", sourceSystem)

	req.URL.RawQuery = q.Encode()

	raw, err := h.client.ExecuteRequest(context.Background(), req, "translate code", true)
	if err != nil {
		return nil, fmt.Errorf("error calling remote endpoint to harmonize code, %v", err)
	}

	res, err := rawToCodes(raw)
	if err != nil {
		return nil, fmt.Errorf("error unmarshalling translate result %v", err)
	}

	if len(res) == 0 {
		res = append(res, HarmonizedCode{
			Code:   sourceCode,
			System: fmt.Sprintf("%s-%s", sourceName, "unharmonized"),
		})
	}

	// Add result to cache.
	h.cache.Put(key, res)

	return res, nil
}

func rawToCodes(raw *json.RawMessage) ([]HarmonizedCode, error) {
	// TODO: Add support for multiple FHIR versions.
	parameters, err := unmarshalR3Parameters(*raw)
	if err != nil {
		return nil, fmt.Errorf("unmarshalling concept map %v failed with error: %v", string(*raw), err)
	}

	var res []HarmonizedCode
	for _, p := range parameters.Parameter {
		if p.Name != "match" {
			continue
		}

		for _, part := range p.Part {
			if part.Name != "concept" {
				continue
			}

			coding := part.ValueCoding

			// We do not support non equivalent codes yet.
			if part.Name == "equivalence" && strings.ToLower(coding.Code) != "equivalent" {
				continue
			}

			res = append(res, HarmonizedCode{
				Code:    coding.Code,
				System:  coding.System,
				Display: coding.Display,
				Version: coding.Version,
			})
		}
	}

	return res, nil
}
