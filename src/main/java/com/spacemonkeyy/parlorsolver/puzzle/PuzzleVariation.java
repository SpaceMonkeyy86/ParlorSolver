package com.spacemonkeyy.parlorsolver.puzzle;

// An entry in the puzzles dataset
public record PuzzleVariation(
    // Internal id of puzzle
    int id,

    PuzzleInput input,

    PuzzleSolution solution
) {}