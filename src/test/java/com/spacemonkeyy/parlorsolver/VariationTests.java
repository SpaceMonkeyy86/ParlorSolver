package com.spacemonkeyy.parlorsolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleVariation;
import com.spacemonkeyy.parlorsolver.solver.Solver;
import com.spacemonkeyy.parlorsolver.value.Statement;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Tests the solver against all puzzle variations
public class VariationTests {
    @Test
    void testSanity() {
        // Makes sure JUnit is set up correctly
        System.out.println("sanity check");
    }

    @Test
    void testNoMissedVariations() throws IOException {
        // Make sure we aren't accidentally skipping any while scraping
        String raw = Files.readString(ScrapePuzzles.wikiPagePath);
        String pattern = "{{ParlorTableRow";

        int index = 0;
        int count = 0;
        while (true) {
            index = raw.indexOf(pattern, index);
            if (index == -1) break;
            index += pattern.length();
            count++;
        }

        String json = Files.readString(ScrapePuzzles.outputPath);
        Gson gson = new GsonBuilder().create();
        PuzzleVariation[] variations = gson.fromJson(json, PuzzleVariation[].class);

        Assertions.assertEquals(count, variations.length);
    }

    @TestFactory
    List<DynamicNode> testVariations() throws IOException {
        String json = Files.readString(ScrapePuzzles.outputPath);
        Gson gson = new GsonBuilder().create();
        PuzzleVariation[] variations = gson.fromJson(json, PuzzleVariation[].class);

        List<DynamicNode> result = new ArrayList<>();

        for (PuzzleVariation variation : variations) {
            List<String> statements = variation.input().allStatements();

            List<DynamicNode> tests = new ArrayList<>();

            for (int i = 0; i < statements.size(); i++) {
                int index = i;
                tests.add(DynamicTest.dynamicTest(statements.get(i), () -> {
                    Map<Statement, Formula> formulas = Parser.parse(variation.input());

                    Map.Entry<Statement, Formula> entry = formulas.entrySet().stream()
                        .sorted(Map.Entry.comparingByKey())
                        .toList().get(index);
                    Statement statement = entry.getKey();
                    Formula formula = entry.getValue();

                    System.out.printf("(%s %d) %s -> %s\n",
                        statement.box().color(),
                        statement.index(),
                        statements.get(index),
                        formula
                    );

                    Assertions.assertNotNull(formula);
                }));
            }

            tests.add(DynamicTest.dynamicTest("Correct Solution", () -> {
                PuzzleSolution solution = Solver.solve(variation.input());

                Assertions.assertNotNull(solution);
                System.out.println(solution);
                Assertions.assertEquals(variation.solution().prize(), solution.prize());
                System.out.println(variation.solution().description());
            }));

            result.add(DynamicContainer.dynamicContainer(variation.toString(), tests));
        }

        return result;
    }
}
