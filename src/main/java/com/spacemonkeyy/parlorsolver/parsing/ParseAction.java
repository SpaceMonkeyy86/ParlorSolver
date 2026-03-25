package com.spacemonkeyy.parlorsolver.parsing;

import com.spacemonkeyy.parlorsolver.formula.Formula;

@FunctionalInterface
public interface ParseAction {
    Formula apply(ParseContext context);
}
