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

// Package auth provides a client to call the service backends based on the type of server specified in the config.
package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"log"
	"net/http"

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/errors" /* copybara-comment: errors */
	"google.golang.org/grpc/credentials/oauth" /* copybara-comment: oauth */
	"google.golang.org/grpc" /* copybara-comment: grpc */
	"golang.org/x/oauth2/google" /* copybara-comment: google */
	"golang.org/x/oauth2" /* copybara-comment: oauth2 */

	httppb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: http_go_proto */
)

const (
	healthcareScope = "https://www.googleapis.com/auth/cloud-healthcare"
)

var servers = make(map[string]*httppb.ServerDefinition)

// Client is a generic interface for communicating with the server.
type Client interface {
	ExecuteRequest(ctx context.Context, req *http.Request, name string, failOnNotFound bool) (*json.RawMessage, error)
}

// LoadServerConfigs loads the server config
func LoadServerConfigs(sl []*httppb.ServerDefinition) error {
	for _, s := range sl {
		if s.GetName() == "" {
			return fmt.Errorf("server name cannot be empty")
		}
		servers[s.GetName()] = s
		log.Println("Loaded server " + s.GetName())
	}
	return nil
}

// Clear clears existing server configs
func Clear() {
	servers = make(map[string]*httppb.ServerDefinition)
}

// GCPClient is used to call a service hosted on GCP.
type GCPClient struct {
	// TODO: Use healthcare Go client when open sourced.
	client *http.Client
}

// ExecuteRequest executes an http request (req). If the request fails, it is logged with the parameter name. Upon receiving a 404, it returns an error or empty JSON object depending on the failOnNotFound flag.
func (sc *GCPClient) ExecuteRequest(ctx context.Context, req *http.Request, name string, failOnNotFound bool) (*json.RawMessage, error) {
	// TODO: Use healthcare Go client when open sourced.
	var resp *http.Response

	ts, err := getToken(ctx)
	if err == nil {
		values := req.URL.Query()
		values.Add("access_token", ts.AccessToken)
		req.URL.RawQuery = values.Encode()
	} else {
		// Not returning error here to continue trying to call server without credentials.
		log.Printf("could not retrieve default credentials %v", err)
	}

	resp, err = sc.client.Do(req)
	if err != nil {
		return nil, fmt.Errorf("%v request returned error %v", name, err)
	}

	defer resp.Body.Close()
	bytes, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		return nil, err
	}

	switch resp.StatusCode {
	case http.StatusOK:
		rawMessage := json.RawMessage(bytes)
		return &rawMessage, nil
	case http.StatusNotFound:
		if failOnNotFound {
			return nil, errors.NotFoundError{Msg: string(bytes)}
		}
		result := json.RawMessage(`{}`)
		return &result, nil
	default:
		return nil, fmt.Errorf("%v request failed with status code %v and body %v", name, resp.StatusCode, string(bytes))
	}
}

// NewClient establishes a client using http.
func NewClient(ctx context.Context) *GCPClient {
	return &GCPClient{client: &http.Client{}}
}

// NewGRPCConnection returns a connection to a gRPC server using Google default credentials.
func NewGRPCConnection(ctx context.Context, addr string) (*grpc.ClientConn, error) {
	ts, err := google.DefaultTokenSource(ctx, healthcareScope)
	if err != nil {
		return nil, fmt.Errorf("error retrieving token: %v", err)
	}
	grpcOpts := []grpc.DialOption{
		grpc.WithPerRPCCredentials(oauth.TokenSource{ts}),
	}
	return grpc.Dial(addr, grpcOpts...)
}

func getToken(ctx context.Context) (*oauth2.Token, error) {
	tokenSource, err := google.DefaultTokenSource(ctx, healthcareScope)
	if err != nil {
		return nil, fmt.Errorf("no default token %v", err)
	}
	return tokenSource.Token()
}
