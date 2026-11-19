package app.lyra.food.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FoodPersistenceAdapter.class})
class FoodPersistenceAdapterTest {

    private static final Quantity HUNDRED_GRAMS = new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM);
    private static final Nutrients ANY = new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null);

    @Autowired
    private FoodPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    @Test
    void tacoMigrationSeedsTheImportedFoods() {
        Integer count = jdbc.sql("select count(*) from food where source = 'TACO'").query(Integer.class).single();

        assertThat(count).isEqualTo(592);
    }

    @Test
    void tacoFoodsKeepTheirPublishedValues() {
        Food rice = adapter.search(FoodSearchQuery.of("arroz tipo 1 cozido"), 1).getFirst();

        assertThat(rice.getName()).isEqualTo("Arroz, tipo 1, cozido");
        assertThat(rice.getSource()).isEqualTo(FoodSource.TACO);
        assertThat(rice.getReferenceAmount().amount()).isEqualByComparingTo("100");
        assertThat(rice.getReferenceAmount().unit()).isEqualTo(MeasurementUnit.GRAM);
        assertThat(rice.getNutrients().kcal()).isEqualByComparingTo("128.2585");
        assertThat(rice.getNutrients().protein()).isEqualByComparingTo("2.5208");
        assertThat(rice.getNutrients().carbohydrate()).isEqualByComparingTo("28.0598");
        assertThat(rice.getNutrients().fat()).isEqualByComparingTo("0.227");
        assertThat(rice.getNutrients().fiber()).isEqualByComparingTo("1.561");
        assertThat(rice.getNutrients().sodium()).isEqualByComparingTo("1.2007");
    }

    @Test
    void searchIgnoresAccentsAndRequiresEveryTerm() {
        List<Food> found = adapter.search(FoodSearchQuery.of("feijao carioca cozido"), 20);

        assertThat(found).isNotEmpty().allSatisfy(food -> assertThat(food.getName())
                .containsIgnoringCase("feijão").containsIgnoringCase("carioca").containsIgnoringCase("cozido"));
    }

    @Test
    void customFoodsComeFirstThenShorterNames() {
        adapter.save(Food.custom("Arroz da vó, com alho e cebola", HUNDRED_GRAMS, ANY));

        List<String> names = adapter.search(FoodSearchQuery.of("arroz"), 3).stream().map(Food::getName).toList();

        assertThat(names.getFirst()).isEqualTo("Arroz da vó, com alho e cebola");
        assertThat(names.get(1).length()).isLessThanOrEqualTo(names.get(2).length());
    }

    @Test
    void searchHonoursTheLimit() {
        IntStream.range(0, 5).forEach(i -> adapter.save(Food.custom("Bolo caseiro " + i, HUNDRED_GRAMS, ANY)));

        assertThat(adapter.search(FoodSearchQuery.of("bolo caseiro"), 3)).hasSize(3);
    }

    @Test
    void savedFoodComesBackWithTheSameValuesIncludingUnknownNutrients() {
        Quantity onePortion = new Quantity(BigDecimal.ONE, MeasurementUnit.PORTION);
        Nutrients milkshake = new Nutrients(new BigDecimal("590.5"), new BigDecimal("13"), new BigDecimal("98.25"),
                new BigDecimal("16"), null, new BigDecimal("0.123456"));
        Food saved = Food.custom("Milkshake de morango", onePortion, milkshake);

        adapter.save(saved);
        Food found = adapter.search(FoodSearchQuery.of("milkshake morango"), 1).getFirst();

        assertThat(found).isEqualTo(saved);
        assertThat(found.getSource()).isEqualTo(FoodSource.CUSTOM);
        assertThat(found.getReferenceAmount()).isEqualTo(onePortion);
        assertThat(found.getNutrients()).isEqualTo(milkshake);
    }

    @Test
    void searchFindsNothingForUnknownTerms() {
        assertThat(adapter.search(FoodSearchQuery.of("xyzzy"), 20)).isEmpty();
    }
}
