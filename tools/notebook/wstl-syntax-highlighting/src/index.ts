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

import 'codemirror/mode/meta';
import 'codemirror/addon/mode/simple';

import {JupyterFrontEnd, JupyterFrontEndPlugin} from '@jupyterlab/application';
import {CodeCell, CodeCellModel} from '@jupyterlab/cells';
import {CodeMirrorEditor, Mode} from '@jupyterlab/codemirror';
import {INotebookTracker, NotebookPanel} from '@jupyterlab/notebook';
import CodeMirror from 'codemirror';

const MAGIC_COMMAND_PREFIX = '%%wstl';
const WHISTLE_MODE = 'wstl';
const WHISTLE_MODE_MIME_TYPE = 'text/x-wstl';
const WHISTLE_EXT = 'wstl';
const DEFAULT_MODE = 'ipython';

function defineWhistleMode(app: JupyterFrontEnd): void {
  // TODO: autogenerate rules from whistle grammar.
  CodeMirror.defineSimpleMode(WHISTLE_MODE, {
    // The start state contains the rules that are intially used.
    start: [
      // Strings.
      {regex: /"(?:[^\\]|\\.)*?(?:"|$)/, token: 'string'},
      // Builtin functions.
      {regex: /\$([a-zA-Z_\d]*)/, token: ['keyword', null, 'def']},
      // Iterator brackets.
      {regex: /\[\]/, token: 'meta'},
      // Function declarations.
      {
        regex: /(def)(\s+)([a-zA-Z_$][\w$]*)/,
        token: ['keyword', null, 'variable-2']
      },
      // Rules are matched in the order in which they appear, so there is
      // no ambiguity between this one and the one above.
      {
        regex:
            /(\$root|\$this)|(?:def|if|where|else|var|out|root|dest|post|this|\^)\b/,
        token: ['builtin', null, 'keyword']
      },
      {regex: /true|false|null|undefined|required/, token: 'builtin'},
      {
        regex: /0x[a-f\d]+|\s+[-+]?(?:\.\d+|\d+\.?\d*)(?:e[-+]?\d+)?/i,
        token: 'number'
      },
      {regex: /\/\/.*/, token: 'comment'},
      // Language operators.
      {regex: /[-+\/*=<>!~?@]+|and|or|\[\*\]/, token: 'operator'},
      // Indent and dedent properties guide autoindentation.
      {regex: /[\{\[\(]/, indent: true},
      {regex: /[\}\]\)]/, dedent: true},
      {regex: /[a-z$][\w$]*/, token: 'variable'},
    ],
    // The meta property contains global information about the mode. It
    // can contain properties like lineComment, which are supported by
    // all modes, and also directives like dontIndentStates, which are
    // specific to simple modes.
    meta: {dontIndentStates: ['comment'], lineComment: '//'}
  });
  CodeMirror.defineMIME(WHISTLE_MODE_MIME_TYPE, WHISTLE_MODE);
  Mode.getModeInfo().push({
    ext: [WHISTLE_EXT],
    mime: WHISTLE_MODE_MIME_TYPE,
    mode: WHISTLE_MODE,
    name: WHISTLE_MODE
  });
  app.docRegistry.addFileType({
    name: 'wstl',
    displayName: 'Whistle File',
    extensions: ['.wstl'],
    mimeTypes: [WHISTLE_MODE_MIME_TYPE],
    iconClass: 'jp-MaterialIcon reactivecore_icon',
    iconLabel: '',
    contentType: 'file',
    fileFormat: 'text'
  });
}

function toggleWhisteMode(cell: CodeCell): void {
  if (cell.editor instanceof CodeMirrorEditor) {
    const editor = cell.editor as CodeMirrorEditor;
    const cellContent = editor.model.value.text;
    const mode = editor.getOption('mode');
    if (mode === WHISTLE_MODE &&
        !cellContent.startsWith(MAGIC_COMMAND_PREFIX)) {
      editor.setOption('mode', DEFAULT_MODE);
      editor.refresh();
    } else if (
        mode !== WHISTLE_MODE && cellContent.startsWith(MAGIC_COMMAND_PREFIX)) {
      editor.setOption('mode', WHISTLE_MODE);
      editor.refresh();
    }
  }
}

function activateExtension(
    app: JupyterFrontEnd, nbTracker: INotebookTracker): void {
  defineWhistleMode(app);

  nbTracker.currentChanged.connect(
      (sender: INotebookTracker, panel: NotebookPanel) => {
        panel.context.ready.then(() => {
          for (const cell of panel.content.widgets) {
            const codeCell = cell as CodeCell;
            if (codeCell.model instanceof CodeCellModel) {
              ((cellModel: CodeCellModel, cc: CodeCell) => {
                const model = cc.model as CodeCellModel;
                model.contentChanged.connect((slot: unknown, args: unknown) => {
                  toggleWhisteMode(cc);
                });
              })(codeCell.model, codeCell);
            }
            toggleWhisteMode(codeCell);
          }
          panel.update();
        });
      });
}

/**
 * Initialization data for the wstl-syntax-highlighting extension.
 */
const extension: JupyterFrontEndPlugin<void> = {
  id: 'wstl-syntax-highlighting',
  autoStart: true,
  requires: [INotebookTracker],
  activate: activateExtension,
};

// Jupyterlab requires plugins to be exported as default.
// tslint:disable-next-line:no-default-export
export default extension;
