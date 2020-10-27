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
