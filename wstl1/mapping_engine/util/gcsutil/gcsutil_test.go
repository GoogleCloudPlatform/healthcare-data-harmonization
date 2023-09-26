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

package gcsutil

import (
	"testing"
)

func TestParseGCSURI_Success(t *testing.T) {
	tests := []struct {
		name                   string
		uri                    string
		bucketName, objectName string
	}{
		{
			"no object name",
			"gs://testbucket",
			"testbucket", "",
		},
		{
			"no object name, trailing '/'",
			"gs://testbucket/",
			"testbucket", "",
		}, {
			"nested dirs",
			"gs://testbucket/path/to/object",
			"testbucket", "path/to/object",
		}, {
			"nested dirs, trailing '/'",
			"gs://testbucket/path/to/object/",
			"testbucket", "path/to/object/",
		},
	}

	for _, test := range tests {
		bucketName, objectName, err := ParseGCSURI(test.uri)
		if bucketName != test.bucketName || objectName != test.objectName || err != nil {
			t.Errorf("%v: ParseGCSURI(%v) => (%v, %v, %v) expected (%v, %v, nil)", test.name, test.uri, bucketName, objectName, err, test.bucketName, test.objectName)
		}
	}
}

func TestParseGCSURI_Errors(t *testing.T) {
	tests := []struct {
		name string
		uri  string
	}{
		{
			"malformed GCS URI",
			"gs:/testbucket",
		},
		{
			"non-GCS URI",
			"cns://cns/testbucket",
		},
		{
			"empty uri",
			"",
		},
	}

	for _, test := range tests {
		bucketName, objectName, err := ParseGCSURI(test.uri)
		if err == nil {
			t.Errorf("%v: ParseGCSURI(%v) => (%v, %v, nil) expected non-nil error", test.name, test.uri, bucketName, objectName)
		}
	}
}
