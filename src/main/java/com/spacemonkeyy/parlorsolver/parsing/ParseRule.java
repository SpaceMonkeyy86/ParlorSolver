package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

import java.util.List;
import java.util.Objects;

public class ParseRule {
    private final List<Token> pattern;
    private final String identifier;
    private final ParseAction action;

    public ParseRule(String pattern, String identifier, ParseAction action) {
        this.pattern = Token.tokenize(pattern);
        this.identifier = identifier;
        this.action = action;
    }

    public List<Token> getPattern() {
        return pattern;
    }

    public String getIdentifier() {
        return identifier;
    }

    public boolean isPartial() {
        return !identifier.equals("sentence");
    }

    public int matches(List<Token> tokens) {
        // Partial rules can match any part of the token string,
        // Full rules must match the entire string
        if (!isPartial() && tokens.size() != pattern.size()) {
            return -1;
        }

        for (int i = 0; i < tokens.size() - pattern.size() + 1; i++) {
            boolean matches = true;
            for (int j = 0; j < pattern.size(); j++) {
                if (!tokensMatch(pattern.get(j), tokens.get(i + j))) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                return i;
            }
        }

        return -1;
    }

    public boolean tryMatch(List<Token> tokens, ParseContext context) {
        int i = matches(tokens);
        if (i == -1) {
            return false;
        }

        // Set bindings for use in the parse action
        context.clearBindings();
        for (int j = 0; j < pattern.size(); j++) {
            if (!pattern.get(j).getIdentifier().isEmpty()) {
                context.addBinding(pattern.get(j).getIdentifier(), tokens.get(i + j));
            }
        }

        Formula formula = action.apply(context);
        String source = Token.stringify(tokens.subList(i, i + pattern.size()));
        Token token = new Token(identifier, source, formula);

        // Substitute resulting token for matched tokens
        for (int j = 0; j < pattern.size(); j++) {
            tokens.remove(i);
        }
        tokens.add(i, token);

        return true;
    }

    private boolean tokensMatch(Token pattern, Token target) {
        if (pattern.getIdentifier().isEmpty()) {
            return target.getIdentifier().isEmpty()
                && Objects.equals(pattern.getSource(), target.getSource());
        } else if (pattern.getIdentifier().equals("word")) {
            // Matches any single word in the source string, even if already matched
            return Token.tokenize(target.getSource()).size() == 1;
        } else {
            return Objects.equals(target.getIdentifier(), pattern.getIdentifier());
        }
    }

    @Override
    public String toString() {
        return Token.stringify(pattern) + " -> " + identifier;
    }
}
