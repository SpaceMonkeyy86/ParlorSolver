package com.spacemonkeyy.parlorsolver.formula;

import com.spacemonkeyy.parlorsolver.solver.EvaluationContext;
import com.spacemonkeyy.parlorsolver.value.Group;
import com.spacemonkeyy.parlorsolver.value.Value;
import com.spacemonkeyy.parlorsolver.value.ValueType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// A node in the expression tree. Represents calling an operator
// on a set of values to produce a new value, or simply a constant value.
public class Formula implements Cloneable {
    private final Operator operator;
    private final List<Formula> arguments;
    private final Value constant;
    private Quantifier quantifier;
    private boolean filter;

    public static Formula constant(Value constant) {
        return new Formula(null, null, constant, false);
    }

    // A function is any operation that takes values and produces a value.
    // A value could be of many different types, so in first-order logic,
    // this could represent a function, predicate, or connective. This is also
    // how quantifiers are evaluated, though quantifiers belong to the arguments
    // and not the predicate itself in these expressions.
    public static Formula function(Operator operator, Formula... arguments) {
        List<Formula> args = Arrays.stream(arguments).toList();
        return new Formula(operator, args, null, false);
    }

    private Formula(Operator operator, List<Formula> arguments, Value constant, boolean filter) {
        this.operator = operator;
        this.arguments = arguments;
        this.constant = constant;
        this.filter = filter;
    }

    public Quantifier getQuantifier() {
        return quantifier;
    }

    // Indicates that this group represents a bound variable.
    // Quantifiers are used in parent formulas with a boolean operator.
    // Quantifiers are attached to the arguments themselves, and not the predicate,
    // to better match how the English language indicates quantities. The phrases
    // "ANY BOX", "ALL BOXES", "ONLY ONE BOX", and "NO BOX" all represent different
    // quantifiers, but they can all be used in statements like ":box IS TRUE." or
    // ":box IS EMPTY.", ignoring incorrect tenses in the grammar.
    public Formula withQuantifier(Quantifier quantifier) {
        this.quantifier = quantifier;
        return this;
    }

    // The map, flat map, and quantifying operations (i.e. any, all, etc.)
    // can be handled transparently by groups. Filters are different and
    // are a property of the function itself. A filter takes a boolean operator
    // and a group, and returns a new group with only elements for which
    // the operator returned true.
    public Formula makeFilter() {
        if (operator.returnType() != ValueType.BOOLEAN) {
            throw new RuntimeException("Filter must have a boolean operator");
        }
        this.filter = true;
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

        return evaluateHelper(ctx, args, 0, false);
    }

    // Recursively handle each argument. If the argument is a group,
    // it should be expanded, and further evaluation should happen for
    // each element in the group. The quantifier for the corresponding
    // formula is then handled appropriately. The recursive execution
    // effectively pulls each quantifier to the front of the function,
    // with the outermost quantifier coming from the first argument.
    private Value evaluateHelper(EvaluationContext ctx, List<Value> args, int index, boolean didFilter) {
        if (index == args.size()) {
            // All groups have been substituted, so args contains a single value
            // from each. Operator is safe to call now.
            return ctx.call(operator, args);
        }

        if (args.get(index).getType() == ValueType.GROUP
            && operator.parameterTypes().get(index) != ValueType.GROUP) {
            // Evaluate for each member of the group.
            // Represents, a map, flat map, filter, or quantifier operation.

            Group group = args.get(index).asGroup();
            List<Value> list = new ArrayList<>(args);
            List<Value> results = new ArrayList<>();

            for (Value value : group.values()) {
                list.set(index, value);

                Value result = evaluateHelper(ctx, list, index + 1, didFilter || filter);

                if (result.getType() == ValueType.GROUP) {
                    // Flat map to avoid a group of groups, such as
                    // "STATEMENTS ON TRUE BOXES"
                    results.addAll(result.asGroup().values());
                } else {
                    // Regular map operation
                    results.add(result);
                }
            }

            if (operator.returnType() == ValueType.BOOLEAN) {
                // Only filter the first argument, since nested filters wouldn't make any sense
                if (filter && !didFilter) {
                    if (results.size() != group.values().size()) {
                        throw new RuntimeException("Invalid use of groups in a filter");
                    }

                    List<Value> filtered = new ArrayList<>();

                    for (int i = 0; i < group.values().size(); i++) {
                        if (results.get(i).asBoolean()) {
                            filtered.add(group.values().get(i));
                        }
                    }

                    return new Value(new Group(filtered));
                }

                // If no quantifier was given, use the "all" quantifier.
                // This matches nicely with statements like "FALSE BOXES ARE EMPTY."
                // where "FALSE BOXES" has no obvious quantifier to associate with it,
                // but the meaning is that ALL false boxes are empty.
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

        return evaluateHelper(ctx, args, index + 1, didFilter);
    }

    public ValueType getType() {
        if (isConstant()) {
            return constant.getType();
        } else if (filter) {
            return ValueType.GROUP;
        } else {
            return operator.returnType();
        }
    }

    @Override
    public Formula clone() {
        try {
            return (Formula) super.clone();
        } catch (CloneNotSupportedException _) {
            throw new RuntimeException("Unreachable");
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (isConstant()) {
            builder.append("const ");
            builder.append(constant);
        } else {
            if (filter) {
                builder.append("FILTER[");
                builder.append(operator.name());
                builder.append("]");
            } else {
                builder.append(operator.name());
            }
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
