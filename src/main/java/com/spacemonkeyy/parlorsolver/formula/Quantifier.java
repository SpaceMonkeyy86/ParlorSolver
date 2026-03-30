package com.spacemonkeyy.parlorsolver.formula;

// When a group is tested against a predicate,
// specifies how many elements should pass in
// order for the entire test to pass. Traditionally
// "all" or "any", but can also be "none", a specific
// number, or a specific range of numbers.
public class Quantifier {
    private final int min;
    private final int max;
    private final boolean inverted;

    public static Quantifier all() {
        // All elements must pass
        return new Quantifier(-1, 0, true);
    }

    public static Quantifier any() {
        // At least one element must pass
        return new Quantifier(1, -1, false);
    }

    public static Quantifier exactly(int count) {
        return new Quantifier(count, count, false);
    }

    public static Quantifier atLeast(int count) {
        return new Quantifier(count, -1, false);
    }

    public static Quantifier atMost(int count) {
        return new Quantifier(-1, count, false);
    }

    private Quantifier(int min, int max, boolean inverted) {
        this.min = min;
        this.max = max;
        this.inverted = inverted;
    }

    public boolean test(int passed, int total) {
        if (inverted) {
            passed = total - passed;
        }

        if (min != -1 && passed < min) {
            return false;
        }

        if (max != -1 && passed > max) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        if (inverted) {
            if (min == -1 && max == 0) {
                return "ALL";
            }
            return "!" + min + "-" + max;
        } else {
            if (max == -1) {
                if (min == 1) {
                    return "ANY";
                }
                return "MIN " + min;
            }
            if (min == -1) {
                return "MAX " + min;
            }
            if (min == max) {
                return "EXACTLY " + min;
            }
            return min + "-" + max;
        }
    }
}
