package com.spacemonkeyy.parlorsolver.value;

import java.util.List;

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

    public Value(String string) {
        type = ValueType.STRING;
        value = string;
    }

    public Value(Box box) {
        type = ValueType.BOX;
        value = box;
    }

    public Value(Statement statement) {
        type = ValueType.STATEMENT;
        value = statement;
    }

    public ValueType getType() {
        return type;
    }

    public boolean isType(ValueType type) {
        return this.type == type;
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

    public String asString() {
        return (String)value;
    }

    public Box asBox() {
        return (Box)value;
    }

    public Statement asStatement() {
        return (Statement)value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
