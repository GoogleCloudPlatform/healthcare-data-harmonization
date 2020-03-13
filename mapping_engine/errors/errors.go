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

// Package errors contains custom error definitions.
package errors

import (
	"fmt"
	"runtime/debug"
)

// NotFoundError is returned when fetch encounters a 404 error.
type NotFoundError struct {
	Msg string
}

func (e NotFoundError) Error() string {
	return e.Msg
}

// RecoverWithTrace is a deferrable function that recovers a panic, merges it with the optrace, and
// passes that back to the given handler (which should probably assign the error return value of the
// function within which this is deferred).
func RecoverWithTrace(trace fmt.Stringer, operationName string, handler func(error)) {
	if r := recover(); r != nil {
		handler(fmt.Errorf("%s panic :(\n\nCause:\n%v\n\nStack:\n\n%sTrace:\n%s", operationName, r, string(debug.Stack()), trace))
	}
}
