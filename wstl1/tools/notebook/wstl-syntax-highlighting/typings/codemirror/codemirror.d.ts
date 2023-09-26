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

// For consistency with CodeMirror capitalization.
// tslint:disable-next-line:enforce-name-casing
import * as CodeMirror from 'codemirror';

/**
 * Define extra codemirror types that do not exist in the DefinitelyTyped
 * type resources.
 */
declare module 'codemirror' {
  // tslint:disable-next-line:no-any
  function defineSimpleMode(id: string, states: any): void;
}
