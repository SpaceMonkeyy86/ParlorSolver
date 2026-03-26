package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class Functions {
    // Basic operations

    public static ValueFunction AND = makeFunction("AND", ctx -> {
        return new Value(ctx.arg(1).asBoolean() && ctx.arg(2).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static ValueFunction NOT = makeFunction("NOT", ctx -> {
        return new Value(!ctx.arg(1).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN);

    // Conversions

    public static ValueFunction BOX_FOR_COLOR = makeFunction("BOX_FOR_COLOR", ctx -> {
        return new Value(new Box(ctx.arg(1).asColor()));
    }, ValueType.COLOR, ValueType.BOX);

    public static ValueFunction COLOR_OF_BOX = makeFunction("COLOR_OF_BOX", ctx -> {
        return new Value(ctx.arg(1).asBox().color());
    }, ValueType.BOX, ValueType.COLOR);

    public static ValueFunction STATEMENT_ON_BOX = makeFunction("STATEMENT_ON_BOX", ctx -> {
        return new Value(new Statement(ctx.arg(1).asBox(), ctx.arg(2).asNumber()));
    }, ValueType.BOX, ValueType.NUMBER, ValueType.STATEMENT);

    // Equality

    public static ValueFunction COLOR_EQUALS = makeFunction("COLOR_EQUALS", ctx -> {
        return new Value(ctx.arg(1).asColor() == ctx.arg(2).asColor());
    }, ValueType.COLOR, ValueType.COLOR, ValueType.BOOLEAN);

    public static ValueFunction NUMBER_EQUALS = makeFunction("NUMBER_EQUALS", ctx -> {
        return new Value(ctx.arg(1).asNumber() == ctx.arg(2).asNumber());
    }, ValueType.NUMBER, ValueType.NUMBER, ValueType.BOOLEAN);

    public static ValueFunction BOX_EQUALS = makeFunction("BOX_EQUALS", ctx -> {
        return new Value(Objects.equals(ctx.arg(1).asBox(), ctx.arg(2).asBox()));
    }, ValueType.BOX, ValueType.BOX, ValueType.BOOLEAN);

    // Evaluation-dependent

    public static ValueFunction STATEMENT_IS_TRUE = makeFunction("STATEMENT_IS_TRUE", ctx -> {
        return new Value(ctx.isStatementTrue(ctx.arg(1).asStatement()));
    }, ValueType.STATEMENT, ValueType.BOOLEAN);

    // Simple checking without having to know in advance every statement on a box
    public static ValueFunction BOX_IS = makeFunction("BOX_IS", ctx -> {
        Box box = ctx.arg(1).asBox();
        boolean bool = ctx.arg(2).asBoolean();
        int statementCount = ctx.getInput().byColor(box.color()).size();

        for (int i = 0; i < statementCount; i++) {
            if (ctx.isStatementTrue(new Statement(box, i)) != bool) {
                return new Value(false);
            }
        }

        return new Value(true);
    }, ValueType.BOX, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static ValueFunction BOX_HAS_GEMS = makeFunction("BOX_HAS_GEMS", ctx -> {
        return new Value(ctx.boxHasGems(ctx.arg(1).asBox()));
    }, ValueType.BOX, ValueType.BOOLEAN);

    private static ValueFunction makeFunction(String name, Function<EvaluationContext, Value> function, ValueType... signature) {
        List<ValueType> types = Arrays.stream(signature).toList();
        List<ValueType> parameterTypes = types.subList(0, types.size() - 1);
        ValueType returnType = types.getLast();

        Function<EvaluationContext, Value> func = ctx -> {
            if (ctx.argCount() != parameterTypes.size()) {
                throw new RuntimeException("Invalid arity");
            }

            for (int i = 0; i < ctx.argCount(); i++) {
                if (!ctx.arg(i + 1).isType(parameterTypes.get(i))) {
                    throw new RuntimeException("Invalid parameter type");
                }
            }

            Value result = function.apply(ctx);
            if (!result.isType(returnType)) {
                throw new RuntimeException("Invalid return type");
            }

            return result;
        };

        return new ValueFunction(name, func, parameterTypes, returnType);
    }
}
