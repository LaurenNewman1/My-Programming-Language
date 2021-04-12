package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        // create a "class Main{"
        //      declare fields
        //      declare "public static void main(String[] args) {
        //                      System.exit(main());
        //               }
        //      declare each of our methods
        //      one of our methods is called main!
        print("public class Main {");
        newline(indent);
        if (!ast.getFields().isEmpty()) {
            newline(++indent);
            int lastField = ast.getFields().size() - 1;
            for (int i = 0; i < lastField; i++) {
                print(ast.getFields().get(i));
                newline(indent);
            }
            print(ast.getFields().get(lastField));
            newline(--indent);
        }
        newline(++indent);
        print("public static void main(String[] args) {");
        newline(++indent);
        print("System.exit(new Main().main());");
        newline(--indent);
        print("}");
        newline(--indent); newline(++indent);
        for (Ast.Method method : ast.getMethods()) {
            print(method);
            newline(--indent); newline(indent);
        }
        print("}");
        return  null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type type = Environment.getType(ast.getTypeName());
        print(type.getJvmName(), " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        // Return type
        if (ast.getReturnTypeName().isPresent()) {
            Environment.Type type = Environment.getType(ast.getReturnTypeName().get());
            print(type.getJvmName(), " ");
        }
        // Parameters
        print(ast.getName(), "(");
        int lastParam = ast.getParameters().size() - 1;
        for (int i = 0; i < lastParam; i++) {
            Environment.Type type = Environment.getType(ast.getParameters().get(i));
            print(type.getJvmName(), " ", ast.getParameters().get(i), ", ");
        }
        // No parameters
        if (lastParam == -1) {
            print(") {");
        }
        else {
            Environment.Type type = Environment.getType(ast.getParameterTypeNames().get(lastParam));
            print(type.getJvmName(), " ", ast.getParameters().get(lastParam), ") {");
        }
        // Statements
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        print(ast.getExpression(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        print(ast.getVariable().getType().getJvmName(),
                " ",
                ast.getVariable().getJvmName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        print(ast.getReceiver(), " = ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        // Initialize
        print("if (", ast.getCondition(), ") {");
        if (!ast.getThenStatements().isEmpty()) {
            // Print then statements
            newline(++indent);
            for (int i = 0; i < ast.getThenStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getThenStatements().get(i));
            }
            // Print else statements
            if (!ast.getElseStatements().isEmpty()) {
                newline(--indent);
                print("} else {");
                newline(++indent);
                for (int i = 0; i < ast.getElseStatements().size(); i++) {
                    if (i != 0) {
                        newline(indent);
                    }
                    print(ast.getElseStatements().get(i));
                }
            }
            newline(--indent);
        }
        // Close
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        print("for (", ast.getValue().getType().getJvmName(), " ", ast.getName(), " : ", ast.getValue(), ") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        print("white (", ast.getCondition(), ") {");
        if (!ast.getStatements().isEmpty()) {
            newline(++indent);
            for (int i = 0; i < ast.getStatements().size(); i++) {
                if (i != 0) {
                    newline(indent);
                }
                print(ast.getStatements().get(i));
            }
            newline(--indent);
        }
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        print("return ", ast.getValue(), ";");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        if (ast.getType().equals(Environment.Type.CHARACTER)) {
            print("'", ast.getLiteral(), "'");
        }
        else if (ast.getType().equals(Environment.Type.STRING)) {
            print("\"", ast.getLiteral(), "\"");
        }
        else {
            print(ast.getLiteral());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        print("(", ast.getExpression(), ")");
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        if (ast.getOperator().equals("AND")) {
            print(ast.getLeft(), " && ", ast.getRight());
        }
        else if (ast.getOperator().equals("OR")) {
            print(ast.getLeft(), " || ", ast.getRight());
        }
        else {
            print(ast.getLeft(), " ", ast.getOperator(), " ", ast.getRight());
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }
        print(ast.getVariable().getJvmName());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        if (ast.getReceiver().isPresent()) {
            print(ast.getReceiver().get(), ".");
        }
        print(ast.getFunction().getJvmName(), "(");
        int lastArg = ast.getArguments().size() - 1;
        for (int i = 0; i < lastArg; i++) {
            print(ast.getArguments().get(i), ", ");
        }
        // No parameters
        if (lastArg == -1) {
            print(") {");
        }
        else {
            print(ast.getArguments().get(lastArg), ")");
        }
        return null;
    }

}
