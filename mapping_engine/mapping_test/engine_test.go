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

package mapping_test

import (
	"encoding/json"
	"testing"

	"github.com/google/go-cmp/cmp" /* copybara-comment: cmp */
	"github.com/google/go-cmp/cmp/cmpopts" /* copybara-comment: cmpopts */

	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/mapping" /* copybara-comment: mapping */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/projector" /* copybara-comment: projector */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/types" /* copybara-comment: types */
	"github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/util/jsonutil" /* copybara-comment: jsonutil */

	mappb "github.com/GoogleCloudPlatform/healthcare-data-harmonization/mapping_engine/proto" /* copybara-comment: mapping_go_proto */
)

func mustParseContainer(json json.RawMessage, t *testing.T) jsonutil.JSONContainer {
	t.Helper()
	c := make(jsonutil.JSONContainer)

	err := c.UnmarshalJSON(json)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func mustParseArray(json json.RawMessage, t *testing.T) jsonutil.JSONArr {
	t.Helper()
	c := make(jsonutil.JSONArr, 0)

	err := c.UnmarshalJSON(json)
	if err != nil {
		t.Fatal(err)
	}
	return c
}

func mustSegmentPath(path string, t *testing.T) []string {
	segs, err := jsonutil.SegmentPath(path)
	if err != nil {
		t.Fatalf("failed to segment %q: %v", path, segs)
	}
	return segs
}

func udfGetBar(container jsonutil.JSONContainer) (jsonutil.JSONToken, error) {
	return jsonutil.GetField(container, "bar")
}

func udfMakeFooBar(foo jsonutil.JSONToken, bar jsonutil.JSONToken) (jsonutil.JSONToken, error) {
	return jsonutil.JSONContainer{
		"foo": &foo,
		"bar": &bar,
	}, nil
}

func buildStackMap(layers ...map[string]jsonutil.JSONToken) types.StackMapInterface {
	sm := types.StackMap{}
	if len(layers) == 0 {
		sm.Push()
	}
	for _, l := range layers {
		sm.Push()
		for k, v := range l {
			vc := v
			sm.Set(k, &vc)
		}
	}
	return &sm
}

func makeTreeFromLeaves(t *testing.T, leaves map[string]jsonutil.JSONToken) jsonutil.JSONToken {
	var root jsonutil.JSONToken = make(jsonutil.JSONContainer)
	for k, v := range leaves {
		if err := jsonutil.SetField(v, k, &root, true, false); err != nil {
			t.Fatalf("error making tree from leaves: can't set %q to %v: %v", k, v, err)
		}
	}
	return root
}

// toNodes converts the given array of tokens into nodes with meta data.
func toNodes(t *testing.T, tokens []jsonutil.JSONToken) []jsonutil.JSONMetaNode {
	t.Helper()
	nodes := make([]jsonutil.JSONMetaNode, len(tokens))
	for i, arg := range tokens {
		tok, err := jsonutil.TokenToNode(arg)
		if err != nil {
			t.Fatalf("error convering tokens to nodes: %v", err)
		}
		nodes[i] = tok
	}
	return nodes
}

func TestFindParentAndRemainderFromArgs(t *testing.T) {
	inputTree, err := jsonutil.TokenToNode(makeTreeFromLeaves(t, map[string]jsonutil.JSONToken{
		"group1.group2[0].array1[0].foo": jsonutil.JSONNum(1),
		"group1.group2[0].array1[0].bar": jsonutil.JSONNum(2),
		"group1.group2[1].array1[0].foo": jsonutil.JSONNum(3),
		"group1.group2[1].array1[0].bar": jsonutil.JSONNum(4),
		"group1.group2[1].array1[1].foo": jsonutil.JSONNum(5),
		"group1.group2[1].array2[0].foo": jsonutil.JSONNum(6),
		"group1.group2[2].sibling1":      jsonutil.JSONNum(7),
		"group1.group2[2].sibling2":      jsonutil.JSONNum(8),
		"group1.container.item1":         jsonutil.JSONNum(9),
		"group1.container.item2":         jsonutil.JSONNum(10),
		"group1.something.item3":         jsonutil.JSONNum(11),
		"groupX.other":                   jsonutil.JSONNum(12),
		"group1.item4":                   jsonutil.JSONNum(13),
	}))
	if err != nil {
		t.Fatalf("error creating test tree: %v", err)
	}

	tests := []struct {
		name         string
		argPaths     []string
		segs         []string
		wantNodePath string
		wantFinal    jsonutil.JSONToken
		wantSegs     []string
	}{
		{
			name:         "non-array sibling",
			argPaths:     []string{"group1.container.item1"},
			segs:         mustSegmentPath("group1.container.item2", t),
			wantNodePath: "group1.container",
			wantFinal:    jsonutil.JSONNum(10),
			wantSegs:     []string{"item2"},
		},
		{
			name:         "array sibling",
			argPaths:     []string{"group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1.bar", t),
			wantNodePath: "group1.group2[1].array1[0]",
			wantFinal:    jsonutil.JSONNum(4),
			wantSegs:     mustSegmentPath("bar", t),
		},
		{
			name:         "non-array ancestor",
			argPaths:     []string{"group1.container.item1"},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "group1",
			wantFinal:    jsonutil.JSONNum(11),
			wantSegs:     mustSegmentPath("something.item3", t),
		},
		{
			name:         "array ancestor",
			argPaths:     []string{"group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1[1].foo", t),
			wantNodePath: "group1.group2[1].array1",
			wantFinal:    jsonutil.JSONNum(5),
			wantSegs:     mustSegmentPath("[1].foo", t),
		},
		{
			name:         "deep array ancestor",
			argPaths:     []string{"group1.group2[1].array2[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1[1].foo", t),
			wantNodePath: "group1.group2[1]",
			wantFinal:    jsonutil.JSONNum(5),
			wantSegs:     mustSegmentPath("array1[1].foo", t),
		},
		{
			name:         "multiple args uses longest match",
			argPaths:     []string{"group1.container.item1", "group1.group2[1].array1[0].foo"},
			segs:         mustSegmentPath("group1.group2.array1.bar", t),
			wantNodePath: "group1.group2[1].array1[0]",
			wantFinal:    jsonutil.JSONNum(4),
			wantSegs:     mustSegmentPath("bar", t),
		},
		{
			name:         "root is only common ancestor",
			argPaths:     []string{"groupX.other"},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "",
			wantFinal:    jsonutil.JSONNum(11),
			wantSegs:     mustSegmentPath("group1.something.item3", t),
		},
		{
			name:         "reference to ancestor",
			argPaths:     []string{"group1.group2[1].array2[0].foo"},
			segs:         mustSegmentPath("group1", t),
			wantNodePath: "group1",
			wantFinal:    toToken(t, mustGetNodeField(t, inputTree, "group1")),
			wantSegs:     []string{},
		},
		{
			name:         "no args",
			argPaths:     []string{},
			segs:         mustSegmentPath("group1.something.item3", t),
			wantNodePath: "nil",
			wantFinal:    nil,
			wantSegs:     mustSegmentPath("group1.something.item3", t),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			args := make([]jsonutil.JSONMetaNode, 0, len(test.argPaths))
			for _, p := range test.argPaths {
				n, err := jsonutil.GetNodeField(inputTree, p)
				if err != nil {
					t.Fatalf("test wants arg %q but this is not a valid path on the input: %v", p, err)
				}
				args = append(args, n)
			}

			gotNode, gotSegs, err := mapping.ParentInfoFromArgs(args, test.segs)
			if err != nil {
				t.Fatalf("ParentInfoFromArgs(%v, %v) => unexpected error: %v", args, test.segs, err)
			}

			finalNode, err := jsonutil.GetNodeFieldSegmented(gotNode, gotSegs)
			if err != nil {
				t.Fatalf("returned path %v is not valid: %v", gotSegs, err)
			}

			finalToken := toToken(t, finalNode)

			if diff := cmp.Diff(finalToken, test.wantFinal); diff != "" {
				t.Errorf("finalToken = %v want %v, diff:\n%s", finalToken, test.wantFinal, diff)
			}
			if !cmp.Equal(test.wantSegs, gotSegs) {
				t.Errorf("gotSegs = %v, want %v", gotSegs, test.wantSegs)
			}
		})
	}
}

