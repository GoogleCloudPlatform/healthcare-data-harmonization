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
import {CompletionItem, createConnection, InitializeResult, ProposedFeatures, TextDocumentPositionParams, TextDocuments, TextDocumentSyncKind} from 'vscode-languageserver';
import {TextDocument} from 'vscode-languageserver-textdocument';

import * as util from './util';


/**
 * Server sets up the necessary components for a Whistle language server to run
 * with Language Server Protocal. It enables the auto-completion of
 * Whistle built-in functions, which is fetched from the text file
 * server/function.txt.
 */
export class Server {
  suggestionList: CompletionItem[];
  documents: TextDocuments<TextDocument>;

  /**
   * Starts a Server which will suggest the built-in functions of Whistle
   * mapping language for auto-completion based on the functions list in the
   * functions file. If another set of suggestion targets is preferred over the
   * default built-in functions listed in server/functions.txt, specify the path
   * to the file containing the set of suggestion targets.
   * @param {string} pathToFunctions The path from server directory to the
   * functions file which contains all the available functions. Leave it blank
   * if you prefer the default Whistle built-in functions.
   * @param {string} pathToServer The path from the compiled server.js to the
   * server directory. Leave it blank if no pathToFunctions is specified or the
   * file is in server/.
   */
  constructor(
      pathToFunctions: string = 'function.txt',
      pathToServer: string = '../../../server') {
    this.suggestionList = util.getFunctions(pathToFunctions, pathToServer);
    this.documents = new TextDocuments(TextDocument);
  }

  onCompletion(textDocumentPosition: TextDocumentPositionParams):
      CompletionItem[] {
    {
      const doc = this.documents.get(textDocumentPosition.textDocument.uri);
      if (doc === undefined) {
        return util.NULL_LIST;
      } else {
        return util.suggestFor(
            doc, textDocumentPosition.position, this.suggestionList);
      }
    }
  }

  onInitialize(): InitializeResult {
    return {
      capabilities: {
        textDocumentSync: TextDocumentSyncKind.Incremental,
        completionProvider:
            {triggerCharacters: [util.TRIGGER_CHAR], resolveProvider: true}
      }
    };
  }


  /**
   * This method starts the connection with the client and suggests words for
   * the word completion items based on the input in the client and the
   * available functions from the functions file.
   */
  run() {
    const connection = createConnection(ProposedFeatures.all);

    this.documents.listen(connection);

    connection.onInitialize(this.onInitialize.bind(this));

    connection.onCompletion(this.onCompletion.bind(this));

    connection.onCompletionResolve((item: CompletionItem): CompletionItem => {
      return item;
    });

    connection.listen();
  }
}
