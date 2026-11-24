package app.lyra.plan.domain;

import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static app.lyra.plan.domain.PlanFixtures.item;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannedMealTest {

    @Test
    void trimsTheNameAndKeepsTheOptionalTime() {
        PlannedMeal meal = new PlannedMeal("  Almoço ", LocalTime.of(12, 30), List.of(item("150")));

        assertThat(meal.name()).isEqualTo("Almoço");
        assertThat(meal.time()).isEqualTo(LocalTime.of(12, 30));
        assertThat(new PlannedMeal("Lanche", null, List.of(item("50"))).time()).isNull();
    }

    @Test
    void rejectsABlankOrTooLongName() {
        List<PlannedItem> items = List.of(item("150"));

        for (String invalid : new String[]{null, "", "   ", "x".repeat(51)}) {
            assertThatThrownBy(() -> new PlannedMeal(invalid, null, items))
                    .isInstanceOf(InvalidInputException.class)
                    .hasMessage("Meal name must have between 1 and 50 characters");
        }
    }

    @Test
    void acceptsANameOfFiftyCharacters() {
        assertThat(new PlannedMeal("x".repeat(50), null, List.of(item("150"))).name()).hasSize(50);
    }

    @Test
    void needsAtLeastOneItem() {
        assertThatThrownBy(() -> new PlannedMeal("Almoço", null, null))
                .isInstanceOf(InvalidInputException.class)
                .hasMessage("A meal needs at least one item");
        assertThatThrownBy(() -> new PlannedMeal("Almoço", null, List.of())).isInstanceOf(InvalidInputException.class);
    }

    @Test
    void keepsACopyOfTheItems() {
        List<PlannedItem> items = new ArrayList<>(List.of(item("150")));
        PlannedMeal meal = new PlannedMeal("Almoço", null, items);

        items.add(item("50"));

        assertThat(meal.items()).hasSize(1);
        assertThatThrownBy(() -> meal.items().add(item("50"))).isInstanceOf(UnsupportedOperationException.class);
    }
}
