package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.ConstantFormula;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.FunctionFormula;
import com.spacemonkeyy.parlorsolver.formula.Functions;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Value;

import java.util.ArrayList;
import java.util.List;

public class Rules {
    public static List<ParseRule> rules = new ArrayList<>();
    private static ParseAction lastAction;

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

        addRule("THE GEMS ARE ON THE DESK.", ctx -> {
            // False; the gems are always in a box.
            return new ConstantFormula(new Value(false));
        });
        addAlias("THE GEMS ARE ON THE FLOOR.");
        addAlias("THE GEMS ARE ON THE TABLE BEHIND YOU.");

        addRule("THERE ARE :number BOXES IN THIS ROOM.", ctx -> {
            // There are three boxes in the room.
            return new FunctionFormula(Functions.NUMBER_EQUALS,
                ctx.get("number"),
                new ConstantFormula(new Value(3))
            );
        });
        addRule("THERE IS ONLY ONE BOX IN THIS ROOM.", ctx -> {
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

        // Numbers

        addRule("ONE", "number", ctx -> {
            return new ConstantFormula(new Value(1));
        });
        addRule("TWO", "number", ctx -> {
            return new ConstantFormula(new Value(2));
        });
        addRule("THREE", "number", ctx -> {
            return new ConstantFormula(new Value(3));
        });
        addRule("FOUR", "number", ctx -> {
            return new ConstantFormula(new Value(4));
        });

        // Boxes

        addRule("THE :color BOX", "box", ctx -> {
            return new FunctionFormula(Functions.BOX_FOR_COLOR, ctx.get("color"));
        });
        addRule("THE MIDDLE BOX", "box", ctx -> {
            // The order of the boxes is blue, white, black
            return new ConstantFormula(new Value(new Box(BoxColor.WHITE)));
        });
        addRule("THIS BOX", "box", Rules::formulaThisBox);

        // Simple statements

        addRule(":box IS :box.", ctx -> {
            return new FunctionFormula(Functions.BOX_EQUALS,
                ctx.get("box", 1),
                ctx.get("box", 2)
            );
        });

        addRule(":box IS :color.", ctx -> {
            return new FunctionFormula(Functions.COLOR_EQUALS,
                new FunctionFormula(Functions.COLOR_OF_BOX, ctx.get("box")),
                ctx.get("color")
            );
        });
        addRule(":box IS NOT :color.", ctx -> {
            return new FunctionFormula(Functions.NOT,
                new FunctionFormula(Functions.COLOR_EQUALS,
                    new FunctionFormula(Functions.COLOR_OF_BOX, ctx.get("box")),
                    ctx.get("color")
                )
            );
        });

        addRule("THIS IS :box.", ctx -> {
            return new FunctionFormula(Functions.BOX_EQUALS,
                formulaThisBox(ctx),
                ctx.get("box")
            );
        });

        addRule(":box IS TRUE.", ctx -> {
            return formulaBoxIsTrue(ctx.get("box"));
        });
        // TODO: Verify the box only has one statement on it
        addAlias("THE STATEMENT ON :box IS TRUE.");

        addRule(":box IS FALSE.", ctx -> {
            // Not the same as the inverse of the box being true, since
            // there can be both true and false statements on a box (or no statements),
            // making the box neither true nor false.
            return formulaBoxIsFalse(ctx.get("box"));
        });

        // Location of gems

        addRule(":box CONTAINS THE GEMS.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"));
        });
        addAlias(":box CONTAINS GEMS.");
        addAlias(":box HAS THE GEMS.");
        addAlias(":box HAS GEMS.");
        addAlias("THE GEMS ARE IN :box.");
        addAlias(":box IS NOT EMPTY.");

        addRule(":box DOES NOT CONTAIN THE GEMS.", ctx -> {
            return new FunctionFormula(Functions.NOT,
                new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"))
            );
        });
        addAlias("THE GEMS ARE NOT IN :box.");
        addAlias(":box IS EMPTY.");

        addRule("THIS IS NOT AN EMPTY BOX.", ctx -> {
            return new FunctionFormula(Functions.BOX_HAS_GEMS, formulaThisBox(ctx));
        });

        addRule(":box IS TRUE AND IT CONTAINS THE GEMS.", ctx -> {
            return new FunctionFormula(Functions.AND,
                formulaBoxIsTrue(ctx.get("box")),
                new FunctionFormula(Functions.BOX_HAS_GEMS, ctx.get("box"))
            );
        });
    }

    private static void addRule(String pattern, ParseAction action) {
        rules.add(new ParseRule(pattern, false, "sentence", action));
        lastAction = action;
    }

    private static void addRule(String pattern, String identifier, ParseAction action) {
        rules.add(new ParseRule(pattern, true, identifier, action));
    }

    private static void addAlias(String pattern) {
        addRule(pattern, lastAction);
    }

    // Helper functions

    private static Formula formulaThisBox(ParseContext ctx) {
        return new ConstantFormula(new Value(new Box(ctx.getCurrentBox())));
    }

    private static Formula formulaBoxIsTrue(Formula box) {
        return new FunctionFormula(Functions.BOX_IS,
            box,
            new ConstantFormula(new Value(true))
        );
    }

    private static Formula formulaBoxIsFalse(Formula box) {
        return new FunctionFormula(Functions.BOX_IS,
            box,
            new ConstantFormula(new Value(false))
        );
    }
}
