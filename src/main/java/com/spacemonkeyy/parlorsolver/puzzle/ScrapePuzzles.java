package com.spacemonkeyy.parlorsolver.puzzle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
Pulls all puzzle variations from the Blue Prince wiki.
Takes the contents of the page https://blueprince.wiki.gg/wiki/Parlor_Game/List_of_Parlor_Games,
parsing each listed puzzle into a PuzzleVariation object and exporting the list to puzzles.json.
 */
public class ScrapePuzzles {
    static Path wikiPagePath = Paths.get("puzzles-wiki-page.txt");
    static Path outputPath = Paths.get("puzzles.json");

    public static void main(String[] args) throws Exception {
        String page;
        try {
            page = Files.readString(wikiPagePath);
        } catch (IOException e) {
            page = fetchWikiPage();
        }

        page = page.replaceAll("\n", "");
        page = page.replaceAll("<!--(.*?)-->", "");
        page = page.replaceAll("\\[\\[(.*?)]]", "$1");
        page = page.replaceAll("\\{\\{UpgradeSpoiler\\|[^|]+?\\|(.*?)}}", " $1");

        List<PuzzleVariation> puzzles = new ArrayList<>();

        // Go through ParlorTableRow objects in order
        Pattern pattern = Pattern.compile("^.*?(\\{\\{ParlorTableRow.*?}})(.*)$");
        while (true) {
            Matcher matcher = pattern.matcher(page);
            if (!matcher.matches()) {
                break;
            }

            try {
                String row = matcher.group(1);
                page = matcher.group(2);
                puzzles.add(parseRow(row));
            } catch (Exception _) {}
        }

        Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
        String json = gson.toJson(puzzles);
        Files.writeString(outputPath, json);
    }

    static PuzzleVariation parseRow(String row) throws Exception {
        Pattern pattern = Pattern.compile(
            "^\\{\\{ParlorTableRow"
            + "\\|ID=(.*?)"
            + "\\|Blue=(.*?)"
            + "\\|White=(.*?)"
            + "\\|Black=(.*?)"
            + "\\|Prize=(.*?)"
            + "\\|Solution=(.*?)"
            + "}}$"
        );

        Matcher matcher = pattern.matcher(row);
        if (!matcher.matches()) {
            throw new Exception("Invalid row: " + row);
        }

        int id = Integer.parseInt(matcher.group(1));
        List<String> blue = parseStatements(matcher.group(2));
        List<String> white = parseStatements(matcher.group(3));
        List<String> black = parseStatements(matcher.group(4));
        BoxColor prize = parseColor(matcher.group(5));
        String explanation = matcher.group(6);

        return new PuzzleVariation(id,
            new PuzzleInput(blue, white, black),
            new PuzzleSolution(prize, explanation)
        );
    }

    static List<String> parseStatements(String statements) {
        if (statements.isEmpty()) {
            return List.of();
        }

        List<String> result = new ArrayList<>();
        for (String statement : statements.split("<hr>")) {
            // Normalize quoted words
            statement = statement.replaceAll("'(\\w+)'", "\"$1\"");

            result.add(statement);
        }

        return result;
    }

    static BoxColor parseColor(String color) throws Exception {
        return switch (color) {
            case "Blue" -> BoxColor.BLUE;
            case "White" -> BoxColor.WHITE;
            case "Black" -> BoxColor.BLACK;
            default -> throw new Exception("Invalid box color: " + color);
        };
    }

    static String fetchWikiPage() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .GET()
            .uri(URI.create("https://blueprince.wiki.gg/wiki/Parlor_Game/List_of_Parlor_Games?action=raw"))
            .build();

        String body;
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> handler = client.send(request, HttpResponse.BodyHandlers.ofString());
            body = handler.body();
        }

        Files.writeString(wikiPagePath, body);
        return body;
    }
}
