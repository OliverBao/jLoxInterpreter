package jlox;

// implements the context free grammar model of Lox,
// using an abstract class which different types of expressions,
// specifically Literals, Unary, Binary and Grouping, implement
// equivalent to being in the same CFG but being different productions.

abstract class Expr {

    /*
     * a generic interface which is used to implement the visitor design pattern
     * it is needed since when we parse each token, we need to know which specific
     * expression it is in: Unary, Binary, Grouping or Literal, since they run
     * different kinds of code
     * 
     * one idea uses the fact that Expr is abstract: in each subclass, implement
     * an "interpret()" abstract method specific to each type of Expr. But this scales
     * poorly as both the parser and interpreter uses the definition of the expressions 
     * here as the CFG productions, which means that code can get really messy and 
     * the interpreter and parser files might intermingle in a complicated way.
     * 
     * actually, this is a part of the expression problem: object oriented design
     * vs operation oriented design (functional). In ObOP, it is easy to create
     * new objects and write methods for them, but when a new operation is
     * introduced, it needs to be rewritten in every single class. similarly, 
     * OpOP is well suited to defining new operations, but creating a new class
     * of objects require adding pattern matching to every operation.
     * 
     * we have this problem here, since we can create the subclasses of Expr easily
     * here, but then when we need to add functionality to suit them to the interpreter,
     * that would require editing every single class to add methods.
     * 
     * one focuses on types, the other on operations
     * 
     * the visitor design pattern introduces the Visitor interface, where the new methods
     * or operations are introduced. then, an "accept()" abstract method is added to 
     * the abstract class, and then each subclass implements it by calling their respective
     * operations in the visitor class, in doing so passing "this", the item itself,
     * for the interface method to operate on.
     * 
     * 1. add abstract method accept() to the abstract class
     * 2. add an interface which implements operations for the specific
     *    subclasses of the abstract class
     * 3. each subclass implements the accept() method, by calling their 
     *    specific operations in the interface, passing !!themselves!! as arguments
     * 4. then the interface operation runs, with the specific object, and 
     *    does the desired thing
     * 
     * in this way, the problem is solved: all the operation needs is the current state
     * of the object that it is going to be called on - and by passing itself, 
     * the subclass gives that required information
     * 
     */
    
    // the abstract method which every subclass will implemen to call the 
    // interface methods below (which implement the desired methods)
    abstract <R> R accept(Visitor<R> visitor);

    // the interface which has the specific operations for each different subclass
    // of Expr, returning a generic value
    interface Visitor<R> {
        R visitLiteralExpr(Literal expr);
        R visitUnaryExpr(Unary expr);
        R visitBinaryExpr(Binary expr);
        R visitGroupingExpr(Grouping expr);
    }

    // the Expr subclass Literal, which has only one field, its value, 
    // parameterised with Object, since it can be String, Integer, etc.
    static class Literal extends Expr {
        final Object value;
        Literal(Object value) {
            this.value = value;
        }
        // implementing the abstract method and passing itself as argument
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    // here the Unary expression just has one token before, and another expr
    static class Unary extends Expr {
        final Token operator;
        final Expr right;
        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    // the binary class, with left, expr, right
    static class Binary extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;
        Binary(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitBinaryExpr(this);
        }
    }

    // the grouping class, with expr
    static class Grouping extends Expr {
        final Expr expression;
        Grouping(Expr expression) {
            this.expression = expression;
        }
        @Override
        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }

}
