package app.lyra.food.domain;

import java.math.BigDecimal;

public record Quantity(BigDecimal amount, MeasurementUnit unit) {

    public Quantity {
        if (amount == null || amount.signum() < 0) {
            throw new InvalidFoodException("Quantity amount must be zero or positive");
        }
        if (unit == null) {
            throw new InvalidFoodException("Quantity unit is required");
        }
    }
}
