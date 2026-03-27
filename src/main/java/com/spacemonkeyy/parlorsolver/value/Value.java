package com.spacemonkeyy.parlorsolver.value;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

// An element of the model's base set.
// Can be a box color, a box, a statement, a number, or true/false.
public class Value {
    private final ValueType type;
    private final Object value;

    public Value(boolean bool) {
        type = ValueType.BOOLEAN;
        value = bool;
    }

    public Value(BoxColor color) {
        type = ValueType.COLOR;
        value = color;
    }

    public Value(int number) {
        type = ValueType.NUMBER;
        value = number;
    }

    public Value(Box box) {
        type = ValueType.BOX;
        value = box;
    }

    public Value(Statement statement) {
        type = ValueType.STATEMENT;
        value = statement;
    }

    public Value(Group group) {
        type = ValueType.GROUP;
        value = group;
    }

    public ValueType getType() {
        return type;
    }

    public boolean typeCheck(ValueType type) {
        if (isGroup()) {
            return asGroup().typeCheck(type);
        }
        return this.type == type;
    }

    public Value apply(Function<Value, Value> func) {
        if (isGroup()) {
            return asGroup().apply(func);
        }
        return func.apply(this);
    }

    public boolean test(Predicate<Value> pred) {
        if (isGroup()) {
            return asGroup().test(pred);
        }
        return pred.test(this);
    }

    public boolean isGroup() {
        return type == ValueType.GROUP;
    }

    public boolean asBoolean() {
        return (boolean)value;
    }

    public BoxColor asColor() {
        return (BoxColor)value;
    }

    public int asNumber() {
        return (int)value;
    }

    public Box asBox() {
        return (Box)value;
    }

    public Statement asStatement() {
        return (Statement)value;
    }

    public Group asGroup() {
        return (Group)value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
