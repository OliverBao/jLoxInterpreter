package jlox;

import java.util.*;

import jlox.Lox.LoxCallable;

public class LoxFunction implements LoxCallable {
    private final Stmt.Function declaration;
    LoxFunction(Stmt. Function delcaration) {
        this.declaration = delcaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // the new environment unique to this run of the function
        // inherits directly from the global environment, from which it can
        // see the variables, but nothing else.
        // another important fact is that this function call is unique, 
        // as in every function *call* gets its own environment, otherwise
        // recursion would not work (when the same variables from the previous
        // environment are being assigned and then handed to the next call)
        // which means that exploring different paths with the call stack
        // doesn't do the backtracking anymore.
        Environment environment = new Environment(interpreter.globals);
        for (int i = 0; i < declaration.params.size(); i++) {
            environment.define(declaration.params.get(i).lexeme, arguments.get(i));
        }
        // by using a try catch block here, the intepreter can always trace'
        // the execution of a function to where it was called, and just directly
        // out of it into the catch block to return the value. neat
        try {
            interpreter.executeBlock(declaration.body,environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn" + declaration.name.lexeme + ">";
    }



}
