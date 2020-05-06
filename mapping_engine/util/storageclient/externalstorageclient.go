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

// Package storageclient defines a wrapper around different ways to download files from GCS.
package storageclient

import (
	"context"
	"fmt"
	"io/ioutil"

	"cloud.google.com/go/storage" /* copybara-comment: storage */
	"google.golang.org/api/option" /* copybara-comment: option */

	goauth2 "golang.org/x/oauth2/google" /* copybara-comment: google */
)

// ThirdPartyClient is a client to communicate with GCS from outside of google3.
type ThirdPartyClient struct {
}

func (c *ThirdPartyClient) ReadBytes(ctx context.Context, bucket string, filename string) ([]byte, error) {
	ts, err := goauth2.DefaultTokenSource(ctx)
	if err != nil {
		return nil, fmt.Errorf("unable to create token source: %v", err)
	}
	client, err := storage.NewClient(ctx, option.WithTokenSource(ts))
	if err != nil {
		return nil, fmt.Errorf("unable to create GCS storage client: %v", err)
	}
	defer client.Close()
	reader, err := client.Bucket(bucket).Object(filename).NewReader(ctx)
	if err != nil {
		return nil, fmt.Errorf("unable to read the file %s/%s from GCS: %v", bucket, filename, err)
	}
	defer reader.Close()
	raw, err := ioutil.ReadAll(reader)
	if err != nil {
		return nil, fmt.Errorf("unable to download the file %s/%s from GCS: %v", bucket, filename, err)
	}
	return raw, nil
}
