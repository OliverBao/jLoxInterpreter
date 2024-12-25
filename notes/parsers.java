package notes;

public class parsers {

    /*
     * the scanner performs the analysis of the lexical grammar,
     * which is character based
     * 
     * the parser performs the analysis of the syntactical grammar,
     * which is token based
     * 
     * basically the parser needs to implement the formal grammer
     * in order to tell which strings are valid and which aren't
     * 
     * and to capture an infinite set of possible strings within
     * finite constraints, we use a finite set of rules for producing
     * new strings, the production rules.
     * 
     * hence, the rules are called the productions and the strings 
     * they procreate are called the derivations.
     * 
     * call back to COMP1600, every production in a context-free grammar 
     * has its single non-terminal on the left hand side, and a body which
     * describes what it can generate
     * 
     * the terminals are the letters from the grammar's alphabet, like
     * literal values, or individual lexemes in this case
     * 
     * a nonterminal is a reference to another rule in the grammar, which
     * allows more rules to be played
     * 
     * and there may be multiple rules with the same name, and
     * when the programs reaches that point, one is allowed to pick any 
     * of the rules
     * 
     * Backus Naur form is a notation used to descriube
     * the syntax of programming languages or other formal languages
     * they are basically like a set of production rules that govern how
     * certain strings and languages can be constructed.
     * 
     */
    
}
