package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.List;
import java.util.function.Function;

// A callable function that works on values.
public record Operator(
    String name,
    Function<EvaluationContext, Value> func,
    List<ValueType> parameterTypes,
    ValueType returnType
) {
    public static Operator compose(Operator first, Operator second) {
        if (second.parameterTypes.size() != 1) {
            throw new RuntimeException("Operator arity mismatch");
        }
        if (second.parameterTypes.getFirst() != first.returnType) {
            throw new RuntimeException("Return type must match parameter type");
        }

        Function<EvaluationContext, Value> func = ctx -> {
            Value value = ctx.call(first, ctx.args());
            return ctx.call(second, List.of(value));
        };

        return new Operator(
            second.name + "." + first.name,
            func,
            first.parameterTypes,
            second.returnType
        );
    }
}
