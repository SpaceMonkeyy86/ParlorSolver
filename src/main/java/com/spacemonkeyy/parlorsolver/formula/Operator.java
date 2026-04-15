package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

// A callable function that works on values.
public record Operator(
    String name,
    Function<EvaluationContext, Value> func,
    List<ValueType> parameterTypes,
    ValueType returnType
) {
    // Takes the output of the first function and inputs it to the second.
    // Useful for filters or quantified predicates where pulling a negation
    // outside the quantifier would erroneously change the meaning.
    public static Operator compose(Operator first, Operator second) {
        if (second.parameterTypes.size() != 1) {
            throw new RuntimeException("Operator arity mismatch");
        }
        if (!first.returnType.convertsTo(second.parameterTypes.getFirst())) {
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

    public static Operator compose(Operator... list) {
        Operator result = list[0];
        for (int i = 1; i < list.length; i++) {
            result = compose(result, list[i]);
        }
        return result;
    }

    // Fixes an argument in an operator to be a set formula. The operator
    // can then be called with fewer arguments than originally possible.
    public static Operator apply(Operator operator, int index, Formula argument) {
        if (index <= 0 || index > operator.parameterTypes.size()) {
            throw new RuntimeException("Index out of bounds");
        }
        if (!argument.getType().convertsTo(operator.parameterTypes.get(index - 1))) {
            throw new RuntimeException("Type mismatch");
        }

        Function<EvaluationContext, Value> func = ctx -> {
            List<Value> arguments = new ArrayList<>(ctx.args());
            arguments.add(index - 1, argument.evaluate(ctx));
            return ctx.call(operator, arguments);
        };

        String name = operator.name + "[" + index + "=" + argument + "]";
        List<ValueType> parameterTypes = new ArrayList<>(operator.parameterTypes);
        parameterTypes.remove(index - 1);

        return new Operator(
            name,
            func,
            parameterTypes,
            operator.returnType
        );
    }

    // Flips the argument order of a two-argument operator.
    public static Operator curry(Operator operator) {
        if (operator.parameterTypes.size() != 2) {
            throw new RuntimeException("Operator cannot be curried");
        }

        Function<EvaluationContext, Value> func = ctx -> {
            return ctx.call(operator, List.of(ctx.arg(2), ctx.arg(1)));
        };

        return new Operator(
            operator.name + "_CURRIED",
            func,
            List.of(
                operator.parameterTypes.get(1),
                operator.parameterTypes.get(0)
            ),
            operator.returnType
        );
    }
}
