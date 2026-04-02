package com.spacemonkeyy.parlorsolver;

import com.spacemonkeyy.parlorsolver.puzzle.PuzzleInput;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.solver.Solver;
import com.spacemonkeyy.parlorsolver.value.BoxColor;

import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        Map<BoxColor, List<String>> map = new HashMap<>();

        for (BoxColor color : BoxColor.values()) {
            map.put(color, new ArrayList<>());
            System.out.printf("Enter statements on the %s box:\n", color.name());

            while (true) {
                String line = scanner.nextLine();
                line = line.toUpperCase();
                if (line.isEmpty()) {
                    break;
                }
                map.get(color).add(line);
            }
        }

        PuzzleInput input = new PuzzleInput(
            map.get(BoxColor.BLUE), map.get(BoxColor.WHITE), map.get(BoxColor.BLACK));

        PuzzleSolution solution = Solver.solve(input);
        if (solution == null) {
            System.out.println("No solution found");
        } else {
            System.out.println("The gems are in the " + solution.prize().toString() + " box.");
        }
    }
}