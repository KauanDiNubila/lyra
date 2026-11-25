package app.lyra.food.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ListCustomFoodsServiceTest {

    private static final UserId USER = UserId.random();

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients ANY = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final ListCustomFoodsService service = new ListCustomFoodsService(repository);

    @Test
    void listsTheCustomFoodsOfTheUserAndNothingOfTheOthers() {
        Food mine = Food.custom("Meu bolo", HUNDRED_GRAMS, ANY);
        repository.save(USER, mine);
        repository.save(UserId.random(), Food.custom("Bolo dele", HUNDRED_GRAMS, ANY));

        assertThat(service.listCustomFoods(USER)).containsExactly(mine);
    }

    @Test
    void listsNothingWhenTheUserHasNoCustomFood() {
        assertThat(service.listCustomFoods(USER)).isEmpty();
    }
}
