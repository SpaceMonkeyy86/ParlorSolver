package com.spacemonkeyy.parlorsolver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleVariation;
import com.spacemonkeyy.parlorsolver.puzzle.ScrapePuzzles;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

// Tests the solver against all puzzle variations
public class VariationTests {
    @Test
    void testSanity() {
        System.out.println("sanity check");
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
            List<DynamicNode> parsingTests = new ArrayList<>();

            parsingTests.add(DynamicTest.dynamicTest("Basic Checks", () -> {
                List<Formula> formulas = Parser.parse(variation.input());
                Assertions.assertNotNull(formulas);
                Assertions.assertEquals(statements.size(), formulas.size());

                for (int i = 0; i < statements.size(); i++) {
                    System.out.printf("%s -> %s\n", statements.get(i), formulas.get(i));
                }
            }));

            for (int i = 0; i < statements.size(); i++) {
                int index = i;
                parsingTests.add(DynamicTest.dynamicTest(statements.get(i), () -> {
                    List<Formula> formulas = Parser.parse(variation.input());
                    Formula formula = formulas.get(index);
                    Assertions.assertNotNull(formula);
                }));
            }

            tests.add(DynamicContainer.dynamicContainer("Parsing", parsingTests));

            tests.add(DynamicTest.dynamicTest("Correct Solution", () -> {
                PuzzleSolution solution = Solver.solve(variation.input());

                Assertions.assertNotNull(solution);
                Assertions.assertEquals(variation.solution().prize(), solution.prize());
            }));

            result.add(DynamicContainer.dynamicContainer(variation.toString(), tests));
        }

        return result;
    }
}
