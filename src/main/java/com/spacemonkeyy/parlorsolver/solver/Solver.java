package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.parsing.Rules;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.*;

public class Solver {
    public static BoxColor solve(PuzzleInput input) {
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

        List<Integer> solutions = new ArrayList<>();

        // Test every combination of variables against the system
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
                solutions.add(bitmap);
            }
        }

        // If all solutions agree on the box with the gems, that is the answer
        BoxColor prize = findAnswer(solutions);
        if (prize != null) {
            return prize;
        }

        // Fallback strategies to eliminate extra solutions

        prize = symmetryFallback(input, solutions);
        if (prize != null) {
            return prize;
        }

        prize = gemHintFallback(input, solutions);
        if (prize != null) {
            return prize;
        }

        return null;
    }

    private static BoxColor findAnswer(List<Integer> solutions) {
        BoxColor prize = null;

        for (int solution : solutions) {
            // The location of the gems in this variable assignment
            BoxColor answer = null;

            for (BoxColor color : BoxColor.values()) {
                if ((solution & 1) == 1) {
                    answer = color;
                    break;
                }
                solution >>= 1;
            }

            if (prize == null) {
                prize = answer;
            } else {
                if (prize != answer) {
                    return null;
                }
            }
        }

        return prize;
    }

    private static BoxColor symmetryFallback(PuzzleInput input, List<Integer> solutions) {
        // If two boxes have equivalent statements,
        // they must have equal truth value by symmetry.
        // (Unless they refer to neighboring boxes, which is not handled.)
        // Only variation 50 needs this.

        // Look for two boxes with equivalent statements
        BoxColor color1 = null;
        BoxColor color2 = null;
        for (BoxColor a : BoxColor.values()) {
            for (BoxColor b : BoxColor.values()) {
                if (a == b) {
                    continue;
                }

                List<String> list1 = input.byColor(a);
                List<String> list2 = input.byColor(b);

                if (new HashSet<>(list1).containsAll(list2)
                    && new HashSet<>(list2).containsAll(list1)) {
                    color1 = a;
                    color2 = b;
                    break;
                }
            }
        }

        if (color1 == null) {
            // No solution found
            return null;
        }

        // Remove symmetrical solutions
        for (int i = 0; i < solutions.size(); i++) {
            for (int j = i + 1; j < solutions.size(); j++) {
                if (swapColors(solutions.get(i), color1, color2, input) == solutions.get(j)) {
                    solutions.remove(i);
                    solutions.remove(j - 1);
                    i--;
                    break;
                }
            }
        }

        return findAnswer(solutions);
    }

    private static int swapColors(int bitmap, BoxColor color1, BoxColor color2, PuzzleInput input) {
        // Swap truth values of statements on these boxes

        int offset1 = offsetOfColor(color1, input);
        int offset2 = offsetOfColor(color2, input);

        int size = input.byColor(color1).size();

        int mask1 = ((1 << size) - 1) << offset1;
        int mask2 = ((1 << size) - 1) << offset2;

        int component1 = (bitmap & mask1) >> offset1;
        int component2 = (bitmap & mask2) >> offset2;

        bitmap &= ~mask1;
        bitmap &= ~mask2;

        bitmap |= (component1 << offset2);
        bitmap |= (component2 << offset1);

        // Also swap location of gems

        offset1 = indexOfColor(color1);
        offset2 = indexOfColor(color2);

        mask1 = 1 << offset1;
        mask2 = 1 << offset2;

        component1 = (bitmap & mask1) >> offset1;
        component2 = (bitmap & mask2) >> offset2;

        bitmap &= ~mask1;
        bitmap &= ~mask2;

        bitmap |= (component1 << offset2);
        bitmap |= (component2 << offset1);

        return bitmap;
    }

    private static int offsetOfColor(BoxColor color, PuzzleInput input) {
        int offset = BoxColor.values().length;
        for (BoxColor c : BoxColor.values()) {
            if (c == color) {
                return offset;
            }
            offset += input.byColor(c).size();
        }
        return -1;
    }

    private static int indexOfColor(BoxColor color) {
        int index = 0;
        for (BoxColor c : BoxColor.values()) {
            if (c == color) {
                return index;
            }
            index++;
        }
        return -1;
    }

    private static BoxColor gemHintFallback(PuzzleInput input, List<Integer> solutions) {
        // If only one statement suggests where the gems could be, that rules out
        // some solutions by symmetry. If it says that the gems are in
        // a specific box, the gems must be in that box no matter what, since
        // otherwise it would be impossible to choose between the other two boxes.
        // Only variation 39 needs this.

        Statement gemHinter = null;
        for (BoxColor color : BoxColor.values()) {
            for (int i = 1; i <= input.byColor(color).size(); i++) {
                Statement statement = new Statement(new Box(color), i);
                String text = input.textOfStatement(statement);

                // It would probably be smarter to look for usage of
                // BOX_HAS_GEMS in the corresponding formula, but this works too
                if (text.contains("GEMS") || text.contains("EMPTY")) {
                    if (gemHinter != null) {
                        return null;
                    }
                    gemHinter = statement;
                }
            }
        }

        // Puzzle would be unsolvable otherwise
        assert gemHinter != null;

        // We need to choose if this box is true or false. If one
        // option leads to a single solution and the other doesn't,
        // we choose that option.

        int index = offsetOfColor(gemHinter.box().color(), input) + gemHinter.index() - 1;

        List<Integer> trueSolutions = new ArrayList<>();
        List<Integer> falseSolutions = new ArrayList<>();

        for (int solution : solutions) {
            if (((solution >> index) & 1) != 0) {
                trueSolutions.add(solution);
            } else {
                falseSolutions.add(solution);
            }
        }

        BoxColor trueAnswer = findAnswer(trueSolutions);
        BoxColor falseAnswer = findAnswer(falseSolutions);

        if (trueAnswer != null && falseAnswer == null) {
            return trueAnswer;
        }

        if (falseAnswer != null && trueAnswer == null) {
            return falseAnswer;
        }

        return null;
    }
}
