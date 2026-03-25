package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

// Result of parsing a statement into a formula
public record ParseResult (
    // The original statement
    String statement,

    // Resulting formula
    Formula formula
) {}