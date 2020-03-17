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

package transpiler

import (
	mpb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

// Welcome to a language with no generics :'(

type valueStack []*mpb.ValueSource

func (s *valueStack) push(v *mpb.ValueSource) *mpb.ValueSource {
	*s = append(*s, v)
	return v
}

func (s *valueStack) peek() *mpb.ValueSource {
	return (*s)[len(*s)-1]
}

func (s *valueStack) pop() *mpb.ValueSource {
	p := s.peek()
	*s = (*s)[:len(*s)-1]
	return p
}

// and creates a ValueSource from this valueStack, along with any other ValueSources, by passing
// them all through _And. This requires all sources in the stack and arguments to be boolean.
func (s *valueStack) and(others ...*mpb.ValueSource) *mpb.ValueSource {
	if len(*s) == 0 && len(others) == 0 {
		return nil
	}

	if len(*s) == 1 && len(others) == 0 {
		return s.peek()
	}
	if len(*s) == 0 && len(others) == 1 {
		return others[0]
	}

	vs := &mpb.ValueSource{
		Projector: "$And",
	}

	addArgs(vs, *s...)
	addArgs(vs, others...)

	return vs
}

// addArgs adds the given args to the given ValueSource, as sources. The first arg will go into
// Source, while the rest will become additionalArgs. Does not overwrite existing sources/args,
// appends everything.
func addArgs(vs *mpb.ValueSource, arg ...*mpb.ValueSource) {
	for _, a := range arg {
		if vs.Source == nil {
			if a.Projector == "" {
				vs.Source = a.Source
			} else {
				vs.Source = &mpb.ValueSource_ProjectedValue{
					ProjectedValue: a,
				}
			}
		} else {
			vs.AdditionalArg = append(vs.AdditionalArg, a)
		}
	}
}

func projectAndSimplify(projector string, args ...*mpb.ValueSource) *mpb.ValueSource {
	vs := &mpb.ValueSource{
		Projector: projector,
	}

	addArgs(vs, args...)

	return vs
}
