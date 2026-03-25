package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

// Represents a value known in advance that does not depend on
// the truth value of any statement or the location of the gems.
public class ConstantFormula implements Formula {
    private final Value value;

    public ConstantFormula(Value value) {
        this.value = value;
    }

    @Override
    public Value evaluate() {
        return value;
    }

    @Override
    public ValueType getType() {
        return value.getType();
    }
}
