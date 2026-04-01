package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operator;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.formula.Quantifier;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
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

        // General groups

        for (String identifier : List.of("box", "statement")) {
            addRule(":number :" + identifier, identifier, ctx -> {
                int count = ctx.get("number").evaluate(null).asNumber();
                return ctx.get(identifier).withQuantifier(Quantifier.exactly(count));
            });
            addAlias(":number OF :" + identifier);
            addAlias("ONLY :number :" + identifier);
            addAlias("ONLY :number OF :" + identifier);

            addRule("THERE ARE :" + identifier + ".", ctx -> {
                // Let the quantifier do its thing with no other manipulation
                return function(Operators.TRIVIAL, ctx.get(identifier));
            });
            addVariant("THERE IS :" + identifier + ".", ctx -> {
                ctx.assumeQuantity(ctx.get(identifier), 1);
            });
        }

        // Boxes

        addRule("THE :color BOX", "box", ctx -> {
            return function(Operators.BOX_FOR_COLOR, ctx.get("color"));
        });
        addAlias("A BOX THAT IS ACTUALLY :color");

        addRule("THE MIDDLE BOX", "box", ctx -> {
            // The order of the boxes is blue, white, black
            return constant(new Value(new Box(BoxColor.WHITE)));
        });
        addRule("THIS BOX", "box", Rules::formulaThisBox);

        addRule(":box AND :box", "box", ctx -> {
            return function(Operators.GROUP,
                ctx.get("box", 1),
                ctx.get("box", 2)
            );
        });
        addAlias("BOTH :box AND :box");

        addRule("ALL :number BOXES", "box", ctx -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(3))
            ));
            return formulaAllBoxes();
        });
        addRule("ONLY :number BOX", "box", ctx -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(1))
            ));
            return formulaAllBoxes().withQuantifier(Quantifier.exactly(1));
        });

        addRule("THE OTHER BOXES", "box", Rules::formulaOtherBoxes);
        addVariant("THE OTHER :number BOXES", ctx -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(2))
            ));
        });
        addVariant("ANOTHER BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));

        addRule(":number BOXES IN THIS ROOM", "box", ctx -> {
            int count = ctx.get("number").evaluate(null).asNumber();
            return formulaAllBoxes().withQuantifier(Quantifier.exactly(count));
        });

        addRule("EMPTY BOXES", "box", ctx -> {
            return function(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT),
                formulaAllBoxes()
            ).makeFilter();
        });
        addAlias("THE EMPTY BOXES");
        addVariant("THE :number EMPTY BOXES", (f, ctx) -> {
            int count = ctx.get("number").evaluate(null).asNumber();
            return f.withQuantifier(Quantifier.exactly(count));
        });

        addRule("BOXES NEXT TO :box", "box", ctx -> {
            return function(Operators.NEIGHBORS, ctx.get("box"));
        });
        addVariant("THE BOX NEXT TO :box", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("BOTH BOXES NEXT TO :box", (f, ctx) ->
            ctx.assumeQuantity(f, 2));
        addVariant("A BOX NEXT TO :box", (Formula f) -> f.withQuantifier(Quantifier.any()));

        addRule(":bool BOXES", "box", ctx -> {
            return function(Operators.BOX_IS,
                formulaAllBoxes(),
                ctx.get("bool")
            ).makeFilter();
        });
        addAlias("THE :bool BOXES");
        addVariant("A :bool BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));
        // Don't assume quantity here because of variation 109
        addVariant("THE :bool BOX", (Formula f) -> f.withQuantifier(Quantifier.exactly(1)));
        addVariant("THE ONLY :bool BOX", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        addRule("A BOX WITH THE WORD :word ON IT", "box", ctx -> {
            return function(Operators.UNIQUE,
                function(Operators.BOX_OF_STATEMENT,
                    function(Operators.STATEMENT_HAS_WORD(ctx.getToken("word").getSource()),
                        formulaAllStatements(ctx)
                    ).makeFilter()
                )
            ).withQuantifier(Quantifier.any());
        });

        // Statements

        addRule("THIS STATEMENT", "statement", Rules::formulaThisStatement);

        addRule("THESE STATEMENTS", "statement", ctx -> {
            // Could refer to either the statements on this box or all statements
            Box box = ctx.getCurrentBox();
            if (ctx.getInput().byColor(box.color()).size() > 1) {
                return function(Operators.STATEMENTS_ON_BOX, constant(new Value(box)));
            } else {
                return formulaAllStatements(ctx);
            }
        });

        addRule("THE OTHER :number STATEMENTS", "statement", ctx -> {
            Formula f = formulaOtherStatements(ctx);
            ctx.addAssumption(function(Operators.EQUALS,
                function(Operators.GROUP_SIZE, f),
                ctx.get("number")
            ));
            return f;
        });

        addRule("STATEMENTS", "statement", Rules::formulaAllStatements);
        addVariant("ALL :number STATEMENTS", (f, ctx) -> {
            ctx.addAssumption(function(Operators.EQUALS,
                function(Operators.GROUP_SIZE, f),
                ctx.get("number")
            ));
            return f;
        });
        addVariant("A STATEMENT", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("EVERY STATEMENT", (Formula f) -> f.withQuantifier(Quantifier.all()));
        addVariant("ONLY :number STATEMENT", (f, ctx) -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(1))
            ));
            return f.withQuantifier(Quantifier.exactly(1));
        });

        addRule(":bool STATEMENTS", "statement", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool), formulaAllStatements(ctx)).makeFilter();
        });
        addAlias(":bool STATEMENT");
        addVariant("A :bool STATEMENT", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("THE ONLY :bool STATEMENT", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        addRule(":statement WITH THE WORD :word", "statement", ctx -> {
            return function(Operators.STATEMENT_HAS_WORD(ctx.getToken("word").getSource()),
                ctx.get("statement")
            ).makeFilter().withQuantifier(ctx.get("statement").getQuantifier());
        });
        addAlias(":statement CONTAINING THE LETTER :word");

        // After statements and boxes have parsed

        addRule("A BOX WITH :statement", "box", ctx -> {
            return function(Operators.UNIQUE,
                function(Operators.BOX_OF_STATEMENT, ctx.get("statement"))
            ).withQuantifier(Quantifier.any());
        });
        addVariant("THE ONLY BOX WITH :statement", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        addRule("THE OTHER BOX WITH :statement", "box", ctx -> {
            // Implies this box also has the statement
            ctx.addAssumption(function(Operators.BOX_HAS_STATEMENT,
                formulaThisBox(ctx),
                ctx.get("statement")
            ));
            return ctx.assumeQuantity(function(Operators.BOX_HAS_STATEMENT,
                formulaOtherBoxes(ctx),
                ctx.get("statement")
            ).makeFilter(), 1);
        });

        addRule("THE STATEMENTS ON :box", "statement", ctx -> {
            return function(Operators.STATEMENTS_ON_BOX, ctx.get("box"));
        });
        addVariant("THE STATEMENT ON :box", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant(":box'S STATEMENT", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        // Simple sentences

        addRule(":box IS :box.", ctx -> {
            return function(Operators.EQUALS,
                ctx.get("box", 1),
                ctx.get("box", 2)
            );
        });

        addRule(":box IS :color.", ctx -> {
            return function(Operators.EQUALS,
                function(Operators.COLOR_OF_BOX, ctx.get("box")),
                ctx.get("color")
            );
        });
        addAlias(":box ARE :color.");
        addRule(":box IS NOT :color.", ctx -> {
            return function(Operators.NOT,
                function(Operators.EQUALS,
                    function(Operators.COLOR_OF_BOX, ctx.get("box")),
                    ctx.get("color")
                )
            );
        });

        addRule("THIS IS :box.", ctx -> {
            return function(Operators.EQUALS,
                formulaThisBox(ctx),
                ctx.get("box")
            );
        });

        addRule(":box IS :bool.", ctx -> {
            return function(Operators.BOX_IS,
                ctx.get("box"),
                ctx.get("bool")
            );
        });
        addAlias(":box ARE :bool.");
        addVariant(":box ARE BOTH :bool.", (f, ctx) ->
            ctx.assumeQuantity(f, 2));

        addRule(":box HAS :statement.", ctx -> {
            // Multiple quantifiers at once
            return function(Operators.BOX_HAS_STATEMENT,
                ctx.get("box"),
                ctx.get("statement")
            );
        });
        addAlias(":box DISPLAYS :statement.");

        addRule(":box BOTH HAVE :statement.", ctx -> {
            ctx.assumeQuantity(ctx.get("box"), 2);
            return function(Operators.BOX_HAS_STATEMENT,
                ctx.get("box").withQuantifier(Quantifier.all()),
                ctx.get("statement").withQuantifier(Quantifier.any())
            );
        });

        addRule(":box DOES NOT HAVE :statement.", ctx -> {
            return function(Operator.compose(Operators.EQUALS, Operators.NOT),
                function(Operators.BOX_OF_STATEMENT, ctx.get("statement")),
                ctx.get("box")
            );
        });

        addRule("THERE ARE :number :box.", ctx -> {
            return function(Operators.EQUALS,
                function(Operators.GROUP_SIZE, ctx.get("box")),
                ctx.get("number")
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
        addAlias("GEMS ARE IN :box.");
        addAlias(":box IS NOT EMPTY.");
        addAlias(":box CONTAIN GEMS.");

        addRule(":box DOES NOT CONTAIN THE GEMS.", ctx -> {
            return function(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT), ctx.get("box"));
        });
        addAlias(":box IS EMPTY.");
        addAlias(":box ARE EMPTY.");
        addVariant("THE GEMS ARE NOT IN :box.", ctx -> {
            ctx.get("box").withQuantifier(Quantifier.all());
        });
        addVariant(":box ARE BOTH EMPTY.", ctx -> {
            ctx.assumeQuantity(ctx.get("box"), 2);
            ctx.get("box").withQuantifier(Quantifier.all());
        });
        addRule("GEMS ARE NOT IN :box OR :box.", ctx -> {
            return function(Operators.AND,
                function(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT), ctx.get("box", 1)),
                function(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT), ctx.get("box", 2))
            );
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
        addRule(":box CONTAINS THE GEMS IF ITS STATEMENT IS :bool.", ctx -> {
            ctx.assumeQuantity(function(Operators.STATEMENTS_ON_BOX, ctx.get("box")), 1);
            return function(Operators.IMPLIES,
                function(Operators.BOX_IS,
                    ctx.get("box"),
                    ctx.get("bool")
                ),
                function(Operators.BOX_HAS_GEMS, ctx.get("box"))
            );
        });

        addRule(":statement IS :bool.", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool), ctx.get("statement"));
        });
        addAlias(":statement ARE :bool.");
        addVariant(":statement ARE ALWAYS :bool.", ctx -> {
            ctx.get("statement").withQuantifier(Quantifier.all());
        });
        addVariant(":statement ARE BOTH :bool.", ctx -> {
            ctx.get("statement").withQuantifier(Quantifier.all());
            ctx.assumeQuantity(ctx.get("statement"), 2);
        });

        addRule(":statement IS BOTH :bool AND :bool.", ctx -> {
            boolean b1 = ctx.get("bool", 1).evaluate(null).asBoolean();
            boolean b2 = ctx.get("bool", 2).evaluate(null).asBoolean();
            return function(Operators.AND,
                function(operatorStatementIs(b1), ctx.get("statement")),
                function(operatorStatementIs(b2), ctx.get("statement"))
            );
        });

        addRule(":statement IS AS :bool AS :statement.", ctx -> {
            return function(Operators.EQUALS,
                function(Operators.STATEMENT_IS_TRUE, ctx.get("statement", 1)),
                function(Operators.STATEMENT_IS_TRUE, ctx.get("statement", 2))
            );
        });

        addRule(":statement APPEARS ON :box.", ctx -> {
            return function(Operators.STATEMENTS_MATCH,
                function(Operators.STATEMENTS_ON_BOX, ctx.get("box"))
                    .withQuantifier(Quantifier.any()),
                ctx.get("statement")
            );
        });

        addRule(":statement HAVE IDENTICAL WORDING.", ctx -> {
            return function(Operators.STATEMENTS_MATCH_GROUP, ctx.get("statement"));
        });

        addRule("THIS IS :statement.", ctx -> {
            return function(Operators.EQUALS,
                ctx.get("statement"),
                formulaThisStatement(ctx)
            );
        });

        sortRules();
    }

    private static void addRule(String pattern, ParseAction action) {
        addRule(pattern, "sentence", action);
    }

    private static void addRule(String pattern, String identifier, ParseAction action) {
        rules.add(new ParseRule(pattern, identifier, action));
        lastIdentifier = identifier;
        lastAction = action;
    }

    private static void addAlias(String pattern) {
        rules.add(new ParseRule(pattern, lastIdentifier, lastAction));
    }

    private static void addVariant(String pattern, Function<Formula, Formula> func) {
        addVariant(pattern, (f, ctx) -> func.apply(f));
    }

    private static void addVariant(String pattern, Consumer<ParseContext> func) {
        addVariant(pattern, (f, ctx) -> {
            func.accept(ctx);
            return f;
        });
    }

    private static void addVariant(String pattern, BiFunction<Formula, ParseContext, Formula> func) {
        ParseAction last = lastAction;
        rules.add(new ParseRule(pattern, lastIdentifier, ctx -> {
            Formula formula = last.apply(ctx);
            return func.apply(formula, ctx);
        }));
    }

    private static void sortRules() {
        // Rules that fit inside other rules should always be checked last.
        // Otherwise, the longer rule would never match.

        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                ParseRule e1 = rules.get(i);
                ParseRule e2 = rules.get(j);

                if (e1.matches(e2.getPattern()) != -1) {
                    // The first rule is shorter, swap them
                    rules.set(j, e1);
                    rules.set(i, e2);
                }
            }
        }
    }

    // Helper functions

    public static Operator operatorStatementIs(boolean bool) {
        Operator operator = Operators.STATEMENT_IS_TRUE;
        if (!bool) {
            operator = Operator.compose(operator, Operators.NOT);
        }
        return operator;
    }

    public static Formula formulaThisBox(ParseContext ctx) {
        return constant(new Value(ctx.getCurrentBox()));
    }

    public static Formula formulaOtherBoxes(ParseContext ctx) {
        List<Value> boxes = new ArrayList<>();
        for (BoxColor color : BoxColor.values()) {
            if (color != ctx.getCurrentBox().color()) {
                boxes.add(new Value(new Box(color)));
            }
        }
        return constant(new Value(new Group(boxes)));
    }

    public static Formula formulaAllBoxes() {
        List<Value> boxes = new ArrayList<>();
        for (BoxColor color : BoxColor.values()) {
            boxes.add(new Value(new Box(color)));
        }
        return constant(new Value(new Group(boxes)));
    }

    public static Formula formulaThisStatement(ParseContext ctx) {
        return constant(new Value(ctx.getCurrentStatement()));
    }

    public static Formula formulaOtherStatements(ParseContext ctx) {
        List<Value> statements = new ArrayList<>();
        for (BoxColor color : BoxColor.values()) {
            for (int i = 1; i <= ctx.getInput().byColor(color).size(); i++) {
                Statement statement = new Statement(new Box(color), i);
                if (!statement.equals(ctx.getCurrentStatement())) {
                    statements.add(new Value(statement));
                }
            }
        }
        return constant(new Value(new Group(statements)));
    }

    public static Formula formulaAllStatements(ParseContext ctx) {
        List<Value> statements = new ArrayList<>();
        for (BoxColor color : BoxColor.values()) {
            for (int i = 1; i <= ctx.getInput().byColor(color).size(); i++) {
                statements.add(new Value(new Statement(new Box(color), i)));
            }
        }
        return constant(new Value(new Group(statements)));
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
