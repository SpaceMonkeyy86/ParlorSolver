package com.spacemonkeyy.parlorsolver.formula;

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
    public Value evaluate() {
        List<Value> arguments = this.arguments.stream().map(Formula::evaluate).toList();
        return function.func().apply(arguments);
    }

    @Override
    public ValueType getType() {
        return function.returnType();
    }
}
