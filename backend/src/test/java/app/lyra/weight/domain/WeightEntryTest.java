package app.lyra.weight.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeightEntryTest {

    private static final LocalDate DATE = LocalDate.of(2026, 11, 19);

    @ParameterizedTest
    @ValueSource(strings = {"20", "82.4", "82.45", "82.450", "500"})
    void acceptsBetweenTwentyAndFiveHundredKilogramsWithUpToTwoDecimals(String kilograms) {
        WeightEntry entry = new WeightEntry(DATE, new BigDecimal(kilograms));

        assertThat(entry.date()).isEqualTo(DATE);
        assertThat(entry.kilograms()).isEqualByComparingTo(kilograms);
    }

    @ParameterizedTest
    @ValueSource(strings = {"19.99", "500.01", "0", "-80", "8240"})
    void refusesAWeightOutsideTheLimits(String kilograms) {
        assertThatThrownBy(() -> new WeightEntry(DATE, new BigDecimal(kilograms)))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Weight must be between 20 and 500 kilograms");
    }

    @Test
    void refusesMoreThanTwoDecimals() {
        assertThatThrownBy(() -> new WeightEntry(DATE, new BigDecimal("82.455")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Weight must have at most two decimal places");
    }

    @Test
    void requiresTheKilograms() {
        assertThatThrownBy(() -> new WeightEntry(DATE, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("kilograms is required");
    }

    @Test
    void requiresTheDate() {
        assertThatThrownBy(() -> new WeightEntry(null, new BigDecimal("82")))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("Weight date is required");
    }
}
