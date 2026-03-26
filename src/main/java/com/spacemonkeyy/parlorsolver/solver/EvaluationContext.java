package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Statement;
import com.spacemonkeyy.parlorsolver.value.Value;

import java.util.List;
import java.util.Map;

// Information available when evaluating formulas
public class EvaluationContext {
    private final PuzzleInput input;
    private final Map<Statement, Boolean> isTrue;
    private final Map<Box, Boolean> hasGems;
    private List<Value> args;

    public EvaluationContext(PuzzleInput input, Map<Statement, Boolean> isTrue, Map<Box, Boolean> hasGems) {
        this.input = input;
        this.isTrue = isTrue;
        this.hasGems = hasGems;
    }

    public PuzzleInput getInput() {
        return input;
    }

    public boolean isStatementTrue(Statement statement) {
        return isTrue.get(statement);
    }

    public boolean boxHasGems(Box box) {
        return hasGems.get(box);
    }

    // Convenience for functions
    public Value arg(int index) {
        return args.get(index - 1);
    }

    public int argCount() {
        return args.size();
    }

    public void setArgs(List<Value> args) {
        this.args = args;
    }
}
