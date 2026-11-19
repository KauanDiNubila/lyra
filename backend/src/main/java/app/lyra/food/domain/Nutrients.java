package app.lyra.food.domain;

import java.math.BigDecimal;

public record Nutrients(
        BigDecimal kcal,
        BigDecimal protein,
        BigDecimal carbohydrate,
        BigDecimal fat,
        BigDecimal fiber,
        BigDecimal sodium) {

    public Nutrients {
        requirePresentAndNotNegative(kcal, "kcal");
        requirePresentAndNotNegative(protein, "protein");
        requirePresentAndNotNegative(carbohydrate, "carbohydrate");
        requirePresentAndNotNegative(fat, "fat");
        requireNotNegative(fiber, "fiber");
        requireNotNegative(sodium, "sodium");
    }

    private static void requirePresentAndNotNegative(BigDecimal value, String name) {
        if (value == null) {
            throw new InvalidInputException(name + " is required");
        }
        requireNotNegative(value, name);
    }

    private static void requireNotNegative(BigDecimal value, String name) {
        if (value != null && value.signum() < 0) {
            throw new InvalidInputException(name + " must not be negative");
        }
    }
}
