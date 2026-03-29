package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operator;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.formula.Quantifier;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Group;
import com.spacemonkeyy.parlorsolver.value.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.spacemonkeyy.parlorsolver.formula.Formula.*;

public class Rules {
    public static List<ParseRule> rules = new ArrayList<>();

    private static ParseAction lastAction;
    private static String lastIdentifier;

    static {
        // Trivial statements

        addRule("YOU ARE IN THE PARLOR.", ctx -> {
            // True; the game takes place in the parlor.
            return constant(new Value(true));
        });
        addRule("THERE IS A SECOND WIND-UP KEY IN THIS ROOM.", ctx -> {
            // False; there is only ever one wind-up key in the room.
            return constant(new Value(false));
        });

        addRule("THE GEMS ARE ON THE DESK.", ctx -> {
            // False; the gems are always in a box.
            return constant(new Value(false));
        });
        addAlias("THE GEMS ARE ON THE FLOOR.");
        addAlias("THE GEMS ARE ON THE TABLE BEHIND YOU.");

        addRule("THERE ARE :number BOXES IN THIS ROOM.", ctx -> {
            // There are three boxes in the room.
            return function(Operators.NUMBER_EQUALS,
                ctx.get("number"),
                constant(new Value(3))
            );
        });
        addRule("THERE IS ONLY ONE BOX IN THIS ROOM.", ctx -> {
            return constant(new Value(false));
        });

        // Booleans

        addRule("TRUE", "bool", ctx -> {
            return constant(new Value(true));
        });
        addRule("FALSE", "bool", ctx -> {
            return constant(new Value(false));
        });

        // Colors

        addRule("BLUE", "color", ctx -> {
            return constant(new Value(BoxColor.BLUE));
        });
        addRule("WHITE", "color", ctx -> {
            return constant(new Value(BoxColor.WHITE));
        });
        addRule("BLACK", "color", ctx -> {
            return constant(new Value(BoxColor.BLACK));
        });

        // Numbers

        addRule("ONE", "number", ctx -> {
            return constant(new Value(1));
        });
        addRule("TWO", "number", ctx -> {
            return constant(new Value(2));
        });
        addRule("THREE", "number", ctx -> {
            return constant(new Value(3));
        });
        addRule("FOUR", "number", ctx -> {
            return constant(new Value(4));
        });

        // Boxes

        addRule("THE :color BOX", "box", ctx -> {
            return function(Operators.BOX_FOR_COLOR, ctx.get("color"));
        });
        addRule("THE MIDDLE BOX", "box", ctx -> {
            // The order of the boxes is blue, white, black
            return constant(new Value(new Box(BoxColor.WHITE)));
        });
        addRule("THIS BOX", "box", Rules::formulaThisBox);

        // Groups

        addRule(":box AND :box", "box", ctx -> {
            return function(Operators.GROUP,
                ctx.get("box", 1),
                ctx.get("box", 2)
            );
        });

        addRule("ALL :number BOXES", "box", ctx -> {
            // TODO: Verify that the number was three
            return formulaAllBoxes();
        });
        addRule("THE OTHER :number BOXES", "box", ctx -> {
            // TODO: Verify that the number was two
            List<Value> boxes = new ArrayList<>();
            for (BoxColor color : BoxColor.values()) {
                if (color != ctx.getCurrentBox()) {
                    boxes.add(new Value(new Box(color)));
                }
            }
            return constant(new Value(new Group(boxes)));
        });

        addRule("BOXES NEXT TO :box", "box", ctx -> {
            return function(Operators.NEIGHBORS, ctx.get("box"));
        });
        addRule("A BOX NEXT TO :box", "box", ctx -> {
            return function(Operators.NEIGHBORS, ctx.get("box"))
                .withQuantifierHint(Quantifier.EXISTS);
        });
        // TODO: Verify there is only one box
        addAlias("THE BOX NEXT TO :box");

        addRule("A :bool BOX", "box", ctx -> {
            return function(Operators.FILTER(Operators.BOX_IS),
                formulaAllBoxes(),
                ctx.get("bool")
            ).withQuantifierHint(Quantifier.EXISTS);
        });
        // TODO: Verify there is only one box
        addAlias("THE :bool BOX");

        addRule("A BOX WITH A :bool STATEMENT", "box", ctx -> {
            return function(Operators.FILTER(Operators.BOX_HAS_BOOL_STATEMENT),
                formulaAllBoxes(),
                ctx.get("bool")
            ).withQuantifierHint(Quantifier.EXISTS);
        });

        addRule("A BOX WITH A STATEMENT", "box", ctx -> {
            return function(Operators.FILTER(Operators.BOX_HAS_STATEMENT), formulaAllBoxes())
                .withQuantifierHint(Quantifier.EXISTS);
        });

        // Simple statements

        addRule(":box IS :box.", ctx -> {
            return function(Operators.BOX_EQUALS,
                ctx.get("box", 1),
                ctx.get("box", 2)
            );
        });

        addRule(":box IS :color.", ctx -> {
            return function(Operators.COLOR_EQUALS,
                function(Operators.COLOR_OF_BOX, ctx.get("box")),
                ctx.get("color")
            );
        });
        addRule(":box IS NOT :color.", ctx -> {
            return function(Operators.NOT,
                function(Operators.COLOR_EQUALS,
                    function(Operators.COLOR_OF_BOX, ctx.get("box")),
                    ctx.get("color")
                )
            );
        });
        addRule(":box ARE :color.", ctx -> {
            return predicate(Operators.COLOR_EQUALS, Quantifier.FORALL,
                function(Operators.COLOR_OF_BOX, ctx.get("box")),
                ctx.get("color")
            );
        });

        addRule("THIS IS :box.", ctx -> {
            return function(Operators.BOX_EQUALS,
                formulaThisBox(ctx),
                ctx.get("box")
            );
        });

        // TODO: check if plurality of verb matches presence of group for rules like these
        addRule(":box IS :bool.", ctx -> {
            return function(Operators.BOX_IS,
                ctx.get("box"),
                ctx.get("bool")
            );
        });
        // TODO: Verify the box only has one statement on it
        addAlias("THE STATEMENT ON :box IS :bool.");
        addRule(":box ARE :bool.", ctx -> {
            return predicate(Operators.BOX_IS, Quantifier.FORALL,
                ctx.get("box"),
                ctx.get("bool")
            );
        });

        addRule(":box DISPLAYS A :bool STATEMENT.", ctx -> {
            return function(Operators.BOX_HAS_BOOL_STATEMENT,
                ctx.get("box"),
                ctx.get("bool")
            );
        });

        // TODO: Is this ever inverted?
        addRule(":box DOES NOT HAVE A STATEMENT.", ctx -> {
            return function(Operators.NOT,
                function(Operators.BOX_HAS_STATEMENT, ctx.get("box"))
            );
        });

        // Location of gems

        addRule(":box CONTAINS THE GEMS.", ctx -> {
            return function(Operators.BOX_HAS_GEMS, ctx.get("box"));
        });
        addAlias(":box CONTAINS GEMS.");
        addAlias(":box HAS THE GEMS.");
        addAlias(":box HAS GEMS.");
        addAlias("THE GEMS ARE IN :box.");
        addAlias(":box IS NOT EMPTY.");

        addRule(":box DOES NOT CONTAIN THE GEMS.", ctx -> {
            return function(Operators.NOT,
                function(Operators.BOX_HAS_GEMS, ctx.get("box"))
            );
        });
        addAlias("THE GEMS ARE NOT IN :box.");
        addAlias(":box IS EMPTY.");

        addRule(":box CONTAIN GEMS.", ctx -> {
            return predicate(Operators.BOX_HAS_GEMS,
                Quantifier.FORALL, ctx.get("box"));
        });
        addRule(":box ARE EMPTY.", ctx -> {
            return predicate(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT),
                Quantifier.FORALL, ctx.get("box"));
        });
        addRule(":box ARE BOTH EMPTY.", ctx -> {
            // TODO: Verify there are two boxes in the group
            return predicate(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT),
                Quantifier.FORALL, ctx.get("box"));
        });

        addRule("THIS IS NOT AN EMPTY BOX.", ctx -> {
            return function(Operators.BOX_HAS_GEMS, formulaThisBox(ctx));
        });

        addRule(":box IS :bool AND IT CONTAINS THE GEMS.", ctx -> {
            return function(Operators.AND,
                function(Operators.BOX_IS,
                    ctx.get("box"),
                    ctx.get("bool")
                ),
                function(Operators.BOX_HAS_GEMS, ctx.get("box"))
            );
        });
    }

    private static void addRule(String pattern, ParseAction action) {
        addRule(pattern, "sentence", action);
    }

    private static void addRule(String pattern, String identifier, ParseAction action) {
        rules.add(new ParseRule(pattern, identifier, action));
        lastAction = action;
        lastIdentifier = identifier;
    }

    private static void addAlias(String pattern) {
        addRule(pattern, lastIdentifier, lastAction);
    }

    // Helper functions

    public static Formula formulaThisBox(ParseContext ctx) {
        return constant(new Value(new Box(ctx.getCurrentBox())));
    }

    public static Formula formulaAllBoxes() {
        List<Value> boxes = new ArrayList<>();
        for (BoxColor color : BoxColor.values()) {
            boxes.add(new Value(new Box(color)));
        }
        return constant(new Value(new Group(boxes)));
    }

    public static Formula formulaBoxIsTrue(Formula box) {
        return function(Operators.BOX_IS,
            box,
            constant(new Value(true))
        );
    }

    public static Formula formulaBoxIsFalse(Formula box) {
        return function(Operators.BOX_IS,
            box,
            constant(new Value(false))
        );
    }

    public static Formula formulaAtLeastOneBox(Function<Formula, Formula> predicate) {
        Formula blue = predicate.apply(constant(new Value(new Box(BoxColor.BLUE))));
        Formula white = predicate.apply(constant(new Value(new Box(BoxColor.WHITE))));
        Formula black = predicate.apply(constant(new Value(new Box(BoxColor.BLACK))));

        return function(Operators.OR3,
            blue,
            white,
            black
        );
    }

    public static Formula formulaExactlyOneBox(Function<Formula, Formula> predicate) {
        Formula blue = predicate.apply(constant(new Value(new Box(BoxColor.BLUE))));
        Formula white = predicate.apply(constant(new Value(new Box(BoxColor.WHITE))));
        Formula black = predicate.apply(constant(new Value(new Box(BoxColor.BLACK))));

        return function(Operators.OR3,
            function(Operators.AND3,
                blue,
                function(Operators.NOT, white),
                function(Operators.NOT, black)
            ),
            function(Operators.AND3,
                function(Operators.NOT, blue),
                white,
                function(Operators.NOT, black)
            ),
            function(Operators.AND3,
                function(Operators.NOT, blue),
                function(Operators.NOT, white),
                black
            )
        );
    }
}
