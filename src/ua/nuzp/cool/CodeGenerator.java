package ua.nuzp.cool;

import org.antlr.v4.runtime.tree.ParseTree;

public class CodeGenerator extends CoolParserBaseVisitor<String> {

    // ===== Поля генератора =====

    private StringBuilder output = new StringBuilder();
    private int tempCounter = 0;
    private int labelCounter = 0;

    public String getGeneratedCode() {
        return output.toString();
    }

    // ===== Допоміжні методи =====

    private String newTemp() {
        return "t" + (++tempCounter);
    }

    private String newLabel() {
        return "L" + (++labelCounter);
    }

    private void emit(String instruction) {
        output.append(instruction).append("\n");
    }

    // ===== Visitor логіка =====

    @Override
    public String visitProgram(CoolParser.ProgramContext ctx) {
        emit("; --- START OF CODE GENERATION ---");
        emit("; Target: 3-Address Code (Intermediate Representation)\n");
        visitChildren(ctx);
        emit("\n; --- END OF PROGRAM ---");
        return null;
    }

    @Override
    public String visitClassDefine(CoolParser.ClassDefineContext ctx) {
        emit("\n; CLASS: " + ctx.type.getText());
        visitChildren(ctx);
        return null;
    }

    @Override
    public String visitMethod(CoolParser.MethodContext ctx) {
        emit("\nFUNC " + ctx.name.getText() + ":");
        visit(ctx.body);
        emit("RET");
        return null;
    }

    @Override
    public String visitAssign(CoolParser.AssignContext ctx) {
        String valueReg = visit(ctx.value);
        String varName = ctx.name.getText();
        emit("MOVE " + varName + ", " + valueReg);
        return varName;
    }

    @Override
    public String visitInt(CoolParser.IntContext ctx) {
        return "#" + ctx.getText();
    }

    @Override
    public String visitId(CoolParser.IdContext ctx) {
        return ctx.OBJECTID().getText();
    }

    @Override
    public String visitString(CoolParser.StringContext ctx) {
        return ctx.getText();
    }

    // Арифметика з оптимізацією (Constant Folding)

    @Override
    public String visitAddSub(CoolParser.AddSubContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String op = ctx.op.getType() == CoolParser.PLUS ? "ADD" : "SUB";

        // Якщо обидва операнди — константи, обчислюємо одразу
        if (left.startsWith("#") && right.startsWith("#")) {
            try {
                int lVal = Integer.parseInt(left.substring(1));
                int rVal = Integer.parseInt(right.substring(1));
                int res = ctx.op.getType() == CoolParser.PLUS ? (lVal + rVal) : (lVal - rVal);

                emit("; OPTIMIZATION: Folded constant " + left + (ctx.op.getType() == CoolParser.PLUS ? "+" : "-") + right);
                return "#" + res;
            } catch (NumberFormatException e) {
            }
        }

        String temp = newTemp();
        emit(op + " " + temp + ", " + left + ", " + right);
        return temp;
    }

    @Override
    public String visitMulDiv(CoolParser.MulDivContext ctx) {
        String left = visit(ctx.left);
        String right = visit(ctx.right);
        String op = ctx.op.getType() == CoolParser.MULT ? "MUL" : "DIV";

        // Constant Folding
        if (left.startsWith("#") && right.startsWith("#")) {
            try {
                int lVal = Integer.parseInt(left.substring(1));
                int rVal = Integer.parseInt(right.substring(1));
                int res = ctx.op.getType() == CoolParser.MULT ? (lVal * rVal) : (lVal / rVal);
                emit("; OPTIMIZATION: Folded constant " + left + (ctx.op.getType() == CoolParser.MULT ? "*" : "/") + right);
                return "#" + res;
            } catch (Exception e) {}
        }

        String temp = newTemp();
        emit(op + " " + temp + ", " + left + ", " + right);
        return temp;
    }

    // ===== Керуючі конструкції =====

    @Override
    public String visitIf(CoolParser.IfContext ctx) {
        String elseLabel = newLabel();
        String endLabel = newLabel();

        String cond = visit(ctx.condition);

        emit("IF_FALSE " + cond + " GOTO " + elseLabel);

        visit(ctx.thenBranch);
        emit("GOTO " + endLabel);

        emit(elseLabel + ":");
        visit(ctx.elseBranch);

        emit(endLabel + ":");
        return null;
    }

    @Override
    public String visitWhile(CoolParser.WhileContext ctx) {
        String startLabel = newLabel();
        String endLabel = newLabel();

        emit(startLabel + ":");
        String cond = visit(ctx.condition);

        emit("IF_FALSE " + cond + " GOTO " + endLabel);

        visit(ctx.body);
        emit("GOTO " + startLabel);

        emit(endLabel + ":");
        return null;
    }

    // ===== Виклик методів =====

    @Override
    public String visitImplicitDispatch(CoolParser.ImplicitDispatchContext ctx) {
        String methodName = ctx.OBJECTID().getText();

        if (ctx.expr() != null) {
            for (CoolParser.ExprContext arg : ctx.expr()) {
                String argTemp = visit(arg);
                emit("PARAM " + argTemp);
            }
        }

        String resultTemp = newTemp();
        emit("CALL " + resultTemp + ", " + methodName);
        return resultTemp;
    }

    // ===== Блоки =====

    @Override
    public String visitBlock(CoolParser.BlockContext ctx) {
        String last = null;
        for(CoolParser.ExprContext e : ctx.expr()) {
            last = visit(e);
        }
        return last;
    }

    // ===== Інші вузли =====

    @Override public String visitCompare(CoolParser.CompareContext ctx) {
        String l = visit(ctx.left); String r = visit(ctx.right);
        String t = newTemp(); emit("CMP " + t + ", " + l + ", " + r); return t;
    }
    @Override public String visitTrue(CoolParser.TrueContext ctx) { return "#1"; }
    @Override public String visitFalse(CoolParser.FalseContext ctx) { return "#0"; }
}