package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.parsing.Rules;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.*;

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
            return new PuzzleSolution(prize, "");
        }

        // Fallback: if two boxes have equivalent statements,
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

        prize = findAnswer(solutions);
        if (prize != null) {
            return new PuzzleSolution(prize, "");
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
}
