package jlox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// here the parser handles syntax - putting things together according
// to the CFGs defined, and pushes to the interpreter the 
// list of statements/expressions/declarations to be handled
// or evaluated the interpreter handles the semantics

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
        return assignment();
    }
    private Expr assignment() {
        // the tricky part for this, is that we can't treat the expression for 
        // assignment the same as a part of the previous expressions, because
        // we can't "evaluate" the LHS, the variable, since it isn't a real
        // expression but a reference that we can use to get a handle on a spot in memory 
        Expr expr = or();
        // above, this finds out what the LHS expression is
        // unless it is a variable, as checked below, do not 
        // return the assign expression, so a + b = c is not allowed
        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable) expr).name;
                return new Expr.Assign(name, value);
            }

            error(equals,"Invalid assignment target.");
        }
        return expr;
    }
    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and() {
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
        }
        return expr;
    }
    private Expr comparison() {
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
            // recursively construct new binary expression
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
        Expr expr = primary();
        while (true) {
            if (match(LEFT_PAREN)) {
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
        }

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression."); // currently any errors will break down here
    }

    private boolean match(TokenType...types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    private boolean check(TokenType type) {
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
        List<Expr> arguments = new ArrayList<>();
        // checks if the next token is a ), if so, then 
        // we are in the zero arguments case
        if (!check(RIGHT_PAREN)) {
            do {
                if (arguments.size() > 255) {
                    error(peek(),"Can't have more than 255 arguments.");
                }
                arguments.add(expression());
                
            } while (match(COMMA));
        }
        Token paren = consume(RIGHT_PAREN,"Expect ')' after arguments");
        return new Expr.Call(callee, paren, arguments);
    }   

    // parse triggers at the top of the recursive descent
    // hierarchy, which fires everything else - this order makes
    // sense since every expression is some type of statement
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            if (match(FUN)) {
                return function("function");

            }
            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }
    private Stmt.Function function(String kind) {
        Token name = consume(IDENTIFIER,"Expect " + kind + " name.");
        consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList<>();
        if (!check(RIGHT_PAREN)) {
            do {
                if (parameters.size() > 255) {
                    error(peek(), "Can't have more than 255 parameters.");
                }
                parameters.add(consume(IDENTIFIER, "Expect paramter name."));
            } while (match(COMMA));
        }
        consume(RIGHT_PAREN, "Expect ')' after parameters");
        consume(LEFT_BRACE, "Expect '{' before " + kind + " body.");
        List<Stmt> body = block();
        return new Stmt.Function(name, parameters, body);
    }
    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        if (match(WHILE)) return whileStatement();
        if (match(FOR)) return forStatement();
        if (match(RETURN)) return returnStmt();
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
        Expr initialiser = null;
        if (match(EQUAL)) {
            initialiser = expression();
        }
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
        Expr expr = expression();
        consume(SEMICOLON, "expect ';' after expression.");
        return new Stmt.Expression(expr);
    }
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();

        while (!check(RIGHT_BRACE) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }
    private Stmt ifStatement() {
        consume(LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = expression();
        consume(RIGHT_PAREN, "Expect ')' after if condition.");

        Stmt thenBranch = statement();
        Stmt elseBranch = null;
        if (match(ELSE)) {
            elseBranch = statement();
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    private Stmt whileStatement() {
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

