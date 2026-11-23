package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;

import java.math.BigDecimal;

public record DailyTargets(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {

    public DailyTargets {
        requireZeroOrPositive(kcal, "kcal");
        requireZeroOrPositive(protein, "protein");
        requireZeroOrPositive(carbohydrate, "carbohydrate");
        requireZeroOrPositive(fat, "fat");
    }

    private static void requireZeroOrPositive(BigDecimal value, String name) {
        if (value == null || value.signum() < 0) {
            throw new InvalidInputException("The " + name + " target must be zero or positive");
        }
    }
}
