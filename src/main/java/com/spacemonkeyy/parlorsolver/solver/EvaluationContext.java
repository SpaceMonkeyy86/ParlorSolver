package com.spacemonkeyy.parlorsolver.solver;

import com.spacemonkeyy.parlorsolver.formula.Operator;
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

    // Sets up arguments list for use in operators, and does type checking.
    public Value call(Operator op, List<Value> args) {
        if (args.size() != op.parameterTypes().size()) {
            throw new RuntimeException("Unexpected number of arguments");
        }

        for (int i = 0; i < args.size(); i++) {
            if (!args.get(i).getType().convertsTo(op.parameterTypes().get(i))) {
                throw new RuntimeException("Wrong argument type");
            }
        }

        this.args = args;
        Value result = op.func().apply(this);

        if (!result.getType().convertsTo(op.returnType())) {
            throw new RuntimeException("Wrong return type");
        }

        return result;
    }

    // Convenience for functions
    public Value arg(int index) {
        return args.get(index - 1);
    }

    public List<Value> args() {
        return args;
    }
}
