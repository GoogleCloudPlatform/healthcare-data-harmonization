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

// Package gcsutil provides utility functions for dealing with GCS URIs.
package gcsutil

import (
	"context"
	"fmt"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/storageclient" /* copybara-comment: storageclient */
)

const (
	gcsURIPrefix = "gs://"
)

var client StorageClient

// StorageClient is an interface to communicate with GCS.
type StorageClient interface {
	ReadBytes(ctx context.Context, bucket string, filename string) ([]byte, error)
}

// ParseGCSURI parses a GCS URI and returns the bucket name and object name, respectively.
// For example, "gs://testbucket/path/to/object", would have a bucket "testbucket" and object path "path/to/object".
// This function does not check the validity of the bucket or object name.
func ParseGCSURI(uri string) (string, string, error) {
	if !strings.HasPrefix(uri, gcsURIPrefix) {
		return "", "", fmt.Errorf("invalid GCS URI: %v", uri)
	}
	toks := strings.SplitN(strings.TrimPrefix(uri, gcsURIPrefix), "/", 2)
	if len(toks) == 1 {
		return toks[0], "", nil
	}
	return toks[0], toks[1], nil
}

// BuildGCSURI builds a GCS URI for the given bucketName and objectName.
// This function does not check the validity of the bucket or object name.
func BuildGCSURI(bucketName, objectName string) string {
	return fmt.Sprintf("%v%v/%v", gcsURIPrefix, bucketName, objectName)
}

// ReadFromGcs reads a file from GCS to a byte array.
func ReadFromGcs(ctx context.Context, gcsLocation string) ([]byte, error) {
	bucket, filename, err := ParseGCSURI(gcsLocation)
	if err != nil {
		return nil, fmt.Errorf("GCS location %s is invalid", gcsLocation)
	}
	return client.ReadBytes(ctx, bucket, filename)
}

// InitializeClient initializes the storage client.
func InitializeClient(c StorageClient) {
	if c == nil {
		client = &storageclient.ThirdPartyClient{}
	} else {
		client = c
	}
}
