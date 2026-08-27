package org.dflib.ql.antlr4;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ExpParserUtilsTest {

    /**
     * Returns a token stream positioned at the token with the given index, which is how the parser sees it when it
     * predicts a call site: the name of the call is the next token.
     */
    private static CommonTokenStream atToken(String text, int index) {
        CommonTokenStream tokens = new CommonTokenStream(new ExpStrictLexer(CharStreams.fromString(text)));
        tokens.fill();
        tokens.seek(index);
        return tokens;
    }

    private static String tokenAfterCall(String text, int nameIndex) {
        return ExpParserUtils.tokenAfterCall(atToken(text, nameIndex)).getText();
    }

    private static boolean typedContinuation(String text, int nameIndex) {
        return ExpParserUtils.continuation(atToken(text, nameIndex)) == ExpParserUtils.CONTINUATION_TYPED;
    }

    private static boolean comparisonContinuation(String text, int nameIndex) {
        return ExpParserUtils.continuation(atToken(text, nameIndex)) == ExpParserUtils.CONTINUATION_COMPARISON;
    }

    @Test
    public void tokenAfterCall_Bare() {
        assertEquals("<EOF>", tokenAfterCall("min(x)", 0));
        assertEquals("+", tokenAfterCall("min(x) + 1", 0));
        assertEquals(",", tokenAfterCall("min(x), 1", 0));
    }

    @Test
    public void tokenAfterCall_Nested() {
        // the parenthesis before the call opens the argument list of "foo", so it is not skipped
        assertEquals(")", tokenAfterCall("foo(min(x)) + 1", 2));
        assertEquals(")", tokenAfterCall("foo((min(x))) + 1", 3));
        assertEquals(")", tokenAfterCall("(foo(min(x)))", 3));
        assertEquals(",", tokenAfterCall("foo(min(x), 1)", 2));
    }

    @Test
    public void tokenAfterCall_Grouped() {
        assertEquals("+", tokenAfterCall("(min(x)) + 1", 1));
        assertEquals("+", tokenAfterCall("((min(x))) + 1", 2));
        assertEquals("*", tokenAfterCall("1 + (min(x)) * 2", 3));
        assertEquals("<EOF>", tokenAfterCall("(min(x))", 1));
    }

    @Test
    public void tokenAfterCall_NotACall() {
        // an identifier is also a column reference, and the scan must terminate on anything it sees
        assertEquals("<EOF>", tokenAfterCall("min + 1", 0));
        assertEquals("<EOF>", tokenAfterCall("foo(min)", 2));
    }

    @Test
    public void typedContinuation_Operator() {
        assertTrue(typedContinuation("min(x) + 1", 0));
        assertTrue(typedContinuation("min(x) and y", 0));
        assertTrue(typedContinuation("(min(x)) * 2", 1));
        assertFalse(typedContinuation("min(x)", 0));
        assertFalse(typedContinuation("foo(min(x)) + 1", 2));
    }

    @Test
    public void typedContinuation_Comparison() {
        // a comparison of a call whose type is not known from its name is built by "fnRelation", not by a typed
        // relation rule, so it is not a typed continuation
        assertFalse(typedContinuation("min(x) > 5", 0));
        assertFalse(typedContinuation("min(x) between 1 and 5", 0));
        assertFalse(typedContinuation("min(x) in (1, 2)", 0));
        assertFalse(typedContinuation("min(x) = 'a'", 0));
    }

    @Test
    public void comparisonContinuation() {
        assertTrue(comparisonContinuation("min(x) > 5", 0));
        assertTrue(comparisonContinuation("min(x) != 5", 0));
        assertTrue(comparisonContinuation("min(x) between 1 and 5", 0));
        assertTrue(comparisonContinuation("min(x) not in (1, 2)", 0));

        assertFalse(comparisonContinuation("min(x)", 0));
        assertFalse(comparisonContinuation("min(x) + 1", 0));
        assertFalse(comparisonContinuation("foo(min(x)) > 5", 2));

        // "fnRelation" matches a call at its left edge, so a comparison of a parenthesized expression is none of
        // its business and the call in it stays free to be typed
        assertFalse(comparisonContinuation("(min(x)) > 5", 1));
    }

    @Test
    public void typedContinuation_PrefixOperator() {
        assertTrue(typedContinuation("not min(x)", 1));
        assertTrue(typedContinuation("not (min(x))", 2));
        assertTrue(typedContinuation("- min(x)", 1));
        assertTrue(typedContinuation("1 - min(x)", 2));

        // "not" is not the name of a call, so the parenthesis after it opens a group
        assertFalse(typedContinuation("not foo(min(x))", 3));
    }

    @SuppressWarnings("UnnecessaryUnicodeEscape")
    @Test
    public void unescapeIdentifier() {
        assertEquals("abc", ExpParserUtils.unescapeIdentifier("abc"));
        assertEquals("a c", ExpParserUtils.unescapeIdentifier("a c"));
        assertEquals("a`c", ExpParserUtils.unescapeIdentifier("a``c"));
        assertEquals("a'c", ExpParserUtils.unescapeIdentifier("a'c"));
        assertEquals("a\\c", ExpParserUtils.unescapeIdentifier("a\\c"));
        assertEquals("a\"c", ExpParserUtils.unescapeIdentifier("a\"c"));
        assertEquals("Abc", ExpParserUtils.unescapeIdentifier("\u0041bc"));
    }
}
