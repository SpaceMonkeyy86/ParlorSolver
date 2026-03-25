package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.List;
import java.util.function.Function;

public class Functions {
    public static ValueFunction BOX_BY_COLOR = Functions.makeFunction(color -> {
        return new Value(new Box(color.asColor()));
    }, ValueType.COLOR, ValueType.BOX);

    public static ValueFunction BOX_IS_TRUE = Functions.makeFunction(box -> {
        // TODO
        return new Value(true);
    }, ValueType.BOX, ValueType.BOOLEAN);

    private static ValueFunction makeFunction(Function<Value, Value> function, ValueType parameterType, ValueType returnType) {
        Function<List<Value>, Value> func = arguments -> {
            if (arguments.size() != 1) {
                throw new RuntimeException("Invalid arity");
            }

            Value argument = arguments.getFirst();
            if (!argument.isType(parameterType)) {
                throw new RuntimeException("Invalid argument type");
            }

            Value result = function.apply(argument);
            if (!result.isType(returnType)) {
                throw new RuntimeException("Invalid return type");
            }

            return result;
        };

        return new ValueFunction(func, List.of(parameterType), returnType);
    }
}
