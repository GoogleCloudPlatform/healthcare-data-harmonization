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

import {Server} from './server';
// TODO(): Keep the function.txt updated with the Whistle built-in
// functions.
/**
 * The input functions file is default to server/function.txt. To use another
 * functions file, put the file in server/ and put the file name as the first
 * parameter of the Server(), or direct to the file location directly by using
 * Server('<file name>', '<relative path from the compiled server.js file at
 * out/server/src/server.js to the directory containing the functions file>').
 */
const server = new Server();
server.run();
