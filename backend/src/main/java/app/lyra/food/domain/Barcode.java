package app.lyra.food.domain;

import app.lyra.shared.domain.InvalidInputException;

import java.util.regex.Pattern;

public record Barcode(String value) {

    private static final Pattern DIGITS = Pattern.compile("\\d{8,14}");

    public Barcode {
        value = value == null ? "" : value.trim();
        if (!DIGITS.matcher(value).matches()) {
            throw new InvalidInputException("Barcode must have between 8 and 14 digits");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
