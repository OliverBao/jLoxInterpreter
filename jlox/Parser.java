package jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/*
 * here the parser handles syntax - putting things together according
 * to the CFGs defined, and pushes to the interpreter the 
 * list of statements/expressions/declarations to be handled
 * or evaluated the interpreter handles the semantics
 * 
 * whenever there's a new type of syntax or language feature to be added, 
 * it needs to be inserted at the right level of recursive descent, for example
 * function calls are immediate when the postfix token '(' is identified, and that
 * binds even more strongly than unary operators like ! or - (since we can imagined
 * -fib(n) which returns the fib number and then negates it) so we must have 
 * call() being even lower down the recursive descent than unary
 * 
 * every time some new language feature is added, we need to edit and run the
 * GenerateAst.java file, which automatically writes in the visitor pattern along
 * with all the fields and the constructor for new language features.
 * 
 */

import static jlox.TokenType.*;

public class Parser {

    private static class ParseError extends RuntimeException {};

    // the tokens passed on from the scanner
    private final List<Token> tokens;

    // pointer to the current token being processed
    private int current = 0;

    // the parser only needs one field, which is the list of tokens that 
    // it is going to read through
    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    /*
     * here the recursive descent fleshes out the productions - previously,
     * in Expr.java it was all about establishing the rules about how the 
     * productions can be written (Binary, Unary, Grouping or Literal)
     */

    // implementing the recursive descent, starting from expression
    // expresion -> equality()
    // equality  -> comparison() ( (== | !=) comparison )*
    // comparison -> term() ( (<= | >= | < | >) term())*
    // and so on

