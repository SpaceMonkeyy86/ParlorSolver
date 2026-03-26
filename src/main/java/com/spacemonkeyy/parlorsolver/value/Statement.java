package com.spacemonkeyy.parlorsolver.value;

// Uniquely identifies a statement in the puzzle
public record Statement(
    Box box,
    int index
) implements Comparable<Statement> {
    public String getVariableName() {
        return box.getVariableName() + "_" + index;
    }

    @Override
    public int compareTo(Statement o) {
        int result = box.compareTo(o.box);
        if (result != 0) return result;
        return Integer.compare(index, o.index);
    }
}
