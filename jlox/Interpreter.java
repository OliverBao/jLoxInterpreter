package jlox;

import java.util.List;

public class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
            checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        System.out.println(expr.operator);
        switch (expr.operator.type) {
            case MINUS:
                checkNumberOperands(expr.operator, left,right);
                return (double) left - (double) right;
            case SLASH:
                checkNumberOperands(expr.operator, left,right);
                return (double) left / (double) right;
            case STAR:
                checkNumberOperands(expr.operator, left,right);
                return (double) left * (double) right;
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (double) left + (double) right;
                } else if (left instanceof String && right instanceof String) {
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left,right);
                return (double) left > (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left,right);
                return (double) left < (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left,right);
                return (double) left >= (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left,right);
                return (double) left <= (double) right;
            case EQUAL_EQUAL:
                return isEqual(left,right);
            case BANG_EQUAL:
                return !isEqual(left,right);
            case AND:
                if (left instanceof Boolean && right instanceof Boolean) {
                    return ((Boolean) left && (Boolean) right);
                } else {
                    throw new RuntimeError(expr.operator, "Boolean operators require Boolean values.");
                }
            case OR:
            if (left instanceof Boolean && right instanceof Boolean) {
                return ((Boolean) left || (Boolean) right);
            } else {
                throw new RuntimeError(expr.operator, "Boolean operators require Boolean values.");
            }
            default:
                break;
        }

        return null;
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        // just run the expression evaluation!
        evaluate(stmt.expression);
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        // passes onto evaluating the expression within the print(), which is sout-ed
        Object value = evaluate(stmt.expression);
        System.out.println(stringify(value));
        return null;
    }

    // the part where the operations that define and evaluate each type of EXPRESSION is ran
    // note that this is for expressions only - statements have the EXECUTE method, which
    // might pass onto the evaluate function
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }
    
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) {
            return;
        }
        throw new RuntimeError(operator, "operand must be a number.");
    }
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }
    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a == b;
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // the method to activate the running of each statement
    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        }
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length()-2);
            }
            return text;
        }
        return object.toString();
    }


}