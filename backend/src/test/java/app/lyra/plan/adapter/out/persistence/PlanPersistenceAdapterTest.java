package app.lyra.plan.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.application.port.out.PlanRepositoryContract;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, PlanPersistenceAdapter.class})
class PlanPersistenceAdapterTest implements PlanRepositoryContract {

    @Autowired
    private PlanPersistenceAdapter adapter;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Autowired
    private JdbcClient jdbc;

    @Override
    public PlanRepository repository() {
        return adapter;
    }

    @Override
    public UUID savedFoodId() {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into food (id, name, search_name, source, reference_amount, reference_unit, kcal, protein, carbohydrate, fat)
                        values (:id, 'Arroz', 'arroz', 'CUSTOM', 100, 'GRAM', 1, 1, 1, 1)
                        """)
                .param("id", id)
                .update();
        return id;
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        jdbc.sql("delete from diet_plan").update();
        jdbc.sql("delete from food where source_reference is null").update();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsASecondActivePlan() {
        adapter.save(plan("Primeiro").activated());
        DietPlan second = plan("Segundo").activated();

        assertThatThrownBy(() -> adapter.save(second)).hasMessageContaining("diet_plan_single_active_idx");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsAnItemOfAFoodThatDoesNotExist() {
        PlannedItem orphan = new PlannedItem(UUID.randomUUID(), new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM));
        DietPlan plan = DietPlan.create("Cutting", plan("Modelo").getTargets(),
                List.of(new PlannedMeal("Almoço", null, List.of(orphan))));

        assertThatThrownBy(() -> adapter.save(plan)).hasMessageContaining("planned_item_food_id_fkey");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replacingThePlanLeavesNoMealOrItemBehind() {
        DietPlan plan = plan("Cutting");
        adapter.save(plan);
        assertThat(count("planned_meal")).isEqualTo(2);
        assertThat(count("planned_item")).isEqualTo(3);

        adapter.save(plan.withContent("Cutting", plan.getTargets(), List.of(new PlannedMeal("Almoço", null, List.of(item("50"))))));

        assertThat(count("planned_meal")).isEqualTo(1);
        assertThat(count("planned_item")).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void activatingAPlanKeepsTheIdsOfItsMealsAndItems() {
        DietPlan plan = plan("Cutting");
        adapter.save(plan);
        List<UUID> before = childIds();

        adapter.save(plan.activated());

        assertThat(before).hasSize(5);
        assertThat(childIds()).containsExactlyInAnyOrderElementsOf(before);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listingPlansLoadsTheirMealsAndItemsInBatches() {
        for (int index = 0; index < 5; index++) {
            adapter.save(plan("Plano " + index));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertThat(adapter.findAll()).hasSize(5);

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private List<UUID> childIds() {
        return jdbc.sql("select id from planned_meal union all select id from planned_item").query(UUID.class).list();
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }
}
