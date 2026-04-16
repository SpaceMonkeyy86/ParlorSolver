package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Statement;

import java.util.*;

public class Parser {
    private static final boolean DEBUG = false;

    public static Map<Statement, Formula> parse(PuzzleInput input) {
        ParseContext context = new ParseContext(input);
        Map<Statement, Formula> result = new HashMap<>();

        for (BoxColor color : BoxColor.values()) {
            List<String> statements = input.byColor(color);
            for (int i = 1; i <= statements.size(); i++) {
                Statement statement = new Statement(new Box(color), i);
                context.setCurrent(statement);

                String text = context.getInput().textOfStatement(statement);
                Formula formula = parseStatement(text, context);

                if (formula != null) {
                    formula = context.includeAssumptions(formula);
                }

                result.put(statement, formula);
            }
        }

        return result;
    }

    // Parsing works by matching declarative parse rules against the tokenized statement.
    // Each rule consumes a set of tokens and produces a new token with a formula.
    // Formulas can then find that intermediate token by its identifier; for instance,
    // a formula representing a box would have the identifier "box". Formulas are combined
    // in a bottom-up approach until there is one token left with the fully parsed formula.
    public static Formula parseStatement(String statement, ParseContext context) {
        List<Token> tokens = Token.tokenize(statement);
        if (DEBUG) {
            System.out.println(Token.stringify(tokens));
        }

        while (true) {
            boolean matched = false;
            for (ParseRule rule : Rules.rules) {
                if (rule.tryMatch(tokens, context)) {
                    if (DEBUG) {
                        System.out.printf("Matched %s\n", rule);
                        System.out.println(Token.stringify(tokens));
                    }
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
