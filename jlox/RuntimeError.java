package jlox;

public class RuntimeError extends RuntimeException {
    final Token token;

    RuntimeError(Token token, String message) {
        // inherit the same message from RuntimeException.
        super(message);
        this.token = token;
    }
}
