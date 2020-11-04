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
import * as fs from 'fs';
import * as path from 'path';

import {CompletionItem, CompletionItemKind} from 'vscode-languageserver';
import {Position, TextDocument} from 'vscode-languageserver-types';

/**
 * The null array of CompletionItem. It contains an empty CompletionItem to
 * prevent being recognized as array, which will cause the onCompletion to
 * create another list.
 */
export const NULL_LIST: CompletionItem[] = [{label: ''}];
/** The trigger character for the user to indicate the Whistle function. */
export const TRIGGER_CHAR: string = '$';

/**
 * suggestFor returns a list of CompletionItem based on the position and
 * preceeding character of a position in a TextDocument. It will return the
 * CompletionItem given to it if the position in the TextDocument is preceeded
 * by the TRIGGER_CHAR and NULL_LIST otherwise.
 * @param {TextDocument} document The TextDocument the suggestion is based on.
 * @param {Position} position The position in the document that is being edited.
 * @param {CompletionItem} items The list of completion items available for
 * suggestion.
 */
export function suggestFor(
    document: TextDocument, position: Position,
    items: CompletionItem[]): CompletionItem[] {
  const lines = document.getText().split('\n');
  if (position.line >= 0 && position.line < lines.length) {
    if (position.character >= 1 &&
        position.character <= lines[position.line].length) {
      const char = document.getText().split('\n')[position.line].charAt(
          position.character - 1);
      if (char === TRIGGER_CHAR) {
        return items;
      }
    } else {
      console.log(`Character index out of bound. Expected range: [1, ${
          lines[position.line].length}] Received index:${position.character}]`);
    }
  } else {
    console.log(`Line index out of bound. Expected range: [0, ${
        lines.length - 1}], received index: ${position.line}`);
  }
  return NULL_LIST;
}

/**
 * The function getFunctions is exported as a util function. It reads the text
 * file containing the built-in functions and parses it into a list of
 * CompletionItem.
 * @param {string} pathToFunctions The path from server directory to the
 * functions file which contains all the available functions.
 * @param {string} pathToServer The path from the compiled util.js to the
 * server directory.
 */
export function getFunctions(
    pathToFunctions: string, pathToServer: string): CompletionItem[] {
  // TODO(b/169611573): Store the details and documentations of the functions
  // in some format and read them here.
  const joinPath = path.join(__dirname, pathToServer, pathToFunctions);
  let wordString = '';
  try {
    wordString = fs.readFileSync(joinPath, 'utf8');
  } catch (err) {
    if (err.code === 'ENOENT') {
      console.log(
          `Function file ${pathToServer}/${pathToFunctions} does not exist.`);
    } else {
      console.log(
          `Unknown error observed when reading the function file: ${err}`);
    }
  }
  return parseFunctionsFile(wordString);
}

function parseFunctionsFile(wordString: string): CompletionItem[] {
  const regEx = /\n+/gi;
  const oneLine = wordString.replace(regEx, ',');
  const trimmed = new Set(oneLine.split(',').map(x => x.trim()));
  trimmed.delete('');
  let wordCompletionItems: CompletionItem[] = [];
  trimmed.forEach(word => {
    wordCompletionItems.push({label: word, kind: CompletionItemKind.Function});
  });
  if (wordCompletionItems.length === 0) {
    wordCompletionItems = NULL_LIST;
  }
  return wordCompletionItems;
}
