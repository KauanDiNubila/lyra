package app.lyra.food.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodNotFoundException;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FindFoodsByIdsServiceTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients ANY = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final FindFoodsByIdsService service = new FindFoodsByIdsService(repository);

    @Test
    void returnsTheRequestedFoodsByTheirIds() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, ANY);
        Food beans = Food.custom("Feijão", HUNDRED_GRAMS, ANY);
        repository.save(rice);
        repository.save(beans);
        repository.save(Food.custom("Frango", HUNDRED_GRAMS, ANY));

        assertThat(service.findByIds(UserId.random(), List.of(rice.getId(), beans.getId())))
                .containsOnlyKeys(rice.getId(), beans.getId())
                .containsEntry(rice.getId(), rice)
                .containsEntry(beans.getId(), beans);
    }

    @Test
    void aRepeatedIdIsReturnedOnce() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, ANY);
        repository.save(rice);

        assertThat(service.findByIds(UserId.random(), List.of(rice.getId(), rice.getId(), rice.getId()))).containsOnlyKeys(rice.getId());
    }

    @Test
    void returnsNothingWhenNoIdIsRequested() {
        repository.save(Food.custom("Arroz", HUNDRED_GRAMS, ANY));

        assertThat(service.findByIds(UserId.random(), List.of())).isEmpty();
    }

    @Test
    void failsNamingTheFirstFoodThatDoesNotExist() {
        Food rice = Food.custom("Arroz", HUNDRED_GRAMS, ANY);
        repository.save(rice);
        UUID unknown = UUID.randomUUID();
        List<UUID> ids = List.of(rice.getId(), unknown);

        assertThatThrownBy(() -> service.findByIds(UserId.random(), ids))
                .isInstanceOf(FoodNotFoundException.class)
                .hasMessage("Food not found: " + unknown);
    }
}
