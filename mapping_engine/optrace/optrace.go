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

// Package optrace contains methods and structures for tracing operations throughout the mapping
// process.
package optrace

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */
	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */

	mappb "github.com/GoogleCloudPlatform/healthcare_data_harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

const (
	indentStartScope = 1
	indentEndScope   = -1
	indentNoScope    = 0
)

// Trace contains data about operations performed.
type Trace struct {
	ops []Op
}

type scopeStart struct{}

func (scopeStart) indent() int { return indentStartScope }

type scopeEnd struct{}

func (scopeEnd) indent() int { return indentEndScope }

// pushOp adds an operation to the operation stack.
func (t *Trace) pushOp(op Op) {
	if t != nil {
		t.ops = append(t.ops, op)
	}
}

// AsError adds the given error to the operation stack and returns the whole trace (as a printable
// error).
func (t *Trace) AsError(err error) error {
	t.pushOp(opFailure{
		Error: err,
	})

	return t
}

// String converts the trace into a stunningly beautiful and easy to read representation of all the
// operations in the trace. Note, that until this point, the individual operations are stored with
// their raw data only, and are only here converted to strings.
func (t *Trace) String() string {
	indent := ""
	sb := strings.Builder{}
	for _, op := range t.ops {
		desc := op.description()
		if desc != "" {
			desc = prefixLines(indent, op.description())
			sb.WriteString(desc)
			sb.WriteString("\n")
		}
		indentDelta := op.indent()
		for ; indentDelta > 0; indentDelta-- {
			indent += "\t"
		}
		for ; indentDelta < 0; indentDelta++ {
			indent = string(indent[:len(indent)-1])
		}
	}

	return sb.String()
}

// Error returns the error string (to implement error interface). Equivalent to String().
func (t *Trace) Error() string {
	return t.String()
}

// Ops gets the list of all pushed Ops in this trace. This slice should not be modified directly.
func (t *Trace) Ops() []Op {
	return t.ops
}

// prefixLines adds the given prefix to every line in the given text.
func prefixLines(prefix, text string) string {
	lines := strings.Split(text, "\n")
	var out []string
	for _, l := range lines {
		out = append(out, fmt.Sprintf("%s%s", prefix, l))
	}

	return strings.Join(out, "\n")
}

// Op represents a single, traceable operation.
type Op interface {
	// description returns the human readable description of this operation.
	description() string

	indent() int
}

// OpStartMapping is the beginning of processing a field mapping.
type OpStartMapping struct {
	scopeStart
	Field *mappb.FieldMapping
}

// StartMapping adds a trace of the beginning of processing a field mapping.
func (t *Trace) StartMapping(field *mappb.FieldMapping) {
	t.pushOp(OpStartMapping{
		Field: field,
	})
}

func (op OpStartMapping) description() string {
	targetDesc := describeFieldTarget(op.Field)
	return fmt.Sprintf("starting field mapping for %s", targetDesc)
}

// OpEndMapping is the end (and result) of processing a field mapping.
type OpEndMapping struct {
	scopeEnd
	Field  *mappb.FieldMapping
	Result jsonutil.JSONToken
}

// EndMapping adds a trace of the end (and result) of processing a field mapping.
func (t *Trace) EndMapping(field *mappb.FieldMapping, result jsonutil.JSONToken) {
	t.pushOp(OpEndMapping{
		Field:  field,
		Result: jsonutil.Deepcopy(result),
	})
}

func (op OpEndMapping) description() string {
	targetDesc := describeFieldTarget(op.Field)
	return fmt.Sprintf("completed field mapping for %s, set value to %v", targetDesc, op.Result)
}

// OpStartBundler is the start of a bundle operation.
type OpStartBundler struct {
	scopeStart
	Input jsonutil.JSONToken
}

// OpEndBundler is the end of a bundle operation.
type OpEndBundler struct {
	scopeEnd
	Output jsonutil.JSONToken
}

func (op OpStartBundler) description() string {
	return fmt.Sprintf("bundling %v", op.Input)
}

func (op OpEndBundler) description() string {
	return fmt.Sprintf("finished bundling with output %v", op.Output)
}

