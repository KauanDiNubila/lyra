package app.lyra.diary.domain;

import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannedTest {

    private static final Quantity GRAMS = new Quantity(new BigDecimal("150"), MeasurementUnit.GRAM);
    private static final Macros MACROS = new Macros(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);

    @Test
    void keepsWhatWasPlanned() {
        UUID foodId = UUID.randomUUID();

        Planned planned = new Planned(foodId, "Arroz", GRAMS, MACROS, 4);

        assertThat(planned.foodId()).isEqualTo(foodId);
        assertThat(planned.foodName()).isEqualTo("Arroz");
        assertThat(planned.quantity()).isEqualTo(GRAMS);
        assertThat(planned.macros()).isEqualTo(MACROS);
        assertThat(planned.slot()).isEqualTo(4);
    }

    @Test
    void acceptsTheFirstSlot() {
        assertThat(new Planned(UUID.randomUUID(), "Arroz", GRAMS, MACROS, 0).slot()).isZero();
    }

    @Test
    void needsAFoodAQuantityAndTheirMacros() {
        UUID foodId = UUID.randomUUID();

        assertThatThrownBy(() -> new Planned(null, "Arroz", GRAMS, MACROS, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Planned(foodId, "Arroz", null, MACROS, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Planned(foodId, "Arroz", GRAMS, null, 0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A planned item needs a food, a quantity and its macros");
    }

    @Test
    void needsAFoodName() {
        UUID foodId = UUID.randomUUID();

        assertThatThrownBy(() -> new Planned(foodId, null, GRAMS, MACROS, 0)).isInstanceOf(InvalidInputException.class);
        assertThatThrownBy(() -> new Planned(foodId, "  ", GRAMS, MACROS, 0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A planned item needs a food, a quantity and its macros");
    }

    @Test
    void rejectsAPlannedQuantityOfZero() {
        Quantity zero = new Quantity(BigDecimal.ZERO, MeasurementUnit.GRAM);

        assertThatThrownBy(() -> new Planned(UUID.randomUUID(), "Arroz", zero, MACROS, 0))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The planned quantity must be positive");
    }

    @Test
    void rejectsANegativeSlot() {
        assertThatThrownBy(() -> new Planned(UUID.randomUUID(), "Arroz", GRAMS, MACROS, -1))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The planned slot must be zero or positive");
    }
}
