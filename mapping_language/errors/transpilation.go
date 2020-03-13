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

// Package errors contains data structures to report and present errors.
package errors

import "fmt"

// TranspilationError contains information about a transpiler specific error.
type TranspilationError struct {
	line int
	col  int
	err  error
}

// NewTranspilationError creates a new transpilation error with the given
// information about the source of the error.
func NewTranspilationError(line, col int, err error) TranspilationError {
	return TranspilationError{
		line: line,
		col:  col,
		err:  err,
	}
}

func (w TranspilationError) Error() string {
	return fmt.Sprintf("[line %d col %d] %v", w.line, w.col, w.err)
}

// Line returns the specific line number in the original Whistle code where the error occurred.
// 1-based.
func (w TranspilationError) Line() int {
	return w.line
}

// Col returns the column number, in the line of the original Whistle code where the error occurred.
// 1-based.
func (w TranspilationError) Col() int {
	return w.col
}
