package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.ConstantFormula;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.FunctionFormula;
import com.spacemonkeyy.parlorsolver.formula.Functions;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Parser {
    static List<ParseRule> rules = new ArrayList<>();

    static {
        // Trivial statements
        addRule("YOU ARE IN THE PARLOR.", ctx -> {
            // True; the game takes place in the parlor.
            return new ConstantFormula(new Value(true));
        });

        // Colors
        addRule("BLUE", "color", ctx -> {
            return new ConstantFormula(new Value(BoxColor.BLUE));
        });

        // Boxes
        addRule("THE :color BOX", "box", ctx -> {
            return new FunctionFormula(Functions.BOX_BY_COLOR, ctx.get("color"));
        });

        // Simple statements
        addRule(":box IS TRUE.", ctx -> {
            return new FunctionFormula(Functions.BOX_IS_TRUE, ctx.get("box"));
        });
    }

    private static void addRule(String pattern, ParseAction action) {
        rules.add(new ParseRule(pattern, false, "sentence", action));
    }

    private static void addRule(String pattern, String identifier, ParseAction action) {
        rules.add(new ParseRule(pattern, true, identifier, action));
    }

    public static Formula parse(String statement) {
        ParseContext context = new ParseContext(statement);
        List<Token> tokens = Token.tokenize(statement);

        while (true) {
            boolean matched = false;
            for (ParseRule rule : rules) {
                if (rule.tryMatch(tokens, context)) {
                    matched = true;
                    break;
                }
            }

            // Failed to parse
            if (!matched) {
                break;
            }

            if (tokens.size() == 1) {
                Token token = tokens.getFirst();
                if (!Objects.equals(token.getIdentifier(), "sentence")) {
                    // Input statement was just a sentence fragment
                    System.err.println("Input must be a full sentence");
                    return null;
                }
                return token.getFormula();
            }
        }

        System.err.println("Unable to parse");
        return null;
    }
}
