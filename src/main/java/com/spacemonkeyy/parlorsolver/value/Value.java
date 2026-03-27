package com.spacemonkeyy.parlorsolver.value;

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
        if (type == ValueType.ANY) {
            return true;
        }
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