// StartBundler adds a trace of the beginning of a bundler operation.
func (t *Trace) StartBundler(input jsonutil.JSONToken) {
	t.pushOp(OpStartBundler{
		Input: jsonutil.Deepcopy(input),
	})
}

// EndBundler adds a trace of the end of a bundler operation.
func (t *Trace) EndBundler(output jsonutil.JSONToken) {
	t.pushOp(OpEndBundler{
		Output: jsonutil.Deepcopy(output),
	})
}

// OpStartProjectorCall is the beginning of a projector's invocation.
type OpStartProjectorCall struct {
	scopeStart
	ProjectorName string
	Args          []jsonutil.JSONMetaNode
	CtxSnapshot   string
}

// StartProjectorCall adds a trace of the beginning of a projector's invocation.
func (t *Trace) StartProjectorCall(projectorName string, args []jsonutil.JSONMetaNode, ctxSnapshot string) {
	t.pushOp(OpStartProjectorCall{
		ProjectorName: projectorName,
		Args:          args,
		CtxSnapshot:   ctxSnapshot,
	})
}

func (op OpStartProjectorCall) description() string {
	if op.ProjectorName == "" {
		return fmt.Sprintf("calling identity(%v)", op.Args)
	}

	return fmt.Sprintf("calling projector %q with %d arguments %v\n\tContext state:\n\t%s", op.ProjectorName, len(op.Args), op.Args, op.CtxSnapshot)
}

// OpEndProjectorCall is the end (and result) of a projector's invocation.
type OpEndProjectorCall struct {
	scopeEnd
	ProjectorName string
	CtxSnapshot   string
	Result        jsonutil.JSONToken
}

// EndProjectorCall adds a trace of the end (and result) of a projector's invocation.
func (t *Trace) EndProjectorCall(projectorName string, ctxSnapshot string, result jsonutil.JSONToken) {
	t.pushOp(OpEndProjectorCall{
		ProjectorName: projectorName,
		CtxSnapshot:   ctxSnapshot,
		Result:        jsonutil.Deepcopy(result),
	})
}

func (op OpEndProjectorCall) description() string {
	if op.ProjectorName == "" {
		return ""
	}
	return fmt.Sprintf("projector %q returning result: %v\n\tContext state:\n\t%s", op.ProjectorName, op.Result, op.CtxSnapshot)
}

// OpStartCloudFunctionCall is the beginning of a cloud function's call.
type OpStartCloudFunctionCall struct {
	scopeStart
	RequestURL  string
	Args        []jsonutil.JSONMetaNode
	CtxSnapshot string
}

// StartCloudFunctionCall adds a trace of the beginning of a cloud function's call.
func (t *Trace) StartCloudFunctionCall(requestURL string, args []jsonutil.JSONMetaNode, ctxSnapshot string) {
	t.pushOp(OpStartCloudFunctionCall{
		RequestURL:  requestURL,
		Args:        args,
		CtxSnapshot: ctxSnapshot,
	})
}

func (op OpStartCloudFunctionCall) description() string {
	return fmt.Sprintf("calling cloud function %s with %d arguments %v\n\tContext state:\n\t%s", op.RequestURL, len(op.Args), op.Args, op.CtxSnapshot)
}

// OpEndCloudFunctionCall is the end (and result) of a cloud function's call.
type OpEndCloudFunctionCall struct {
	scopeEnd
	RequestURL  string
	CtxSnapshot string
	Status      string
}

// EndCloudFunctionCall adds a trace of the end (and result) of a cloud function's call.
func (t *Trace) EndCloudFunctionCall(requestURL string, ctxSnapshot string, status string) {
	t.pushOp(OpEndCloudFunctionCall{
		RequestURL:  requestURL,
		CtxSnapshot: ctxSnapshot,
		Status:      status,
	})
}

func (op OpEndCloudFunctionCall) description() string {
	return fmt.Sprintf("cloud function %s returning status: %s\n\tContext state:\n\t%s", op.RequestURL, op.Status, op.CtxSnapshot)
}

// OpStartConditionCheck is the beginning of a field mapping condition check.
type OpStartConditionCheck struct {
	scopeStart
}

// StartConditionCheck adds a trace of the beginning of a field mapping condition check.
func (t *Trace) StartConditionCheck() {
	t.pushOp(OpStartConditionCheck{})
}

