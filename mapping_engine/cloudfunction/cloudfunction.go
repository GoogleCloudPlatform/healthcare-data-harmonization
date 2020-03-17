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

// Package cloudfunction contains methods for creating and calling projectors containing Google cloud functions.
// TODO: Need an end-to-end mapping library test for mocked cloud function calls.
package cloudfunction

import (
	"bytes"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"net/http"
	"regexp"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

const (
	cloudFunctionURLPattern = `^http(s)?://.*$`
)

// LoadCloudFunctionProjectors registers all defined Google cloud functions.
func LoadCloudFunctionProjectors(r *types.Registry, cloudfuncs []*httppb.CloudFunction) error {
	for _, cf := range cloudfuncs {
		proj, err := FromCloudFunction(cf)

		if err != nil {
			return fmt.Errorf("error loading cloud function %s: %v", cf.Name, err)
		}

		if err = r.RegisterProjector(cf.Name, proj); err != nil {
			return fmt.Errorf("error registering projector %s: %v", cf.Name, err)
		}
	}
	return nil
}

// FromCloudFunction creates a projector from a Google cloud function.
// Currently only supports HTTP trigger to call cloud function.
func FromCloudFunction(cf *httppb.CloudFunction) (types.Projector, error) {
	if err := validateCloudFunction(cf); err != nil {
		return nil, fmt.Errorf("invalid Google cloud function definition: %v", err)
	}
	return func(metaArgs []jsonutil.JSONMetaNode, pctx *types.Context) (jsonutil.JSONToken, error) {
		pctx.Trace.StartProjectorCall(cf.Name, metaArgs, pctx.String())

		args := make([]jsonutil.JSONToken, len(metaArgs))
		for i, metaArg := range metaArgs {
			node, err := jsonutil.NodeToToken(metaArg)
			if err != nil {
				return nil, fmt.Errorf("error converting args: %v", err)
			}
			args[i] = node
		}

		var body []byte
		var err error

		if len(args) == 1 {
			body, err = json.Marshal(args[0])
		} else {
			body, err = json.Marshal(args)
		}

		if err != nil {
			return nil, fmt.Errorf("error marshaling arguments into a JSON object: %v", err)
		}

		pctx.Trace.StartCloudFunctionCall(cf.RequestUrl, metaArgs, pctx.String())

		resp, err := http.Post(cf.RequestUrl, "application/json", bytes.NewBuffer(body))
		if err != nil {
			return nil, fmt.Errorf("cloud function request failed due to: %v", err)
		}

		pctx.Trace.EndCloudFunctionCall(cf.RequestUrl, "<same as start>", resp.Status)

		if resp.StatusCode != http.StatusOK {
			message, _ := ioutil.ReadAll(resp.Body)
			return nil, fmt.Errorf("error http response status code: %v, error message: %s", resp.Status, message)
		}

		defer resp.Body.Close()
		bytes, err := ioutil.ReadAll(resp.Body)
		if err != nil {
			return nil, fmt.Errorf("error reading http response body: %v", err)
		}

		if len(bytes) == 0 {
			return nil, errors.New("error empty http response body")
		}

		val, err := jsonutil.UnmarshalJSON(json.RawMessage(bytes))
		if err != nil {
			return nil, fmt.Errorf("error unmarshal http response body into JSONToken: %v", err)
		}

		pctx.Trace.EndProjectorCall(cf.Name, "<same as start>", val)
		return val, nil
	}, nil
}

func validateCloudFunction(cf *httppb.CloudFunction) error {
	if !strings.HasPrefix(cf.Name, "@") {
		return fmt.Errorf("invalid cloud function name, should start with '@'")
	}
	m, err := regexp.Compile(cloudFunctionURLPattern)
	if err != nil {
		return fmt.Errorf("invalid cloud function url pattern: %v", err)
	}
	if !m.MatchString(cf.RequestUrl) {
		return fmt.Errorf("error cloud function request url: %s", cf.RequestUrl)
	}
	return nil
}
