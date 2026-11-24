package app.lyra.food.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindFoodServiceTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients ANY = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final FindFoodService service = new FindFoodService(repository);

    @Test
    void returnsTheFoodWithThatId() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, ANY);
        repository.save(rice);
        repository.save(Food.custom("Feijão", HUNDRED_GRAMS, ANY));

        assertThat(service.findById(UserId.random(), rice.getId())).isEqualTo(rice);
    }

    @Test
    void failsWhenThereIsNoFoodWithThatId() {
        UUID unknown = UUID.randomUUID();

        assertThatThrownBy(() -> service.findById(UserId.random(), unknown))
                .isInstanceOf(FoodNotFoundException.class)
                .hasMessage("Food not found: " + unknown);
    }
}
