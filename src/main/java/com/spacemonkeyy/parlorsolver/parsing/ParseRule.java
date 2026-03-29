package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

import java.util.List;
import java.util.Objects;

public class ParseRule {
    private final List<Token> pattern;
    private final boolean partial;
    private final String identifier;
    private final ParseAction action;

    public ParseRule(String pattern, boolean partial, String identifier, ParseAction action) {
        this.pattern = Token.tokenize(pattern);
        this.partial = partial;
        this.identifier = identifier;
        this.action = action;
    }

    public boolean tryMatch(List<Token> tokens, ParseContext context) {
        // Partial rules can match any part of the token string,
        // Full rules must match the entire string
        if (!partial && tokens.size() != pattern.size()) {
            return false;
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
                // Set bindings for use in the parse action
                context.clearBindings();
                for (int j = 0; j < pattern.size(); j++) {
                    if (!pattern.get(j).getIdentifier().isEmpty()) {
                        context.addBinding(pattern.get(j).getIdentifier(), tokens.get(i + j).getFormula());
                    }
                }

                Formula formula = action.apply(context);
                Token token = new Token(identifier, formula);

                // Substitute resulting token for matched tokens
                for (int j = 0; j < pattern.size(); j++) {
                    tokens.remove(i);
                }
                tokens.add(i, token);

                return true;
            }
        }

        return false;
    }

    public boolean tokensMatch(Token pattern, Token target) {
        if (pattern.getIdentifier().isEmpty()) {
            return Objects.equals(pattern.getSource(), target.getSource());
        } else {
            return Objects.equals(target.getIdentifier(), pattern.getIdentifier());
        }
    }

    @Override
    public String toString() {
        return Token.stringify(pattern) + " -> " + identifier;
    }
}
