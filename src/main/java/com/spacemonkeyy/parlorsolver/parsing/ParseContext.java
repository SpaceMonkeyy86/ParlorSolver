package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.formula.Operators;
import com.spacemonkeyy.parlorsolver.formula.Quantifier;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.value.Box;
import com.spacemonkeyy.parlorsolver.value.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Information used in rules to generate formulas
public class ParseContext {
    // The input to the puzzle
    private final PuzzleInput input;

    // Intermediate tokens that were matched as part of the rule
    private final Map<String, List<Token>> bindings;

    // Current statement info
    private Statement currentStatement;
    private Statement lastStatement;
    private List<Formula> assumptions;

    public ParseContext(PuzzleInput input) {
        this.input = input;
        bindings = new HashMap<>();
    }

    public PuzzleInput getInput() {
        return input;
    }

    public Statement getCurrentStatement() {
        return currentStatement;
    }

    public Box getCurrentBox() {
        return currentStatement.box();
    }

    public Statement getLastStatement() {
        return lastStatement;
    }

    public void setCurrent(Statement statement) {
        lastStatement = currentStatement;
        currentStatement = statement;
        assumptions = new ArrayList<>();
    }

    public void clearBindings() {
        bindings.clear();
    }

    // Bindings are already matched parts of the string
    // which have a formula and are referenced by an identifier
    public void addBinding(String identifier, Token token) {
        if (!bindings.containsKey(identifier)) {
            bindings.put(identifier, new ArrayList<>());
        }
        bindings.get(identifier).add(token);
    }

    // Accessing existing tokens or formulas inside a rule

    public Token getToken(String identifier) {
        return bindings.get(identifier).getFirst();
    }

    public Token getToken(String identifier, int index) {
        return bindings.get(identifier).get(index - 1);
    }

    public Formula get(String identifier) {
        return getToken(identifier).getFormula();
    }

    public Formula get(String identifier, int index) {
        return getToken(identifier, index).getFormula();
    }

    // Assumptions are additional constraints implied by certain phrases.
    // For example, "BOTH STATEMENTS ON THIS BOX" implies that the box has
    // exactly 2 statements, since the grammar wouldn't make sense otherwise.
    // Assumptions are ANDed together with the main formula before evaluating.
    public void addAssumption(Formula assumption) {
        assumptions.add(assumption);
    }

    public Formula assumeQuantity(Formula formula, int count) {
        return assumeQuantity(formula, Quantifier.exactly(count));
    }

    public Formula assumeQuantity(Formula formula, Quantifier quantifier) {
        addAssumption(Formula.function(Operators.TRIVIAL,
            formula.clone().withQuantifier(quantifier)
        ));
        return formula;
    }

    // Combines the formula with its assumptions.
    public Formula includeAssumptions(Formula formula) {
        for (Formula assumption : assumptions) {
            formula = Formula.function(Operators.AND,
                formula,
                assumption
            );
        }
        return formula;
    }
}
