package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.List;
import java.util.function.Function;

// A callable function that works on values.
public record ValueFunction(
    String name,
    Function<EvaluationContext, Value> func,
    List<ValueType> parameterTypes,
    ValueType returnType
) {
}
