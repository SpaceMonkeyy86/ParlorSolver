package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// A part of the statement used in parsing. Can be either
// a single word or symbol which has not been matched or
// a group of words parsed into a formula.
public class Token {
    // The type of phrase this token represents, or empty if just a single word.
    private final String identifier;

    // The originating word, or empty if this is a phrase.
    private final String source;

    // The parsed formula, if this is a phrase.
    private final Formula formula;

    public Token(String source) {
        if (source.startsWith(":")) {
            identifier = source.substring(1);
        } else {
            identifier = "";
        }
        this.source = source;
        formula = null;
    }

    public Token(String identifier, String source, Formula formula) {
        this.identifier = identifier;
        this.source = source;
        this.formula = formula;
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getSource() {
        return source;
    }

    public Formula getFormula() {
        return formula;
    }

    public static List<Token> tokenize(String statement) {
        // Generally, each word should be its own token.
        // The period is a separate token.
        // Possessives ('s) should be separate tokens from their root words.
        // Quotes surrounding words are removed.

        Pattern pattern = Pattern.compile("^(\"\\w+\"|.\\w*) ?(.*)$");
        List<Token> tokens = new ArrayList<>();

        while (!statement.isEmpty()) {
            Matcher matcher = pattern.matcher(statement);
            if (!matcher.matches()) {
                throw new RuntimeException("Malformed statement");
            }

            String word = matcher.group(1);
            if (word.startsWith("\"")) {
                word = word.substring(1, word.length() - 1);
            }

            tokens.add(new Token(word));
            statement = matcher.group(2);
        }

        return tokens;
    }

    // The reverse of tokenize()
    public static String stringify(List<Token> tokens) {
        StringBuilder builder = new StringBuilder();
        for (Token token : tokens) {
            if (!builder.isEmpty() && (!token.identifier.isEmpty()
                || Character.isLetterOrDigit(token.source.charAt(0)))) {
                builder.append(" ");
            }
            builder.append(token.toString());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        if (identifier.isEmpty()) {
            return source;
        }
        if (identifier.equals("word") && !source.startsWith(":")) {
            return "word{\"" + source + "\"}";
        }
        if (formula == null) {
            return ":" + identifier;
        }
        return identifier + "{" + formula + "}";
    }
}
