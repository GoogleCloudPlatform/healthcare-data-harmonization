grammar Whistle;

@lexer::members {
  // Variable to track if lexing is inside string context.
  // Necessary to distinguish STRING_FRAGMENT_INTERP_MIDDLE from structures like
  // '... } blockModifier ({ ...' or '...}; target: {...'.
  boolean inString = false;

  // Channels for whitespace and comments.
  public static final int WHITESPACE = 1;
  public static final int COMMENTS = 2;
}

// ======================== Lexer rules ========================
// reserved keywords
IF          : 'if';
THEN        : 'then';
ELSE        : 'else';
BOOL        : 'true' | 'false';
DEF         : 'def';
VAR         : 'var';

SIDE        : 'side';
// TODO(): Deprecate root keyword.
ROOT        : 'root';
GLOBAL      : 'global';
IMPORT      : 'import';
OPTION      : 'option';
NEWLINE     : '\n' | '\r\n';
AS          : 'as';
REQUIRED    : 'required';
PACKAGE     : 'package';

// Merge modes
MERGE       : 'merge';
APPEND      : 'append';
REPLACE     : 'replace';
EXTEND      : 'extend';

// Arithmetic operators
ADD         : '+';
MINUS       : '-';
MULT        : '*';
DIV         : '/';

// Comparison operators
NEQ         : '!=';
EQ          : '==';
GT          : '>';
GTEQ        : '>=';
LT          : '<';
LTEQ        : '<=';

// Logical operators

NOT         : '!';
AND         : 'and';
OR          : 'or';
NOTNIL      : '?';

// Symbols and delimitors

PATH_DELIM     : '.';
OPEN_BRACKET   : '(';
CLOSE_BRACKET  : ')';
OPEN_ARRAY     : '[';
CLOSE_ARRAY    : ']';
OPEN_BRACE     : '{';
CLOSE_BRACE    : '}';
ARG_DELIM      : ',';
PKG_REF        : '::';
TARGET_ASSIGN  : ':';

// Integers and indices
INTEGER   : [0-9]+;
WILDCARD  : OPEN_ARRAY   '*'   CLOSE_ARRAY;

// identifiers
fragment COMPLEX_IDENTIFIER_START_CHAR : [$A-Za-z_] | '\\' .;
fragment COMPLEX_IDENTIFIER_CHAR       : [$A-Za-z_0-9] | '\\' .;

SIMPLE_IDENTIFIER
    : [$a-zA-Z_][$a-zA-Z_0-9]*
;

