// Copyright 2020 Google LLC
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

// Package errors contains utilties for wrapping and pretty printing mapping errors. The intended
// use is to Wrap errors with information as they bubble back up the stack.
// For information on this paradigm, see https://blog.golang.org/go1.13-errors
package errors

import (
	"fmt"
	"runtime/debug"
)

const (
	// FunctionStartPrefix is the prefix to use for formatting error locations that indicate the start
	// of the function.
	FunctionStartPrefix = ""

	// FunctionStartSuffix is the suffix to use for formatting error locations that indicate the start
	// of the function.
	FunctionStartSuffix = "\n"

	// DefaultPrefix is the prefix to use for formatting error locations (that are not function
	// starts).
	DefaultPrefix = "\t"

	// DefaultSuffix is the suffix to use for formatting error locations (that are not function
	// starts).
	DefaultSuffix = ""
)

// Wrap is the opposite of Go's errors.Unwrap, and a utility method for formatting wrapped errors.
// This implementation puts the inner error at the top, followed by a new line followed by the
// outter error; i.e. it is a thin wrapper of fmt.Errorf's %w verb.
// Wrap can be called with a nil inner error, which will subsequently return nil.
func Wrap(outter, inner error) error {
	if inner != nil {
		return fmt.Errorf("%w\n%v", inner, outter)
	}
	return nil
}

// stringErrorLocation is an error location consisting of just a string.
type stringErrorLocation struct {
	location string

	// isFunctionStart indicates whether to use the function start prefix when formatting.
	isFunctionStart bool
}

// Locationf returns a string based implementation of an Error to use as a wrapper, similar to
// fmt.Sprintf.
func Locationf(format string, formatArgs ...interface{}) error {
	return stringErrorLocation{
		location: fmt.Sprintf(format, formatArgs...),
	}
}

// FnLocationf returns a string based implementation of an Error to use as a wrapper, similar to
// fmt.Sprintf.
func FnLocationf(format string, formatArgs ...interface{}) error {
	return stringErrorLocation{
		location:        fmt.Sprintf(format, formatArgs...),
		isFunctionStart: true,
	}
}

func (s stringErrorLocation) String() string {
	return s.Error()
}

func (s stringErrorLocation) Error() string {
	prefix, suffix := DefaultPrefix, DefaultSuffix
	if s.isFunctionStart {
		prefix, suffix = FunctionStartPrefix, FunctionStartSuffix
	}
	return prefix + s.location + suffix
}

// NotFoundError is returned when fetch encounters a 404 error.
type NotFoundError struct {
	Msg string
}

func (e NotFoundError) Error() string {
	return e.Msg
}

// Recover is a deferrable function that recovers a panic, and passes that back to the given handler
// (which should probably assign the error return value of the function within which this is
// deferred).
func Recover(operationName string, handler func(error)) {
	if r := recover(); r != nil {
		handler(fmt.Errorf("%s panic :(\n\nCause:\n%v\n\nStack:\n\n%s", operationName, r, string(debug.Stack())))
	}
}
