// Copyright 2020 Google LLC.
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

package errors

import (
	"fmt"

	"google.golang.org/protobuf/proto" /* copybara-comment: proto */
	"google.golang.org/protobuf/reflect/protoreflect" /* copybara-comment: protoreflect */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// ProtoLocation is an implementation of error that generates locations from proto messages.
type ProtoLocation struct {
	name            string
	msg             proto.Message
	isFunctionStart bool
}

// NewProtoLocation generates a ProtoLocation based on a given message and its parent message. The
// parent message is used to derive a name for the proto message value (i.e. based on which field in
// the parent contains the given message value).
func NewProtoLocation(msg, parent proto.Message) ProtoLocation {
	fields := msg.ProtoReflect().Descriptor().Fields()
	name := findNameFieldValue(msg, fields)

	// Try to build the name by finding the field in the parent that contains the message.
	if name == "" && parent != nil {
		name = getNameFromParentField(msg, parent)
	}

	return ProtoLocation{
		name:            name,
		msg:             msg,
		isFunctionStart: isProtoMessageFunction(msg),
	}
}

func findNameFieldValue(msg proto.Message, fields protoreflect.FieldDescriptors) string {
	if f := fields.ByName("name"); f != nil {
		return msg.ProtoReflect().Get(f).String()
	}
	return ""
}

// getNameFromParentField finds a field in the parent message that contains the given msg, and
// returns its name.
func getNameFromParentField(msg, parent proto.Message) string {
	parentFields := parent.ProtoReflect().Descriptor().Fields()
	for i := 0; i < parentFields.Len(); i++ {
		f := parentFields.Get(i)

		pfi := parent.ProtoReflect().Get(f).Interface()
		switch pfit := pfi.(type) {
		case protoreflect.List:
			for j := 0; j < pfit.Len(); j++ {
				item := pfit.Get(j)
				if itemMsg, ok := item.Interface().(protoreflect.Message); ok && proto.Equal(itemMsg.Interface(), msg) {
					return fmt.Sprintf("%s %s", SuffixNumber(j+1), f.Name())
				}
			}
		case protoreflect.Message:
			if proto.Equal(pfit.Interface(), msg) {
				return string(f.Name())
			}
		}
	}
	return ""
}

// NewProtoLocationf generates a ProtoLocation based on a given message and a format string with
// format arguments. The format string is used as a name for the proto message and the message
// itself is used for its type.
func NewProtoLocationf(msg proto.Message, nameFormat string, nameFormatArgs ...interface{}) ProtoLocation {
	return ProtoLocation{
		name:            fmt.Sprintf(nameFormat, nameFormatArgs...),
		msg:             msg,
		isFunctionStart: isProtoMessageFunction(msg),
	}
}

func (p ProtoLocation) String() string {
	return p.Error()
}

func (p ProtoLocation) Error() string {
	prefix, suffix := DefaultPrefix, DefaultSuffix
	if p.isFunctionStart {
		prefix, suffix = FunctionStartPrefix, FunctionStartSuffix
	}

	if vs, ok := p.msg.(*mappb.ValueSource); ok && p.name == "value_source" && vs.Projector != "" {
		return fmt.Sprintf("%sEvaluating arguments for %s%s", prefix, vs.Projector, suffix)
	}

	typ := string(p.msg.ProtoReflect().Type().Descriptor().FullName().Name())

	if p.name == "" {
		return prefix + typ + suffix
	}

	return fmt.Sprintf("%s%s [%s]%s", prefix, p.name, typ, suffix)
}

func isProtoMessageFunction(msg proto.Message) bool {
	projDef := &mappb.ProjectorDefinition{}
	return msg.ProtoReflect().Type().Descriptor().FullName() == projDef.ProtoReflect().Type().Descriptor().FullName()
}
