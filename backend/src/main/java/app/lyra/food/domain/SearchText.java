package app.lyra.food.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class SearchText {

    private SearchText() {
    }

    public static String normalize(String text) {
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }
}
