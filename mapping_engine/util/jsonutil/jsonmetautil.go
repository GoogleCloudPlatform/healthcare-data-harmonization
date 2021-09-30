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

package jsonutil

import (
	"fmt"
	"strconv"
	"strings"
)

// JSONMetaNode represents a JSONToken with at least a Parent and Key, and some way to derive a full
// JSON dot/bracket notation path.
type JSONMetaNode interface {
	Parent() JSONMetaNode
	Key() string
	Path() string
	ContentString(level int) string
	Provenance() Provenance
	ProvenanceString() string
}

// Provenance contains information on the source provenance of this node if it was created
// by the computation of some other nodes going through a function.
type Provenance struct {
	Sources  []JSONMetaNode
	Function string
}

// ShallowString returns the paths of the arguments and the function used to get this node. It does
// not recursively check for provenance of the parent nodes.
func (c Provenance) ShallowString() string {
	parents := []string{}
	for _, s := range c.Sources {
		str := "..."
		if s.Path() != s.Key() && s.Key() != "" {
			str = s.Path()
		} else if s.Key() != "" {
			str = "..." + s.Key()
		} else if s.Provenance().Function != "" {
			str = fmt.Sprintf("%s(...)", s.Provenance().Function)
		}

		parents = append(parents, str)
	}

	if c.Function != "" {
		return fmt.Sprintf("%s( %s )", c.Function, strings.Join(parents, ", "))
	}
	return strings.Join(parents, ", ")
}

// JSONMeta is a container for the common JSONMeta data.
type JSONMeta struct {
	key        string
	provenance Provenance
}

// NewJSONMeta creates a new JSONMeta with the given key and Provenance information.
func NewJSONMeta(key string, cp Provenance) JSONMeta {
	return JSONMeta{
		key:        key,
		provenance: cp,
	}
}

// Key returns this Meta's key (boxed array index like [1] for array elements,
// dotted keys like ".foo" for container fields).
func (jm JSONMeta) Key() string {
	return jm.key
}

// Parent returns this Meta's parent node, if this node was not computationally derived.
func (jm JSONMeta) Parent() JSONMetaNode {
	if len(jm.provenance.Sources) != 1 || jm.provenance.Function != "" {
		return nil
	}
	return jm.provenance.Sources[0]
}

// Provenance returns the provenance information of this node.
func (jm JSONMeta) Provenance() Provenance {
	return jm.provenance
}

// ContentString prints the meta content as a string.
func (jm JSONMeta) ContentString(_ int) string {
	return fmt.Sprintf("%s => %v", jm.Path(), jm.Parent())
}

// Path returns the full JSON path in dot/bracket notation to this Meta by following its parents.
func (jm JSONMeta) Path() string {
	sb := make([]string, 0)
	var c JSONMetaNode = jm
	for c != nil {
		sb = append([]string{c.Key()}, sb...)
		// Prepend a "." for non-index segments.
		if _, ok := c.Parent().(JSONMetaContainerNode); ok {
			sb = append([]string{"."}, sb...)
		}
		c = c.Parent()
	}

	return strings.TrimLeft(strings.Join(sb, ""), ".")
}

// ProvenanceString produces a string representing the approximate provenance of this JSONMeta. This
// uses Provenance.ShallowString and thus may contain incomplete information. It shall not
// include any content of the data.
func (jm JSONMeta) ProvenanceString() string {
	if jm.Key() == "" {
		return jm.provenance.ShallowString()
	}
	cp := jm.Provenance().ShallowString()
	if cp == "" {
		return jm.Key()
	}

	return fmt.Sprintf("%s.%s", cp, jm.Key())
}

// JSONMetaContainerNode is a JSONMetaNode with an additional Children value.
type JSONMetaContainerNode struct {
	// Value used intentionally to avoid modification.
	JSONMeta
	Children map[string]JSONMetaNode
}

// String produces a string representation of JSONMetaContainerNode, including its path.
func (j JSONMetaContainerNode) String() string {
	return fmt.Sprintf("(%q -> %s)", j.Path(), j.ContentString(0))
}

