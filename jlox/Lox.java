package jlox;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
// import jlox.Token;

public class Lox {

    // the fact that it is static means that the same interpreter
    // is being used during the same REPL session, so that 
    // global variables are stored
    private static final Interpreter interpreter = new Interpreter();

    // error handlers !! must reread
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() + "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }

    /*
     * the main function, for running the interpreter
     */
    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: jlox [script]");
            System.exit(64);
        } else if (args.length == 1) {
            System.out.println(args[0]);
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    /*
     * run by file, as per specified path
     */
    private static void runFile(String path) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes,Charset.defaultCharset()));
        if (hadError) {
            System.exit(65);
        }
        if (hadRuntimeError) {
            System.exit(70);
        }
    }

    /*
     * run from cmd line, via the line by line >>
     */
    private static void runPrompt() throws IOException {
        InputStreamReader input = new InputStreamReader(System.in);
        BufferedReader reader = new BufferedReader(input);
        for (;;) {
            System.out.print(">>> ");
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            run(line);
            hadError = false;
        }
    }

    /*
     * starting the process by scanning - for now just printing tokens
     */
    private static void run(String source) {
        Scanner scanner = new Scanner(source);
        List<Token> tokens = scanner.scanTokens();
        // for (Token token : tokens) {
        //     System.out.println("Line " + token.line + ": " + token.lexeme + " " + token.literal + " " + token.type);
        // }
        Parser parser = new Parser(tokens);
        List<Stmt> statements = parser.parse();
        if (hadError) return;
        // System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(statements);
    }


    /*
     * overloaded, error handling, this is an error directly from the Scanner
     */
    static void error(int line, String message) {
        report(line, "", message);
    }

    /*
     * function for writing the error reporting message
     */
    private static void report(int line, String where, String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    /*
     * deciding which kind of error to report during parsing, calls report()
     * to output the specific string for either error at the end of input or
     * at a specific spot
     * 
     * this overloaded error() is for errors from the Parser
     */
    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end" , message);
        } else {
            report (token.line, " at '" +  token.lexeme + "'", message);
        }
    }


}
