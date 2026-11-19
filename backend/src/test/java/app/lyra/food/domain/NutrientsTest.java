package app.lyra.food.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NutrientsTest {

    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final BigDecimal NEGATIVE = new BigDecimal("-0.1");

    @Test
    void fiberAndSodiumMayBeUnknown() {
        Nutrients nutrients = new Nutrients(ONE, ONE, ONE, ONE, null, null);

        assertThat(nutrients.fiber()).isNull();
        assertThat(nutrients.sodium()).isNull();
    }

    @Test
    void zeroIsAValidValue() {
        BigDecimal zero = BigDecimal.ZERO;

        assertThat(new Nutrients(zero, zero, zero, zero, zero, zero).kcal()).isZero();
    }

    @Test
    void energyAndMacronutrientsAreRequired() {
        assertThatThrownBy(() -> new Nutrients(null, ONE, ONE, ONE, null, null)).hasMessage("kcal is required");
        assertThatThrownBy(() -> new Nutrients(ONE, null, ONE, ONE, null, null)).hasMessage("protein is required");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, null, ONE, null, null)).hasMessage("carbohydrate is required");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, ONE, null, null, null)).hasMessage("fat is required");
    }

    @Test
    void noValueMayBeNegative() {
        assertThatThrownBy(() -> new Nutrients(NEGATIVE, ONE, ONE, ONE, null, null)).hasMessage("kcal must not be negative");
        assertThatThrownBy(() -> new Nutrients(ONE, NEGATIVE, ONE, ONE, null, null)).hasMessage("protein must not be negative");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, NEGATIVE, ONE, null, null)).hasMessage("carbohydrate must not be negative");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, ONE, NEGATIVE, null, null)).hasMessage("fat must not be negative");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, ONE, ONE, NEGATIVE, null)).hasMessage("fiber must not be negative");
        assertThatThrownBy(() -> new Nutrients(ONE, ONE, ONE, ONE, null, NEGATIVE)).hasMessage("sodium must not be negative");
    }
}