// ContentString produces a string representation of the contents of JSONMetaContainerNode.
func (j JSONMetaContainerNode) ContentString(level int) string {
	if level > 1 {
		return "{...}"
	}

	var o []string
	for k, v := range j.Children {
		vs := "null"
		if v != nil {
			vs = v.ContentString(level + 1)
		}
		o = append(o, fmt.Sprintf("%s:%v", k, vs))
	}
	return fmt.Sprintf("{%s}", strings.Join(o, ", "))
}

// JSONMetaArrayNode is a JSONMetaNode with an additional Array value.
type JSONMetaArrayNode struct {
	// Value used intentionally to avoid modification.
	JSONMeta
	Items []JSONMetaNode
}

// String produces a string representation of JSONMetaArrayNode, including its path.
func (j JSONMetaArrayNode) String() string {
	return fmt.Sprintf("(%q -> %s)", j.Path(), j.ContentString(0))
}

// ContentString produces a string representation of the contents of JSONMetaArrayNode.
func (j JSONMetaArrayNode) ContentString(level int) string {
	if level > 1 {
		return "[...]"
	}

	var o []string
	for _, v := range j.Items {
		vs := "null"
		if v != nil {
			vs = v.ContentString(level + 1)
		}
		o = append(o, vs)
	}

	return fmt.Sprintf("[%s]", strings.Join(o, ", "))
}

// JSONMetaPrimitiveNode is a JSONMetaNode with an additional primitive value.
type JSONMetaPrimitiveNode struct {
	// Value used intentionally to avoid modification.
	JSONMeta
	Value JSONPrimitive
}

// String produces a string representation of JSONMetaPrimitiveNode, including its path.
func (j JSONMetaPrimitiveNode) String() string {
	return fmt.Sprintf("(%q -> %s)", j.Path(), j.ContentString(0))
}

// ContentString produces a string representation of the contents of JSONMetaPrimitiveNode.
func (j JSONMetaPrimitiveNode) ContentString(_ int) string {
	return fmt.Sprintf("%v", j.Value)
}

// TokenToNode converts a JSONToken to a JSONMetaNode, filling the meta data.
func TokenToNode(token JSONToken) (JSONMetaNode, error) {
	return tokenToNode(Provenance{}, "", token)
}

// TokenToNodeWithProvenance converts a JSONToken to a JSONMetaNode, filling the meta data with the
// given provenance.
func TokenToNodeWithProvenance(token JSONToken, key string, cp Provenance) (JSONMetaNode, error) {
	return tokenToNode(cp, key, token)
}

func tokenToNode(cp Provenance, key string, token JSONToken) (JSONMetaNode, error) {
	m := JSONMeta{key: key, provenance: cp}
	switch t := token.(type) {
	case JSONContainer:
		cn := JSONMetaContainerNode{JSONMeta: m, Children: make(map[string]JSONMetaNode)}
		for k, v := range t {
			vn, err := tokenToNode(Provenance{Sources: []JSONMetaNode{cn}}, k, *v)
			if err != nil {
				return nil, err
			}
			cn.Children[k] = vn
		}
		return cn, nil
	case JSONArr:
		an := JSONMetaArrayNode{JSONMeta: m, Items: make([]JSONMetaNode, 0, len(t))}
		for i, v := range t {
			// "[i]" is the key.
			vn, err := tokenToNode(Provenance{Sources: []JSONMetaNode{an}}, "["+strconv.Itoa(i)+"]", v)
			if err != nil {
				return nil, err
			}
			an.Items = append(an.Items, vn)
		}
		return an, nil
	case JSONStr, JSONNum, JSONBool:
		return JSONMetaPrimitiveNode{JSONMeta: m, Value: t.(JSONPrimitive)}, nil
	case nil:
		return nil, nil
	default:
		return nil, fmt.Errorf("unexpected token type: %T", t)
	}
}