    private Expr expression() {
        // the very top of expression parsing, more for theoretical alignment than
        // for practical utility. fires the expression parsing process
        return assignment();
    }
    private Expr assignment() {
        // the tricky part for this, is that we can't treat the expression for 
        // assignment the same as a part of the previous expressions, because
        // we can't "evaluate" the LHS, the variable, since it isn't a real
        // expression but a reference that we can use to get a handle on a spot in memory 

        // hence, this looks ahead (using rescursive descent) to see if the LHS
        // is a variable, which is then checked on line 75
        Expr expr = or();
        // unless it is a variable, as checked below, do not 
        // return the assign expression, so a + b = c is not allowed

        if (match(EQUAL)) { // checks if we are to start the assignment process
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) { // would be true if it recursively descends to PRIMARY and finds an identifier
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals,"Invalid assignment target.");
        }
        // parses the assignment Syntax Tree node to the interpreter
        return expr;
    }
    private Expr or() {
        Expr expr = and();
        while (match(OR)) { // checks ahead to see if it is the OR operator, 
        // since that is how the parser knows that this isn't just a binary expression
            Token operator = previous();
            // if passed, then operator is OR
            Expr right = and();
            // continue parsing to the right here the right can just be expression,
            // since the idea of recursive descent is that once we are evaluating an 
            // expression an unparsed expression can be any node in the AST.
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and() {
        // exactly the same as how OR was implemented - check for AND, then move on
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL,EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
            // again, the parser doesn't do anything with the units of expressions that
            // it recognises - it just puts it together as a node on the AST and passes
            // it onto the interpreter which actually does the evaluating
        }
        return expr;
    }
    private Expr comparison() {
        // exactly the same as equality(), but for comparing values
        Expr expr = term();
        while (match(LESS,LESS_EQUAL,GREATER,GREATER_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr,operator,right);
        }
        return expr;
    }
    private Expr term() {
        Expr expr = factor();
        while (match(MINUS,PLUS)) {
            Token operator = previous();
            Expr right = factor();
            // recursively construct new binary expression after the recursive
            // descent evaluates the left and right sides
            expr = new Expr.Binary(expr,operator,right);
        }
        return expr;
    }
    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH,STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr,operator,right);
        }
        return expr;
    }
    private Expr unary() {
        if (match(BANG,MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return call();
    }
    private Expr call() {
        // call has even high precedence than unary, since -fib(n) returns -1
        // moreover, the subtle part is that call is a part of an expression - 
        // it's a trigger, not a statement in the sense that it produces a directly
        // visible effect or control flow
        Expr expr = primary();
        while (true) {
            // the loop keeps going because of possible expressions such as expr()()() when methods return methods
            if (match(LEFT_PAREN)) {
                // the finishCall actually runs the function implementation
                expr = finishCall(expr);
            } else {
                break;
            }
        }
        return expr;
    }
    private Expr primary() {

        // I had an error here where I previously used the TokenType FALSE 
        // instead of the java backend boolean false, which didn't raise an 
        // error, but led to a bug in the interpreter where it would do 
        // boolean comparisons using FALSE, and hence always returned false in the 
        // interpeter, which catered only to semantics, so when the syntax was
        // unchecked, particularly when the error is a insidious one where no
        // runtime exceptions are raised, it becomes a huge problem.
                
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);

        if (match(NUMBER,STRING)) {
            return new Expr.Literal(previous().literal);
        }

        if (match(IDENTIFIER)) {
            // here we use previous because when match() returns true,
            // it advances the current pointer forwards
            return new Expr.Variable(previous());
            // and a variable is also a form of a statement
            // and remember, the parser is NOT where we find values of stuff and 
            // store them - this just passes to the interpreter the variable node
            // as a part of the AST.
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression."); // currently any errors will break down here
    }

    private boolean match(TokenType...types) {
        // if keep consuming tokens until a respective match is no longer found
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private boolean check(TokenType type) {
        // returns whether the next token is as expected
        if (isAtEnd()) return false;
        return peek().type == type;
    }
    private Token advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }
    private boolean isAtEnd() {
        return peek().type == EOF;
    }
    private Token peek() {
        return tokens.get(current);
    }
    private Token previous() {
        return tokens.get(current-1);
    }
    // consumes an expected token - otherwise returns the error message
    // in other words, the same as match except it only consumes one token,
    // and returns an error message instead of a boolean value, and hence is used
    // when 
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        // throw error(peek(),message);

        // not too sure what happens here: the throw statement is supposed to be used
        // to exit the entire session, whereas the try and catch statement actually
        // keeps the REPL session going
        try {
            error(peek(), message);
            return peek();
        } catch (ParseError e) {
            return peek();
        }
    }

    private Expr finishCall(Expr callee) {
        // here the callee is the previous expression which did the calling
        // much in the sense that calling is a binary operation, with () and with
        // the name of the function being called

        List<Expr> arguments = new ArrayList<>();

        // checks if the next token is a ), if so, then 
        // we are in the zero arguments case
        if (!check(RIGHT_PAREN)) {
            do { // otherwise, keep adding the arguments, while commas are matched
                 // or a error is thrown  
                if (arguments.size() > 255) {
                    error(peek(),"Can't have more than 255 arguments.");
                }
                arguments.add(expression());
                
            } while (match(COMMA));
        }
        // of course, expect the right parentheses to close off the argument list
        Token paren = consume(RIGHT_PAREN,"Expect ')' after arguments");
        // add the new call object to the AST
        return new Expr.Call(callee, paren, arguments);
    }   

    // parse triggers at the top of the recursive descent
    // hierarchy, which fires everything else - this order makes
    // sense since every expression is some type of statement, and 
    // every statement is some kind of declaration.
    private Stmt declaration() {
        try {
            // trying to redirect to the correct branches of the AST
            if (match(VAR)) {
                return varDeclaration(); // both these are special, since they might require extra parsing
            }
            if (match(FUN)) {
                // this is a function declaration, in contrast to a call
                return function("function"); // both these are special, since they might require extra parsing
            }
            // otherwise, default to the next level
            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }
    private Stmt.Function function(String kind) {
        // in contrast to call, this is is for the node of function declaration

        // begins with the required function name
        Token name = consume(IDENTIFIER,"Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();

        // won't let you defined a function which has more than 255 parameters
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() > 255) {
                    error(peek(), "Can't defined functions with more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect paramter name."));
            } while (match(COMMA)); // keep adding parameters until no more comma
        }
        // match the function definition syntax
        consume(RIGHT_PAREN, "Expect ')' after parameters");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");

        // begin the block statement, and then store everything so far: name, pararms
        // and the body of statements that the function actually rune - as a function object
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    private Stmt statement() {
        // all sorts of different statements, which are mainly for different kinds
        // of control flow - another reason why Expression and Stmt are separate
        // because expressions are value based whereas statements are control flow/
        // side-effect based.
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(RETURN)) return returnStmt();
        // if there is not any control flow keywords detected, then it must be an expression
        return expressionStatement();
    }
    private Stmt returnStmt() {
        Token keyword = previous();
        Expr value = null;
        if (!check(SEMICOLON)) {
            value = expression();
        }
        consume(SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword,value);
    }
    private Stmt varDeclaration() {
        Token name = consume(IDENTIFIER, "Expect variable name.");
        // default, without an initialiser, the variable takes the value of nil
        Expr initialiser = null;
        if (match(EQUAL)) {
            // otherwise, consume an expression
            initialiser = expression();
        }
        // require a semicolon to finish a statement
        consume(SEMICOLON, "Expect ';' after variable declaration.");
        return new Stmt.Var(name,initialiser);
    }
    private Stmt printStatement() {
        //collapse to the expression productions
        Expr value = expression();
        consume(SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }
    private Stmt expressionStatement() {
        // pass onto the expression parse tree
        Expr expr = expression();
        consume(SEMICOLON, "expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    private List<Stmt> block() {
        // the sequence of statements within the braces
        List<Stmt> statements = new ArrayList<>();

        // while a right brace is not detected, keep adding new statements
        // to the list of statements, by triggering the declaration, top
        // level recursive descent
        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    private Stmt ifStatement() {
        // required syntax
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        // the "condition" part of the if (condition)
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        // parse the entire following statement - if it is a block then that 
        // is also parsed
        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        
        // if there is a else token, then go ahead and parse it
        if (match(ELSE)) {
            elseBranch = statement();
        }

        // give the statement IF type response
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt whileStatement() {
        // begins the required syntax
        consume(LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = statement(); // this, along with the of the ifStatement, seems to only
        // allow for a single statement to be executed in the body of the while loop
        // however, this is not the case, since at the bottom of the recursive descent,
        // block() function is called, which is the beginning of a new recursive descent
        // on a new statement
        return new Stmt.While(condition, body);
    }
    private Stmt forStatement() {
        // this is a whole de-sugaring statement, since it defaults to constructing
        // an whileStatement by deconstructing the for syntax
        consume(LEFT_PAREN, "Expect '(' after 'for'");
        Stmt initialiser;
        if (match(SEMICOLON)) {
            initialiser = null;
        } else if (match(VAR)) {
            initialiser = varDeclaration();
        } else {
            initialiser = expressionStatement();
        }
        Expr condition = null;
        if (!check(SEMICOLON)) {
            condition = expression();
        }
        consume(SEMICOLON,"Expect ';' after loop condition");
        Expr increment = null;
        if (!check(RIGHT_PAREN)) {
            increment = expression();
        }
        consume (RIGHT_PAREN,"Expect ')' after for clauses.");
        Stmt body = statement();

        if (increment != null) {
            body = new Stmt.Block(Arrays.asList(body,new Stmt.Expression(increment)));
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        body = new Stmt.While(condition, body);

        if (initialiser != null) {
            body = new Stmt.Block(Arrays.asList(initialiser,body));
        }
        
        return body;

    }
    
    // finishes parsing the single line until it meets with a semicolon
    private void synchronise() {
        advance();
        while (!isAtEnd()) {
            if (previous().type == SEMICOLON) {
                return;
            }
            switch (peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                default:
                    advance();
                    return;
            }
        }
    }
    
    // runs the Parser error which also prints the problematic token
    private ParseError error(Token token, String message) {
        Lox.error(token, message);
        return new ParseError();
    }

    // the parsing function, which initiates the statement building process
    // each iteration of the while loop adds a full statement that the CFG
    // recognises to the statements passed onto the interpreter
    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }
}

