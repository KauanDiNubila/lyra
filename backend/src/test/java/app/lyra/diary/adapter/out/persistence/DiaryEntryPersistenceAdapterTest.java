package app.lyra.diary.adapter.out.persistence;

import app.lyra.TestcontainersConfiguration;
import app.lyra.TestUsers;
import app.lyra.diary.application.port.out.DiaryEntryRepository;
import app.lyra.diary.application.port.out.DiaryEntryRepositoryContract;
import app.lyra.diary.domain.DiaryEntry;
import app.lyra.shared.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, DiaryEntryPersistenceAdapter.class})
class DiaryEntryPersistenceAdapterTest implements DiaryEntryRepositoryContract {

    @Autowired
    private DiaryEntryPersistenceAdapter adapter;

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
    public DiaryEntryRepository repository() {
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
        jdbc.sql("delete from diary_entry").update();
        jdbc.sql("delete from diet_plan").update();
        jdbc.sql("delete from food where source_reference is null").update();
        jdbc.sql("delete from app_user").update();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsAnEntryOfAFoodThatDoesNotExist() {
        DiaryEntry entry = entry(DATE, "Almoço", "100", nutrients(null, null));
        DiaryEntry orphan = new DiaryEntry(entry.getId(), DATE, "Almoço", UUID.randomUUID(), "Arroz",
                entry.getFoodReference(), entry.getNutrients(), entry.getQuantity());

        assertThatThrownBy(() -> adapter.save(owner, orphan)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsTwoEntriesOfTheSameDateForTheSameSlot() {
        UUID foodId = savedFoodId();
        adapter.save(owner, planEntry(DATE, "Almoço", foodId, foodId, 0, "150"));
        DiaryEntry again = planEntry(DATE, "Jantar", foodId, foodId, 0, "100");

        assertThatThrownBy(() -> adapter.save(owner, again))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("diary_entry_user_id_entry_date_planned_slot_idx");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void acceptsTheSameSlotOnAnotherDate() {
        UUID foodId = savedFoodId();
        adapter.save(owner, planEntry(DATE, "Almoço", foodId, foodId, 0, "150"));

        assertThatCode(() -> adapter.save(owner, planEntry(DATE.plusDays(1), "Almoço", foodId, foodId, 0, "150")))
                .doesNotThrowAnyException();
    }


    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void acceptsTheSameSlotOfTheSameDateForAnotherUser() {
        UUID foodId = savedFoodId();
        adapter.save(owner, planEntry(DATE, "Almoço", foodId, foodId, 0, "150"));

        assertThatCode(() -> adapter.save(otherOwner, planEntry(DATE, "Almoço", foodId, foodId, 0, "150")))
                .doesNotThrowAnyException();
    }
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void rejectsAPlannedFoodThatDoesNotExist() {
        UUID foodId = savedFoodId();
        DiaryEntry entry = planEntry(DATE, "Almoço", foodId, UUID.randomUUID(), 0, "150");

        assertThatThrownBy(() -> adapter.save(owner, entry))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("diary_entry_planned_food_id_fkey");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesAQuantityOfZeroOutsideThePlan() {
        UUID foodId = savedFoodId();

        assertThatThrownBy(() -> insertRow(foodId, "0", ""))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("diary_entry_quantity_amount_check");
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void refusesHalfOfThePlannedColumns() {
        UUID foodId = savedFoodId();

        assertThatThrownBy(() -> insertRow(foodId, "1", ", planned_slot", ", 0"))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("diary_entry_planned_all_or_none_check");
    }

    private void insertRow(UUID foodId, String quantity, String extraColumns) {
        insertRow(foodId, quantity, extraColumns, "");
    }

    private void insertRow(UUID foodId, String quantity, String extraColumns, String extraValues) {
        jdbc.sql("""
                        insert into diary_entry (id, user_id, entry_date, meal_name, food_id, food_name, reference_amount,
                            reference_unit, kcal, protein, carbohydrate, fat, quantity_amount, quantity_unit%s)
                        values (:id, :user, :date, 'Almoço', :food, 'Arroz', 100, 'GRAM', 1, 1, 1, 1, %s, 'GRAM'%s)
                        """.formatted(extraColumns, quantity, extraValues))
                .param("id", UUID.randomUUID())
                .param("user", owner.value())
                .param("date", DATE)
                .param("food", foodId)
                .update();
    }
}
