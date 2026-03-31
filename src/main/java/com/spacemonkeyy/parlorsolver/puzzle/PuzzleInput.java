package com.spacemonkeyy.parlorsolver.puzzle;

import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Statement;

import java.util.ArrayList;
import java.util.List;

// Input to solver
public record PuzzleInput(
    // Statements on blue box
    List<String> blue,

    // Statements on white box
    List<String> white,

    // Statements on black box
    List<String> black
) {
    public List<String> byColor(BoxColor color) {
        return switch (color) {
            case BLUE -> blue;
            case WHITE -> white;
            case BLACK -> black;
        };
    }

    public List<String> allStatements() {
        List<String> result = new ArrayList<>();
        result.addAll(blue);
        result.addAll(white);
        result.addAll(black);
        return result;
    }

    public String textOfStatement(Statement statement) {
        return byColor(statement.box().color()).get(statement.index() - 1);
    }
}