func (op OpStartConditionCheck) description() string {
	return "checking condition..."
}

// OpEndConditionCheck is the end (and result) of field mapping condition check.
type OpEndConditionCheck struct {
	scopeEnd
	Result bool
}

// EndConditionCheck adds a trace of end (and result) of field mapping condition check.
func (t *Trace) EndConditionCheck(result bool) {
	t.pushOp(OpEndConditionCheck{
		Result: result,
	})
}

func (op OpEndConditionCheck) description() string {
	return fmt.Sprintf("condition is %v", op.Result)
}

// OpValueResolved traces the completely resolved/evaluated value of some ValueSource.
type OpValueResolved struct {
	Value  jsonutil.JSONMetaNode
	Source string
}

// ValueResolved adds a trace of the completely resolved/evaluated value of some ValueSource.
func (t *Trace) ValueResolved(value jsonutil.JSONMetaNode, source string) {
	t.pushOp(OpValueResolved{
		Value:  value,
		Source: source,
	})
}

func (op OpValueResolved) description() string {
	return fmt.Sprintf("resolved %s as %v", op.Source, op.Value)
}

func (op OpValueResolved) indent() int {
	return indentNoScope
}

// OpContextScan traces the result of looking for a source in the context/argument ancestors.
type OpContextScan struct {
	Value   jsonutil.JSONMetaNode
	Segs    []string
	RemSegs []string
}

// ContextScan adds a trace of the result of looking for a source in the context/argument ancestors.
func (t *Trace) ContextScan(value jsonutil.JSONMetaNode, segs []string, remSegs []string) {
	t.pushOp(OpContextScan{
		Value:   value,
		Segs:    segs,
		RemSegs: remSegs,
	})
}

func (op OpContextScan) description() string {
	if cmp.Equal(op.Segs, op.RemSegs) {
		return fmt.Sprintf("attempted to find %v in context but no ancestor existed", op.Segs)
	}
	return fmt.Sprintf("got value from context source %v, remaining path is %v (relative to node %v)", op.Segs, op.RemSegs, op.Value)
}

func (op OpContextScan) indent() int {
	return indentNoScope
}

// OpStartFetchCall is the beginning of a fetch.
type OpStartFetchCall struct {
	scopeStart
	URL    string
	Method string
}

// StartFetchCall adds a trace of the beginning of a fetch.
func (t *Trace) StartFetchCall(url string, method string) {
	t.pushOp(OpStartFetchCall{
		URL:    url,
		Method: method,
	})
}

func (op OpStartFetchCall) description() string {
	return fmt.Sprintf("starting fetch call to url: %s with method %s", op.URL, op.Method)
}

// OpEndFetchCall is the end of a fetch.
type OpEndFetchCall struct {
	scopeEnd
	Response *json.RawMessage
	Error    error
}

// EndFetchCall adds a trace of the end of a fetch.
func (t *Trace) EndFetchCall(response *json.RawMessage, err error) {
	t.pushOp(OpEndFetchCall{
		Response: response,
		Error:    err,
	})
}

func (op OpEndFetchCall) description() string {
	if op.Error != nil {
		return fmt.Sprintf("fetch encountered an error %v", op.Error)
	}

	return fmt.Sprintf("fetch successfully returned response: %v", string(*op.Response))
}

// opFailure stores an error encountered during processing.
type opFailure struct {
	Error error
}

func (op opFailure) description() string {
	return fmt.Sprintf("\t!> %v", op.Error)
}

func (op opFailure) indent() int {
	return indentNoScope
}

func describeFieldTarget(field *mappb.FieldMapping) string {
	target := field.Target

	switch v := target.(type) {
	case *mappb.FieldMapping_TargetField:
		return fmt.Sprintf("field %q", v.TargetField)
	case *mappb.FieldMapping_TargetLocalVar:
		return fmt.Sprintf("var %q", v.TargetLocalVar)
	case *mappb.FieldMapping_TargetObject:
		return fmt.Sprintf("object %q", v.TargetObject)
	case nil:
		return "<self> (no target specified)"
	default:
		return "unknown target (has the proto definition changed and the code not been updated?)"
	}
}
