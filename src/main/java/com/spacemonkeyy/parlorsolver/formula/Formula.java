package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Group;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A node in the expression tree. Represents calling an operator
// on a set of values to produce a new value. Can also represent
// a constant value.
public class Formula {
    private final Operator operator;
    private final List<Formula> arguments;
    private final Value constant;
    private Quantifier quantifier;

    public static Formula constant(Value constant) {
        return new Formula(null, null, constant);
    }

    public static Formula function(Operator operator, Formula... arguments) {
        List<Formula> args = Arrays.stream(arguments).toList();
        return new Formula(operator, args, null);
    }

    private Formula(Operator operator, List<Formula> arguments, Value constant) {
        this.operator = operator;
        this.arguments = arguments;
        this.constant = constant;
    }

    // Indicates that this group represents a bound variable.
    // Quantifiers are used in parent formulas that are functions.
    public Formula withQuantifier(Quantifier quantifier) {
        this.quantifier = quantifier;
        return this;
    }

    public boolean isConstant() {
        return constant != null;
    }

    public Value evaluate(EvaluationContext ctx) {
        if (isConstant()) {
            return constant;
        }

        List<Value> args = arguments.stream()
            .map(formula -> formula.evaluate(ctx))
            .toList();

        return evaluateHelper(ctx, args, 0);
    }

    private Value evaluateHelper(EvaluationContext ctx, List<Value> args, int index) {
        if (index == args.size()) {
            // All groups have been substituted, so args contains a single value
            // from each. Operator is safe to call now.
            return ctx.call(operator, args);
        }

        if (args.get(index).getType() == ValueType.GROUP
            && operator.parameterTypes().get(index) != ValueType.GROUP) {
            // Evaluate for each member of the group,
            // then combine results according to the quantifier.

            Group group = args.get(index).asGroup();
            List<Value> list = new ArrayList<>(args);
            List<Value> results = new ArrayList<>();

            for (Value value : group.values()) {
                list.set(index, value);
                results.add(evaluateHelper(ctx, list, index + 1));
            }

            if (operator.returnType() == ValueType.BOOLEAN) {
                // If no quantifier was given, use the "all" quantifier
                Quantifier quant = arguments.get(index).quantifier;
                if (quant == null) {
                    quant = Quantifier.all();
                }

                int passed = 0;
                int total = 0;

                for (Value result : results) {
                    total++;
                    if (result.asBoolean()) {
                        passed++;
                    }
                }

                return new Value(quant.test(passed, total));
            } else {
                return new Value(new Group(results));
            }
        }

        return evaluateHelper(ctx, args, index + 1);
    }

    public ValueType getType() {
        if (isConstant()) {
            return constant.getType();
        } else {
            return operator.returnType();
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isConstant()) {
            builder.append("const ");
            builder.append(constant);
        } else {
            builder.append(operator.name());
            builder.append("(");
            for (int i = 0; i < arguments.size(); i++) {
                if (arguments.get(i).quantifier != null) {
                    builder.append("<");
                    builder.append(arguments.get(i).quantifier);
                    builder.append(">");
                }
                builder.append(arguments.get(i));
                if (i != arguments.size() - 1) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }

        return builder.toString();
    }
}
