package tool;
import java.io.*;
import java.util.*;;

public class GenerateAst {

    public static void main(String[] args) throws IOException {
        String outputDir = "jlox";

        // the outputDir is the directory to place the new file in,
        // the base name is the name of the file,
        // and the third argument is the list of CFG productions
        defineAst(outputDir, "Expr", Arrays.asList(
            "Literal  : Object value",
                 "Unary    : Token operator, Expr right",
                 "Binary   : Expr left, Token operator, Expr right",
                 "Grouping : Expr expression"
        ));

        defineAst(outputDir, "Stmt", Arrays.asList(
            "Expression : Expr expression",
                 "Print      : Expr expression"
        ));

    }

    // write the abstract class which implements each CFG production rule
    private static void defineAst(
        String outputDir, String baseName, List<String> types)
        throws IOException {
            
            // formats the path to write to, which is the directory, now
            // with the file name specified
            String path = outputDir + "/" + baseName + ".java";

            // initialise the writer object, which creates the file & does the writing
            PrintWriter writer = new PrintWriter(path,"UTF-8");

            // java boilerplate code for the abstract class
            writer.println("package jlox;");
            // writer.println();
            // writer.println("import java.util.*;");
            writer.println();
            writer.println("abstract class " + baseName + " {");
            writer.println();

            defineVisitor(writer,baseName,types);

            writer.println();
            writer.println("    abstract <R> R accept(Visitor<R> visitor);");
            writer.println();

            // for each production, write the relevant class implementation of the 
            // abstract Expr class, writing to its fields and constructors
            for (String type : types) {
                String className = type.split(":")[0].trim();
                String fields = type.split(":")[1].trim();
                defineType(writer, baseName, className,fields);
            }

            writer.println("}");
            writer.close();
    }

    // write the implementation of each subclass for the abstract class
    private static void defineType(
        PrintWriter writer, String baseName,
        String className, String fieldList) {

            writer.println("    static class " + className + " extends " + baseName + " {");

            String[] fields = fieldList.split(", ");

            for (String field : fields) {
                writer.println("        final " + field + ";");
            }

            writer.println("        " + className + "(" + fieldList + ") {");

            for (String field : fields) {
                String name = field.split(" ")[1];
                writer.println("            this." + name + " = " + name + ";");
            }

            writer.println("        }");

            writer.println("        @Override");
            writer.println("        <R> R accept(Visitor<R> visitor) {");
            writer.println("            return visitor.visit" + className + baseName + "(this);");
            writer.println("        }");   


            writer.println("    }");
            writer.println();

    }
      
    private static void defineVisitor(
        PrintWriter writer, String baseName, List<String> types) {
            writer.println("    interface Visitor<R> {");

            for (String type : types) {
                String typeName = type.split(":")[0].trim();
                writer.println("        R visit" + typeName + baseName + "(" + typeName + " " + baseName.toLowerCase() + ");");
            }
            writer.println("    }");
    }
        

}
