package com.spacemonkeyy.parlorsolver.puzzle;

import com.spacemonkeyy.parlorsolver.value.BoxColor;

// Output of solver
public record PuzzleSolution(
    // The box with the gems
    BoxColor prize,

    // Explanation of how to solve the puzzle
    String description
) {}
