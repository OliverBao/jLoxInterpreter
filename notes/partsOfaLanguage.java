package notes;

public class partsOfaLanguage {

    /*
    the first step is called scanning, lexing, tokenising, or lexical analysis

    this is where the characters in the source files are actually read to be
    split into more meaningful "words", or lexemes, and where meaningless stuff
    such as whitespaces and meaningful distinctions such as parentheses are
    identified, discarded or treated respectively.

    when these lexemes are then packaged together with other information specifically
    about what each of them means, then they become a token

    these tokens can be words or single characters

     */

    /*

    the second step is called parsing, where the flat sequence of tokens is
    built into a syntax tree, called an abstract syntax tree or just a tree,
    which is built based on semantic meaning and identifies syntax errors

     */


    /*

    the above two steps are generally the same across all programming languages, but the next
    step, static analysis, where the starting state of the code actually becomes meaningful,
    is still not yet defined.

    it is here that all the variables used, are assigned spaces in memory, if they are global or
    local or private or protected, where operations and other stuff are queued, etc.

    so this means tying identifiers to where they are defined in memory where they are in scope,
    and hence also we can do the type checking (static)

    the whole process is a pipeline such that the next step becomes easier. this means that lexing,
    parsing and static analysis are all a part of the front end, deciphering the code, and the
    back end is where the processed stuff is fitted specifically to run on the architectures that
    are being used and where the code will run.

    in the middle it might take an "intermediate form", which is not related either source or destination
    forms, but is useful to be stored in, which also allows support for multiple source languages and
    target platforms (by just writing different front ends and back ends for each source and destination instead
    of writing compilers between every single combination)

     */

    /*

    the next step is optimizing, when the intermediate representation can be optimized into something with the
    same semantics but faster

     */

    /*

    after all that the last step is code generation, the generation of machine code that can actually be run, but
    a human might not want to read.

    this is machine code, which is either ran on a real machine, which means that it is architecture specific
    (working on x86 means it will not work on ARM), or a virtual machine, which is some idealised
    form that is portable across different architectures.

     */

    /*

    writing a virtual machine is like simulating a hypothetical chip which executes bytecode by
    simulating it at runtime. But in exchange one gets simplicity and portability, since if
    a VM is implemented in C then any machine which has a C compiler can just run that code

    another method is transpiling, where you would use another already in place IR, and transpile your
    code that language to use its optimisation and code generation.

    the difference between a compiler and an interpreter is that a compiler translates the
    code wholly all at once, such as into an executable or other intermediate forms, usually
    lower level. however, it does not run the file.

    an interpreter takes the source code and executes it directly. it runs programs from the source

    there are clear distinctions, such as GCC and Clang are all compiled languages - the code is
    translated to machine code and then ran immediately (which can involve compilation in the process)

    an interpreted language would be directly executed by parsing and then traversing the syntax tree

     */

}
