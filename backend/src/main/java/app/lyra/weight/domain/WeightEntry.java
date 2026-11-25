package app.lyra.weight.domain;

import app.lyra.shared.domain.InvalidInputException;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeightEntry(LocalDate date, BigDecimal kilograms) {

    private static final BigDecimal MINIMUM = new BigDecimal("20");
    private static final BigDecimal MAXIMUM = new BigDecimal("500");
    private static final int MAXIMUM_DECIMAL_PLACES = 2;

    public WeightEntry {
        if (date == null) {
            throw new InvalidInputException("Weight date is required");
        }
        if (kilograms == null) {
            throw new InvalidInputException("kilograms is required");
        }
        if (kilograms.compareTo(MINIMUM) < 0 || kilograms.compareTo(MAXIMUM) > 0) {
            throw new InvalidInputException("Weight must be between 20 and 500 kilograms");
        }
        if (kilograms.stripTrailingZeros().scale() > MAXIMUM_DECIMAL_PLACES) {
            throw new InvalidInputException("Weight must have at most two decimal places");
        }
    }
}
