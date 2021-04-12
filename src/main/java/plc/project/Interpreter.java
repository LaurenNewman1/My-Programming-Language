package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {
        for (Ast.Field field : ast.getFields()) {
            visit(field);
        }
        for (Ast.Method method : ast.getMethods()) {
            visit(method);
        }
        Environment.Function main = scope.lookupFunction("main", 0);
        return main.invoke(Arrays.asList());
    }

    @Override
    public Environment.PlcObject visit(Ast.Field ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Method ast) {
        Scope curr = scope;
        scope.defineFunction(ast.getName(), ast.getParameters().size(), args -> {
            try {
                scope = new Scope(curr);
                // Check number of arguments
                if (ast.getParameters().size() != args.size()) {
                    throw new RuntimeException("Incorrect number of arguments passed to method.");
                }
                // Create variables for each parameter
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), args.get(i));
                }
                // Visit statements
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            catch (Return ret) {
                return ret.value;
            }
            return Environment.NIL;
        });
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Declaration ast) {
        if (ast.getValue().isPresent()) {
            scope.defineVariable(ast.getName(), visit(ast.getValue().get()));
        }
        else {
            scope.defineVariable(ast.getName(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Assignment ast) {
        // Require access type
        if (!isType(Ast.Expr.Access.class, ast.getReceiver())) {
            throw new RuntimeException("Type access required.");
        }
        Ast.Expr.Access access = (Ast.Expr.Access) ast.getReceiver();
        // Field assignment
        if (access.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(access.getReceiver().get());
            receiver.getField(access.getName()).setValue(visit(ast.getValue()));
        }
        // Variable assignment
        else {
            Environment.Variable var = scope.lookupVariable(access.getName());
            var.setValue(visit(ast.getValue()));
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.If ast) {
        if (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getThenStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        else {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getElseStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.For ast) {
        Iterable<?> it = requireType(Iterable.class, visit(ast.getValue()));
        // Iterate through for loop
        for (Object obj : it) {
            try {
                scope = new Scope(scope);
                scope.defineVariable(ast.getName(), (Environment.PlcObject)obj);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.While ast) {
        while (requireType(Boolean.class, visit(ast.getCondition()))) {
            try {
                scope = new Scope(scope);
                for (Ast.Stmt stmt : ast.getStatements()) {
                    visit(stmt);
                }
            }
            finally {
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Stmt.Return ast) {
        throw new Return(visit(ast.getValue()));
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Literal ast) {
        if (ast.getLiteral() == null) {
            return Environment.NIL;
        }
        return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Group ast) {
        Environment.PlcObject eval = visit(ast.getExpression());
        return Environment.create(eval.getValue());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Binary ast) {
        if (ast.getOperator().equals("OR")) {
            Boolean leftBool = requireType(Boolean.class, visit(ast.getLeft()));
            if (leftBool) {
                return Environment.create(true);
            }
            Boolean rightBool = requireType(Boolean.class, visit(ast.getRight()));
            if (rightBool) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        if (ast.getOperator().equals("AND")) {
            Boolean leftBool = requireType(Boolean.class, visit(ast.getLeft()));
            // Short circuit
            if (!leftBool) {
                return Environment.create(false);
            }
            Boolean rightBool = requireType(Boolean.class, visit(ast.getRight()));
            if (leftBool && rightBool) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        Environment.PlcObject left = visit(ast.getLeft());
        Environment.PlcObject right = visit(ast.getRight());
        if (ast.getOperator().equals("<") || ast.getOperator().equals(">")
                    || ast.getOperator().equals("<=") || ast.getOperator().equals(">=")) {
            if (!isType(left.getValue().getClass(), right)) {
                throw new RuntimeException("Attempted to compare different types");
            }
            Comparable<Object> leftComp = requireType(Comparable.class, left);
            Comparable<?> rightComp = requireType(Comparable.class, right);
            switch (ast.getOperator()) {
                case "<":
                    if (leftComp.compareTo(rightComp) < 0)
                        return Environment.create(true);
                    return Environment.create(false);
                case ">":
                    if (leftComp.compareTo(rightComp) > 0)
                        return Environment.create(true);
                    return Environment.create(false);
                case "<=":
                    if (leftComp.compareTo(rightComp) < 0 || leftComp.compareTo(rightComp) == 0)
                        return Environment.create(true);
                    return Environment.create(false);
                case ">=":
                    if (leftComp.compareTo(rightComp) > 0 || leftComp.compareTo(rightComp) == 0)
                        return Environment.create(true);
                    return Environment.create(false);
            }
            throw new RuntimeException("Failed to execute comparison");
        }
        else if (ast.getOperator().equals("==")) {
            if (Objects.equals(left, right)) {
                return Environment.create(true);
            }
            return Environment.create(false);
        }
        else if (ast.getOperator().equals("!=")) {
            if (Objects.equals(left, right)) {
                return Environment.create(false);
            }
            return Environment.create(true);
        }
        // Concat
        else if (ast.getOperator().equals("+") && (isType(String.class, left) || isType(String.class, right))) {
            String leftStr = left.getValue().toString();
            String rightStr = right.getValue().toString();
            return Environment.create(leftStr.concat(rightStr));
        }
        // Arithmetic
        else if (ast.getOperator().equals("+") || ast.getOperator().equals("-")
                || ast.getOperator().equals("*") || ast.getOperator().equals("/")) {
            // Decimals
            if (isType(BigDecimal.class, left) && isType(BigDecimal.class, right)) {
                BigDecimal leftVal = (BigDecimal) left.getValue();
                BigDecimal rightVal = (BigDecimal) right.getValue();
                switch (ast.getOperator()) {
                    case "+":
                        return Environment.create(leftVal.add(rightVal));
                    case "-":
                        return Environment.create(leftVal.subtract(rightVal));
                    case "*":
                        return Environment.create(leftVal.multiply(rightVal));
                    case "/":
                        if (rightVal.equals(BigDecimal.ZERO)) {
                            throw new RuntimeException("Divide by zero");
                        }
                        return Environment.create(leftVal.divide(rightVal, 1, RoundingMode.HALF_EVEN));
                }
            }
            // Integers
            else if (isType(BigInteger.class, left) && isType(BigInteger.class, right)) {
                BigInteger leftVal = (BigInteger) left.getValue();
                BigInteger rightVal = (BigInteger) right.getValue();
                switch (ast.getOperator()) {
                    case "+":
                        return Environment.create(leftVal.add(rightVal));
                    case "-":
                        return Environment.create(leftVal.subtract(rightVal));
                    case "*":
                        return Environment.create(leftVal.multiply(rightVal));
                    case "/":
                        if (rightVal.equals(BigInteger.ZERO)) {
                            throw new RuntimeException("Divide by zero");
                        }
                        return Environment.create(leftVal.divide(rightVal));
                }
            }
            throw new RuntimeException("Unexpected type");

        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Access ast) {
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            return receiver.getField(ast.getName()).getValue();
        }
        else {
            return scope.lookupVariable(ast.getName()).getValue();
        }
    }

    @Override
    public Environment.PlcObject visit(Ast.Expr.Function ast) {
        // Has receiver
        if (ast.getReceiver().isPresent()) {
            Environment.PlcObject receiver = visit(ast.getReceiver().get());
            // Arguments
            List<Environment.PlcObject> args = new ArrayList<>();
            for (Ast.Expr a : ast.getArguments()) {
                args.add(visit(a));
            }
            return receiver.callMethod(ast.getName(), args);
        }
        // Arguments
        List<Environment.PlcObject> args = new ArrayList<>();
        for (Ast.Expr a : ast.getArguments()) {
            args.add(visit(a));
        }
        // Regular function
        Environment.Function func = scope.lookupFunction(ast.getName(), ast.getArguments().size());
        Scope curr = scope;
        Environment.PlcObject returnValue = func.invoke(args);
        scope = curr;
        return returnValue;
    }

    public boolean isType(Class<?> t, Environment.PlcObject a) {
        return a.getValue().getClass().equals(t);
    }

    public boolean isType(Class<?> t, Ast.Expr a) {
        return a.getClass().equals(t);
    }

    /**
     * Helper function to ensure an object is of the appropriate type.
     */
    private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + "."); //TODO
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
