package ua.nuzp.cool;

import org.antlr.v4.runtime.tree.ParseTree;
import java.util.*;

public class SemanticAnalyzer extends CoolParserBaseVisitor<String> {

    // ===== Таблиця символів =====

    enum SymbolType { VARIABLE, METHOD, CLASS }

    static class Symbol {
        String name;
        String type;
        SymbolType kind;
        boolean isUsed = false;

        public Symbol(String name, String type, SymbolType kind) {
            this.name = name;
            this.type = type;
            this.kind = kind;
        }
    }

    static class Scope {
        Scope parent;
        Map<String, Symbol> symbols = new HashMap<>();

        public Scope(Scope parent) { this.parent = parent; }

        public void define(Symbol sym) { symbols.put(sym.name, sym); }

        public Symbol resolve(String name) {
            if (symbols.containsKey(name)) return symbols.get(name);
            if (parent != null) return parent.resolve(name);
            return null;
        }

        public Collection<Symbol> getLocalSymbols() {
            return symbols.values();
        }
    }

    // ===== Поля аналізатора =====

    private Scope currentScope;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final Map<String, Symbol> globalClasses = new HashMap<>();

    public SemanticAnalyzer() {
        currentScope = new Scope(null);
        defineBaseClasses();
    }

    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }

    // ===== Базові класи =====

    private void defineBaseClasses() {
        defineGlobalClass("Object");
        defineGlobalClass("IO");
        defineGlobalClass("Int");
        defineGlobalClass("String");
        defineGlobalClass("Bool");
    }

    private void defineGlobalClass(String name) {
        Symbol s = new Symbol(name, name, SymbolType.CLASS);
        s.isUsed = true;
        globalClasses.put(name, s);
        currentScope.define(s);
    }

    private void error(int line, String msg) {
        errors.add("Error (Line " + line + "): " + msg);
    }

    private void warn(int line, String msg) {
        warnings.add("Warning (Line " + line + "): " + msg);
    }

    // ===== Visitor логіка =====

    @Override
    public String visitProgram(CoolParser.ProgramContext ctx) {
        // Реєстрація класів
        for (CoolParser.ClassDefineContext c : ctx.classDefine()) {
            String name = c.type.getText();
            if (globalClasses.containsKey(name)) {
                error(c.start.getLine(), "Class " + name + " is already defined.");
            } else {
                Symbol s = new Symbol(name, name, SymbolType.CLASS);
                globalClasses.put(name, s);
                currentScope.define(s);
            }
        }
        return super.visitProgram(ctx);
    }

    @Override
    public String visitClassDefine(CoolParser.ClassDefineContext ctx) {
        currentScope = new Scope(currentScope);
        currentScope.define(new Symbol("self", ctx.type.getText(), SymbolType.VARIABLE));

        super.visitClassDefine(ctx);

        currentScope = currentScope.parent;
        return ctx.type.getText();
    }

    @Override
    public String visitMethod(CoolParser.MethodContext ctx) {
        String mName = ctx.name.getText();
        currentScope.define(new Symbol(mName, ctx.returnType.getText(), SymbolType.METHOD));

        currentScope = new Scope(currentScope);

        if (ctx.formal() != null) {
            for (CoolParser.FormalContext f : ctx.formal()) {
                currentScope.define(new Symbol(f.name.getText(), f.type.getText(), SymbolType.VARIABLE));
            }
        }

        visit(ctx.body);

        // Перевірка невикористаних параметрів
        for (Symbol s : currentScope.getLocalSymbols()) {
            if (s.kind == SymbolType.VARIABLE && !s.name.equals("self") && !s.isUsed) {
                warn(ctx.start.getLine(), "Unused parameter: '" + s.name + "'");
            }
        }

        currentScope = currentScope.parent;
        return ctx.returnType.getText();
    }

    @Override
    public String visitBlock(CoolParser.BlockContext ctx) {
        String lastType = "Object";
        for (CoolParser.ExprContext expr : ctx.expr()) {
            lastType = visit(expr);
        }
        return lastType;
    }

    @Override
    public String visitLet(CoolParser.LetContext ctx) {
        currentScope = new Scope(currentScope);

        for (CoolParser.LetMapContext map : ctx.letMap()) {
            currentScope.define(new Symbol(map.name.getText(), map.type.getText(), SymbolType.VARIABLE));
            if (map.value != null) visit(map.value);
        }

        String type = visit(ctx.body);

        // Перевірка невикористаних локальних змінних
        for (Symbol s : currentScope.getLocalSymbols()) {
            if (s.kind == SymbolType.VARIABLE && !s.isUsed) {
                warn(ctx.start.getLine(), "Unused local variable: '" + s.name + "'");
            }
        }

        currentScope = currentScope.parent;
        return type;
    }

    @Override
    public String visitIf(CoolParser.IfContext ctx) {
        String condText = ctx.condition.getText();

        if (condText.equals("true")) {
            warn(ctx.start.getLine(), "'if true': Dead else branch.");
        } else if (condText.equals("false")) {
            warn(ctx.start.getLine(), "'if false': Dead then branch.");
        }

        String condType = visit(ctx.condition);
        if (!"Bool".equals(condType))
            error(ctx.start.getLine(), "If condition must be Bool");

        String t = visit(ctx.thenBranch);
        String e = visit(ctx.elseBranch);

        return t.equals(e) ? t : "Object";
    }

    @Override
    public String visitWhile(CoolParser.WhileContext ctx) {
        if (ctx.condition.getText().equals("true")) {
            warn(ctx.start.getLine(), "Infinite loop detected.");
        }

        String condType = visit(ctx.condition);
        if (!"Bool".equals(condType))
            error(ctx.start.getLine(), "Loop condition must be Bool");

        visit(ctx.body);
        return "Object";
    }

    @Override
    public String visitAssign(CoolParser.AssignContext ctx) {
        String varName = ctx.name.getText();
        Symbol sym = currentScope.resolve(varName);
        String valType = visit(ctx.value);

        if (sym == null) {
            error(ctx.start.getLine(), "Assignment to undeclared variable: " + varName);
        } else if (!isCompatible(valType, sym.type)) {
            error(ctx.start.getLine(), "Type mismatch. Cannot assign " + valType + " to " + sym.type);
        }

        return valType;
    }

    @Override
    public String visitId(CoolParser.IdContext ctx) {
        String name = ctx.OBJECTID().getText();
        Symbol sym = currentScope.resolve(name);

        if (sym == null) {
            error(ctx.start.getLine(), "Undeclared identifier: " + name);
            return "Object";
        }

        sym.isUsed = true;
        return sym.type;
    }

    @Override
    public String visitImplicitDispatch(CoolParser.ImplicitDispatchContext ctx) {
        String mName = ctx.OBJECTID().getText();

        if (ctx.expr() != null) {
            for (CoolParser.ExprContext arg : ctx.expr()) visit(arg);
        }

        Symbol sym = currentScope.resolve(mName);
        if (sym == null && !isStandardIOMethod(mName)) {
            error(ctx.start.getLine(), "Undefined method: " + mName);
            return "Object";
        }

        return sym != null ? sym.type : "Object";
    }

    @Override
    public String visitDispatch(CoolParser.DispatchContext ctx) {
        visit(ctx.target);
        if (ctx.expr() != null) {
            for (CoolParser.ExprContext arg : ctx.expr()) visit(arg);
        }
        return "Object";
    }

    @Override
    public String visitAddSub(CoolParser.AddSubContext ctx) {
        String l = visit(ctx.left);
        String r = visit(ctx.right);
        if (!"Int".equals(l) || !"Int".equals(r))
            error(ctx.start.getLine(), "Arithmetic requires Int.");
        return "Int";
    }

    @Override
    public String visitMulDiv(CoolParser.MulDivContext ctx) {
        String l = visit(ctx.left);
        String r = visit(ctx.right);
        if (!"Int".equals(l) || !"Int".equals(r))
            error(ctx.start.getLine(), "Arithmetic requires Int.");
        return "Int";
    }

    @Override
    public String visitCompare(CoolParser.CompareContext ctx) {
        visit(ctx.left);
        visit(ctx.right);
        return "Bool";
    }

    // Літерали
    @Override public String visitInt(CoolParser.IntContext ctx) { return "Int"; }
    @Override public String visitString(CoolParser.StringContext ctx) { return "String"; }
    @Override public String visitTrue(CoolParser.TrueContext ctx) { return "Bool"; }
    @Override public String visitFalse(CoolParser.FalseContext ctx) { return "Bool"; }
    @Override public String visitNew(CoolParser.NewContext ctx) { return ctx.type.getText(); }

    // ===== Допоміжні =====

    private boolean isCompatible(String actual, String expected) {
        if (expected == null || actual == null) return true;
        if (expected.equals("Object")) return true;
        return expected.equals(actual);
    }

    private boolean isStandardIOMethod(String name) {
        return name.startsWith("out_") || name.startsWith("in_");
    }
}