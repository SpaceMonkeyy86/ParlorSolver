package com.spacemonkeyy.parlorsolver.puzzle;

// Output of solver
public record PuzzleSolution(
    // The box with the gems
    BoxColor prize,

    // Explanation of how to solve the puzzle
    String description
) {}
