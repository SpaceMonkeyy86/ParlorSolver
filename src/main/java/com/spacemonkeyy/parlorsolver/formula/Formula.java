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
    private final Quantifier quantifier;

    // Information "bubbled up" to parent formulas
    private Quantifier quantifierHint;

    public static Formula constant(Value constant) {
        return new Formula(null, null, constant, null);
    }

    public static Formula function(Operator operator, Formula... arguments) {
        List<Formula> args = Arrays.stream(arguments).toList();

        // Sometimes the indication of a quantifier is present in the expression
        // forming the group and not the predicate itself.
        // For example, with the statement "ONE OF THE OTHER BOXES IS FALSE.",
        // the predicate is ":box IS FALSE.", but the quantifier is hinted to be
        // existential by the rule "ONE OF :group".
        // TODO: Add these rules or update the comment
        // TODO: Consider expanding this to include rules like "TWO OF :group"

        if (operator.returnType() == ValueType.BOOLEAN) {
            Quantifier hint = args.getFirst().quantifierHint;
            if (hint != null) {
                return predicate(operator, hint, arguments);
            }
        }

        return new Formula(operator, args, null, null);
    }

    // Predicates without quantifiers are treated like normal functions
    // A "predicate" in this project refers specifically to a quantified boolean function
    public static Formula predicate(Operator operator, Quantifier quantifier, Formula... arguments) {
        List<Formula> args = Arrays.stream(arguments).toList();
        if (operator.returnType() != ValueType.BOOLEAN) {
            throw new RuntimeException("Predicates must return a boolean");
        }
        return new Formula(operator, args, null, quantifier);
    }

    private Formula(Operator operator, List<Formula> arguments, Value constant, Quantifier quantifier) {
        this.operator = operator;
        this.arguments = arguments;
        this.constant = constant;
        this.quantifier = quantifier;
    }

    public Formula withQuantifierHint(Quantifier hint) {
        quantifierHint = hint;
        return this;
    }

    public boolean isConstant() {
        return constant != null;
    }

    public boolean isPredicate() {
        return quantifier != null;
    }

    public Value evaluate(EvaluationContext ctx) {
        if (isConstant()) {
            return constant;
        }

        List<Value> args = arguments.stream()
            .map(formula -> formula.evaluate(ctx))
            .toList();

        if (args.getFirst().getType() == ValueType.GROUP
            && operator.parameterTypes().getFirst() != ValueType.GROUP) {
            // Evaluate for each member of the group,
            // then combine results according to the quantifier.
            // The group must be the first argument.

            Group group = args.getFirst().asGroup();
            List<Value> list = new ArrayList<>(args);
            List<Value> results = new ArrayList<>();

            for (Value value : group.values()) {
                list.set(0, value);
                results.add(ctx.call(operator, list));
            }

            if (isPredicate()) {
                boolean existential = quantifier == Quantifier.EXISTS;
                for (Value result : results) {
                    if (result.asBoolean() == existential) {
                        return new Value(existential);
                    }
                }
                return new Value(!existential);
            } else {
                return new Value(new Group(results));
            }
        }

        if (isPredicate()) {
            throw new RuntimeException("Predicate requires first argument to be a group");
        }

        return ctx.call(operator, args);
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
            if (isPredicate()) {
                builder.append("<");
                builder.append(quantifier);
                builder.append(">");
            }
            builder.append(operator.name());
            builder.append("(");
            for (int i = 0; i < arguments.size(); i++) {
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
