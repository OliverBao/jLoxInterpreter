package jlox;

import java.util.*;

/* 
 * this is the environment object which will store all the values of the 
 * defined variables. implemented as a hashmap, with a recursive field which
 * leads to the previous enclosing lexical scope, so that the scope hierarchy
 * is implemented correctly
 */

public class Environment {

    // stores the values of all the variables, which are represented as strings
    // (as opposed to tokens, since tokens can have the same lexeme but still)
    // differ based on their line numbers, etc. here we want a universal reference
    private final Map<String,Object> values = new HashMap<>();

    // the enclosing environment, the entire environment above, which might also
    // contain more and more environments above it.
    // this is a classic use of recursion, and is used during runtime when the 
    // interpreter finds a new block, it creates a new environment, with the current
    // hashmap empty, but enclosing ones full.
    final Environment enclosing;

    Environment() {
        enclosing = null;
    }

    // recursively defined environments, which implements
    // the scoping
    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    // just insert the values - here we need to use object, since the values
    // we store can be strings, integers, etc. and even later function definitions.
    void define(String name, Object value) {
        values.put(name,value);
    }

    // get value of a stored name
    Object get(Token name) {
        // if the current environment has the target lexeme, return its associated value
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }
        // else, search in the enclosing environments above
        if (enclosing != null) {
            return enclosing.get(name);
        }
        // otherwise, just throw an error - it's not defined
        // throwing the RuntimeError doesn't crash the entire REPL, since it is the 
        // custom RuntimeError that we defined - why not? I don't think that it's caught anywhere
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assign(Token name, Object value) {
        // first check the local scope to assign
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }
        // if a variable name isn't in the local scope, then it should be in the
        // higher up ones
        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }
        // if it's not in the higher up ones, it's not anywhere, so cut
        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

}
