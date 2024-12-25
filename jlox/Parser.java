package jlox;

import java.util.ArrayList;
import java.util.List;
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
        return equality();
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

        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }

        throw error(peek(), "Expect expression.");
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
            statements.add(statement());
        }
        return statements;
    }

    private Stmt statement() {
        if (match(PRINT)) return printStatement();
        return expressionStatement();
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

    // private void synchronise() {
    //     advance();
    //     while (!isAtEnd()) {
    //         if (previous().type == SEMICOLON) {
    //             return;
    //         }
    //         switch (peek().type) {
    //             case CLASS:
    //             case FUN:
    //             case VAR:
    //             case FOR:
    //             case IF:
    //             case WHILE:
    //             case PRINT:
    //             case RETURN:
    //                 return;
    //         }
    //         advance();
    //     }
    // }
}