// NodeToToken converts a JSONMetaNode to a JSONToken, losing the meta data.
func NodeToToken(node JSONMetaNode) (JSONToken, error) {
	switch n := node.(type) {
	case JSONMetaContainerNode:
		jc := make(JSONContainer)
		for k, v := range n.Children {
			t, err := NodeToToken(v)
			if err != nil {
				return nil, err
			}
			jc[k] = &t
		}
		return jc, nil
	case JSONMetaArrayNode:
		ja := make(JSONArr, 0, len(n.Items))
		for _, v := range n.Items {
			t, err := NodeToToken(v)
			if err != nil {
				return nil, err
			}
			ja = append(ja, t)
		}
		return ja, nil
	case JSONMetaPrimitiveNode:
		return n.Value.(JSONToken), nil
	case nil:
		return nil, nil
	default:
		return nil, fmt.Errorf("unexpected node type: %T", n)
	}
}

// GetNodeField returns the child given a path in dot/bracket notation like foo.bar[3].baz
func GetNodeField(node JSONMetaNode, path string) (JSONMetaNode, error) {
	segs, err := SegmentPath(path)
	if err != nil {
		return nil, fmt.Errorf("failed to segment path: %v", err)
	}
	return GetNodeFieldSegmented(node, segs)
}

// GetNodeFieldSegmented returns the child given a path in parsed dot/bracket notation like
// ["foo", "bar", "[3]", "baz"]
func GetNodeFieldSegmented(node JSONMetaNode, segments []string) (JSONMetaNode, error) {
	n, _, err := getNodeFieldSegmented(node, segments)
	return n, err
}

// getNodeFieldSegmented finds the node given a segmented path. It returns the node (if found) and
// a boolean indicating whether an array expansion [*] occurred somewhere in the given path.
func getNodeFieldSegmented(node JSONMetaNode, segments []string) (JSONMetaNode, bool, error) {
	if len(segments) == 0 {
		return node, false, nil
	}

	seg := segments[0]

	switch n := node.(type) {
	case JSONMetaPrimitiveNode:
		return nil, false, fmt.Errorf("attempted to key into primitive with %q", seg)
	case JSONMetaArrayNode:
		if !IsIndex(seg) {
			return nil, false, fmt.Errorf("expected an array index with brackets like [123] or [*] but got %q", seg)
		}

		idxSubstr := seg[1 : len(seg)-1]

		if idxSubstr == "*" {
			retNode := JSONMetaArrayNode{
				JSONMeta: n.JSONMeta,
			}

			for i := range n.Items {
				f, expand, err := getNodeFieldSegmented(n.Items[i], segments[1:])
				if err != nil {
					return nil, false, fmt.Errorf("error expanding [*] on item index %d: %v", i, err)
				}

				// If an array expansion occurs down the line, we need to unnest the resulting array here.
				if expand {
					fArr, ok := f.(JSONMetaArrayNode)
					if !ok {
						return nil, false, fmt.Errorf("bug: getNodeFieldSegmented returned true for expansion but value was not an array (was %T)", f)
					}
					retNode.Items = append(retNode.Items, fArr.Items...)
				} else {
					retNode.Items = append(retNode.Items, f)
				}
			}

			return retNode, true, nil
		}

		idx, err := strconv.Atoi(idxSubstr)
		if err != nil {
			return nil, false, fmt.Errorf("could not parse array index %q: %v", seg, err)
		}

		if idx < 0 {
			return nil, false, fmt.Errorf("negative array indices are not supported but got %d", idx)
		}
		if idx >= len(n.Items) {
			// TODO(): Consider returning a different value for fields that don't exist vs
			// fields that are actually set to null.
			return nil, false, nil
		}

		return getNodeFieldSegmented(n.Items[idx], segments[1:])
	case JSONMetaContainerNode:
		if IsIndex(seg) {
			return nil, false, fmt.Errorf("expected an object key, but got an array index %q", seg)
		}

		if val, ok := n.Children[seg]; ok {
			return getNodeFieldSegmented(val, segments[1:])
		}
		// TODO(): Consider returning a different value for fields that don't exist vs
		// fields that are actually set to null.
		return nil, false, nil
	case nil:
		return nil, false, nil
	default:
		return nil, false, fmt.Errorf("found node of un-navigable type %T", node)
	}
}
