package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.parsing.Rules;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Solver {
    public static PuzzleSolution solve(PuzzleInput input) {
        Map<String, Integer> variableNames = new HashMap<>();
        int variableCount = 0;

        for (BoxColor color : BoxColor.values()) {
            Box box = new Box(color);
            variableNames.put(box.getVariableName(), variableCount++);
        }

        for (BoxColor color : BoxColor.values()) {
            Box box = new Box(color);
            List<String> statements = input.byColor(color);
            for (int i = 1; i <= statements.size(); i++) {
                Statement statement = new Statement(box, i);
                variableNames.put(statement.getVariableName(), variableCount++);
            }
        }

        List<Formula> system = new ArrayList<>();

        // Exactly one box contains the gems
        system.add(Rules.formulaExactlyOneBox(box -> Formula.function(Operators.BOX_HAS_GEMS, box)));

        // There is always at least one box with only true statements
        system.add(Rules.formulaAtLeastOneBox(Rules::formulaBoxIsTrue));

        // There is always at least one box with only false statements
        system.add(Rules.formulaAtLeastOneBox(Rules::formulaBoxIsFalse));

        Map<Statement, Formula> map = Parser.parse(input);

        for (Formula formula : map.values()) {
            if (formula == null) {
                // Failed to parse
                return null;
            }
        }

        List<Map.Entry<Statement, Formula>> sorted = map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey())
            .toList();

        for (Map.Entry<Statement, Formula> entry : sorted) {
            Statement statement = entry.getKey();
            Formula formula = entry.getValue();

            // Each statement must be consistent with its truth value
            system.add(Formula.function(Operators.EQUALS,
                formula,
                Formula.function(Operators.STATEMENT_IS_TRUE,
                    Formula.constant(new Value(statement))
                )
            ));
        }

        BoxColor prize = null;

        for (int bitmap = 0; bitmap < 1 << variableCount; bitmap++) {
            List<Boolean> variables = new ArrayList<>();
            int temp = bitmap;
            for (int i = 0; i < variableCount; i++) {
                variables.add((temp & 1) == 1);
                temp >>= 1;
            }

            EvaluationContext ctx = new EvaluationContext(input, variables, variableNames);

            boolean solved = true;
            for (Formula formula : system) {
                Value result = formula.evaluate(ctx);
                if (result.getType() != ValueType.BOOLEAN) {
                    throw new RuntimeException("Invalid equation");
                }
                if (!result.asBoolean()) {
                    solved = false;
                    break;
                }
            }

            if (solved) {
                BoxColor answer = findAnswer(ctx);

                // Multiple variable assignments can be consistent
                // as long as they all have the gems in the same box
                if (prize != null) {
                    if (prize != answer) {
                        // Multiple possible answers; unsolvable
                        return null;
                    }
                } else {
                    prize = answer;
                }
            }
        }

        if (prize == null) {
            // No solution found
            return null;
        }

        return new PuzzleSolution(prize, "");
    }

    private static BoxColor findAnswer(EvaluationContext ctx) {
        for (BoxColor color : BoxColor.values()) {
            Box box = new Box(color);
            if (ctx.getVariable(box.getVariableName())) {
                return color;
            }
        }
        return null;
    }
}
