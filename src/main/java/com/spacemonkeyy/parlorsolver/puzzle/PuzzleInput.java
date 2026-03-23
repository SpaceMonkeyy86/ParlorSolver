package com.spacemonkeyy.parlorsolver.puzzle;

import java.util.List;

// Input to solver
public record PuzzleInput(
    // Statements on blue box
    List<String> blue,

    // Statements on white box
    List<String> white,

    // Statements on black box
    List<String> black
) {}