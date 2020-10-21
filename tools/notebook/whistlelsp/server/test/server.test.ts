import 'jasmine';

import {CompletionItem, CompletionItemKind, TextDocumentIdentifier, TextDocumentPositionParams, TextDocuments, TextDocumentSyncKind} from 'vscode-languageserver';
import {TextDocument} from 'vscode-languageserver-textdocument';
import {Position} from 'vscode-languageserver-types';

import {Server} from '../src/server';
import * as util from '../src/util';


const suggestionList: CompletionItem[] = [
  {label: 'Div', kind: CompletionItemKind.Function},
  {label: 'Mol', kind: CompletionItemKind.Function},
  {label: 'Mul', kind: CompletionItemKind.Function}
];

const emptyTextDocuments = new TextDocuments(TextDocument);

describe('Server: constructor', () => {
  const testCases = [
    {
      name: 'use default functions file',
      fileParams: [],
      expectedFileParams: ['function.txt', '../../../server'],
      functionsMock: util.NULL_LIST
    },
    {
      name: 'specify the file name only',
      fileParams: ['function2.txt'],
      expectedFileParams: ['function2.txt', '../../../server'],
      functionsMock: suggestionList
    },
    {
      name: 'both file name and path',
      fileParams: ['function3.txt', 'mock/directory'],
      expectedFileParams: ['function3.txt', 'mock/directory'],
      functionsMock: suggestionList
    }
  ];
  testCases.forEach(test => {
    it(test.name, () => {
      spyOn(util, 'getFunctions').and.returnValue(test.functionsMock);
      let server = new Server();
      if (test.fileParams.length === 1) {
        server = new Server(test.fileParams[0]);
      } else if (test.fileParams.length === 2) {
        server = new Server(test.fileParams[0], test.fileParams[1]);
      }
      expect(server.suggestionList).toEqual(test.functionsMock);
      expect(server.documents).toEqual(emptyTextDocuments);
      expect(util.getFunctions)
          .toHaveBeenCalledWith(
              test.expectedFileParams[0], test.expectedFileParams[1]);
    });
  });
});

describe('Server: onInitialize', () => {
  it('Simple server', () => {
    const server = new Server();
    const initialized = server.onInitialize();
    expect(initialized).toEqual({
      capabilities: {
        textDocumentSync: TextDocumentSyncKind.Incremental,
        completionProvider:
            {triggerCharacters: [util.TRIGGER_CHAR], resolveProvider: true}
      }
    });
  });
});

describe('Server: onCompletion', () => {
  const textDocument: TextDocument = TextDocument.create('', 'EN', 1, '$ D ');
  const testCases = [
    {
      name: 'suggest completion list',
      position: Position.create(0, 1),
      document: textDocument,
      suggestedMock: suggestionList,
      docUndefined: false
    },
    {
      name: 'suggest null list',
      position: Position.create(1, 1),
      document: textDocument,
      suggestedMock: util.NULL_LIST,
      docUndefined: false
    },
    {
      name: 'position out of bound, suggest null list',
      position: Position.create(2, 1),
      document: textDocument,
      suggestedMock: util.NULL_LIST,
      docUndefined: false
    },
    {
      name: 'doc undefined',
      position: Position.create(3, 1),
      document: undefined,
      suggestedMock: suggestionList,
      docUndefined: true
    }
  ];

  testCases.forEach(test => {
    it(test.name, () => {
      console.log = jasmine.createSpy('log');
      spyOn(util, 'suggestFor').and.returnValue(test.suggestedMock);
      const documentParam: TextDocumentPositionParams = {
        textDocument: TextDocumentIdentifier.create('mock/uri/file.wstl'),
        position: test.position
      };
      const server = new Server();
      spyOn(server.documents, 'get').and.returnValue(test.document);
      const completion = server.onCompletion(documentParam);
      if (test.docUndefined) {
        expect(completion).toEqual(util.NULL_LIST);
        expect(util.suggestFor).not.toHaveBeenCalled();
      } else {
        expect(completion).toEqual(test.suggestedMock);
        expect(util.suggestFor).toHaveBeenCalled();
      }
    });
  });
});
