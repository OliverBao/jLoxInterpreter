package jlox;

import java.util.List;

/*
 * here is where the visitor methods full get defined, as how the interpreter will
 * process each different kind of input expression
 */

public class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {

    private Environment environment = new Environment();

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
            default:
                break;
        }

        return null;
    }
    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return environment.get(expr.name) != null ? environment.get(expr.name) : new RuntimeError(expr.name, "Can't reference uninitialised variable");
    }
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        // just run the expression evaluation!
        System.out.println(stringify(evaluate(stmt.expression)));
        return null;
    }
    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        // passes onto evaluating the expression within the print(), which is sout-ed
        Object value = evaluate(stmt.expression);
        // currently a problem - variables after assignment
        // are printed as their memory locations
        if (value.getClass() == Expr.Variable.class) {
            System.out.println(stringify(environment.get(((Expr.Variable) value).name)));
        }
        System.out.println(stringify(value));
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        // whether there is an initialiser or not, there is still a value
        // if there is an initialiser, then the value is set to
        // what the initialiser expression evaluates to, otherwise
        // it stays as null
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        environment.assign(expr.name,expr.value);
        return expr.value;
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }


    // the part where the operations that define and evaluate each type of EXPRESSION is ran
    // note that this is for expressions only - statements have the EXECUTE method, which
    // might pass onto the evaluate function
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void executeBlock(List<Stmt> statements, Environment environment) {
        // saves the current environment, to
        Environment previous = this.environment;
        try {
            // replace it with an empty new one, initialised
            // with the constructor which places the current one 
            // as a field
            this.environment = environment;
    
            for (Stmt statement : statements) {
                execute(statement);
            }
        } finally {
            // reset the current environment, ie, unsave the block one
            this.environment = previous;
        }
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
    // statement analogue to evaluate() for expressions
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
