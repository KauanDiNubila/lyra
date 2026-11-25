package app.lyra.food.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.TestUsers;
import app.lyra.food.application.port.out.FoodRepository;
import app.lyra.food.application.port.out.FoodRepositoryContract;
import app.lyra.food.domain.Barcode;
import app.lyra.food.domain.Food;
import app.lyra.food.domain.FoodSearchQuery;
import app.lyra.food.domain.FoodSource;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Nutrients;
import app.lyra.food.domain.Quantity;
import app.lyra.food.domain.SearchText;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, FoodPersistenceAdapter.class})
class FoodPersistenceAdapterTest implements FoodRepositoryContract {

    @Autowired
    private FoodPersistenceAdapter adapter;

    @Autowired
    private JdbcClient jdbc;

    private UserId owner;
    private UserId otherOwner;

    @BeforeEach
    void createTheUsers() {
        owner = TestUsers.insert(jdbc);
        otherOwner = TestUsers.insert(jdbc);
    }

    @Override
    public void seedTaco(Food food) {
        jdbc.sql("""
                        insert into food (id, name, search_name, source, source_reference, reference_amount, reference_unit,
                            kcal, protein, carbohydrate, fat)
                        values (:id, :name, :searchName, 'TACO', :reference, 100, 'GRAM', 1, 1, 1, 1)
                        """)
                .param("id", food.getId())
                .param("name", food.getName())
                .param("searchName", SearchText.normalize(food.getName()))
                .param("reference", "TEST-" + food.getId())
                .update();
    }

    @Override
    public UserId owner() {
        return owner;
    }

    @Override
    public UserId otherOwner() {
        return otherOwner;
    }

    @Override
    public FoodRepository repository() {
        return adapter;
    }

    @AfterEach
    void removeFoodsCreatedByTheTest() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoRequestsStoringTheSameProductAtTheSameTimeKeepASingleRow() throws Exception {
        int rounds = 40;
        try (ExecutorService pool = Executors.newFixedThreadPool(2)) {
            for (int round = 1; round <= rounds; round++) {
                Barcode barcode = new Barcode(String.format("%014d", round));
                CyclicBarrier bothAreReady = new CyclicBarrier(2);
                Callable<Food> store = () -> {
                    Food product = new Food(UUID.randomUUID(), "Quimera corrida", FoodSource.OPEN_FOOD_FACTS,
                            new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM),
                            new Nutrients(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, null, null));
                    bothAreReady.await();
                    return adapter.saveProduct(barcode, product);
                };
                Future<Food> first = pool.submit(store);
                Future<Food> second = pool.submit(store);

                assertThat(second.get().getId()).isEqualTo(first.get().getId());
            }
        }

        assertThat(jdbc.sql("select count(*) from food where source_reference like 'off:%'").query(Integer.class).single())
                .isEqualTo(rounds);
    }

    @Test
    void tacoMigrationSeedsTheImportedFoods() {
        Integer count = jdbc.sql("select count(*) from food where source = 'TACO'").query(Integer.class).single();

        assertThat(count).isEqualTo(592);
    }

    @Test
    void tacoFoodsKeepTheirPublishedValues() {
        Food rice = adapter.search(owner, FoodSearchQuery.of("arroz tipo 1 cozido"), 1).getFirst();

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
