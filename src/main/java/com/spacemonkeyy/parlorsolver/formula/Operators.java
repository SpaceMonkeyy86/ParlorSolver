package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

public class Operators {
    // Basic operations

    public static Operator AND = makeOperator("AND", ctx -> {
        return new Value(ctx.arg(1).asBoolean() && ctx.arg(2).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator OR = makeOperator("OR", ctx -> {
        return new Value(ctx.arg(1).asBoolean() || ctx.arg(2).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator AND3 = makeOperator("AND3", ctx -> {
        return new Value(ctx.arg(1).asBoolean() && ctx.arg(2).asBoolean() && ctx.arg(3).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator OR3 = makeOperator("OR3", ctx -> {
        return new Value(ctx.arg(1).asBoolean() || ctx.arg(2).asBoolean() || ctx.arg(3).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator NOT = makeOperator("NOT", ctx -> {
        return new Value(!ctx.arg(1).asBoolean());
    }, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator EQUALS = makeOperator("EQUALS", ctx -> {
        return new Value(ctx.arg(1).equals(ctx.arg(2)));
    }, ValueType.ANY, ValueType.ANY, ValueType.BOOLEAN);

    // Grouping

    public static Operator GROUP = makeOperator("GROUP", ctx -> {
        // TODO: Support adding to existing groups
        return new Value(new Group(List.of(ctx.arg(1), ctx.arg(2))));
    }, ValueType.ANY, ValueType.ANY, ValueType.GROUP);

    public static Operator GROUP_SIZE = makeOperator("GROUP_SIZE", ctx -> {
        return new Value(ctx.arg(1).asGroup().values().size());
    }, ValueType.GROUP, ValueType.NUMBER);

    public static Operator UNIQUE = makeOperator("UNIQUE", ctx -> {
        Group group = ctx.arg(1).asGroup();
        List<Value> result = new ArrayList<>();
        for (Value value : group.values()) {
            boolean duplicate = false;
            for (Value other : result) {
                if (value.equals(other)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                result.add(value);
            }
        }
        return new Value(new Group(result));
    }, ValueType.GROUP, ValueType.GROUP);

    public static Operator NEIGHBORS = makeOperator("NEIGHBORS", ctx -> {
        // Order of boxes is blue, white, black
        Box box = ctx.arg(1).asBox();
        List<Value> neighbors = switch (box.color()) {
            case BLUE, BLACK -> List.of(new Value(new Box(BoxColor.WHITE)));
            case WHITE -> List.of(new Value(new Box(BoxColor.BLUE)), new Value(new Box(BoxColor.BLACK)));
        };
        return new Value(new Group(neighbors));
    }, ValueType.BOX, ValueType.GROUP);

    // Conversions

    public static Operator BOX_FOR_COLOR = makeOperator("BOX_FOR_COLOR", ctx -> {
        return new Value(new Box(ctx.arg(1).asColor()));
    }, ValueType.COLOR, ValueType.BOX);

    public static Operator COLOR_OF_BOX = makeOperator("COLOR_OF_BOX", ctx -> {
        return new Value(ctx.arg(1).asBox().color());
    }, ValueType.BOX, ValueType.COLOR);

    public static Operator STATEMENTS_ON_BOX = makeOperator("STATEMENTS_ON_BOX", ctx -> {
        Box box = ctx.arg(1).asBox();
        List<Value> statements = new ArrayList<>();
        for (int i = 1; i <= ctx.getInput().byColor(box.color()).size(); i++) {
            statements.add(new Value(new Statement(box, i)));
        }
        return new Value(new Group(statements));
    }, ValueType.BOX, ValueType.GROUP);

    public static Operator STATEMENT_ON_BOX = makeOperator("STATEMENT_ON_BOX", ctx -> {
        return new Value(new Statement(ctx.arg(1).asBox(), ctx.arg(2).asNumber()));
    }, ValueType.BOX, ValueType.NUMBER, ValueType.STATEMENT);

    public static Operator BOX_OF_STATEMENT = makeOperator("BOX_OF_STATEMENT", ctx -> {
        return new Value(ctx.arg(1).asStatement().box());
    }, ValueType.STATEMENT, ValueType.BOX);

    // Information on statements

    public static Operator STATEMENTS_MATCH = makeOperator("STATEMENTS_MATCH", ctx -> {
       Statement a = ctx.arg(1).asStatement();
       Statement b = ctx.arg(2).asStatement();
       return new Value(ctx.getInput().textOfStatement(a).equals(ctx.getInput().textOfStatement(b)));
    }, ValueType.STATEMENT, ValueType.STATEMENT, ValueType.BOOLEAN);

    public static Operator STATEMENTS_MATCH_GROUP = makeOperator("STATEMENTS_MATCH_GROUP", ctx -> {
        Group group = ctx.arg(1).asGroup();
        if (group.values().isEmpty()) {
            return new Value(true);
        }

        String first = ctx.getInput().textOfStatement(group.values().getFirst().asStatement());
        for (int i = 1; i < group.values().size(); i++) {
            Statement statement = group.values().get(i).asStatement();
            if (!first.equals(ctx.getInput().textOfStatement(statement))) {
                return new Value(false);
            }
        }
        return new Value(true);
    }, ValueType.GROUP, ValueType.BOOLEAN);

    // Strings are not values so need to be handled like this
    public static Operator STATEMENT_HAS_WORD(String word) {
        return makeOperator("STATEMENT_HAS_WORD(\"" + word + "\")", ctx -> {
            Statement statement = ctx.arg(1).asStatement();
            return new Value(ctx.getInput().textOfStatement(statement).contains(word));
        }, ValueType.STATEMENT, ValueType.BOOLEAN);
    }

    // Helpers

    public static Operator BOX_HAS_STATEMENT = makeOperator("BOX_HAS_STATEMENT", ctx -> {
        return new Value(ctx.arg(2).asStatement().box().equals(ctx.arg(1).asBox()));
    }, ValueType.BOX, ValueType.STATEMENT, ValueType.BOOLEAN);

    // Evaluation variables
    // TODO: Try to have only STATEMENT_IS_TRUE and BOX_HAS_GEMS as primitives

    public static Operator STATEMENT_IS_TRUE = makeOperator("STATEMENT_IS_TRUE", ctx -> {
        Statement statement = ctx.arg(1).asStatement();
        return new Value(ctx.getVariable(statement.getVariableName()));
    }, ValueType.STATEMENT, ValueType.BOOLEAN);

    // Simple checking without having to know in advance every statement on a box
    public static Operator BOX_IS = makeOperator("BOX_IS", ctx -> {
        Box box = ctx.arg(1).asBox();
        boolean bool = ctx.arg(2).asBoolean();

        int statementCount = ctx.getInput().byColor(box.color()).size();
        if (statementCount == 0) {
            // A box with no statements is neither true nor false
            return new Value(false);
        }

        for (int i = 1; i <= statementCount; i++) {
            Statement statement = new Statement(box, i);
            if (ctx.getVariable(statement.getVariableName()) != bool) {
                return new Value(false);
            }
        }

        return new Value(true);
    }, ValueType.BOX, ValueType.BOOLEAN, ValueType.BOOLEAN);

    public static Operator BOX_HAS_GEMS = makeOperator("BOX_HAS_GEMS", ctx -> {
        Box box = ctx.arg(1).asBox();
        return new Value(ctx.getVariable(box.getVariableName()));
    }, ValueType.BOX, ValueType.BOOLEAN);

    // Higher-order operators

    public static Operator FILTER(Operator predicate) {
        if (predicate.returnType() != ValueType.BOOLEAN) {
            throw new RuntimeException("Not a predicate");
        }

        Function<EvaluationContext, Value> func = ctx -> {
            Group group = ctx.arg(1).asGroup();
            List<Value> result = new ArrayList<>();

            for (Value value : group.values()) {
                if (ctx.call(predicate, List.of(value)).asBoolean()) {
                    result.add(value);
                }
            }

            return new Value(new Group(result));
        };

        return new Operator(
            "FILTER(" + predicate.name() + ")",
            func,
            List.of(ValueType.GROUP),
            ValueType.GROUP
        );
    }

    public static Operator ALL(Operator predicate) {
        if (predicate.returnType() != ValueType.BOOLEAN) {
            throw new RuntimeException("Not a predicate");
        }

        Function<EvaluationContext, Value> func = ctx -> {
            Group group = ctx.arg(1).asGroup();
            for (Value value : group.values()) {
                if (ctx.call(predicate, List.of(value)).asBoolean()) {
                    return new Value(false);
                }
            }
            return new Value(true);
        };

        return new Operator(
            "ALL(" + predicate.name() + ")",
            func,
            List.of(ValueType.GROUP),
            ValueType.BOOLEAN
        );
    }

    private static Operator makeOperator(String name, Function<EvaluationContext, Value> func, ValueType... signature) {
        List<ValueType> types = Arrays.stream(signature).toList();
        assert !types.isEmpty();

        List<ValueType> parameterTypes = types.subList(0, types.size() - 1);
        ValueType returnType = types.getLast();

        return new Operator(name, func, parameterTypes, returnType);
    }
}
