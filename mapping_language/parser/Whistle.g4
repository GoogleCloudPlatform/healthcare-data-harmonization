// Copyright 2019 Google LLC
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

grammar Whistle
;

IF
    : 'if'
    | 'iff'
; // Backwards compatibility

WHERE
    : 'where'
;

ELSE
    : 'else'
;

VAR
    : 'var'
;

OBJ
    : 'obj' // Deprecated: b/148284692
    | 'out'
;

THIS
    : '$this'
    | '<this>' // Deprecated: b/148939976
;

ROOT_INPUT
    : '$root'
;

ROOT
    : 'root'
;

DEST
    : 'dest'
;

BOOL
    : 'true'
    | 'false'
;

NEWLINE
    : '\n'
    | EOF
;

ARROW
    : '=>'
;

WILDCARD
    : '[*]'
;

// Arithmetic operators
MUL
    : '*'
;
DIV
    : '/'
;
SUB
    : '-'
;
ADD
    : '+'
;

// Comparison operators
NEQ
    : '~='
;
EQ
    : '='
;
GT
    : '>'
;
GTEQ
    : '>='
;
LT
    : '<'
;
LTEQ
    : '<='
;

// Logic operators
NOT
    : '~'
;
AND
    : 'and'
;
OR
    : 'or'
;

// Data operators
NOTNIL
    : '?'
;

DEF
    : 'def'
;

INDEX
    : '[' INTEGER ']'
;

DELIM
    : '.'
;

TOKEN
    : [$A-Za-z_][$A-Za-z_0-9/-]*
;

OWMOD
    : '!'
;

ARRAYMOD
    : '[]'
;

CTX
    : '^'
;

INTEGER
    : [0-9]+
;

COMMENT
    : '//' COMMENTCHAR*
;

fragment COMMENTCHAR
    : ~[\r\n]
;

STRING
    : '"' STRINGCHAR* '"'
;

fragment STRINGCHAR
    : ~["\\]
    | '\\' ["\\]
;

WS
    : [ \r\t]+ -> channel(HIDDEN)
;
UNKNOWN
    : .
;

bioperator1
    : MUL
    | DIV
;

bioperator2
    : ADD
    | SUB
;

bioperator3
    : EQ
    | NEQ
    | GT
    | GTEQ
    | LT
    | LTEQ
;

bioperator4
    : AND
    | OR
;

postunoperator
    : NOTNIL
;

preunoperator
    : NOT
;

floatingPoint
    : SUB? INTEGER ('.' INTEGER)?
;

root
    : (mapping | comment | projectorDef | NEWLINE)* postProcess? NEWLINE* EOF
  ;

projectorDef
    : DEF TOKEN '(' ((argAlias (',' argAlias)*) | argAlias?) ')' NEWLINE? block NEWLINE?
;

argAlias
    : TOKEN
;

conditionBlock
    : ifBlock elseBlock? NEWLINE?
;

ifBlock
    : condition block
;

elseBlock
    : ELSE block
;

inlineCondition
    : '(' condition ')'
;

inlineFilter
    : '[' filter ']'
;

block
    : '{' NEWLINE? (mapping | comment | conditionBlock | NEWLINE)* '}'
;

mapping
    : target inlineCondition? ':' expression (
        ';'
        | comment
        | NEWLINE
        | EOF
    )
;

comment
  : COMMENT (EOF|NEWLINE)
  ;

condition
    : IF expression
;

filter
    : WHERE expression
;

expression
    : // Operator precedence is determined by order of alternatives.
    source                              # ExprSource
    | ARROW TOKEN                       # ExprNoArg
    | expression postunoperator         # ExprPostOp
    | preunoperator expression          # ExprPreOp
    | expression bioperator1 expression # ExprBiOp
    | expression bioperator2 expression # ExprBiOp
    | expression bioperator3 expression # ExprBiOp
    | expression bioperator4 expression # ExprBiOp
    | expression ARROW TOKEN ARRAYMOD?  # ExprProjection
    // This is a workaround for x ,y => a => b always being parsed as x, (y => a) => b, no matter
    // what order these rules are in.
    | sourceContainer (',' sourceContainer)* ARROW TOKEN ARRAYMOD? # ExprProjection
;

sourceContainer
    : source
;

source
    : floatingPoint                  # SourceConstNum
    | (VAR | DEST)? path (inlineFilter ARRAYMOD?)? # SourceInput
    | STRING                         # SourceConstStr
    | BOOL                           # SourceConstBool
    | '(' expression ')' ARRAYMOD?   # SourceProjection
;

target
    : VAR path  # TargetVar
    | ROOT path # TargetRootField
    | OBJ TOKEN # TargetObj
    | THIS      # TargetThis
    | path      # TargetField
;

path
    : pathHead pathSegment* ARRAYMOD? OWMOD?
;

pathHead
    : ROOT_INPUT
    | ROOT // Deprecated: b/148939976
    | TOKEN
    | INDEX
    | ARRAYMOD
    | WILDCARD
;

pathSegment
    : DELIM TOKEN
    | DELIM INTEGER
    | WILDCARD
    | INDEX
    | ARRAYMOD
;

postProcess
    : 'post' ARROW TOKEN  # postProcessName
    | 'post' projectorDef # postProcessInline
;
