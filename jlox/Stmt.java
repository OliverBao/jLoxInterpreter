package jlox;

/*
 * for now the statement class is really just an extension of Expr.java,
 * since the two kinds of statements defined, expression statements (>>> 3+4;)
 * and print statements (print(3+4);) are objects which just hold the expression
 * object, which means that 
 */

abstract class Stmt {

    interface Visitor<R> {
        R visitExpressionStmt(Expression stmt);
        R visitPrintStmt(Print stmt);
    }

    abstract <R> R accept(Visitor<R> visitor);

    // the kind of statement that's really an expression
    static class Expression extends Stmt {
        final Expr expression;
        Expression(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitExpressionStmt(this);
        }
    }

    // the print statement
    static class Print extends Stmt {
        final Expr expression;
        Print(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitPrintStmt(this);
        }
    }

}
