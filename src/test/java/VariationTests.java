import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.spacemonkeyy.parlorsolver.Solver;
import com.spacemonkeyy.parlorsolver.formula.Formula;
import com.spacemonkeyy.parlorsolver.parsing.Parser;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleSolution;
import com.spacemonkeyy.parlorsolver.puzzle.PuzzleVariation;
import com.spacemonkeyy.parlorsolver.puzzle.ScrapePuzzles;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

// Tests the solver against all puzzle variations
public class VariationTests {
    @Test
    void testSanity() {
        System.out.println("sanity check");
    }

    public static Stream<PuzzleVariation> allVariations() throws IOException {
        String json = Files.readString(ScrapePuzzles.outputPath);
        Gson gson = new GsonBuilder().create();
        PuzzleVariation[] variations = gson.fromJson(json, PuzzleVariation[].class);
        return Arrays.stream(variations);
    }

    @ParameterizedTest
    @MethodSource("allVariations")
    void testParsing(PuzzleVariation variation) {
        List<Formula> result = Parser.parse(variation.input());

        Assertions.assertEquals(variation.input().allStatements().size(), result.size());
        for (Formula formula : result) {
            Assertions.assertNotNull(formula);
            System.out.println(formula);
        }
    }

    @ParameterizedTest
    @MethodSource("allVariations")
    void testCorrectSolution(PuzzleVariation variation) {
        PuzzleSolution solution = Solver.solve(variation.input());

        Assertions.assertNotNull(solution);
        Assertions.assertEquals(variation.solution().prize(), solution.prize());
    }
}
