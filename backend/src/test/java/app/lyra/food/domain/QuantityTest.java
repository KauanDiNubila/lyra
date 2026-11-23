package app.lyra.food.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuantityTest {

    @Test
    void zeroIsAllowedSoASkippedItemCanBeRecorded() {
        assertThat(new Quantity(BigDecimal.ZERO, MeasurementUnit.PORTION).amount()).isZero();
    }

    @Test
    void negativeAmountsAndMissingPartsAreRejected() {
        assertThatThrownBy(() -> new Quantity(new BigDecimal("-1"), MeasurementUnit.GRAM)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Quantity(null, MeasurementUnit.GRAM)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Quantity(BigDecimal.TEN, null)).isInstanceOf(InvalidInputException.class);
    }
}
