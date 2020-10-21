import 'jasmine';
import * as fs from 'fs';

import {CompletionItem, CompletionItemKind} from 'vscode-languageserver';
import {Position, TextDocument} from 'vscode-languageserver-types';

import * as util from '../src/util';

const suggestionList: CompletionItem[] = [
  {label: 'Div', kind: CompletionItemKind.Function},
  {label: 'Mol', kind: CompletionItemKind.Function},
  {label: 'Mul', kind: CompletionItemKind.Function}
];

describe('suggestFor: Single line document', () => {
  const testCases = [
    {
      name: 'suggest with $',
      position: Position.create(0, 1),
      expectedList: suggestionList,
      expectLog: false
    },
    {
      name: 'suggest with space',
      position: Position.create(0, 2),
      expectedList: util.NULL_LIST,
      expectLog: false
    },
    {
      name: 'suggest with letter D',
      position: Position.create(0, 3),
      expectedList: util.NULL_LIST,
      expectLog: false
    },
    {
      name: 'suggest at character position 0',
      position: Position.create(0, 0),
      expectedList: util.NULL_LIST,
      expectLog: true
    },
    {
      name: 'character position out of bound',
      position: Position.create(0, 5),
      expectedList: util.NULL_LIST,
      expectLog: true
    },
    {
      name: 'line position out of bound',
      position: Position.create(1, 1),
      expectedList: util.NULL_LIST,
      expectLog: true
    }
  ];

  const docString: TextDocument = TextDocument.create('', 'EN', 1, '$ D ');

  testCases.forEach(test => {
    it(test.name, () => {
      console.log = jasmine.createSpy('log');
      const suggested =
          util.suggestFor(docString, test.position, suggestionList);
      expect(suggested).toEqual(test.expectedList);
      if (test.expectLog) {
        expect(console.log).toHaveBeenCalled();
      } else {
        expect(console.log).not.toHaveBeenCalled();
      }
    });
  });
});

describe('suggestFor: Multi line document', () => {
  const testCases = [
    {
      name: 'suggest with $ at first line',
      position: Position.create(0, 1),
      expectedList: suggestionList,
      expectLog: false
    },
    {
      name: 'suggest with space at second line',
      position: Position.create(1, 1),
      expectedList: util.NULL_LIST,
      expectLog: false
    },
    {
      name: 'suggest with h at third line',
      position: Position.create(2, 1),
      expectedList: util.NULL_LIST,
      expectLog: false
    },
    {
      name: 'suggest with $ at third line',
      position: Position.create(2, 7),
      expectedList: suggestionList,
      expectLog: false
    },
    {
      name: 'line number out of bound',
      position: Position.create(3, 1),
      expectedList: util.NULL_LIST,
      expectLog: true
    },
    {
      name: 'character number out of bound',
      position: Position.create(0, 5),
      expectedList: util.NULL_LIST,
      expectLog: true
    }
  ];

  const docString: TextDocument =
      TextDocument.create('', 'EN', 1, '$ D \n alpha \nhello $ ');

  testCases.forEach(test => {
    it(test.name, () => {
      console.log = jasmine.createSpy('log');
      const suggested =
          util.suggestFor(docString, test.position, suggestionList);

      expect(suggested.length).toBe(test.expectedList.length);
      expect(suggested).toEqual(test.expectedList);
      if (test.expectLog) {
        expect(console.log).toHaveBeenCalled();
      } else {
        expect(console.log).not.toHaveBeenCalled();
      }
    });
  });
});

describe('suggestFor: Empty document', () => {
  const testCases = [{
    name: 'suggest with position (0, 1)',
    position: Position.create(0, 1),
    expectedList: util.NULL_LIST
  }];

  const docString: TextDocument = TextDocument.create('', 'EN', 1, '');

  testCases.forEach(test => {
    it(test.name, () => {
      console.log = jasmine.createSpy('log');
      const suggested =
          util.suggestFor(docString, test.position, suggestionList);
      expect(suggested.length).toBe(test.expectedList.length);
      expect(suggested).toEqual(test.expectedList);
      expect(console.log).toHaveBeenCalled();
    });
  });
});

describe('getFunctions: File from correct address', () => {
  const testCases = [
    {
      name: 'File with 3 functions',
      fileContent: 'Div,Mol,Mul',
      expectedWords: suggestionList
    },
    {
      name: 'File with 3 functions, one white spaces after comma',
      fileContent: 'Div, Mol, Mul',
      expectedWords: suggestionList
    },
    {
      name: 'File with 3 functions, multiple white spaces in between',
      fileContent: '   Div  ,  \n Mol,Mul    ',
      expectedWords: suggestionList
    },
    {
      name: 'File with 1 function, multiple white spaces and comma',
      fileContent: ', Mol, ,',
      expectedWords: [{label: 'Mol', kind: CompletionItemKind.Function}]
    },
    {name: 'Empty file', fileContent: '', expectedWords: util.NULL_LIST}, {
      name: 'File with only comma and spaces',
      fileContent: '  , \n,',
      expectedWords: util.NULL_LIST
    },
    {
      name: 'File with 1 function, spaces and 2 duplication',
      fileContent: 'Div, , Div,  Div ,',
      expectedWords: [{label: 'Div', kind: CompletionItemKind.Function}]
    },
    {
      name: 'File with 2 functions and their duplication',
      fileContent: 'Div,Mol,Div,Mol,Mol',
      expectedWords: [
        {label: 'Div', kind: CompletionItemKind.Function},
        {label: 'Mol', kind: CompletionItemKind.Function}
      ]
    }
  ];
  testCases.forEach(test => {
    it(test.name, () => {
      spyOn(fs, 'readFileSync').and.returnValue(test.fileContent);
      const functions = util.getFunctions('function.txt', 'mock/valid/path');
      expect(functions).toEqual(test.expectedWords);
      expect(fs.readFileSync).toHaveBeenCalled();
    });
  });
});

describe('getFunctions: Invalid file path', () => {
  const testCases = [
    {name: 'File not found', errorMessage: 'ENOENT, no such file or directory'},
    {name: 'Other error thrown', errorMessage: 'random error'}
  ];
  testCases.forEach(test => {
    it(test.name, () => {
      spyOn(fs, 'readFileSync').and.throwError(test.errorMessage);
      console.log = jasmine.createSpy('log');
      const functions =
          util.getFunctions('function.txt', 'path/to/function/dir');
      expect(functions).toEqual(util.NULL_LIST);
      expect(fs.readFileSync).toHaveBeenCalled();
      expect(console.log).toHaveBeenCalled();
    });
  });
});
