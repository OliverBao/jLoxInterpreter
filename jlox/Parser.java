package jlox;

import java.util.ArrayList;
import java.util.List;

// here the parser handles syntax - putting things together according
// to the CFGs defined, and pushes to the interpreter the 
// list of statements/expressions/declarations to be handled
// or evaluated (the interpreter handles the semantics)

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
        Expr expr = equality();
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
        return primary();
    }
    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(FALSE);
        if (match(TRUE)) return new Expr.Literal(TRUE);
        if (match(NIL)) return new Expr.Literal(NIL);

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

    // parse triggers at the top of the recursive descent
    // hierarchy, which fires everything else - this order makes
    // sense since every expression is some type of statement
    private Stmt declaration() {
        try {
            if (match(VAR)) {
                return varDeclaration();
            }
            return statement();
        } catch (ParseError error) {
            synchronise();
            return null;
        }
    }
    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        if (match(LEFT_BRACE)) return new Stmt.Block(block());
        return expressionStatement();
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
    //collapse to the expression productions
    private Stmt printStatement() {
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
}

