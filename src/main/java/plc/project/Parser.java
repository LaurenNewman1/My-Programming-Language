package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Field> fields = new ArrayList<>();
        List<Ast.Method> methods = new ArrayList<>();
        while(match("LET")) {
            fields.add(parseField());
        }
        while(match("DEF")) {
            methods.add(parseMethod());
        }
        if (tokens.has(0)) {
            throw new ParseException("Cannot have any fields after methods", tokens.getErrorIndex());
        }
        return new Ast.Source(fields, methods);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Field parseField() throws ParseException {
        Ast.Stmt.Declaration dec = parseDeclarationStatement();
        return new Ast.Field(dec.getName(), dec.getValue());
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Method parseMethod() throws ParseException {
        String name = tokens.get(0).getLiteral();
        List<String> parameters = new ArrayList<>();
        List<Ast.Stmt> statements = new ArrayList<>();
        if (!match(Token.Type.IDENTIFIER)) {
            throw new ParseException("Missing an identifier in method", tokens.getErrorIndex());
        }
        if (!match("(")) {
            throw new ParseException("Missing open parenthesis", tokens.getErrorIndex());
        }
        if (peek(Token.Type.IDENTIFIER)) {
            parameters.add(tokens.get(0).getLiteral());
            tokens.advance();
            while (match(",")) {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Expected identifier", tokens.getErrorIndex());
                }
                parameters.add(tokens.get(0).getLiteral());
                tokens.advance();
            }
        }
        if (!match(")")) {
            throw new ParseException("Missing ending parenthesis", tokens.getErrorIndex());
        }
        if (!match("DO")) {
            throw new ParseException("Missing DO", tokens.getErrorIndex());
        }
        while (!match("END")) {
            Ast.Stmt statement = parseStatement();
            statements.add(statement);
        }
        return new Ast.Method(name, parameters, statements);
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Stmt parseStatement() throws ParseException {
        if (match("LET")) {
            return parseDeclarationStatement();
        }
        else if (match("IF")) {
            return parseIfStatement();
        }
        else if (match("FOR")) {
            return parseForStatement();
        }
        else if (match("WHILE")) {
            return parseWhileStatement();
        }
        else if (match("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Stmt.Expression exprStmt = parseExpressionStatement();
            if (match("=")) {
                return parseAssignmentStatement(exprStmt);
            }
            if (!match(";")) {
                throw new ParseException("Missing semicolon after expression statement", tokens.getErrorIndex());
            }
            return exprStmt;
        }
    }

    /**
     * Parses the {@code expression-statement} rule. This method is called if
     * the next tokens do not start another statement type, as explained in the
     * javadocs of {@link #parseStatement()}.
     */
    public Ast.Stmt.Expression parseExpressionStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        return new Ast.Stmt.Expression(expression);
    }

    public Ast.Stmt.Assignment parseAssignmentStatement(Ast.Stmt.Expression receiver) throws ParseException {
        Ast.Expr value = parseExpression();
        if (!match(";")) {
            throw new ParseException("Missing semicolon after assignment statement", tokens.getErrorIndex());
        }
        return new Ast.Stmt.Assignment(receiver.getExpression(), value);
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Stmt.Declaration parseDeclarationStatement() throws ParseException {
        String name;
        Optional<Ast.Expr> value = Optional.empty();
        // Declared variable
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected an identifier", tokens.getErrorIndex());
        }
        name = tokens.get(0).getLiteral();
        tokens.advance();
        // Initialization
        if (match("=")) {
            Ast.Expr optionalExpr = parseExpression();
            if (optionalExpr == null) {
                throw new ParseException("Expected an identifier", tokens.getErrorIndex());
            }
            value = Optional.of(optionalExpr);
        }
        // Semicolon
        if (!match(";")) {
            throw new ParseException("Missing semicolon after declaration", tokens.getErrorIndex());
        }
        return new Ast.Stmt.Declaration(name, value);
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Stmt.If parseIfStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        List<Ast.Stmt> thenStatements = new ArrayList<>();
        List<Ast.Stmt> elseStatements = new ArrayList<>();
        if (!match("DO")) {
            throw new ParseException("Missing DO", tokens.getErrorIndex());
        }
        while (!match("END") && !peek("ELSE")) {
            Ast.Stmt statement = parseStatement();
            thenStatements.add(statement);
        }
        if (match("ELSE")) {
            while (!peek("END")) {
                Ast.Stmt statement = parseStatement();
                elseStatements.add(statement);
            }
        }
        return new Ast.Stmt.If(expression, thenStatements, elseStatements);
    }

    /**
     * Parses a for statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a for statement, aka
     * {@code FOR}.
     */
    public Ast.Stmt.For parseForStatement() throws ParseException {
        String name;
        Ast.Expr value;
        List<Ast.Stmt> statements = new ArrayList<>();
        // For Condition
        if (!peek(Token.Type.IDENTIFIER)) {
            throw new ParseException("Expected an identifier", tokens.getErrorIndex());
        }
        name = tokens.get(0).getLiteral();
        tokens.advance();
        if (!match("IN")) {
            throw new ParseException("Missing IN", tokens.getErrorIndex());
        }
        // In Condition
        value = parseExpression();
        // Statements
        if (!match("DO")) {
            throw new ParseException("Missing DO", tokens.getErrorIndex());
        }
        while (!match("END")) {
            Ast.Stmt statement = parseStatement();
            statements.add(statement);
        }
        return new Ast.Stmt.For(name, value, statements);
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Stmt.While parseWhileStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        List<Ast.Stmt> statements = new ArrayList<>();
        if (!match("DO")) {
            throw new ParseException("Missing DO", tokens.getErrorIndex());
        }
        while (!match("END")) {
            Ast.Stmt statement = parseStatement();
            statements.add(statement);
        }
        return new Ast.Stmt.While(expression, statements);
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Stmt.Return parseReturnStatement() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (!match(";")) {
            throw new ParseException("Missing semicolon after return statement", tokens.getErrorIndex());
        }
        return new Ast.Stmt.Return(expression);
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expr parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expr parseLogicalExpression() throws ParseException {
        Ast.Expr logicExpr = parseEqualityExpression();
        if (peek("AND") || peek("OR"))  {
            String operator;
            Ast.Expr rightExpr;
            while (peek("AND") || peek("OR")) {
                operator = tokens.get(0).getLiteral();
                tokens.advance();
                rightExpr = parseEqualityExpression();
                logicExpr = new Ast.Expr.Binary(operator, logicExpr, rightExpr);
            }
        }
        return logicExpr;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expr parseEqualityExpression() throws ParseException {
        Ast.Expr addExpr = parseAdditiveExpression();
        if (peek(">") || peek("<") || peek("<=") || peek(">=")
                || peek("==") || peek("!=")) {
            String operator;
            Ast.Expr rightExpr;
            while (peek(">") || peek("<") || peek("<=") || peek(">=")
                    || peek("==") || peek("!=")) {
                operator = tokens.get(0).getLiteral();
                tokens.advance();
                rightExpr = parseAdditiveExpression();
                addExpr = new Ast.Expr.Binary(operator, addExpr, rightExpr);
            }
        }
        return addExpr;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expr parseAdditiveExpression() throws ParseException {
        Ast.Expr multExpr = parseMultiplicativeExpression();
        if (peek("+") || peek("-")) {
            String operator;
            Ast.Expr rightExpr;
            while (peek("+") || peek("-")) {
                operator = tokens.get(0).getLiteral();
                tokens.advance();
                rightExpr = parseMultiplicativeExpression();
                multExpr = new Ast.Expr.Binary(operator, multExpr, rightExpr);
            }
        }
        return multExpr;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expr parseMultiplicativeExpression() throws ParseException {
        Ast.Expr secondaryExpr = parseSecondaryExpression();
        if (peek("*") || peek("/")) {
            String operator;
            Ast.Expr rightExpr;
            while (peek("*") || peek("/")) {
                operator = tokens.get(0).getLiteral();
                tokens.advance();
                rightExpr = parseSecondaryExpression();
                secondaryExpr = new Ast.Expr.Binary(operator, secondaryExpr, rightExpr);
            }
        }
        return secondaryExpr;
    }

    /**
     * Parses the {@code secondary-expression} rule.
     */
    public Ast.Expr parseSecondaryExpression() throws ParseException {
        Ast.Expr primaryExpr = parsePrimaryExpression();
        // In a class
        if (peek(".")) {
            Ast.Expr.Access acc;
            Ast.Expr.Function func;
            while (match(".")) {
                if (!peek(Token.Type.IDENTIFIER)) {
                    throw new ParseException("Invalid function/access name", tokens.getErrorIndex());
                }
                if (peek(Token.Type.IDENTIFIER, "(")) {
                    func = parseFunction();
                    primaryExpr = new Ast.Expr.Function(Optional.of(primaryExpr), func.getName(), func.getArguments());
                }
                else {
                    acc = parseAccess();
                    primaryExpr = new Ast.Expr.Access(Optional.of(primaryExpr), acc.getName());
                }
            }
        }
        return primaryExpr;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expr parsePrimaryExpression() throws ParseException {
        // NIL
        if (peek("NIL")) {
            tokens.advance();
            return new Ast.Expr.Literal(null);
        }
        // Literal - Boolean
        else if (peek("TRUE") || peek("FALSE")) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            return new Ast.Expr.Literal(Boolean.valueOf(literal));
        }
        // Literal - Integer
        else if (peek(Token.Type.INTEGER)) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            return new Ast.Expr.Literal((new BigInteger(literal)));
        }
        // Literal - Decimal
        else if (peek(Token.Type.DECIMAL)) {
            String literal = tokens.get(0).getLiteral();
            tokens.advance();
            return new Ast.Expr.Literal((new BigDecimal(literal)));
        }
        // Literal - Character
        else if (peek(Token.Type.CHARACTER)) {
            String charString = tokens.get(0).getLiteral();
            charString = charString.substring(1, charString.length() - 1);
            charString = replaceEscapes(charString);
            tokens.advance();
            return new Ast.Expr.Literal(charString.charAt(0));
        }
        // Literal - String
        else if (peek(Token.Type.STRING)) {
            String strString = tokens.get(0).getLiteral();
            strString = strString.substring(1, strString.length() - 1);
            strString = replaceEscapes(strString);
            tokens.advance();
            return new Ast.Expr.Literal(strString);
        }
        // Group
        else if (match("(")) {
            return parseGroup();
        }
        // Access or Function
        else if (peek(Token.Type.IDENTIFIER)) {
            if (peek(Token.Type.IDENTIFIER, "(")) {
                return parseFunction();
            }
            return parseAccess();
        }
        throw new ParseException("No valid expression was found", tokens.getErrorIndex());
    }

    public Ast.Expr.Access parseAccess() throws ParseException {
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        return new Ast.Expr.Access(Optional.empty(), name);
    }

    public Ast.Expr.Function parseFunction() throws ParseException {
        String name = tokens.get(0).getLiteral();
        tokens.advance();
        tokens.advance();
        List<Ast.Expr> arguments = new ArrayList<>();
        if (!peek(")") && tokens.has(1)) {
            arguments.add(parseExpression());
        }
        while (match(",")) {
            arguments.add(parseExpression());
        }
        if (!match(")")) {
            throw new ParseException("Missing ending parenthesis", tokens.getErrorIndex());
        }
        return new Ast.Expr.Function(Optional.empty(), name, arguments);
    }

    public Ast.Expr.Group parseGroup() throws ParseException {
        Ast.Expr expression = parseExpression();
        if (!match(")")) {
            throw new ParseException("Missing ending parenthesis", tokens.getErrorIndex());
        }
        return new Ast.Expr.Group(expression);
    }

    public String replaceEscapes(String str) {
        str = str.replace("\\\b", "\b");
        str = str.replace("\\n", "\n");
        str = str.replace("\\\r", "\r");
        str = str.replace("\\\t", "\t");
        str = str.replace("\\\'", "\'");
        str = str.replace("\\\"", "\"");
        str = str.replace("\\\\", "\\");
        return str;
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            }
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            }
            else if (patterns[i] instanceof  String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            }
            else {
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

        public int getErrorIndex() {
            if (has(0)) {
                return get(0).getIndex();
            }
            return get(-1).getIndex() + get(-1).getLiteral().length();
        }

    }

}