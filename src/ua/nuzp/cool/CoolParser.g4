parser grammar CoolParser;

// Використовує токени з CoolLexer
options { tokenVocab=CoolLexer; }

// ===== Програма =====
program
    : classDefine+ EOF
    ;

// ===== Клас =====
classDefine
    : CLASS type=TYPEID (INHERITS parent=TYPEID)? LBRACE (feature SEMI)* RBRACE SEMI
    ;

// ===== Члени класу =====
feature
    : method
    | attribute
    ;

// Метод
method
    : name=OBJECTID LPAREN (formal (COMMA formal)*)? RPAREN
      COLON returnType=TYPEID
      LBRACE body=expr RBRACE
    ;

// Атрибут
attribute
    : name=OBJECTID COLON type=TYPEID (ASSIGN value=expr)?
    ;

// Параметр методу
formal
    : name=OBJECTID COLON type=TYPEID
    ;

// ===== Вирази =====
expr
    // Присвоєння
    : name=OBJECTID ASSIGN value=expr                                         # Assign

    // Виклик методів
    | target=expr DOT (AT type=TYPEID)? name=OBJECTID LPAREN (expr (COMMA expr)*)? RPAREN  # Dispatch
    | name=OBJECTID LPAREN (expr (COMMA expr)*)? RPAREN                       # ImplicitDispatch

    // Керування потоком
    | IF condition=expr THEN thenBranch=expr ELSE elseBranch=expr FI          # If
    | WHILE condition=expr LOOP body=expr POOL                                # While
    | LBRACE (expr SEMI)+ RBRACE                                              # Block

    // Let / Case
    | LET letMap (COMMA letMap)* IN body=expr                                 # Let
    | CASE value=expr OF (caseBranch SEMI)+ ESAC                              # Case

    // Створення об’єкта
    | NEW type=TYPEID                                                         # New

    // Унарні
    | ISVOID e=expr                                                           # IsVoid
    | TILDE e=expr                                                            # Negation
    | NOT e=expr                                                              # Not

    // Бінарні арифметичні
    | left=expr op=(MULT | DIV) right=expr                                    # MulDiv
    | left=expr op=(PLUS | MINUS) right=expr                                  # AddSub

    // Порівняння
    | left=expr op=(LE | LT | EQUALS) right=expr                              # Compare

    // Дужки
    | LPAREN e=expr RPAREN                                                    # Paren

    // Значення
    | ID=OBJECTID                                                             # Id
    | INT_CONST                                                               # Int
    | STRING_CONST                                                            # String
    | TRUE                                                                    # True
    | FALSE                                                                   # False
    ;

// ===== Допоміжні правила =====
letMap
    : name=OBJECTID COLON type=TYPEID (ASSIGN value=expr)?
    ;

caseBranch
    : name=OBJECTID COLON type=TYPEID DARROW result=expr
    ;