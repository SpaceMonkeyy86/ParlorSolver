package com.spacemonkeyy.parlorsolver.value;

import java.util.List;

// A homogenous set of values. Can be used in place of
// a single value inside a function or predicate.
public record Group(
    List<Value> values,
    ValueType type
) {
    public Group(List<Value> values) {
        this(values, findType(values));
    }

    private static ValueType findType(List<Value> values) {
        if (values.isEmpty()) {
            return ValueType.ANY;
        }

        ValueType type = values.getFirst().getType();
        for (Value value : values) {
            if (value.getType() != type) {
                throw new RuntimeException("Value type mismatch");
            }
        }

        return type;
    }
}
