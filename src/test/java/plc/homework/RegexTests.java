package plc.homework;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains JUnit tests for {@link Regex}. Test structure for steps 1 & 2 are
 * provided, you must create this yourself for step 3.
 *
 * To run tests, either click the run icon on the left margin, which can be used
 * to run all tests or only a specific test. You should make sure your tests are
 * run through IntelliJ (File > Settings > Build, Execution, Deployment > Build
 * Tools > Gradle > Run tests using <em>IntelliJ IDEA</em>). This ensures the
 * name and inputs for the tests are displayed correctly in the run window.
 */
public class RegexTests {

    /**
     * This is a parameterized test for the {@link Regex#EMAIL} regex. The
     * {@link ParameterizedTest} annotation defines this method as a
     * parameterized test, and {@link MethodSource} tells JUnit to look for the
     * static method {@link #testEmailRegex()}.
     *
     * For personal preference, I include a test name as the first parameter
     * which describes what that test should be testing - this is visible in
     * IntelliJ when running the tests (see above note if not working).
     */
    @ParameterizedTest
    @MethodSource
    public void testEmailRegex(String test, String input, boolean success) {
        test(input, Regex.EMAIL, success);
    }

    /**
     * This is the factory method providing test cases for the parameterized
     * test above - note that it is static, takes no arguments, and has the same
     * name as the test. The {@link Arguments} object contains the arguments for
     * each test to be passed to the function above.
     */
    public static Stream<Arguments> testEmailRegex() {
        return Stream.of(
                Arguments.of("Alphanumeric", "thelegend27@gmail.com", true),
                Arguments.of("UF Domain", "otherdomain@ufl.edu", true),
                Arguments.of("Missing Domain Dot", "missingdot@gmailcom", false),
                Arguments.of("Symbols", "symbols#$%@gmail.com", false),
                // TODO
                Arguments.of("Base", "johndoe@gmail.com", true),
                Arguments.of("Number", "johndoe1@gmail.com", true),
                Arguments.of("Period", "johndoe.@gmail.com", true),
                Arguments.of("Dash", "johndoe-@gmail.com", true),
                Arguments.of("Capital", "Johndoe@gmail.com", true),
                Arguments.of("Underscore", "johndoe_@gmail.com", true),
                Arguments.of("Mid: Length 0", "johndoe@.com", true),
                Arguments.of("Mid: Length 1", "johndoe@g.com", true),
                Arguments.of("End: Length 2", "johndoe@gmail.co", true),

                Arguments.of("Beg: Unknown Symbol", "johndoe#@gmail.com", false),
                Arguments.of("Name Length 0", "@gmail.com", false),
                Arguments.of("Missing @", "johndoegmail.com", false),
                Arguments.of("Mid: Unknown Symbol", "johndoe@gmail#.com", false),
                Arguments.of("Missing Dot", "johndoe@gmailcom", false),
                Arguments.of("End: Length <2", "johndoe@gmail.c", false),
                Arguments.of("End: Length >3", "johndoe@gmail.comm", false),
                Arguments.of("End: Capital", "johndoe@gmail.Com", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testEvenStringsRegex(String test, String input, boolean success) {
        test(input, Regex.EVEN_STRINGS, success);
    }

    public static Stream<Arguments> testEvenStringsRegex() {
        return Stream.of(
                //what has ten letters and starts with gas?
                Arguments.of("10 Characters", "automobile", true),
                Arguments.of("14 Characters", "i<3pancakes10!", true),
                Arguments.of("6 Characters", "6chars", false),
                Arguments.of("13 Characters", "i<3pancakes9!", false),
                // TODO
                Arguments.of("10 Characters", "regexregex", true),
                Arguments.of("14 Characters", "regexregexrege", true),
                Arguments.of("20 Characters", "regexregexregexregex", true),

                Arguments.of("8 Characters", "regexrege", false),
                Arguments.of("15 Characters", "regexregexregex", false),
                Arguments.of("22 Characters", "regexregexregexregexr", false)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testIntegerListRegex(String test, String input, boolean success) {
        test(input, Regex.INTEGER_LIST, success);
    }

    public static Stream<Arguments> testIntegerListRegex() {
        return Stream.of(
                Arguments.of("Single Element", "[1]", true),
                Arguments.of("Multiple Elements", "[1,2,3]", true),
                Arguments.of("Missing Brackets", "1,2,3", false),
                Arguments.of("Missing Commas", "[1 2 3]", false),
                // TODO
                Arguments.of("Empty", "[]", true),
                Arguments.of("Multiple Digit Entry", "[11]", true),
                Arguments.of("With Space Multiple", "[1, 2]", true),
                Arguments.of("Many Elements", "[1, 2, 3, 4, 5]", true),
                Arguments.of("Inconsistent Spacing", "[1,2, 3,4,5]", true),

                Arguments.of("1 Bracket", "[", false),
                Arguments.of("Trailing Comma", "[1,]", false),
                Arguments.of("Leading Commas", "[,1]", false),
                Arguments.of("No Brackets", "1", false),
                Arguments.of("Empty String", "", false),
                Arguments.of("No commas", "[1 2]", false),
                Arguments.of("Extra comma", "[1,,2]", false)
        );
    }

    //TODO
    @ParameterizedTest
    @MethodSource
    public void testNumberRegex(String test, String input, boolean success) {
        test(input, Regex.NUMBER, success);
    }

    //TODO
    public static Stream<Arguments> testNumberRegex() {
        return Stream.of(
                Arguments.of("One Digit", "1", true),
                Arguments.of("Multiple Digits", "11", true),
                Arguments.of("Decimal Point 1 and 1", "1.1", true),
                Arguments.of("Decimal Point 1 and 2", "1.11", true),
                Arguments.of("Decimal Point 2 and 1", "11.1", true),
                Arguments.of("With +", "+1", true),
                Arguments.of("With -", "-1", true),
                Arguments.of("With + and decimal", "+1.1", true),

                Arguments.of("Decimal in front", ".1", false),
                Arguments.of("Decimal in back", "1.", false),
                Arguments.of("Letter", "a", false),
                Arguments.of("+ at the end", "1+", false),
                Arguments.of("- at the end", "1-", false),
                Arguments.of("+ and -", "+-1", false)
        );
    }

    //TODO
    @ParameterizedTest
    @MethodSource
    public void testStringRegex(String test, String input, boolean success) {
        test(input, Regex.STRING, success);
    }

    //TODO
    public static Stream<Arguments> testStringRegex() {
        return Stream.of(
            Arguments.of("Empty Quotes", "\"\"", true),
            Arguments.of("Word", "\"Hello\"", true),
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

            Arguments.of("Missing Quote", "\"", false),
            Arguments.of("Quotes Not On Outside", "\"Hel\"lo", false),
            Arguments.of("Unterminated", "\"Hello", false),
            Arguments.of("Non-string", "12345", false),
            Arguments.of("Invalid Escape", "Hel\\lo", false)
        );
    }

    /**
     * Asserts that the input matches the given pattern. This method doesn't do
     * much now, but you will see this concept in future assignments.
     */
    private static void test(String input, Pattern pattern, boolean success) {
        Assertions.assertEquals(success, pattern.matcher(input).matches());
    }

}
