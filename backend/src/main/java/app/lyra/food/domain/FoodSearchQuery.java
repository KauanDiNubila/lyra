package app.lyra.food.domain;

import java.util.Arrays;
import java.util.List;

public record FoodSearchQuery(List<String> terms) {

    private static final int MIN_LENGTH = 2;

    public FoodSearchQuery {
        terms = List.copyOf(terms);
    }

    public static FoodSearchQuery of(String rawQuery) {
        String normalized = rawQuery == null ? "" : SearchText.normalize(rawQuery.replaceAll("\\p{Punct}", " "));
        if (normalized.length() < MIN_LENGTH) {
            throw new InvalidInputException("Search query must have at least " + MIN_LENGTH + " characters");
        }
        return new FoodSearchQuery(Arrays.asList(normalized.split(" ")));
    }
}
