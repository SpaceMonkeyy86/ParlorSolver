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

    public static ValueFunction AND = makeFunction("AND", (bool1, bool2) -> {
        return new Value(bool1.asBoolean() && bool2.asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static ValueFunction NOT = makeFunction("NOT", bool -> {
        return new Value(!bool.asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN);

    // Conversions

    public static ValueFunction BOX_OF_COLOR = makeFunction("BOX_OF_COLOR", color -> {
        return new Value(new Box(color.asColor()));
    }, ValueType.COLOR, ValueType.BOX);

    public static ValueFunction COLOR_OF_BOX = makeFunction("COLOR_OF_BOX", box -> {
        return new Value(box.asBox().color());
    }, ValueType.BOX, ValueType.COLOR);

    // Equality

    public static ValueFunction COLOR_EQUALS = makeFunction("COLOR_EQUALS", (color1, color2) -> {
        return new Value(color1.asColor() == color2.asColor());
    }, ValueType.COLOR, ValueType.COLOR, ValueType.BOOLEAN);

    public static ValueFunction NUMBER_EQUALS = makeFunction("NUMBER_EQUALS", (number1, number2) -> {
        return new Value(number1.asNumber() == number2.asNumber());
    }, ValueType.NUMBER, ValueType.NUMBER, ValueType.BOOLEAN);

    public static ValueFunction BOX_EQUALS = makeFunction("BOX_EQUALS", (box1, box2) -> {
        return new Value(Objects.equals(box1.asBox(), box2.asBox()));
    }, ValueType.BOX, ValueType.BOX, ValueType.BOOLEAN);

    public static ValueFunction BOX_IS_TRUE = makeFunction("BOX_IS_TRUE", box -> {
        // TODO
        return new Value(false);
    }, ValueType.BOX, ValueType.BOOLEAN);

    public static ValueFunction BOX_IS_FALSE = makeFunction("BOX_IS_FALSE", box -> {
        // TODO
        return new Value(false);
    }, ValueType.BOX, ValueType.BOOLEAN);

    public static ValueFunction BOX_HAS_GEMS = makeFunction("BOX_HAS_GEMS", box -> {
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
