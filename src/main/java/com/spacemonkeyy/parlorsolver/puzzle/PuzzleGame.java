package com.spacemonkeyy.parlorsolver.puzzle;

import java.util.List;

// A puzzle variation as stored on the Blue Prince wiki
public record PuzzleGame(
    // Internal id of puzzle
    int id,

    // Statements on blue box
    List<String> blue,

    // Statements on white box
    List<String> white,

    // Statements on black box
    List<String> black,

    // The box with the gems
    BoxColor prize,

    // Explanation of how to solve the puzzle as found on the wiki
    String solution
) {}