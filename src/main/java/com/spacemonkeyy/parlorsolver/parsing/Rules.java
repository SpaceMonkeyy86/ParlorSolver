package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operator;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.formula.Quantifier;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

        addRule("THIS STATEMENT IS OF NO HELP AT ALL.", ctx -> {
            // False; if this statement were actually true, then by definition
            // it does not matter that we assume that it is false.
            return constant(new Value(false));
        });

        addRule("THIS PUZZLE IS HARDER THAN IT SEEMS.", ctx -> {
            // Indeterminate, could be either true or false
            // (In reality this statement is always false)
            return function(Operators.STATEMENT_IS_TRUE, formulaThisStatement(ctx));
        });

        addRule("YOU WILL NOT SOLVE THIS PUZZLE.", ctx -> {
            // False; all puzzles are solvable and can be solved with enough effort.
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
        addRule("FIVE", "number", ctx -> {
            return constant(new Value(5));
        });
        addRule("SIX", "number", ctx -> {
            return constant(new Value(6));
        });

        for (int i = 1; i < 10; i++) {
            int number = i;
            addRule(String.valueOf(i), "number", ctx -> {
                return constant(new Value(number));
            });
        }

        // General groups

        for (String identifier : List.of("box", "statement")) {
            addRule(":number :" + identifier, identifier, ctx -> {
                int count = ctx.get("number").evaluate(null).asNumber();
                return ctx.get(identifier).withQuantifier(Quantifier.exactly(count));
            });
            addAlias("EXACTLY :number :" + identifier);
            addAlias(":number OF :" + identifier);
            addAlias("ONLY :number :" + identifier);
            addAlias("ONLY :number OF :" + identifier);

            addRule("AT LEAST :number :" + identifier, identifier, ctx -> {
                int count = ctx.get("number").evaluate(null).asNumber();
                return ctx.get(identifier).withQuantifier(Quantifier.atLeast(count));
            });
            addRule("MORE THAN :number :" + identifier, identifier, ctx -> {
                int count = ctx.get("number").evaluate(null).asNumber();
                return ctx.get(identifier).withQuantifier(Quantifier.atLeast(count + 1));
            });

            addRule("BOTH OF :" + identifier, identifier, ctx -> {
                return ctx.get(identifier).withQuantifier(Quantifier.exactly(2));
            });

            addRule("THERE ARE :" + identifier + ".", ctx -> {
                // Let the quantifier do its thing with no other manipulation
                return function(Operators.TRIVIAL, ctx.get(identifier));
            });
            addAlias("THERE IS :" + identifier + ".");
            /*
            addVariant("THERE IS :" + identifier + ".", ctx -> {
                ctx.assumeQuantity(ctx.get(identifier), 1);
            });
             */
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

        addRule("EACH BOX", "box", ctx -> Rules.formulaAllBoxes());
        addAlias("BOXES IN THIS ROOM");
        addVariant("ALL :number BOXES", ctx -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(3))
            ));
        });
        addVariant(":number BOXES IN THIS ROOM", (f, ctx) -> {
            int count = ctx.get("number").evaluate(null).asNumber();
            return f.withQuantifier(Quantifier.exactly(count));
        });
        addVariant("ONLY :number BOX", (Formula f) ->
            f.withQuantifier(Quantifier.exactly(1)));
        addVariant("ONLY :number BOX IN THIS ROOM", (Formula f) ->
            f.withQuantifier(Quantifier.exactly(1)));

        addRule("THE OTHER BOXES", "box", Rules::formulaOtherBoxes);
        addVariant("THE OTHER :number BOXES", ctx -> {
            ctx.addAssumption(function(Operators.EQUALS,
                ctx.get("number"),
                constant(new Value(2))
            ));
        });
        addVariant("ANOTHER BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));

        addRule("EMPTY BOXES", "box", ctx -> {
            return function(Operator.compose(Operators.BOX_HAS_GEMS, Operators.NOT),
                formulaAllBoxes()
            ).makeFilter();
        });
        addAlias("THE EMPTY BOXES");
        addVariant("AN EMPTY BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("THE :number EMPTY BOXES", (f, ctx) -> {
            int count = ctx.get("number").evaluate(null).asNumber();
            return f.withQuantifier(Quantifier.exactly(count));
        });

        addRule("BOXES NEXT TO :box", "box", ctx -> {
            return function(Operators.NEIGHBORS,
                formulaAllBoxes(),
                ctx.get("box")
            ).makeFilter();
        });
        addVariant("A BOX NEXT TO :box", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("THE BOX NEXT TO :box", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("BOTH BOXES NEXT TO :box", (f, ctx) ->
            ctx.assumeQuantity(f, 2));

        addRule(":box NEXT TO :box", "box", ctx -> {
            return function(Operators.NEIGHBORS,
                ctx.get("box", 1),
                ctx.get("box", 2)
            ).makeFilter().withQuantifier(ctx.get("box", 1).getQuantifier());
        });

        addRule(":bool BOX", "box", ctx -> {
            return function(Operators.BOX_IS,
                formulaAllBoxes(),
                ctx.get("bool")
            ).makeFilter();
        });
        addAlias(":bool BOXES");
        addAlias("THE :bool BOXES");
        addVariant("A :bool BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("A COMPLETELY :bool BOX", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("A BOX WITH ONLY :bool STATEMENTS", (Formula f) -> f.withQuantifier(Quantifier.any()));
        // Don't assume quantity here because of variation 109
        addVariant("THE :bool BOX", (Formula f) -> f.withQuantifier(Quantifier.exactly(1)));
        addVariant("THE ONLY :bool BOX", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        addRule("A BOX THAT CONTAINS A MIX OF TRUE AND FALSE STATEMENTS", "box", ctx -> {
            Formula trueStatements = function(operatorStatementIs(true),
                formulaAllStatements(ctx)
            ).makeFilter().withQuantifier(Quantifier.any());
            Formula falseStatements = function(operatorStatementIs(false),
                formulaAllStatements(ctx)
            ).makeFilter().withQuantifier(Quantifier.any());

            return function(Operators.BOX_HAS_STATEMENT,
                function(Operators.BOX_HAS_STATEMENT,
                    formulaAllBoxes(),
                    trueStatements
                ).makeFilter(),
                falseStatements
            ).makeFilter().withQuantifier(Quantifier.any());
        });
        addVariant("NO BOX THAT CONTAINS A MIX OF TRUE AND FALSE STATEMENTS",
            (Formula f) -> f.withQuantifier(Quantifier.none()));

        addRule("A BOX CONTAINING GEMS", "box", ctx -> {
            return function(Operators.BOX_HAS_GEMS,
                formulaAllBoxes()
            ).makeFilter().withQuantifier(Quantifier.any());
        });
        addVariant("THE BOX WITH THE GEMS", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

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

        addRule("THE ABOVE STATEMENT", "statement", ctx -> {
            return constant(new Value(ctx.getLastStatement()));
        });

        addRule("THE TOP STATEMENT OF :box", "statement", ctx -> {
            return function(Operators.STATEMENT_ON_BOX,
                ctx.get("box"),
                constant(new Value(1))
            );
        });

        addRule("TOP STATEMENT", "statement", ctx -> {
            List<Value> values = new ArrayList<>();
            for (BoxColor color : BoxColor.values()) {
                if (!ctx.getInput().byColor(color).isEmpty()) {
                    values.add(new Value(new Statement(new Box(color), 1)));
                }
            }
            return constant(new Value(new Group(values)));
        });
        addAlias("THE TOP STATEMENTS");
        addRule("BOTTOM STATEMENT", "statement", ctx -> {
            List<Value> values = new ArrayList<>();
            for (BoxColor color : BoxColor.values()) {
                if (!ctx.getInput().byColor(color).isEmpty()) {
                    values.add(new Value(new Statement(new Box(color),
                        ctx.getInput().byColor(color).size())));
                }
            }
            return constant(new Value(new Group(values)));
        });

        addRule("STATEMENTS", "statement", Rules::formulaAllStatements);
        addAlias("ALL STATEMENTS");
        addAlias("STATEMENTS ON BOXES IN THIS ROOM");
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

        addRule(":bool STATEMENT", "statement", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool), formulaAllStatements(ctx)).makeFilter();
        });
        addVariant("A :bool STATEMENT", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("THE ONLY :bool STATEMENT", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("NO :bool STATEMENTS", (Formula f) -> f.withQuantifier(Quantifier.none()));

        addRule(":bool :statement", "statement", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool),
                ctx.get("statement")
            ).makeFilter();
        });
        addAlias("A :bool :statement");

        addRule(":statement WITH THE WORD :word", "statement", ctx -> {
            return function(Operators.STATEMENT_CONTAINS(ctx.getToken("word").getSource(), true),
                ctx.get("statement")
            ).makeFilter().withQuantifier(ctx.get("statement").getQuantifier());
        });
        addAlias(":statement CONTAINING THE WORD :word");
        addAlias(":statement DISPLAYING THE WORD :word");

        addRule(":statement CONTAINING THE LETTER :word", "statement", ctx -> {
            return function(Operators.STATEMENT_CONTAINS(ctx.getToken("word").getSource(), false),
                ctx.get("statement")
            ).makeFilter().withQuantifier(ctx.get("statement").getQuantifier());
        });

        addRule("EVERY STATEMENT CLAIMING WHERE THE GEMS ARE", "statement", ctx -> {
            return function(Operators.STATEMENT_CONTAINS("THE GEMS ARE", true),
                formulaAllStatements(ctx)
            ).makeFilter();
        });

        addRule("THE STATEMENT MATCHING :statement", "statement", ctx -> {
            return ctx.assumeQuantity(function(Operators.STATEMENTS_MATCH,
                formulaOtherStatements(ctx),
                ctx.get("statement")
            ).makeFilter(), 1);
        });

        // After statements and boxes have parsed

        addRule("A BOX WITH :statement", "box", ctx -> {
            return function(Operators.BOX_HAS_STATEMENT,
                formulaAllBoxes(),
                ctx.get("statement")
            ).makeFilter().withQuantifier(Quantifier.any());
        });
        addVariant("THE BOX WITH :statement", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("THE ONLY BOX WITH :statement", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("NO BOX THAT DISPLAYS :statement", (Formula f) -> f.withQuantifier(Quantifier.none()));
        addVariant("NO BOX THAT CONTAINS :statement", (Formula f) -> f.withQuantifier(Quantifier.none()));

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

        addRule(":statement ON :box", "statement", ctx -> {
            return function(Operator.curry(Operators.BOX_HAS_STATEMENT),
                ctx.get("statement"),
                ctx.get("box")
            ).makeFilter().withQuantifier(ctx.get("statement").getQuantifier());
        });
        addAlias(":statement WRITTEN ON :box");

        addRule("THE STATEMENTS ON :box", "statement", ctx -> {
            return function(Operators.STATEMENTS_ON_BOX, ctx.get("box"));
        });
        addAlias("ALL THE STATEMENTS ON :box");
        addAlias(":box'S STATEMENTS");
        addVariant("THE STATEMENT ON :box", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant(":box'S STATEMENT", (f, ctx) ->
            ctx.assumeQuantity(f, 1));
        addVariant("BOTH STATEMENTS ON :box", (f, ctx) ->
            ctx.assumeQuantity(f, 2));
        addVariant("BOTH STATEMENTS WRITTEN ON :box", (f, ctx) ->
            ctx.assumeQuantity(f, 2));

        // Fix "ONLY :number OF :box" getting parsed before ":box'S STATEMENTS"
        addRule("ONLY :number OF :box'S STATEMENTS", "statement", ctx -> {
            int count = ctx.get("number").evaluate(null).asNumber();
            return function(Operators.STATEMENTS_ON_BOX,
                ctx.get("box")
            ).withQuantifier(Quantifier.exactly(count));
        });

        addRule("THE SECOND STATEMENT ON :box", "statement", ctx -> {
            ctx.assumeQuantity(function(Operators.STATEMENTS_ON_BOX, ctx.get("box")),
                Quantifier.atLeast(2));
            return function(Operators.STATEMENT_ON_BOX,
                ctx.get("box"),
                constant(new Value(2))
            );
        });

        // Word play

        addRule("EVERY BOX WITH THE WORD :word", "box", ctx -> {
            return formulaBoxesWithWord(ctx, ctx.getToken("word").getSource());
        });
        addAlias("ALL BOXES DISPLAYING THE WORD :word");
        addVariant("A BOX WITH THE WORD :word ON IT", (Formula f) -> f.withQuantifier(Quantifier.any()));
        addVariant("A BOX MENTIONING THE WORD :word", (Formula f) -> f.withQuantifier(Quantifier.any()));

        addRule("THE BOX THAT CLAIMS TO BE :color", "box", ctx -> {
            String word = ctx.get("color").evaluate(null).asColor().toString();
            return ctx.assumeQuantity(formulaBoxesWithWord(ctx,
                "THIS BOX IS THE " + word + " BOX") ,1);
        });

        addRule("A BOX WITH THE LONGEST WORD", "box", ctx -> {
            return formulaBoxesWithWord(ctx, longestWord(ctx)).withQuantifier(Quantifier.any());
        });
        addRule("A BOX WITH THE SHORTEST WORD", "box", ctx -> {
            return formulaBoxesWithWord(ctx, shortestWord(ctx)).withQuantifier(Quantifier.any());
        });

        addRule(":word IS THE LONGEST WORD ON A BOX.", ctx -> {
            return constant(new Value(longestWord(ctx).equals(ctx.getToken("word").getSource())));
        });
        addRule(":word IS THE SHORTEST WORD ON A BOX.", ctx -> {
            return constant(new Value(shortestWord(ctx).equals(ctx.getToken("word").getSource())));
        });

        addRule("THERE ARE WORDS ON :box.", ctx -> {
            return function(Operators.TRIVIAL,
                function(Operators.STATEMENTS_ON_BOX, ctx.get("box"))
                    .withQuantifier(Quantifier.any())
            );
        });

        addRule(":statement CONTAIN WORDS.", ctx -> {
            return function(Operators.TRIVIAL, ctx.get("statement"));
        });

        addRule("A BOX WITH THE MOST WORDS", "box", ctx -> {
            List<Value> values = new ArrayList<>();
            int maxWordCount = 0;
            for (BoxColor color : BoxColor.values()) {
                int wordCount = 0;
                for (String statement : ctx.getInput().byColor(color)) {
                    wordCount += statement.split(" ").length;
                }
                if (wordCount > maxWordCount) {
                    values.clear();
                    maxWordCount = wordCount;
                }
                if (wordCount >= maxWordCount) {
                    values.add(new Value(new Box(color)));
                }
            }
            return constant(new Value(new Group(values)));
        });
        addVariant("THE BOX WITH THE MOST WORDS", (f, ctx) ->
            ctx.assumeQuantity(f, 1));

        addRule(":statement WITH AN ODD NUMBER OF WORDS", "statement", ctx -> {
            return function(Operators.WORD_COUNT(
                "ODD_WORD_COUNT",
                    count -> count % 2 == 1
                ),
                ctx.get("statement")
            ).makeFilter().withQuantifier(ctx.get("statement").getQuantifier());
        });

        addRule("A STATEMENT WITH LESS THAN :number WORDS", "statement", ctx -> {
            int x = ctx.get("number").evaluate(null).asNumber();
            return function(
                Operators.WORD_COUNT(
                    "LESS_THAN_" + x + "_WORDS",
                    count -> count < x
                ),
                formulaAllStatements(ctx)
            ).makeFilter().withQuantifier(Quantifier.any());
        });

        addRule(":statement HAVE LESS THAN :number WORDS.", ctx -> {
            int x = ctx.get("number").evaluate(null).asNumber();
            return function(
                Operators.WORD_COUNT(
                    "LESS_THAN_" + x + "_WORDS",
                    count -> count < x
                ),
                ctx.get("statement")
            );
        });

        addRule("A :number WORD STATEMENT", "statement", ctx -> {
            int x = ctx.get("number").evaluate(null).asNumber();
            return function(
                Operators.WORD_COUNT(
                    "HAS_" + x + "_WORDS",
                    count -> count == x
                ),
                formulaAllStatements(ctx)
            ).makeFilter().withQuantifier(Quantifier.any());
        });

        addRule(":statement CONTAINS :number WORDS.", ctx -> {
            int x = ctx.get("number").evaluate(null).asNumber();
            return function(
                Operators.WORD_COUNT(
                    "HAS_" + x + "_WORDS",
                    count -> count == x
                ),
                ctx.get("statement")
            );
        });

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
        addAlias(":box CONTAINS :statement.");

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
        addAlias(":box DOES NOT HAVE GEMS.");
        addAlias(":box IS EMPTY.");
        addAlias(":box ARE EMPTY.");
        // A slight stretch, the inverse is taken to be
        // "YOU WILL OPEN :box AND FIND IT NOT EMPTY", i.e. it contains the gems.
        addAlias("YOU WILL OPEN :box AND FIND IT EMPTY.");
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
        addAlias(":box IS :bool AND IT CONTAINS GEMS.");

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
        addRule(":box IS EMPTY IF IT IS :bool.", ctx -> {
            return function(Operators.IMPLIES,
                function(Operators.BOX_IS,
                    ctx.get("box"),
                    ctx.get("bool")
                ),
                function(Operators.NOT,
                    function(Operators.BOX_HAS_GEMS, ctx.get("box"))
                )
            );
        });

        // Statements about statements

        addRule(":statement IS :bool.", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool), ctx.get("statement"));
        });
        addAlias(":statement ARE :bool.");
        addVariant(":statement IS ALWAYS :bool.", ctx -> {
            ctx.get("statement").withQuantifier(Quantifier.all());
        });
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

        addRule(":statement ARE EITHER BOTH :bool OR BOTH :bool.", ctx -> {
            boolean b1 = ctx.get("bool", 1).evaluate(null).asBoolean();
            boolean b2 = ctx.get("bool", 2).evaluate(null).asBoolean();
            return function(Operators.OR,
                function(operatorStatementIs(b1),
                    ctx.get("statement").withQuantifier(Quantifier.all())
                ),
                function(operatorStatementIs(b2),
                    ctx.get("statement").withQuantifier(Quantifier.all())
                )
            );
        });

        addRule(":statement IS ON :box.", ctx -> {
            return function(Operators.EQUALS,
                function(Operators.BOX_OF_STATEMENT, ctx.get("statement")),
                ctx.get("box")
            );
        });

        addRule(":statement HAVE IDENTICAL WORDING.", ctx -> {
            return function(Operators.STATEMENTS_MATCH_GROUP, ctx.get("statement"));
        });

        addRule("THIS IS :statement.", ctx -> {
            return function(Operators.EQUALS,
                formulaThisStatement(ctx),
                ctx.get("statement")
            );
        });

        addRule("THAT IS :bool.", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(bool), constant(new Value(ctx.getLastStatement())));
        });
        addRule("THAT'S NOT :bool.", ctx -> {
            boolean bool = ctx.get("bool").evaluate(null).asBoolean();
            return function(operatorStatementIs(!bool), constant(new Value(ctx.getLastStatement())));
        });

        // Oddballs

        addRule("THERE ARE AN EQUAL NUMBER OF TRUE AND FALSE STATEMENTS.", ctx -> {
            return function(Operators.EQUALS,
                function(Operators.GROUP_SIZE,
                    function(operatorStatementIs(true),
                        formulaAllStatements(ctx)
                    ).makeFilter()
                ),
                function(Operators.GROUP_SIZE,
                    function(operatorStatementIs(false),
                        formulaAllStatements(ctx)
                    ).makeFilter()
                )
            );
        });
        addAlias("THERE ARE THE SAME NUMBER OF TRUE AND FALSE STATEMENTS ON BOXES.");

        addRule("THERE IS NO BOX THAT DISPLAYS A NUMBER THAT ALSO APPEARS ON ANOTHER BOX.", ctx -> {
            List<Set<Integer>> sets = new ArrayList<>();

            for (BoxColor color : BoxColor.values()) {
                Set<Integer> set = new HashSet<>();
                for (String statement : ctx.getInput().byColor(color)) {
                    for (Token token : Token.tokenize(statement)) {
                        try {
                            set.add(Integer.parseInt(token.getSource()));
                        } catch (NumberFormatException _) {}
                    }
                }
                sets.add(set);
            }

            for (int i = 0; i < sets.size(); i++) {
                for (int j = i + 1; j < sets.size(); j++) {
                    Set<Integer> set = sets.get(i);
                    set.retainAll(sets.get(j));
                    if (!set.isEmpty()) {
                        return constant(new Value(false));
                    }
                }
            }

            return constant(new Value(true));
        });

        addRule("A BOX MENTIONING A SPECIFIC COLOR", "box", ctx -> {
            List<Value> values = new ArrayList<>();
            for (BoxColor color : BoxColor.values()) {
                if (boxMentionsColor(ctx, color)) {
                    values.add(new Value(new Box(color)));
                }
            }
            return constant(new Value(new Group(values)))
                .withQuantifier(Quantifier.any());
        });

        addRule("THERE IS NO BOX THAT MENTIONS A SPECIFIC COLOR.", ctx -> {
            for (BoxColor color : BoxColor.values()) {
                if (boxMentionsColor(ctx, color)) {
                    return constant(new Value(false));
                }
            }

            return constant(new Value(true));
        });

        addRule("THERE IS NO BOX THAT IS BETWEEN 2 EMPTY BOXES.", ctx -> {
            // This is equivalent to saying the white box is not between two empty boxes,
            // i.e. the white box does not have the gems.
            return function(Operators.NOT,
                function(Operators.BOX_HAS_GEMS, constant(new Value(new Box(BoxColor.WHITE))))
            );
        });

        addRule("A BOX WITH A STATEMENT THAT IS ALSO ON ANOTHER BOX", "box", ctx -> {
            List<Statement> statements = new ArrayList<>();
            for (BoxColor color : BoxColor.values()) {
                for (int i = 1; i <= ctx.getInput().byColor(color).size(); i++) {
                    statements.add(new Statement(new Box(color), i));
                }
            }

            Set<Value> values = new HashSet<>();

            for (int i = 0; i < statements.size(); i++) {
                for (int j = i + 1; j < statements.size(); j++) {
                    String a = ctx.getInput().textOfStatement(statements.get(i));
                    String b = ctx.getInput().textOfStatement(statements.get(j));
                    if (statements.get(i).box().equals(statements.get(j).box())) {
                        continue;
                    }
                    if (a.equals(b)) {
                        values.add(new Value(statements.get(i).box()));
                        values.add(new Value(statements.get(j).box()));
                    }
                }
            }

            return constant(new Value(new Group(values.stream().toList())))
                .withQuantifier(Quantifier.any());
        });

        // Special case because it is not literally referring to "this statement"
        // but instead a statement with the same text as this statement
        addRule("THIS STATEMENT APPEARS ON ANOTHER BOX.", ctx -> {
            return function(Operators.STATEMENTS_MATCH,
                function(Operators.STATEMENTS_ON_BOX,
                    formulaOtherBoxes(ctx)).withQuantifier(Quantifier.any()
                ),
                formulaThisStatement(ctx)
            );
        });

        addRule("THEY ARE BOTH :bool.", ctx -> {
            if (!ctx.getInput().textOfStatement(ctx.getLastStatement())
                .equals("THERE ARE TWO STATEMENTS ON THIS BOX.")) {
                throw new RuntimeException("Pronouns aren't fully supported");
            }
            // Both statements on this box are false, i.e. this box is false.
            return function(Operators.BOX_IS,
                formulaThisBox(ctx),
                constant(new Value(false))
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

        boolean modified = false;

        for (int i = 0; i < rules.size(); i++) {
            for (int j = i + 1; j < rules.size(); j++) {
                ParseRule e1 = rules.get(i);
                ParseRule e2 = rules.get(j);

                if (e1.matches(e2.getPattern()) != -1) {
                    modified = true;
                    rules.set(i, e2);
                    rules.set(j, e1);
                }
            }
        }

        if (modified) {
            sortRules();
        }

        for (int i = 0; i < rules.size(); i++) {
            // This rule specifically needs to be checked last
            // because of how easily it can break other rules
            if (Token.stringify(rules.get(i).getPattern()).equals("STATEMENTS")) {
                rules.add(rules.remove(i));
                break;
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

    public static Formula formulaBoxesWithWord(ParseContext ctx, String word) {
        return function(Operators.UNIQUE,
            function(Operators.BOX_OF_STATEMENT,
                function(Operators.STATEMENT_CONTAINS(word, true),
                    formulaAllStatements(ctx)
                ).makeFilter()
            )
        );
    }

    public static String longestWord(ParseContext ctx) {
        String result = "";
        for (String statement : ctx.getInput().allStatements()) {
            for (Token token : Token.tokenize(statement)) {
                String word = token.getSource();
                if (Character.isLetter(word.charAt(0)) && word.length() > result.length()) {
                    result = word;
                }
            }
        }
        return result;
    }

    public static String shortestWord(ParseContext ctx) {
        String result = "definitely not the shortest word";
        for (String statement : ctx.getInput().allStatements()) {
            for (Token token : Token.tokenize(statement)) {
                String word = token.getSource();
                if (Character.isLetter(word.charAt(0)) && word.length() < result.length()) {
                    result = word;
                }
            }
        }
        return result;
    }

    public static boolean boxMentionsColor(ParseContext ctx, BoxColor color) {
        for (String statement : ctx.getInput().byColor(color)) {
            for (Token token : Token.tokenize(statement)) {
                for (BoxColor c : BoxColor.values()) {
                    if (token.getSource().equals(c.name())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
