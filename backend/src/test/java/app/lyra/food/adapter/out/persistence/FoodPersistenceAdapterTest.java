package app.lyra.food.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.application.port.out.FoodRepositoryContract;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FoodPersistenceAdapter.class})
class FoodPersistenceAdapterTest implements FoodRepositoryContract {

    @Autowired
    private FoodPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    @Override
    public FoodRepository repository() {
        return adapter;
    }

    @AfterEach
    void removeFoodsCreatedByTheTest() {
        jdbc.sql("delete from food where source_reference is null").update();
    }

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
}
