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

package wstlserver

import (
	"strconv"
)

const (
	// FallbackSessionID sentinel session used when the client has not specified a session. As use cases
	// arise will revisit whether to have dynamic server generated session ids.
	FallbackSessionID = "-1"

	// transientSessionID sentinel session that exists for the lifetime of a request.
	transientSessionID = "-2"
)

// Session maintains the state of an incremental transformation.
type Session struct {
	// Unique identifier of the session.
	id string

	// Whistle transformer context.
	Context *Context
}

// NewSession instantiates a session with either the provided sessionID or the FallbackSessionID.
func NewSession(sessionID string) (*Session, error) {
	// TODO  Pass in StorageClient (not nill)
	trans, err := NewContext(&storageClient{})
	if err != nil {
		return nil, err
	}
	if sessionID != "" {
		if _, err := strconv.ParseInt(sessionID, 10, 64); err != nil {
			return nil, err
		}
		return &Session{id: sessionID, Context: trans}, nil
	}
	return &Session{id: FallbackSessionID, Context: trans}, nil
}

// ID returns the session ID as a string.
func (s *Session) ID() string {
	return s.id
}
