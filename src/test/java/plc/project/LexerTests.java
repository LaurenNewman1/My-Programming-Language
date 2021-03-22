package plc.project;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class LexerTests {

    @ParameterizedTest
    @MethodSource
    void testIdentifier(String test, String input, boolean success) {
        test(input, Token.Type.IDENTIFIER, success);
    }

    private static Stream<Arguments> testIdentifier() {
        return Stream.of(
                Arguments.of("Alphabetic", "getName", true),
                Arguments.of("Alphanumeric", "thelegend27", true),
                Arguments.of("Leading Hyphen", "-five", false),
                Arguments.of("Leading Digit", "1fish2fish3fishbluefish", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testInteger(String test, String input, boolean success) {
        test(input, Token.Type.INTEGER, success);
    }

    private static Stream<Arguments> testInteger() {
        return Stream.of(
                Arguments.of("Single Digit", "1", true),
                Arguments.of("Multiple Digits", "11", true),
                Arguments.of("With +", "+1", true),
                Arguments.of("With -", "-1", true),

                Arguments.of("Decimal", "123.456", false),
                Arguments.of("Signed Decimal", "-1.0", false),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Letter", "a", false),
                Arguments.of("+ at the end", "1+", false),
                Arguments.of("- at the end", "1-", false),
                Arguments.of("+ and -", "+-1", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testDecimal(String test, String input, boolean success) {
        test(input, Token.Type.DECIMAL, success);
    }

    private static Stream<Arguments> testDecimal() {
        return Stream.of(
                Arguments.of("Multiple Digits", "123.456", true),
                Arguments.of("Negative Decimal", "-1.0", true),
                Arguments.of("Decimal Point 1 and 1", "1.1", true),
                Arguments.of("Decimal Point 1 and 2", "1.11", true),
                Arguments.of("Decimal Point 2 and 1", "11.1", true),
                Arguments.of("With + and decimal", "+1.1", true),
                Arguments.of("Trailing Decimal", "1.", false),
                Arguments.of("Leading Decimal", ".5", false),
                Arguments.of("Integer", "1", false),
                Arguments.of("Decimal in front", ".1", false),
                Arguments.of("Letter", "a", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCharacter(String test, String input, boolean success) {
        test(input, Token.Type.CHARACTER, success);
    }

    private static Stream<Arguments> testCharacter() {
        return Stream.of(
                Arguments.of("Alphabetic", "\'c\'", true),
                Arguments.of("Newline Escape", "\'\\n\'", true),
                Arguments.of("Empty", "\'\'", false),
                Arguments.of("Multiple", "\'abc\'", false),
                Arguments.of("Unterminated", "\'c", false),
                Arguments.of("Unstarted", "c\'", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testString(String test, String input, boolean success) {
        test(input, Token.Type.STRING, success);
    }

    private static Stream<Arguments> testString() {
        return Stream.of(
                Arguments.of("Empty", "\"\"", true),
                Arguments.of("Single", "\"a\"", true),
                Arguments.of("Number", "\"1\"", true),
                Arguments.of("Alphabetic", "\"abc\"", true),
                Arguments.of("Vertical line", "\"Hello\\b\"", true),
                Arguments.of("New line", "\"Hello\\n\"", true),
                Arguments.of("Vertical line", "\"Hello\\r\"", true),
                Arguments.of("Tab", "\"Hello\\t\"", true),
                Arguments.of("Apostrophe", "\"Hello\\'\"", true),
                Arguments.of("Back Slash", "\"Hello\\\\\"", true),
                Arguments.of("Mid Word Escape", "\"Hel\\\"lo\"", true),
                Arguments.of("2 Escapes", "\"Hel\\\"lo\\\"\"", true),
                Arguments.of("Number Strings", "\"12345\"", true),
                Arguments.of("Only Escape", "\"\\\"\"", true),
                Arguments.of("Symbol", "\"Hello!\"", true),

                Arguments.of("Newline Escape", "\"Hello,\\nWorld\"", true),
                Arguments.of("Unterminated", "\"unterminated", false),
                Arguments.of("Invalid Escape", "\"invalid\\escape\"", false),
                Arguments.of("Missing Quote", "\"", false),
                Arguments.of("Quotes Not On Outside", "\"Hel\"lo", false),
                Arguments.of("Non-string", "12345", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testOperator(String test, String input, boolean success) {
        //this test requires our lex() method, since that's where whitespace is handled.
        test(input, Arrays.asList(new Token(Token.Type.OPERATOR, input, 0)), success);
    }

    private static Stream<Arguments> testOperator() {
        return Stream.of(
                Arguments.of("Character", "(", true),
                Arguments.of("Comparison", "<=", true),
                Arguments.of("Equals", "==", true),
                Arguments.of("Plus", "+", true),
                Arguments.of("Space", " ", false),
                Arguments.of("Tab", "\t", false),
                Arguments.of("Regular char", "a", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    void testExamples(String test, String input, List<Token> expected) {
        test(input, expected, true);
    }

    private static Stream<Arguments> testExamples() {
        return Stream.of(
                Arguments.of("Example 1", "LET x = 5;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "LET", 0),
                        new Token(Token.Type.IDENTIFIER, "x", 4),
                        new Token(Token.Type.OPERATOR, "=", 6),
                        new Token(Token.Type.INTEGER, "5", 8),
                        new Token(Token.Type.OPERATOR, ";", 9)
                )),
                Arguments.of("Example 2", "print(\"Hello, World!\");", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "print", 0),
                        new Token(Token.Type.OPERATOR, "(", 5),
                        new Token(Token.Type.STRING, "\"Hello, World!\"", 6),
                        new Token(Token.Type.OPERATOR, ")", 21),
                        new Token(Token.Type.OPERATOR, ";", 22)
                )),
                Arguments.of("Example 3", "num1 = num2 * 15.1;", Arrays.asList(
                        new Token(Token.Type.IDENTIFIER, "num1", 0),
                        new Token(Token.Type.OPERATOR, "=", 5),
                        new Token(Token.Type.IDENTIFIER, "num2", 7),
                        new Token(Token.Type.OPERATOR, "*", 12),
                        new Token(Token.Type.DECIMAL, "15.1", 14),
                        new Token(Token.Type.OPERATOR, ";", 18)
                ))
        );
    }

    @Test
    void testException() {
        // Strings
        ParseException exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"unterminated").lex());
        Assertions.assertEquals(13, exception.getIndex());

        // Characters
        exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\'\'").lex());
        Assertions.assertEquals(1, exception.getIndex());
        exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\'ab\'").lex());
        Assertions.assertEquals(2, exception.getIndex());

        // Escape
        exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("\"esc\\\"").lex());
        Assertions.assertEquals(6, exception.getIndex());

        // Operator
        exception = Assertions.assertThrows(ParseException.class,
                () -> new Lexer("").lex());
        Assertions.assertEquals(0, exception.getIndex());
    }

    /**
     * Tests that lexing the input through {@link Lexer#lexToken()} produces a
     * single token with the expected type and literal matching the input.
     */
    private static void test(String input, Token.Type expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            } else {
                Assertions.assertNotEquals(new Token(expected, input, 0), new Lexer(input).lexToken());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

    /**
     * Tests that lexing the input through {@link Lexer#lex()} matches the
     * expected token list.
     */
    private static void test(String input, List<Token> expected, boolean success) {
        try {
            if (success) {
                Assertions.assertEquals(expected, new Lexer(input).lex());
            } else {
                Assertions.assertNotEquals(expected, new Lexer(input).lex());
            }
        } catch (ParseException e) {
            Assertions.assertFalse(success, e.getMessage());
        }
    }

}
