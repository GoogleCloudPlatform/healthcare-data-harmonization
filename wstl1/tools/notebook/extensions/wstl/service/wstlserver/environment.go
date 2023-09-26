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
	"fmt"
	"strconv"
	"sync"
)

// Environment maintains all activite sessions.
type Environment struct {
	Sessions *sync.Map
}

// NewEnvironment instantiates and initializes the environment that will manage all sessions.
func NewEnvironment() *Environment {
	return &Environment{
		Sessions: new(sync.Map),
	}
}

// Session returns the requested active session in the environment. Returns an error if the
// session does not exist.
func (e *Environment) Session(id string) (*Session, error) {
	_, err := strconv.ParseInt(id, 10, 64)
	if err != nil {
		return nil, fmt.Errorf("session id %q does not contain an integer", id)
	}
	sess, exists := e.Sessions.Load(id)
	if !exists {
		return nil, fmt.Errorf("session with id %q not found", id)
	}
	return sess.(*Session), nil
}

// CreateTransientSession creates a session used to fullfill a request and has a lifetime
// of the request.
func (e *Environment) CreateTransientSession() (*Session, error) {
	return NewSession(transientSessionID)
}

// CreateSession creates and adds a new session to the environment. Returns an error if a
// session with the supplied identifier already exists.
func (e *Environment) CreateSession(id string) (*Session, error) {
	if e.SessionExists(id) {
		return nil, fmt.Errorf("session id %q already exists", id)
	}
	sess, err := NewSession(id)
	if err != nil {
		return nil, err
	}
	e.Sessions.Store(id, sess)
	return sess, nil
}

// DestroySession removes the session from the current environment. Returns an error if
// the session does not exist.
func (e *Environment) DestroySession(id string) error {
	if !e.SessionExists(id) {
		return fmt.Errorf("session id %q doesn't exist", id)
	}
	e.Sessions.Delete(id)
	return nil
}

// SessionExists is a convenience function that checks whether a condition exists.
func (e *Environment) SessionExists(id string) bool {
	_, err := e.Session(id)
	return err == nil
}
