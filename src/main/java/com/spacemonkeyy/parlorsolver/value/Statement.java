package com.spacemonkeyy.parlorsolver.value;

// Uniquely identifies a statement in the puzzle
public record Statement(
    Box box,
    int index
) {}
