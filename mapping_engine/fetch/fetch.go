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

// Package fetch handles operations related to prefetching resources from an existing store.
package fetch

import (
	"context"
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/auth" /* copybara-comment: auth */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

// LoadFetchProjectors loads all HttpFetchQuery projectors from LibraryConfig, with name as the lookup key.
func LoadFetchProjectors(ctx context.Context, r *types.Registry, httpProjectors []*httppb.HttpFetchQuery) error {
	for _, q := range httpProjectors {
		name := q.GetName()
		projector, err := buildFetchProjector(ctx, q)
		if err != nil {
			return err
		}
		if err := r.RegisterProjector(name, projector); err != nil {
			return err
		}
		log.Println("Registered projector with name: " + name)
	}

	return nil
}

func buildFetchProjector(ctx context.Context, httpQuery *httppb.HttpFetchQuery) (types.Projector, error) {
	return func(arguments []jsonutil.JSONMetaNode, pctx *types.Context) (jsonutil.JSONToken, error) {
		errLocation := errors.FnLocationf("Fetch Function Preamble %q", httpQuery.GetName())

		requestMethodNode, err := mapping.EvaluateValueSource(httpQuery.GetRequestMethod(), arguments, nil, pctx)
		if err != nil {
			return nil, errors.Wrap(errLocation, fmt.Errorf("error occurred in getting request method %s", err))
		}
		requestURLNode, err := mapping.EvaluateValueSource(httpQuery.GetRequestUrl(), arguments, nil, pctx)
		if err != nil {
			return nil, errors.Wrap(errLocation, fmt.Errorf("error occurred in getting request url %s", err))
		}

		requestURL, err := jsonutil.NodeToToken(requestURLNode)
		if err != nil {
			return nil, errors.Wrap(errLocation, err)
		}

		url, ok := requestURL.(jsonutil.JSONStr)
		if !ok {
			return nil, errors.Wrap(errLocation, fmt.Errorf("request url should be a string"))
		}

		requestMethod, err := jsonutil.NodeToToken(requestMethodNode)
		if err != nil {
			return nil, errors.Wrap(errLocation, err)
		}

		method, ok := requestMethod.(jsonutil.JSONStr)
		if !ok {
			return nil, errors.Wrap(errLocation, fmt.Errorf("request method should be a string"))
		}

		if strings.ToUpper(string(method)) != http.MethodGet {
			return nil, errors.Wrap(errLocation, fmt.Errorf("only GET method is supported"))
		}

		errLocation = errors.FnLocationf("Fetch Function %q", httpQuery.GetName())

		client := auth.NewClient(ctx)
		req, err := http.NewRequest(http.MethodGet, string(url), nil)
		if err != nil {
			return nil, errors.Wrap(errLocation, fmt.Errorf("error building new request %v", err))
		}
		q := req.URL.Query()
		req.URL.RawQuery = q.Encode()
		resource, err := client.ExecuteRequest(ctx, req, "search resources", false)

		if err != nil {
			return nil, errors.Wrap(errLocation, fmt.Errorf("error searching for resources %v", err))
		}

		jc := &jsonutil.JSONContainer{}
		if err := jc.UnmarshalJSON(*resource); err != nil {
			return nil, errors.Wrap(errLocation, fmt.Errorf("error parsing retrieved resources %s", err))
		}

		return *jc, nil
	}, nil
}
