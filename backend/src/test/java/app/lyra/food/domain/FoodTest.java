package app.lyra.food.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients RICE = new Nutrients(
            new BigDecimal("128.3"), new BigDecimal("2.5"), new BigDecimal("28.1"), new BigDecimal("0.2"), null, null);

    @Test
    void customFoodGetsAnIdAndTheCustomSource() {
        Food food = Food.custom("  Arroz da casa  ", HUNDRED_GRAMS, RICE);

        assertThat(food.getId()).isNotNull();
        assertThat(food.getSource()).isEqualTo(FoodSource.CUSTOM);
        assertThat(food.getName()).isEqualTo("Arroz da casa");
        assertThat(food.getReferenceAmount()).isEqualTo(HUNDRED_GRAMS);
        assertThat(food.getNutrients()).isEqualTo(RICE);
    }

    @Test
    void foodsWithTheSameIdAreEqual() {
        UUID id = UUID.randomUUID();

        Food first = new Food(id, "Arroz", FoodSource.TACO, HUNDRED_GRAMS, RICE);
        Food second = new Food(id, "Outro nome", FoodSource.CUSTOM, HUNDRED_GRAMS, RICE);

        assertThat(first).isEqualTo(second).hasSameHashCodeAs(second);
        assertThat(first).isNotEqualTo(Food.custom("Arroz", HUNDRED_GRAMS, RICE));
    }

    @Test
    void nameMustHaveBetweenTwoAndTwoHundredCharacters() {
        assertThatThrownBy(() -> Food.custom(" a ", HUNDRED_GRAMS, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThatThrownBy(() -> Food.custom(null, HUNDRED_GRAMS, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThatThrownBy(() -> Food.custom("x".repeat(201), HUNDRED_GRAMS, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThat(Food.custom("x".repeat(200), HUNDRED_GRAMS, RICE).getName()).hasSize(200);
    }

    @Test
    void referenceAmountMustBePositive() {
        Quantity zero = new Quantity(BigDecimal.ZERO, MeasurementUnit.GRAM);

        assertThatThrownBy(() -> Food.custom("Arroz", zero, RICE))
                .isInstanceOf(InvalidFoodException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void requiredPartsMustBePresent() {
        UUID id = UUID.randomUUID();

        assertThatThrownBy(() -> new Food(null, "Arroz", FoodSource.TACO, HUNDRED_GRAMS, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThatThrownBy(() -> new Food(id, "Arroz", null, HUNDRED_GRAMS, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThatThrownBy(() -> new Food(id, "Arroz", FoodSource.TACO, null, RICE)).isInstanceOf(InvalidFoodException.class);
        assertThatThrownBy(() -> new Food(id, "Arroz", FoodSource.TACO, HUNDRED_GRAMS, null)).isInstanceOf(InvalidFoodException.class);
    }
}
