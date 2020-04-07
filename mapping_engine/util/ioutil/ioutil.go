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

// Package ioutil contains utility methods for IO operations.
package ioutil

import (
	"io/ioutil"
	"log"
	"os"
	"path/filepath"
)

// MustRead tries to read the content of the file specified by the path.
// The name parameter is used strictly for formatting the errors.
func MustRead(path, name string) []byte {
	b, err := ioutil.ReadFile(path)
	if err != nil {
		log.Fatalf("Could not read %s file %q: %v", name, path, err)
	}
	return b
}

// MustReadDir gets the file names in the dir specified by the path.
// It ignores sub directories. The name parameter is used strictly
// for formatting the errors.
func MustReadDir(path, name string) []string {
	fis, err := ioutil.ReadDir(path)
	if err != nil {
		log.Fatalf("Could not read %s dir %q: %v", name, path, err)
	}
	var o []string
	for _, fi := range fis {
		p := filepath.Join(path, fi.Name())
		if fi.IsDir() {
			continue
		}
		o = append(o, p)
	}
	return o
}

// MustReadGlob gets the file/directory names that match the path.
// The name parameter is used strictly for formatting the errors.
func MustReadGlob(pattern, name string) []string {
	fis, err := filepath.Glob(pattern)
	if err != nil {
		log.Fatalf("Could not read %s glob %q: %v", name, pattern, err)
	}
	return fis
}

// Exists checks if a file or folder exists.
func Exists(name string) bool {
	_, err := os.Stat(name)
	return !os.IsNotExist(err)
}
