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

import * as path from 'path';
import {ExtensionContext, workspace} from 'vscode';
import {LanguageClient, LanguageClientOptions, ServerOptions, TransportKind} from 'vscode-languageclient';

let client: LanguageClient;

/**
 * Export this function as part of the LSP client.
 * It starts the language clients and connects the client with the server.
 * @param {ExtensionContext} context The context the extension runs on.
 */
export function activate(context: ExtensionContext) {
  const serverModule =
      context.asAbsolutePath(path.join('out', 'server', 'src', 'main.js'));
  const debugOptions = {execArgv: ['--nolazy', '--inspect=6009']};
  const serverOptions: ServerOptions = {
    run: {module: serverModule, transport: TransportKind.ipc},
    debug: {
      module: serverModule,
      transport: TransportKind.ipc,
      options: debugOptions
    }
  };
  const clientOptions: LanguageClientOptions = {
    // TODO(b/169611573): Configure the document selector to whistle language.
    // Configure it in package.json and client/package.json as well.
    documentSelector: [{scheme: 'file', language: 'plaintext'}],
    synchronize: {
      configurationSection: 'whistleLanguageClient',
      fileEvents: workspace.createFileSystemWatcher('**/.clientrc')
    }
  };

  client = new LanguageClient(
      'whistleLanguageClient', 'Whistle Language Client', serverOptions,
      clientOptions);

  client.start();
}

/**
 * Export this function as part of the LSP client.
 * When the session is asserted to end from the frontend, it closes the client.
 */
export function deactivate(): Thenable<void>|undefined {
  if (!client) {
    return undefined;
  }
  return client.stop();
}