COMPLEX_IDENTIFIER
    : COMPLEX_IDENTIFIER_START_CHAR COMPLEX_IDENTIFIER_CHAR*
    | '\'' ('\\\'' | ~['])+ '\''
;

// Strings
fragment STRINGCHAR : ~["\\{}] | '\\' ["\\{}];
TERM_STRING         : '"';
CONST_STRING        : TERM_STRING STRINGCHAR* TERM_STRING;

STRING_FRAGMENT_INTERP_START   : TERM_STRING STRINGCHAR* OPEN_BRACE {inString = true;};
STRING_FRAGMENT_INTERP_MIDDLE  : CLOSE_BRACE STRINGCHAR* OPEN_BRACE {inString}?;
STRING_FRAGMENT_INTERP_END     : CLOSE_BRACE STRINGCHAR* TERM_STRING {inString}? {inString = false;};

// Putting whitespace and comments into separate channel to allow extracting
// structured information from comments in the future (e.g. document generation)

fragment COMMENTCHAR: ~[\r\n];
COMMENT: '//' COMMENTCHAR* (NEWLINE|EOF) -> channel(2); // comments

WS: [ \r\t]+ -> channel(1);                       // whitespece

UNKNOWN: .;

// ======================== parser rules ========================

program: packageName? (optionStatement | NEWLINE)* (importStatement | NEWLINE)* (statement | NEWLINE | functionDef)* EOF;

emptySelector: OPEN_ARRAY CLOSE_ARRAY;

num: MINUS? INTEGER ('.' INTEGER)?;

string
    :  CONST_STRING                                       # ConstStr
    |  STRING_FRAGMENT_INTERP_START
           (expression STRING_FRAGMENT_INTERP_MIDDLE?)+
       STRING_FRAGMENT_INTERP_END                         # InterpStr
;

identifier: SIMPLE_IDENTIFIER | COMPLEX_IDENTIFIER
  // New keywords are valid identifiers for backwards compatibility
  | MERGE | APPEND | EXTEND | REPLACE
  ;

packageName
    : PACKAGE (identifier | CONST_STRING) NEWLINE
;

// TODO(): Uncomment it and implement importing specific functions.
// symbolsRename
//     : ((SIMPLE_IDENTIFIER (AS SIMPLE_IDENTIFIER)? ',')| NEWLINE)* (SIMPLE_IDENTIFIER (AS SIMPLE_IDENTIFIER)? NEWLINE*)
// ;

importStatement
    : IMPORT string (';' | NEWLINE)                       # ImportStr
    | IMPORT functionCall (';' | NEWLINE)                 # ImportFunctionCall
    // TODO(): Uncomment it and implement importing specific functions.
    // : IMPORT (CONST_STRING | functionCall) (AS SIMPLE_IDENTIFIER)? (';' | NEWLINE)
    // | IMPORT (CONST_STRING | functionCall) OPEN_BRACE symbolsRename CLOSE_BRACE (';' | NEWLINE)
;

optionStatement
    : OPTION CONST_STRING (';' | NEWLINE)
;

functionIdentifier
    : SIMPLE_IDENTIFIER
    | MERGE | APPEND | EXTEND | REPLACE
;

functionName
    : (identifier PKG_REF)* functionIdentifier      #FunctionNameSimple
    | MULT PKG_REF functionIdentifier               #FunctionNameWildcard
;

argMod: REQUIRED;

argAlias
    : argMod? identifier
;

functionDef
    : DEF functionIdentifier OPEN_BRACKET ( NEWLINE*
        argAlias (ARG_DELIM NEWLINE* argAlias)* NEWLINE*
    )? CLOSE_BRACKET NEWLINE* expression (EOF | NEWLINE)
;

expression
    : block                                            # ExprBlock
    | source                                           # ExprSource
    | expression postfixOperator                       # ExprPostfixOp
    | prefixOperator expression                        # ExprPrefixOp
    | expression infixOperator1 expression             # ExprInfixOp
    | expression infixOperator2 expression             # ExprInfixOp
    | expression infixOperator3 expression             # ExprInfixOp
    | expression infixOperator4 expression             # ExprInfixOpLambda
    | IF expression THEN expression NEWLINE* (ELSE expression)? # ExprCondition
;

block
    : OPEN_BRACE (statement | NEWLINE)* CLOSE_BRACE
;

functionCall
    : functionName OPEN_BRACKET
          (NEWLINE* expression (ARG_DELIM NEWLINE* expression)* NEWLINE*)?
    CLOSE_BRACKET
;

statement
    : (target TARGET_ASSIGN)? expression (';' | NEWLINE | EOF)
;

selector
    : OPEN_ARRAY functionName expression? CLOSE_ARRAY
;

prefixOperator
    : NOT
;

postfixOperator
    : NOTNIL
;

// infixOperator 1, 2, 3 are group by precedence
// infixOperator 4 need to be wrapped in lambda for short circuiting
infixOperator1
    : MULT
    | DIV
;

infixOperator2
    : ADD
    | MINUS
;

infixOperator3
    : EQ
    | NEQ
    | GT
    | GTEQ
    | LT
    | LTEQ
;

infixOperator4
    : AND
    | OR
;

source
    : num                                   # SourceConstNum
    | sourcePath                            # SourceInput
    | string                                # SourceConstStr
    | BOOL                                  # SourceConstBool
    | OPEN_BRACKET expression CLOSE_BRACKET # SourceExpression
    | OPEN_ARRAY NEWLINE* (expression (',' NEWLINE* expression)* NEWLINE*)? CLOSE_ARRAY #SourceListInit
;

sourcePath
    : sourcePathHead (sourcePathSegment)*
;

sourcePathHead
    : functionCall                          # FunctionCallSource
    | identifier                            # InputSource
;

index     : OPEN_ARRAY INTEGER CLOSE_ARRAY;

fieldName   : PATH_DELIM (INTEGER | identifier);

sourcePathSegment
    : NEWLINE* fieldName                          #PathFieldName
    | selector                                    #PathSelector
    | (index | WILDCARD | emptySelector)          #PathIndex
;

targetKeyword: VAR | ROOT | SIDE;

targetMergeMode
    : MERGE | APPEND | EXTEND | REPLACE
;

target
    : targetMergeMode? targetKeyword? targetPath           # TargetStatic
    | functionCall                        # TargetCustomSink
;

targetPath
    : targetPathHead (targetPathSegment)*
;

targetPathHead
    : identifier
    | index
    | emptySelector
;

targetPathSegment
    : NEWLINE* fieldName
    | index
    | emptySelector
;
