package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.ConstantFormula;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.FunctionFormula;
import com.spacemonkeyy.parlorsolver.formula.Functions;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
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
        addRule("THERE IS A SECOND WIND-UP KEY IN THIS ROOM.", ctx -> {
            // False; there is only ever one wind-up key in the room.
            return new ConstantFormula(new Value(false));
        });

        // Colors
        addRule("BLUE", "color", ctx -> {
            return new ConstantFormula(new Value(BoxColor.BLUE));
        });
        addRule("WHITE", "color", ctx -> {
            return new ConstantFormula(new Value(BoxColor.WHITE));
        });
        addRule("BLACK", "color", ctx -> {
            return new ConstantFormula(new Value(BoxColor.BLACK));
        });

        // Boxes
        addRule("THE :color BOX", "box", ctx -> {
            return new FunctionFormula(Functions.BOX_BY_COLOR, ctx.get("color"));
        });
        addRule("THE MIDDLE BOX", "box", ctx -> {
            // The order of the boxes is blue, white, black
            return new ConstantFormula(new Value(new Box(BoxColor.WHITE)));
        });
        addRule("THIS BOX", "box", ctx -> {
            return new ConstantFormula(new Value(new Box(ctx.getCurrentBox())));
        });

        // Simple statements
        addRule(":box IS TRUE.", ctx -> {
            return new FunctionFormula(Functions.BOX_IS_TRUE, ctx.get("box"));
        });
        addRule("THE STATEMENT ON :box IS TRUE.", ctx -> {
            // TODO: Assert the box only has one statement
            return new FunctionFormula(Functions.BOX_IS_TRUE, ctx.get("box"));
        });
        addRule(":box IS :box.", ctx -> {
            return new FunctionFormula(Functions.BOX_EQUALS, ctx.get("box", 1), ctx.get("box", 2));
        });

        // Location of gems
        addRule(":box CONTAINS THE GEMS.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"));
        });
        addRule(":box CONTAINS GEMS.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"));
        });
        addRule(":box HAS THE GEMS.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"));
        });
        addRule("THE GEMS ARE IN :box.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"));
        });
        addRule(":box IS EMPTY.", ctx -> {
            return new FunctionFormula(Functions.NOT, new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box")));
        });
        addRule("THE GEMS ARE NOT IN :box.", ctx -> {
            return new FunctionFormula(Functions.NOT, new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box")));
        });
    }

    private static void addRule(String pattern, ParseAction action) {
        rules.add(new ParseRule(pattern, false, "sentence", action));
    }

    private static void addRule(String pattern, String identifier, ParseAction action) {
        rules.add(new ParseRule(pattern, true, identifier, action));
    }

    public static List<Formula> parse(PuzzleInput input) {
        ParseContext context = new ParseContext(input);
        List<Formula> result = new ArrayList<>();

        for (BoxColor color : BoxColor.values()) {
            List<String> statements = input.byColor(color);
            for (int i = 0; i < statements.size(); i++) {
                context.setCurrent(color, i);
                result.add(parseStatement(context.getCurrentStatement(), context));
            }
        }

        return result;
    }

    private static Formula parseStatement(String statement, ParseContext context) {
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
                    return null;
                }
                return token.getFormula();
            }
        }

        return null;
    }
}
