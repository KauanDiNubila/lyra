package app.lyra.food.application.service;

import app.lyra.food.domain.Food;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.shared.domain.InvalidInputException;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FoodSearchServiceTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients ANY = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    private final InMemoryFoodRepository repository = new InMemoryFoodRepository();
    private final FoodSearchService service = new FoodSearchService(repository);

    @Test
    void findsFoodsMatchingEveryTermIgnoringAccentsAndCase() {
        repository.save(Food.custom("Feijão, carioca, cozido", HUNDRED_GRAMS, ANY));
        repository.save(Food.custom("Feijão, preto, cozido", HUNDRED_GRAMS, ANY));
        repository.save(Food.custom("Arroz, tipo 1, cozido", HUNDRED_GRAMS, ANY));

        assertThat(service.search(UserId.random(), "FEIJAO cozido")).extracting(Food::getName)
                .containsExactlyInAnyOrder("Feijão, carioca, cozido", "Feijão, preto, cozido");
    }

    @Test
    void returnsAtMostTwentyFoods() {
        IntStream.range(0, 25).forEach(i -> repository.save(Food.custom("Arroz " + i, HUNDRED_GRAMS, ANY)));

        assertThat(service.search(UserId.random(), "arroz")).hasSize(FoodSearchService.MAX_RESULTS);
    }

    @Test
    void rejectsAQueryThatIsTooShort() {
        assertThatThrownBy(() -> service.search(UserId.random(), "a")).isInstanceOf(InvalidInputException.class);
    }
}
