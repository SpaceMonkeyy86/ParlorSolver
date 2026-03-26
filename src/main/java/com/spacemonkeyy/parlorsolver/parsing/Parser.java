package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Statement;

import java.util.*;

public class Parser {
    public static Map<Statement, Formula> parse(PuzzleInput input) {
        ParseContext context = new ParseContext(input);
        Map<Statement, Formula> result = new HashMap<>();

        for (BoxColor color : BoxColor.values()) {
            Box box = new Box(color);
            List<String> statements = input.byColor(color);
            for (int i = 0; i < statements.size(); i++) {
                context.setCurrent(color, i);

                Statement statement = new Statement(box, i);
                Formula formula = parseStatement(context.getCurrentStatement(), context);
                result.put(statement, formula);
            }
        }

        return result;
    }

    private static Formula parseStatement(String statement, ParseContext context) {
        List<Token> tokens = Token.tokenize(statement);

        while (true) {
            boolean matched = false;
            for (ParseRule rule : Rules.rules) {
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
