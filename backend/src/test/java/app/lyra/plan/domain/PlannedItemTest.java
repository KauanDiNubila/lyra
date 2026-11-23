package app.lyra.plan.domain;

import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static app.lyra.plan.domain.PlanFixtures.RICE_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannedItemTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);

    @Test
    void keepsTheFoodAndTheQuantity() {
        PlannedItem item = new PlannedItem(RICE_ID, HUNDRED_GRAMS);

        assertThat(item.foodId()).isEqualTo(RICE_ID);
        assertThat(item.quantity()).isEqualTo(HUNDRED_GRAMS);
    }

    @Test
    void rejectsAQuantityOfZero() {
        Quantity zero = new Quantity(BigDecimal.ZERO, MeasurementUnit.GRAM);

        assertThatThrownBy(() -> new PlannedItem(RICE_ID, zero))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("The planned quantity must be positive");
    }

    @Test
    void requiresTheFoodAndTheQuantity() {
        assertThatThrownBy(() -> new PlannedItem(null, HUNDRED_GRAMS))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A planned item needs a food and a quantity");
        assertThatThrownBy(() -> new PlannedItem(RICE_ID, null)).isInstanceOf(InvalidInputException.class);
    }
}