func toToken(t *testing.T, node jsonutil.JSONMetaNode) jsonutil.JSONToken {
	t.Helper()
	tok, err := jsonutil.NodeToToken(node)
	if err != nil {
		t.Fatalf("could not convert %v to token: %v", node, err)
	}
	return tok
}

func TestEvaluateArgSource(t *testing.T) {
	inputTree, err := jsonutil.TokenToNode(makeTreeFromLeaves(t, map[string]jsonutil.JSONToken{
		"group1.group2[0].array1[0].foo": jsonutil.JSONNum(1),
		"group1.group2[0].array1[0].bar": jsonutil.JSONNum(2),
		"group1.group2[1].array1[0].foo": jsonutil.JSONNum(3),
		"group1.group2[1].array1[0].bar": jsonutil.JSONNum(4),
		"group1.group2[1].array1[1].foo": jsonutil.JSONNum(5),
		"group1.group2[1].array2[0].foo": jsonutil.JSONNum(6),
		"group1.group2[2].sibling1":      jsonutil.JSONNum(7),
		"group1.group2[2].sibling2":      jsonutil.JSONNum(8),
		"group1.container.item1":         jsonutil.JSONNum(9),
		"group1.container.item2":         jsonutil.JSONNum(10),
		"group1.something.item3":         jsonutil.JSONNum(11),
		"groupX.other":                   jsonutil.JSONNum(12),
	}))
	if err != nil {
		t.Fatalf("error creating test tree: %v", err)
	}
	tests := []struct {
		name  string
		argVs mappb.ValueSource_FromInput
		args  []jsonutil.JSONMetaNode
		want  jsonutil.JSONMetaNode
	}{
		{
			name:  "single arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "groupX.other"}},
			args:  []jsonutil.JSONMetaNode{inputTree},
			want:  mustGetNodeField(t, inputTree, "groupX.other"),
		},
		{
			name:  "single arg with array",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "group1.group2"}},
			args:  []jsonutil.JSONMetaNode{inputTree},
			want:  mustGetNodeField(t, inputTree, "group1.group2"),
		},
		{
			name:  "single arg which is itself array",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "[0]"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0]"),
		},
		{
			name:  "single enumerated arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: "group2[]"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1")},
			want:  mustGetNodeField(t, inputTree, "group1.group2"),
		},
		{
			name:  "empty ref to one arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 1, Field: ""}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other")},
			want:  mustGetNodeField(t, inputTree, "groupX.other"),
		},
		{
			name:  "numeric ref to multiple args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 2, Field: ""}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  mustGetNodeField(t, inputTree, "group1.something.item3"),
		},
		{
			name:  "context ref (to nothing) with multiple args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "context"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "groupX.other"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  nil,
		},
		{
			name:  "(numeric) context ref (to nothing) with no args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "1"}},
			args:  []jsonutil.JSONMetaNode{},
			want:  nil,
		},
		{
			name:  "context ref with args",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "group1.group2.array1.bar"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].foo"), mustGetNodeField(t, inputTree, "group1.something.item3")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].bar"),
		},
		{
			name:  "context ref with single arg",
			argVs: mappb.ValueSource_FromInput{FromInput: &mappb.ValueSource_InputSource{Arg: 0, Field: "group1.group2.array1.bar"}},
			args:  []jsonutil.JSONMetaNode{mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].foo")},
			want:  mustGetNodeField(t, inputTree, "group1.group2[0].array1[0].bar"),
		},
	}
	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			got, err := mapping.EvaluateArgSource(test.argVs.FromInput, test.args, types.NewContext(types.NewRegistry()))

			if err != nil {
				t.Fatalf("evaluateFromSource(%v, %v, <new context>) unexpected error %v", test.argVs, test.args, err)
			}

			if diff := cmp.Diff(got, test.want, cmpopts.IgnoreUnexported(jsonutil.JSONMeta{})); diff != "" {
				t.Errorf("evaluateFromSource(%v, %v, <new context>) => %v want %v diff %s", test.argVs, test.args, got, test.want, diff)
			}
		})
	}
}

func mustGetNodeField(t *testing.T, root jsonutil.JSONMetaNode, path string) jsonutil.JSONMetaNode {
	n, err := jsonutil.GetNodeField(root, path)
	if err != nil {
		t.Fatalf("could not find node %q in %v: %v", path, root, err)
	}
	return n
}

func buildProjector(t *testing.T, fn interface{}) types.Projector {
	t.Helper()
	p, err := projector.FromFunction(fn, "test")
	if err != nil {
		t.Fatalf("failed to build projector: %v", err)
	}
	return p
}

func mustTokenToNode(t *testing.T, token jsonutil.JSONToken) jsonutil.JSONMetaNode {
	t.Helper()
	n, err := jsonutil.TokenToNode(token)
	if err != nil {
		t.Fatalf("failed to convert token %v to node: %v", token, err)
	}
	return n
}
