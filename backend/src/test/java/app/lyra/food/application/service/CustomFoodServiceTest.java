package app.lyra.food.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomFoodServiceTest {

    private static final Quantity ONE_PORTION = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);
    private static final Nutrients MILKSHAKE = new Nutrients(
            new BigDecimal("590"), new BigDecimal("13"), new BigDecimal("98"), new BigDecimal("16"), null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final CustomFoodService service = new CustomFoodService(repository);

    @Test
    void createsAndStoresACustomFood() {
        Food food = service.create("Milkshake McDonald's", ONE_PORTION, MILKSHAKE);

        assertThat(food.getSource()).isEqualTo(FoodSource.CUSTOM);
        assertThat(repository.all()).containsExactly(food);
    }

    @Test
    void storesNothingWhenTheFoodIsInvalid() {
        assertThatThrownBy(() -> service.create("", ONE_PORTION, MILKSHAKE)).isInstanceOf(InvalidInputException.class);

        assertThat(repository.all()).isEmpty();
    }
}
