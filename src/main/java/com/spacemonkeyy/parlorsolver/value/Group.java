package com.spacemonkeyy.parlorsolver.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

// A homogenous set of values. Can be used in place of
// a single value inside a function. Inside a regular
// function, maps each element in the group using that
// function. Inside a predicate, acts as a quantifier
// and checks whether one or all elements pass the test.
public class Group {
    private final List<Value> values;
    private final ValueType type;
    private final Quantifier quantifier;

    public Group(List<Value> values, Quantifier quantifier) {
        this.values = values;
        this.quantifier = quantifier;
        if (values.isEmpty()) {
            type = null;
        } else {
            type = values.getFirst().getType();
            for (Value value : values) {
                if (value.getType() != type) {
                    throw new RuntimeException("Value type mismatch");
                }
            }
        }
    }

    public boolean typeCheck(ValueType type) {
        if (this.type == null) return true;
        return this.type == type;
    }

    public Value apply(Function<Value, Value> func) {
        List<Value> results = new ArrayList<>();
        for (Value value : values) {
            results.add(func.apply(value));
        }
        return new Value(new Group(results, quantifier));
    }

    public boolean test(Predicate<Value> predicate) {
        return switch (quantifier) {
            case FORALL -> testForall(predicate);
            case EXISTS -> testExists(predicate);
        };
    }

    private boolean testForall(Predicate<Value> predicate) {
        for (Value value : values) {
            if (!predicate.test(value)) {
                return false;
            }
        }
        return true;
    }

    private boolean testExists(Predicate<Value> predicate) {
        for (Value value : values) {
            if (predicate.test(value)) {
                return true;
            }
        }
        return false;
    }
}
