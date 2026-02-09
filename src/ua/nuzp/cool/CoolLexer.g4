lexer grammar CoolLexer;

@header {
package ua.lpnu.cool;
}

// Ключові слова (нечутливі до регістру, окрім true/false)
CLASS       : [cC][lL][aA][sS][sS] ;
ELSE        : [eE][lL][sS][eE] ;
FI          : [fF][iI] ;
IF          : [iI][fF] ;
IN          : [iI][nN] ;
INHERITS    : [iI][nN][hH][eE][rR][iI][tT][sS] ;
ISVOID      : [iI][sS][vV][oO][iI][dD] ;
LET         : [lL][eE][tT] ;
LOOP        : [lL][oO][oO][pP] ;
POOL        : [pP][oO][oO][lL] ;
THEN        : [tT][hH][eE][nN] ;
WHILE       : [wW][hH][iI][lL][eE] ;
CASE        : [cC][aA][sS][eE] ;
ESAC        : [eE][sS][aA][cC] ;
NEW         : [nN][eE][wW] ;
OF          : [oO][fF] ;
NOT         : [nN][oO][tT] ;

TRUE        : 't'[rR][uU][eE] ;
FALSE       : 'f'[aA][lL][sS][eE] ;

// Оператори та роздільники
DARROW      : '=>' ;
ASSIGN      : '<-' ;
LE          : '<=' ;
PLUS        : '+' ;
MINUS       : '-' ;
MULT        : '*' ;
DIV         : '/' ;
TILDE       : '~' ;
LT          : '<' ;
EQUALS      : '=' ;
LPAREN      : '(' ;
RPAREN      : ')' ;
LBRACE      : '{' ;
RBRACE      : '}' ;
DOT         : '.' ;
COMMA       : ',' ;
SEMI        : ';' ;
COLON       : ':' ;
AT          : '@' ;

// Ідентифікатори (типи з великої, об’єкти з малої)
TYPEID      : [A-Z][a-zA-Z0-9_]* ;
OBJECTID    : [a-z][a-zA-Z0-9_]* ;

// Константи
INT_CONST    : [0-9]+ ;
STRING_CONST : '"' ( '\\' . | ~('\\'|'"') )* '"' ;

// Коментарі
SINGLE_COMMENT : '--' ~[\r\n]* -> skip ;
BLOCK_COMMENT_START : '(*' -> pushMode(COMMENT_MODE) ;

// Режим вкладених block-коментарів
mode COMMENT_MODE;
    BLOCK_COMMENT_NEST : '(*' -> pushMode(COMMENT_MODE) ;
    BLOCK_COMMENT_END  : '*)' -> popMode ;
    COMMENT_CONTENT    : . -> skip ;

// Пробіли та помилки
mode DEFAULT_MODE;
WS    : [ \t\r\n\f]+ -> skip ;
ERROR : . ;