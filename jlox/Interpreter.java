package jlox;

import java.util.ArrayList;
import java.util.List;

import jlox.Lox.LoxCallable;

/*
 * here is where the visitor methods full get defined, as how the interpreter will
 * process each different kind of input expression
 */

public class Interpreter implements Expr.Visitor<Object>,Stmt.Visitor<Void> {
    
    // this environment is anchor to the outermost environment,
    // since the other original environment changes depending on
    // lexical scope - this variable is a fixed reference
    final Environment globals = new Environment();

    private Environment environment = globals;

    Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public int arity() {
                return 0;
            }
            @Override
            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double) System.currentTimeMillis()/1000.0;
            }
            @Override
            public String toString() {
                return "<native fn>";
            }
        });
    }

    /*
     * implement the visit methods, which are actually interpreting and making meaning
     * out of the parsed expressions and statements
     */
    
    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        // just return the value of the token that is carried in the expression
        return expr.value;
    }
    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        // undo the grouping syntax and return the evaluated result of the 
        // expression inside the grouping
        return evaluate(expr.expression);
    }
    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        // first evaluate the right side of unary expression
        Object right = evaluate(expr.right);

        // then based on the case of what the unary operator is, either
        switch (expr.operator.type) {
            case MINUS:
            // return the negative version of the value, only after doing a 
            // type check - since lox is dynamically typed
            checkNumberOperand(expr.operator, right);
                return -(double)right;
            case BANG:
            // otherwise just return the truthy value
                return !isTruthy(right);
            default:
                break;
        }
        return null;
    }
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        // evaluate the left and right before examining what the operator is
        // and hence what to do with the two left and right values
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch (expr.operator.type) {
            case MINUS:
            // dynamic type checking
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
                    // extra operation support for string concatenation
                    return (String) left + (String) right;
                }
                throw new RuntimeError(expr.operator, "Operands must be two numbers or two strings.");
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double) left > (double) right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double) left < (double) right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double) left >= (double) right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
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
        // give back the environment object - if the variable is undefined, then
        // error handling is passed onto the environment object, otherwise, if 
        // the variable being called is not initialised, return the error report,
        // otherwise return the variable value as per the scoping hierarchy implemented
        // by the environment
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
        System.out.println(stringify(value));
        return null;
    }
    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
    /*  this function also is the one that allows the repeated
        usage of 'var' in both assigning and declaraing variables
        or the lack of usage thereof
        the part where a variable doesn't need the "int" or "var"
        prefix for declaration is a feature of a language called
        implicit declaration.
        this might have the problem of a misspelled variable obtaining a new
        value, which might interfere with other parts of the code when 
        this misassigned variable is global
        another one is that a variable created inside a block is intended
        for use in the current environment, but in other cases
        when one wants to access the same variable in outer scopes, instead
        of creating a new one with the same value one can just want
        to be able to refer to it - ie with the python global keyword, or
        instead nonlocal

        whether there is an initialiser or not, there is still a value
        if there is an initialiser, then the value is set to
        what the initialiser expression evaluates to, otherwise
        it stays as null
        */

        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }
        environment.define(stmt.name.lexeme, value);
        return null;
    }
    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        // big mistake: instead of the below, I had originally written
        // environment.assign(expr.name,expr.value)
        // where expr.value actually has a Expr type, which
        // means that when the value is called it's only a 
        // node on the AST, instead of an actual value
        environment.assign(expr.name,evaluate(expr.value));
        return expr.value;
    }
    @Override
    public Object visitCallExpr(Expr.Call expr) {
        // evalute the callee - checks on later with !(callee instanceof LoxCallable) ->
        Object callee = evaluate(expr.callee);

        // initialise the list of arguments
        List<Object> arguments = new ArrayList<>();
        for (Expr argument : expr.arguments) {
            arguments.add(evaluate(argument));
        }

        // checks if the callee has already been declared as a function, otherwise the call cannot
        // go ahead
        if (!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }

        // converts the callee to the LoxCallable class and then runs the .call() method there
        LoxCallable function = (LoxCallable) callee;

        // not before checking arity
        if (arguments.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expeted " + function.arity() + " arguments but got " + arguments.size() + ".");
        }
        return function.call(this,arguments);
    }
    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        // use the specialised executeBlock method, which executes the 
        // list of statements in block within the new environment
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }
    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        // actually quite simple - literally implemented by how
        // java runs the condition control flow
        if (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }
    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);
        // check the left side if the right side needs to be evaluated
        if (expr.operator.type == TokenType.OR) {
            if (isTruthy(left)) return left;
        } else {
            if (!isTruthy(left)) return left;
        }
        // if needed, then give the right side
        return evaluate(expr.right);
    }
    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        // literally, uses the java while loop to run the statements within the block
        while (isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }
    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        // use a self defined object to represent the function at runtime
        // this is NOT visitCallExpr!! this is where the function declaration
        // is identified and then the function object stored within the environment

        // here is where all the information from function() in the parser is 
        // converted into a LoxFunction object, with all the callee, param and
        // body information
        LoxFunction function  = new LoxFunction(stmt);
        environment.define(stmt.name.lexeme, function);
        return null;
    }
    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = evaluate(stmt.value);
        }
        throw new Return(value);
    }
    // the part where the operations that define and evaluate each type of EXPRESSION is ran
    // note that this is for expressions only - statements have the EXECUTE method, which
    // might pass onto the evaluate function
    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
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
        if (a instanceof Double && b instanceof Double) {
            return (double) a == (double) b;
        }
        return a == b;
    }
    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // the main method! for every statement that the parser returns, execute
    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    // the method to activate the running of each statement via triggering the visit methods
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
