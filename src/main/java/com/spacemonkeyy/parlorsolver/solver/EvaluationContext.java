package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Value;

import java.util.List;
import java.util.Map;

// Information available when evaluating formulas
public class EvaluationContext {
    private final PuzzleInput input;
    private final List<Boolean> variables;
    private final Map<String, Integer> variableNames;
    private List<Value> args;

    public EvaluationContext(PuzzleInput input, List<Boolean> variables, Map<String, Integer> variableNames) {
        this.input = input;
        this.variables = variables;
        this.variableNames = variableNames;
    }

    public PuzzleInput getInput() {
        return input;
    }

    public boolean getVariable(String name) {
        return variables.get(variableNames.get(name));
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
