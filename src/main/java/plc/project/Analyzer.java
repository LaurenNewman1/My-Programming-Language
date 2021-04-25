package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Method method;
    //private Environment.Type returnType;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        Environment.Function main = scope.lookupFunction("main", 0);
        requireAssignable(Environment.Type.INTEGER, main.getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Field ast) {
        Environment.Type type = Environment.getType(ast.getTypeName());
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            requireAssignable(type, ast.getValue().get().getType());
        }
        scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Method ast) {
        this.method = ast;
        // Return Type
        Environment.Type returnType = Environment.Type.NIL;
        if (ast.getReturnTypeName().isPresent()) {
            returnType = Environment.getType(ast.getReturnTypeName().get());
        }
        // Parameters
        List<Environment.Type> parameterTypes = new ArrayList<>();
        for (int i = 0; i < ast.getParameters().size(); i++) {
            Environment.Type type = Environment.getType(ast.getParameterTypeNames().get(i));
            scope.defineVariable(ast.getParameters().get(i), ast.getParameters().get(i), type, Environment.NIL);
            parameterTypes.add(type);
        }
        // Define & Set Function
        scope.defineFunction(ast.getName(), ast.getName(), parameterTypes,
                returnType, args -> Environment.NIL);
        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getParameters().size()));
        // Statements
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Expression ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Function)) {
            throw new RuntimeException("Statement expressions must call functions");
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Declaration ast) {
        // Type
        Environment.Type type = null;
        if (ast.getTypeName().isPresent()) {
            type = Environment.getType(ast.getTypeName().get());
        }
        // Initialization
        if (ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if (ast.getTypeName().isPresent()) {
                requireAssignable(type, ast.getValue().get().getType());
            }
            else {
                type = ast.getValue().get().getType();
            }
        }
        // Handle no type without assignment
        if (type == null) {
            throw new RuntimeException("Declaration statement without assignment must have type specified");
        }
        // Declaration
        scope.defineVariable(ast.getName(), ast.getName(), type, Environment.NIL);
        ast.setVariable(scope.lookupVariable(ast.getName()));
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Assignment ast) {
        // Visit
        visit(ast.getReceiver());
        visit(ast.getValue());
        // Verify Requirements
        if (!(ast.getReceiver() instanceof Ast.Expr.Access)) {
            throw new RuntimeException("Receiver must be an access expression");
        }
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.If ast) {
        // Condition
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        // Check that list isn't empty
        if (ast.getThenStatements().isEmpty()) {
            throw new RuntimeException("Then statements list is empty");
        }
        // Then Statements
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getThenStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        // Else Statements
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getElseStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.For ast) {
        // Verify requirements
        visit(ast.getValue());
        requireAssignable(Environment.Type.INTEGER_ITERABLE, ast.getValue().getType());
        if (ast.getStatements().isEmpty()) {
            throw new RuntimeException("For statement list cannot be empty.");
        }
        // Visit statements
        try {
            scope = new Scope(scope);
            scope.defineVariable(ast.getName(), ast.getName(), Environment.Type.INTEGER, Environment.NIL);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.While ast) {
        visit(ast.getCondition());
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());
        try {
            scope = new Scope(scope);
            for (Ast.Stmt stmt : ast.getStatements()) {
                visit(stmt);
            }
        }
        finally {
            scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Stmt.Return ast) {
        visit(ast.getValue());
        Environment.Type returnType = Environment.getType(method.getReturnTypeName().get());
        requireAssignable(returnType, ast.getValue().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Literal ast) {
        // Nil
        if (ast.getLiteral() == null) {
            ast.setType(Environment.Type.NIL);
        }
        // Boolean
        else if (ast.getLiteral() instanceof Boolean) {
            ast.setType(Environment.Type.BOOLEAN);
        }
        // Character
        else if (ast.getLiteral() instanceof Character) {
            ast.setType(Environment.Type.CHARACTER);
        }
        // String
        else if (ast.getLiteral() instanceof String) {
            ast.setType(Environment.Type.STRING);
        }
        // Integer
        else if (ast.getLiteral() instanceof BigInteger) {
            if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0) {
                throw new RuntimeException("BigInteger value out of bounds");
            }
            else if (((BigInteger) ast.getLiteral()).compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) < 0) {
                throw new RuntimeException("BigInteger value out of bounds");
            }
            ast.setType(Environment.Type.INTEGER);
        }
        // Decimal
        else if (ast.getLiteral() instanceof BigDecimal) {
            if (((BigDecimal) ast.getLiteral()).doubleValue() == Double.POSITIVE_INFINITY ||
                    ((BigDecimal) ast.getLiteral()).doubleValue() == Double.NEGATIVE_INFINITY) {
                throw new RuntimeException("BigDecimal value out of bounds");
            }
            ast.setType(Environment.Type.DECIMAL);
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Group ast) {
        if (!(ast.getExpression() instanceof Ast.Expr.Binary)) {
            throw new RuntimeException("Groups must contain a binary expression");
        }
        visit(ast.getExpression());
        ast.setType(ast.getExpression().getType());
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Binary ast) {
        visit(ast.getLeft());
        visit(ast.getRight());
        // Boolean Operator -> Boolean
        if (ast.getOperator().equals("AND") || ast.getOperator().equals("OR")) {
            requireAssignable(Environment.Type.BOOLEAN, ast.getLeft().getType());
            requireAssignable(Environment.Type.BOOLEAN, ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        // Comparable Operator -> Boolean
        else if (ast.getOperator().equals("<") || ast.getOperator().equals("<=") || ast.getOperator().equals(">") ||
                ast.getOperator().equals(">=") || ast.getOperator().equals("==") || ast.getOperator().equals("!=")) {
            requireAssignable(Environment.Type.COMPARABLE, ast.getLeft().getType());
            requireAssignable(Environment.Type.COMPARABLE, ast.getRight().getType());
            requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
            ast.setType(Environment.Type.BOOLEAN);
        }
        // Concat -> String
        else if (ast.getOperator().equals("+") && (ast.getLeft().getType().equals(Environment.Type.STRING) ||
                ast.getRight().getType().equals(Environment.Type.STRING))) {
            ast.setType(Environment.Type.STRING);
        }
        // Arithmetic
        else if (ast.getOperator().equals("+") || ast.getOperator().equals("-") || ast.getOperator().equals("*")
                || ast.getOperator().equals("/")) {
            // Integer Arithmetic -> Integer
            if (ast.getLeft().getType().equals(Environment.Type.INTEGER)){
                requireAssignable(Environment.Type.INTEGER, ast.getRight().getType());
                ast.setType(Environment.Type.INTEGER);
            }
            // Decimal Arithmetic -> Decimal
            else if (ast.getLeft().getType().equals(Environment.Type.DECIMAL)) {
                requireAssignable(Environment.Type.DECIMAL, ast.getRight().getType());
                ast.setType(Environment.Type.DECIMAL);
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Access ast) {
        // Has Receiver
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Ast.Expr.Access expr = ast;
            List<String> chain = new ArrayList<>();
            // Get outermost receiver
            while(expr.getReceiver().isPresent()) {
                chain.add(expr.getName());
                expr = (Ast.Expr.Access)expr.getReceiver().get();
            }
            // Chain inwards to get field
            Environment.Variable receiver = scope.lookupVariable(expr.getName());
            for(int i = chain.size() - 1; i > -1; i--) {
                receiver = receiver.getType().getField(chain.get(i));
            }
            ast.setVariable(receiver);
        }
        // Variable
        else {
            ast.setVariable(scope.lookupVariable(ast.getName()));
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expr.Function ast) {
        // Has receiver
        if (ast.getReceiver().isPresent()) {
            visit(ast.getReceiver().get());
            Environment.Function method = ast.getReceiver().get().getType().getMethod(ast.getName(), ast.getArguments().size());
            // Verify Arguments // Starts at 1 !!!
            for (int i = 1; i < method.getParameterTypes().size(); i++) {
                visit(ast.getArguments().get(i-1));
                requireAssignable(method.getParameterTypes().get(i), ast.getArguments().get(i-1).getType());
            }
            ast.setFunction(method);
        }
        // Regular function
        else {
            Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());
            // Verify Arguments
            for (int i = 0; i < ast.getArguments().size(); i++) {
                visit(ast.getArguments().get(i));
                requireAssignable(func.getParameterTypes().get(i), ast.getArguments().get(i).getType());
            }
            ast.setFunction(func);
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        if (target.equals(Environment.Type.ANY)) {
            return;
        }
        else if (target.equals(Environment.Type.COMPARABLE)) {
            if (!type.equals(Environment.Type.INTEGER) && !type.equals(Environment.Type.DECIMAL) &&
                !type.equals(Environment.Type.CHARACTER) && !type.equals(Environment.Type.STRING) &&
                !type.equals(Environment.Type.COMPARABLE)) {
                throw new RuntimeException("Error: Not comparable");
            }
        }
        else if (!type.equals(target)) {
            throw new RuntimeException("Error: Not assignable");
        }
    }

}
