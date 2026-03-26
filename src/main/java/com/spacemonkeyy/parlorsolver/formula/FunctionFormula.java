package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.Arrays;
import java.util.List;

// Represents a function applied to one or more values.
public class FunctionFormula implements Formula {
    private final ValueFunction function;
    private final List<Formula> arguments;

    public FunctionFormula(ValueFunction function, Formula... arguments) {
        this.function = function;
        this.arguments = Arrays.stream(arguments).toList();
    }

    @Override
    public Value evaluate(EvaluationContext ctx) {
        List<Value> arguments = this.arguments.stream()
            .map(formula -> formula.evaluate(ctx)).toList();
        ctx.setArgs(arguments);
        return function.func().apply(ctx);
    }

    @Override
    public ValueType getType() {
        return function.returnType();
    }

    @Override
    public String toString() {
        return function.name() + "(" + String.join(", ", arguments.stream().map(Formula::toString).toList()) + ")";
    }
}
