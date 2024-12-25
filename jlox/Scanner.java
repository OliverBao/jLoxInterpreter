package jlox;
import java.util.*;
import static jlox.TokenType.*;

/*
 * Documentation:
 * 
 * currently does not support having ' apostrophes / fixed - exotic characters not being 
 *  handled due to wrong indexing (used character instead of index) 
 * don't know what ctrl+D does
 * otherwise can run from terminal
 */

public class Scanner {

    // source is the text to be scanned
    private final String source;

    // the list of tokens to be returned
    private final List<Token> tokens = new ArrayList<>();

    // pointers to the current start and end of strings
    private int start = 0;
    private int current = 0;
    private int line = 1;

    // initialise the keywords of Lox
    private static final Map<String,TokenType> keywords;
    static {
        keywords = new HashMap<>();
        keywords.put("and", AND);
        keywords.put("class", CLASS);
        keywords.put("else", ELSE);
        keywords.put("false", FALSE);
        keywords.put("for", FOR);
        keywords.put("fun", FUN);
        keywords.put("if", IF);
        keywords.put("nil", NIL);
        keywords.put("or", OR);
        keywords.put("print", PRINT);
        keywords.put("return", RETURN);
        keywords.put("super", SUPER);
        keywords.put("this", THIS);
        keywords.put("true", TRUE);
        keywords.put("var", VAR);
        keywords.put("while", WHILE);
    }

    // constructor for the scanner class which sets the current string being read from
    Scanner(String source) {
        this.source = source;
    }

    // main loop, which keeps running scanToken() until the current point reaches the 
    // end of the string to be read, and then adds a EOF token
    public List<Token> scanTokens() {
        while (!isAtEnd()) {
            start = current;
            scanToken();
        }
        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    // checks for the current pointer to be at the end of the string, used
    // for knowing when to stop reading, esp when reading special lexemes
    private boolean isAtEnd() {
        return current >= source.length();
    }

    // main function call, which scans via categorisation, first the single
    // character lexems, then the longer character ones, and then the ones 
    // where peeking recurs until it meets \n or another character.
    private void scanToken() {
        char c = advance(); // looks at the next character
        switch (c) {

            // single character lexemes
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '+': addToken(PLUS); break;
            case '-': addToken(MINUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // double character lexemes, which uses match to do 1 step look ahead
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : BANG); break;
            case '<': addToken(match('=') ? LESS_EQUAL : BANG); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : BANG); break;
            case '/':
                if (match('/')) {
                    while(peek() != '\n' && !isAtEnd()) advance();
                } else {
                    addToken(SLASH);
                } break;

            // characters to ignore
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++; break;

            // reads the string
            case '"': string(); break;

            // handles cases of lexemes with arbitrary length
            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    // if else recognised character, then send print out the error message
                    Lox.error(line, "Unexpected character: " + String.valueOf(source.charAt(current-1)));
                }
                break;
        }
    }

    // gives next character to scan
    private char advance() {
        current++;
        return source.charAt(current-1);
    }

    // overloaded function for adding tokens without literals, such as indentifiers or keywords
    private void addToken(TokenType type) {
        addToken(type,null);
    }
    
    // overloaded function which adds tokens with literals, such as integers or strings
    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start,current);
        tokens.add(new Token(type, text, literal, line));
    }

    // one step look ahead to match current character, for //
    private boolean match(char expected) {
        if (isAtEnd()) return false;
        if (peek() != expected) return false;
        current ++;
        return true;
    }

    // used to just look ahead for characters, but not move the pointers
    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }
    private char peekNext() {
        if (current+1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current+1);
    }
    
    // checks for either a integer input or string input
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }
        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }
        advance();
        String value = source.substring(start+1,current-1);
        addToken(STRING,value);
    }
    private boolean isDigit(char c) {
        return c >= '0' &&  c <= '9';
    }
    private void number() {
        while (isDigit(peek())) {
            advance();
        }
        if (peek() == '.' && isDigit(peekNext())) {
            advance();
        }
        while (isDigit(peek())) {
            advance();
        }
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));;

    } 

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               (c == '_');
    }
    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }
    
    // checks for identifiers and keywords (since keywords are identifiers, can be bundled)
    // which are of arbitrary length, checks the keyword array, else classifies as identifier
    private void identifier() {
        while (isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start,current);
        TokenType type = keywords.get(text);
        if (type == null) {
            type = IDENTIFIER;
        }
        addToken(type);
    }

}
