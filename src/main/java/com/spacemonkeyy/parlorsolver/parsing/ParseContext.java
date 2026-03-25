package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Information used in rules to generate formulas
public class ParseContext {
    // The full input statement
    // TODO: Replace this with PuzzleInput
    private final String statement;
    private final Map<String, List<Formula>> bindings;

    public ParseContext(String statement) {
        this.statement = statement;
        bindings = new HashMap<>();
    }

    public String getStatement() {
        return statement;
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
