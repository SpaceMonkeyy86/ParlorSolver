package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Functions {
    // Basic operations
    public static ValueFunction NOT = Functions.makeFunction("NOT", bool -> {
        return new Value(!bool.asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN);

    // Conversions
    public static ValueFunction BOX_BY_COLOR = Functions.makeFunction("BOX_BY_COLOR", color -> {
        return new Value(new Box(color.asColor()));
    }, ValueType.COLOR, ValueType.BOX);

    // Equality
    public static ValueFunction BOX_EQUALS = Functions.makeFunction("BOX_EQUALS", (box1, box2) -> {
        return new Value(Objects.equals(box1.asBox(), box2.asBox()));
    }, ValueType.BOX, ValueType.BOX, ValueType.BOOLEAN);

    public static ValueFunction BOX_IS_TRUE = Functions.makeFunction("BOX_IS_TRUE", box -> {
        // TODO
        return new Value(false);
    }, ValueType.BOX, ValueType.BOOLEAN);

    public static ValueFunction BOX_HAS_GEMS = Functions.makeFunction("BOX_HAS_GEMS", box -> {
        // TODO
        return new Value(false);
    }, ValueType.BOX, ValueType.BOOLEAN);

    private static ValueFunction makeFunction(String name, Function<Value, Value> function, ValueType p1, ValueType r) {
        Function<List<Value>, Value> func = arguments -> {
            if (arguments.size() != 1) {
                throw new RuntimeException("Invalid arity");
            }

            Value argument = arguments.get(0);
            if (!argument.isType(p1)) {
                throw new RuntimeException("Invalid argument type");
            }

            Value result = function.apply(argument);
            if (!result.isType(r)) {
                throw new RuntimeException("Invalid return type");
            }

            return result;
        };

        return new ValueFunction(name, func, List.of(p1), r);
    }

    private static ValueFunction makeFunction(String name, BiFunction<Value, Value, Value> function, ValueType p1, ValueType p2, ValueType r) {
        Function<List<Value>, Value> func = arguments -> {
            if (arguments.size() != 2) {
                throw new RuntimeException("Invalid arity");
            }

            Value a1 = arguments.get(0);
            if (!a1.isType(p1)) {
                throw new RuntimeException("Invalid argument type");
            }

            Value a2 = arguments.get(1);
            if (!a2.isType(p2)) {
                throw new RuntimeException("Invalid argument type");
            }

            Value result = function.apply(a1, a2);
            if (!result.isType(r)) {
                throw new RuntimeException("Invalid return type");
            }

            return result;
        };

        return new ValueFunction(name, func, List.of(p1, p2), r);
    }
}
