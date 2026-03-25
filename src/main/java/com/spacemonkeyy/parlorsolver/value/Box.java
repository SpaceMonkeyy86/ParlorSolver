package com.spacemonkeyy.parlorsolver.value;

// Represents a box. Different from a color because
// a box may refer to its statements, color, or contents,
// while a color does not do these things.
public record Box(
    BoxColor color
) {}
