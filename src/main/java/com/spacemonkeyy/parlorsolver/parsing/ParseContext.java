package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.BoxColor;
import com.spacemonkeyy.parlorsolver.value.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Information used in rules to generate formulas
public class ParseContext {
    // The input to the puzzle
    private final PuzzleInput input;

    // Intermediate formulas that were matched as part of the rule
    private final Map<String, List<Formula>> bindings;

    // Current box and statement index
    private BoxColor currentColor;
    private int currentStatement;

    public ParseContext(PuzzleInput input) {
        this.input = input;
        bindings = new HashMap<>();
    }

    public PuzzleInput getInput() {
        return input;
    }

    public BoxColor getCurrentBox() {
        return currentColor;
    }

    public Statement getCurrentStatement() {
        return new Statement(new Box(currentColor), currentStatement);
    }

    public void setCurrent(BoxColor color, int index) {
        currentColor = color;
        currentStatement = index;
    }

    public void clearBindings() {
        bindings.clear();
    }

    // Bindings are already matched parts of the string
    // which have a formula and are referenced by an identifier
    public void addBinding(String identifier, Formula formula) {
        if (!bindings.containsKey(identifier)) {
            bindings.put(identifier, new ArrayList<>());
        }
        bindings.get(identifier).add(formula);
    }

    public Formula get(String identifier) {
        return bindings.get(identifier).getFirst();
    }

    public Formula get(String identifier, int index) {
        return bindings.get(identifier).get(index - 1);
    }
}
