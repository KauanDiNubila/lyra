package app.lyra.plan.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.TestUsers;
import app.lyra.food.domain.MeasurementUnit;
import app.lyra.food.domain.Quantity;
import app.lyra.plan.application.port.out.PlanRepository;
import app.lyra.plan.application.port.out.PlanRepositoryContract;
import app.lyra.plan.domain.DietPlan;
import app.lyra.plan.domain.PlannedItem;
import app.lyra.plan.domain.PlannedMeal;
import app.lyra.shared.domain.UserId;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
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

    private UserId owner;
    private UserId otherOwner;

    @BeforeEach
    void createTheUsers() {
        owner = TestUsers.insert(jdbc);
        otherOwner = TestUsers.insert(jdbc);
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
    public PlanRepository repository() {
        return adapter;
    }

    @Override
    public UUID savedFoodId() {
        UUID id = UUID.randomUUID();
        jdbc.sql("""
                        insert into food (id, user_id, name, search_name, source, reference_amount, reference_unit, kcal, protein, carbohydrate, fat)
                        values (:id, :user, 'Arroz', 'arroz', 'CUSTOM', 100, 'GRAM', 1, 1, 1, 1)
                        """)
                .param("id", id)
                .param("user", owner.value())
                .update();
        return id;
    }

    @AfterEach
    void removeWhatTheTestCreated() {
        TestUsers.deleteEverything(jdbc);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsASecondActivePlan() {
        adapter.save(owner, plan("Primeiro").activated());
        DietPlan second = plan("Segundo").activated();

        assertThatThrownBy(() -> adapter.save(owner, second)).hasMessageContaining("diet_plan_single_active_per_user_idx");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsAnItemOfAFoodThatDoesNotExist() {
        PlannedItem orphan = new PlannedItem(UUID.randomUUID(), new Quantity(new BigDecimal("100"), MeasurementUnit.GRAM));
        DietPlan plan = DietPlan.create("Cutting", plan("Modelo").getTargets(),
                List.of(new PlannedMeal("Almoço", null, List.of(orphan))));

        assertThatThrownBy(() -> adapter.save(owner, plan)).hasMessageContaining("planned_item_food_id_fkey");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void replacingThePlanLeavesNoMealOrItemBehind() {
        DietPlan plan = plan("Cutting");
        adapter.save(owner, plan);
        assertThat(count("planned_meal")).isEqualTo(2);
        assertThat(count("planned_item")).isEqualTo(3);

        adapter.save(owner, plan.withContent("Cutting", plan.getTargets(), List.of(new PlannedMeal("Almoço", null, List.of(item("50"))))));

        assertThat(count("planned_meal")).isEqualTo(1);
        assertThat(count("planned_item")).isEqualTo(1);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void activatingAPlanKeepsTheIdsOfItsMealsAndItems() {
        DietPlan plan = plan("Cutting");
        adapter.save(owner, plan);
        List<UUID> before = childIds();

        adapter.save(owner, plan.activated());

        assertThat(before).hasSize(5);
        assertThat(childIds()).containsExactlyInAnyOrderElementsOf(before);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void listingPlansLoadsTheirMealsAndItemsInBatches() {
        for (int index = 0; index < 5; index++) {
            adapter.save(owner, plan("Plano " + index));
        }
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        assertThat(adapter.findAll(owner)).hasSize(5);

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
    }

    private List<UUID> childIds() {
        return jdbc.sql("select id from planned_meal union all select id from planned_item").query(UUID.class).list();
    }

    private int count(String table) {
        return jdbc.sql("select count(*) from " + table).query(Integer.class).single();
    }
}
