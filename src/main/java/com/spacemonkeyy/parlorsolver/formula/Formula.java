package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

public interface Formula {
    // Evaluate the formula.
    Value evaluate(EvaluationContext ctx);

    // Get the type this formula evaluates to.
    ValueType getType();
}
